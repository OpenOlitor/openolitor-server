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
package ch.openolitor.arbeitseinsatz.models

import ch.openolitor.core.JSONSerializable
import ch.openolitor.core.models._
import ch.openolitor.stammdaten.models._
import org.joda.time.DateTime

sealed trait ArbeitseinsatzStatus extends Product

object ArbeitseinsatzStatus {
  def apply(value: String): ArbeitseinsatzStatus = {
    Vector(InVorbereitung, Bereit, Abgesagt, Archiviert) find (_.toString == value) getOrElse (Bereit)
  }
}

case object InVorbereitung extends ArbeitseinsatzStatus
case object Bereit extends ArbeitseinsatzStatus
case object Abgesagt extends ArbeitseinsatzStatus
case object Archiviert extends ArbeitseinsatzStatus

case class ArbeitskategorieId(id: Long) extends BaseId
case class ArbeitskategorieBez(id: String) extends BaseStringId

case class Arbeitskategorie(
  id: ArbeitskategorieId,
  beschreibung: String,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[ArbeitskategorieId] {
}

case class ArbeitskategorieModify(beschreibung: String) extends JSONSerializable

case class ArbeitsangebotId(id: Long) extends BaseId

trait IArbeitsangebot extends BaseEntity[ArbeitsangebotId] {
  val id: ArbeitsangebotId
  val kopieVon: Option[ArbeitsangebotId]
  val titel: String
  val bezeichnung: Option[String]
  val ort: Option[String]
  val zeitVon: DateTime
  val zeitBis: DateTime
  val arbeitskategorien: Set[ArbeitskategorieBez]
  val anzahlEingeschriebene: Int
  val anzahlPersonen: Option[Int]
  val mehrPersonenOk: Boolean
  val einsatzZeit: Option[BigDecimal]
  val status: ArbeitseinsatzStatus
}

case class Arbeitsangebot(
  id: ArbeitsangebotId,
  kopieVon: Option[ArbeitsangebotId],
  titel: String,
  bezeichnung: Option[String],
  ort: Option[String],
  zeitVon: DateTime,
  zeitBis: DateTime,
  arbeitskategorien: Set[ArbeitskategorieBez],
  anzahlEingeschriebene: Int,
  anzahlPersonen: Option[Int],
  mehrPersonenOk: Boolean,
  einsatzZeit: Option[BigDecimal],
  status: ArbeitseinsatzStatus,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[ArbeitsangebotId]

case class ArbeitsangebotModify(
  kopieVon: Option[ArbeitsangebotId],
  titel: String,
  bezeichnung: Option[String],
  ort: Option[String],
  zeitVon: DateTime,
  zeitBis: DateTime,
  arbeitskategorien: Set[ArbeitskategorieBez],
  anzahlPersonen: Option[Int],
  mehrPersonenOk: Boolean,
  einsatzZeit: Option[BigDecimal],
  status: ArbeitseinsatzStatus
) extends JSONSerializable

case class ArbeitsangebotMailRequest(
  ids: Seq[ArbeitsangebotId],
  subject: String,
  body: String
) extends JSONSerializable

case class ArbeitsangebotMailContext(
  person: Person,
  arbeitsangebot: Arbeitsangebot
) extends JSONSerializable

case class ArbeitseinsatzId(id: Long) extends BaseId

trait IArbeitseinsatz extends BaseEntity[ArbeitseinsatzId] {
  val id: ArbeitseinsatzId
  val arbeitsangebotId: ArbeitsangebotId
  val arbeitsangebotTitel: String
  val arbeitsangebotStatus: ArbeitseinsatzStatus
  val zeitVon: DateTime
  val zeitBis: DateTime
  val kundeId: KundeId
  val kundeBezeichnung: String
  val personId: Option[PersonId]
  val personName: Option[String]
  val aboId: Option[AboId]
  val aboBezeichnung: Option[String]
  val anzahlPersonen: Int
  val bemerkungen: Option[String]
  val email: Option[String]
  val telefonMobil: Option[String]
}

case class Arbeitseinsatz(
  id: ArbeitseinsatzId,
  arbeitsangebotId: ArbeitsangebotId,
  arbeitsangebotTitel: String,
  arbeitsangebotStatus: ArbeitseinsatzStatus,
  zeitVon: DateTime,
  zeitBis: DateTime,
  einsatzZeit: Option[BigDecimal],
  kundeId: KundeId,
  kundeBezeichnung: String,
  personId: Option[PersonId],
  personName: Option[String],
  aboId: Option[AboId],
  aboBezeichnung: Option[String],
  anzahlPersonen: Int,
  bemerkungen: Option[String],
  email: Option[String],
  telefonMobil: Option[String],
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[ArbeitseinsatzId]

case class ArbeitseinsatzDetail(
  id: ArbeitseinsatzId,
  arbeitsangebotId: ArbeitsangebotId,
  arbeitsangebotTitel: String,
  arbeitsangebotStatus: ArbeitseinsatzStatus,
  zeitVon: DateTime,
  zeitBis: DateTime,
  einsatzZeit: Option[BigDecimal],
  kundeId: KundeId,
  kundeBezeichnung: String,
  aboId: Option[AboId],
  aboBezeichnung: Option[String],
  personId: Option[PersonId],
  personName: Option[String],
  anzahlPersonen: Int,
  bemerkungen: Option[String],
  email: Option[String],
  telefonMobil: Option[String],
  //additional Detail fields
  arbeitsangebot: Arbeitsangebot,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[ArbeitseinsatzId]

case class ArbeitseinsatzDetailReport(
  id: ArbeitseinsatzId,
  arbeitsangebotId: ArbeitsangebotId,
  arbeitsangebotTitel: String,
  arbeitsangebotStatus: ArbeitseinsatzStatus,
  zeitVon: DateTime,
  zeitBis: DateTime,
  einsatzZeit: Option[BigDecimal],
  kundeId: KundeId,
  kundeBezeichnung: String,
  aboId: Option[AboId],
  aboBezeichnung: Option[String],
  personId: Option[PersonId],
  personName: Option[String],
  anzahlPersonen: Int,
  bemerkungen: Option[String],
  email: Option[String],
  telefonMobil: Option[String],
  //additional Detail fields
  arbeitsangebot: Arbeitsangebot,
  projekt: ProjektReport,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends JSONSerializable

case class ArbeitseinsatzModify(
  arbeitsangebotId: ArbeitsangebotId,
  zeitVon: DateTime,
  zeitBis: DateTime,
  einsatzZeit: Option[BigDecimal],
  kundeId: KundeId,
  personId: Option[PersonId],
  aboId: Option[AboId],
  anzahlPersonen: Int,
  bemerkungen: Option[String]
) extends JSONSerializable

case class ArbeitseinsatzCreate(
  arbeitsangebotId: ArbeitsangebotId,
  kundeId: KundeId,
  personId: Option[PersonId],
  anzahlPersonen: Int,
  bemerkungen: Option[String]
) extends JSONSerializable

case class ArbeitseinsatzAbrechnung(
  kundeId: KundeId,
  kundeBezeichnung: String,
  summeEinsaetzeSoll: BigDecimal,
  summeEinsaetzeIst: BigDecimal,
  summeEinsaetzeDelta: BigDecimal
) extends JSONSerializable

case class ArbeitsangeboteDuplicate(
  arbeitsangebotId: ArbeitsangebotId,
  daten: Seq[DateTime]
) extends JSONSerializable

case class ArbeitsangebotDuplicate(
  arbeitsangebotId: ArbeitsangebotId,
  zeitVon: DateTime
) extends JSONSerializable

/* Used to trigger more complex filtering on overview searches */

case class ArbeitsComplexFlags(
  kundeAktiv: Boolean
) extends JSONSerializable