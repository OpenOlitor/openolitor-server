package ch.openolitor.kundenportal

import ch.openolitor.buchhaltung.BuchhaltungJsonProtocol
import ch.openolitor.core.{ BaseRoutesWithDBSpec, SpecSubjects }
import ch.openolitor.core.db.WithWriteRepositories
import ch.openolitor.core.security.Subject
import ch.openolitor.stammdaten.{ MockStammdatenRoutes, StammdatenJsonProtocol, StammdatenRouteServiceInteractions }
import ch.openolitor.stammdaten.models._

import scala.concurrent.Await

class KundenportalRoutesAbosSpec extends BaseRoutesWithDBSpec with SpecSubjects with StammdatenRouteServiceInteractions with StammdatenJsonProtocol with BuchhaltungJsonProtocol with WithWriteRepositories {
  sequential

  import ch.openolitor.util.Fixtures._

  protected val stammdatenRouteService = new MockStammdatenRoutes(sysConfig, system)
  protected val kundenportalRouteService = new MockKundenportalRoutes(sysConfig, system)

  protected var oskiKunde: KundeDetail = null
  protected lazy val oskiSubject: Subject = Subject("oski", oskiKunde.ansprechpersonen.head.id, oskiKunde.id, Some(KundenZugang), None)

  override def beforeAll() = {
    super.beforeAll()

    oskiKunde = setupAbo()
  }

  "KundenportalRoutes for Abos" should {
    "list Abos" in {
      implicit val subject = oskiSubject
      Get(s"/kundenportal/abos") ~> kundenportalRouteService.kundenportalRoute ~> check {
        val abos = responseAs[List[AboDetail]]
        abos.size === 1
        abos.head.abotypId === abotypId
      }
    }

    "list Lieferungen" in {
      implicit val subject = oskiSubject

      Get(s"/kundenportal/abos/${abotypId.id}/vertriebe/${vertriebIdDepot.id}/lieferungen") ~> kundenportalRouteService.kundenportalRoute ~> check {
        val result = responseAs[List[LieferungDetail]]

        result.size === 1

        result.head.status === Abgeschlossen
      }
    }
  }

  private def setupAbo(): KundeDetail = {
    implicit val adminPersonId = adminSubject.personId
    implicit val subject = adminSubject

    createTrinityOfAbos()

    createLieferplanung()

    closeLieferplanung()

    val kunde = Await.result(stammdatenRouteService.stammdatenReadRepository.getKunden, defaultTimeout).filter(_.bezeichnung.contains("Oski")).head

    Await.result(stammdatenRouteService.stammdatenReadRepository.getKundeDetail(kunde.id), defaultTimeout).get
  }
}
