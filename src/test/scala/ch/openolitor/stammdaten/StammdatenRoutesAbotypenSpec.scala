package ch.openolitor.stammdaten

import ch.openolitor.core.Macros.copyTo
import ch.openolitor.core.models.{ EntityCreated, EntityModified }
import ch.openolitor.core.security.Subject
import ch.openolitor.core.{ BaseRoutesWithDBSpec, SpecSubjects }
import ch.openolitor.stammdaten.models._
import org.joda.time.LocalDate

import scala.concurrent.Await

class StammdatenRoutesAbotypenSpec extends BaseRoutesWithDBSpec with SpecSubjects with StammdatenRouteServiceInteractions with StammdatenJsonProtocol {
  sequential

  protected val stammdatenRouteService = new MockStammdatenRoutes(sysConfig, system)

  implicit val subject: Subject = adminSubject

  "StammdatenRoutes for abotypen" should {
    "create abotyp" in {
      val create = AbotypModify("abotyp", None, Monatlich, None, None, BigDecimal(30), ProLieferung, None, Lieferungen, None, None, None, None, "red", None, 1, BigDecimal(10), true)

      Post("/abotypen", create) ~> stammdatenRouteService.stammdatenRoute ~> check {
        //expectDBEvents(3) { (creations, _, _, _) =>
        //  oneEventMatches[Kunde](creations)(_.bezeichnung === "bezeichnung")
        //}
        dbEventProbe.expectMsgType[EntityCreated[Abotyp]]
        dbEventProbe.expectNoMessage()

        val result = Await.result(stammdatenRouteService.stammdatenReadRepository.getAbotypen(asyncConnectionPoolContext, None, None), defaultTimeout)
        result.size === 1
        result.head.name === "abotyp"
      }
    }

    "get abotyp" in {
      Get("/abotypen") ~> stammdatenRouteService.stammdatenRoute ~> check {
        val result = responseAs[List[Abotyp]]

        result.size === 1
        result.head.name === "abotyp"
      }
    }

    "modify abotyp" in {
      val abotyp = Await.result(stammdatenRouteService.stammdatenReadRepository.getAbotypen, defaultTimeout).head
      val modify = copyTo[Abotyp, Abotyp](abotyp).copy(name = "ABOTYP")

      Post(s"/abotypen/${abotyp.id.id}", modify) ~> stammdatenRouteService.stammdatenRoute ~> check {
        dbEventProbe.expectMsgType[EntityModified[Abotyp]]
        dbEventProbe.expectNoMessage()

        val result = Await.result(stammdatenRouteService.stammdatenReadRepository.getAbotypen(asyncConnectionPoolContext, None, None), defaultTimeout)
        result.size === 1
        result.head.name === "ABOTYP"
      }
    }
  }
}
