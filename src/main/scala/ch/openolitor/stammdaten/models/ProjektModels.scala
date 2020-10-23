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

import java.util.Locale

import ch.openolitor.core.JSONSerializable
import ch.openolitor.core.models._
import ch.openolitor.core.scalax.Tuple28
import org.joda.time.{ DateTime, LocalDate }

sealed trait EinsatzEinheit extends Product

object EinsatzEinheit {
  def apply(value: String): EinsatzEinheit = {
    Vector(Stunden, Halbtage, Tage, Punkte) find (_.toString == value) getOrElse (Stunden)
  }
}

case object Stunden extends EinsatzEinheit
case object Halbtage extends EinsatzEinheit
case object Tage extends EinsatzEinheit
case object Punkte extends EinsatzEinheit

case class ProjektId(id: Long) extends BaseId

case class Geschaeftsjahr(monat: Int, tag: Int) {

  /**
   * Errechnet den Start des Geschäftsjahres aufgrund eines Datums
   */
  def start(date: LocalDate = LocalDate.now): LocalDate = {
    val geschaftsjahrInJahr = new LocalDate(date.year.get, monat, tag)
    date match {
      case d if d.isBefore(geschaftsjahrInJahr) =>
        //Wir sind noch im "alten" Geschäftsjahr
        geschaftsjahrInJahr.minusYears(1)
      case d =>
        //Wir sind bereits im neuen Geschäftsjahr
        geschaftsjahrInJahr
    }
  }

  /**
   * Errechnet der Key für ein Geschäftsjahr aufgrund eines Datum. Der Key des Geschäftsjahres leitet sich aus dem Startdatum
   * des Geschäftsjahres ab. Wird der Start des Geschäftsjahres auf den Start des Kalenderjahres gesetzt, wird das Kalenderjahr als
   * key benutzt, ansonsten setzt sich der Key aus Monat/Jahr zusammen
   */
  def key(date: LocalDate = LocalDate.now): String = {
    val startDate = start(date)
    if (monat == 1 && tag == 1) {
      startDate.year.getAsText
    } else {
      s"${startDate.getMonthOfYear}/${startDate.getYear}"
    }
  }

  /**
   * Retourniert 'true' wenn die übergebenen Daten im selben Geschäftsjahr liegen. 'false' wenn dies nicht so ist.
   * Wird nur ein Datum übergeben wird zum aktuelle Moment verglichen.
   */
  def isInSame(date: LocalDate, comparteTo: LocalDate = LocalDate.now): Boolean = {
    key(date) == key(comparteTo)
  }
}

