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
package ch.openolitor.buchhaltung

import java.util.UUID

import ch.openolitor.core.models._
import ch.openolitor.core.repositories.BaseRepository
import ch.openolitor.core.repositories.BaseRepository._
import ch.openolitor.buchhaltung.models._
import scalikejdbc._
import scalikejdbc.TypeBinder._
import ch.openolitor.core.repositories.DBMappings
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.repositories.BaseEntitySQLSyntaxSupport
import ch.openolitor.core.scalax._
import ch.openolitor.stammdaten.StammdatenDBMappings

//DB Model bindig
trait BuchhaltungDBMappings extends DBMappings with StammdatenDBMappings {
  import TypeBinder._

  //DB binders
  implicit val rechnungStatusBinder: Binders[RechnungStatus] = Binders.string.xmap(RechnungStatus.apply, _.productPrefix)
  implicit val zahlungsEingangStatusBinder: Binders[ZahlungsEingangStatus] = Binders.string.xmap(ZahlungsEingangStatus.apply, _.productPrefix)

  implicit val rechnungIdSqlBinder = baseIdParameterBinderFactory[RechnungId](RechnungId.apply)
  implicit val zahlungsEingangIdSqlBinder = baseIdParameterBinderFactory[ZahlungsEingangId](ZahlungsEingangId.apply)
  implicit val zahlungsImportIdSqlBinder = baseIdParameterBinderFactory[ZahlungsImportId](ZahlungsImportId.apply)

  implicit val rechnungMapping = new BaseEntitySQLSyntaxSupport[Rechnung] {
    override val tableName = "Rechnung"

    override lazy val columns = autoColumns[Rechnung]()

    def apply(rn: ResultName[Rechnung])(rs: WrappedResultSet): Rechnung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Rechnung): Seq[ParameterBinder] =
      parameters(Rechnung.unapply(entity).get)

    override def updateParameters(entity: Rechnung) = {
      super.updateParameters(entity) ++ Seq(
        column.kundeId -> entity.kundeId,
        column.aboId -> entity.aboId,
        column.titel -> entity.titel,
        column.anzahlLieferungen -> entity.anzahlLieferungen,
        column.waehrung -> entity.waehrung,
        column.betrag -> entity.betrag,
        column.einbezahlterBetrag -> entity.einbezahlterBetrag,
        column.rechnungsDatum -> entity.rechnungsDatum,
        column.faelligkeitsDatum -> entity.faelligkeitsDatum,
        column.eingangsDatum -> entity.eingangsDatum,
        column.status -> entity.status,
        column.referenzNummer -> entity.referenzNummer,
        column.esrNummer -> entity.esrNummer,
        column.strasse -> entity.strasse,
        column.hausNummer -> entity.hausNummer,
        column.adressZusatz -> entity.adressZusatz,
        column.plz -> entity.plz,
        column.ort -> entity.ort
      )
    }
  }

  implicit val zahlungsImportMapping = new BaseEntitySQLSyntaxSupport[ZahlungsImport] {
    override val tableName = "ZahlungsImport"

    override lazy val columns = autoColumns[ZahlungsImport]()

    def apply(rn: ResultName[ZahlungsImport])(rs: WrappedResultSet): ZahlungsImport =
      autoConstruct(rs, rn)

    def parameterMappings(entity: ZahlungsImport): Seq[ParameterBinder] =
      parameters(ZahlungsImport.unapply(entity).get)

    override def updateParameters(entity: ZahlungsImport) = {
      super.updateParameters(entity) ++ Seq(
        column.file -> entity.file,
        column.anzahlZahlungsEingaenge -> entity.anzahlZahlungsEingaenge,
        column.anzahlZahlungsEingaengeErledigt -> entity.anzahlZahlungsEingaengeErledigt
      )
    }
  }

  implicit val zahlungsEingangMapping = new BaseEntitySQLSyntaxSupport[ZahlungsEingang] {
    override val tableName = "ZahlungsEingang"

    override lazy val columns = autoColumns[ZahlungsEingang]()

    def apply(rn: ResultName[ZahlungsEingang])(rs: WrappedResultSet): ZahlungsEingang =
      autoConstruct(rs, rn)

    def parameterMappings(entity: ZahlungsEingang): Seq[ParameterBinder] =
      parameters(ZahlungsEingang.unapply(entity).get)

    override def updateParameters(entity: ZahlungsEingang) = {
      super.updateParameters(entity) ++ Seq(
        column.erledigt -> entity.erledigt,
        column.bemerkung -> entity.bemerkung
      )
    }
  }
}
