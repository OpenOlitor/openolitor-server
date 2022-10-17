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
package ch.openolitor.arbeitseinsatz

import ch.openolitor.arbeitseinsatz.models.{ ArbeitsangebotMailRequest, _ }
import ch.openolitor.arbeitseinsatz.ArbeitseinsatzCommandHandler.SendEmailToArbeitsangebotPersonenEvent
import ch.openolitor.core.BaseJsonProtocol
import ch.openolitor.core.models.PersonId
import ch.openolitor.stammdaten.StammdatenJsonProtocol
import ch.openolitor.stammdaten.models.{ AboId, KundeId, PersonContact, ProjektReport }
import com.typesafe.scalalogging.LazyLogging
import org.joda.time.DateTime
import spray.json._

/**
 * JSON Format deklarationen für das Modul rbeitseinsatz
 */
trait ArbeitseinsatzJsonProtocol extends BaseJsonProtocol with LazyLogging with StammdatenJsonProtocol {

  //enum formats
  implicit val arbeitseinsatzStatusFormat = enumFormat(ArbeitseinsatzStatus.apply)

  //id formats
  implicit val arbeitskategorieIdFormat = baseIdFormat(ArbeitskategorieId)
  implicit val arbeitsangebotIdFormat = baseIdFormat(ArbeitsangebotId)
  implicit val arbeitseinsatzIdFormat = baseIdFormat(ArbeitseinsatzId)

  implicit val arbeitskategorieBezIdFormat = new RootJsonFormat[ArbeitskategorieBez] {
    def write(obj: ArbeitskategorieBez): JsValue =
      JsString(obj.id)

    def read(json: JsValue): ArbeitskategorieBez =
      json match {
        case JsString(id) => ArbeitskategorieBez(id)
        case kt           => sys.error(s"Unknown ArbeitskategorieBez:$kt")
      }
  }

  implicit val arbeitskategorieFormat = jsonFormat6(Arbeitskategorie)
  implicit val arbeitsangebotFormat = jsonFormat17(Arbeitsangebot)
  implicit val arbeitsangeboteDuplicateFormat = jsonFormat2(ArbeitsangeboteDuplicate)
  implicit val arbeitseinsatzFormat = jsonFormat22(Arbeitseinsatz)
  implicit val arbeitseinsatzCreateFormat = jsonFormat6(ArbeitseinsatzCreate)

  implicit val personContactFormat = jsonFormat2(PersonContact)

  implicit val arbeitseinsatzDetailFormat = new RootJsonFormat[ArbeitseinsatzDetail] {
    override def read(json: JsValue): ArbeitseinsatzDetail = {
      val fields = json.asJsObject.fields
      ArbeitseinsatzDetail(
        fields("id").convertTo[ArbeitseinsatzId],
        fields("arbeitsangebotId").convertTo[ArbeitsangebotId],
        fields("arbeitsangebotTitel").convertTo[String],
        fields("arbeitsangebotStatus").convertTo[ArbeitseinsatzStatus],
        fields("zeitVon").convertTo[DateTime],
        fields("zeitBis").convertTo[DateTime],
        fields.get("einsatzZeit").fold(Option.empty[BigDecimal])(_.convertTo[Option[BigDecimal]]),
        fields("kundeId").convertTo[KundeId],
        fields("kundeBezeichnung").convertTo[String],
        fields.get("aboId").fold(Option.empty[AboId])(_.convertTo[Option[AboId]]),
        fields.get("aboBezeichnung").fold(Option.empty[String])(_.convertTo[Option[String]]),
        fields.get("personId").fold(Option.empty[PersonId])(_.convertTo[Option[PersonId]]),
        fields.get("personName").fold(Option.empty[String])(_.convertTo[Option[String]]),
        fields("anzahlPersonen").convertTo[Int],
        fields.get("bemerkungen").fold(Option.empty[String])(_.convertTo[Option[String]]),
        fields.get("email").fold(Option.empty[String])(_.convertTo[Option[String]]),
        fields.get("telefonMobil").fold(Option.empty[String])(_.convertTo[Option[String]]),
        fields("contactPermission").convertTo[Boolean],
        fields("arbeitsangebot").convertTo[Arbeitsangebot],
        fields("coworkers").convertTo[PersonContact],
        fields("erstelldat").convertTo[DateTime],
        fields("ersteller").convertTo[PersonId],
        fields("modifidat").convertTo[DateTime],
        fields("modifikator").convertTo[PersonId]
      )
    }

    override def write(obj: ArbeitseinsatzDetail): JsValue = JsObject(
      Map(
        "id" -> obj.id.toJson,
        "arbeitsangebotId" -> obj.arbeitsangebotId.toJson,
        "arbeitsangebotTitel" -> obj.arbeitsangebotTitel.toJson,
        "arbeitsangebotStatus" -> obj.arbeitsangebotStatus.toJson,
        "zeitVon" -> obj.zeitVon.toJson,
        "zeitBis" -> obj.zeitBis.toJson,
        "einsatzZeit" -> obj.einsatzZeit.toJson,
        "kundeId" -> obj.kundeId.toJson,
        "kundeBezeichnung" -> obj.kundeBezeichnung.toJson,
        "aboId" -> obj.aboId.toJson,
        "aboBezeichnung" -> obj.aboBezeichnung.toJson,
        "personId" -> obj.personId.toJson,
        "personName" -> obj.personName.toJson,
        "anzahlPersonen" -> obj.anzahlPersonen.toJson,
        "bemerkungen" -> obj.bemerkungen.toJson,
        "email" -> obj.email.toJson,
        "telefonMobil" -> obj.telefonMobil.toJson,
        "contactPermission" -> obj.contactPermission.toJson,
        //additional Detail fields
        "arbeitsangebot" -> obj.arbeitsangebot.toJson,
        "coworkers" -> obj.coworkers.toJson,
        //modification flags
        "erstelldat" -> obj.erstelldat.toJson,
        "ersteller" -> obj.ersteller.toJson,
        "modifidat" -> obj.modifidat.toJson,
        "modifikator" -> obj.modifikator.toJson
      ).filterNot(_._2 == JsNull)
    )
  }

