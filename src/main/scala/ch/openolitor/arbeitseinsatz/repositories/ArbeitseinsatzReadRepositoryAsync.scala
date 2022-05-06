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
import scalikejdbc.async.{ makeSQLToOptionAsync => _, makeSQLToListAsync => _, _ }

import scala.concurrent._

trait ArbeitseinsatzReadRepositoryAsync extends BaseReadRepositoryAsync {
  def getArbeitskategorien(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Arbeitskategorie]]

  def getArbeitsangebote(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Arbeitsangebot]]
  def getArbeitsangebot(arbeitsangebotId: ArbeitsangebotId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Arbeitsangebot]]
  def getFutureArbeitsangebote(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Arbeitsangebot]]
  def getArbeitseinsaetze(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Arbeitseinsatz]]
  def getArbeitseinsaetze(arbeitsangebotId: ArbeitsangebotId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Arbeitseinsatz]]
  def getArbeitseinsatz(arbeitseinsatzId: ArbeitseinsatzId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Arbeitseinsatz]]
  def getArbeitseinsatzDetail(arbeitseinsatzId: ArbeitseinsatzId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[ArbeitseinsatzDetail]]
  def getArbeitseinsaetze(kundeId: KundeId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Arbeitseinsatz]]
  def getFutureArbeitseinsaetze(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Arbeitseinsatz]]
  def getFutureArbeitseinsaetze(kundeId: KundeId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Arbeitseinsatz]]
  def getArbeitseinsatzabrechnung(xFlags: Option[ArbeitsComplexFlags])(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[ArbeitseinsatzAbrechnung]]
  def getArbeitseinsatzDetailByArbeitsangebot(arbeitsangebotId: ArbeitsangebotId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[ArbeitseinsatzDetail]]
}

class ArbeitseinsatzReadRepositoryAsyncImpl extends ArbeitseinsatzReadRepositoryAsync with LazyLogging with ArbeitseinsatzRepositoryQueries {
  def getArbeitskategorien(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Arbeitskategorie]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getArbeitskategorienQuery.future()
  }

  def getArbeitsangebote(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Arbeitsangebot]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getArbeitsangeboteQuery.future()
  }

  def getArbeitsangebot(arbeitsangebotId: ArbeitsangebotId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Arbeitsangebot]] = {
    import scalikejdbc.async.makeSQLToOptionAsync
    getArbeitsangebotQuery(arbeitsangebotId).future()
  }

  def getFutureArbeitsangebote(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Arbeitsangebot]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getFutureArbeitsangeboteQuery.future()
  }

  def getArbeitseinsaetze(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Arbeitseinsatz]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getArbeitseinsaetzeQuery.future()
  }

  def getArbeitseinsaetze(arbeitsangebotId: ArbeitsangebotId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Arbeitseinsatz]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getArbeitseinsaetzeQuery(arbeitsangebotId).future()
  }

  def getArbeitseinsaetze(kundeId: KundeId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Arbeitseinsatz]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getArbeitseinsaetzeQuery(kundeId).future()
  }

  def getFutureArbeitseinsaetze(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Arbeitseinsatz]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getFutureArbeitseinsaetzeQuery.future()
  }

  def getArbeitseinsatz(arbeitseinsatzId: ArbeitseinsatzId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Arbeitseinsatz]] = {
    import scalikejdbc.async.makeSQLToOptionAsync
    getArbeitseinsatzQuery(arbeitseinsatzId).future()
  }

  def getArbeitseinsatzDetail(arbeitseinsatzId: ArbeitseinsatzId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[ArbeitseinsatzDetail]] = {
    getArbeitseinsatzDetailQuery(arbeitseinsatzId).future()
  }

  def getFutureArbeitseinsaetze(kundeId: KundeId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Arbeitseinsatz]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getFutureArbeitseinsaetzeQuery(kundeId).future()
  }

  def getArbeitseinsatzabrechnung(xFlags: Option[ArbeitsComplexFlags])(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[ArbeitseinsatzAbrechnung]] = {
    xFlags match {
      case Some(acf) if acf.kundeAktiv =>
        getArbeitseinsatzabrechnungOnlyAktivKundenQuery.future()
      case _ =>
        getArbeitseinsatzabrechnungQuery.future()
    }
  }

  def getArbeitseinsatzDetailByArbeitsangebot(arbeitsangebotId: ArbeitsangebotId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[ArbeitseinsatzDetail]] = {
    getArbeitseinsatzDetailByArbeitsangebotQuery(arbeitsangebotId).future()
  }
}
