package ch.openolitor.stammdaten.repositories

import ch.openolitor.stammdaten.StammdatenDBMappings

import scalikejdbc._

trait StammdatenProjektRepositoryQueries extends StammdatenDBMappings {
  lazy val projekt = projektMapping.syntax("projekt")

  protected def getProjektQuery = {
    withSQL {
      select
        .from(projektMapping as projekt)
    }.map(projektMapping(projekt)).single
  }
}
