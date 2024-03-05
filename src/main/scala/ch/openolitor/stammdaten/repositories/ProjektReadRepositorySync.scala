package ch.openolitor.stammdaten.repositories

import ch.openolitor.stammdaten.models.Projekt
import scalikejdbc.DBSession

trait ProjektReadRepositorySync {
  def getProjekt(implicit session: DBSession): Option[Projekt]
}

trait ProjektReadRepositorySyncImpl extends ProjektReadRepositorySync with StammdatenProjektRepositoryQueries {
  def getProjekt(implicit session: DBSession): Option[Projekt] = {
    getProjektQuery.apply()
  }
}
