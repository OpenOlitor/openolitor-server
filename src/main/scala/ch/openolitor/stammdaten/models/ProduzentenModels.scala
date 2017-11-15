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

import ch.openolitor.core.models._
import org.joda.time.DateTime
import ch.openolitor.core.scalax.Tuple23
import ch.openolitor.core.JSONSerializable

case class BaseProduzentId(id: String) extends BaseStringId

case class ProduzentId(id: Long) extends BaseId

trait IProduzent extends BaseEntity[ProduzentId] {
  val id: ProduzentId
  val name: String
  val vorname: Option[String]
  val kurzzeichen: String
  val strasse: Option[String]
  val hausNummer: Option[String]
  val adressZusatz: Option[String]
  val plz: String
  val ort: String
  val bemerkungen: Option[String]
  val email: String
  val telefonMobil: Option[String]
  val telefonFestnetz: Option[String]
  val iban: Option[String] //maybe use dedicated type
  val bank: Option[String]
  val mwst: Boolean
  val mwstSatz: Option[BigDecimal]
  val mwstNr: Option[String]
  val aktiv: Boolean
}

case class Produzent(
  id: ProduzentId,
  name: String,
  vorname: Option[String],
  kurzzeichen: String,
  strasse: Option[String],
  hausNummer: Option[String],
  adressZusatz: Option[String],
  plz: String,
  ort: String,
  bemerkungen: Option[String],
  email: String,
  telefonMobil: Option[String],
  telefonFestnetz: Option[String],
  iban: Option[String], //maybe use dedicated type
  bank: Option[String],
  mwst: Boolean,
  mwstSatz: Option[BigDecimal],
  mwstNr: Option[String],
  aktiv: Boolean,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends IProduzent

object Produzent {
  def unapply(p: Produzent) = {
    Some(Tuple23(
      p.id: ProduzentId,
      p.name: String,
      p.vorname: Option[String],
      p.kurzzeichen: String,
      p.strasse: Option[String],
      p.hausNummer: Option[String],
      p.adressZusatz: Option[String],
      p.plz: String,
      p.ort: String,
      p.bemerkungen: Option[String],
      p.email: String,
      p.telefonMobil: Option[String],
      p.telefonFestnetz: Option[String],
      p.iban: Option[String], //maybe use dedicated type
      p.bank: Option[String],
      p.mwst: Boolean,
      p.mwstSatz: Option[BigDecimal],
      p.mwstNr: Option[String],
      p.aktiv: Boolean,
      //modification flags
      p.erstelldat: DateTime,
      p.ersteller: PersonId,
      p.modifidat: DateTime,
      p.modifikator: PersonId
    ))
  }

  def build(
    id: ProduzentId = ProduzentId(0),
    name: String,
    vorname: Option[String] = None,
    kurzzeichen: String,
    strasse: Option[String] = None,
    hausNummer: Option[String] = None,
    adressZusatz: Option[String] = None,
    plz: String,
    ort: String,
    bemerkungen: Option[String] = None,
    email: String,
    telefonMobil: Option[String] = None,
    telefonFestnetz: Option[String] = None,
    iban: Option[String] = None,
    bank: Option[String] = None,
    mwst: Boolean = false,
    mwstSatz: Option[BigDecimal] = None,
    mwstNr: Option[String] = None,
    aktiv: Boolean = true
  )(implicit person: PersonId): Produzent = {
    Produzent(
      id,
      name,
      vorname,
      kurzzeichen,
      strasse,
      hausNummer,
      adressZusatz,
      plz,
      ort,
      bemerkungen,
      email,
      telefonMobil,
      telefonFestnetz,
      iban,
      bank,
      mwst,
      mwstSatz,
      mwstNr,
      aktiv,
      //modification flags
      erstelldat = DateTime.now,
      ersteller = person,
      modifidat = DateTime.now,
      modifikator = person
    )
  }

}

case class ProduzentModify(
  name: String,
  vorname: Option[String],
  kurzzeichen: String,
  strasse: Option[String],
  hausNummer: Option[String],
  adressZusatz: Option[String],
  plz: String,
  ort: String,
  bemerkungen: Option[String],
  email: String,
  telefonMobil: Option[String],
  telefonFestnetz: Option[String],
  iban: Option[String], //maybe use dedicated type
  bank: Option[String],
  mwst: Boolean,
  mwstSatz: Option[BigDecimal],
  mwstNr: Option[String],
  aktiv: Boolean
) extends JSONSerializable

case class ProduzentDetailReport(
  id: ProduzentId,
  name: String,
  vorname: Option[String],
  kurzzeichen: String,
  strasse: Option[String],
  hausNummer: Option[String],
  adressZusatz: Option[String],
  plz: String,
  ort: String,
  bemerkungen: Option[String],
  email: String,
  telefonMobil: Option[String],
  telefonFestnetz: Option[String],
  iban: Option[String], //maybe use dedicated type
  bank: Option[String],
  mwst: Boolean,
  mwstSatz: Option[BigDecimal],
  mwstNr: Option[String],
  aktiv: Boolean,
  //Report infos
  projekt: ProjektReport,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[ProduzentId] with IProduzentReport with JSONSerializable

trait IProduzentReport extends IProduzent {
  lazy val strasseUndNummer = strasse.map(_ + hausNummer.map(" " + _).getOrElse(""))
  lazy val plzOrt = plz + " " + ort

  lazy val adresszeilen = Seq(
    Some(name),
    strasseUndNummer,
    Some(plzOrt)
  ).flatten.padTo(6, "")
}
