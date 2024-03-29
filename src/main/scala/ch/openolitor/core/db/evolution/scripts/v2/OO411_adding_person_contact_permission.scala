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
import ch.openolitor.core.db.evolution.scripts.DefaultDBScripts
import ch.openolitor.stammdaten.StammdatenDBMappings
import ch.openolitor.arbeitseinsatz.ArbeitseinsatzDBMappings
import com.typesafe.scalalogging.LazyLogging
import scalikejdbc._

import scala.util.{ Success, Try }

object OO411_adding_person_contact_permission {

  val addingPersonContactPermission = new Script with LazyLogging with StammdatenDBMappings with DefaultDBScripts {
    def execute(sysConfig: SystemConfig)(implicit session: DBSession): Try[Boolean] = {
      alterTableAddColumnIfNotExists(personMapping, "contact_permission", "VARCHAR(1)", "categories")
      Success(true)
    }
  }
  val addingArbeitseinsatzContactPermission = new Script with LazyLogging with ArbeitseinsatzDBMappings with DefaultDBScripts {
    def execute(sysConfig: SystemConfig)(implicit session: DBSession): Try[Boolean] = {
      alterTableAddColumnIfNotExists(arbeitseinsatzMapping, "contact_permission", "VARCHAR(1)", "telefon_mobil")
      Success(true)
    }
  }
  val contactPermissionDefaultValue = new Script with LazyLogging with StammdatenDBMappings with DefaultDBScripts {
    def execute(sysConfig: SystemConfig)(implicit session: DBSession): Try[Boolean] = {
      sql"""UPDATE Person SET contact_permission = '0';""".execute.apply()
      sql"""UPDATE Arbeitseinsatz SET contact_permission = '0';""".execute.apply()

      Success(true)
    }
  }

  val scripts = Seq(addingPersonContactPermission, addingArbeitseinsatzContactPermission, contactPermissionDefaultValue)
}
