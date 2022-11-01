package ch.openolitor.stammdaten

import akka.http.scaladsl.model.{ Multipart, StatusCodes }
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
import org.joda.time.{ DateTime, LocalDate, Months }
import org.odftoolkit.odfdom.doc.OdfDocument
import spray.json.{ JsNumber, JsObject, JsString }

import scala.concurrent.Await

class StammdatenRoutesLieferplanungSpec extends BaseRoutesWithDBSpec with SpecSubjects with StammdatenJsonProtocol {
  sequential

  private val service = new MockStammdatenRoutes(sysConfig, system)
  private val clientMessagesService = new MockClientMessagesRouteService(sysConfig, system)

  implicit val subject: Subject = adminSubject

  "StammdatenRoutes for Lieferplanung" should {
    "create Depot" in {
      val depot = DepotModify("Deep Oh", "DEP", None, None, None, None, None, None, None, None, Some("Wasserwerkgasse"), None, "3011", "Bern", true, None, Some("#00ccff"), None, None, None, None)

      Post("/depots", depot) ~> service.stammdatenRoute ~> check {
        status === StatusCodes.Created

        dbEventProbe.expectMsgType[EntityCreated[Depot]]
        dbEventProbe.expectNoMessage()

        val result = Await.result(service.stammdatenReadRepository.getDepots, defaultTimeout)
        result.size === 1
      }
    }

    "create Abotyp with Vertrieb and Vertriebsart" in {
      val abotyp = AbotypModify("Vegi", None, Woechentlich, Some(LocalDate.now().withDayOfWeek(1).minus(Months.TWO)), None, BigDecimal(17.5), ProQuartal, None, Unbeschraenkt, Some(Frist(6, Monatsfrist)), Some(Frist(1, Monatsfrist)), Some(4),
        None, "#ff0000", Some(BigDecimal(17.0)), 2, BigDecimal(12), true)
      val depot = Await.result(service.stammdatenReadRepository.getDepots, defaultTimeout).head

      Post("/abotypen", abotyp) ~> service.stammdatenRoute ~> check {
        status === StatusCodes.Created

        dbEventProbe.expectMsgType[EntityCreated[Abotyp]]

        val result = Await.result(service.stammdatenReadRepository.getAbotypen(asyncConnectionPoolContext, None, None), defaultTimeout)
        result.size === 1
        val abotypId = result(0).id

        Post(s"/abotypen/${abotypId.id}/vertriebe", VertriebModify(abotypId, Donnerstag, None)) ~> service.stammdatenRoute ~> check {
          status === StatusCodes.Created

          val vertrieb = dbEventProbe.expectMsgType[EntityCreated[Vertrieb]]

          Await.result(service.stammdatenReadRepository.getVertriebe(abotypId), defaultTimeout).size === 1

          val json = JsObject(Map("depotId" -> JsNumber(depot.id.id), "abotypId" -> JsNumber(abotypId.id), "vertriebId" -> JsNumber(vertrieb.entity.id.id), "typ" -> JsString("Depotlieferung")))

          Post(s"/abotypen/${abotypId.id}/vertriebe/${vertrieb.entity.id.id}/vertriebsarten", json) ~> service.stammdatenRoute ~> check {
            status === StatusCodes.Created

            dbEventProbe.expectMsgType[EntityCreated[Depotlieferung]]
            dbEventProbe.expectNoMessage()

            Await.result(service.stammdatenReadRepository.getVertriebsarten(vertrieb.entity.id), defaultTimeout).size === 1

            val lieferungenAbotypCreate = LieferungenAbotypCreate(abotypId, vertrieb.entity.id, (0 to 4).map(months => DateTime.now().withDayOfWeek(1).plusMonths(months)))

            Post(s"/abotypen/${abotypId.id}/vertriebe/${vertrieb.entity.id.id}/lieferungen/aktionen/generieren", lieferungenAbotypCreate) ~> service.stammdatenRoute ~> check {
              status === StatusCodes.Created

              expectDBEvents(5) { (creations, _, _, _) =>
                dbEventProbe.expectNoMessage()
                oneEventMatches[Lieferung](creations)(_.abotypId === abotypId)
              }
            }
          }
        }
      }
    }

    "create ZusatzAbotyp" in {
      val zusatzAbotyp = ZusatzAbotypModify("Eier", None, Some(LocalDate.now().withDayOfWeek(1).minus(Months.TWO)), None, BigDecimal(5.2), ProLieferung, None, Unbeschraenkt, Some(Frist(2, Monatsfrist)), Some(Frist(1, Monatsfrist)), Some(2), None, "#ffcc00", Some(BigDecimal(5)), BigDecimal(10), true, CHF)

      Post("/zusatzAbotypen", zusatzAbotyp) ~> service.stammdatenRoute ~> check {
        status === StatusCodes.Created

        dbEventProbe.expectMsgType[EntityCreated[ZusatzAbotyp]]
        dbEventProbe.expectNoMessage()

        val result = Await.result(service.stammdatenReadRepository.getZusatzAbotypen(asyncConnectionPoolContext, None, None), defaultTimeout)
        result.size === 1
      }
    }

    "create Kunde with Abo" in {
      val kundeCreate = KundeModify(true, None, "Wasserwerkgasse", Some("2"), None, "3011", "Bern", None, false, None, None, None, None, None, None, None, None, None, Set(), Seq(), Seq(PersonModify(
        None, None, "Untertor", "Oski", Some("info@example.com"), None, None, None, Set(), None, None, false
      )), None, None)

      Post("/kunden", kundeCreate) ~> service.stammdatenRoute ~> check {
        status === StatusCodes.Created

        val kunde = dbEventProbe.expectMsgType[EntityCreated[Kunde]]
        dbEventProbe.expectMsgType[EntityCreated[KontoDaten]]
        dbEventProbe.expectMsgType[EntityCreated[Person]]
        dbEventProbe.expectNoMessage()

        val kunden = Await.result(service.stammdatenReadRepository.getKunden, defaultTimeout)
        // the result list includes system administrator
        kunden.size === 2

        val abo = DepotlieferungAboCreate(kunde.entity.id, "Untertor", VertriebsartId(1), DepotId(1), LocalDate.now().withDayOfWeek(1).minus(Months.ONE), None, None)

        Post(s"/kunden/${kunde.entity.id.id}/abos", abo) ~> service.stammdatenRoute ~> check {
          status === StatusCodes.Created

          val created = dbEventProbe.expectMsgType[EntityCreated[DepotlieferungAbo]]

          Await.result(service.stammdatenReadRepository.getAboDetail(created.entity.id), defaultTimeout) must beSome

          val guthabenModify = AboGuthabenModify(0, 12, "You deserve it")

          Post(s"/kunden/${kunde.entity.id.id}/abos/${created.entity.id.id}/aktionen/guthabenanpassen", guthabenModify) ~> service.stammdatenRoute ~> check {
            status === StatusCodes.Accepted

            expectDBEvents(9) { (creations, modifications, _, _) =>
              creations.size === 1
              modifications.size === 8

              oneEventMatches[Pendenz](creations)(_.bemerkung.get must contain("You deserve it"))

              oneEventMatches[Depot](modifications)(_.anzahlAbonnentenAktiv === 1)
              oneEventMatches[Abotyp](modifications)(_.anzahlAbonnentenAktiv === 1)
              oneEventMatches[Kunde](modifications)(_.anzahlAbosAktiv === 1)
              oneEventMatches[Vertrieb](modifications)(_.anzahlAbosAktiv === 1)
              oneEventMatches[Depotlieferung](modifications)(_.anzahlAbosAktiv === 1)
              oneEventMatches[DepotlieferungAbo](modifications)(_.guthaben === 12)
            }
          }
        }
      }
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

        expectDBEvents(5) { (creations, _, _, _) =>
          oneEventMatches[DepotAuslieferung](creations)(_.status === Erfasst)
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

      WS("/", wsClient.flow) ~> clientMessagesService.routes ~> check {
        isWebSocketUpgrade === true

        // logging in manually via ws to attach our wsClient
        wsClient.sendMessage(s"""{"type":"Login","token":"${adminSubjectToken}"}""")
        val loginOkMessage = wsClient.expectMessage()
        loginOkMessage.asTextMessage.getStrictText must contain("LoggedIn")

        Post(s"/depotauslieferungen/berichte/lieferetiketten", formData) ~> service.stammdatenRoute ~> check {
          status === StatusCodes.OK

          val reportServiceResult = responseAs[AsyncReportServiceResult]

          reportServiceResult.hasErrors === false

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

          val file = message.payload.get.asInstanceOf[FileResultPayload].file

          val odt = OdfDocument.loadDocument(file)
          val contentAsString = odt.getContentRoot.getChildNodes.toString
          contentAsString must contain("Deep Oh")
          contentAsString must contain("Untertor Oski")

          file.isFile must beTrue
        }
      }
    }
  }
}
