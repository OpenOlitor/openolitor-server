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
package ch.openolitor.kundenportal.repositories

import scalikejdbc.async.{ makeSQLToListAsync => _, makeSQLToOptionAsync => _, _ }

import scala.concurrent.ExecutionContext
import ch.openolitor.core.db._
import ch.openolitor.core.db.OOAsyncDB._

import scala.concurrent._
import ch.openolitor.stammdaten.models._
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.Macros._
import ch.openolitor.util.parsing.{ FilterExpr, GeschaeftsjahrFilter }
import ch.openolitor.core.security.Subject
import ch.openolitor.buchhaltung.models.RechnungId
import ch.openolitor.buchhaltung.models.Rechnung
import ch.openolitor.buchhaltung.models.RechnungDetail
import ch.openolitor.arbeitseinsatz.models._

trait KundenportalReadRepositoryAsync {
  def getProjekt(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[ProjektKundenportal]]
  def getGeschaeftsjahre(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[GeschaeftsjahrStart]]
  def getKontoDatenProjekt(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[KontoDaten]]

  def getHauptabos(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr], owner: Subject): Future[List[AboDetail]]
  def getZusatzAbosByHauptAbo(aboId: AboId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr], owner: Subject): Future[List[ZusatzAboDetail]]

  def getLieferungenDetails(aboId: AbotypId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr], owner: Subject): Future[List[LieferungDetail]]
  def getLieferungenDetails(aboId: AbotypId, vertriebId: VertriebId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr], owner: Subject): Future[List[LieferungDetail]]
  def getLieferungenMainAndAdditionalDetails(aboTypId: AbotypId, vertriebId: VertriebId, aboId: AboId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr], owner: Subject): Future[List[LieferungDetail]]
  def getLieferungenDetail(lieferungId: LieferungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext, owner: Subject): Future[Option[LieferungDetail]]

  def getRechnungen(implicit asyncCpContext: MultipleAsyncConnectionPoolContext, owner: Subject): Future[List[Rechnung]]
  def getRechnungDetail(id: RechnungId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext, owner: Subject): Future[Option[RechnungDetail]]

  def getArbeitseinsaetze(implicit asyncCpContext: MultipleAsyncConnectionPoolContext, owner: Subject): Future[List[ArbeitseinsatzDetail]]
  def getArbeitsangebote(implicit asyncCpContext: MultipleAsyncConnectionPoolContext, owner: Subject, gjFilter: Option[GeschaeftsjahrFilter]): Future[List[Arbeitsangebot]]
}

class KundenportalReadRepositoryAsyncImpl extends KundenportalReadRepositoryAsync with LazyLogging with KundenportalRepositoryQueries {
  def getProjekt(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[ProjektKundenportal]] = {
    import scalikejdbc.async.makeSQLToOptionAsync
    getProjektQuery.future() map (_ map (projekt => {
      val t = copyTo[Projekt, ProjektKundenportal](projekt)
      t
    }))
  }

  def getGeschaeftsjahre(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[GeschaeftsjahrStart]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getGeschaeftsjahreQuery.future()
  }

  def getKontoDatenProjekt(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[KontoDaten]] = {
    import scalikejdbc.async.makeSQLToOptionAsync
    getKontoDatenProjektQuery.future()
  }

  def getDepotlieferungAbos(implicit asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr], owner: Subject): Future[List[DepotlieferungAboDetail]] = {
    getDepotlieferungAbosQuery(filter).future()
  }

  def getHeimlieferungAbos(implicit asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr], owner: Subject): Future[List[HeimlieferungAboDetail]] = {
    getHeimlieferungAbosQuery(filter).future()
  }

  def getPostlieferungAbos(implicit asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr], owner: Subject): Future[List[PostlieferungAboDetail]] = {
    getPostlieferungAbosQuery(filter).future()
  }

  def getHauptabos(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr], owner: Subject): Future[List[AboDetail]] = {
    for {
      d <- getDepotlieferungAbos
      h <- getHeimlieferungAbos
      p <- getPostlieferungAbos
    } yield {
      d ::: h ::: p
    }
  }

  def getZusatzAbosByHauptAbo(aboId: AboId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr], owner: Subject): Future[List[ZusatzAboDetail]] = {
    getZusatzAbosByHauptAboQuery(aboId, filter).future()
  }

  def getLieferungenDetails(abotypId: AbotypId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr], owner: Subject): Future[List[LieferungDetail]] = {
    import scalikejdbc.async.makeOneToManySQLToListAsync
    getLieferungenByAbotypQuery(abotypId, filter).future()
  }

  def getLieferungenDetails(abotypId: AbotypId, vertriebId: VertriebId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr], owner: Subject): Future[List[LieferungDetail]] = {
    import scalikejdbc.async.makeOneToManySQLToListAsync
    getLieferungenDetailsQuery(abotypId, vertriebId, filter).future()
  }

  def getLieferungenMainAndAdditionalDetails(abotypId: AbotypId, vertriebId: VertriebId, aboId: AboId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr], owner: Subject): Future[List[LieferungDetail]] = {
    import scalikejdbc.async.makeOneToManySQLToListAsync
    for {
      zusatzabos <- getZusatzAbosByHauptAbo(aboId)
      mainLieferungen <- getLieferungenDetailsQuery(abotypId, vertriebId, filter).future()
      zusatzaboLieferungen <- Future.sequence(zusatzabos.map(z => getLieferungenByAbotypQuery(z.abotypId, None).future()))
    } yield mainLieferungen ++ zusatzaboLieferungen.flatMap(identity)
  }

  def getLieferungenDetail(lieferungId: LieferungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext, owner: Subject): Future[Option[LieferungDetail]] = {
    getLieferungenDetailQuery(lieferungId).future()
  }

  def getRechnungen(implicit asyncCpContext: MultipleAsyncConnectionPoolContext, owner: Subject): Future[List[Rechnung]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getRechnungenQuery.future()
  }

  def getRechnungDetail(id: RechnungId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext, owner: Subject): Future[Option[RechnungDetail]] = {
    getRechnungDetailQuery(id).future()
  }

  def getArbeitseinsaetze(implicit asyncCpContext: MultipleAsyncConnectionPoolContext, owner: Subject): Future[List[ArbeitseinsatzDetail]] = {
    getArbeitseinsaetzeQuery.future()
  }

  def getArbeitsangebote(implicit asyncCpContext: MultipleAsyncConnectionPoolContext, owner: Subject, gjFilter: Option[GeschaeftsjahrFilter]): Future[List[Arbeitsangebot]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getArbeitsangeboteQuery(gjFilter).future()
  }
}
