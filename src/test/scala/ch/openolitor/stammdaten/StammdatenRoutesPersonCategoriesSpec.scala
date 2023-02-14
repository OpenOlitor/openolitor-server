package ch.openolitor.stammdaten

import ch.openolitor.core.Macros.copyTo
import ch.openolitor.core.models.{ EntityCreated, EntityDeleted, EntityModified }
import ch.openolitor.core.security.Subject
import ch.openolitor.core.{ BaseRoutesWithDBSpec, SpecSubjects }
import ch.openolitor.stammdaten.models._

import scala.concurrent.Await

class StammdatenRoutesPersonCategoriesSpec extends BaseRoutesWithDBSpec with SpecSubjects with StammdatenRouteServiceInteractions with StammdatenJsonProtocol {
  sequential

  protected val stammdatenRouteService = new MockStammdatenRoutes(sysConfig, system)

  implicit val subject: Subject = adminSubject

  "StammdatenRoutes for personCategories" should {
    "create personCategory" in {
      val create = PersonCategoryCreate(PersonCategoryNameId("2"), Some("Description"))

      Post("/personCategories", create) ~> stammdatenRouteService.stammdatenRoute ~> check {
        dbEventProbe.expectMsgType[EntityCreated[PersonCategory]]
        dbEventProbe.expectNoMessage()

        val result = Await.result(stammdatenRouteService.stammdatenReadRepository.getPersonCategory(asyncConnectionPoolContext), defaultTimeout)
        result.size === 1
        result.head.description === Some("Description")
      }
    }

    "get personCategory" in {
      Get("/personCategories") ~> stammdatenRouteService.stammdatenRoute ~> check {
        val result = responseAs[List[PersonCategory]]

        result.size === 1
        result.head.description === Some("Description")
      }
    }

    "modify personCategory" in {
      val personCategory = Await.result(stammdatenRouteService.stammdatenReadRepository.getPersonCategory, defaultTimeout).head
      val modify = copyTo[PersonCategory, PersonCategory](personCategory).copy(description = Some("DESCRIPTION"))

      Post(s"/personCategories/${personCategory.id.id}", modify) ~> stammdatenRouteService.stammdatenRoute ~> check {
        dbEventProbe.expectMsgType[EntityModified[PersonCategory]]
        dbEventProbe.expectNoMessage()

        val result = Await.result(stammdatenRouteService.stammdatenReadRepository.getPersonCategory(asyncConnectionPoolContext), defaultTimeout)
        result.size === 1
        result.head.description === Some("DESCRIPTION")
      }
    }
    "delete personCategory" in {
      val personCategory = Await.result(stammdatenRouteService.stammdatenReadRepository.getPersonCategory, defaultTimeout).head
      Delete(s"/personCategories/${personCategory.id.id}") ~> stammdatenRouteService.stammdatenRoute ~> check {
        dbEventProbe.expectMsgType[EntityDeleted[PersonCategory]]
        dbEventProbe.expectNoMessage()
        val result = Await.result(stammdatenRouteService.stammdatenReadRepository.getPersonCategory(asyncConnectionPoolContext), defaultTimeout)
        result.size === 0
      }
    }
  }
}
