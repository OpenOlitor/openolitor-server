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

import ch.openolitor.stammdaten.models.AboId
import java.util.UUID
import ch.openolitor.core.models._
import ch.openolitor.buchhaltung.models._
import scalikejdbc._
import ch.openolitor.core.repositories.DBMappings
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.repositories.BaseEntitySQLSyntaxSupport
import ch.openolitor.core.scalax._
import ch.openolitor.stammdaten.StammdatenDBMappings
import ch.openolitor.core.repositories.BaseParameter

//DB Model bindig
trait BuchhaltungDBMappings extends DBMappings with StammdatenDBMappings with BaseParameter {

  // ParameterBinders
  //  implicit val rechnungIdParameterBinderFactory: ParameterBinderFactory[RechnungId] = baseIdParameterBinderFactory(RechnungId.apply _)
  //  implicit val rechnungsPositionIdParameterBinderFactory: ParameterBinderFactory[RechnungsPositionId] = baseIdParameterBinderFactory(RechnungsPositionId.apply _)
  //  implicit val optionRechnungsPositionIdParameterBinderFactory: ParameterBinderFactory[Option[RechnungsPositionId]] = optionBaseIdParameterBinderFactory(RechnungsPositionId.apply _)
  //  implicit val zahlungsImportIdBinder: ParameterBinderFactory[ZahlungsImportId] = baseIdParameterBinderFactory(ZahlungsImportId.apply _)
  //  implicit val zahlungsEingangIdBinder: ParameterBinderFactory[ZahlungsEingangId] = baseIdParameterBinderFactory(ZahlungsEingangId.apply _)

  implicit def rechnungStatusParameterBinderFactory[A <: RechnungStatus]: ParameterBinderFactory[A] = toStringParameterBinderFactory
  implicit def rechnungsPositionStatusParameterBinderFactory[A <: RechnungsPositionStatus.RechnungsPositionStatus]: ParameterBinderFactory[A] = toStringParameterBinderFactory
  implicit def rechnungsPositionTypParameterBinderFactory[A <: RechnungsPositionTyp.RechnungsPositionTyp]: ParameterBinderFactory[A] = toStringParameterBinderFactory
  // implicit val optionRechnungIdBinder: ParameterBinderFactory[Option[RechnungId]] = optionBa
  // implicit val optionAboIdBinder: ParameterBinderFactory[Option[AboId]] = optionBaseIdParameterBinderFactory(AboId.apply _)

  implicit val zahlungsEingangStatusParameterBinderFactory: ParameterBinderFactory[ZahlungsEingangStatus] = toStringParameterBinderFactory

  // TypeBinders
  implicit val rechnungIdTypeBinder: TypeBinder[RechnungId] = baseIdTypeBinder(RechnungId.apply _)
  implicit val rechnungsPositionIdTypeBinder: TypeBinder[RechnungsPositionId] = baseIdTypeBinder(RechnungsPositionId.apply _)
  implicit val optionRechnungsPositionIdTypeBinder: TypeBinder[Option[RechnungsPositionId]] = optionBaseIdTypeBinder(RechnungsPositionId.apply _)
  implicit val zahlungsImportIdTypeBinder: TypeBinder[ZahlungsImportId] = baseIdTypeBinder(ZahlungsImportId.apply _)
  implicit val zahlungsEingangIdTypeBinder: TypeBinder[ZahlungsEingangId] = baseIdTypeBinder(ZahlungsEingangId.apply _)

  implicit val rechnungStatusTypeBinder: TypeBinder[RechnungStatus] = toStringTypeBinder(RechnungStatus.apply)
  implicit val rechnungsPositionStatusTypeBinder: TypeBinder[RechnungsPositionStatus.RechnungsPositionStatus] = toStringTypeBinder(RechnungsPositionStatus.apply)
  implicit val rechnungsPositionTypTypeBinder: TypeBinder[RechnungsPositionTyp.RechnungsPositionTyp] = toStringTypeBinder(RechnungsPositionTyp.apply)
  implicit val optionRechnungIdBinder: TypeBinder[Option[RechnungId]] = optionBaseIdTypeBinder(RechnungId.apply _)
  implicit val optionAboIdBinder: TypeBinder[Option[AboId]] = optionBaseIdTypeBinder(AboId.apply _)

  implicit val zahlungsEingangStatusTypeBinder: TypeBinder[ZahlungsEingangStatus] = toStringTypeBinder(ZahlungsEingangStatus.apply)

  // declare parameterbinderfactories for enum type to allow dynamic type convertion of enum subtypes

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
        column.titel -> entity.titel,
        column.betrag -> entity.betrag,
        column.einbezahlterBetrag -> entity.einbezahlterBetrag,
        column.waehrung -> entity.waehrung,
        column.rechnungsDatum -> entity.rechnungsDatum,
        column.faelligkeitsDatum -> entity.faelligkeitsDatum,
        column.eingangsDatum -> entity.eingangsDatum,
        column.status -> entity.status,
        column.referenzNummer -> entity.referenzNummer,
        column.esrNummer -> entity.esrNummer,
        column.fileStoreId -> entity.fileStoreId,
        column.anzahlMahnungen -> entity.anzahlMahnungen,
        column.mahnungFileStoreIds -> entity.mahnungFileStoreIds,
        column.strasse -> entity.strasse,
        column.hausNummer -> entity.hausNummer,
        column.adressZusatz -> entity.adressZusatz,
        column.plz -> entity.plz,
        column.ort -> entity.ort
      )
    }
  }

  implicit val rechnungsPositionMapping = new BaseEntitySQLSyntaxSupport[RechnungsPosition] {
    override val tableName = "RechnungsPosition"

    override lazy val columns = autoColumns[RechnungsPosition]()

    def apply(rn: ResultName[RechnungsPosition])(rs: WrappedResultSet): RechnungsPosition =
      autoConstruct(rs, rn)

    def parameterMappings(entity: RechnungsPosition): Seq[ParameterBinder] =
      parameters(RechnungsPosition.unapply(entity).get)

    override def updateParameters(entity: RechnungsPosition) = {
      super.updateParameters(entity) ++ Seq(
        column.rechnungId -> entity.rechnungId,
        column.parentRechnungsPositionId -> entity.parentRechnungsPositionId,
        column.aboId -> entity.aboId,
        column.kundeId -> entity.kundeId,
        column.betrag -> entity.betrag,
        column.waehrung -> entity.waehrung,
        column.anzahlLieferungen -> entity.anzahlLieferungen,
        column.beschrieb -> entity.beschrieb,
        column.status -> entity.status,
        column.typ -> entity.typ,
        column.sort -> entity.sort
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
