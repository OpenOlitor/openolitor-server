package ch.openolitor.core.db.evolution.scripts.v2

import ch.openolitor.core.SystemConfig
import ch.openolitor.core.db.evolution.Script
import ch.openolitor.core.db.evolution.scripts.DefaultDBScripts
import ch.openolitor.stammdaten.StammdatenDBMappings
import com.typesafe.scalalogging.LazyLogging
import scalikejdbc._

import scala.util.{ Success, Try }

object OO69_kunde_aktiv {
  val AddUserAktivFlag = new Script with LazyLogging with StammdatenDBMappings with DefaultDBScripts {
    def execute(sysConfig: SystemConfig)(implicit session: DBSession): Try[Boolean] = {
      alterTableAddColumnIfNotExists(kundeMapping, "aktiv", "varchar(1) not null default 1", "id")

      sql"""UPDATE ${kundeMapping.table} AS k
            SET k.aktiv = 1""".execute.apply()
      Success(true)
    }
  }

  val scripts = Seq(AddUserAktivFlag)
}
