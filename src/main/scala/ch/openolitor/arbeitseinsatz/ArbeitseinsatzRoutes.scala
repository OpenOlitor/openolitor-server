/*                                                                           *\
*    ____                   ____  ___ __                                      *
*   / __ \____  ___  ____  / __ \/ (_) /_____  _____                          *
*  / / / / __ \/ _ \/ __ \/ / / / / / __/ __ \/ ___/   OpenOlitor             *
* / /_/ / /_/ /  __/ / / / /_/ / / / /_/ /_/ / /       contributed by tegonal *
* \____/ .___/\___/_/ /_/\____/_/_/\__/\____/_/        http://openolitor.ch   *
*     /_/                                                                     *
*                                                                             *
* This program is free software: you can redistribute it and/or modify it     *
* under the terms of the GNU General Public License as published by           *
* the Free Software Foundation, either version 3 of the License,              *
* or (at your option) any later version.                                      *
*                                                                             *
* This program is distributed in the hope that it will be useful, but         *
* WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY  *
* or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for *
* more details.                                                               *
*                                                                             *
* You should have received a copy of the GNU General Public License along     *
* with this program. If not, see http://www.gnu.org/licenses/                 *
*                                                                             *
\*                                                                           */
package ch.openolitor.arbeitseinsatz

import akka.actor._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.http.scaladsl.server.Route
import ch.openolitor.arbeitseinsatz.eventsourcing.ArbeitseinsatzEventStoreSerializer
import ch.openolitor.arbeitseinsatz.models._
import ch.openolitor.arbeitseinsatz.reporting._
import ch.openolitor.arbeitseinsatz.repositories._
import ch.openolitor.core._
import ch.openolitor.core.db._
import ch.openolitor.core.domain.EntityStore._
import ch.openolitor.core.filestore._
import ch.openolitor.core.models._
import ch.openolitor.core.security.Subject
import ch.openolitor.stammdaten.models.KundeId
import ch.openolitor.util.parsing.{ QueryFilter, UriQueryFilterParser, UriQueryParamGeschaeftsjahrParser }
import ch.openolitor.stammdaten.repositories.{ DefaultStammdatenReadRepositoryAsyncComponent, StammdatenReadRepositoryAsyncComponent }

import scala.concurrent.ExecutionContext

