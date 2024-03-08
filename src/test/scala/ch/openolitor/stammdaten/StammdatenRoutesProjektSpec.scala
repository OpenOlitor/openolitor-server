package ch.openolitor.stammdaten

import org.apache.pekko.http.scaladsl.model.StatusCodes
import ch.openolitor.core.{ BaseRoutesWithDBSpec, SpecSubjects }
import ch.openolitor.core.security.Subject
import ch.openolitor.stammdaten.models.{ Projekt, ProjektModify }
import ch.openolitor.core.Macros._
import ch.openolitor.core.models.EntityModified

import scala.concurrent.Await

class StammdatenRoutesProjektSpec extends BaseRoutesWithDBSpec with SpecSubjects with StammdatenJsonProtocol {
  sequential

  protected val stammdatenRouteService = new MockStammdatenRoutes(sysConfig, system)
  implicit val subject: Subject = adminSubject

  "StammdatenRoutes for Projekt" should {
    "return Projekt" in {
      Get("/projekt") ~> stammdatenRouteService.stammdatenRoute ~> check {
        val response = responseAs[Option[Projekt]]
        status === StatusCodes.OK
        response.headOption.map { head =>
          head.bezeichnung === "Demo Projekt"
        }
        response must beSome
      }
    }

    "update Projekt" in {
      val projekt = Await.result(stammdatenRouteService.stammdatenReadRepository.getProjekt, defaultTimeout).get
      val update = copyTo[Projekt, ProjektModify](projekt).copy(bezeichnung = "Updated")

      Post(s"/projekt/${projekt.id.id}", update) ~> stammdatenRouteService.stammdatenRoute ~> check {
        status === StatusCodes.Accepted

        dbEventProbe.expectMsgType[EntityModified[Projekt]]

        val updated = Await.result(stammdatenRouteService.stammdatenReadRepository.getProjekt, defaultTimeout).get
        updated.bezeichnung === "Updated"
      }
    }
  }
}
