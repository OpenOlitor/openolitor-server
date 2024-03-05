package ch.openolitor.stammdaten

import ch.openolitor.core.SystemConfigReference
import ch.openolitor.stammdaten.repositories.ProjektReadRepositorySync
import scalikejdbc.DBSession

trait ProjektHelper extends SystemConfigReference {
  lazy val bccAddress = config.getString("smtp.bcc")

  def projektReadRepository: ProjektReadRepositorySync

  def determineBcc(implicit session: DBSession): Option[String] = {
    projektReadRepository.getProjekt flatMap { projekt =>
      Option.when(projekt.sendEmailToBcc)(bccAddress)
    }
  }
}
