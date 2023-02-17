package ch.openolitor.kundenportal

import ch.openolitor.buchhaltung.BuchhaltungJsonProtocol
import ch.openolitor.core.db.WithWriteRepositories
import ch.openolitor.core.filestore.FileType
import ch.openolitor.core.security.Subject
import ch.openolitor.core.{ BaseRoutesWithDBSpec, SpecSubjects }
import ch.openolitor.stammdaten.models._
import ch.openolitor.stammdaten.{ MockStammdatenRoutes, StammdatenJsonProtocol, StammdatenRouteServiceInteractions }

import scala.concurrent.Await

class KundenportalRoutesProjectSpec extends BaseRoutesWithDBSpec with SpecSubjects with StammdatenRouteServiceInteractions with StammdatenJsonProtocol with BuchhaltungJsonProtocol with WithWriteRepositories {
  sequential

  import ch.openolitor.util.Fixtures._

  protected var oskiKunde: KundeDetail = null
  protected lazy val oskiSubject: Subject = Subject("oski", oskiKunde.ansprechpersonen.head.id, oskiKunde.id, Some(KundenZugang), None)
  protected val stammdatenRouteService = new MockStammdatenRoutes(sysConfig, system)
  protected val kundenportalRouteService = new MockKundenportalRoutes(sysConfig, system)

  override def beforeAll() = {
    super.beforeAll()
    oskiKunde = setupProjekt()
  }

  "KundenportalRoutes for Project" should {
    "get project" in {
      implicit val subject = oskiSubject
      Get(s"/kundenportal/projekt") ~> kundenportalRouteService.kundenportalRoute ~> check {
        val projekt = responseAs[Option[ProjektKundenportal]]
        projekt.head.bezeichnung === "Demo Projekt"
      }
    }
  }
  private def setupProjekt(): KundeDetail = {
    implicit val adminPersonId = adminSubject.personId
    implicit val subject = adminSubject

    createTrinityOfAbos()
    val kunde = Await.result(stammdatenRouteService.stammdatenReadRepository.getKunden, defaultTimeout).filter(_.bezeichnung.contains("Oski")).head

    Await.result(stammdatenRouteService.stammdatenReadRepository.getKundeDetail(kunde.id), defaultTimeout).get
  }
}
