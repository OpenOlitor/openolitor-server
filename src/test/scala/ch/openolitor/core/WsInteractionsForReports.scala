package ch.openolitor.core

import akka.http.scaladsl.testkit.WSProbe
import akka.testkit.TestProbe
import ch.openolitor.core.jobs.JobQueueService.{ FetchJobResult, FileResultPayload, JobResult }
import ch.openolitor.core.reporting.AsyncReportServiceResult
import ch.openolitor.core.ws.MockClientMessagesRouteService
import org.specs2.matcher.MatchResult

import java.io.File

trait WsInteractionsForReports extends BaseRoutesSpec with SpecSubjects {

  protected lazy val clientMessagesService = new MockClientMessagesRouteService(sysConfig, system)

  protected def withWsProbeFakeLogin(wsClient: WSProbe)(block: WSProbe => MatchResult[_]) = {
    WS("/", wsClient.flow) ~> clientMessagesService.routes ~> check {
      isWebSocketUpgrade === true

      // logging in manually via ws to attach our wsClient
      wsClient.sendMessage(s"""{"type":"Login","token":"${adminSubjectToken}"}""")
      val loginOkMessage = wsClient.expectMessage()
      loginOkMessage.asTextMessage.getStrictText must contain("LoggedIn")

      block(wsClient)
    }
  }

  protected def awaitFileViaWebsocket(wsClient: WSProbe, reportServiceResult: AsyncReportServiceResult): File = {
    // receive progress update
    val progressMessage = wsClient.expectMessage()
    progressMessage.asTextMessage.getStrictText must contain(""""numberOfTasksInProgress":1""")

    // receive task completion
    val completionMessage = wsClient.expectMessage()
    completionMessage.asTextMessage.getStrictText must contain(""""progresses":[]""")

    // fetch the result directly from jobQueueService
    val probe = TestProbe()
    probe.send(jobQueueService, FetchJobResult(adminSubject.personId, reportServiceResult.jobId.id))

    val message = probe.expectMsgType[JobResult]

    message.payload must beSome

    message.payload.get.asInstanceOf[FileResultPayload].file
  }
}
