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

import ch.openolitor.arbeitseinsatz.models._
import ch.openolitor.core.db.OOAsyncDB._
import ch.openolitor.core.db._
import ch.openolitor.core.repositories._
import ch.openolitor.stammdaten.models.KundeId
import com.typesafe.scalalogging.LazyLogging
import scalikejdbc.async._

import scala.concurrent._

trait ArbeitseinsatzReadRepositoryAsync extends BaseReadRepositoryAsync {
  def getArbeitskategorien(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Arbeitskategorie]]

  def getArbeitsangebote(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Arbeitsangebot]]
  def getArbeitsangebot(arbeitsangebotId: ArbeitsangebotId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Arbeitsangebot]]
  def getFutureArbeitsangebote(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Arbeitsangebot]]
  def getArbeitseinsaetze(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Arbeitseinsatz]]
  def getArbeitseinsaetze(arbeitsangebotId: ArbeitsangebotId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Arbeitseinsatz]]
  def getArbeitseinsatz(arbeitseinsatzId: ArbeitseinsatzId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Arbeitseinsatz]]
  def getArbeitseinsaetze(kundeId: KundeId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Arbeitseinsatz]]
  def getFutureArbeitseinsaetze(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Arbeitseinsatz]]
  def getFutureArbeitseinsaetze(kundeId: KundeId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Arbeitseinsatz]]
  def getArbeitseinsatzabrechnung(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[ArbeitseinsatzAbrechnung]]
}

class ArbeitseinsatzReadRepositoryAsyncImpl extends ArbeitseinsatzReadRepositoryAsync with LazyLogging with ArbeitseinsatzRepositoryQueries {
  def getArbeitskategorien(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Arbeitskategorie]] = {
    getArbeitskategorienQuery.future
  }

  def getArbeitsangebote(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Arbeitsangebot]] = {
    getArbeitsangeboteQuery.future
  }

  def getArbeitsangebot(arbeitsangebotId: ArbeitsangebotId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Arbeitsangebot]] = {
    getArbeitsangebotQuery(arbeitsangebotId).future
  }

  def getFutureArbeitsangebote(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Arbeitsangebot]] = {
    getFutureArbeitsangeboteQuery.future
  }

  def getArbeitseinsaetze(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Arbeitseinsatz]] = {
    getArbeitseinsaetzeQuery.future
  }

  def getArbeitseinsaetze(arbeitsangebotId: ArbeitsangebotId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Arbeitseinsatz]] = {
    getArbeitseinsaetzeQuery(arbeitsangebotId).future
  }

  def getArbeitseinsaetze(kundeId: KundeId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Arbeitseinsatz]] = {
    getArbeitseinsaetzeQuery(kundeId).future
  }

  def getFutureArbeitseinsaetze(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Arbeitseinsatz]] = {
    getFutureArbeitseinsaetzeQuery.future
  }

  def getArbeitseinsatz(arbeitseinsatzId: ArbeitseinsatzId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Arbeitseinsatz]] = {
    getArbeitseinsatzQuery(arbeitseinsatzId).future
  }

  def getFutureArbeitseinsaetze(kundeId: KundeId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Arbeitseinsatz]] = {
    getFutureArbeitseinsaetzeQuery(kundeId).future
  }

  def getArbeitseinsatzabrechnung(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[ArbeitseinsatzAbrechnung]] = {
    getArbeitseinsatzabrechnungQuery.future
  }
}
