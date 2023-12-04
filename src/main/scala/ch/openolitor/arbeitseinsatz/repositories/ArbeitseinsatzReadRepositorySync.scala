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
import ch.openolitor.core.repositories._
import ch.openolitor.stammdaten.models.{ KundeId, Person, Projekt }
import ch.openolitor.util.parsing.{ GeschaeftsjahrFilter, QueryFilter }
import com.typesafe.scalalogging.LazyLogging
import scalikejdbc.DBSession

trait ArbeitseinsatzReadRepositorySync extends BaseReadRepositorySync {
  def getArbeitskategorien(implicit session: DBSession): List[Arbeitskategorie]

  def getArbeitsangebote(implicit session: DBSession, gjFilter: Option[GeschaeftsjahrFilter], queryString: Option[QueryFilter]): List[Arbeitsangebot]
  def getArbeitsangebot(arbeitsangebotId: ArbeitsangebotId)(implicit session: DBSession): Option[Arbeitsangebot]
  def getFutureArbeitsangebote(implicit session: DBSession): List[Arbeitsangebot]
  def getArbeitseinsaetze(implicit session: DBSession, queryString: Option[QueryFilter]): List[Arbeitseinsatz]
  def getArbeitseinsatz(arbeitseinsatzId: ArbeitseinsatzId)(implicit session: DBSession): Option[Arbeitseinsatz]
  def getArbeitseinsaetze(kundeId: KundeId)(implicit session: DBSession): List[Arbeitseinsatz]
  def getFutureArbeitseinsaetze(implicit session: DBSession): List[Arbeitseinsatz]
  def getFutureArbeitseinsaetze(kundeId: KundeId)(implicit session: DBSession): List[Arbeitseinsatz]
  def getArbeitseinsatzabrechnung(implicit session: DBSession, queryString: Option[QueryFilter]): List[ArbeitseinsatzAbrechnung]
  def getArbeitseinsatzDetailByArbeitsangebot(arbeitsangebotId: ArbeitsangebotId)(implicit session: DBSession): List[ArbeitseinsatzDetail]
  def getPersonenByArbeitsangebot(arbeitsangebotId: ArbeitsangebotId)(implicit session: DBSession): List[Person]
  def getProjekt(implicit session: DBSession): Option[Projekt]
}

trait ArbeitseinsatzReadRepositorySyncImpl extends ArbeitseinsatzReadRepositorySync with LazyLogging with ArbeitseinsatzRepositoryQueries {
  def getArbeitskategorien(implicit session: DBSession): List[Arbeitskategorie] = {
    getArbeitskategorienQuery.apply()
  }

  def getArbeitsangebote(implicit session: DBSession, gjFilter: Option[GeschaeftsjahrFilter], queryString: Option[QueryFilter]): List[Arbeitsangebot] = {
    getArbeitsangeboteQuery(gjFilter, queryString).apply()
  }

  def getArbeitsangebot(arbeitsangebotId: ArbeitsangebotId)(implicit session: DBSession): Option[Arbeitsangebot] = {
    getArbeitsangebotQuery(arbeitsangebotId).apply()
  }

  def getFutureArbeitsangebote(implicit session: DBSession): List[Arbeitsangebot] = {
    getFutureArbeitsangeboteQuery.apply()
  }

  def getArbeitseinsaetze(implicit session: DBSession, queryString: Option[QueryFilter]): List[Arbeitseinsatz] = {
    getArbeitseinsaetzeQuery(queryString).apply()
  }

  def getArbeitseinsaetze(kundeId: KundeId)(implicit session: DBSession): List[Arbeitseinsatz] = {
    getArbeitseinsaetzeQuery(kundeId).apply()
  }

  def getFutureArbeitseinsaetze(implicit session: DBSession): List[Arbeitseinsatz] = {
    getFutureArbeitseinsaetzeQuery.apply()
  }

  def getArbeitseinsatz(arbeitseinsatzId: ArbeitseinsatzId)(implicit session: DBSession): Option[Arbeitseinsatz] = {
    getArbeitseinsatzQuery(arbeitseinsatzId).apply()
  }

  def getFutureArbeitseinsaetze(kundeId: KundeId)(implicit session: DBSession): List[Arbeitseinsatz] = {
    getFutureArbeitseinsaetzeQuery(kundeId).apply()
  }

  def getArbeitseinsatzabrechnung(implicit session: DBSession, queryString: Option[QueryFilter]): List[ArbeitseinsatzAbrechnung] = {
    getArbeitseinsatzabrechnungQuery(queryString).apply()
  }

  def getArbeitseinsatzDetailByArbeitsangebot(arbeitsangebotId: ArbeitsangebotId)(implicit session: DBSession): List[ArbeitseinsatzDetail] = {
    getArbeitseinsatzDetailByArbeitsangebotQuery(arbeitsangebotId).apply()
  }

  def getPersonenByArbeitsangebot(arbeitsangebotId: ArbeitsangebotId)(implicit session: DBSession): List[Person] = {
    getPersonenByArbeitsangebotQuery(arbeitsangebotId).apply()
  }

  def getProjekt(implicit session: DBSession): Option[Projekt] = {
    getProjektQuery.apply()
  }
}
