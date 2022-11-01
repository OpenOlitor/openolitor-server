package ch.openolitor.stammdaten

import akka.http.scaladsl.model.StatusCodes
import ch.openolitor.core.{ BaseRoutesWithDBSpec, SpecSubjects }
import ch.openolitor.core.models.EntityCreated
import ch.openolitor.core.security.Subject
import ch.openolitor.stammdaten.models._
import org.joda.time.{ DateTime, LocalDate, Months }
import org.specs2.matcher.MatchResult
import spray.json.{ JsNumber, JsObject, JsString }

import scala.concurrent.Await

trait StammdatenRouteServiceInteractions extends BaseRoutesWithDBSpec with SpecSubjects with StammdatenJsonProtocol {
  import ch.openolitor.util.Fixtures._

  protected def createSimpleAbo(service: MockStammdatenRoutes)(implicit subject: Subject): MatchResult[_] = {
    Post("/kunden", kundeCreateUntertorOski) ~> service.stammdatenRoute ~> check {
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

  protected def createSimpleVertriebVertriebsart(service: MockStammdatenRoutes)(implicit subject: Subject): MatchResult[_] = {
    val json = JsObject(Map("depotId" -> JsNumber(depotWwgId.id), "abotypId" -> JsNumber(abotypId.id), "vertriebId" -> JsNumber(vertriebId.id), "typ" -> JsString("Depotlieferung")))

    Post(s"/abotypen/${abotypId.id}/vertriebe/${vertriebDonnerstag.id.id}/vertriebsarten", json) ~> service.stammdatenRoute ~> check {
      status === StatusCodes.Created

      dbEventProbe.expectMsgType[EntityCreated[Depotlieferung]]
      dbEventProbe.expectNoMessage()

      Await.result(service.stammdatenReadRepository.getVertriebsarten(vertriebDonnerstag.id), defaultTimeout).size === 1

      val lieferungenAbotypCreate = LieferungenAbotypCreate(abotypId, vertriebDonnerstag.id, (0 to 4).map(months => DateTime.now().withDayOfWeek(1).plusMonths(months)))

      Post(s"/abotypen/${abotypId.id}/vertriebe/${vertriebDonnerstag.id.id}/lieferungen/aktionen/generieren", lieferungenAbotypCreate) ~> service.stammdatenRoute ~> check {
        status === StatusCodes.Created

        expectDBEvents(5) { (creations, _, _, _) =>
          dbEventProbe.expectNoMessage()
          oneEventMatches[Lieferung](creations)(_.abotypId === abotypId)
        }
      }
    }
  }
}
