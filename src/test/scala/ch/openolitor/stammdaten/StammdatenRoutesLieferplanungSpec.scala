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

  private val service = new MockStammdatenRoutes(sysConfig, system)
  private val clientMessagesService = new MockClientMessagesRouteService(sysConfig, system)

  implicit val subject: Subject = adminSubject

  "StammdatenRoutes for Lieferplanung" should {
    "create Depot" in {
      Post("/depots", depotWwgModify) ~> service.stammdatenRoute ~> check {
        status === StatusCodes.Created

        dbEventProbe.expectMsgType[EntityCreated[Depot]]
        dbEventProbe.expectNoMessage()

        val result = Await.result(service.stammdatenReadRepository.getDepots, defaultTimeout)
        result.size === 1
      }
    }

    "create Abotyp with Vertrieb and Vertriebsart" in {
      val depot = Await.result(service.stammdatenReadRepository.getDepots, defaultTimeout).head

      Post("/abotypen", abotypVegiModify) ~> service.stammdatenRoute ~> check {
        status === StatusCodes.Created

        dbEventProbe.expectMsgType[EntityCreated[Abotyp]]

        val result = Await.result(service.stammdatenReadRepository.getAbotypen(asyncConnectionPoolContext, None, None), defaultTimeout)
        result.size === 1
        val abotypId = result(0).id

        Post(s"/abotypen/${abotypId.id}/vertriebe", vertriebDonnerstagModify) ~> service.stammdatenRoute ~> check {
          status === StatusCodes.Created

          val vertrieb = dbEventProbe.expectMsgType[EntityCreated[Vertrieb]]

          Await.result(service.stammdatenReadRepository.getVertriebe(abotypId), defaultTimeout).size === 1

          createSimpleVertriebVertriebsart(service)
        }
      }
    }

    "create ZusatzAbotyp" in {
      Post("/zusatzAbotypen", zusatzAbotypEier) ~> service.stammdatenRoute ~> check {
        status === StatusCodes.Created

        dbEventProbe.expectMsgType[EntityCreated[ZusatzAbotyp]]
        dbEventProbe.expectNoMessage()

        val result = Await.result(service.stammdatenReadRepository.getZusatzAbotypen(asyncConnectionPoolContext, None, None), defaultTimeout)
        result.size === 1
      }
    }

    "create Kunde with Abo" in {
      createSimpleAbo(service)
    }

    "create Lieferplanung" in {
      val lieferplanungCreate = LieferplanungCreate(None)

      Post("/lieferplanungen", lieferplanungCreate) ~> service.stammdatenRoute ~> check {
        status === StatusCodes.Created

        expectDBEvents(6) { (creations, _, _, _) =>
          oneEventMatches[Lieferplanung](creations)(_.status === Offen)

          allEventsMatch[Korb](creations)(_.status === WirdGeliefert)
        }
      }
    }

    "close Lieferplanung" in {
      val lieferplanung = Await.result(service.stammdatenReadRepository.getLatestLieferplanung, defaultTimeout).get

      Post(s"/lieferplanungen/${lieferplanung.id.id}/aktionen/abschliessen") ~> service.stammdatenRoute ~> check {
        status === StatusCodes.OK

        expectDBEvents(5) { (creations, modifications, _, _) =>
          creations.size === 1
          modifications.size === 4
          allEventsMatch[DepotAuslieferung](creations)(_.status === Erfasst)
        }
      }
    }

    "generate Lieferetiketten" in {
      implicit val filter: Option[FilterExpr] = None
      implicit val gjFilter: Option[GeschaeftsjahrFilter] = None
      implicit val queryString: Option[QueryFilter] = None

      val depotAuslieferungen = Await.result(service.stammdatenReadRepository.getDepotAuslieferungen, defaultTimeout)

      depotAuslieferungen.size === 1

      val formData = Multipart.FormData(
        Multipart.FormData.BodyPart.Strict("report", "true"),
        Multipart.FormData.BodyPart.Strict("pdfDownloaden", "true"),
        Multipart.FormData.BodyPart.Strict("pdfMerge", "zip"),
        Multipart.FormData.BodyPart.Strict("ids", s"${depotAuslieferungen.head.id.id}")
      )

      val wsClient = WSProbe()

      withWsProbeFakeLogin(wsClient) { wsClient =>
        Post(s"/depotauslieferungen/berichte/lieferetiketten", formData) ~> service.stammdatenRoute ~> check {
          status === StatusCodes.OK

          val reportServiceResult = responseAs[AsyncReportServiceResult]

          reportServiceResult.hasErrors === false

          val file = awaitFileViaWebsocket(wsClient, reportServiceResult)

          val odt = OdfDocument.loadDocument(file)
          val contentAsString = odt.getContentRoot.getChildNodes.toString
          contentAsString must contain("Deep Oh")
          contentAsString must contain("Untertor Oski")

          file.isFile must beTrue
        }
      }
    }

    "generate Korbuebersicht using custom vorlage" in {
      implicit val filter: Option[FilterExpr] = None
      implicit val gjFilter: Option[GeschaeftsjahrFilter] = None
      implicit val queryString: Option[QueryFilter] = None

      val depotAuslieferungen = Await.result(service.stammdatenReadRepository.getDepotAuslieferungen, defaultTimeout)

      depotAuslieferungen.size === 1

      val formData = Multipart.FormData(
        Multipart.FormData.BodyPart.Strict("report", "true"),
        Multipart.FormData.BodyPart.Strict("pdfGenerieren", "false"),
        Multipart.FormData.BodyPart.Strict("pdfDownloaden", "true"),
        Multipart.FormData.BodyPart.Strict("pdfMerge", "zip"),
        Multipart.FormData.BodyPart.Strict("ids", s"${depotAuslieferungen.head.id.id}"),
        Multipart.FormData.BodyPart.fromFile("vorlage", ContentTypes.`application/octet-stream`, new File(getClass.getClassLoader.getResource("VorlageKorbUebersichtGnu.odt").getPath))
      )

      val wsClient = WSProbe()

      withWsProbeFakeLogin(wsClient) { wsClient =>
        Post(s"/depotauslieferungen/berichte/korbuebersicht", formData) ~> service.stammdatenRoute ~> check {
          status === StatusCodes.OK

          val reportServiceResult = responseAs[AsyncReportServiceResult]

          reportServiceResult.hasErrors === false

          val file = awaitFileViaWebsocket(wsClient, reportServiceResult)

          val odt = OdfDocument.loadDocument(file)
          val contentAsString = odt.getContentRoot.getChildNodes.toString
          contentAsString must contain("Deep Oh")

          file.isFile must beTrue
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
