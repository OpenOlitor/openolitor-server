package ch.openolitor.arbeitseinsatz

import akka.http.scaladsl.model.{ Multipart, StatusCodes }
import akka.http.scaladsl.testkit.WSProbe
import ch.openolitor.arbeitseinsatz.models._
import ch.openolitor.core.{ BaseRoutesWithDBSpec, SpecSubjects }
import ch.openolitor.core.db.WithWriteRepositories
import ch.openolitor.core.reporting.AsyncReportServiceResult
import ch.openolitor.stammdaten.{ MockStammdatenRoutes, StammdatenJsonProtocol, StammdatenRouteServiceInteractions }
import org.joda.time.DateTime
import org.odftoolkit.odfdom.doc.OdfDocument

import scala.concurrent.Await

class ArbeitseinsatzRoutesAdminSetupSpec extends BaseRoutesWithDBSpec with SpecSubjects with StammdatenRouteServiceInteractions with StammdatenJsonProtocol with ArbeitseinsatzJsonProtocol with WithWriteRepositories {
  sequential

  import ch.openolitor.util.Fixtures._

  protected val stammdatenRouteService = new MockStammdatenRoutes(sysConfig, system)
  protected val arbeitseinsatzRouteService = new MockArbeitseinsatzRoutes(sysConfig, system)

  implicit val subject = adminSubject

  override def beforeAll() = {
    super.beforeAll()

    createTrinityOfAbos()
  }

  "ArbeitseinsatzRoutes for Arbeitseinsaetze" should {
    val arbeitskategorieBeschreibung = "Dein Einsatz zählt"
    "create Arbeitskategorie" in {
      val create = ArbeitskategorieModify(arbeitskategorieBeschreibung)

      Post("/arbeitskategorien", create) ~> arbeitseinsatzRouteService.arbeitseinsatzRoute ~> check {
        expectDBEvents(1) { (creations, _, _, _) =>
          oneEventMatches[Arbeitskategorie](creations)(_.beschreibung === arbeitskategorieBeschreibung)
        }
      }
    }

    "get Arbeitskategorie" in {
      Get("/arbeitskategorien") ~> arbeitseinsatzRouteService.arbeitseinsatzRoute ~> check {
        val result = responseAs[List[Arbeitskategorie]]

        result.size === 1
        result.head.beschreibung === arbeitskategorieBeschreibung
      }
    }

    "modify Arbeitskategorie" in {
      val arbeitskategorie = Await.result(arbeitseinsatzRouteService.arbeitseinsatzReadRepository.getArbeitskategorien, defaultTimeout).head
      val modify = ArbeitskategorieModify(arbeitskategorieBeschreibung + "!")

      Post(s"/arbeitskategorien/${arbeitskategorie.id.id}", modify) ~> arbeitseinsatzRouteService.arbeitseinsatzRoute ~> check {
        expectDBEvents(1) { (_, modifications, _, _) =>
          oneEventMatches[Arbeitskategorie](modifications)(_.beschreibung === arbeitskategorieBeschreibung + "!")
        }
      }
    }

    "create Arbeitsangebot" in {
      val create = ArbeitsangebotModify(None, "Das Arbeitsangebot", Some("Ein richtig gutes Angebot"), Some("Bern"), DateTime.now().plusDays(1), DateTime.now().plusDays(1).plusHours(4), Set(ArbeitskategorieBez(arbeitskategorieBeschreibung + "!")), Some(8), true, Some(4), Bereit)

      Post("/arbeitsangebote", create) ~> arbeitseinsatzRouteService.arbeitseinsatzRoute ~> check {
        expectDBEvents(1) { (creations, _, _, _) =>
          oneEventMatches[Arbeitsangebot](creations)(_.ort === Some("Bern"))
        }
      }
    }

    "get Arbeitsangebot" in {
      Get("/arbeitsangebote") ~> arbeitseinsatzRouteService.arbeitseinsatzRoute ~> check {
        val result = responseAs[List[Arbeitsangebot]]

        result.size === 1
        result.head.ort === Some("Bern")
      }
    }

    "add Person to Arbeitsangebot" in {
      val arbeitsangebot = Await.result(arbeitseinsatzRouteService.arbeitseinsatzReadRepository.getArbeitsangebote, defaultTimeout).head
      val kunde = Await.result(stammdatenRouteService.stammdatenReadRepository.getKunden, defaultTimeout).filter(_.bezeichnung.contains(kundeCreateHaseFritz.ansprechpersonen.head.name)).head
      val person = Await.result(stammdatenRouteService.stammdatenReadRepository.getPersonen(kunde.id), defaultTimeout).head
      val abo = Await.result(stammdatenRouteService.stammdatenReadRepository.getKundeDetail(kunde.id), defaultTimeout).get.abos.head

      val create = ArbeitseinsatzModify(arbeitsangebot.id, arbeitsangebot.zeitVon, arbeitsangebot.zeitBis, Some(4), kunde.id, Some(person.id), Some(abo.id), 1, None, false)

      Post(s"/arbeitsangebote/${arbeitsangebot.id.id}/arbeitseinsaetze", create) ~> arbeitseinsatzRouteService.arbeitseinsatzRoute ~> check {
        expectDBEvents(2) { (creations, modifications, _, _) =>
          oneEventMatches[Arbeitseinsatz](creations)(_.kundeBezeichnung === kunde.bezeichnung)
          oneEventMatches[Arbeitsangebot](modifications)(_.anzahlEingeschriebene === 1)
        }
      }
    }

    "Arbeitseinsatz Brief" in {
      val arbeitseinsatz = Await.result(arbeitseinsatzRouteService.arbeitseinsatzReadRepository.getArbeitseinsaetze, defaultTimeout).head

      val formData = Multipart.FormData(
        Multipart.FormData.BodyPart.Strict("report", "true"),
        Multipart.FormData.BodyPart.Strict("pdfGenerieren", "false"),
        Multipart.FormData.BodyPart.Strict("pdfDownloaden", "true"),
        Multipart.FormData.BodyPart.Strict("pdfMerge", "zip"),
        Multipart.FormData.BodyPart.Strict("ids", s"${arbeitseinsatz.id.id}")
      )

      val wsClient = WSProbe()

      withWsProbeFakeLogin(wsClient) { wsClient =>
        Post(s"/arbeitseinsaetze/berichte/arbeitseinsatzbrief", formData) ~> arbeitseinsatzRouteService.arbeitseinsatzRoute ~> check {
          status === StatusCodes.OK

          val reportServiceResult = responseAs[AsyncReportServiceResult]

          reportServiceResult.hasErrors === false

          val file = awaitFileViaWebsocket(wsClient, reportServiceResult)

          val odt = OdfDocument.loadDocument(file)
          val contentAsString = odt.getContentRoot.getChildNodes.toString
          contentAsString must contain("Guten Tag")
        }
      }
    }
  }
}
