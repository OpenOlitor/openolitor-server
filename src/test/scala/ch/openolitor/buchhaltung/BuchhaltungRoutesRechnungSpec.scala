package ch.openolitor.buchhaltung

import akka.http.scaladsl.model.StatusCodes
import ch.openolitor.buchhaltung.models.{ Rechnung, RechnungenContainer, RechnungsPosition, RechnungsPositionenCreateRechnungen }
import ch.openolitor.core.{ BaseRoutesWithDBSpec, SpecSubjects }
import ch.openolitor.core.db.WithWriteRepositories
import ch.openolitor.core.Macros.copyTo
import ch.openolitor.stammdaten.{ MockStammdatenRoutes, StammdatenJsonProtocol, StammdatenRouteServiceInteractions }
import ch.openolitor.stammdaten.models._
import ch.openolitor.util.parsing.{ FilterExpr, GeschaeftsjahrFilter, QueryFilter }

import scala.concurrent.Await

class BuchhaltungRoutesRechnungSpec extends BaseRoutesWithDBSpec with SpecSubjects with StammdatenRouteServiceInteractions with StammdatenJsonProtocol with BuchhaltungJsonProtocol with WithWriteRepositories {
  sequential

  import ch.openolitor.util.Fixtures._

  private val service = new MockStammdatenRoutes(sysConfig, system)
  private val buchhaltungRouteService = new MockBuchhaltungRoutes(sysConfig, system)

  implicit val subject = adminSubject

  private val validIban = "AD12000120302003591001000000000001"

  override def beforeAll() = {
    super.beforeAll()

    setupAbo()
  }

  "BuchhaltungRoutes for Rechnungen" should {
    "create manual Rechnung" in {
      val aboRechnungsPositionBisAnzahlLieferungenCreate = AboRechnungsPositionBisAnzahlLieferungenCreate(Seq(AboId(1)), "The Title", 6, None, CHF)

      Post("/abos/aktionen/anzahllieferungenrechnungspositionen", aboRechnungsPositionBisAnzahlLieferungenCreate) ~> service.stammdatenRoute ~> check {
        expectDBEvents(1) { (creations, _, _, _) =>
          oneEventMatches[RechnungsPosition](creations)(_.betrag === abotypVegi.preis * 6)
        }
      }
    }

    "get Rechnungsposition" in {
      Get("/rechnungspositionen") ~> buchhaltungRouteService.buchhaltungRoute ~> check {
        val result = responseAs[List[RechnungsPosition]]
        result.size === 1

        result.head.betrag === abotypVegi.preis * 6
      }
    }

    "create Rechnungen" in {
      implicit val filter: Option[FilterExpr] = None
      implicit val gjFilter: Option[GeschaeftsjahrFilter] = None
      implicit val queryString: Option[QueryFilter] = None
      val rechnungsPositionen = Await.result(buchhaltungRouteService.buchhaltungReadRepository.getRechnungsPositionen, defaultTimeout)

      rechnungsPositionen.size === 1

      val rechnungenCreate = RechnungsPositionenCreateRechnungen(rechnungsPositionen.map(_.id), "The Invoice for The Title", now, now.plusMonths(1))

      Post("/rechnungspositionen/aktionen/createrechnungen", rechnungenCreate) ~> buchhaltungRouteService.buchhaltungRoute ~> check {
        expectDBEvents(3) { (creations, modifications, _, _) =>
          creations.size === 1
          modifications.size === 2
          oneEventMatches[Rechnung](creations)(_.betrag === abotypVegi.preis * 6)
        }
      }
    }

    "get Rechnungen" in {
      Get("/rechnungen") ~> buchhaltungRouteService.buchhaltungRoute ~> check {
        val result = responseAs[List[Rechnung]]
        result.size === 1

        result.head.betrag === abotypVegi.preis * 6
      }
    }

    "modify Projekt Kontodaten accordingly" in {
      val kontoDaten = Await.result(service.stammdatenReadRepository.getKontoDatenProjekt, defaultTimeout).get
      val kontoDatenModify = copyTo[KontoDaten, KontoDatenModify](kontoDaten).copy(iban = Some(validIban), creditorIdentifier = Some("Street Creditor"))

      Post(s"/kontodaten/${kontoDaten.id.id}", kontoDatenModify) ~> service.stammdatenRoute ~> check {
        status === StatusCodes.Accepted

        expectDBEvents(1) { (_, modifications, _, _) =>
          oneEventMatches[KontoDaten](modifications)(_.iban === Some(validIban))
        }
      }
    }

    "modify Kunde Kontodaten accordingly" in {
      val kunden = Await.result(service.stammdatenReadRepository.getKunden, defaultTimeout)
      val kunde = kunden.find(_.bezeichnung.contains(kundeCreateUntertorOski.ansprechpersonen.head.name)).get
      val kontoDaten = Await.result(service.stammdatenReadRepository.getKontoDatenKunde(kunde.id), defaultTimeout).get
      val kontoDatenModify = copyTo[KontoDaten, KontoDatenModify](kontoDaten).copy(iban = Some(validIban), nameAccountHolder = Some("Oski"))

      Post(s"/kontodaten/${kontoDaten.id.id}", kontoDatenModify) ~> service.stammdatenRoute ~> check {
        status === StatusCodes.Accepted

        expectDBEvents(1) { (_, modifications, _, _) =>
          oneEventMatches[KontoDaten](modifications)(_.iban === Some(validIban))
        }
      }
    }

    "pain008 v02" in {
      pain008("02")
    }

    "pain008 v07" in {
      pain008("07")
    }
  }

  private def pain008(version: String) = {
    implicit val filter: Option[FilterExpr] = None
    implicit val gjFilter: Option[GeschaeftsjahrFilter] = None
    implicit val queryString: Option[QueryFilter] = None
    val rechnungen = Await.result(buchhaltungRouteService.buchhaltungReadRepository.getRechnungen, defaultTimeout)
    val rechnungsIds = RechnungenContainer(rechnungen.map(_.id))

    Post(s"/rechnungen/aktionen/pain_008_001_$version", rechnungsIds) ~> buchhaltungRouteService.buchhaltungRoute ~> check {
      status === StatusCodes.OK
      val result = responseAs[String]

      result must contain(validIban)
    }
  }

  private def setupAbo() = {
    implicit val adminPersonId = adminSubject.personId

    insertEntity[Depot, DepotId](depotWwg)

    insertEntity[Abotyp, AbotypId](abotypVegi)

    insertEntity[Vertrieb, VertriebId](vertriebDonnerstag)

    createSimpleVertriebVertriebsart(service)

    createSimpleAbo(service)
  }
}