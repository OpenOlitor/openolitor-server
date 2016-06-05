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
import spray.http.MediaTypes._
import spray.httpx.marshalling.ToResponseMarshallable._
import spray.httpx.SprayJsonSupport._
import spray.routing.Directive._
import spray.json._
import spray.json.DefaultJsonProtocol._
import ch.openolitor.core._
import ch.openolitor.core.domain._
import ch.openolitor.core.db._
import spray.httpx.unmarshalling.Unmarshaller
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util._
import java.util.UUID
import akka.pattern.ask
import scala.concurrent.duration._
import akka.util.Timeout
import ch.openolitor.stammdaten.models._
import ch.openolitor.core.models._
import spray.httpx.marshalling._
import spray.httpx.unmarshalling._
import scala.concurrent.Future
import ch.openolitor.core.Macros._
import ch.openolitor.buchhaltung.eventsourcing.BuchhaltungEventStoreSerializer
import stamina.Persister
import ch.openolitor.buchhaltung.models._
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.filestore._
import akka.actor._
import scala.io.Source
import ch.openolitor.buchhaltung.zahlungsimport.ZahlungsImportParser
import ch.openolitor.buchhaltung.zahlungsimport.ZahlungsImportRecordResult
import ch.openolitor.core.security.Subject
import ch.openolitor.core.reporting._
import ch.openolitor.core.reporting.ReportSystem._
import ch.openolitor.util.InputStreamUtil._
import java.io.InputStream
import java.util.zip.ZipInputStream

