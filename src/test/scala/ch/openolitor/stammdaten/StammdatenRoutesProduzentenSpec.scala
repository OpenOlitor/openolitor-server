package ch.openolitor.stammdaten

import org.apache.pekko.http.scaladsl.model.{ Multipart, StatusCodes }
import org.apache.pekko.http.scaladsl.testkit.WSProbe
import ch.openolitor.core.{ BaseRoutesWithDBSpec, SpecSubjects }
import ch.openolitor.core.reporting.AsyncReportServiceResult
import ch.openolitor.core.security.Subject
import ch.openolitor.core.Macros.copyTo
import ch.openolitor.stammdaten.models._
import org.odftoolkit.odfdom.doc.OdfDocument

import scala.concurrent.Await

class StammdatenRoutesProduzentenSpec extends BaseRoutesWithDBSpec with SpecSubjects with StammdatenRouteServiceInteractions with StammdatenJsonProtocol {
  sequential

  import ch.openolitor.util.Fixtures._

  protected val stammdatenRouteService = new MockStammdatenRoutes(sysConfig, system)

  implicit val subject: Subject = adminSubject

  "StammdatenRoutes for Produzenten and Produkte" should {
    "create Produzent" in {
      val create = ProduzentModify("Prod", Some("The"), "PRD", Some("WWG"), Some("2"), None, "3011", "Bern", None, "prod@example.com", None, None, None, Some("The Bank"), true, Some(2.3), Some("CHE-123"), true)

      Post("/produzenten", create) ~> stammdatenRouteService.stammdatenRoute ~> check {
        expectDBEvents(1) { (creations, _, _, _) =>
          oneEventMatches[Produzent](creations)(_.name === "Prod")
        }
      }
    }

    "get Produzent" in {
      Get("/produzenten") ~> stammdatenRouteService.stammdatenRoute ~> check {
        val result = responseAs[List[Produzent]]

        result.size === 1
        result.head.email === "prod@example.com"
      }
    }

    "modify Produzen" in {
      val produzent = Await.result(stammdatenRouteService.stammdatenReadRepository.getProduzenten, defaultTimeout).head
      val modify = copyTo[Produzent, ProduzentModify](produzent).copy(iban = Some(validIban))

      Post(s"/produzenten/${produzent.id.id}", modify) ~> stammdatenRouteService.stammdatenRoute ~> check {
        expectDBEvents(1) { (_, modifications, _, _) =>
          oneEventMatches[Produzent](modifications)(_.iban === Some(validIban))
        }
      }
    }

    "create Produktekategorie" in {
      val create = ProduktekategorieModify("Gemüse und anderes")

      Post("/produktekategorien", create) ~> stammdatenRouteService.stammdatenRoute ~> check {
        expectDBEvents(1) { (creations, _, _, _) =>
          oneEventMatches[Produktekategorie](creations)(_.beschreibung === "Gemüse und anderes")
        }
      }
    }

    "get Produktkategorie" in {
      Get("/produktekategorien") ~> stammdatenRouteService.stammdatenRoute ~> check {
        val result = responseAs[List[Produktekategorie]]

        result.size === 1
        result.head.beschreibung === "Gemüse und anderes"
      }
    }

    "modify Produktkategorie" in {
      val produktekategorie = Await.result(stammdatenRouteService.stammdatenReadRepository.getProduktekategorien, defaultTimeout).head
      val modify = copyTo[Produktekategorie, ProduktekategorieModify](produktekategorie).copy(beschreibung = "Gemüse")

      Post(s"/produktekategorien/${produktekategorie.id.id}", modify) ~> stammdatenRouteService.stammdatenRoute ~> check {
        expectDBEvents(1) { (_, modifications, _, _) =>
          oneEventMatches[Produktekategorie](modifications)(_.beschreibung === "Gemüse")
        }
      }
    }

    "create Produkt" in {
      val produktekategorie = Await.result(stammdatenRouteService.stammdatenReadRepository.getProduktekategorien, defaultTimeout).head
      val produzent = Await.result(stammdatenRouteService.stammdatenReadRepository.getProduzenten, defaultTimeout).head
      val create = ProduktModify("Federkohl", Dezember, Februar, Seq(produktekategorie.beschreibung), Some(0.5), Kilogramm, 11.20, Seq(produzent.kurzzeichen))

      Post("/produkte", create) ~> stammdatenRouteService.stammdatenRoute ~> check {
        expectDBEvents(1) { (creations, _, _, _) =>
          oneEventMatches[Produkt](creations)(_.name === "Federkohl")
        }
      }
    }

    "get Produkt" in {
      val produzent = Await.result(stammdatenRouteService.stammdatenReadRepository.getProduzenten, defaultTimeout).head
      Get("/produkte") ~> stammdatenRouteService.stammdatenRoute ~> check {
        val result = responseAs[List[Produkt]]

        result.size === 1
        result.head.produzenten === Seq(produzent.kurzzeichen)
      }
    }

    "Produzentenbrief" in {
      val produzent = Await.result(stammdatenRouteService.stammdatenReadRepository.getProduzenten, defaultTimeout).head

      val formData = Multipart.FormData(
        Multipart.FormData.BodyPart.Strict("report", "true"),
        Multipart.FormData.BodyPart.Strict("pdfGenerieren", "false"),
        Multipart.FormData.BodyPart.Strict("pdfDownloaden", "true"),
        Multipart.FormData.BodyPart.Strict("pdfMerge", "zip"),
        Multipart.FormData.BodyPart.Strict("ids", s"${produzent.id.id}")
      )

      val wsClient = WSProbe()

      withWsProbeFakeLogin(wsClient) { wsClient =>
        Post(s"/produzenten/berichte/produzentenbrief", formData) ~> stammdatenRouteService.stammdatenRoute ~> check {
          status === StatusCodes.OK

          val reportServiceResult = responseAs[AsyncReportServiceResult]

          reportServiceResult.hasErrors === false

          val file = awaitFileViaWebsocket(wsClient, reportServiceResult)

          val odt = OdfDocument.loadDocument(file)
          val contentAsString = odt.getContentRoot.getChildNodes.toString
          contentAsString must contain("Depot")
        }
      }
    }
  }
}
