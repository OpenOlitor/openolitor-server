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
package ch.openolitor.buchhaltung.repositories

import scalikejdbc._
import ch.openolitor.core.repositories._
import ch.openolitor.core.repositories.BaseWriteRepository
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.EventStream

/**
 * Synchronous Repository
 */
trait BuchhaltungWriteRepository extends BuchhaltungReadRepositorySync
  with BuchhaltungInsertRepository
  with BuchhaltungUpdateRepository
  with BuchhaltungDeleteRepository
  with BaseWriteRepository
  with EventStream {
  def cleanupDatabase(implicit cpContext: ConnectionPoolContext): Unit
}

trait BuchhaltungWriteRepositoryImpl extends BuchhaltungReadRepositorySyncImpl
  with BuchhaltungInsertRepositoryImpl
  with BuchhaltungUpdateRepositoryImpl
  with BuchhaltungDeleteRepositoryImpl
  with BuchhaltungWriteRepository
  with LazyLogging
  with BuchhaltungRepositoryQueries {
  override def cleanupDatabase(implicit cpContext: ConnectionPoolContext): Unit = {
    DB autoCommit { implicit session =>
      sql"truncate table ${rechnungMapping.table}".execute.apply()
      sql"truncate table ${rechnungsPositionMapping.table}".execute.apply()
      sql"truncate table ${zahlungsImportMapping.table}".execute.apply()
      sql"truncate table ${zahlungsEingangMapping.table}".execute.apply()
    }
  }
}