  implicit val arbeitseinsatzDetailReportFormat = new RootJsonFormat[ArbeitseinsatzDetailReport] {
    override def read(json: JsValue): ArbeitseinsatzDetailReport = {
      val fields = json.asJsObject.fields
      ArbeitseinsatzDetailReport(
        fields("id").convertTo[ArbeitseinsatzId],
        fields("arbeitsangebotId").convertTo[ArbeitsangebotId],
        fields("arbeitsangebotTitel").convertTo[String],
        fields("arbeitsangebotStatus").convertTo[ArbeitseinsatzStatus],
        fields("zeitVon").convertTo[DateTime],
        fields("zeitBis").convertTo[DateTime],
        fields.get("einsatzZeit").fold(Option.empty[BigDecimal])(_.convertTo[Option[BigDecimal]]),
        fields("kundeId").convertTo[KundeId],
        fields("kundeBezeichnung").convertTo[String],
        fields.get("aboId").fold(Option.empty[AboId])(_.convertTo[Option[AboId]]),
        fields.get("aboBezeichnung").fold(Option.empty[String])(_.convertTo[Option[String]]),
        fields.get("personId").fold(Option.empty[PersonId])(_.convertTo[Option[PersonId]]),
        fields.get("personName").fold(Option.empty[String])(_.convertTo[Option[String]]),
        fields("anzahlPersonen").convertTo[Int],
        fields.get("bemerkungen").fold(Option.empty[String])(_.convertTo[Option[String]]),
        fields.get("email").fold(Option.empty[String])(_.convertTo[Option[String]]),
        fields.get("telefonMobil").fold(Option.empty[String])(_.convertTo[Option[String]]),
        fields("arbeitsangebot").convertTo[Arbeitsangebot],
        fields("projekt").convertTo[ProjektReport],
        fields("erstelldat").convertTo[DateTime],
        fields("ersteller").convertTo[PersonId],
        fields("modifidat").convertTo[DateTime],
        fields("modifikator").convertTo[PersonId]
      )
    }

    override def write(obj: ArbeitseinsatzDetailReport): JsValue = JsObject(
      Map(
        "id" -> obj.id.toJson,
        "arbeitsangebotId" -> obj.arbeitsangebotId.toJson,
        "arbeitsangebotTitel" -> obj.arbeitsangebotTitel.toJson,
        "arbeitsangebotStatus" -> obj.arbeitsangebotStatus.toJson,
        "zeitVon" -> obj.zeitVon.toJson,
        "zeitBis" -> obj.zeitBis.toJson,
        "einsatzZeit" -> obj.einsatzZeit.toJson,
        "kundeId" -> obj.kundeId.toJson,
        "kundeBezeichnung" -> obj.kundeBezeichnung.toJson,
        "aboId" -> obj.aboId.toJson,
        "aboBezeichnung" -> obj.aboBezeichnung.toJson,
        "personId" -> obj.personId.toJson,
        "personName" -> obj.personName.toJson,
        "anzahlPersonen" -> obj.anzahlPersonen.toJson,
        "bemerkungen" -> obj.bemerkungen.toJson,
        "email" -> obj.email.toJson,
        "telefonMobil" -> obj.telefonMobil.toJson,
        "arbeitsangebot" -> obj.arbeitsangebot.toJson,
        "projekt" -> obj.projekt.toJson,
        "erstelldat" -> obj.erstelldat.toJson,
        "ersteller" -> obj.ersteller.toJson,
        "modifidat" -> obj.modifidat.toJson,
        "modifikator" -> obj.modifikator.toJson
      ).filterNot(_._2 == JsNull)
    )
  }

  implicit val arbeitskategorieModifyFormat = jsonFormat1(ArbeitskategorieModify)
  implicit val arbeitsangebotModifyFormat = jsonFormat11(ArbeitsangebotModify)
  implicit val arbeitseinsatzModifyFormat = jsonFormat10(ArbeitseinsatzModify)

  implicit val arbeitseinsatzAbrechnungFormat = jsonFormat5(ArbeitseinsatzAbrechnung)

  implicit val arbeitsangebotDuplicateFormat = jsonFormat2(ArbeitsangebotDuplicate)
  implicit val arbeitsComplexFlagsFormat = jsonFormat1(ArbeitsComplexFlags)
  implicit val optionArbeitsComplexFlagsFormat = new OptionFormat[ArbeitsComplexFlags]

  implicit val arbeitsangebotMailContextFormat = jsonFormat2(ArbeitsangebotMailContext)
  implicit val arbeitsangebotMailRequestFormat = jsonFormat4(ArbeitsangebotMailRequest)

  // event formats
  implicit val sendEmailToArbeitsangebotPersonenEventFormat = jsonFormat5(SendEmailToArbeitsangebotPersonenEvent)
}
