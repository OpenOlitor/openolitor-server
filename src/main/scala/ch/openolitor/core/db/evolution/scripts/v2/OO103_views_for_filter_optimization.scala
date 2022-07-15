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

/**
 * Remaining indexes from OO597_DBScripts
 */
object OO103_views_for_filter_optimization {
  val CreateKundenViewForFilterOptimization = new Script with LazyLogging with StammdatenDBMappings {
    def execute(sysConfig: SystemConfig)(implicit session: DBSession): Try[Boolean] = {
      sql"""
        CREATE VIEW if not exists `KundenSearch` AS
          select
            k.id AS id,
            k.bezeichnung AS bezeichnung,
            concat(k.bezeichnung, ',', (
              select group_concat(p.name, ',', p.vorname separator ',')
              from Person p
              where k.id = p.kunde_id)
            ) AS kunden_search_values
          from Kunde as k;
      """.execute.apply()
      Success(true)
    }
  }

  val CreatePersonenViewForFilterOptimization = new Script with LazyLogging with StammdatenDBMappings {
    def execute(sysConfig: SystemConfig)(implicit session: DBSession): Try[Boolean] = {
      sql"""
        CREATE VIEW if not exists `PersonenSearch` AS
          select
            p.id AS id,
            concat(p.name, ',', p.vorname, ',' , p.email) AS personen_search_values
          from Person as p;
      """.execute.apply()
      Success(true)
    }
  }

  val scripts = Seq(CreateKundenViewForFilterOptimization, CreatePersonenViewForFilterOptimization)
}
