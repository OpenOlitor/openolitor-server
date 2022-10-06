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

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Directives.{ parameters => httpParameters }
import ch.openolitor.core._
import ch.openolitor.core.domain._
import ch.openolitor.core.db._

import scala.util._
import akka.pattern.ask
import ch.openolitor.buchhaltung.eventsourcing.BuchhaltungEventStoreSerializer
import stamina.Persister
import ch.openolitor.buchhaltung.models._
import ch.openolitor.stammdaten.models._
import ch.openolitor.core.filestore._
import akka.actor._
import akka.http.scaladsl.server.Route
import ch.openolitor.buchhaltung.zahlungsimport.ZahlungsImportParser
import ch.openolitor.buchhaltung.zahlungsimport.ZahlungsImportRecordResult
import ch.openolitor.core.security.Subject
import ch.openolitor.stammdaten.repositories.StammdatenReadRepositoryAsyncComponent
import ch.openolitor.stammdaten.repositories.DefaultStammdatenReadRepositoryAsyncComponent
import ch.openolitor.buchhaltung.reporting.RechnungReportService
import ch.openolitor.util.parsing.{ FilterExpr, GeschaeftsjahrFilter, QueryFilter, UriQueryFilterParser, UriQueryParamFilterParser, UriQueryParamGeschaeftsjahrParser }
import ch.openolitor.buchhaltung.repositories.DefaultBuchhaltungReadRepositoryAsyncComponent
import ch.openolitor.buchhaltung.repositories.BuchhaltungReadRepositoryAsyncComponent
import ch.openolitor.buchhaltung.reporting.MahnungReportService

import java.io.ByteArrayInputStream
import scala.concurrent.duration.SECONDS
import scala.concurrent.duration.Duration
import ch.openolitor.buchhaltung.rechnungsexport.iso20022._

import scala.concurrent.{ Await, ExecutionContext, Future }

