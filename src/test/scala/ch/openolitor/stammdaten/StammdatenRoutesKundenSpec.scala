package ch.openolitor.stammdaten

import akka.http.scaladsl.server.Directives.onSuccess
import ch.openolitor.core.Macros.copyTo
import ch.openolitor.core.security.Subject
import ch.openolitor.core.{BaseRoutesWithDBSpec, SpecSubjects}
import ch.openolitor.stammdaten.models._

import scala.concurrent.Await

class StammdatenRoutesKundenSpec extends BaseRoutesWithDBSpec with SpecSubjects with StammdatenRouteServiceInteractions with StammdatenJsonProtocol {
  sequential

  protected val stammdatenRouteService = new MockStammdatenRoutes(sysConfig, system)

  implicit val subject: Subject = adminSubject

  "StammdatenRoutes for Kunden and Personen" should {
    "create Kunden" in {
      val create = KundeModify( true, Some("bezeichnung"), "strasse", Some("02"), None, "383832", "Bern", Some("Bern"), false, None, None, None, None, None, None, Some(""), Some(982121), Some(982121), Set.empty, Seq.empty   , Seq.empty   , None , None )

      Post("/kunden", create) ~> stammdatenRouteService.stammdatenRoute ~> check {
        expectDBEvents(1) { (creations, _, _, _) =>
          oneEventMatches[Kunde](creations)(_.bezeichnung === "bezeichnung")
        }
      }
    }

    "get kunde" in {
      Get("/kunden") ~> stammdatenRouteService.stammdatenRoute ~> check {
        val result = responseAs[List[Kunde]]

        result.size === 1
        result.head.strasse === "strasse"
      }
    }

    "modify kunde" in {
      val kunde = Await.result(stammdatenRouteService.stammdatenReadRepository.getKunden, defaultTimeout).head
      val modify = copyTo[Kunde, Kunde](kunde).copy(strasse = "STRASSE")
      val modify1 = copyTo[Kunde, KundeModify](modify, "pendenzen" -> Seq.empty, "ansprechpersonen" -> Seq.empty , "kontoDaten" -> None, "bezeichnung" -> None)
//      val modify = copyTo[Kunde, KundeModify](modify,"bezeichnung" -> (Some(kunde.bezeichnung): Option[String]),"pendenzen" -> Seq.empty, "ansprechpersonen" -> Seq.empty , "kontoDaten" -> None)

      Post(s"/kunden/${kunde.id.id}", modify1) ~> stammdatenRouteService.stammdatenRoute ~> check {
        expectDBEvents(1) { (_, modifications, _, _) =>
          oneEventMatches[Produzent](modifications)(_.strasse === "STRASSE")
        }
      }
    }
  }
}
