package ch.openolitor.stammdaten

import ch.openolitor.core.Macros.copyTo
import ch.openolitor.core.models.{ EntityCreated, EntityDeleted, EntityModified }
import ch.openolitor.core.security.Subject
import ch.openolitor.core.{ BaseRoutesWithDBSpec, SpecSubjects }
import ch.openolitor.stammdaten.models._

import scala.concurrent.Await

class StammdatenRoutesTourenSpec extends BaseRoutesWithDBSpec with SpecSubjects with StammdatenRouteServiceInteractions with StammdatenJsonProtocol {
  sequential

  protected val stammdatenRouteService = new MockStammdatenRoutes(sysConfig, system)

  implicit val subject: Subject = adminSubject

  "StammdatenRoutes for Touren" should {
    "create Tour" in {
      val create = TourCreate("Tour", Some("Tour description"))

      Post("/touren", create) ~> stammdatenRouteService.stammdatenRoute ~> check {
        dbEventProbe.expectMsgType[EntityCreated[Tour]]
        dbEventProbe.expectNoMessage()

        val result = Await.result(stammdatenRouteService.stammdatenReadRepository.getTouren, defaultTimeout)
        result.size === 1
        result.head.beschreibung === Some("Tour description")
      }
    }

    "get tour" in {
      Get("/touren") ~> stammdatenRouteService.stammdatenRoute ~> check {
        val result = responseAs[List[Tour]]

        result.size === 1
        result.head.beschreibung === Some("Tour description")
      }
    }

    "modify tour" in {
      val tour = Await.result(stammdatenRouteService.stammdatenReadRepository.getTouren, defaultTimeout).head
      val modify = copyTo[Tour, TourModify](tour, "tourlieferungen" -> Seq.empty).copy(beschreibung = Some("TOUR DESCRIPTION"))

      Post(s"/touren/${tour.id.id}", modify) ~> stammdatenRouteService.stammdatenRoute ~> check {
        dbEventProbe.expectMsgType[EntityModified[Tour]]
        dbEventProbe.expectNoMessage()

        val result = Await.result(stammdatenRouteService.stammdatenReadRepository.getTouren, defaultTimeout)
        result.size === 1
        result.head.beschreibung === Some("TOUR DESCRIPTION")
      }
    }
    "delete tour" in {
      val tour = Await.result(stammdatenRouteService.stammdatenReadRepository.getTouren, defaultTimeout).head
      Delete(s"/touren/${tour.id.id}") ~> stammdatenRouteService.stammdatenRoute ~> check {
        dbEventProbe.expectMsgType[EntityDeleted[Tour]]
        dbEventProbe.expectNoMessage()
        val result = Await.result(stammdatenRouteService.stammdatenReadRepository.getTouren, defaultTimeout)
        result.size === 0
      }
    }
  }
}
