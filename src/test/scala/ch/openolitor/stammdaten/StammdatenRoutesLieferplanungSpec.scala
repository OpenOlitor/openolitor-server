package ch.openolitor.stammdaten

import akka.http.scaladsl.model.{ ContentTypes, Multipart, StatusCodes }
import akka.http.scaladsl.testkit.WSProbe
import ch.openolitor.core.{ BaseRoutesWithDBSpec, SpecSubjects }
import ch.openolitor.core.models.EntityCreated
import ch.openolitor.core.reporting.AsyncReportServiceResult
import ch.openolitor.core.security.Subject
import ch.openolitor.stammdaten.models._
import org.odftoolkit.odfdom.doc.OdfDocument

import java.io.File
import scala.concurrent.Await

class StammdatenRoutesLieferplanungSpec extends BaseRoutesWithDBSpec with SpecSubjects with StammdatenRouteServiceInteractions with StammdatenJsonProtocol {
  sequential

  import ch.openolitor.util.Fixtures._

  protected val stammdatenRouteService = new MockStammdatenRoutes(sysConfig, system)

  implicit val subject: Subject = adminSubject

  "StammdatenRoutes for Lieferplanung" should {
    "create Depot" in {
      createDepotWwg()
    }

    "get Depot" in {
      Get(s"/depots") ~> stammdatenRouteService.stammdatenRoute ~> check {
        val result = responseAs[List[Depot]]

        result.size === 1

        result.head.ort === depotWwg.ort
      }
    }

    "create Tour" in {
      createTour()
    }

    "get Tour" in {
      Get(s"/touren") ~> stammdatenRouteService.stammdatenRoute ~> check {
        val result = responseAs[List[Tour]]

        result.size === 1

        result.head.name === tourCreate.name
      }
    }

    "create Abotyp with Vertrieb and Vertriebsarten" in {
      createAbotypWithVertriebeAndVertriebsarten()
    }

    "get Abotypen" in {
      Get(s"/abotypen") ~> stammdatenRouteService.stammdatenRoute ~> check {
        val result = responseAs[List[Abotyp]]

        result.size === 1
        result.head.name === abotypVegiModify.name
      }
    }

    "get Vertriebe" in {
      Get(s"/vertriebe") ~> stammdatenRouteService.stammdatenRoute ~> check {
        val result = responseAs[List[Vertrieb]]

        result.size === 3

        result.map(_.id) must containTheSameElementsAs(Seq(vertriebIdDepot, vertriebIdTour, vertriebIdPost))
      }
    }

    "create ZusatzAbotyp" in {
      Post("/zusatzAbotypen", zusatzAbotypEier) ~> stammdatenRouteService.stammdatenRoute ~> check {
        status === StatusCodes.Created

        dbEventProbe.expectMsgType[EntityCreated[ZusatzAbotyp]]
        dbEventProbe.expectNoMessage()

        val result = Await.result(stammdatenRouteService.stammdatenReadRepository.getZusatzAbotypen(asyncConnectionPoolContext, None, None), defaultTimeout)
        result.size === 1
      }
    }

    "get ZusatzAbotyp" in {
      Get(s"/zusatzAbotypen") ~> stammdatenRouteService.stammdatenRoute ~> check {
        val result = responseAs[List[ZusatzAbotyp]]

        result.size === 1
        result.head.name === zusatzAbotypEier.name
      }
    }

    "create Kunden" in {
      createKunde(kundeCreateUntertorOski)
      createKunde(kundeCreateMatteEdi)
      createKunde(kundeCreateHaseFritz)
    }

    "get Kunden" in {
      Get(s"/kunden") ~> stammdatenRouteService.stammdatenRoute ~> check {
        val result = responseAs[List[KundeUebersicht]]

        result.map(_.bezeichnung) must containAllOf(Seq("Untertor Oski", "Matte Edi", "Hase Fritz"))

        result.map { kundeUebersicht =>
          Get(s"/kunden/${kundeUebersicht.id.id}") ~> stammdatenRouteService.stammdatenRoute ~> check {
            val result = responseAs[KundeDetail]
            result.bezeichnung === kundeUebersicht.bezeichnung
          }
        }
      }
    }

    "create Abo for Kunde (Depot)" in {
      createDepotlieferungAbo(kundeCreateUntertorOski)
    }

    "create Abo for Kunde (Tour)" in {
      createTourlieferungAbo(kundeCreateMatteEdi)
    }

    "create Abo for Kunde (Post)" in {
      createPostlieferungAbo(kundeCreateHaseFritz)
    }

    "get Abos" in {
      Get(s"/abos") ~> stammdatenRouteService.stammdatenRoute ~> check {
        val result = responseAs[List[Abo]]

        result.size === 3
      }
    }

    "create Lieferplanung" in {
      createLieferplanung()
    }

    "get Lieferplanung" in {
      Get(s"/lieferplanungen") ~> stammdatenRouteService.stammdatenRoute ~> check {
        val result = responseAs[List[Lieferplanung]]

        result.size === 1
      }
    }

    "close Lieferplanung" in {
      closeLieferplanung()
    }

    "generate Lieferetiketten (Depot)" in {
      val depotAuslieferungen = Await.result(stammdatenRouteService.stammdatenReadRepository.getDepotAuslieferungen, defaultTimeout)

      depotAuslieferungen.size === 1

      val formData = Multipart.FormData(
        Multipart.FormData.BodyPart.Strict("report", "true"),
        Multipart.FormData.BodyPart.Strict("pdfDownloaden", "true"),
        Multipart.FormData.BodyPart.Strict("pdfMerge", "zip"),
        Multipart.FormData.BodyPart.Strict("ids", s"${depotAuslieferungen.head.id.id}")
      )

      val wsClient = WSProbe()

      withWsProbeFakeLogin(wsClient) { wsClient =>
        Post(s"/depotauslieferungen/berichte/lieferetiketten", formData) ~> stammdatenRouteService.stammdatenRoute ~> check {
          status === StatusCodes.OK

          val reportServiceResult = responseAs[AsyncReportServiceResult]

          reportServiceResult.hasErrors === false

          val file = awaitFileViaWebsocket(wsClient, reportServiceResult)

          val odt = OdfDocument.loadDocument(file)
          val contentAsString = odt.getContentRoot.getChildNodes.toString
          contentAsString must contain("Deep Oh")
          contentAsString must contain("Untertor Oski")
          contentAsString must not contain ("Matte Edi")

          file.isFile must beTrue
        }
      }
    }

    "generate Korbuebersicht using custom vorlage (Depot)" in {
      val depotAuslieferungen = Await.result(stammdatenRouteService.stammdatenReadRepository.getDepotAuslieferungen, defaultTimeout)

      depotAuslieferungen.size === 1

      val formData = Multipart.FormData(
        Multipart.FormData.BodyPart.Strict("report", "true"),
        Multipart.FormData.BodyPart.Strict("pdfGenerieren", "true"),
        Multipart.FormData.BodyPart.Strict("pdfDownloaden", "true"),
        Multipart.FormData.BodyPart.Strict("pdfMerge", "zip"),
        Multipart.FormData.BodyPart.Strict("ids", s"${depotAuslieferungen.head.id.id}"),
        Multipart.FormData.BodyPart.fromFile("vorlage", ContentTypes.`application/octet-stream`, new File(getClass.getClassLoader.getResource("VorlageKorbUebersichtGnu.odt").getPath))
      )

      val wsClient = WSProbe()

      withWsProbeFakeLogin(wsClient) { wsClient =>
        Post(s"/depotauslieferungen/berichte/korbuebersicht", formData) ~> stammdatenRouteService.stammdatenRoute ~> check {
          status === StatusCodes.OK

          val reportServiceResult = responseAs[AsyncReportServiceResult]

          reportServiceResult.hasErrors === false

          val file = awaitFileViaWebsocket(wsClient, reportServiceResult)
          file.getName must endWith(".pdf")
        }
      }
    }
  }

}
