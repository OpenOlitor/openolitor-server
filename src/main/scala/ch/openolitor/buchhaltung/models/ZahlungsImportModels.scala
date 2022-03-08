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
package ch.openolitor.buchhaltung.models

import ch.openolitor.core.models._
import org.joda.time.DateTime
import ch.openolitor.core.JSONSerializable
import ch.openolitor.stammdaten.models._

sealed trait ZahlungsEingangStatus
case object Ok extends ZahlungsEingangStatus
case object Storno extends ZahlungsEingangStatus
case object AbgelehntFinanzinstitut extends ZahlungsEingangStatus
case object BetragNichtKorrekt extends ZahlungsEingangStatus
case object BereitsVerarbeitet extends ZahlungsEingangStatus
case object ReferenznummerNichtGefunden extends ZahlungsEingangStatus

object ZahlungsEingangStatus {
  def apply(value: String): ZahlungsEingangStatus = {
    Vector(Ok, Storno, AbgelehntFinanzinstitut, BetragNichtKorrekt, BereitsVerarbeitet, ReferenznummerNichtGefunden).find(_.toString == value).getOrElse(Ok)
  }
}

sealed trait ZahlungsExportStatus
case object Created extends ZahlungsExportStatus
case object Sent extends ZahlungsExportStatus
case object Archived extends ZahlungsExportStatus

object ZahlungsExportStatus {
  def apply(value: String): ZahlungsExportStatus = {
    Vector(Created, Sent, Archived).find(_.toString == value).getOrElse(Created)
  }
}

case class ZahlungsEingangId(id: Long) extends BaseId

case class ZahlungsImportId(id: Long) extends BaseId

case class ZahlungsImport(
  id: ZahlungsImportId,
  file: String,
  anzahlZahlungsEingaenge: Int,
  anzahlZahlungsEingaengeErledigt: Int,
  // modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[ZahlungsImportId]

case class ZahlungsImportCreate(
  id: ZahlungsImportId,
  file: String,
  zahlungsEingaenge: Seq[ZahlungsEingangCreate]
) extends JSONSerializable

case class ZahlungsImportDetail(
  id: ZahlungsImportId,
  file: String,
  zahlungsEingaenge: Seq[ZahlungsEingang],
  // modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends JSONSerializable

case class ZahlungsEingang(
  id: ZahlungsEingangId,
  zahlungsImportId: ZahlungsImportId,
  rechnungId: Option[RechnungId],
  kundeBezeichnung: Option[String],
  kundeId: Option[KundeId],
  transaktionsart: String,
  teilnehmerNummer: Option[String],
  iban: Option[String],
  referenzNummer: String,
  waehrung: Waehrung,
  betrag: BigDecimal,
  aufgabeDatum: DateTime,
  verarbeitungsDatum: DateTime,
  gutschriftsDatum: DateTime,
  debitor: Option[String],
  status: ZahlungsEingangStatus,
  erledigt: Boolean,
  bemerkung: Option[String],
  kundeBemerkung: String,
  kundeId: KundeId,
  // modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[ZahlungsEingangId]

case class ZahlungsEingangCreate(
  id: ZahlungsEingangId,
  zahlungsImportId: ZahlungsImportId,
  rechnungId: Option[RechnungId],
  kundeBezeichnung: Option[String],
  kundeId: Option[KundeId],
  transaktionsart: String,
  teilnehmerNummer: Option[String],
  iban: Option[String],
  debitor: Option[String],
  referenzNummer: String,
  waehrung: Waehrung,
  betrag: BigDecimal,
  aufgabeDatum: DateTime,
  verarbeitungsDatum: DateTime,
  gutschriftsDatum: DateTime,
  status: ZahlungsEingangStatus,
  kundeBemerkung: String,
  kundeId: KundeId
) extends JSONSerializable

case class ZahlungsEingangModifyErledigt(
  id: ZahlungsEingangId,
  bemerkung: Option[String]
) extends JSONSerializable

case class ZahlungsExportId(id: Long) extends BaseId

case class ZahlungsExport(
  id: ZahlungsExportId,
  fileName: String,
  rechnungen: Set[RechnungId],
  status: ZahlungsExportStatus,
  // modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[ZahlungsExportId]

case class ZahlungsExportCreate(
  id: ZahlungsExportId,
  fileName: String,
  rechnungen: Set[RechnungId],
  status: ZahlungsExportStatus
) extends JSONSerializable