trait ArbeitseinsatzRoutes extends BaseRouteService
  with ActorReferences
  with AsyncConnectionPoolContextAware
  with ArbeitseinsatzJsonProtocol
  with ArbeitseinsatzEventStoreSerializer
  with ArbeitsangebotReportService
  with ArbeitseinsatzReportService
  with FileTypeFilenameMapping
  with Defaults {
  self: ArbeitseinsatzReadRepositoryAsyncComponent with FileStoreComponent with StammdatenReadRepositoryAsyncComponent =>

  implicit val kundeIdPath = long2BaseIdPathMatcher(KundeId.apply)

  implicit val arbeitskategorieIdPath = long2BaseIdPathMatcher(ArbeitskategorieId.apply)
  implicit val arbeitsangebotIdPath = long2BaseIdPathMatcher(ArbeitsangebotId.apply)
  implicit val arbeitseinsatzIdPath = long2BaseIdPathMatcher(ArbeitseinsatzId.apply)

  def arbeitseinsatzRoute(implicit subject: Subject): Route =
    parameters("q".?, "g".?) { (q, g) =>
      implicit val queryFilter = q flatMap {
        queryFilter =>
          UriQueryFilterParser.parse(queryFilter)
      }
      implicit val datumsFilter = g flatMap { geschaeftsjahrString =>
        UriQueryParamGeschaeftsjahrParser.parse(geschaeftsjahrString)
      }
      path("arbeitskategorien" ~ exportFormatPath.?) {
        exportFormat =>
          get(list(arbeitseinsatzReadRepository.getArbeitskategorien, exportFormat)) ~
            post(create[ArbeitskategorieModify, ArbeitskategorieId](ArbeitskategorieId.apply _))
      } ~
        path("arbeitskategorien" / arbeitskategorieIdPath) {
          id =>
            (put | post)(update[ArbeitskategorieModify, ArbeitskategorieId](id)) ~
              delete(remove(id))
        } ~
        path("arbeitsangebote" ~ exportFormatPath.?) {
          exportFormat =>
            get(list(arbeitseinsatzReadRepository.getArbeitsangebote, exportFormat)) ~
              post(create[ArbeitsangebotModify, ArbeitsangebotId](ArbeitsangebotId.apply _))
        } ~
        path("arbeitsangebote" / "zukunft" ~ exportFormatPath.?) {
          exportFormat =>
            get(list(arbeitseinsatzReadRepository.getFutureArbeitsangebote, exportFormat))
        } ~
        path("arbeitsangebote" / arbeitsangebotIdPath) {
          id =>
            get(detail(arbeitseinsatzReadRepository.getArbeitsangebot(id))) ~
              (put | post)(update[ArbeitsangebotModify, ArbeitsangebotId](id)) ~
              delete(remove(id))
        } ~
        path("arbeitsangebote" / arbeitsangebotIdPath / "archive") {
          id =>
              (put | post)(update[ArbeitsangebotModify, ArbeitsangebotId](id))
        } ~
        path("arbeitsangebote" / arbeitsangebotIdPath / "aktionen" / "duplizieren") {
          id =>
            post {
              extractRequest {
                request =>
                  entity(as[ArbeitsangeboteDuplicate]) {
                    entity =>
                      created(request)(entity)
                  }
              }
            }
        } ~
        path("arbeitsangebote" / "berichte" / "arbeitsangebote") {
          implicit val personId = subject.personId
          generateReport[ArbeitsangebotId](None, generateArbeitsangebotReports(VorlageArbeitangebot) _)(ArbeitsangebotId.apply)
        } ~
        path("arbeitsangebote" / arbeitsangebotIdPath / "berichte" / "arbeitseinsatzbrief") {
          id =>
            (post) {
              implicit val personId = subject.personId
              generateReport[ArbeitsangebotId](Some(id), generateArbeitsangebotReports(VorlageArbeitangebot) _)(ArbeitsangebotId.apply)
            }
        } ~
        path("arbeitsangebote" / arbeitsangebotIdPath / "arbeitseinsaetze") {
          id =>
            get(list(arbeitseinsatzReadRepository.getArbeitseinsaetze(id))) ~
              post(create[ArbeitseinsatzModify, ArbeitseinsatzId](ArbeitseinsatzId.apply _))
        } ~
        path("arbeitsangebote" / arbeitsangebotIdPath / "arbeitseinsaetze" / arbeitseinsatzIdPath) {
          (arbeitsangebotId, arbeitseinsatzId) =>
            get(detail(arbeitseinsatzReadRepository.getArbeitseinsatz(arbeitseinsatzId))) ~
              (put | post)(update[ArbeitseinsatzModify, ArbeitseinsatzId](arbeitseinsatzId)) ~
              delete(remove(arbeitseinsatzId))
        } ~
        path("arbeitseinsaetze" ~ exportFormatPath.?) {
          exportFormat =>
            get(list(arbeitseinsatzReadRepository.getArbeitseinsaetze, exportFormat)) ~
              post(create[ArbeitseinsatzModify, ArbeitseinsatzId](ArbeitseinsatzId.apply _))
        } ~
        path("arbeitseinsaetze" / kundeIdPath ~ exportFormatPath.?) {
          (kundeId, exportFormat) =>
            get(list(arbeitseinsatzReadRepository.getArbeitseinsaetze(kundeId), exportFormat))
        } ~
        path("arbeitseinsaetze" / "zukunft" ~ exportFormatPath.?) {
          exportFormat =>
            get(list(arbeitseinsatzReadRepository.getFutureArbeitseinsaetze, exportFormat))
        } ~
        path("arbeitseinsaetze" / arbeitseinsatzIdPath) {
          id =>
            get(detail(arbeitseinsatzReadRepository.getArbeitseinsatz(id))) ~
              (put | post)(update[ArbeitseinsatzModify, ArbeitseinsatzId](id)) ~
              delete(remove(id))
        } ~
        path("arbeitseinsaetze" / "berichte" / "arbeitseinsatzbrief") {
          implicit val personId = subject.personId
          generateReport[ArbeitseinsatzId](None, generateArbeitseinsatzReports(VorlageArbeitseinsatz) _)(ArbeitseinsatzId.apply)
        } ~
        path("arbeitseinsaetze" / arbeitseinsatzIdPath / "berichte" / "arbeitseinsatzbrief") {
          id =>
            post {
              implicit val personId = subject.personId
              generateReport[ArbeitseinsatzId](Some(id), generateArbeitseinsatzReports(VorlageArbeitseinsatz) _)(ArbeitseinsatzId.apply)
            }
        } ~
        path("arbeitseinsaetze" / kundeIdPath / "zukunft" ~ exportFormatPath.?) {
          (kunedId, exportFormat) =>
            get(list(arbeitseinsatzReadRepository.getFutureArbeitseinsaetze(kunedId), exportFormat))
        } ~
        path("arbeitseinsatzabrechnung" ~ exportFormatPath.?) {
          exportFormat =>
            parameter("x".as[ArbeitsComplexFlags] ?) {
              xFlags: Option[ArbeitsComplexFlags] =>
                get(list(arbeitseinsatzReadRepository.getArbeitseinsatzabrechnung(xFlags), exportFormat))
            }
        } ~
        path("mailing" / "sendEmailToArbeitsangebotPersonen") {
          post {
            entity(as[ArbeitsangebotMailRequest]) {
              arbeitsangebotMailRequest =>
                sendEmailToArbeitsangebotPersonen(arbeitsangebotMailRequest.subject, arbeitsangebotMailRequest.body, arbeitsangebotMailRequest.replyTo, arbeitsangebotMailRequest.ids)
            }
          }
        }
    }

  private def sendEmailToArbeitsangebotPersonen(emailSubject: String, body: String, replyTo: Option[String], ids: Seq[ArbeitsangebotId])(implicit subject: Subject) = {
    onSuccess((entityStore ? ArbeitseinsatzCommandHandler.SendEmailToArbeitsangebotPersonenCommand(subject.personId, emailSubject, body, replyTo, ids))) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Something went wrong with the mail generation, please check the correctness of the template.")
      case _ =>
        complete("")
    }
  }

}

class DefaultArbeitseinsatzRoutes(
  override val dbEvolutionActor: ActorRef,
  override val entityStore: ActorRef,
  override val eventStore: ActorRef,
  override val mailService: ActorRef,
  override val reportSystem: ActorRef,
  override val sysConfig: SystemConfig,
  override val system: ActorSystem,
  override val airbrakeNotifier: ActorRef,
  override val jobQueueService: ActorRef
) extends ArbeitseinsatzRoutes
  with DefaultArbeitseinsatzReadRepositoryAsyncComponent
  with DefaultFileStoreComponent
  with DefaultStammdatenReadRepositoryAsyncComponent {
  override implicit protected val executionContext: ExecutionContext = system.dispatcher
}