trait BuchhaltungRoutes
  extends BaseRouteService
  with ActorReferences
  with AsyncConnectionPoolContextAware
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
  implicit val zahlungsExportIdPath = long2BaseIdPathMatcher(ZahlungsExportId.apply)

  import EntityStore._

  def buchhaltungRoute(implicit subect: Subject): Route =
    httpParameters("f".?, "g".?, "q".?) { (f, g, q) =>
      implicit val filter = f flatMap { filterString =>
        UriQueryParamFilterParser.parse(filterString)
      }
      implicit val datumsFilter = g flatMap { geschaeftsjahrString =>
        UriQueryParamGeschaeftsjahrParser.parse(geschaeftsjahrString)
      }
      implicit val queryFilter = q flatMap { queryFilter =>
        UriQueryFilterParser.parse(queryFilter)
      }

      rechnungenRoute ~ rechnungspositionenRoute ~ zahlungsImportsRoute ~ mailingRoute ~ zahlungsExportsRoute
    }

  private def pain008Route(version: String)(implicit subect: Subject): Route = post {
    extractRequest { _ =>
      entity(as[RechnungenContainer]) { cont =>
        onSuccess(buchhaltungReadRepository.getByIds(rechnungMapping, cont.ids)) { rechnungen =>
          generatePain008(rechnungen, version) match {
            case Right(xmlData) => {
              val bytes = xmlData.getBytes(java.nio.charset.StandardCharsets.UTF_8)
              storeToFileStore(ZahlungsExportDaten, None, new ByteArrayInputStream(bytes), s"pain_008_001_$version") { (fileId, _) =>
                createZahlungExport(fileId, rechnungen, xmlData)
              }
            }
            case Left(errorMessage) => {
              logger.debug(s"Some data needs to be introduce in the system before creating the pain_008_001_$version : $errorMessage")
              complete(StatusCodes.BadRequest, s"Some data needs to be introduce in the system before creating the pain_008_001_$version: $errorMessage")
            }
          }
        }
      }
    }
  }

  def rechnungenRoute(implicit subect: Subject, filter: Option[FilterExpr], gjFilter: Option[GeschaeftsjahrFilter], queryString: Option[QueryFilter]) =
    path("rechnungen" ~ exportFormatPath.?) { exportFormat =>
      get(list(buchhaltungReadRepository.getRechnungen, exportFormat)) ~
        post(create[RechnungCreateFromRechnungsPositionen, RechnungId](RechnungId.apply _))
    } ~
      path("rechnungen" / "aktionen" / "downloadrechnungen") {
        post {
          extractRequest { _ =>
            entity(as[RechnungenDownloadContainer]) { cont =>
              onSuccess(buchhaltungReadRepository.getByIds(rechnungMapping, cont.ids)) { rechnungen =>
                val fileStoreIds = rechnungen.map(_.fileStoreId.map(FileStoreFileId(_))).flatten
                logger.debug(s"Download rechnungen with filestoreRefs:$fileStoreIds")
                downloadAll(
                  "Rechnungen_" + System.currentTimeMillis,
                  GeneriertRechnung,
                  fileStoreIds,
                  if (cont.pdfMerge.equals("pdfMerge")) true else false
                )
              }
            }
          }
        }
      } ~
      path("rechnungen" / "aktionen" / "downloadmahnungen") {
        post {
          extractRequest { _ =>
            entity(as[RechnungenDownloadContainer]) { cont =>
              onSuccess(buchhaltungReadRepository.getByIds(rechnungMapping, cont.ids)) { rechnungen =>
                val fileStoreIds = rechnungen.map(_.mahnungFileStoreIds.map(FileStoreFileId(_))).flatten
                logger.debug(s"Download mahnungen with filestoreRefs:$fileStoreIds")
                downloadAll(
                  "Mahnungen_" + System.currentTimeMillis,
                  GeneriertMahnung,
                  fileStoreIds,
                  if (cont.pdfMerge.equals("pdfMerge")) true else false
                )
              }
            }
          }
        }
      } ~
      path("rechnungen" / "aktionen" / "pain_008_001_02") {
        pain008Route("02")
      } ~
      path("rechnungen" / "aktionen" / "pain_008_001_07") {
        pain008Route("07")
      } ~
      path("rechnungen" / "aktionen" / "verschicken") {
        post {
          extractRequest { _ =>
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

  def rechnungspositionenRoute(implicit subect: Subject, filter: Option[FilterExpr], queryString: Option[QueryFilter]) =
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

  def zahlungsImportsRoute(implicit subect: Subject, filter: Option[FilterExpr], queryString: Option[QueryFilter]) =
    path("zahlungsimports") {
      get(list(buchhaltungReadRepository.getZahlungsImports)) ~
        (put | post) {
          upload() { (content, fileName) =>
            // read the file once and pass the same content along
            Future { (content.readAllBytes(), fileName) }
          } { (uploadData, fileName) =>
            ZahlungsImportParser.parse(uploadData) match {
              case Success(importResult) =>
                storeToFileStore(ZahlungsImportDaten, None, new ByteArrayInputStream(uploadData), fileName) { (fileId, meta) =>
                  createZahlungsImport(fileId, importResult.records)
                }
              case Failure(e) => complete(StatusCodes.BadRequest, s"Die Datei konnte nicht gelesen werden: $e")
            }
          }
        }
    } ~
      path("zahlungsimports" / zahlungsImportIdPath) { id =>
        get(detail(buchhaltungReadRepository.getZahlungsImportDetail(id)))
      } ~
      path("zahlungsimports" / zahlungsImportIdPath / "zahlungseingaenge" / zahlungsEingangIdPath / "aktionen" / "erledigen") { (_, _) =>
        post(entity(as[ZahlungsEingangModifyErledigt]) { entity => zahlungsEingangErledigen(entity) })
      } ~
      path("zahlungsimports" / zahlungsImportIdPath / "zahlungseingaenge" / zahlungsEingangIdPath / "aktionen" / "ignore") { (_, _) =>
        post(entity(as[ZahlungsEingangModifyErledigt]) { entity => zahlungsEingangIgnore(entity) })
      } ~
      path("zahlungsimports" / zahlungsImportIdPath / "zahlungseingaenge" / "aktionen" / "automatischerledigen") { _ =>
        post(entity(as[Seq[ZahlungsEingangModifyErledigt]]) { entities => zahlungsEingaengeErledigen(entities) })
      }

  private def mailingRoute(implicit subject: Subject): Route =
    path("mailing" / "sendEmailToInvoicesSubscribers") {
      post {
        extractRequest { request =>
          entity(as[RechnungMailRequest]) { rechnungMailRequest =>
            sendEmailsToInvoicesSubscribers(rechnungMailRequest.subject, rechnungMailRequest.body, rechnungMailRequest.replyTo, rechnungMailRequest.ids, rechnungMailRequest.attachInvoice)
          }
        }
      }
    }
  def zahlungsExportsRoute(implicit subect: Subject) =
    path("zahlungsexports") {
      get(list(buchhaltungReadRepository.getZahlungsExports))
    } ~
      path("zahlungsexports" / zahlungsExportIdPath) { id =>
        get(detail(buchhaltungReadRepository.getZahlungsExportDetail(id))) ~
          (put | post)(update[ZahlungsExportCreate, ZahlungsExportId](id))
      } ~
      path("zahlungsexports" / zahlungsExportIdPath / "download") { id =>
        get {
          onSuccess(buchhaltungReadRepository.getZahlungsExportDetail(id)) {
            case Some(zahlungExport) =>
              download(ZahlungsExportDaten, zahlungExport.fileName)
            case None =>
              complete(StatusCodes.NotFound, s"zahlung export nicht gefunden: $id")
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

  def zahlungsEingangIgnore(entity: ZahlungsEingangModifyErledigt)(implicit idPersister: Persister[ZahlungsEingangId, _], subject: Subject) = {
    onSuccess((entityStore ? BuchhaltungCommandHandler.ZahlungsEingangIgnoreCommand(subject.personId, entity))) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Der Zahlungseingang konnte nicht erledigt werden")
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

  def createZahlungExport(file: String, rechnungen: List[Rechnung], fileContent: String)(implicit subject: Subject) = {
    onSuccess(entityStore ? BuchhaltungCommandHandler.ZahlungsExportCreateCommand(subject.personId, rechnungen, file)) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"The file could not be exported. Make sure all the invoices have an Iban and a account holder name. The CSA needs also to have a valid Iban and Creditor Identifier")
      case _ => complete(fileContent)
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

  private def sendEmailsToInvoicesSubscribers(emailSubject: String, body: String, replyTo: Option[String], ids: Seq[RechnungId], attachInvoice: Boolean)(implicit subject: Subject) = {
    onSuccess(entityStore ? BuchhaltungCommandHandler.SendEmailToInvoicesSubscribersCommand(subject.personId, emailSubject, body, replyTo, ids, attachInvoice)) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Something went wrong with the mail generation, please check the correctness of the template.")
      case _ =>
        complete("")
    }
  }

  def generatePain008(ids: List[Rechnung], version: String = "02")(implicit subect: Subject): Either[String, String] = {
    val NbOfTxs = ids.size.toString

    val rechnungenWithFutures: Future[List[(Rechnung, KontoDaten)]] = Future.sequence(ids.map { rechnung =>
      stammdatenReadRepository.getKontoDatenKunde(rechnung.kundeId).map { k => (rechnung, k.get) }
    })
    val d = Duration(1, SECONDS)

    val rechnungen: List[(Rechnung, KontoDaten)] = Await.result(rechnungenWithFutures, d)
    val kontoDatenProjekt: KontoDaten = Await.result(stammdatenReadRepository.getKontoDatenProjekt, d).get
    val projekt: Projekt = Await.result(stammdatenReadRepository.getProjekt, d).get

    (kontoDatenProjekt.iban, kontoDatenProjekt.creditorIdentifier) match {
      case (Some(iban), Some(_)) if iban.isBlank => Left(s"The iban is not defined for the project")
      case (Some(_), Some(id)) if id.isBlank     => Left("The creditorIdentifier is not defined for the project ")
      case (None, Some(_))                       => Left(s"The iban is not defined for the project")
      case (Some(_), None)                       => Left("The creditorIdentifier is not defined for the project ")
      case (None, None)                          => Left("Neither the creditorIdentifier nor the iban is defined for the project")
      case _ =>
        val emptyIbanList = checkEmptyIban(rechnungen)
        if (emptyIbanList.isEmpty) {
          version match {
            case "02" =>
              val xmlText = Pain008_001_02_Export.exportPain008_001_02(rechnungen, kontoDatenProjekt, NbOfTxs, projekt)
              Right(xmlText)
            case "07" =>
              val xmlText = Pain008_001_07_Export.exportPain008_001_07(rechnungen, kontoDatenProjekt, NbOfTxs, projekt)
              Right(xmlText)
            case v =>
              Left(s"Unsupported pain008 version: $v")
          }
        } else {
          val decoratedEmptyList = emptyIbanList.mkString(" ")
          Left(s"The iban or name account holder is not defined for the user: $decoratedEmptyList")
        }
    }
  }

  def checkEmptyIban(rechnungen: List[(Rechnung, KontoDaten)])(implicit subect: Subject): List[KundeId] = {
    rechnungen flatMap { rechnung =>
      (rechnung._2.iban, rechnung._2.nameAccountHolder) match {
        case (None, _)          => Some(rechnung._1.kundeId)
        case (_, None)          => Some(rechnung._1.kundeId)
        case (Some(_), Some(_)) => None
      }
    }
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
  override val airbrakeNotifier: ActorRef,
  override val jobQueueService: ActorRef
)
  extends BuchhaltungRoutes
  with DefaultBuchhaltungReadRepositoryAsyncComponent
  with DefaultStammdatenReadRepositoryAsyncComponent
  with DefaultFileStoreComponent {
  override implicit protected val executionContext: ExecutionContext = system.dispatcher
}
