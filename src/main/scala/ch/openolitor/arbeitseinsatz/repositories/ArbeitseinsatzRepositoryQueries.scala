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
import scalikejdbc.async.FutureImplicits._
import ch.openolitor.core.db._
import ch.openolitor.core.db.OOAsyncDB._
import ch.openolitor.core.repositories._
import ch.openolitor.core.repositories.BaseWriteRepository
import scala.concurrent._
import ch.openolitor.stammdaten.models._
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.EventStream
import ch.openolitor.arbeitseinsatz.models._
import ch.openolitor.core.Macros._
import ch.openolitor.stammdaten.StammdatenDBMappings
import ch.openolitor.util.parsing.FilterExpr
import ch.openolitor.util.querybuilder.UriQueryParamToSQLSyntaxBuilder
import ch.openolitor.arbeitseinsatz.ArbeitseinsatzDBMappings
import ch.openolitor.stammdaten.models.KundeId
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

trait ArbeitseinsatzRepositoryQueries extends LazyLogging with ArbeitseinsatzDBMappings {

  lazy val arbeitskategorie = arbeitskategorieMapping.syntax("arbeitskategorie")
  lazy val arbeitsangebot = arbeitsangebotMapping.syntax("arbeitsangebot")
  lazy val arbeitseinsatz = arbeitseinsatzMapping.syntax("arbeitseinsatz")

  protected def getArbeitskategorienQuery = {
    withSQL {
      select
        .from(arbeitskategorieMapping as arbeitskategorie)
    }.map(arbeitskategorieMapping(arbeitskategorie)).list
  }

  protected def getArbeitsangeboteQuery = {
    withSQL {
      select
        .from(arbeitsangebotMapping as arbeitsangebot)
        .orderBy(arbeitsangebot.zeitVon)
    }.map(arbeitsangebotMapping(arbeitsangebot)).list
  }

  protected def getArbeitsangebotQuery(arbeitsangebotId: ArbeitsangebotId) = {
    withSQL {
      select
        .from(arbeitsangebotMapping as arbeitsangebot)
        .where.eq(arbeitsangebot.id, arbeitsangebotId)
    }.map(arbeitsangebotMapping(arbeitsangebot)).single
  }

  protected def getFutureArbeitsangeboteQuery = {
    withSQL {
      select
        .from(arbeitsangebotMapping as arbeitsangebot)
        .where.ge(arbeitsangebot.zeitVon, new DateTime())
        .orderBy(arbeitsangebot.zeitVon)
    }.map(arbeitsangebotMapping(arbeitsangebot)).list
  }

  protected def getArbeitseinsaetzeQuery = {
    withSQL {
      select
        .from(arbeitseinsatzMapping as arbeitseinsatz)
        .orderBy(arbeitseinsatz.zeitVon)
    }.map(arbeitseinsatzMapping(arbeitseinsatz)).list
  }

  protected def getArbeitseinsatzQuery(arbeitseinsatzId: ArbeitseinsatzId) = {
    withSQL {
      select
        .from(arbeitseinsatzMapping as arbeitseinsatz)
        .where.eq(arbeitseinsatz.id, arbeitseinsatzId)
    }.map(arbeitseinsatzMapping(arbeitseinsatz)).single
  }

  protected def getArbeitseinsaetzeQuery(kundeId: KundeId) = {
    withSQL {
      select
        .from(arbeitseinsatzMapping as arbeitseinsatz)
        .where.eq(arbeitseinsatz.kundeId, kundeId)
        .orderBy(arbeitseinsatz.zeitVon)
    }.map(arbeitseinsatzMapping(arbeitseinsatz)).list
  }

  protected def getFutureArbeitseinsaetzeQuery = {
    withSQL {
      select
        .from(arbeitseinsatzMapping as arbeitseinsatz)
        .where.ge(arbeitseinsatz.zeitVon, new DateTime())
        .orderBy(arbeitseinsatz.zeitVon)
    }.map(arbeitseinsatzMapping(arbeitseinsatz)).list
  }

  protected def getFutureArbeitseinsaetzeQuery(kundeId: KundeId) = {
    withSQL {
      select
        .from(arbeitseinsatzMapping as arbeitseinsatz)
        .where.ge(arbeitseinsatz.zeitVon, new DateTime())
        .and.eq(arbeitseinsatz.kundeId, kundeId)
        .orderBy(arbeitseinsatz.zeitVon)
    }.map(arbeitseinsatzMapping(arbeitseinsatz)).list
  }

}
