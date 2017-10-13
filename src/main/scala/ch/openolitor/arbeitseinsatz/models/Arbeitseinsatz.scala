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

import ch.openolitor.core.models._
import ch.openolitor.stammdaten.models._
import org.joda.time.DateTime
import ch.openolitor.core.JSONSerializable

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
  val arbeitskategorien: Seq[ArbeitskategorieId]
  val anzahlPersonen: Option[Int]
  val mehrPersonenOk: Boolean
  val einsatzZeit: Option[Int]
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
  arbeitskategorien: Seq[ArbeitskategorieId],
  anzahlPersonen: Option[Int],
  mehrPersonenOk: Boolean,
  einsatzZeit: Option[Int],
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
  arbeitskategorien: Seq[ArbeitskategorieId],
  anzahlPersonen: Option[Int],
  mehrPersonenOk: Boolean,
  einsatzZeit: Option[Int],
  status: ArbeitseinsatzStatus
) extends JSONSerializable

case class ArbeitseinsatzId(id: Long) extends BaseId

trait IArbeitseinsatz extends BaseEntity[ArbeitseinsatzId] {
  val id: ArbeitseinsatzId
  val arbeitsangebotId: ArbeitsangebotId
  val arbeitsangebotTitel: String
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
}

case class Arbeitseinsatz(
  id: ArbeitseinsatzId,
  arbeitsangebotId: ArbeitsangebotId,
  arbeitsangebotTitel: String,
  zeitVon: DateTime,
  zeitBis: DateTime,
  kundeId: KundeId,
  kundeBezeichnung: String,
  personId: Option[PersonId],
  personName: Option[String],
  aboId: Option[AboId],
  aboBezeichnung: Option[String],
  anzahlPersonen: Int,
  bemerkungen: Option[String],
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
  zeitVon: DateTime,
  zeitBis: DateTime,
  kundeId: KundeId,
  kundeBezeichnung: String,
  aboId: Option[AboId],
  aboBezeichnung: Option[String],
  personId: Option[PersonId],
  personName: Option[String],
  anzahlPersonen: Int,
  bemerkungen: Option[String],
  //additional Detail fields
  arbeitsangebot: Arbeitsangebot,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[ArbeitseinsatzId]

case class ArbeitseinsatzModify(
  arbeitsangebotId: ArbeitsangebotId,
  arbeitsangebotTitel: String,
  zeitVon: DateTime,
  zeitBis: DateTime,
  kundeId: KundeId,
  kundeBezeichnung: String,
  personId: Option[PersonId],
  personName: Option[String],
  aboId: Option[AboId],
  aboBezeichnung: Option[String],
  anzahlPersonen: Int,
  bemerkungen: Option[String]
) extends JSONSerializable

case class ArbeitseinsatzCreate(
  arbeitsangebotId: ArbeitsangebotId,
  kundeId: KundeId,
  anzahlPersonen: Int,
  bemerkungen: Option[String]
) extends JSONSerializable
