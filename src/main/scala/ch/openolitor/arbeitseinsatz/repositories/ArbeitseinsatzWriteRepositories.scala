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
package ch.openolitor.arbeitseinsatz.repositories

import ch.openolitor.core.models._
import scalikejdbc._
import ch.openolitor.core.repositories.BaseWriteRepository
import ch.openolitor.arbeitseinsatz._
import ch.openolitor.arbeitseinsatz.models._
import org.joda.time.DateTime
import akka.actor.ActorSystem
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.AkkaEventStream
import ch.openolitor.core.EventStream

trait ArbeitseinsatzWriteRepository extends ArbeitseinsatzReadRepositorySync
    with ArbeitseinsatzInsertRepository
    with ArbeitseinsatzUpdateRepository
    with ArbeitseinsatzDeleteRepository
    with BaseWriteRepository
    with EventStream {
  def cleanupDatabase(implicit cpContext: ConnectionPoolContext)
}

trait ArbeitseinsatzWriteRepositoryImpl extends ArbeitseinsatzReadRepositorySyncImpl
    with ArbeitseinsatzInsertRepositoryImpl
    with ArbeitseinsatzUpdateRepositoryImpl
    with ArbeitseinsatzDeleteRepositoryImpl
    with ArbeitseinsatzWriteRepository
    with LazyLogging
    with ArbeitseinsatzRepositoryQueries {
  override def cleanupDatabase(implicit cpContext: ConnectionPoolContext) = {
    DB autoCommit { implicit session =>
      sql"truncate table ${arbeitskategorieMapping.table}".execute.apply()
      sql"truncate table ${arbeitsangebotMapping.table}".execute.apply()
      sql"truncate table ${arbeitseinsatzMapping.table}".execute.apply()
    }
  }

  //def deleteLieferpositionen(id: LieferungId)(implicit session: DBSession): Int = {
  //  deleteLieferpositionenQuery(id).update.apply
  //}

}
