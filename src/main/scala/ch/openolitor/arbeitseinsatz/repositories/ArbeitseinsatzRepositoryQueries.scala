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
import sqls.{ distinct, count }
import ch.openolitor.core.db._
import ch.openolitor.core.repositories._
import ch.openolitor.core.repositories.BaseRepository._
import ch.openolitor.arbeitseinsatz.models._
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.buchhaltung.models._
import ch.openolitor.core.Macros._
import ch.openolitor.util.DateTimeUtil._
import org.joda.time.DateTime
import ch.openolitor.arbeitseinsatz.ArbeitseinsatzDBMappings
import ch.openolitor.util.querybuilder.UriQueryParamToSQLSyntaxBuilder
import ch.openolitor.util.parsing.FilterExpr
import org.joda.time.LocalDate

trait ArbeitseinsatzRepositoryQueries extends LazyLogging with ArbeitseinsatzDBMappings {

  lazy val arbeitskategorieTyp = arbeitskategorieMapping.syntax("arbeitskategorie")
  lazy val arbeitsangebotTyp = arbeitsangebotMapping.syntax("arbeitsangebot")
  lazy val arbeitseinsatzTyp = arbeitseinsatzMapping.syntax("arbeitseinsatz")

  protected def getArbeitskategorienQuery = {
    withSQL {
      select
        .from(arbeitskategorieMapping as arbeitskategorieTyp)
    }.map(arbeitskategorieMapping(arbeitskategorieTyp)).list
  }

  protected def getFutureArbeitseinsaetzeQuery = {
    withSQL {
      select
        .from(arbeitseinsatzMapping as arbeitseinsatzTyp)
        .where.ge(arbeitseinsatzTyp.zeitVon, new DateTime())
        .orderBy(arbeitseinsatzTyp.zeitVon)
    }.map(arbeitseinsatzMapping(arbeitseinsatzTyp)).list
  }

  protected def getArbeitseinsaetzeQuery = {
    withSQL {
      select
        .from(arbeitseinsatzMapping as arbeitseinsatzTyp)
        .orderBy(arbeitseinsatzTyp.zeitVon)
    }.map(arbeitseinsatzMapping(arbeitseinsatzTyp)).list
  }

}
