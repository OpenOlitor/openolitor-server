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
package ch.openolitor.stammdaten.models

import org.joda.time.DateTime
import ch.openolitor.core.models._
import ch.openolitor.core.JSONSerializable

case class SammelbestellungId(id: Long) extends BaseId

case class Sammelbestellung(
  id: SammelbestellungId,
  produzentId: ProduzentId,
  produzentKurzzeichen: String,
  lieferplanungId: LieferplanungId,
  status: LieferungStatus,
  datum: DateTime,
  datumAbrechnung: Option[DateTime],
  datumVersendet: Option[DateTime],
  preisTotal: BigDecimal,
  steuerSatz: Option[BigDecimal],
  steuer: BigDecimal,
  totalSteuer: BigDecimal,

  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[SammelbestellungId]

case class SammelbestellungMail(
  id: SammelbestellungId,
  produzentId: ProduzentId,
  produzentKurzzeichen: String,
  lieferplanungId: LieferplanungId,
  status: LieferungStatus,
  datum: DateTime,
  datumAbrechnung: Option[DateTime],
  datumVersendet: Option[DateTime],
  preisTotal: BigDecimal,
  steuerSatz: Option[BigDecimal],
  steuer: BigDecimal,
  totalSteuer: BigDecimal,
  bestellungen: Seq[BestellungMail],
  projekt: Projekt,
  produzent: Produzent,

  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[SammelbestellungId]

object SammelbestellungMail {
  def build(
    id: SammelbestellungId = SammelbestellungId(0),
    produzentId: ProduzentId = ProduzentId(0),
    produzentKurzzeichen: String,
    lieferplanungId: LieferplanungId = LieferplanungId(0),
    status: LieferungStatus,
    datum: DateTime,
    datumAbrechnung: Option[DateTime] = None,
    datumVersendet: Option[DateTime] = None,
    preisTotal: BigDecimal,
    steuerSatz: Option[BigDecimal] = None,
    steuer: BigDecimal,
    totalSteuer: BigDecimal,
    bestellungen: Seq[BestellungMail] = Seq(),
    projekt: Projekt,
    produzent: Produzent
  )(implicit person: PersonId): SammelbestellungMail = SammelbestellungMail(
    id,
    produzentId,
    produzentKurzzeichen,
    lieferplanungId,
    status,
    datum,
    datumAbrechnung,
    datumVersendet,
    preisTotal,
    steuerSatz,
    steuer,
    totalSteuer,
    bestellungen,
    projekt,
    produzent,
    //modification flags
    erstelldat = DateTime.now,
    ersteller = person,
    modifidat = DateTime.now,
    modifikator = person
  )
}

case class SammelbestellungDetail(
  id: SammelbestellungId,
  produzentId: ProduzentId,
  produzentKurzzeichen: String,
  lieferplanungId: LieferplanungId,
  status: LieferungStatus,
  datum: DateTime,
  datumAbrechnung: Option[DateTime],
  datumVersendet: Option[DateTime],
  preisTotal: BigDecimal,
  steuerSatz: Option[BigDecimal],
  steuer: BigDecimal,
  totalSteuer: BigDecimal,

  produzent: Produzent,
  bestellungen: Seq[BestellungDetail],

  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends JSONSerializable

case class BestellungId(id: Long) extends BaseId

/**
 * Eine Bestellung wird pro adminProzente unter Sammelbestellung gruppiert.
 */
case class Bestellung(
  id: BestellungId,
  sammelbestellungId: SammelbestellungId,
  // Summe der Preise der Bestellpositionen
  preisTotal: BigDecimal,
  steuerSatz: Option[BigDecimal],
  // Berechnete Steuer nach Abzug (adminProzenteAbzug)
  steuer: BigDecimal,
  totalSteuer: BigDecimal,
  adminProzente: BigDecimal,
  // Berechneter Abzug auf preisTotal
  adminProzenteAbzug: BigDecimal,
  totalNachAbzugAdminProzente: BigDecimal,

  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[BestellungId]

case class BestellungMail(
  id: BestellungId,
  sammelbestellungId: SammelbestellungId,
  // Summe der Preise der Bestellpositionen
  preisTotal: BigDecimal,
  steuerSatz: Option[BigDecimal],
  // Berechnete Steuer nach Abzug (adminProzenteAbzug)
  steuer: BigDecimal,
  totalSteuer: BigDecimal,
  adminProzente: BigDecimal,
  bestellpositionen: Seq[BestellpositionMail],
  // Berechneter Abzug auf preisTotal
  adminProzenteAbzug: BigDecimal,
  totalNachAbzugAdminProzente: BigDecimal,

  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[BestellungId] {
  def hasAdminProzente: Boolean = adminProzente > 0
}

object BestellungMail {
  def build(
    id: BestellungId = BestellungId(0),
    sammelbestellungId: SammelbestellungId = SammelbestellungId(0),
    // Summe der Preise der Bestellpositionen
    preisTotal: BigDecimal,
    steuerSatz: Option[BigDecimal] = None,
    // Berechnete Steuer nach Abzug (adminProzenteAbzug)
    steuer: BigDecimal,
    totalSteuer: BigDecimal,
    adminProzente: BigDecimal,
    bestellpositionen: Seq[BestellpositionMail] = Seq(),
    // Berechneter Abzug auf preisTotal
    adminProzenteAbzug: BigDecimal,
    totalNachAbzugAdminProzente: BigDecimal
  )(implicit person: PersonId): BestellungMail = BestellungMail(
    id,
    sammelbestellungId,
    // Summe der Preise der Bestellpositionen
    preisTotal,
    steuerSatz,
    // Berechnete Steuer nach Abzug (adminProzenteAbzug)
    steuer,
    totalSteuer,
    adminProzente,
    bestellpositionen,
    // Berechneter Abzug auf preisTotal
    adminProzenteAbzug,
    totalNachAbzugAdminProzente,
    //modification flags
    erstelldat = DateTime.now,
    ersteller = person,
    modifidat = DateTime.now,
    modifikator = person
  )
}

case class BestellungDetail(
  id: BestellungId,
  preisTotal: BigDecimal,
  steuerSatz: Option[BigDecimal],
  steuer: BigDecimal,
  totalSteuer: BigDecimal,
  adminProzente: BigDecimal,
  adminProzenteAbzug: BigDecimal,
  totalNachAbzugAdminProzente: BigDecimal,
  positionen: Seq[Bestellposition],
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[BestellungId]

@Deprecated
case class BestellungAusgeliefert(
  datum: DateTime,
  ids: Seq[BestellungId]
) extends JSONSerializable

case class SammelbestellungAusgeliefert(
  datum: DateTime,
  ids: Seq[SammelbestellungId]
) extends JSONSerializable

@Deprecated
case class BestellungenCreate(
  lieferplanungId: LieferplanungId
) extends JSONSerializable

@Deprecated
case class BestellungCreate(
  produzentId: ProduzentId,
  lieferplanungId: LieferplanungId,
  datum: DateTime
) extends JSONSerializable

case class SammelbestellungModify(
  produzentId: ProduzentId,
  lieferplanungId: LieferplanungId,
  datum: DateTime
) extends JSONSerializable

case class SammelbestellungStatusModify(
  status: LieferungStatus
) extends JSONSerializable

case class SammelbestellungCreate(
  id: SammelbestellungId,
  produzentId: ProduzentId,
  lieferplanungId: LieferplanungId,
  datum: DateTime
) extends JSONSerializable

case class BestellpositionId(id: Long) extends BaseId

trait BestellpositionCalculatedFields {
  val menge: BigDecimal
  val anzahl: Int
  val preisEinheit: Option[BigDecimal]

  lazy val mengeTotal = anzahl * menge
  lazy val preisPos = (preisEinheit.getOrElse(BigDecimal(0)) * menge).setScale(2, BigDecimal.RoundingMode.HALF_UP)
  lazy val detail = if (preisEinheit.getOrElse(BigDecimal(0)).compare(preisPos) == 0) "" else s""" ≙ ${preisPos}"""
}

case class Bestellposition(
  id: BestellpositionId,
  bestellungId: BestellungId,
  produktId: Option[ProduktId],
  produktBeschrieb: String,
  preisEinheit: Option[BigDecimal],
  einheit: Liefereinheit,
  menge: BigDecimal,
  preis: Option[BigDecimal],
  anzahl: Int,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[BestellpositionId] with BestellpositionCalculatedFields

case class BestellpositionMail(
  id: BestellpositionId,
  bestellungId: BestellungId,
  produktId: Option[ProduktId],
  produktBeschrieb: String,
  preisEinheit: Option[BigDecimal],
  einheit: Liefereinheit,
  menge: BigDecimal,
  preis: Option[BigDecimal],
  anzahl: Int,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[BestellpositionId] with BestellpositionCalculatedFields

object BestellpositionMail {
  def build(
    id: BestellpositionId = BestellpositionId(0),
    bestellungId: BestellungId = BestellungId(0),
    produktId: Option[ProduktId] = None,
    produktBeschrieb: String,
    preisEinheit: Option[BigDecimal] = None,
    einheit: Liefereinheit,
    menge: BigDecimal,
    preis: Option[BigDecimal] = None,
    anzahl: Int
  )(implicit person: PersonId): BestellpositionMail = BestellpositionMail(
    id,
    bestellungId,
    produktId,
    produktBeschrieb,
    preisEinheit,
    einheit,
    menge,
    preis,
    anzahl,
    //modification flags
    erstelldat = DateTime.now,
    ersteller = person,
    modifidat = DateTime.now,
    modifikator = person
  )
}

case class BestellpositionModify(
  bestellungId: BestellungId,
  produktId: Option[ProduktId],
  produktBeschrieb: String,
  preisEinheit: Option[BigDecimal],
  einheit: Liefereinheit,
  menge: BigDecimal,
  preis: Option[BigDecimal],
  anzahl: Int
) extends JSONSerializable

case class ProduzentenabrechnungReport(
  produzentId: ProduzentId,
  produzentKurzzeichen: String,
  produzent: ProduzentDetailReport,
  bestellungenDetails: Seq[SammelbestellungDetail],
  preisTotal: BigDecimal,
  steuerSatz: Option[BigDecimal],
  steuer: BigDecimal,
  totalSteuer: BigDecimal,
  //Zusatzinformationen
  projekt: ProjektReport
) extends JSONSerializable

