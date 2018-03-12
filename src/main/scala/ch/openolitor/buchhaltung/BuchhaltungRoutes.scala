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
package ch.openolitor.buchhaltung

import spray.routing._
import spray.http._
import spray.httpx.marshalling.ToResponseMarshallable._
import spray.httpx.SprayJsonSupport._
import ch.openolitor.core._
import ch.openolitor.core.domain._
import ch.openolitor.core.db._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util._
import akka.pattern.ask
import ch.openolitor.buchhaltung.eventsourcing.BuchhaltungEventStoreSerializer
import stamina.Persister
import ch.openolitor.buchhaltung.models._
import ch.openolitor.stammdaten.models._
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.filestore._
import akka.actor._
import ch.openolitor.buchhaltung.zahlungsimport.ZahlungsImportParser
import ch.openolitor.buchhaltung.zahlungsimport.ZahlungsImportRecordResult
import ch.openolitor.core.security.Subject
import ch.openolitor.stammdaten.repositories.StammdatenReadRepositoryAsyncComponent
import ch.openolitor.stammdaten.repositories.DefaultStammdatenReadRepositoryAsyncComponent
import ch.openolitor.buchhaltung.reporting.RechnungReportService
import ch.openolitor.util.parsing.UriQueryParamFilterParser
import ch.openolitor.util.parsing.FilterExpr
import ch.openolitor.buchhaltung.repositories.DefaultBuchhaltungReadRepositoryAsyncComponent
import ch.openolitor.buchhaltung.repositories.BuchhaltungReadRepositoryAsyncComponent
import ch.openolitor.buchhaltung.reporting.MahnungReportService
import java.io._
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import scala.concurrent.duration.SECONDS
import scala.concurrent.duration.Duration

import ch.openolitor.buchhaltung.rechnungsexport.RechnungExportRecordResult
import ch.openolitor.buchhaltung.rechnungsexport.iso20022.Pain008_003_02_Export
import scalikejdbc.TxBoundary.Future

import scala.concurrent.Await

