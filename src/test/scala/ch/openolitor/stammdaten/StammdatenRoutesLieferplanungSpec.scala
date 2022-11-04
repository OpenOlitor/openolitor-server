package ch.openolitor.stammdaten

import akka.http.scaladsl.model.{ ContentTypes, Multipart, StatusCodes }
import akka.http.scaladsl.testkit.WSProbe
import akka.testkit.TestProbe
import ch.openolitor.core.{ BaseRoutesWithDBSpec, SpecSubjects }
import ch.openolitor.core.jobs.JobQueueService.{ FetchJobResult, FileResultPayload, JobResult }
import ch.openolitor.core.models.EntityCreated
import ch.openolitor.core.reporting.AsyncReportServiceResult
import ch.openolitor.core.security.Subject
import ch.openolitor.core.ws.MockClientMessagesRouteService
import ch.openolitor.stammdaten.models._
import ch.openolitor.util.parsing.{ FilterExpr, GeschaeftsjahrFilter, QueryFilter }
import org.odftoolkit.odfdom.doc.OdfDocument
import org.specs2.matcher.MatchResult

import java.io.File
import scala.concurrent.Await

class StammdatenRoutesLieferplanungSpec extends BaseRoutesWithDBSpec with SpecSubjects with StammdatenRouteServiceInteractions with StammdatenJsonProtocol {
  sequential

  import ch.openolitor.util.Fixtures._

  protected val stammdatenRouteService = new MockStammdatenRoutes(sysConfig, system)
  protected val clientMessagesService = new MockClientMessagesRouteService(sysConfig, system)

  implicit val subject: Subject = adminSubject

  "StammdatenRoutes for Lieferplanung" should {
    "create Depot" in {
      createDepotWwg()
    }

    "create Tour" in {
      createTour()
    }

    "create Abotyp with Vertrieb and Vertriebsarten" in {
      createAbotypWithVertriebeAndVertriebsarten()
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

    "create Kunden" in {
      createKunde(kundeCreateUntertorOski)
      createKunde(kundeCreateMatteEdi)
      createKunde(kundeCreateHaseFritz)
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

    "create Lieferplanung" in {
      createLieferplanung()
    }

    "close Lieferplanung" in {
      closeLieferplanung()
    }

    "generate Lieferetiketten (Depot)" in {
      implicit val filter: Option[FilterExpr] = None
      implicit val gjFilter: Option[GeschaeftsjahrFilter] = None
      implicit val queryString: Option[QueryFilter] = None

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
      implicit val filter: Option[FilterExpr] = None
      implicit val gjFilter: Option[GeschaeftsjahrFilter] = None
      implicit val queryString: Option[QueryFilter] = None

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

  private def withWsProbeFakeLogin(wsClient: WSProbe)(block: WSProbe => MatchResult[_]) = {
    WS("/", wsClient.flow) ~> clientMessagesService.routes ~> check {
      isWebSocketUpgrade === true

      // logging in manually via ws to attach our wsClient
      wsClient.sendMessage(s"""{"type":"Login","token":"${adminSubjectToken}"}""")
      val loginOkMessage = wsClient.expectMessage()
      loginOkMessage.asTextMessage.getStrictText must contain("LoggedIn")

      block(wsClient)
    }
  }

  private def awaitFileViaWebsocket(wsClient: WSProbe, reportServiceResult: AsyncReportServiceResult) = {
    // receive progress update
    val progressMessage = wsClient.expectMessage()
    progressMessage.asTextMessage.getStrictText must contain(""""numberOfTasksInProgress":1""")

    // receive task completion
    val completionMessage = wsClient.expectMessage()
    completionMessage.asTextMessage.getStrictText must contain(""""progresses":[]""")

    // fetch the result directly from jobQueueService
    val probe = TestProbe()
    probe.send(jobQueueService, FetchJobResult(subject.personId, reportServiceResult.jobId.id))

    val message = probe.expectMsgType[JobResult]

    message.payload must beSome

    message.payload.get.asInstanceOf[FileResultPayload].file
  }
}
