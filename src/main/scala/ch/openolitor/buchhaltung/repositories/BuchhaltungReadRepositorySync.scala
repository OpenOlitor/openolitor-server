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
import ch.openolitor.stammdaten.models._
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.buchhaltung.models._
import ch.openolitor.stammdaten.repositories.{ ProjektReadRepositorySync, ProjektReadRepositorySyncImpl }
import ch.openolitor.util.parsing.{ FilterExpr, QueryFilter }

trait BuchhaltungReadRepositorySync extends BaseReadRepositorySync with ProjektReadRepositorySync {
  def getRechnungen(implicit session: DBSession, cpContext: ConnectionPoolContext): List[Rechnung]
  def getKundenRechnungen(kundeId: KundeId)(implicit session: DBSession, cpContext: ConnectionPoolContext): List[Rechnung]
  def getRechnungDetail(id: RechnungId)(implicit session: DBSession, cpContext: ConnectionPoolContext): Option[RechnungDetail]
  def getRechnungByReferenznummer(referenzNummer: String)(implicit session: DBSession, cpContext: ConnectionPoolContext): Option[Rechnung]

  def getRechnungsPositionenByRechnungsId(rechnungId: RechnungId)(implicit session: DBSession, cpContext: ConnectionPoolContext): List[RechnungsPosition]

  def getZahlungsImports(implicit session: DBSession, cpContext: ConnectionPoolContext, filter: Option[FilterExpr], queryString: Option[QueryFilter]): List[ZahlungsImport]
  def getZahlungsImportDetail(id: ZahlungsImportId)(implicit session: DBSession, cpContext: ConnectionPoolContext): Option[ZahlungsImportDetail]
  def getZahlungsEingangByReferenznummer(referenzNummer: String)(implicit session: DBSession, cpContext: ConnectionPoolContext): Option[ZahlungsEingang]

  def getPerson(rechnungId: RechnungId)(implicit session: DBSession): List[Person]
  def getZahlungsExports(implicit session: DBSession, cpContext: ConnectionPoolContext): List[ZahlungsExport]
  def getZahlungsExportDetail(id: ZahlungsExportId)(implicit session: DBSession, cpContext: ConnectionPoolContext): Option[ZahlungsExport]

  def getKontoDatenProjekt(implicit session: DBSession): Option[KontoDaten]
  def getKontoDatenKunde(id: KundeId)(implicit session: DBSession): Option[KontoDaten]
}

trait BuchhaltungReadRepositorySyncImpl extends BuchhaltungReadRepositorySync with LazyLogging with BuchhaltungRepositoryQueries with ProjektReadRepositorySyncImpl {
  def getRechnungen(implicit session: DBSession, cpContext: ConnectionPoolContext): List[Rechnung] = {
    getRechnungenQuery(None, None, None).apply()
  }

  def getKundenRechnungen(kundeId: KundeId)(implicit session: DBSession, cpContext: ConnectionPoolContext): List[Rechnung] = {
    getKundenRechnungenQuery(kundeId).apply()
  }

  def getRechnungDetail(id: RechnungId)(implicit session: DBSession, cpContext: ConnectionPoolContext): Option[RechnungDetail] = {
    getRechnungDetailQuery(id).apply()
  }

  def getRechnungByReferenznummer(referenzNummer: String)(implicit session: DBSession, cpContext: ConnectionPoolContext): Option[Rechnung] = {
    getRechnungByReferenznummerQuery(referenzNummer).apply()
  }

  def getRechnungsPositionenByRechnungsId(rechnungId: RechnungId)(implicit session: DBSession, cpContext: ConnectionPoolContext): List[RechnungsPosition] = {
    getRechnungsPositionenByRechnungsIdQuery(rechnungId).apply()
  }

  def getZahlungsImports(implicit session: DBSession, cpContext: ConnectionPoolContext, filter: Option[FilterExpr], queryString: Option[QueryFilter]): List[ZahlungsImport] = {
    getZahlungsImportsQuery(filter, queryString).apply()
  }

  def getZahlungsImportDetail(id: ZahlungsImportId)(implicit session: DBSession, cpContext: ConnectionPoolContext): Option[ZahlungsImportDetail] = {
    getZahlungsImportDetailQuery(id).apply()
  }

  def getZahlungsEingangByReferenznummer(referenzNummer: String)(implicit session: DBSession, cpContext: ConnectionPoolContext): Option[ZahlungsEingang] = {
    getZahlungsEingangByReferenznummerQuery(referenzNummer)()
  }

  def getKontoDatenProjekt(implicit session: DBSession): Option[KontoDaten] = {
    getKontoDatenProjektQuery.apply()
  }

  def getPerson(rechnungId: RechnungId)(implicit session: DBSession): List[Person] = {
    getPersonQuery(rechnungId).apply()
  }

  def getKontoDatenKunde(id: KundeId)(implicit session: DBSession): Option[KontoDaten] = {
    getKontoDatenKundeQuery(id).apply()
  }

  def getZahlungsExports(implicit session: DBSession, cpContext: ConnectionPoolContext): List[ZahlungsExport] = {
    getZahlungsExportsQuery.apply()
  }

  def getZahlungsExportDetail(id: ZahlungsExportId)(implicit session: DBSession, cpContext: ConnectionPoolContext): Option[ZahlungsExport] = {
    getZahlungsExportQuery(id).apply()
  }
}