trait BuchhaltungRoutes extends HttpService with ActorReferences
  with AsyncConnectionPoolContextAware with SprayDeserializers with DefaultRouteService with LazyLogging
  with BuchhaltungJsonProtocol
  with BuchhaltungEventStoreSerializer
  with RechnungReportService
  with MahnungReportService
  with BuchhaltungDBMappings {
  self: BuchhaltungReadRepositoryAsyncComponent with FileStoreComponent with StammdatenReadRepositoryAsyncComponent =>

  implicit val rechnungIdPath = long2BaseIdPathMatcher(RechnungId.apply)
  implicit val rechnungsPositionIdPath = long2BaseIdPathMatcher(RechnungsPositionId.apply)
  implicit val zahlungsImportIdPath = long2BaseIdPathMatcher(ZahlungsImportId.apply)
  implicit val zahlungsEingangIdPath = long2BaseIdPathMatcher(ZahlungsEingangId.apply)

  import EntityStore._

  def buchhaltungRoute(implicit subect: Subject) =
    parameters('f.?) { (f) =>
      implicit val filter = f flatMap { filterString =>
        UriQueryParamFilterParser.parse(filterString)
      }
      rechnungenRoute ~ rechnungspositionenRoute ~ zahlungsImportsRoute ~ mailingRoute
    }

  def rechnungenRoute(implicit subect: Subject, filter: Option[FilterExpr]) =
    path("rechnungen" ~ exportFormatPath.?) { exportFormat =>
      get(list(buchhaltungReadRepository.getRechnungen, exportFormat)) ~
        post(create[RechnungCreateFromRechnungsPositionen, RechnungId](RechnungId.apply _))
    } ~
      path("rechnungen" / "aktionen" / "downloadrechnungen") {
        post {
          requestInstance { request =>
            entity(as[RechnungenContainer]) { cont =>
              onSuccess(buchhaltungReadRepository.getByIds(rechnungMapping, cont.ids)) { rechnungen =>
                val fileStoreIds = rechnungen.map(_.fileStoreId.map(FileStoreFileId(_))).flatten
                logger.debug(s"Download rechnungen with filestoreRefs:$fileStoreIds")
                downloadAll("Rechnungen_" + System.currentTimeMillis + ".zip", GeneriertRechnung, fileStoreIds)
              }
            }
          }
        }
      } ~
      path("rechnungen" / "aktionen" / "downloadmahnungen") {
        post {
          requestInstance { request =>
            entity(as[RechnungenContainer]) { cont =>
              onSuccess(buchhaltungReadRepository.getByIds(rechnungMapping, cont.ids)) { rechnungen =>
                val fileStoreIds = rechnungen.map(_.mahnungFileStoreIds.map(FileStoreFileId(_))).flatten
                logger.debug(s"Download mahnungen with filestoreRefs:$fileStoreIds")
                downloadAll("Mahnungen_" + System.currentTimeMillis + ".zip", GeneriertMahnung, fileStoreIds)
              }
            }
          }
        }
      } ~
      path("rechnungen" / "aktionen" / "pain_008") {
        //get(list(buchhaltungReadRepository.getRechnungsExports)) ~
        get(download(RechnungExportDaten, "exportFile.xml")) ~
          (put | post) {
            requestInstance { request =>
              entity(as[RechnungenContainer]) { cont =>
                onSuccess(buchhaltungReadRepository.getByIds(rechnungMapping, cont.ids)) { rechnungen =>
                  val xml = generatePain008(rechnungen)
                  val stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))
                  //storeToFileStore(RechnungExportDaten, Some("exportFile"), stream, "exportFile.xml") { (id, metadata) =>
                  //  complete("File uploaded")
                  //}
                  ???
                }
              }
            }
          }
        //val xml = generatePain008()
        //storeToFileStore(RechnungExportDaten, None, new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), "fileToDownload") { (fileId, meta) =>
        //  createRechnungExportPain008("fileToDownload", Seq())
        //}

        //download(RechnungExportDaten, "fileToDownload")
      } ~
      path("rechnungen" / "aktionen" / "verschicken") {
        post {
          requestInstance { request =>
            entity(as[RechnungenContainer]) { cont =>
              verschicken(cont.ids)
            }
          }
        }
      } ~
      path("rechnungen" / "berichte" / "rechnungen") {
        (post)(rechnungBerichte())
      } ~
      path("rechnungen" / "berichte" / "mahnungen") {
        (post)(mahnungBerichte())
      } ~
      path("rechnungen" / rechnungIdPath) { id =>
        get({
          detail(buchhaltungReadRepository.getRechnungDetail(id))
        }) ~
          delete(deleteRechnung(id)) ~
          (put | post)(entity(as[RechnungModify]) { entity => safeRechnung(id, entity) })
      } ~
      path("rechnungen" / rechnungIdPath / "aktionen" / "downloadrechnung") { id =>
        (get)(
          onSuccess(buchhaltungReadRepository.getRechnungDetail(id)) { detail =>
            detail flatMap { rechnung =>
              rechnung.fileStoreId map { fileStoreId =>
                download(GeneriertRechnung, fileStoreId)
              }
            } getOrElse (complete(StatusCodes.BadRequest))
          }
        )
      } ~
      path("rechnungen" / rechnungIdPath / "aktionen" / "download" / Segment) { (id, fileStoreId) =>
        (get)(
          onSuccess(buchhaltungReadRepository.getRechnungDetail(id)) { detail =>
            detail map { rechnung =>
              download(GeneriertMahnung, fileStoreId)
            } getOrElse (complete(StatusCodes.BadRequest))
          }
        )
      } ~
      path("rechnungen" / rechnungIdPath / "aktionen" / "verschicken") { id =>
        (post)(verschicken(id))
      } ~
      path("rechnungen" / rechnungIdPath / "aktionen" / "mahnungverschicken") { id =>
        (post)(mahnungVerschicken(id))
      } ~
      path("rechnungen" / rechnungIdPath / "aktionen" / "bezahlen") { id =>
        (post)(entity(as[RechnungModifyBezahlt]) { entity => bezahlen(id, entity) })
      } ~
      path("rechnungen" / rechnungIdPath / "aktionen" / "stornieren") { id =>
        (post)(stornieren(id))
      } ~
      path("rechnungen" / rechnungIdPath / "berichte" / "rechnung") { id =>
        (post)(rechnungBericht(id))
      } ~
      path("rechnungen" / rechnungIdPath / "berichte" / "mahnung") { id =>
        (post)(mahnungBericht(id))
      }

  def rechnungspositionenRoute(implicit subect: Subject, filter: Option[FilterExpr]) =
    path("rechnungspositionen" ~ exportFormatPath.?) { exportFormat =>
      get(list(buchhaltungReadRepository.getRechnungsPositionen, exportFormat))
    } ~
      path("rechnungspositionen" / rechnungsPositionIdPath) { id =>
        delete(deleteRechnungsPosition(id)) ~
          (put | post)(entity(as[RechnungsPositionModify]) { entity => safeRechnungsPosition(id, entity) })
      } ~
      path("rechnungspositionen" / "aktionen" / "createrechnungen") {
        post {
          entity(as[RechnungsPositionenCreateRechnungen]) { rechnungenCreate =>
            createRechnungen(rechnungenCreate)
          }
        }
      }

  def zahlungsImportsRoute(implicit subect: Subject) =
    path("zahlungsimports") {
      get(list(buchhaltungReadRepository.getZahlungsImports)) ~
        (put | post)(upload { (form, content, fileName) =>
          // read the file once and pass the same content along
          val uploadData = Iterator continually content.read takeWhile (-1 !=) map (_.toByte) toArray

          ZahlungsImportParser.parse(uploadData) match {
            case Success(importResult) =>
              storeToFileStore(ZahlungsImportDaten, None, new ByteArrayInputStream(uploadData), fileName) { (fileId, meta) =>
                createZahlungsImport(fileId, importResult.records)
              }
            case Failure(e) => complete(StatusCodes.BadRequest, s"Die Datei konnte nicht gelesen werden: $e")
          }
        })
    } ~
      path("zahlungsimports" / zahlungsImportIdPath) { id =>
        get(detail(buchhaltungReadRepository.getZahlungsImportDetail(id)))
      } ~
      path("zahlungsimports" / zahlungsImportIdPath / "zahlungseingaenge" / zahlungsEingangIdPath / "aktionen" / "erledigen") { (_, zahlungsEingangId) =>
        post(entity(as[ZahlungsEingangModifyErledigt]) { entity => zahlungsEingangErledigen(entity) })
      } ~
      path("zahlungsimports" / zahlungsImportIdPath / "zahlungseingaenge" / "aktionen" / "automatischerledigen") { id =>
        post(entity(as[Seq[ZahlungsEingangModifyErledigt]]) { entities => zahlungsEingaengeErledigen(entities) })
      }

  private def mailingRoute(implicit subject: Subject): Route =
    path("mailing" / "sendEmailToInvoicesSubscribers") {
      post {
        requestInstance { request =>
          entity(as[RechnungMailRequest]) { rechnungMailRequest =>
            sendEmailsToInvoicesSubscribers(rechnungMailRequest.subject, rechnungMailRequest.body, rechnungMailRequest.ids, rechnungMailRequest.attachInvoice)
          }
        }
      }
    }

  def verschicken(id: RechnungId)(implicit idPersister: Persister[RechnungId, _], subject: Subject) = {
    onSuccess(entityStore ? BuchhaltungCommandHandler.RechnungVerschickenCommand(subject.personId, id)) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Rechnung konnte nicht in den Status 'Verschickt' gesetzt werden")
      case _ =>
        complete("")
    }
  }

  def verschicken(ids: Seq[RechnungId])(implicit idPersister: Persister[RechnungId, _], subject: Subject) = {
    onSuccess(entityStore ? BuchhaltungCommandHandler.RechnungenVerschickenCommand(subject.personId, ids)) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Es konnten keine Rechnungen in den Status 'Verschickt' gesetzt werden")
      case _ =>
        complete("")
    }
  }

  def mahnungVerschicken(id: RechnungId)(implicit idPersister: Persister[RechnungId, _], subject: Subject) = {
    onSuccess(entityStore ? BuchhaltungCommandHandler.RechnungMahnungVerschickenCommand(subject.personId, id)) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Rechnung konnte nicht in den Status 'MahnungVerschickt' gesetzt werden")
      case _ =>
        complete("")
    }
  }

  def bezahlen(id: RechnungId, entity: RechnungModifyBezahlt)(implicit idPersister: Persister[RechnungId, _], subject: Subject) = {
    onSuccess(entityStore ? BuchhaltungCommandHandler.RechnungBezahlenCommand(subject.personId, id, entity)) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Rechnung konnte nicht in den Status 'Bezahlt' gesetzt werden")
      case _ =>
        complete("")
    }
  }

  def stornieren(id: RechnungId)(implicit idPersister: Persister[RechnungId, _], subject: Subject) = {
    onSuccess(entityStore ? BuchhaltungCommandHandler.RechnungStornierenCommand(subject.personId, id)) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Rechnung konnte nicht in den Status 'Storniert' gesetzt werden")
      case _ =>
        complete("")
    }
  }

  def createZahlungsImport(file: String, zahlungsEingaenge: Seq[ZahlungsImportRecordResult])(implicit idPersister: Persister[ZahlungsImportId, _], subject: Subject) = {
    onSuccess((entityStore ? BuchhaltungCommandHandler.ZahlungsImportCreateCommand(subject.personId, file, zahlungsEingaenge))) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Rechnung konnte nicht in den Status 'Bezahlt' gesetzt werden")
      case _ =>
        complete("")
    }
  }

  def zahlungsEingangErledigen(entity: ZahlungsEingangModifyErledigt)(implicit idPersister: Persister[ZahlungsEingangId, _], subject: Subject) = {
    onSuccess((entityStore ? BuchhaltungCommandHandler.ZahlungsEingangErledigenCommand(subject.personId, entity))) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Der Zahlungseingang konnte nicht erledigt werden")
      case _ =>
        complete("")
    }
  }

  def zahlungsEingaengeErledigen(entities: Seq[ZahlungsEingangModifyErledigt])(implicit idPersister: Persister[ZahlungsEingangId, _], subject: Subject) = {
    onSuccess((entityStore ? BuchhaltungCommandHandler.ZahlungsEingaengeErledigenCommand(subject.personId, entities))) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Es konnten nicht alle Zahlungseingänge erledigt werden")
      case _ =>
        complete("")
    }
  }

  def rechnungBericht(id: RechnungId)(implicit idPersister: Persister[ZahlungsEingangId, _], subject: Subject) = {
    implicit val personId = subject.personId
    generateReport[RechnungId](Some(id), generateRechnungReports _)(RechnungId.apply)
  }

  def rechnungBerichte()(implicit idPersister: Persister[ZahlungsEingangId, _], subject: Subject) = {
    implicit val personId = subject.personId
    generateReport[RechnungId](None, generateRechnungReports _)(RechnungId.apply)
  }

  def mahnungBericht(id: RechnungId)(implicit idPersister: Persister[ZahlungsEingangId, _], subject: Subject) = {
    implicit val personId = subject.personId
    generateReport[RechnungId](Some(id), generateMahnungReports _)(RechnungId.apply)
  }

  def mahnungBerichte()(implicit idPersister: Persister[ZahlungsEingangId, _], subject: Subject) = {
    implicit val personId = subject.personId
    generateReport[RechnungId](None, generateMahnungReports _)(RechnungId.apply)
  }

  def createRechnungen(rechnungenCreate: RechnungsPositionenCreateRechnungen)(implicit subject: Subject) = {
    onSuccess((entityStore ? BuchhaltungCommandHandler.CreateRechnungenCommand(subject.personId, rechnungenCreate))) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Es konnten nicht alle Rechnungen für die gegebenen RechnungsPositionen erstellt werden.")
      case _ =>
        complete("")
    }
  }

  def deleteRechnung(rechnungId: RechnungId)(implicit subject: Subject) = {
    onSuccess((entityStore ? BuchhaltungCommandHandler.DeleteRechnungCommand(subject.personId, rechnungId))) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Die Rechnung kann nur gelöscht werden wenn sie im Status Erstellt ist.")
      case _ =>
        complete("")
    }
  }

  def safeRechnung(rechnungId: RechnungId, rechnungModify: RechnungModify)(implicit subject: Subject) = {
    onSuccess((entityStore ? BuchhaltungCommandHandler.SafeRechnungCommand(subject.personId, rechnungId, rechnungModify))) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Die Rechnung kann nur gespeichert werden wenn sie im Status Erstellt ist und keine Rechnungspositionen hat.")
      case _ =>
        complete("")
    }
  }

  def deleteRechnungsPosition(rechnungsPositionId: RechnungsPositionId)(implicit subject: Subject) = {
    onSuccess((entityStore ? BuchhaltungCommandHandler.DeleteRechnungsPositionCommand(subject.personId, rechnungsPositionId))) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Die Rechnungsposition kann nur gelöscht werden wenn sie im Status Offen ist.")
      case _ =>
        complete("")
    }
  }

  def safeRechnungsPosition(rechnungsPositionId: RechnungsPositionId, rechnungsPositionModify: RechnungsPositionModify)(implicit subject: Subject) = {
    onSuccess((entityStore ? BuchhaltungCommandHandler.SafeRechnungsPositionCommand(subject.personId, rechnungsPositionId, rechnungsPositionModify))) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Die Rechnungsposition kann nur gespeichert werden wenn sie im Status Offen ist.")
      case _ =>
        complete("")
    }
  }

  private def sendEmailsToInvoicesSubscribers(emailSubject: String, body: String, ids: Seq[RechnungId], attachInvoice: Boolean)(implicit subject: Subject) = {
    onSuccess((entityStore ? BuchhaltungCommandHandler.SendEmailToInvoicesSubscribersCommand(subject.personId, emailSubject, body, ids, attachInvoice))) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Something went wrong with the mail generation, please check the correctness of the template.")
      case _ =>
        complete("")
    }

  def generatePain008(ids: List[Rechnung])(implicit subect: Subject): String = {
    val NbOfTxs = ids.size.toString

    val kontoDatenProjektWithFuture = stammdatenReadRepository.getKontoDatenProjekt map { maybeKontoDatenProjekt =>
      maybeKontoDatenProjekt match {
        case Some(kdp) => kdp
      }
    }

    val rechnungenWithFutures = ids.map {
      rechnung =>
        stammdatenReadRepository.getKontoDatenKunde(rechnung.kundeId).map { maybeKontoDatenKunde =>
          maybeKontoDatenKunde match {
            case Some(kontoDatenKunde) => (rechnung, kontoDatenKunde)
          }
        }
    }
    val d = Duration(1, SECONDS)

    //sequence will transform from list[Future] to future[list]
    val rechnungen = Await.result(scala.concurrent.Future.sequence(rechnungenWithFutures), d)
    val kontoDatenProjekt = Await.result(kontoDatenProjektWithFuture, d)
    Pain008_003_02_Export.exportPain008_003_02(rechnungen, kontoDatenProjekt, NbOfTxs)
  }
}

class DefaultBuchhaltungRoutes(
  override val dbEvolutionActor: ActorRef,
  override val entityStore: ActorRef,
  override val eventStore: ActorRef,
  override val mailService: ActorRef,
  override val reportSystem: ActorRef,
  override val sysConfig: SystemConfig,
  override val system: ActorSystem,
  override val fileStore: FileStore,
  override val actorRefFactory: ActorRefFactory,
  override val airbrakeNotifier: ActorRef,
  override val jobQueueService: ActorRef
)
  extends BuchhaltungRoutes
  with DefaultBuchhaltungReadRepositoryAsyncComponent
  with DefaultStammdatenReadRepositoryAsyncComponent
