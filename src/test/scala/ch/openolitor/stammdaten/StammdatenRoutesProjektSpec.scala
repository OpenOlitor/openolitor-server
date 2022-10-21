package ch.openolitor.stammdaten

import akka.http.scaladsl.model.StatusCodes
import ch.openolitor.core.{ BaseRoutesWithDBSpec, SpecSubjects }
import ch.openolitor.core.security.Subject
import ch.openolitor.stammdaten.models.Projekt

class StammdatenRoutesProjektSpec extends BaseRoutesWithDBSpec with SpecSubjects with StammdatenJsonProtocol {
  sequential

  private val service = new MockStammdatenRoutes(sysConfig, system)
  implicit val subject: Subject = adminSubject

  "StammdatenRoutes" should {
    "return Projekt" in {
      Get("/projekt") ~> service.stammdatenRoute ~> check {
        val response = responseAs[Option[Projekt]]
        status === StatusCodes.OK
        response.headOption.map { head =>
          head.bezeichnung === "Demo Projekt"
        }
        response must beSome
      }
    }
  }
}