trait BuchhaltungRoutes extends HttpService with ActorReferences
    with AsyncConnectionPoolContextAware with SprayDeserializers with DefaultRouteService with LazyLogging
    with BuchhaltungJsonProtocol
    with BuchhaltungEventStoreSerializer
    with RechnungReportService
    with ReportJsonProtocol {
  self: BuchhaltungReadRepositoryComponent with FileStoreComponent =>

  implicit val rechnungIdPath = long2BaseIdPathMatcher(RechnungId.apply)
  implicit val zahlungsImportIdPath = long2BaseIdPathMatcher(ZahlungsImportId.apply)
  implicit val zahlungsEingangIdPath = long2BaseIdPathMatcher(ZahlungsEingangId.apply)

  import EntityStore._

  def buchhaltungRoute(implicit subect: Subject) = rechnungenRoute ~ zahlungsImportsRoute

  def rechnungenRoute(implicit subect: Subject) =
    path("rechnungen") {
      get(list(buchhaltungReadRepository.getRechnungen)) ~
        post(create[RechnungModify, RechnungId](RechnungId.apply _))
    } ~
      path("rechnungen" / rechnungIdPath) { id =>
        get(detail(buchhaltungReadRepository.getRechnungDetail(id))) ~
          delete(remove(id))
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
      }

  def zahlungsImportsRoute(implicit subect: Subject) =
    path("zahlungsimports") {
      get(list(buchhaltungReadRepository.getZahlungsImports)) ~
        (put | post)(upload { (form, content, fileName) =>
          // parse
          ZahlungsImportParser.parse(Source.fromInputStream(content).getLines) match {
            case Success(importResult) =>
              storeToFileStore(ZahlungsImportDaten, None, content, fileName) { (fileId, meta) =>
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

  def verschicken(id: RechnungId)(implicit idPersister: Persister[RechnungId, _], subject: Subject) = {
    onSuccess(entityStore ? BuchhaltungCommandHandler.RechnungVerschickenCommand(subject.personId, id)) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Could not transit to status Verschickt")
      case _ =>
        complete("")
    }
  }

  def mahnungVerschicken(id: RechnungId)(implicit idPersister: Persister[RechnungId, _], subject: Subject) = {
    onSuccess(entityStore ? BuchhaltungCommandHandler.RechnungMahnungVerschickenCommand(subject.personId, id)) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Could not transit to status MahnungVerschickt")
      case _ =>
        complete("")
    }
  }

  def bezahlen(id: RechnungId, entity: RechnungModifyBezahlt)(implicit idPersister: Persister[RechnungId, _], subject: Subject) = {
    onSuccess(entityStore ? BuchhaltungCommandHandler.RechnungBezahlenCommand(subject.personId, id, entity)) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Could not transit to status Bezahlt")
      case _ =>
        complete("")
    }
  }

  def stornieren(id: RechnungId)(implicit idPersister: Persister[RechnungId, _], subject: Subject) = {
    onSuccess(entityStore ? BuchhaltungCommandHandler.RechnungStornierenCommand(subject.personId, id)) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Could not transit to status Storniert")
      case _ =>
        complete("")
    }
  }

  def createZahlungsImport(file: String, zahlungsEingaenge: Seq[ZahlungsImportRecordResult])(implicit idPersister: Persister[ZahlungsImportId, _], subject: Subject) = {
    onSuccess((entityStore ? BuchhaltungCommandHandler.ZahlungsImportCreateCommand(subject.personId, file, zahlungsEingaenge))) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Could not transit to status Bezahlt")
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
    uploadOpt("vorlage") { formData => file =>
      //use custom or default template whether content was delivered or not
      (for {
        vorlage <- loadVorlage(file)
        pdfGenerieren <- Try(formData.fields.collectFirst {
          case b @ BodyPart(entity, headers) if b.name == Some("pdfGenerieren") =>
            entity.asString.toBoolean
        }.getOrElse(false))
        pdfAblegen <- Try(pdfGenerieren && formData.fields.collectFirst {
          case b @ BodyPart(entity, headers) if b.name == Some("pdfAblegen") =>
            entity.asString.toBoolean
        }.getOrElse(false))
        downloadFile <- Try(!pdfAblegen || formData.fields.collectFirst {
          case b @ BodyPart(entity, headers) if b.name == Some("pdfDownloaden") =>
            entity.asString.toBoolean
        }.getOrElse(true))
      } yield {
        val config = ReportConfig[RechnungId](Seq(id), vorlage, pdfGenerieren, pdfAblegen)

        onSuccess(generateRechnungReports(config)) {
          case Left(serviceError) =>
            complete(StatusCodes.BadRequest, s"Der Bericht konnte nicht erzeugt werden:$serviceError")
          case Right(result) if result.hasErrors =>
            val errorString = result.validationErrors.map(_.message).mkString(",")
            complete(StatusCodes.BadRequest, s"Der Bericht konnte nicht erzeugt werden:${errorString}")
          case Right(result) =>
            result.result match {
              case SingleReportResult(_, Left(ReportError(error))) => complete(StatusCodes.BadRequest, s"Der Bericht konnte nicht erzeugt werden:$error")
              case SingleReportResult(_, Right(DocumentReportResult(result, name))) =>
                respondWithHeader(HttpHeaders.`Content-Disposition`("attachment", Map(("filename", name)))) {
                  respondWithMediaType(MediaTypes.`application/vnd.oasis.opendocument.text`) {
                    stream(result)
                  }
                }
              case SingleReportResult(_, Right(PdfReportResult(result, name))) =>
                respondWithHeader(HttpHeaders.`Content-Disposition`("attachment", Map(("filename", name)))) {
                  respondWithMediaType(MediaTypes.`application/pdf`) {
                    stream(result)
                  }
                }
              case SingleReportResult(_, Right(StoredPdfReportResult(fileType, id))) if downloadFile => download(fileType, id.id)
              case SingleReportResult(_, Right(StoredPdfReportResult(fileType, id))) => complete(id.id)
              case ZipReportResult(_, errors, zip) if !zip.isDefined =>
                val errorString: String = errors.map(_.error).mkString("\n")
                complete(StatusCodes.BadRequest, errorString)
              case ZipReportResult(_, _, zip) if zip.isDefined =>
                //TODO: stream zip
                //stream(new ZipInputStream(zip.get))
                ???
              case x =>
                logger.error(s"Received unexpected result:$x")
                complete(StatusCodes.BadRequest, s"Der Bericht konnte nicht erzeugt werden")
            }
        }
      }) match {
        case Success(result) => result
        case Failure(error) => complete(StatusCodes.BadRequest, s"Der Bericht konnte nicht erzeugt werden:${error}")
      }
    }
  }

  def loadVorlage(file: Option[(InputStream, String)]): Try[BerichtsVorlage] = {
    file map {
      case (is, name) => is.toByteArray.map(result => EinzelBerichtsVorlage(result))
    } getOrElse Success(StandardBerichtsVorlage)
  }
}

class DefaultBuchhaltungRoutes(
  override val entityStore: ActorRef,
  override val eventStore: ActorRef,
  override val reportSystem: ActorRef,
  override val sysConfig: SystemConfig,
  override val system: ActorSystem,
  override val fileStore: FileStore,
  override val actorRefFactory: ActorRefFactory
)
    extends BuchhaltungRoutes
    with DefaultBuchhaltungReadRepositoryComponent
