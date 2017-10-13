/*                                                                           *\
*    ____                   ____  ___ __                                      *
*   / __ \____  ___  ____  / __ \/ (_) /_____  _____                          *
*  / / / / __ \/ _ \/ __ \/ / / / / / __/ __ \/ ___/   OpenOlitor             *
* / /_/ / /_/ /  __/ / / / /_/ / / / /_/ /_/ / /       contributed by tegonal *
* \____/ .___/\___/_/ /_/\____/_/_/\__/\____/_/        http://openolitor.ch   *
*     /_/                                                                     *
*                                                                             *
* This program is free software: you can redistribute it and/or modify it     *
* under the terms of the GNU General Public License as published by           *
* the Free Software Foundation, either version 3 of the License,              *
* or (at your option) any later version.                                      *
*                                                                             *
* This program is distributed in the hope that it will be useful, but         *
* WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY  *
* or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for *
* more details.                                                               *
*                                                                             *
* You should have received a copy of the GNU General Public License along     *
* with this program. If not, see http://www.gnu.org/licenses/                 *
*                                                                             *
\*                                                                           */
package ch.openolitor.core.db.evolution.scripts.v2

import ch.openolitor.core.SystemConfig
import ch.openolitor.core.db.evolution.Script
import ch.openolitor.stammdaten.StammdatenDBMappings
import com.typesafe.scalalogging.LazyLogging
import scalikejdbc._
import scala.util.{ Success, Try }
import ch.openolitor.core.db.evolution.scripts.DefaultDBScripts
import scala.collection.Seq

object OO109_Arbeitseinsatz {
  val StammdatenScripts = new Script with LazyLogging with StammdatenDBMappings with DefaultDBScripts {
    def execute(sysConfig: SystemConfig)(implicit session: DBSession): Try[Boolean] = {
      alterTableAddColumnIfNotExists(projektMapping, "generierte_mails_senden", "VARCHAR(1)", "maintenance_mode")
      alterTableAddColumnIfNotExists(projektMapping, "einsatz_einheit", "VARCHAR(20)", "generierte_mails_senden")
      alterTableAddColumnIfNotExists(projektMapping, "einsatz_absage_vorlauf_tage", "DECIMAL(2,0)", "einsatz_einheit")
      alterTableAddColumnIfNotExists(projektMapping, "einsatz_show_liste_kunde", "VARCHAR(1)", "einsatz_absage_vorlauf_tage")

      alterTableAddColumnIfNotExists(abotypMapping, "anzahl_einsaetze", "int", "anzahl_abwesenheiten")
      alterTableAddColumnIfNotExists(zusatzAbotypMapping, "anzahl_einsaetze", "int", "anzahl_abwesenheiten")
      alterTableAddColumnIfNotExists(depotlieferungAboMapping, "anzahl_einsaetze", "VARCHAR(500)", "anzahl_lieferungen")
      alterTableAddColumnIfNotExists(heimlieferungAboMapping, "anzahl_einsaetze", "VARCHAR(500)", "anzahl_lieferungen")
      alterTableAddColumnIfNotExists(postlieferungAboMapping, "anzahl_einsaetze", "VARCHAR(500)", "anzahl_lieferungen")
      //alterTableAddColumnIfNotExists(zusatzAboMapping, "anzahl_einsaetze", "VARCHAR(500)", "anzahl_lieferungen")

      sql"""update Projekt set generierte_mails_senden = 1, einsatz_einheit = 'Halbtage',
      einsatz_absage_vorlauf_tage = 3, einsatz_show_liste_kunde = 1""".execute.apply()

      Success(true)

    }
  }
  val scripts = Seq(StammdatenScripts)
}
