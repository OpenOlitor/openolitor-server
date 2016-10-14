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
import scalikejdbc.async._
import scala.concurrent.ExecutionContext
import ch.openolitor.core.db._
import ch.openolitor.core.db.OOAsyncDB._
import ch.openolitor.core.repositories._
import ch.openolitor.core.repositories.BaseRepository._
import scala.concurrent._
import ch.openolitor.arbeitseinsatz.models._
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.arbeitseinsatz.models._
import ch.openolitor.core.Macros._
import ch.openolitor.util.DateTimeUtil._
import org.joda.time.DateTime
import ch.openolitor.util.parsing.FilterExpr

trait ArbeitseinsatzReadRepository {
  def getArbeitseinsaetze(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Arbeitseinsatz]]
  def getFutureArbeitseinsaetze(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Arbeitseinsatz]]
}

class ArbeitseinsatzReadRepositoryImpl extends BaseReadRepository with ArbeitseinsatzReadRepository with LazyLogging with ArbeitseinsatzRepositoryQueries {
  def getArbeitseinsaetze(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Arbeitseinsatz]] = {
    getArbeitseinsaetzeQuery.future
  }

  def getFutureArbeitseinsaetze(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Arbeitseinsatz]] = {
    getFutureArbeitseinsaetzeQuery.future
  }
}