case class Projekt(
  id: ProjektId,
  bezeichnung: String,
  strasse: Option[String],
  hausNummer: Option[String],
  adressZusatz: Option[String],
  plz: Option[String],
  ort: Option[String],
  preiseSichtbar: Boolean,
  preiseEditierbar: Boolean,
  emailErforderlich: Boolean,
  waehrung: Waehrung,
  geschaeftsjahrMonat: Int,
  geschaeftsjahrTag: Int,
  twoFactorAuthentication: Map[Rolle, Boolean],
  sprache: Locale,
  welcomeMessage1: Option[String],
  welcomeMessage2: Option[String],
  maintenanceMode: Boolean,
  generierteMailsSenden: Boolean,
  einsatzEinheit: EinsatzEinheit,
  einsatzAbsageVorlaufTage: Int,
  einsatzShowListeKunde: Boolean,
  sendEmailToBcc: Boolean,
  defaultPaymentType: Option[String],
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[ProjektId] {
  lazy val geschaftsjahr = Geschaeftsjahr(geschaeftsjahrMonat, geschaeftsjahrTag)
}

object Projekt {
  def unapply(o: Projekt) = {
    Some(Tuple28(
      o.id,
      o.bezeichnung,
      o.strasse,
      o.hausNummer,
      o.adressZusatz,
      o.plz,
      o.ort,
      o.preiseSichtbar,
      o.preiseEditierbar,
      o.emailErforderlich,
      o.waehrung,
      o.geschaeftsjahrMonat,
      o.geschaeftsjahrTag,
      o.twoFactorAuthentication,
      o.sprache,
      o.welcomeMessage1,
      o.welcomeMessage2,
      o.maintenanceMode,
      o.generierteMailsSenden,
      o.einsatzEinheit,
      o.einsatzAbsageVorlaufTage,
      o.einsatzShowListeKunde,
      o.sendEmailToBcc,
      o.defaultPaymentType,
      o.erstelldat,
      o.ersteller,
      o.modifidat,
      o.modifikator
    ))
  }

  def build(
    id: ProjektId = ProjektId(0),
    bezeichnung: String,
    strasse: Option[String] = None,
    hausNummer: Option[String] = None,
    adressZusatz: Option[String] = None,
    plz: Option[String] = None,
    ort: Option[String] = None,
    preiseSichtbar: Boolean = false,
    preiseEditierbar: Boolean = false,
    emailErforderlich: Boolean = false,
    waehrung: Waehrung = CHF,
    geschaeftsjahrMonat: Int = 1,
    geschaeftsjahrTag: Int = 1,
    twoFactorAuthentication: Map[Rolle, Boolean] = Map(),
    sprache: Locale = Locale.GERMAN,
    welcomeMessage1: Option[String] = None,
    welcomeMessage2: Option[String] = None,
    maintenanceMode: Boolean = false,
    generierteMailsSenden: Boolean = false,
    einsatzEinheit: EinsatzEinheit = Stunden,
    einsatzAbsageVorlaufTage: Int = 3,
    einsatzShowListeKunde: Boolean = true,
    sendEmailToBcc: Boolean = true,
    defaultPaymentType: Option[String] = None
  )(implicit person: PersonId): Projekt = {
    Projekt(
      id,
      bezeichnung,
      strasse,
      hausNummer,
      adressZusatz,
      plz,
      ort,
      preiseSichtbar,
      preiseEditierbar,
      emailErforderlich,
      waehrung,
      geschaeftsjahrMonat,
      geschaeftsjahrTag,
      twoFactorAuthentication,
      sprache,
      welcomeMessage1,
      welcomeMessage2,
      maintenanceMode,
      generierteMailsSenden,
      einsatzEinheit,
      einsatzAbsageVorlaufTage,
      einsatzShowListeKunde,
      sendEmailToBcc,
      defaultPaymentType,
      erstelldat = DateTime.now,
      ersteller = person,
      modifidat = DateTime.now,
      modifikator = person
    )
  }
}

case class ProjektPublik(
  id: ProjektId,
  bezeichnung: String,
  strasse: Option[String],
  hausNummer: Option[String],
  adressZusatz: Option[String],
  plz: Option[String],
  ort: Option[String],
  preiseSichtbar: Boolean,
  waehrung: Waehrung,
  geschaeftsjahrMonat: Int,
  geschaeftsjahrTag: Int,
  welcomeMessage1: Option[String],
  maintenanceMode: Boolean,
  einsatzEinheit: EinsatzEinheit,
  einsatzAbsageVorlaufTage: Int,
  einsatzShowListeKunde: Boolean,
  sendEmailToBcc: Boolean,
  defaultPaymentType: Option[String]
) extends JSONSerializable

case class ProjektReport(
  id: ProjektId,
  bezeichnung: String,
  strasse: Option[String],
  hausNummer: Option[String],
  adressZusatz: Option[String],
  plz: Option[String],
  ort: Option[String],
  preiseSichtbar: Boolean,
  preiseEditierbar: Boolean,
  emailErforderlich: Boolean,
  waehrung: Waehrung,
  geschaeftsjahrMonat: Int,
  geschaeftsjahrTag: Int,
  twoFactorAuthentication: Map[Rolle, Boolean],
  sprache: Locale,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[ProjektId] {
  lazy val geschaftsjahr = Geschaeftsjahr(geschaeftsjahrMonat, geschaeftsjahrTag)
  lazy val strasseUndNummer = strasse.map(_ + hausNummer.map(" " + _).getOrElse(""))
  lazy val plzOrt = plz.map(_ + ort.map(" " + _).getOrElse(""))

  lazy val adresszeilen = Seq(
    Some(bezeichnung),
    adressZusatz,
    strasseUndNummer,
    plzOrt
  ).flatten.padTo(6, "")
}

case class ProjektModify(
  bezeichnung: String,
  strasse: Option[String],
  hausNummer: Option[String],
  adressZusatz: Option[String],
  plz: Option[String],
  ort: Option[String],
  preiseSichtbar: Boolean,
  preiseEditierbar: Boolean,
  emailErforderlich: Boolean,
  waehrung: Waehrung,
  geschaeftsjahrMonat: Int,
  geschaeftsjahrTag: Int,
  twoFactorAuthentication: Map[Rolle, Boolean],
  sprache: Locale,
  welcomeMessage1: Option[String],
  welcomeMessage2: Option[String],
  maintenanceMode: Boolean,

  generierteMailsSenden: Boolean,
  einsatzEinheit: EinsatzEinheit,
  einsatzAbsageVorlaufTage: Int,
  einsatzShowListeKunde: Boolean,
  sendEmailToBcc: Boolean,
  defaultPaymentType: Option[String]
) extends JSONSerializable

@deprecated("This class exists for compatibility purposes only", "OO 2.2 (Arbeitseinsatz)")
case class ProjektV1(
  id: ProjektId,
  bezeichnung: String,
  strasse: Option[String],
  hausNummer: Option[String],
  adressZusatz: Option[String],
  plz: Option[String],
  ort: Option[String],
  preiseSichtbar: Boolean,
  preiseEditierbar: Boolean,
  emailErforderlich: Boolean,
  waehrung: Waehrung,
  geschaeftsjahrMonat: Int,
  geschaeftsjahrTag: Int,
  twoFactorAuthentication: Map[Rolle, Boolean],
  sprache: Locale,
  welcomeMessage1: Option[String],
  welcomeMessage2: Option[String],
  maintenanceMode: Boolean,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[ProjektId] {
  lazy val geschaftsjahr = Geschaeftsjahr(geschaeftsjahrMonat, geschaeftsjahrTag)
}

case class KundentypId(id: String) extends BaseStringId

case class CustomKundentypId(id: Long) extends BaseId

case class CustomKundentyp(
  id: CustomKundentypId,
  val kundentyp: KundentypId,
  val beschreibung: Option[String],
  anzahlVerknuepfungen: Int,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[CustomKundentypId]

// Don't use!
case class CustomKundentypModifyV1(beschreibung: Option[String]) extends JSONSerializable

case class CustomKundentypModify(kundentyp: KundentypId, id: CustomKundentypId, beschreibung: Option[String]) extends JSONSerializable
case class CustomKundentypCreate(kundentyp: KundentypId, beschreibung: Option[String]) extends JSONSerializable
