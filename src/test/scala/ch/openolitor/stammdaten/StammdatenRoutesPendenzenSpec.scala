package ch.openolitor.stammdaten

import ch.openolitor.core.Macros.copyTo
import ch.openolitor.core.models.{ EntityCreated, EntityDeleted, EntityModified }
import ch.openolitor.core.security.Subject
import ch.openolitor.core.{ BaseRoutesWithDBSpec, SpecSubjects }
import ch.openolitor.stammdaten.models._
import org.joda.time.DateTime

import scala.concurrent.Await

class StammdatenRoutesPendenzenSpec extends BaseRoutesWithDBSpec with SpecSubjects with StammdatenRouteServiceInteractions with StammdatenJsonProtocol {
  sequential

  protected val stammdatenRouteService = new MockStammdatenRoutes(sysConfig, system)

  implicit val subject: Subject = adminSubject

  "StammdatenRoutes for pendenzen" should {
    "create pendenzen" in {
      val kunde = Await.result(stammdatenRouteService.stammdatenReadRepository.getKunden, defaultTimeout).head
      val create = PendenzModify(None, DateTime.now(), Some("Description"), Erledigt)

      Post(s"/kunden/${kunde.id.id}/pendenzen", create) ~> stammdatenRouteService.stammdatenRoute ~> check {
        dbEventProbe.expectMsgType[EntityCreated[Pendenz]]
        dbEventProbe.expectMsgType[EntityModified[Kunde]]
        dbEventProbe.expectMsgType[EntityModified[Pendenz]]
        dbEventProbe.expectNoMessage()

        val result = Await.result(stammdatenRouteService.stammdatenReadRepository.getPendenzen, defaultTimeout)
        result.size === 1
        result.head.bemerkung === Some("Description")
      }
    }

    "get pendenzen" in {
      Get("/pendenzen") ~> stammdatenRouteService.stammdatenRoute ~> check {
        val result = responseAs[List[Pendenz]]

        result.size === 1
        result.head.bemerkung === Some("Description")
      }
    }

    "modify pendenzen" in {
      val pendenz = Await.result(stammdatenRouteService.stammdatenReadRepository.getPendenzen, defaultTimeout).head
      val modify = copyTo[Pendenz, Pendenz](pendenz).copy(bemerkung = Some("DESCRIPTION"))

      Post(s"/pendenzen/${pendenz.id.id}", modify) ~> stammdatenRouteService.stammdatenRoute ~> check {
        dbEventProbe.expectMsgType[EntityModified[Pendenz]]
        dbEventProbe.expectNoMessage()

        val result = Await.result(stammdatenRouteService.stammdatenReadRepository.getPendenzen, defaultTimeout)
        result.size === 1
        result.head.bemerkung === Some("DESCRIPTION")
      }
    }

    "delete pendenzen" in {
      val kunde = Await.result(stammdatenRouteService.stammdatenReadRepository.getKunden, defaultTimeout).head
      val pendenz = Await.result(stammdatenRouteService.stammdatenReadRepository.getPendenzen, defaultTimeout).head
      Delete(s"/kunden/${kunde.id.id}/pendenzen/${pendenz.id.id}") ~> stammdatenRouteService.stammdatenRoute ~> check {
        dbEventProbe.expectMsgType[EntityDeleted[Pendenz]]
        dbEventProbe.expectMsgType[EntityModified[Kunde]]
        dbEventProbe.expectNoMessage()
        val result = Await.result(stammdatenRouteService.stammdatenReadRepository.getPendenzen, defaultTimeout)
        result.size === 0
      }
    }
  }
}
