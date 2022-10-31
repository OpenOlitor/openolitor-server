package ch.openolitor.stammdaten

import akka.http.scaladsl.model.StatusCodes
import ch.openolitor.core.{ BaseRoutesWithDBSpec, SpecSubjects }
import ch.openolitor.core.models.EntityCreated
import ch.openolitor.core.security.Subject
import ch.openolitor.stammdaten.models.{ LieferungenAbotypCreate, _ }
import org.joda.time.{ DateTime, LocalDate, Months }
import spray.json.{ JsNumber, JsObject, JsString }

import scala.concurrent.Await

class StammdatenRoutesLieferplanungSpec extends BaseRoutesWithDBSpec with SpecSubjects with StammdatenJsonProtocol {
  sequential

  private val service = new MockStammdatenRoutes(sysConfig, system)
  implicit val subject: Subject = adminSubject

  "StammdatenRoutes for Lieferplanung" should {
    "create Depot" in {
      val depot = DepotModify("Deep Oh", "DEP", None, None, None, None, None, None, None, None, Some("Wasserwerkgasse"), None, "3011", "Bern", true, None, Some("#00ccff"), None, None, None, None)

      Post("/depots", depot) ~> service.stammdatenRoute ~> check {
        status === StatusCodes.Created

        // wait for modification to happen
        dbEventProbe.expectMsgType[EntityCreated[Depot]]

        val result = Await.result(service.stammdatenReadRepository.getDepots, defaultTimeout)
        result.size === 1
      }
    }

    "create Abotyp with Vertrieb and Vertriebsart" in {
      val abotyp = AbotypModify("Vegi", None, Woechentlich, Some(LocalDate.now().withDayOfWeek(1).minus(Months.TWO)), None, BigDecimal(17.5), ProQuartal, None, Unbeschraenkt, Some(Frist(6, Monatsfrist)), Some(Frist(1, Monatsfrist)), Some(4),
        None, "#ff0000", Some(BigDecimal(17.0)), 2, BigDecimal(12), true)
      val depot = Await.result(service.stammdatenReadRepository.getDepots, defaultTimeout).head

      Post("/abotypen", abotyp) ~> service.stammdatenRoute ~> check {
        status === StatusCodes.Created

        // wait for modification to happen
        dbEventProbe.expectMsgType[EntityCreated[Abotyp]]

        val result = Await.result(service.stammdatenReadRepository.getAbotypen(asyncConnectionPoolContext, None, None), defaultTimeout)
        result.size === 1
        val abotypId = result(0).id

        Post(s"/abotypen/${abotypId.id}/vertriebe", VertriebModify(abotypId, Donnerstag, None)) ~> service.stammdatenRoute ~> check {
          status === StatusCodes.Created

          val vertrieb = dbEventProbe.expectMsgType[EntityCreated[Vertrieb]]

          Await.result(service.stammdatenReadRepository.getVertriebe(abotypId), defaultTimeout).size === 1

          val json = JsObject(Map("depotId" -> JsNumber(depot.id.id), "abotypId" -> JsNumber(abotypId.id), "vertriebId" -> JsNumber(vertrieb.entity.id.id), "typ" -> JsString("Depotlieferung")))

          Post(s"/abotypen/${abotypId.id}/vertriebe/${vertrieb.entity.id.id}/vertriebsarten", json) ~> service.stammdatenRoute ~> check {
            status === StatusCodes.Created

            dbEventProbe.expectMsgType[EntityCreated[Depotlieferung]]

            Await.result(service.stammdatenReadRepository.getVertriebsarten(vertrieb.entity.id), defaultTimeout).size === 1

            val lieferungenAbotypCreate = LieferungenAbotypCreate(abotypId, vertrieb.entity.id, (0 to 4).map(months => DateTime.now().withDayOfWeek(1).plusMonths(months)))

            Post(s"/abotypen/${abotypId.id}/vertriebe/${vertrieb.entity.id.id}/lieferungen/aktionen/generieren", lieferungenAbotypCreate) ~> service.stammdatenRoute ~> check {
              status === StatusCodes.Created

              expectCRUDEvents(5) { (creations, _, _) =>
                withEvents[Lieferung](creations)(_.abotypId === abotypId)
              }
            }
          }
        }
      }
    }

    "create ZusatzAbotyp" in {
      val zusatzAbotyp = ZusatzAbotypModify("Eier", None, Some(LocalDate.now().withDayOfWeek(1).minus(Months.TWO)), None, BigDecimal(5.2), ProLieferung, None, Unbeschraenkt, Some(Frist(2, Monatsfrist)), Some(Frist(1, Monatsfrist)), Some(2), None, "#ffcc00", Some(BigDecimal(5)), BigDecimal(10), true, CHF)

      Post("/zusatzAbotypen", zusatzAbotyp) ~> service.stammdatenRoute ~> check {
        status === StatusCodes.Created

        // wait for modification to happen
        dbEventProbe.expectMsgType[EntityCreated[ZusatzAbotyp]]

        val result = Await.result(service.stammdatenReadRepository.getZusatzAbotypen(asyncConnectionPoolContext, None, None), defaultTimeout)
        result.size === 1
      }
    }

    "create Kunde with Abo" in {
      val kundeCreate = KundeModify(true, None, "Wasserwerkgasse", Some("2"), None, "3011", "Bern", None, false, None, None, None, None, None, None, None, None, None, Set(), Seq(), Seq(PersonModify(
        None, None, "Untertor", "Oski", Some("info@example.com"), None, None, None, Set(), None, None, false
      )), None, None)

      Post("/kunden", kundeCreate) ~> service.stammdatenRoute ~> check {
        status === StatusCodes.Created

        // wait for modification to happen
        val kunde = dbEventProbe.expectMsgType[EntityCreated[Kunde]]
        dbEventProbe.expectMsgType[EntityCreated[KontoDaten]]
        dbEventProbe.expectMsgType[EntityCreated[Person]]

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

            expectCRUDEvents(9) { (creations, modifications, _) =>
              creations.size === 1
              modifications.size === 8

              withEvents[Pendenz](creations)(_.bemerkung.get must contain("You deserve it"))

              withEvents[Depot](modifications)(_.anzahlAbonnentenAktiv === 1)
              withEvents[Abotyp](modifications)(_.anzahlAbonnentenAktiv === 1)
              withEvents[Kunde](modifications)(_.anzahlAbosAktiv === 1)
              withEvents[Vertrieb](modifications)(_.anzahlAbosAktiv === 1)
              withEvents[Depotlieferung](modifications)(_.anzahlAbosAktiv === 1)
              withEvents[DepotlieferungAbo](modifications)(_.guthaben === 12)
            }
          }
        }
      }
    }

    "create Lieferplanung" in {
      val lieferplanungCreate = LieferplanungCreate(None)

      Post("/lieferplanungen", lieferplanungCreate) ~> service.stammdatenRoute ~> check {
        status === StatusCodes.Created

        val lieferplanung = dbEventProbe.expectMsgType[EntityCreated[Lieferplanung]]

        lieferplanung.entity.status === Offen
      }
    }
  }
}
