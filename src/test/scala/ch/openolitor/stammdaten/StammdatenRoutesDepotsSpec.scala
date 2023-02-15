package ch.openolitor.stammdaten

import ch.openolitor.core.Macros.copyTo
import ch.openolitor.core.models.{ EntityCreated, EntityDeleted, EntityModified }
import ch.openolitor.core.security.Subject
import ch.openolitor.core.{ BaseRoutesWithDBSpec, SpecSubjects }
import ch.openolitor.stammdaten.models._

import scala.concurrent.Await

class StammdatenRoutesDepotsSpec extends BaseRoutesWithDBSpec with SpecSubjects with StammdatenRouteServiceInteractions with StammdatenJsonProtocol {
  sequential

  protected val stammdatenRouteService = new MockStammdatenRoutes(sysConfig, system)

  implicit val subject: Subject = adminSubject

  "StammdatenRoutes for Depots" should {
    "create Depots" in {
      val create = DepotModify("Depot", "DEP", None, None, None, None, None, None, None, None, None, None, "821", "Bern", false, None, None, None, None, None, None)
      Post("/depots", create) ~> stammdatenRouteService.stammdatenRoute ~> check {
        dbEventProbe.expectMsgType[EntityCreated[Depot]]
        dbEventProbe.expectNoMessage()

        val result = Await.result(stammdatenRouteService.stammdatenReadRepository.getDepots, defaultTimeout)
        result.size === 1
        result.head.name === "Depot"
      }
    }

    "get depot " in {
      Get("/depots") ~> stammdatenRouteService.stammdatenRoute ~> check {
        val result = responseAs[List[Depot]]

        result.size === 1
        result.head.name === "Depot"
      }
    }

    "get all persons by depots" in {
      Get("/depots/personen/alle") ~> stammdatenRouteService.stammdatenRoute ~> check {
        val result = responseAs[List[PersonSummary]]

        result.size === 1
        result.head.name === "Administrator"
        result.head.vorname === "System"
      }
    }

    "get aktiv persons by depots" in {
      Get("/depots/personen/aktiv") ~> stammdatenRouteService.stammdatenRoute ~> check {
        val result = responseAs[List[PersonSummary]]
        result.size === 0
      }
    }

    "modify depot" in {
      val depot = Await.result(stammdatenRouteService.stammdatenReadRepository.getDepots, defaultTimeout).head
      val modify = copyTo[Depot, DepotModify](depot, "depotlieferungen" -> Seq.empty).copy(name = "DEPOT")

      Post(s"/depots/${depot.id.id}", modify) ~> stammdatenRouteService.stammdatenRoute ~> check {
        dbEventProbe.expectMsgType[EntityModified[Depot]]
        dbEventProbe.expectNoMessage()

        val result = Await.result(stammdatenRouteService.stammdatenReadRepository.getDepots, defaultTimeout)
        result.size === 1
        result.head.name === "DEPOT"
      }
    }

    "modify depot" in {
      val depot = Await.result(stammdatenRouteService.stammdatenReadRepository.getDepots, defaultTimeout).head
      val modify = copyTo[Depot, DepotModify](depot, "depotlieferungen" -> Seq.empty).copy(name = "DEPOT")

      Put(s"/depots/${depot.id.id}", modify) ~> stammdatenRouteService.stammdatenRoute ~> check {
        dbEventProbe.expectMsgType[EntityModified[Depot]]
        dbEventProbe.expectNoMessage()

        val result = Await.result(stammdatenRouteService.stammdatenReadRepository.getDepots, defaultTimeout)
        result.size === 1
        result.head.name === "DEPOT"
      }
    }

    "delete depot" in {
      val depot = Await.result(stammdatenRouteService.stammdatenReadRepository.getDepots, defaultTimeout).head
      Delete(s"/depots/${depot.id.id}") ~> stammdatenRouteService.stammdatenRoute ~> check {
        dbEventProbe.expectMsgType[EntityDeleted[Tour]]
        dbEventProbe.expectNoMessage()
        val result = Await.result(stammdatenRouteService.stammdatenReadRepository.getDepots, defaultTimeout)
        result.size === 0
      }
    }
  }
}
