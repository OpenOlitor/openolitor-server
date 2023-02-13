package ch.openolitor.stammdaten

import ch.openolitor.core.Macros.copyTo
import ch.openolitor.core.models.{ EntityCreated, EntityModified }
import ch.openolitor.core.security.Subject
import ch.openolitor.core.{ BaseRoutesWithDBSpec, SpecSubjects }
import ch.openolitor.stammdaten.models._
import org.joda.time.LocalDate

import scala.concurrent.Await

class StammdatenRoutesKundenSpec extends BaseRoutesWithDBSpec with SpecSubjects with StammdatenRouteServiceInteractions with StammdatenJsonProtocol {
  sequential

  protected val stammdatenRouteService = new MockStammdatenRoutes(sysConfig, system)

  implicit val subject: Subject = adminSubject

  "StammdatenRoutes for Kunden and Personen" should {
    "create Kunden" in {
      val person = PersonModify(None, None, "Name", "Vorname", None, None, None, None, Set.empty, None, None, false)
      val create = KundeModify(true, Some("bezeichnung"), "strasse", Some("02"), None, "383832", "Bern", Some("Bern"), false, None, None, None, None, None, None, Some(""), None, None, Set.empty, Seq.empty, Seq(person), None, None)

      Post("/kunden", create) ~> stammdatenRouteService.stammdatenRoute ~> check {
        //expectDBEvents(3) { (creations, _, _, _) =>
        //  oneEventMatches[Kunde](creations)(_.bezeichnung === "bezeichnung")
        //}
        dbEventProbe.expectMsgType[EntityCreated[Kunde]]
        dbEventProbe.expectMsgType[EntityCreated[KontoDaten]]
        dbEventProbe.expectMsgType[EntityCreated[Person]]
        dbEventProbe.expectNoMessage()

        val result = Await.result(stammdatenRouteService.stammdatenReadRepository.getKunden(asyncConnectionPoolContext), defaultTimeout)
        result.size === 2
        result.head.bezeichnung === "bezeichnung"
      }
    }

    "get kunde" in {
      Get("/kunden") ~> stammdatenRouteService.stammdatenRoute ~> check {
        val result = responseAs[List[KundeUebersicht]]

        result.size === 2
        result.find(k => k.bezeichnung == "bezeichnung").head.strasse === "strasse"
      }
    }

    "modify kunde" in {
      val kunde = Await.result(stammdatenRouteService.stammdatenReadRepository.getKunden, defaultTimeout).find(k => k.bezeichnung == "bezeichnung").head
      val person = Seq(PersonModify(None, None, "Name", "Vorname", None, None, None, None, Set.empty, None, None, false))
      val modify = copyTo[Kunde, Kunde](kunde).copy(strasse = "STRASSE")
      val modify1 = copyTo[Kunde, KundeModify](modify, "pendenzen" -> Seq.empty, "ansprechpersonen" -> person, "kontoDaten" -> None, "bezeichnung" -> None)

      Post(s"/kunden/${kunde.id.id}", modify1) ~> stammdatenRouteService.stammdatenRoute ~> check {
        dbEventProbe.expectMsgType[EntityModified[Kunde]]
        dbEventProbe.expectMsgType[EntityCreated[Person]]
        dbEventProbe.expectNoMessage()

        val result = Await.result(stammdatenRouteService.stammdatenReadRepository.getKunden(asyncConnectionPoolContext), defaultTimeout)
        result.size === 2
        result.head.strasse === "STRASSE"
      }
    }

    "create abo " in {
      val kunde = Await.result(stammdatenRouteService.stammdatenReadRepository.getKunden, defaultTimeout).head
      val abotyp = Await.result(stammdatenRouteService.stammdatenReadRepository.getAbotypen, defaultTimeout).head
      val vertrieb = Await.result(stammdatenRouteService.stammdatenReadRepository.getVertriebe(abotyp.id), defaultTimeout).head
      val vertriebSart = Await.result(stammdatenRouteService.stammdatenReadRepository.getVertriebsarten(vertrieb.id), defaultTimeout).head
      val depot = Await.result(stammdatenRouteService.stammdatenReadRepository.getDepots, defaultTimeout).head
      val aboCreate = DepotlieferungAboCreate(kunde.id, kunde.bezeichnung, vertriebSart.id, depot.id, LocalDate.now(), None, Some(BigDecimal(32)))

      Post(s"/kunden/${kunde.id.id}/abos", aboCreate) ~> stammdatenRouteService.stammdatenRoute ~> check {
        expectDBEvents(7) { (creations, _, _, _) =>
          oneEventMatches[DepotlieferungAbo](creations)(_.price === "32")
        }
        //val result = Await.result(stammdatenRouteService.stammdatenReadRepository.getKunden(asyncConnectionPoolContext), defaultTimeout)
        //result.size === 2
        //result.head.strasse === "STRASSE"
      }

    }
  }
}
