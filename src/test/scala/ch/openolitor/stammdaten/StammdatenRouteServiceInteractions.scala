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
  protected val stammdatenRouteService: MockStammdatenRoutes

  import ch.openolitor.util.Fixtures._

  protected def createDepotWwg()(implicit subject: Subject): MatchResult[_] = {
    Post("/depots", depotWwgModify) ~> stammdatenRouteService.stammdatenRoute ~> check {
      status === StatusCodes.Created

      dbEventProbe.expectMsgType[EntityCreated[Depot]]
      dbEventProbe.expectNoMessage()

      val result = Await.result(stammdatenRouteService.stammdatenReadRepository.getDepots, defaultTimeout)
      result.size === 1
    }
  }

  protected def createTour()(implicit subject: Subject): MatchResult[_] = {
    Post("/touren", tourCreate) ~> stammdatenRouteService.stammdatenRoute ~> check {
      status === StatusCodes.Created

      dbEventProbe.expectMsgType[EntityCreated[Tour]]
      dbEventProbe.expectNoMessage()

      val result = Await.result(stammdatenRouteService.stammdatenReadRepository.getTouren, defaultTimeout)
      result.size === 1
    }
  }

  protected def createAbotypWithVertriebeAndVertriebsarten()(implicit subject: Subject): MatchResult[_] = {
    Post("/abotypen", abotypVegiModify) ~> stammdatenRouteService.stammdatenRoute ~> check {
      status === StatusCodes.Created

      dbEventProbe.expectMsgType[EntityCreated[Abotyp]]

      val result = Await.result(stammdatenRouteService.stammdatenReadRepository.getAbotypen(asyncConnectionPoolContext, None, None), defaultTimeout)
      result.size === 1
      val abotypId = result(0).id

      Post(s"/abotypen/${abotypId.id}/vertriebe", vertriebDonnerstagModifyDepot) ~> stammdatenRouteService.stammdatenRoute ~> check {
        status === StatusCodes.Created

        val vertrieb = dbEventProbe.expectMsgType[EntityCreated[Vertrieb]]

        Await.result(stammdatenRouteService.stammdatenReadRepository.getVertriebe(abotypId), defaultTimeout).size === 1

        createDepotVertriebVertriebsart()
      }

      Post(s"/abotypen/${abotypId.id}/vertriebe", vertriebDonnerstagModifyTour) ~> stammdatenRouteService.stammdatenRoute ~> check {
        status === StatusCodes.Created

        val vertrieb = dbEventProbe.expectMsgType[EntityCreated[Vertrieb]]

        Await.result(stammdatenRouteService.stammdatenReadRepository.getVertriebe(abotypId), defaultTimeout).size === 2

        createTourVertriebVertriebsart()
      }

      Post(s"/abotypen/${abotypId.id}/vertriebe", vertriebDonnerstagModifyPost) ~> stammdatenRouteService.stammdatenRoute ~> check {
        status === StatusCodes.Created

        val vertrieb = dbEventProbe.expectMsgType[EntityCreated[Vertrieb]]

        Await.result(stammdatenRouteService.stammdatenReadRepository.getVertriebe(abotypId), defaultTimeout).size === 3

        createPostVertriebVertriebsart()
      }
    }
  }

  protected def createKunde(kundeModify: KundeModify)(implicit subject: Subject): MatchResult[_] = {
    Post("/kunden", kundeModify) ~> stammdatenRouteService.stammdatenRoute ~> check {
      status === StatusCodes.Created

      expectDBEvents(3) { (creations, _, _, _) =>
        dbEventProbe.expectNoMessage()
        oneEventMatches[Kunde](creations)(_.bezeichnung must contain(kundeModify.ansprechpersonen.head.name))
        oneEventMatches[KontoDaten](creations)(_.iban must beSome)
        oneEventMatches[Person](creations)(_.name must contain(kundeModify.ansprechpersonen.head.name))
      }
    }
  }

  protected def createDepotlieferungAbo(kundeModify: KundeModify)(implicit subject: Subject): MatchResult[_] = {
    val kunden = Await.result(stammdatenRouteService.stammdatenReadRepository.getKunden, defaultTimeout)
    val kunde = kunden.filter(_.bezeichnung.contains(kundeModify.ansprechpersonen.head.name)).head

    val abo = DepotlieferungAboCreate(kunde.id, s"DepotlieferungAbo für ${kunde.bezeichnung}", VertriebsartId(1), DepotId(1), LocalDate.now().withDayOfWeek(1).minus(Months.ONE), None, None)

    Post(s"/kunden/${kunde.id.id}/abos", abo) ~> stammdatenRouteService.stammdatenRoute ~> check {
      status === StatusCodes.Created

      expectDBEvents(6) { (creations, modifications, _, _) =>
        oneEventMatches[DepotlieferungAbo](creations)(_.aktiv must beTrue)

        oneEventMatches[Depot](modifications)(_.anzahlAbonnentenAktiv === 1)
        oneEventMatches[Abotyp](modifications)(_.anzahlAbonnentenAktiv === 1)
        oneEventMatches[Kunde](modifications)(_.anzahlAbosAktiv === 1)
        oneEventMatches[Vertrieb](modifications)(_.anzahlAbosAktiv === 1)

        val abo = oneOf[DepotlieferungAbo](creations)(_ => true)

        Await.result(stammdatenRouteService.stammdatenReadRepository.getAboDetail(abo.id), defaultTimeout) must beSome

        val guthabenModify = AboGuthabenModify(0, 12, "You deserve it")

        Post(s"/kunden/${kunde.id.id}/abos/${abo.id.id}/aktionen/guthabenanpassen", guthabenModify) ~> stammdatenRouteService.stammdatenRoute ~> check {
          status === StatusCodes.Accepted

          expectDBEvents(4) { (creations, modifications, _, _) =>
            creations.size === 1
            modifications.size === 3
            oneEventMatches[Pendenz](creations)(_.bemerkung.get must contain("You deserve it"))
            oneEventMatches[DepotlieferungAbo](modifications)(_.guthaben === 12)
          }
        }
      }
    }
  }

  protected def createTourlieferungAbo(kundeModify: KundeModify)(implicit subject: Subject): MatchResult[_] = {
    val kunden = Await.result(stammdatenRouteService.stammdatenReadRepository.getKunden, defaultTimeout)
    val kunde = kunden.filter(_.bezeichnung.contains(kundeModify.ansprechpersonen.head.name)).head

    val abo = HeimlieferungAboCreate(kunde.id, s"HeimlieferungAbo (Tour) für ${kunde.bezeichnung}", VertriebsartId(2), TourId(1), LocalDate.now().withDayOfWeek(1).minus(Months.ONE), None, None)

    Post(s"/kunden/${kunde.id.id}/abos", abo) ~> stammdatenRouteService.stammdatenRoute ~> check {
      status === StatusCodes.Created

      expectDBEvents(7) { (creations, modifications, _, _) =>
        oneEventMatches[HeimlieferungAbo](creations)(_.aktiv must beTrue)
        oneEventMatches[Tourlieferung](creations)(_.kundeBezeichnung === kundeModify.bezeichnung)

        oneEventMatches[Tour](modifications)(_.anzahlAbonnentenAktiv === 1)
        oneEventMatches[Abotyp](modifications)(_.anzahlAbonnentenAktiv === 1)
        oneEventMatches[Kunde](modifications)(_.anzahlAbosAktiv === 1)
        oneEventMatches[Vertrieb](modifications)(_.anzahlAbosAktiv === 1)

        val abo = oneOf[HeimlieferungAbo](creations)(_ => true)

        Await.result(stammdatenRouteService.stammdatenReadRepository.getAboDetail(abo.id), defaultTimeout) must beSome

        val guthabenModify = AboGuthabenModify(0, 12, "You deserve it")

        Post(s"/kunden/${kunde.id.id}/abos/${abo.id.id}/aktionen/guthabenanpassen", guthabenModify) ~> stammdatenRouteService.stammdatenRoute ~> check {
          status === StatusCodes.Accepted

          expectDBEvents(4) { (creations, modifications, _, _) =>
            creations.size === 1
            modifications.size === 3

            oneEventMatches[Pendenz](creations)(_.bemerkung.get must contain("You deserve it"))
            oneEventMatches[HeimlieferungAbo](modifications)(_.guthaben === 12)
          }
        }
      }
    }
  }

  protected def createPostlieferungAbo(kundeModify: KundeModify)(implicit subject: Subject): MatchResult[_] = {
    val kunden = Await.result(stammdatenRouteService.stammdatenReadRepository.getKunden, defaultTimeout)
    val kunde = kunden.filter(_.bezeichnung.contains(kundeModify.ansprechpersonen.head.name)).head

    val abo = PostlieferungAboCreate(kunde.id, s"PostlieferungAbo für ${kunde.bezeichnung}", VertriebsartId(3), LocalDate.now().withDayOfWeek(1).minus(Months.ONE), None, None)

    Post(s"/kunden/${kunde.id.id}/abos", abo) ~> stammdatenRouteService.stammdatenRoute ~> check {
      status === StatusCodes.Created

      expectDBEvents(7) { (creations, modifications, _, _) =>
        oneEventMatches[PostlieferungAbo](creations)(_.aktiv must beTrue)

        oneEventMatches[Abotyp](modifications)(_.anzahlAbonnentenAktiv === 1)
        oneEventMatches[Kunde](modifications)(_.anzahlAbosAktiv === 1)
        oneEventMatches[Vertrieb](modifications)(_.anzahlAbosAktiv === 1)

        val abo = oneOf[PostlieferungAbo](creations)(_ => true)

        Await.result(stammdatenRouteService.stammdatenReadRepository.getAboDetail(abo.id), defaultTimeout) must beSome

        val guthabenModify = AboGuthabenModify(0, 12, "You deserve it")

        Post(s"/kunden/${kunde.id.id}/abos/${abo.id.id}/aktionen/guthabenanpassen", guthabenModify) ~> stammdatenRouteService.stammdatenRoute ~> check {
          status === StatusCodes.Accepted

          expectDBEvents(4) { (creations, modifications, _, _) =>
            creations.size === 1
            modifications.size === 3

            oneEventMatches[Pendenz](creations)(_.bemerkung.get must contain("You deserve it"))
            oneEventMatches[PostlieferungAbo](modifications)(_.guthaben === 12)
          }
        }
      }
    }
  }

  protected def createDepotVertriebVertriebsart()(implicit subject: Subject): MatchResult[_] = {
    val json = JsObject(Map("depotId" -> JsNumber(depotWwgId.id), "abotypId" -> JsNumber(abotypId.id), "vertriebId" -> JsNumber(vertriebIdDepot.id), "typ" -> JsString("Depotlieferung")))
    createVertriebVertriebsart(json, vertriebIdDepot)

  }
  protected def createTourVertriebVertriebsart()(implicit subject: Subject): MatchResult[_] = {
    val json = JsObject(Map("tourId" -> JsNumber(tourId.id), "abotypId" -> JsNumber(abotypId.id), "vertriebId" -> JsNumber(vertriebIdTour.id), "typ" -> JsString("Heimlieferung")))
    createVertriebVertriebsart(json, vertriebIdTour)
  }

  protected def createPostVertriebVertriebsart()(implicit subject: Subject): MatchResult[_] = {
    val json = JsObject(Map("tourId" -> JsNumber(tourId.id), "abotypId" -> JsNumber(abotypId.id), "vertriebId" -> JsNumber(vertriebIdPost.id), "typ" -> JsString("Postlieferung")))
    createVertriebVertriebsart(json, vertriebIdPost)
  }

  private def createVertriebVertriebsart(json: JsObject, vertriebId: VertriebId)(implicit subject: Subject): MatchResult[_] = {
    Post(s"/abotypen/${abotypId.id}/vertriebe/${vertriebId.id}/vertriebsarten", json) ~> stammdatenRouteService.stammdatenRoute ~> check {
      status === StatusCodes.Created

      expectDBEvents(1) { (creations, _, _, _) =>
        dbEventProbe.expectNoMessage()
        creations.size === 1
      }

      val lieferungenAbotypCreate = LieferungenAbotypCreate(abotypId, vertriebId, (0 to 4).map(months => DateTime.now().withTimeAtStartOfDay().withDayOfWeek(4).plusMonths(months)))

      Post(s"/abotypen/${abotypId.id}/vertriebe/${vertriebId.id}/lieferungen/aktionen/generieren", lieferungenAbotypCreate) ~> stammdatenRouteService.stammdatenRoute ~> check {
        status === StatusCodes.Created

        expectDBEvents(5) { (creations, _, _, _) =>
          dbEventProbe.expectNoMessage()
          oneEventMatches[Lieferung](creations)(_.abotypId === abotypId)
        }
      }
    }
  }

  protected def createLieferplanung()(implicit subject: Subject): MatchResult[_] = {
    val lieferplanungCreate = LieferplanungCreate(None)

    Post("/lieferplanungen", lieferplanungCreate) ~> stammdatenRouteService.stammdatenRoute ~> check {
      status === StatusCodes.Created

      expectDBEvents(12) { (creations, _, _, _) =>
        oneEventMatches[Lieferplanung](creations)(_.status === Offen)
        allEventsMatch[Korb](creations)(_.status === WirdGeliefert)
      }
    }
  }

  protected def closeLieferplanung()(implicit subject: Subject): MatchResult[_] = {
    val lieferplanung = Await.result(stammdatenRouteService.stammdatenReadRepository.getLatestLieferplanung, defaultTimeout).get

    Post(s"/lieferplanungen/${lieferplanung.id.id}/aktionen/abschliessen") ~> stammdatenRouteService.stammdatenRoute ~> check {
      status === StatusCodes.OK

      expectDBEvents(12) { (creations, modifications, _, _) =>
        creations.size === 3
        modifications.size === 9

        oneEventMatches[DepotAuslieferung](creations)(_.status === Erfasst)
        oneEventMatches[TourAuslieferung](creations)(_.status === Erfasst)
        oneEventMatches[PostAuslieferung](creations)(_.status === Erfasst)
      }
    }
  }

  protected def createTrinityOfAbos()(implicit subject: Subject): MatchResult[_] = {
    createDepotWwg()

    createTour()

    createAbotypWithVertriebeAndVertriebsarten()

    createKunde(kundeCreateUntertorOski)
    createKunde(kundeCreateMatteEdi)
    createKunde(kundeCreateHaseFritz)

    createDepotlieferungAbo(kundeCreateUntertorOski)
    createTourlieferungAbo(kundeCreateMatteEdi)
    createPostlieferungAbo(kundeCreateHaseFritz)
  }
}
