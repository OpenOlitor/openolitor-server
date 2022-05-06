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

import ch.openolitor.buchhaltung.models._
import ch.openolitor.core.db._
import ch.openolitor.core.db.OOAsyncDB._
import ch.openolitor.core.repositories._
import ch.openolitor.stammdaten.models._
import ch.openolitor.util.parsing.{ FilterExpr, GeschaeftsjahrFilter }
import com.typesafe.scalalogging.LazyLogging
import scalikejdbc.{ HasExtractor, OneToManies6SQLToOption }
import scalikejdbc.async.{ makeSQLToOptionAsync => _, makeSQLToListAsync => _, _ }

import scala.concurrent._

object BuchhaltungReadRepositoryAsync {
  import scalikejdbc.async._
  implicit class SQLToAsync[A, B1, B2, B3, B4, B5, B6, Z](sql: OneToManies6SQLToOption[A, B1, B2, B3, B4, B5, B6, HasExtractor, Z]) {
    def asyncResult()(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Z]] = {
      makeOneToManies6SQLToOptionAsync[A, B1, B2, B3, B4, B5, B6, Z](sql).future()
    }
  }
}

/**
 * Asynchronous Repository
 */
trait BuchhaltungReadRepositoryAsync extends BaseReadRepositoryAsync {
  def getRechnungen(implicit asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr], gjFilter: Option[GeschaeftsjahrFilter]): Future[List[Rechnung]]
  def getRechnungsPositionen(implicit asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr]): Future[List[RechnungsPosition]]
  def getKundenRechnungen(kundeId: KundeId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Rechnung]]
  def getRechnungDetail(id: RechnungId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[RechnungDetail]]
  def getRechnungByReferenznummer(referenzNummer: String)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Rechnung]]

  def getZahlungsImports(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[ZahlungsImport]]
  def getZahlungsImportDetail(id: ZahlungsImportId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[ZahlungsImportDetail]]

  def getZahlungsExports(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[ZahlungsExport]]
  def getZahlungsExportDetail(id: ZahlungsExportId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[ZahlungsExport]]
  def getProjekt(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Projekt]]
}

class BuchhaltungReadRepositoryAsyncImpl extends BuchhaltungReadRepositoryAsync with LazyLogging with BuchhaltungRepositoryQueries {
  def getRechnungen(implicit asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr], gjFilter: Option[GeschaeftsjahrFilter]): Future[List[Rechnung]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getRechnungenQuery(filter, gjFilter).future()
  }

  def getRechnungsPositionen(implicit asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr]): Future[List[RechnungsPosition]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getRechnungsPositionQuery(filter).future()
  }

  def getKundenRechnungen(kundeId: KundeId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Rechnung]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getKundenRechnungenQuery(kundeId).future()
  }

  def getRechnungDetail(id: RechnungId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[RechnungDetail]] = {
    getRechnungDetailQuery(id).future()
  }

  def getRechnungByReferenznummer(referenzNummer: String)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Rechnung]] = {
    import scalikejdbc.async.makeSQLToOptionAsync
    getRechnungByReferenznummerQuery(referenzNummer).future()
  }

  def getZahlungsImports(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[ZahlungsImport]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getZahlungsImportsQuery.future()
  }

  def getZahlungsImportDetail(id: ZahlungsImportId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[ZahlungsImportDetail]] = {
    getZahlungsImportDetailQuery(id).future()
  }

  def getZahlungsExports(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[ZahlungsExport]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getZahlungsExportsQuery.future()
  }

  def getZahlungsExportDetail(id: ZahlungsExportId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[ZahlungsExport]] = {
    import scalikejdbc.async.makeSQLToOptionAsync
    getZahlungsExportQuery(id).future()
  }

  def getProjekt(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Projekt]] = {
    import scalikejdbc.async.makeSQLToOptionAsync
    getProjektQuery.future()
  }
}

