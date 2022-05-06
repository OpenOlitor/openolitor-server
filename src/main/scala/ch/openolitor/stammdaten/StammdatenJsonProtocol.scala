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
package ch.openolitor.stammdaten

import ch.openolitor.core.{ BaseJsonProtocol, JSONSerializable }
import ch.openolitor.core.models.PersonId
import ch.openolitor.core.reporting.ReportJsonProtocol
import ch.openolitor.core.reporting.models.MultiReport
import ch.openolitor.stammdaten.models.{ Depotlieferung, LieferplanungCreated, ProjektKundenportal, Sammelbestellung, _ }
import ch.openolitor.stammdaten.StammdatenCommandHandler.{ AboAktiviertEvent, AboDeaktiviertEvent, AbwesenheitCreateEvent, AuslieferungAlsAusgeliefertMarkierenEvent, EinladungGesendetEvent, LieferplanungAbrechnenEvent, LieferplanungAbschliessenEvent, LieferplanungDataModifiedEvent, LoginAktiviertEvent, LoginDeaktiviertEvent, OtpResetEvent, PasswortGewechseltEvent, PasswortResetGesendetEvent, RolleGewechseltEvent, SammelbestellungAlsAbgerechnetMarkierenEvent, SammelbestellungVersendenEvent, SendEmailToAboSubscriberEvent, SendEmailToAbotypSubscriberEvent, SendEmailToDepotSubscriberEvent, SendEmailToKundeEvent, SendEmailToPersonEvent, SendEmailToTourSubscriberEvent, SendEmailToZusatzabotypSubscriberEvent }
import com.typesafe.scalalogging.LazyLogging
import org.joda.time._
import spray.json._

import java.util.Locale
import scala.collection.immutable.TreeMap

/**
 * JSON Format deklarationen für das Modul Stammdaten
 */
trait StammdatenJsonProtocol extends BaseJsonProtocol with ReportJsonProtocol with LazyLogging {

  //enum formats
  implicit val wochentagFormat: RootJsonFormat[Wochentag] = enumFormat(x => Wochentag.apply(x).getOrElse(Montag))
  implicit val monatFormat: RootJsonFormat[Monat] = enumFormat(x => Monat.apply(x).getOrElse(Januar))
  implicit val rhythmusFormat: RootJsonFormat[Rhythmus] = enumFormat(Rhythmus.apply)
  implicit val preiseinheitFormat: JsonFormat[Preiseinheit] = new JsonFormat[Preiseinheit] {
    def write(obj: Preiseinheit): JsValue =
      obj match {
        case ProLieferung => JsString("Lieferung")
        case ProMonat     => JsString("Monat")
        case ProQuartal   => JsString("Quartal")
        case ProJahr      => JsString("Jahr")
        case ProAbo       => JsString("Abo")
      }

    def read(json: JsValue): Preiseinheit =
      json match {
        case JsString("Lieferung") => ProLieferung
        case JsString("Quartal")   => ProQuartal
        case JsString("Monat")     => ProMonat
        case JsString("Jahr")      => ProJahr
        case JsString("Abo")       => ProAbo
        case pe                    => sys.error(s"Unknown Preiseinheit:$pe")
      }
  }

  implicit val fristeinheitFormat: JsonFormat[Fristeinheit] = new JsonFormat[Fristeinheit] {
    def write(obj: Fristeinheit): JsValue =
      obj match {
        case Wochenfrist => JsString("Wochen")
        case Monatsfrist => JsString("Monate")
      }

    def read(json: JsValue): Fristeinheit =
      json match {
        case JsString("Wochen") => Wochenfrist
        case JsString("Monate") => Monatsfrist
        case pe                 => sys.error(s"Unknown Fristeinheit:$pe")
      }
  }

  implicit val rolleFormat: RootJsonFormat[Rolle] = new RootJsonFormat[Rolle] {
    def write(obj: Rolle): JsValue =
      obj match {
        case AdministratorZugang => JsString("Administrator")
        case KundenZugang        => JsString("Kunde")
      }

    def read(json: JsValue): Rolle =
      json match {
        case JsString("Administrator") => AdministratorZugang
        case JsString("Kunde")         => KundenZugang
        case pe                        => sys.error(s"Unknown Rolle:$pe")
      }
  }

  implicit val anredeFormat: JsonFormat[Anrede] = new JsonFormat[Anrede] {
    def write(obj: Anrede): JsValue =
      obj match {
        case Herr => JsString("Herr")
        case Frau => JsString("Frau")
      }

    def read(json: JsValue): Anrede =
      json match {
        case JsString("Herr") => Herr
        case JsString("Frau") => Frau
        case pe               => sys.error(s"Unknown Anrede:$pe")
      }
  }

  implicit val paymentTypeFormat: JsonFormat[PaymentType] = new JsonFormat[PaymentType] {
    def write(obj: PaymentType): JsValue =
      obj match {
        case Anderer     => JsString("Anderer")
        case DirectDebit => JsString("DirectDebit")
        case Transfer    => JsString("Transfer")
      }

    def read(json: JsValue): PaymentType =
      json match {
        case JsString("Anderer")     => Anderer
        case JsString("DirectDebit") => DirectDebit
        case JsString("Transfer")    => Transfer
        case pe                      => sys.error(s"Unknown payment type:$pe")
      }
  }

  implicit val secondFactorType: JsonFormat[SecondFactorType] = new JsonFormat[SecondFactorType] {
    def write(obj: SecondFactorType): JsValue =
      obj match {
        case OtpSecondFactorType   => JsString("otp")
        case EmailSecondFactorType => JsString("email")
      }

    def read(json: JsValue): SecondFactorType =
      json match {
        case JsString("otp")   => OtpSecondFactorType
        case JsString("email") => EmailSecondFactorType
        case pe                => sys.error(s"Unknown secondfactor type:$pe")
      }
  }

  implicit val waehrungFormat: RootJsonFormat[Waehrung] = enumFormat(Waehrung.apply)
  implicit val einsatzEinheitFormat: RootJsonFormat[EinsatzEinheit] = enumFormat(EinsatzEinheit.apply)
  implicit val laufzeiteinheitFormat: RootJsonFormat[Laufzeiteinheit] = enumFormat(Laufzeiteinheit.apply)
  implicit val lieferungStatusFormat: RootJsonFormat[LieferungStatus] = enumFormat(LieferungStatus.apply)
  implicit val korbStatusFormat: RootJsonFormat[KorbStatus] = enumFormat(KorbStatus.apply)
  implicit val auslieferungStatusFormat: RootJsonFormat[AuslieferungStatus] = enumFormat(AuslieferungStatus.apply)
  implicit val pendenzStatusFormat: RootJsonFormat[PendenzStatus] = enumFormat(PendenzStatus.apply)
  implicit val liefereinheitFormat: RootJsonFormat[Liefereinheit] = enumFormat(Liefereinheit.apply)

  //id formats
  implicit val vertriebIdFormat: RootJsonFormat[VertriebId] = baseIdFormat(VertriebId)
  implicit val vertriebsartIdFormat: RootJsonFormat[VertriebsartId] = baseIdFormat(VertriebsartId)
  implicit val abotypIdFormat: RootJsonFormat[AbotypId] = baseIdFormat(AbotypId.apply _)
  implicit val depotIdFormat: RootJsonFormat[DepotId] = baseIdFormat(DepotId)
  implicit val tourIdFormat: RootJsonFormat[TourId] = baseIdFormat(TourId)
  implicit val auslieferungIdFormat: RootJsonFormat[AuslieferungId] = baseIdFormat(AuslieferungId)
  implicit val optionAuslieferungIdFormat: OptionFormat[AuslieferungId] = new OptionFormat[AuslieferungId]
  implicit val kundeIdFormat: RootJsonFormat[KundeId] = baseIdFormat(KundeId)
  implicit val pendenzIdFormat: RootJsonFormat[PendenzId] = baseIdFormat(PendenzId)
  implicit val aboIdFormat: RootJsonFormat[AboId] = baseIdFormat(AboId.apply _)
  implicit val lieferungIdFormat: RootJsonFormat[LieferungId] = baseIdFormat(LieferungId)
  implicit val lieferungOnLieferplanungIdFormat: RootJsonFormat[LieferungOnLieferplanungId] = baseIdFormat(LieferungOnLieferplanungId)
  implicit val lieferplanungIdFormat: RootJsonFormat[LieferplanungId] = baseIdFormat(LieferplanungId)
  implicit val lieferpositionIdFormat: RootJsonFormat[LieferpositionId] = baseIdFormat(LieferpositionId)
  implicit val bestellungIdFormat: RootJsonFormat[BestellungId] = baseIdFormat(BestellungId)
  implicit val sammelbestellungIdFormat: RootJsonFormat[SammelbestellungId] = baseIdFormat(SammelbestellungId)
  implicit val bestellpositionIdFormat: RootJsonFormat[BestellpositionId] = baseIdFormat(BestellpositionId)
  implicit val customKundentypIdFormat: RootJsonFormat[CustomKundentypId] = baseIdFormat(CustomKundentypId.apply)
  implicit val personCategoryIdFormat: RootJsonFormat[PersonCategoryId] = baseIdFormat(PersonCategoryId.apply)
  implicit val personCategoryNameIdFormat: RootJsonFormat[PersonCategoryNameId] = new RootJsonFormat[PersonCategoryNameId] {
    def write(obj: PersonCategoryNameId): JsValue =
      JsString(obj.id)

    def read(json: JsValue): PersonCategoryNameId =
      json match {
        case JsString(id) => PersonCategoryNameId(id)
        case kt           => sys.error(s"Unknown PersonCategoryNameId:$kt")
      }
  }
  implicit val abwesenheitIdFormat: RootJsonFormat[AbwesenheitId] = baseIdFormat(AbwesenheitId.apply)
  implicit val projektVorlageIdFormat: RootJsonFormat[ProjektVorlageId] = baseIdFormat(ProjektVorlageId.apply)
  implicit val kundentypIdFormat: RootJsonFormat[KundentypId] = new RootJsonFormat[KundentypId] {
    def write(obj: KundentypId): JsValue =
      JsString(obj.id)

    def read(json: JsValue): KundentypId =
      json match {
        case JsString(id) => KundentypId(id)
        case kt           => sys.error(s"Unknown KundentypId:$kt")
      }
  }
  implicit val produktIdFormat: RootJsonFormat[ProduktId] = baseIdFormat(ProduktId.apply)
  implicit val optionProduktIdFormat: OptionFormat[ProduktId] = new OptionFormat[ProduktId]
  implicit val produktekategorieIdFormat: RootJsonFormat[ProduktekategorieId] = baseIdFormat(ProduktekategorieId.apply)
  implicit val baseProduktekategorieIdFormat: JsonFormat[BaseProduktekategorieId] = new JsonFormat[BaseProduktekategorieId] {
    def write(obj: BaseProduktekategorieId): JsValue =
      JsString(obj.id)

    def read(json: JsValue): BaseProduktekategorieId =
      json match {
        case JsString(id) => BaseProduktekategorieId(id)
        case kt           => sys.error(s"Unknown BaseProduktekategorieId:$kt")
      }
  }
  implicit val produzentIdFormat: RootJsonFormat[ProduzentId] = baseIdFormat(ProduzentId.apply)
  implicit val baseProduzentIdFormat: JsonFormat[BaseProduzentId] = new JsonFormat[BaseProduzentId] {
    def write(obj: BaseProduzentId): JsValue =
      JsString(obj.id)

    def read(json: JsValue): BaseProduzentId =
      json match {
        case JsString(id) => BaseProduzentId(id)
        case kt           => sys.error(s"Unknown BaseProduzentId:$kt")
      }
  }
  implicit val projektIdFormat: RootJsonFormat[ProjektId] = baseIdFormat(ProjektId.apply)
  implicit val kontoDatenIdFormat: RootJsonFormat[KontoDatenId] = baseIdFormat(KontoDatenId.apply)
  implicit val korbIdFormat: RootJsonFormat[KorbId] = baseIdFormat(KorbId.apply)
  implicit val einladungIdFormat: RootJsonFormat[EinladungId] = baseIdFormat(EinladungId.apply)

  implicit val lieferzeitpunktFormat: RootJsonFormat[Lieferzeitpunkt] = new RootJsonFormat[Lieferzeitpunkt] {
    def write(obj: Lieferzeitpunkt): JsValue =
      obj match {
        case w: Wochentag => w.toJson
        case _            => JsObject()
      }

    def read(json: JsValue): Lieferzeitpunkt =
      json.convertTo[Wochentag]
  }

  implicit val liefersaisonFormat: RootJsonFormat[Liefersaison] = new RootJsonFormat[Liefersaison] {
    def write(obj: Liefersaison): JsValue =
      obj match {
        case m: Monat => m.toJson
        case _        => JsObject()
      }

    def read(json: JsValue): Liefersaison =
      json.convertTo[Monat]
  }

  implicit val lieferpositionFormat: RootJsonFormat[Lieferposition] = jsonFormat15(Lieferposition)
  implicit val tourLieferungFormat = jsonFormat18(Tourlieferung.apply)
  implicit val tourFormat: RootJsonFormat[Tour] = jsonFormat9(Tour)
  implicit val tourCreateFormat: RootJsonFormat[TourCreate] = jsonFormat2(TourCreate)
  implicit val tourModifyFormat: RootJsonFormat[TourModify] = jsonFormat3(TourModify)
  implicit val depotSummaryFormat: RootJsonFormat[DepotSummary] = jsonFormat3(DepotSummary)
  implicit val korbModifyFormat: RootJsonFormat[KorbModify] = jsonFormat1(KorbModify)
  implicit val tourAuslieferungModifyFormat: RootJsonFormat[TourAuslieferungModify] = jsonFormat1(TourAuslieferungModify)

  implicit val depotModifyFormat: RootJsonFormat[DepotModify] = jsonFormat21(DepotModify)

  implicit val postlieferungDetailFormat: RootJsonFormat[PostlieferungDetail] = jsonFormat8(PostlieferungDetail)
  implicit val heimlieferungDetailFormat: RootJsonFormat[HeimlieferungDetail] = jsonFormat10(HeimlieferungDetail)
  implicit val depotlieferungDetailFormat: RootJsonFormat[DepotlieferungDetail] = jsonFormat10(DepotlieferungDetail)

  implicit val vertriebsartDetailFormat = new RootJsonFormat[VertriebsartDetail] {
    def write(obj: VertriebsartDetail): JsValue =
      JsObject((obj match {
        case p: PostlieferungDetail   => p.toJson
        case hl: HeimlieferungDetail  => hl.toJson
        case dl: DepotlieferungDetail => dl.toJson
      }).asJsObject.fields + ("typ" -> JsString(obj.productPrefix.replaceAll("Detail", ""))))

    def read(json: JsValue): VertriebsartDetail =
      json.asJsObject.getFields("typ") match {
        case Seq(JsString("Postlieferung"))  => json.convertTo[PostlieferungDetail]
        case Seq(JsString("Heimlieferung"))  => json.convertTo[HeimlieferungDetail]
        case Seq(JsString("Depotlieferung")) => json.convertTo[DepotlieferungDetail]
      }
  }

  implicit val postlieferungModifyFormat: RootJsonFormat[PostlieferungModify] = jsonFormat0(PostlieferungModify)
  implicit val depotlieferungModifyFormat: RootJsonFormat[DepotlieferungModify] = jsonFormat1(DepotlieferungModify)
  implicit val heimlieferungModifyFormat: RootJsonFormat[HeimlieferungModify] = jsonFormat1(HeimlieferungModify)

  implicit val abosComplexFlagsFormat: RootJsonFormat[AbosComplexFlags] = jsonFormat1(AbosComplexFlags)
  implicit val optionAbosComplexFlagsFormat = new OptionFormat[AbosComplexFlags]

  implicit val vertriebsartModifyFormat = new RootJsonFormat[VertriebsartModify] {
    def write(obj: VertriebsartModify): JsValue =
      JsObject((obj match {
        case p: PostlieferungModify   => p.toJson
        case hl: HeimlieferungModify  => hl.toJson
        case dl: DepotlieferungModify => dl.toJson
      }).asJsObject.fields + ("typ" -> JsString(obj.productPrefix.replaceAll("Detail", ""))))

    def read(json: JsValue): VertriebsartModify = {
      json.asJsObject.getFields("typ") match {
        case Seq(JsString("Postlieferung"))  => json.convertTo[PostlieferungModify]
        case Seq(JsString("Heimlieferung"))  => json.convertTo[HeimlieferungModify]
        case Seq(JsString("Depotlieferung")) => json.convertTo[DepotlieferungModify]
      }
    }
  }

  implicit val postlieferungAbotypModifyFormat: RootJsonFormat[PostlieferungAbotypModify] = jsonFormat1(PostlieferungAbotypModify)
  implicit val depotlieferungAbotypModifyFormat: RootJsonFormat[DepotlieferungAbotypModify] = jsonFormat2(DepotlieferungAbotypModify)
  implicit val heimlieferungAbotypModifyFormat: RootJsonFormat[HeimlieferungAbotypModify] = jsonFormat2(HeimlieferungAbotypModify)

  implicit val vertriebsartAbotypModifyFormat = new RootJsonFormat[VertriebsartAbotypModify] {
    def write(obj: VertriebsartAbotypModify): JsValue =
      JsString(obj.productPrefix)

    def read(json: JsValue): VertriebsartAbotypModify = {
      if (!json.asJsObject.getFields("depotId").isEmpty) {
        json.convertTo[DepotlieferungAbotypModify]
      } else if (!json.asJsObject.getFields("tourId").isEmpty) {
        json.convertTo[HeimlieferungAbotypModify]
      } else {
        json.convertTo[PostlieferungAbotypModify]
      }
    }
  }

  implicit val postAuslieferungFormat: RootJsonFormat[PostAuslieferung] = jsonFormat8(PostAuslieferung)
  implicit val tourAuslieferungFormat: RootJsonFormat[TourAuslieferung] = jsonFormat10(TourAuslieferung)
  implicit val depotAuslieferungFormat: RootJsonFormat[DepotAuslieferung] = jsonFormat10(DepotAuslieferung)

  implicit val auslieferungFormat = new RootJsonFormat[Auslieferung] {
    def write(obj: Auslieferung): JsValue =
      JsObject((obj match {
        case p: PostAuslieferung  => p.toJson
        case t: TourAuslieferung  => t.toJson
        case d: DepotAuslieferung => d.toJson
      }).asJsObject.fields + ("typ" -> JsString(obj.productPrefix)))

    def read(json: JsValue): Auslieferung = {
      json.asJsObject.getFields("typ") match {
        case Seq(JsString("PostAuslieferung"))  => json.convertTo[PostAuslieferung]
        case Seq(JsString("TourAuslieferung"))  => json.convertTo[TourAuslieferung]
        case Seq(JsString("DepotAuslieferung")) => json.convertTo[DepotAuslieferung]
      }
    }
  }

  // json formatter which adds calculated boolean field
  def enhanceWithBooleanFlagAddManuallyBecauseOfMissingLib[E <: AktivRange](flag: String)(implicit defaultFormat: JsonFormat[E]): RootJsonFormat[E] = new RootJsonFormat[E] {
    def write(obj: E): JsValue = {
      JsObject(defaultFormat.write(obj)
        .asJsObject.fields +
        (flag -> JsBoolean(
          obj.aktiv
        )))
    }

    def read(json: JsValue): E = defaultFormat.read(json)
  }

  implicit val fristFormat: RootJsonFormat[Frist] = jsonFormat2(Frist)

  implicit val abotypModifyFormat: RootJsonFormat[AbotypModify] = jsonFormat18(AbotypModify)

  implicit val baseAbotypFormat = new RootJsonFormat[Abotyp] {
    override def read(json: JsValue): Abotyp = {
      val fields = json.asJsObject.fields
      Abotyp(
        fields("id").convertTo[AbotypId],
        fields("name").convertTo[String],
        fields("beschreibung").convertTo[Option[String]],
        fields("lieferrhythmus").convertTo[Rhythmus],
        fields("aktivVon").convertTo[Option[LocalDate]],
        fields("aktivBis").convertTo[Option[LocalDate]],
        fields("preis").convertTo[BigDecimal],
        fields("preiseinheit").convertTo[Preiseinheit],
        fields("laufzeit").convertTo[Option[Int]],
        fields("laufzeiteinheit").convertTo[Laufzeiteinheit],
        fields("vertragslaufzeit").convertTo[Option[Frist]],
        fields("kuendigungsfrist").convertTo[Option[Frist]],
        fields("anzahlAbwesenheiten").convertTo[Option[Int]],
        fields("anzahlEinsaetze").convertTo[Option[BigDecimal]],
        fields("farbCode").convertTo[String],
        fields("zielpreis").convertTo[Option[BigDecimal]],
        fields("guthabenMindestbestand").convertTo[Int],
        fields("adminProzente").convertTo[BigDecimal],
        fields("wirdGeplant").convertTo[Boolean],
        fields("anzahlAbonnenten").convertTo[Int],
        fields("anzahlAbonnentenAktiv").convertTo[Int],
        fields("letzteLieferung").convertTo[Option[DateTime]],
        fields("waehrung").convertTo[Waehrung],
        fields("erstelldat").convertTo[DateTime],
        fields("ersteller").convertTo[PersonId],
        fields("modifidat").convertTo[DateTime],
        fields("modifikator").convertTo[PersonId]
      )
    }

    override def write(obj: Abotyp): JsValue = JsObject(
      "id" -> obj.id.toJson,
      "name" -> obj.name.toJson,
      "beschreibung" -> obj.beschreibung.toJson,
      "lieferrhythmus" -> obj.lieferrhythmus.toJson,
      "aktivVon" -> obj.aktivVon.toJson,
      "aktivBis" -> obj.aktivBis.toJson,
      "preis" -> obj.preis.toJson,
      "preiseinheit" -> obj.preiseinheit.toJson,
      "laufzeit" -> obj.laufzeit.toJson,
      "laufzeiteinheit" -> obj.laufzeiteinheit.toJson,
      "vertragslaufzeit" -> obj.vertragslaufzeit.toJson,
      "kuendigungsfrist" -> obj.kuendigungsfrist.toJson,
      "anzahlAbwesenheiten" -> obj.anzahlAbwesenheiten.toJson,
      "anzahlEinsaetze" -> obj.anzahlEinsaetze.toJson,
      "farbCode" -> obj.farbCode.toJson,
      "zielpreis" -> obj.zielpreis.toJson,
      "guthabenMindestbestand" -> obj.guthabenMindestbestand.toJson,
      "adminProzente" -> obj.adminProzente.toJson,
      "wirdGeplant" -> obj.wirdGeplant.toJson,
      "anzahlAbonnenten" -> obj.anzahlAbonnenten.toJson,
      "anzahlAbonnentenAktiv" -> obj.anzahlAbonnentenAktiv.toJson,
      "letzteLieferung" -> obj.letzteLieferung.toJson,
      "waehrung" -> obj.waehrung.toJson,
      "erstelldat" -> obj.erstelldat.toJson,
      "ersteller" -> obj.ersteller.toJson,
      "modifidat" -> obj.modifidat.toJson,
      "modifikator" -> obj.modifikator.toJson,
      // additional
      "aktiv" -> JsBoolean(
        obj.aktiv
      )
    )
  }

  implicit val zusatzAbotypFormat = new RootJsonFormat[ZusatzAbotyp] {
    override def read(json: JsValue): ZusatzAbotyp = {
      val fields = json.asJsObject.fields
      ZusatzAbotyp(
        fields("id").convertTo[AbotypId],
        fields("name").convertTo[String],
        fields("beschreibung").convertTo[Option[String]],
        fields("aktivVon").convertTo[Option[LocalDate]],
        fields("aktivBis").convertTo[Option[LocalDate]],
        fields("preis").convertTo[BigDecimal],
        fields("preiseinheit").convertTo[Preiseinheit],
        fields("laufzeit").convertTo[Option[Int]],
        fields("laufzeiteinheit").convertTo[Laufzeiteinheit],
        fields("vertragslaufzeit").convertTo[Option[Frist]],
        fields("kuendigungsfrist").convertTo[Option[Frist]],
        fields("anzahlAbwesenheiten").convertTo[Option[Int]],
        fields("anzahlEinsaetze").convertTo[Option[BigDecimal]],
        fields("farbCode").convertTo[String],
        fields("zielpreis").convertTo[Option[BigDecimal]],
        fields("adminProzente").convertTo[BigDecimal],
        fields("wirdGeplant").convertTo[Boolean],
        fields("anzahlAbonnenten").convertTo[Int],
        fields("anzahlAbonnentenAktiv").convertTo[Int],
        fields("letzteLieferung").convertTo[Option[DateTime]],
        fields("waehrung").convertTo[Waehrung],
        fields("erstelldat").convertTo[DateTime],
        fields("ersteller").convertTo[PersonId],
        fields("modifidat").convertTo[DateTime],
        fields("modifikator").convertTo[PersonId]
      )
    }

    override def write(obj: ZusatzAbotyp): JsValue = JsObject(
      "id" -> obj.id.toJson,
      "name" -> obj.name.toJson,
      "beschreibung" -> obj.beschreibung.toJson,
      "aktivVon" -> obj.aktivVon.toJson,
      "aktivBis" -> obj.aktivBis.toJson,
      "preis" -> obj.preis.toJson,
      "preiseinheit" -> obj.preiseinheit.toJson,
      "laufzeit" -> obj.laufzeit.toJson,
      "laufzeiteinheit" -> obj.laufzeiteinheit.toJson,
      "vertragslaufzeit" -> obj.vertragslaufzeit.toJson,
      "kuendigungsfrist" -> obj.kuendigungsfrist.toJson,
      "anzahlAbwesenheiten" -> obj.anzahlAbwesenheiten.toJson,
      "anzahlEinsaetze" -> obj.anzahlEinsaetze.toJson,
      "farbCode" -> obj.farbCode.toJson,
      "zielpreis" -> obj.zielpreis.toJson,
      "adminProzente" -> obj.adminProzente.toJson,
      "wirdGeplant" -> obj.wirdGeplant.toJson,
      "anzahlAbonnenten" -> obj.anzahlAbonnenten.toJson,
      "anzahlAbonnentenAktiv" -> obj.anzahlAbonnentenAktiv.toJson,
      "letzteLieferung" -> obj.letzteLieferung.toJson,
      "waehrung" -> obj.waehrung.toJson,
      "erstelldat" -> obj.erstelldat.toJson,
      "ersteller" -> obj.ersteller.toJson,
      "modifidat" -> obj.modifidat.toJson,
      "modifikator" -> obj.modifikator.toJson,
      // additional
      "aktiv" -> JsBoolean(
        obj.aktiv
      )
    )
  }

  implicit val iAboTypWriter = new RootJsonWriter[IAbotyp] {
    override def write(obj: IAbotyp): JsValue = obj match {
      case abotyp: Abotyp             => abotyp.toJson
      case zusatzAbotyp: ZusatzAbotyp => zusatzAbotyp.toJson
    }
  }

  implicit val zusatzAbotypModifyFormat: RootJsonFormat[ZusatzAbotypModify] = jsonFormat17(ZusatzAbotypModify)

  implicit val zusatzaboCreateFormat: RootJsonFormat[ZusatzAboCreate] = jsonFormat3(ZusatzAboCreate)
  implicit val zusatzaboModifyFormat: RootJsonFormat[ZusatzAboModify] = jsonFormat7(ZusatzAboModify)
  implicit val zusatzAboReportFormat: RootJsonFormat[ZusatzAboReport] = jsonFormat19(ZusatzAboReport)

  implicit val treeMapIntFormat = new JsonFormat[TreeMap[String, Int]] {
    def write(obj: TreeMap[String, Int]): JsValue = {
      val elems = obj.toTraversable.map {
        case (key, value) => JsObject("key" -> JsString(key), "value" -> JsNumber(value))
      }.toVector
      JsArray(elems)
    }

    def read(json: JsValue): TreeMap[String, Int] =
      json match {
        case JsArray(elems) =>
          val entries = elems.map { elem =>
            elem.asJsObject.getFields("key", "value") match {
              case Seq(JsString(key), JsNumber(value)) =>
                (key -> value.toInt)
            }
          }.toSeq
          (TreeMap.empty[String, Int] /: entries) { (tree, c) => tree + c }
        case pt => sys.error(s"Unknown treemap:$pt")
      }
  }

  implicit val treeMapBigDecimalFormat = new JsonFormat[TreeMap[String, BigDecimal]] {
    def write(obj: TreeMap[String, BigDecimal]): JsValue = {
      val elems = obj.toTraversable.map {
        case (key, value) => JsObject("key" -> JsString(key), "value" -> JsNumber(value))
      }.toVector
      JsArray(elems)
    }

    def read(json: JsValue): TreeMap[String, BigDecimal] =
      json match {
        case JsArray(elems) =>
          val entries = elems.map { elem =>
            elem.asJsObject.getFields("key", "value") match {
              case Seq(JsString(key), JsNumber(value)) =>
                (key -> value.asInstanceOf[BigDecimal])
              case Seq(JsString(key), JsString(value)) =>
                (key -> BigDecimal(value))
            }
          }.toSeq
          (TreeMap.empty[String, BigDecimal] /: entries) { (tree, c) => tree + c }
        case pt => sys.error(s"Unknown treemap:$pt")
      }
  }

  implicit val vertriebFormat: RootJsonFormat[Vertrieb] = jsonFormat12(Vertrieb)
  implicit val vertriebModifyFormat: RootJsonFormat[VertriebModify] = jsonFormat3(VertriebModify)

  implicit val depotaboFormat = new RootJsonFormat[DepotlieferungAbo] {
    override def read(json: JsValue): DepotlieferungAbo = {
      val fields = json.asJsObject.fields
      DepotlieferungAbo(
        fields("id").convertTo[AboId],
        fields("kundeId").convertTo[KundeId],
        fields("kunde").convertTo[String],
        fields("vertriebsartId").convertTo[VertriebsartId],
        fields("vertriebId").convertTo[VertriebId],
        fields("vertriebBeschrieb").convertTo[Option[String]],
        fields("abotypId").convertTo[AbotypId],
        fields("abotypName").convertTo[String],
        fields("depotId").convertTo[DepotId],
        fields("depotName").convertTo[String],
        fields("start").convertTo[LocalDate],
        fields("ende").convertTo[Option[LocalDate]],
        fields("price").convertTo[Option[BigDecimal]],
        fields("guthabenVertraglich").convertTo[Option[Int]],
        fields("guthaben").convertTo[Int],
        fields("guthabenInRechnung").convertTo[Int],
        fields("letzteLieferung").convertTo[Option[DateTime]],
        fields("anzahlAbwesenheiten").convertTo[TreeMap[String, Int]],
        fields("anzahlLieferungen").convertTo[TreeMap[String, Int]],
        fields("aktiv").convertTo[Boolean],
        fields("zusatzAboIds").convertTo[Set[AboId]],
        fields("zusatzAbotypNames").convertTo[Seq[String]],
        fields("anzahlEinsaetze").convertTo[TreeMap[String, BigDecimal]],
        fields("erstelldat").convertTo[DateTime],
        fields("ersteller").convertTo[PersonId],
        fields("modifidat").convertTo[DateTime],
        fields("modifikator").convertTo[PersonId]
      )
    }

    override def write(obj: DepotlieferungAbo): JsValue = JsObject(
      "id" -> obj.id.toJson,
      "kundeId" -> obj.kundeId.toJson,
      "kunde" -> obj.kunde.toJson,
      "vertriebsartId" -> obj.vertriebsartId.toJson,
      "vertriebId" -> obj.vertriebId.toJson,
      "vertriebBeschrieb" -> obj.vertriebBeschrieb.toJson,
      "abotypId" -> obj.abotypId.toJson,
      "abotypName" -> obj.abotypName.toJson,
      "depotId" -> obj.depotId.toJson,
      "depotName" -> obj.depotName.toJson,
      "start" -> obj.start.toJson,
      "ende" -> obj.ende.toJson,
      "price" -> obj.price.toJson,
      "guthabenVertraglich" -> obj.guthabenVertraglich.toJson,
      "guthaben" -> obj.guthaben.toJson,
      "guthabenInRechnung" -> obj.guthabenInRechnung.toJson,
      "letzteLieferung" -> obj.letzteLieferung.toJson,
      "anzahlAbwesenheiten" -> obj.anzahlAbwesenheiten.toJson,
      "anzahlLieferungen" -> obj.anzahlLieferungen.toJson,
      "aktiv" -> obj.aktiv.toJson,
      "zusatzAboIds" -> obj.zusatzAboIds.toJson,
      "zusatzAbotypNames" -> obj.zusatzAbotypNames.toJson,
      "anzahlEinsaetze" -> obj.anzahlEinsaetze.toJson,
      "erstelldat" -> obj.erstelldat.toJson,
      "ersteller" -> obj.ersteller.toJson,
      "modifidat" -> obj.modifidat.toJson,
      "modifikator" -> obj.modifikator.toJson
    )
  }

  implicit val abwesenheitFormat: RootJsonFormat[Abwesenheit] = jsonFormat9(Abwesenheit)
  implicit val abwesenheitModifyFormat: RootJsonFormat[AbwesenheitModify] = jsonFormat3(AbwesenheitModify)
  implicit val lieferungFormat: RootJsonFormat[Lieferung] = jsonFormat19(Lieferung)
  implicit val lieferplanungFormat: RootJsonFormat[Lieferplanung] = jsonFormat8(Lieferplanung)
  implicit val lieferpositionOpenFormat: RootJsonFormat[LieferpositionOpen] = jsonFormat13(LieferpositionOpen)
  implicit val lieferungOpenDetailFormat: RootJsonFormat[LieferungOpenDetail] = jsonFormat13(LieferungOpenDetail)
  implicit val lieferplanungOpenDetailFormat: RootJsonFormat[LieferplanungOpenDetail] = jsonFormat9(LieferplanungOpenDetail)

  implicit val depotaboDetailFormat = new RootJsonFormat[DepotlieferungAboDetail] {
    override def read(json: JsValue): DepotlieferungAboDetail = {
      val fields = json.asJsObject.fields
      DepotlieferungAboDetail(
        fields("id").convertTo[AboId],
        fields("kundeId").convertTo[KundeId],
        fields("kunde").convertTo[String],
        fields("vertriebsartId").convertTo[VertriebsartId],
        fields("vertriebId").convertTo[VertriebId],
        fields("vertriebBeschrieb").convertTo[Option[String]],
        fields("abotypId").convertTo[AbotypId],
        fields("abotypName").convertTo[String],
        fields("depotId").convertTo[DepotId],
        fields("depotName").convertTo[String],
        fields("start").convertTo[LocalDate],
        fields("ende").convertTo[Option[LocalDate]],
        fields("price").convertTo[Option[BigDecimal]],
        fields("guthabenVertraglich").convertTo[Option[Int]],
        fields("guthaben").convertTo[Int],
        fields("guthabenInRechnung").convertTo[Int],
        fields("letzteLieferung").convertTo[Option[DateTime]],
        fields("anzahlAbwesenheiten").convertTo[TreeMap[String, Int]],
        fields("anzahlLieferungen").convertTo[TreeMap[String, Int]],
        fields("aktiv").convertTo[Boolean],
        fields("anzahlEinsaetze").convertTo[TreeMap[String, BigDecimal]],
        fields("zusatzAboIds").convertTo[Set[AboId]],
        fields("zusatzAbotypNames").convertTo[Seq[String]],
        fields("erstelldat").convertTo[DateTime],
        fields("ersteller").convertTo[PersonId],
        fields("modifidat").convertTo[DateTime],
        fields("modifikator").convertTo[PersonId],
        fields("abwesenheiten").convertTo[Seq[Abwesenheit]],
        fields("lieferdaten").convertTo[Seq[Lieferung]],
        fields("abotyp").convertTo[Option[Abotyp]],
        fields("vertrieb").convertTo[Option[Vertrieb]]
      )
    }

    override def write(obj: DepotlieferungAboDetail): JsValue = JsObject(
      "id" -> obj.id.toJson,
      "kundeId" -> obj.kundeId.toJson,
      "kunde" -> obj.kunde.toJson,
      "vertriebsartId" -> obj.vertriebsartId.toJson,
      "vertriebId" -> obj.vertriebId.toJson,
      "vertriebBeschrieb" -> obj.vertriebBeschrieb.toJson,
      "abotypId" -> obj.abotypId.toJson,
      "abotypName" -> obj.abotypName.toJson,
      "depotId" -> obj.depotId.toJson,
      "depotName" -> obj.depotName.toJson,
      "start" -> obj.start.toJson,
      "ende" -> obj.ende.toJson,
      "price" -> obj.price.toJson,
      "guthabenVertraglich" -> obj.guthabenVertraglich.toJson,
      "guthaben" -> obj.guthaben.toJson,
      "guthabenInRechnung" -> obj.guthabenInRechnung.toJson,
      "letzteLieferung" -> obj.letzteLieferung.toJson,
      "anzahlAbwesenheiten" -> obj.anzahlAbwesenheiten.toJson,
      "anzahlLieferungen" -> obj.anzahlLieferungen.toJson,
      "aktiv" -> obj.aktiv.toJson,
      "anzahlEinsaetze" -> obj.anzahlEinsaetze.toJson,
      "zusatzAboIds" -> obj.zusatzAboIds.toJson,
      "zusatzAbotypNames" -> obj.zusatzAbotypNames.toJson,
      "erstelldat" -> obj.erstelldat.toJson,
      "ersteller" -> obj.ersteller.toJson,
      "modifidat" -> obj.modifidat.toJson,
      "modifikator" -> obj.modifikator.toJson,
      "abwesenheiten" -> obj.abwesenheiten.toJson,
      "lieferdaten" -> obj.lieferdaten.toJson,
      "abotyp" -> obj.abotyp.toJson,
      "vertrieb" -> obj.vertrieb.toJson
    )
  }

  implicit val depotaboModifyFormat: RootJsonFormat[DepotlieferungAboModify] = jsonFormat4(DepotlieferungAboModify)

  implicit val heimlieferungAboFormat = new RootJsonFormat[HeimlieferungAbo] {
    override def read(json: JsValue): HeimlieferungAbo = {
      val fields = json.asJsObject.fields
      HeimlieferungAbo(
        fields("id").convertTo[AboId],
        fields("kundeId").convertTo[KundeId],
        fields("kunde").convertTo[String],
        fields("vertriebsartId").convertTo[VertriebsartId],
        fields("vertriebId").convertTo[VertriebId],
        fields("vertriebBeschrieb").convertTo[Option[String]],
        fields("abotypId").convertTo[AbotypId],
        fields("abotypName").convertTo[String],
        fields("tourId").convertTo[TourId],
        fields("tourName").convertTo[String],
        fields("start").convertTo[LocalDate],
        fields("ende").convertTo[Option[LocalDate]],
        fields("price").convertTo[Option[BigDecimal]],
        fields("guthabenVertraglich").convertTo[Option[Int]],
        fields("guthaben").convertTo[Int],
        fields("guthabenInRechnung").convertTo[Int],
        fields("letzteLieferung").convertTo[Option[DateTime]],
        fields("anzahlAbwesenheiten").convertTo[TreeMap[String, Int]],
        fields("anzahlLieferungen").convertTo[TreeMap[String, Int]],
        fields("aktiv").convertTo[Boolean],
        fields("zusatzAboIds").convertTo[Set[AboId]],
        fields("zusatzAbotypNames").convertTo[Seq[String]],
        fields("anzahlEinsaetze").convertTo[TreeMap[String, BigDecimal]],
        fields("erstelldat").convertTo[DateTime],
        fields("ersteller").convertTo[PersonId],
        fields("modifidat").convertTo[DateTime],
        fields("modifikator").convertTo[PersonId]
      )
    }

    override def write(obj: HeimlieferungAbo): JsValue = JsObject(
      "id" -> obj.id.toJson,
      "kundeId" -> obj.kundeId.toJson,
      "kunde" -> obj.kunde.toJson,
      "vertriebsartId" -> obj.vertriebsartId.toJson,
      "vertriebId" -> obj.vertriebId.toJson,
      "vertriebBeschrieb" -> obj.vertriebBeschrieb.toJson,
      "abotypId" -> obj.abotypId.toJson,
      "abotypName" -> obj.abotypName.toJson,
      "tourId" -> obj.tourId.toJson,
      "tourName" -> obj.tourName.toJson,
      "start" -> obj.start.toJson,
      "ende" -> obj.ende.toJson,
      "price" -> obj.price.toJson,
      "guthabenVertraglich" -> obj.guthabenVertraglich.toJson,
      "guthaben" -> obj.guthaben.toJson,
      "guthabenInRechnung" -> obj.guthabenInRechnung.toJson,
      "letzteLieferung" -> obj.letzteLieferung.toJson,
      "anzahlAbwesenheiten" -> obj.anzahlAbwesenheiten.toJson,
      "anzahlLieferungen" -> obj.anzahlLieferungen.toJson,
      "aktiv" -> obj.aktiv.toJson,
      "zusatzAboIds" -> obj.zusatzAboIds.toJson,
      "zusatzAbotypNames" -> obj.zusatzAbotypNames.toJson,
      "anzahlEinsaetze" -> obj.anzahlEinsaetze.toJson,
      "erstelldat" -> obj.erstelldat.toJson,
      "ersteller" -> obj.ersteller.toJson,
      "modifidat" -> obj.modifidat.toJson,
      "modifikator" -> obj.modifikator.toJson
    )
  }

  implicit val heimlieferungAboDetailFormat = new RootJsonFormat[HeimlieferungAboDetail] {
    override def read(json: JsValue): HeimlieferungAboDetail = {
      val fields = json.asJsObject.fields
      HeimlieferungAboDetail(
        fields("id").convertTo[AboId],
        fields("kundeId").convertTo[KundeId],
        fields("kunde").convertTo[String],
        fields("vertriebsartId").convertTo[VertriebsartId],
        fields("vertriebId").convertTo[VertriebId],
        fields("vertriebBeschrieb").convertTo[Option[String]],
        fields("abotypId").convertTo[AbotypId],
        fields("abotypName").convertTo[String],
        fields("tourId").convertTo[TourId],
        fields("tourName").convertTo[String],
        fields("start").convertTo[LocalDate],
        fields("ende").convertTo[Option[LocalDate]],
        fields("price").convertTo[Option[BigDecimal]],
        fields("guthabenVertraglich").convertTo[Option[Int]],
        fields("guthaben").convertTo[Int],
        fields("guthabenInRechnung").convertTo[Int],
        fields("letzteLieferung").convertTo[Option[DateTime]],
        fields("anzahlAbwesenheiten").convertTo[TreeMap[String, Int]],
        fields("anzahlLieferungen").convertTo[TreeMap[String, Int]],
        fields("aktiv").convertTo[Boolean],
        fields("zusatzAboIds").convertTo[Set[AboId]],
        fields("zusatzAbotypNames").convertTo[Seq[String]],
        fields("anzahlEinsaetze").convertTo[TreeMap[String, BigDecimal]],
        fields("erstelldat").convertTo[DateTime],
        fields("ersteller").convertTo[PersonId],
        fields("modifidat").convertTo[DateTime],
        fields("modifikator").convertTo[PersonId],
        fields("abwesenheiten").convertTo[Seq[Abwesenheit]],
        fields("lieferdaten").convertTo[Seq[Lieferung]],
        fields("abotyp").convertTo[Option[Abotyp]],
        fields("vertrieb").convertTo[Option[Vertrieb]]
      )
    }

    override def write(obj: HeimlieferungAboDetail): JsValue = JsObject(
      "id" -> obj.id.toJson,
      "kundeId" -> obj.kundeId.toJson,
      "kunde" -> obj.kunde.toJson,
      "vertriebsartId" -> obj.vertriebsartId.toJson,
      "vertriebId" -> obj.vertriebId.toJson,
      "vertriebBeschrieb" -> obj.vertriebBeschrieb.toJson,
      "abotypId" -> obj.abotypId.toJson,
      "abotypName" -> obj.abotypName.toJson,
      "tourId" -> obj.tourId.toJson,
      "tourName" -> obj.tourName.toJson,
      "start" -> obj.start.toJson,
      "ende" -> obj.ende.toJson,
      "price" -> obj.price.toJson,
      "guthabenVertraglich" -> obj.guthabenVertraglich.toJson,
      "guthaben" -> obj.guthaben.toJson,
      "guthabenInRechnung" -> obj.guthabenInRechnung.toJson,
      "letzteLieferung" -> obj.letzteLieferung.toJson,
      "anzahlAbwesenheiten" -> obj.anzahlAbwesenheiten.toJson,
      "anzahlLieferungen" -> obj.anzahlLieferungen.toJson,
      "aktiv" -> obj.aktiv.toJson,
      "zusatzAboIds" -> obj.zusatzAboIds.toJson,
      "zusatzAbotypNames" -> obj.zusatzAbotypNames.toJson,
      "anzahlEinsaetze" -> obj.anzahlEinsaetze.toJson,
      "erstelldat" -> obj.erstelldat.toJson,
      "ersteller" -> obj.ersteller.toJson,
      "modifidat" -> obj.modifidat.toJson,
      "modifikator" -> obj.modifikator.toJson,
      "abwesenheiten" -> obj.abwesenheiten.toJson,
      "lieferdaten" -> obj.lieferdaten.toJson,
      "abotyp" -> obj.abotyp.toJson,
      "vertrieb" -> obj.vertrieb.toJson
    )
  }

  implicit val heimlieferungAboModifyFormat: RootJsonFormat[HeimlieferungAboModify] = jsonFormat4(HeimlieferungAboModify)

  implicit val postlieferungAboFormat = new RootJsonFormat[PostlieferungAbo] {
    override def read(json: JsValue): PostlieferungAbo = {
      val fields = json.asJsObject.fields
      PostlieferungAbo(
        fields("id").convertTo[AboId],
        fields("kundeId").convertTo[KundeId],
        fields("kunde").convertTo[String],
        fields("vertriebsartId").convertTo[VertriebsartId],
        fields("vertriebId").convertTo[VertriebId],
        fields("vertriebBeschrieb").convertTo[Option[String]],
        fields("abotypId").convertTo[AbotypId],
        fields("abotypName").convertTo[String],
        fields("start").convertTo[LocalDate],
        fields("ende").convertTo[Option[LocalDate]],
        fields("price").convertTo[Option[BigDecimal]],
        fields("guthabenVertraglich").convertTo[Option[Int]],
        fields("guthaben").convertTo[Int],
        fields("guthabenInRechnung").convertTo[Int],
        fields("letzteLieferung").convertTo[Option[DateTime]],
        fields("anzahlAbwesenheiten").convertTo[TreeMap[String, Int]],
        fields("anzahlLieferungen").convertTo[TreeMap[String, Int]],
        fields("aktiv").convertTo[Boolean],
        fields("zusatzAboIds").convertTo[Set[AboId]],
        fields("zusatzAbotypNames").convertTo[Seq[String]],
        fields("anzahlEinsaetze").convertTo[TreeMap[String, BigDecimal]],
        fields("erstelldat").convertTo[DateTime],
        fields("ersteller").convertTo[PersonId],
        fields("modifidat").convertTo[DateTime],
        fields("modifikator").convertTo[PersonId]
      )
    }

    override def write(obj: PostlieferungAbo): JsValue = JsObject(
      "id" -> obj.id.toJson,
      "kundeId" -> obj.kundeId.toJson,
      "kunde" -> obj.kunde.toJson,
      "vertriebsartId" -> obj.vertriebsartId.toJson,
      "vertriebId" -> obj.vertriebId.toJson,
      "vertriebBeschrieb" -> obj.vertriebBeschrieb.toJson,
      "abotypId" -> obj.abotypId.toJson,
      "abotypName" -> obj.abotypName.toJson,
      "start" -> obj.start.toJson,
      "ende" -> obj.ende.toJson,
      "price" -> obj.price.toJson,
      "guthabenVertraglich" -> obj.guthabenVertraglich.toJson,
      "guthaben" -> obj.guthaben.toJson,
      "guthabenInRechnung" -> obj.guthabenInRechnung.toJson,
      "letzteLieferung" -> obj.letzteLieferung.toJson,
      "anzahlAbwesenheiten" -> obj.anzahlAbwesenheiten.toJson,
      "anzahlLieferungen" -> obj.anzahlLieferungen.toJson,
      "aktiv" -> obj.aktiv.toJson,
      "zusatzAboIds" -> obj.zusatzAboIds.toJson,
      "zusatzAbotypNames" -> obj.zusatzAbotypNames.toJson,
      "anzahlEinsaetze" -> obj.anzahlEinsaetze.toJson,
      "erstelldat" -> obj.erstelldat.toJson,
      "ersteller" -> obj.ersteller.toJson,
      "modifidat" -> obj.modifidat.toJson,
      "modifikator" -> obj.modifikator.toJson
    )
  }

  implicit val postlieferungAboDetailFormat = new RootJsonFormat[PostlieferungAboDetail] {
    override def read(json: JsValue): PostlieferungAboDetail = {
      val fields = json.asJsObject.fields
      PostlieferungAboDetail(
        fields("id").convertTo[AboId],
        fields("kundeId").convertTo[KundeId],
        fields("kunde").convertTo[String],
        fields("vertriebsartId").convertTo[VertriebsartId],
        fields("vertriebId").convertTo[VertriebId],
        fields("vertriebBeschrieb").convertTo[Option[String]],
        fields("abotypId").convertTo[AbotypId],
        fields("abotypName").convertTo[String],
        fields("start").convertTo[LocalDate],
        fields("ende").convertTo[Option[LocalDate]],
        fields("price").convertTo[Option[BigDecimal]],
        fields("guthabenVertraglich").convertTo[Option[Int]],
        fields("guthaben").convertTo[Int],
        fields("guthabenInRechnung").convertTo[Int],
        fields("letzteLieferung").convertTo[Option[DateTime]],
        fields("anzahlAbwesenheiten").convertTo[TreeMap[String, Int]],
        fields("anzahlLieferungen").convertTo[TreeMap[String, Int]],
        fields("aktiv").convertTo[Boolean],
        fields("zusatzAboIds").convertTo[Set[AboId]],
        fields("zusatzAbotypNames").convertTo[Seq[String]],
        fields("anzahlEinsaetze").convertTo[TreeMap[String, BigDecimal]],
        fields("erstelldat").convertTo[DateTime],
        fields("ersteller").convertTo[PersonId],
        fields("modifidat").convertTo[DateTime],
        fields("modifikator").convertTo[PersonId],
        fields("abwesenheiten").convertTo[Seq[Abwesenheit]],
        fields("lieferdaten").convertTo[Seq[Lieferung]],
        fields("abotyp").convertTo[Option[Abotyp]],
        fields("vertrieb").convertTo[Option[Vertrieb]]
      )
    }

    override def write(obj: PostlieferungAboDetail): JsValue = JsObject(
      "id" -> obj.id.toJson,
      "kundeId" -> obj.kundeId.toJson,
      "kunde" -> obj.kunde.toJson,
      "vertriebsartId" -> obj.vertriebsartId.toJson,
      "vertriebId" -> obj.vertriebId.toJson,
      "vertriebBeschrieb" -> obj.vertriebBeschrieb.toJson,
      "abotypId" -> obj.abotypId.toJson,
      "abotypName" -> obj.abotypName.toJson,
      "start" -> obj.start.toJson,
      "ende" -> obj.ende.toJson,
      "price" -> obj.price.toJson,
      "guthabenVertraglich" -> obj.guthabenVertraglich.toJson,
      "guthaben" -> obj.guthaben.toJson,
      "guthabenInRechnung" -> obj.guthabenInRechnung.toJson,
      "letzteLieferung" -> obj.letzteLieferung.toJson,
      "anzahlAbwesenheiten" -> obj.anzahlAbwesenheiten.toJson,
      "anzahlLieferungen" -> obj.anzahlLieferungen.toJson,
      "aktiv" -> obj.aktiv.toJson,
      "zusatzAboIds" -> obj.zusatzAboIds.toJson,
      "zusatzAbotypNames" -> obj.zusatzAbotypNames.toJson,
      "anzahlEinsaetze" -> obj.anzahlEinsaetze.toJson,
      "erstelldat" -> obj.erstelldat.toJson,
      "ersteller" -> obj.ersteller.toJson,
      "modifidat" -> obj.modifidat.toJson,
      "modifikator" -> obj.modifikator.toJson,
      "abwesenheiten" -> obj.abwesenheiten.toJson,
      "lieferdaten" -> obj.lieferdaten.toJson,
      "abotyp" -> obj.abotyp.toJson,
      "vertrieb" -> obj.vertrieb.toJson
    )
  }

  implicit val postlieferungAboModifyFormat: RootJsonFormat[PostlieferungAboModify] = jsonFormat4(PostlieferungAboModify)

  implicit val zusatzAboFormat: RootJsonFormat[ZusatzAbo] = jsonFormat22(ZusatzAbo)
  implicit val zusatzAboDetailFormat: RootJsonFormat[ZusatzAboDetail] = jsonFormat22(ZusatzAboDetail)

  implicit val iAbotypFormat = new RootJsonFormat[IAbotyp] {
    def write(obj: IAbotyp): JsValue =
      JsObject((obj match {
        case a: Abotyp       => a.toJson
        case z: ZusatzAbotyp => z.toJson
      }).asJsObject.fields + ("typ" -> JsString(obj.productPrefix)))

    def read(json: JsValue): IAbotyp = {
      json.asJsObject.getFields("typ") match {
        case Seq(JsString("Abotyp"))       => json.convertTo[Abotyp]
        case Seq(JsString("ZusatzAbotyp")) => json.convertTo[ZusatzAbotyp]
      }
    }
  }

  implicit val aboDetailFormat = new RootJsonFormat[AboDetail] {
    def write(obj: AboDetail): JsValue =
      obj match {
        case d: DepotlieferungAboDetail => d.toJson
        case h: HeimlieferungAboDetail  => h.toJson
        case p: PostlieferungAboDetail  => p.toJson
        case _                          => JsObject()
      }

    def read(json: JsValue): AboDetail = {
      if (!json.asJsObject.getFields("depotId").isEmpty) {
        json.convertTo[DepotlieferungAboDetail]
      } else if (!json.asJsObject.getFields("tourId").isEmpty) {
        json.convertTo[HeimlieferungAboDetail]
      } else {
        json.convertTo[PostlieferungAboDetail]
      }
    }
  }

  implicit val hauptAboFormat = new RootJsonFormat[HauptAbo] {
    def write(obj: HauptAbo): JsValue =
      obj match {
        case d: DepotlieferungAbo => d.toJson
        case h: HeimlieferungAbo  => h.toJson
        case p: PostlieferungAbo  => p.toJson
        case _                    => JsObject()
      }

    def read(json: JsValue): HauptAbo = {
      if (!json.asJsObject.getFields("depotId").isEmpty) {
        json.convertTo[DepotlieferungAbo]
      } else if (!json.asJsObject.getFields("tourId").isEmpty) {
        json.convertTo[HeimlieferungAbo]
      } else {
        json.convertTo[PostlieferungAbo]
      }
    }
  }

  implicit val aboFormat = new RootJsonFormat[Abo] {
    def write(obj: Abo): JsValue =
      obj match {
        case d: DepotlieferungAbo => d.toJson
        case h: HeimlieferungAbo  => h.toJson
        case p: PostlieferungAbo  => p.toJson
        case z: ZusatzAbo         => z.toJson
        case _                    => JsObject()
      }

    def read(json: JsValue): Abo = {
      if (!json.asJsObject.getFields("depotId").isEmpty) {
        json.convertTo[DepotlieferungAbo]
      } else if (!json.asJsObject.getFields("tourId").isEmpty) {
        json.convertTo[HeimlieferungAbo]
      } else if (!json.asJsObject.getFields("hauptAboId").isEmpty) {
        json.convertTo[ZusatzAbo]
      } else {
        json.convertTo[PostlieferungAbo]
      }
    }
  }

  implicit val depotlieferungAboCreateFormat: RootJsonFormat[DepotlieferungAboCreate] = jsonFormat7(DepotlieferungAboCreate)
  implicit val heimlieferungAboCreateFormat: RootJsonFormat[HeimlieferungAboCreate] = jsonFormat7(HeimlieferungAboCreate)
  implicit val postlieferungAboCreateFormat: RootJsonFormat[PostlieferungAboCreate] = jsonFormat6(PostlieferungAboCreate)

  implicit val aboCreateFormat = new RootJsonFormat[AboCreate] {
    def write(obj: AboCreate): JsValue =
      JsString(obj.productPrefix)

    def read(json: JsValue): AboCreate = {
      logger.debug("Got new AboCreate" + json.compactPrint)
      if (!json.asJsObject.getFields("depotId").isEmpty) {
        json.convertTo[DepotlieferungAboCreate]
      } else if (!json.asJsObject.getFields("tourId").isEmpty) {
        json.convertTo[HeimlieferungAboCreate]
      } else {
        json.convertTo[PostlieferungAboCreate]
      }
    }
  }

  implicit val aboModifyFormat = new RootJsonFormat[AboModify] {
    def write(obj: AboModify): JsValue =
      JsString(obj.productPrefix)

    def read(json: JsValue): AboModify = {
      logger.debug("Got new AboModify" + json.compactPrint)
      if (!json.asJsObject.getFields("depotId").isEmpty) {
        json.convertTo[DepotlieferungAboModify]
      } else if (!json.asJsObject.getFields("tourId").isEmpty) {
        json.convertTo[HeimlieferungAboModify]
      } else {
        json.convertTo[PostlieferungAboModify]
      }
    }
  }

  implicit val aboPriceModifyFormat: RootJsonFormat[AboPriceModify] = jsonFormat2(AboPriceModify)
  implicit val aboGuthabenModifyFormat: RootJsonFormat[AboGuthabenModify] = jsonFormat3(AboGuthabenModify)
  implicit val aboVertriebsartModifyFormat: RootJsonFormat[AboVertriebsartModify] = jsonFormat3(AboVertriebsartModify)

  implicit val sammelbestellungCreateFormat: RootJsonFormat[SammelbestellungCreate] = jsonFormat4(SammelbestellungCreate)
  implicit val vertriebRecalculationModify: RootJsonFormat[VertriebRecalculationsModify] = jsonFormat2(VertriebRecalculationsModify)
  implicit val lieferungAbotypCreateFormat: RootJsonFormat[LieferungAbotypCreate] = jsonFormat3(LieferungAbotypCreate)
  implicit val lieferungenAbotypCreateFormat: RootJsonFormat[LieferungenAbotypCreate] = jsonFormat3(LieferungenAbotypCreate)
  implicit val lieferungModifyFormat: RootJsonFormat[LieferungModify] = jsonFormat8(LieferungModify)
  implicit val lieferungAbgeschlossenModifyFormat: RootJsonFormat[LieferungAbgeschlossenModify] = jsonFormat2(LieferungAbgeschlossenModify)
  implicit val lieferplanungModifyFormat: RootJsonFormat[LieferplanungModify] = jsonFormat1(LieferplanungModify)
  implicit val lieferpositionModifyFormat: RootJsonFormat[LieferpositionModify] = jsonFormat10(LieferpositionModify)
  implicit val lieferpositionenCreateFormat: RootJsonFormat[LieferpositionenModify] = jsonFormat2(LieferpositionenModify)
  implicit val lieferungPositionenModifyFormat: RootJsonFormat[LieferungPositionenModify] = jsonFormat2(LieferungPositionenModify)
  implicit val lieferplanungDataModifyFormat: RootJsonFormat[LieferplanungDataModify] = jsonFormat3(LieferplanungDataModify)
  implicit val lieferplanungCreateFormat: RootJsonFormat[LieferplanungCreate] = jsonFormat1(LieferplanungCreate)
  implicit val lieferungPlanungAddFormat: RootJsonFormat[LieferungPlanungAdd] = jsonFormat2(LieferungPlanungAdd)
  implicit val lieferungPlanungRemoveFormat: RootJsonFormat[LieferungPlanungRemove] = jsonFormat0(LieferungPlanungRemove)
  implicit val bestellungenCreateFormat: RootJsonFormat[BestellungenCreate] = jsonFormat1(BestellungenCreate)
  implicit val bestellungStatusModify: RootJsonFormat[SammelbestellungStatusModify] = jsonFormat1(SammelbestellungStatusModify)
  implicit val bestellpositionModifyFormat: RootJsonFormat[BestellpositionModify] = jsonFormat8(BestellpositionModify)
  implicit val sammelbestellungModifyFormat: RootJsonFormat[SammelbestellungModify] = jsonFormat3(SammelbestellungModify)
  implicit val produktModifyFormat: RootJsonFormat[ProduktModify] = jsonFormat8(ProduktModify)
  implicit val produktekategorieModifyFormat: RootJsonFormat[ProduktekategorieModify] = jsonFormat1(ProduktekategorieModify)
  implicit val produzentModifyFormat: RootJsonFormat[ProduzentModify] = jsonFormat18(ProduzentModify)

  implicit val korbCreateFormat: RootJsonFormat[KorbCreate] = jsonFormat4(KorbCreate)
  implicit val korbModifyAuslieferungFormat: RootJsonFormat[KorbAuslieferungModify] = jsonFormat2(KorbAuslieferungModify)
  implicit val korbLieferungFormat: RootJsonFormat[KorbLieferung] = jsonFormat12(KorbLieferung)

  implicit val projektKundenportalFormat: RootJsonFormat[ProjektKundenportal] = jsonFormat22(ProjektKundenportal)

  implicit val projektModifyFormat = new RootJsonFormat[ProjektModify] {
    override def read(json: JsValue): ProjektModify = {
      val fields = json.asJsObject.fields
      ProjektModify(
        fields("bezeichnung").convertTo[String],
        fields("strasse").convertTo[Option[String]],
        fields("hausNummer").convertTo[Option[String]],
        fields("adressZusatz").convertTo[Option[String]],
        fields("plz").convertTo[Option[String]],
        fields("ort").convertTo[Option[String]],
        fields("preiseSichtbar").convertTo[Boolean],
        fields("preiseEditierbar").convertTo[Boolean],
        fields("emailErforderlich").convertTo[Boolean],
        fields("waehrung").convertTo[Waehrung],
        fields("geschaeftsjahrMonat").convertTo[Int],
        fields("geschaeftsjahrTag").convertTo[Int],
        fields("twoFactorAuthentication").convertTo[Map[Rolle, Boolean]],
        fields("defaultSecondFactorType").convertTo[SecondFactorType],
        fields("sprache").convertTo[Locale],
        fields("welcomeMessage1").convertTo[Option[String]],
        fields("welcomeMessage2").convertTo[Option[String]],
        fields("maintenanceMode").convertTo[Boolean],
        fields("generierteMailsSenden").convertTo[Boolean],
        fields("einsatzEinheit").convertTo[EinsatzEinheit],
        fields("einsatzAbsageVorlaufTage").convertTo[Int],
        fields("einsatzShowListeKunde").convertTo[Boolean],
        fields("sendEmailToBcc").convertTo[Boolean],
        fields("messageForMembers").convertTo[Option[String]]
      )
    }

    override def write(obj: ProjektModify): JsValue = JsObject(
      "bezeichnung" -> obj.bezeichnung.toJson,
      "strasse" -> obj.strasse.toJson,
      "hausNummer" -> obj.hausNummer.toJson,
      "adressZusatz" -> obj.adressZusatz.toJson,
      "plz" -> obj.plz.toJson,
      "ort" -> obj.ort.toJson,
      "preiseSichtbar" -> obj.preiseSichtbar.toJson,
      "preiseEditierbar" -> obj.preiseEditierbar.toJson,
      "emailErforderlich" -> obj.emailErforderlich.toJson,
      "waehrung" -> obj.waehrung.toJson,
      "geschaeftsjahrMonat" -> obj.geschaeftsjahrMonat.toJson,
      "geschaeftsjahrTag" -> obj.geschaeftsjahrTag.toJson,
      "twoFactorAuthentication" -> obj.twoFactorAuthentication.toJson,
      "defaultSecondFactorType" -> obj.defaultSecondFactorType.toJson,
      "sprache" -> obj.sprache.toJson,
      "welcomeMessage1" -> obj.welcomeMessage1.toJson,
      "welcomeMessage2" -> obj.welcomeMessage2.toJson,
      "maintenanceMode" -> obj.maintenanceMode.toJson,
      "generierteMailsSenden" -> obj.generierteMailsSenden.toJson,
      "einsatzEinheit" -> obj.einsatzEinheit.toJson,
      "einsatzAbsageVorlaufTage" -> obj.einsatzAbsageVorlaufTage.toJson,
      "einsatzShowListeKunde" -> obj.einsatzShowListeKunde.toJson,
      "sendEmailToBcc" -> obj.sendEmailToBcc.toJson,
      "messageForMembers" -> obj.messageForMembers.toJson
    )
  }

  implicit val projektVorlageCreateFormat: RootJsonFormat[ProjektVorlageCreate] = jsonFormat3(ProjektVorlageCreate)
  implicit val projektVorlageModifyFormat: RootJsonFormat[ProjektVorlageModify] = jsonFormat2(ProjektVorlageModify)
  implicit val projektVorlageUploadFormat: RootJsonFormat[ProjektVorlageUpload] = jsonFormat1(ProjektVorlageUpload)

  implicit val kontoDatenModifyFormat: RootJsonFormat[KontoDatenModify] = jsonFormat11(KontoDatenModify)

  implicit val kundeFormat = new RootJsonFormat[Kunde] {
    override def read(json: JsValue): Kunde = {
      val fields = json.asJsObject.fields
      Kunde(
        fields("id").convertTo[KundeId],
        fields("aktiv").convertTo[Boolean],
        fields("bezeichnung").convertTo[String],
        fields("strasse").convertTo[String],
        fields("hausNummer").convertTo[Option[String]],
        fields("adressZusatz").convertTo[Option[String]],
        fields("plz").convertTo[String],
        fields("ort").convertTo[String],
        fields("bemerkungen").convertTo[Option[String]],
        fields("abweichendeLieferadresse").convertTo[Boolean],
        fields("bezeichnungLieferung").convertTo[Option[String]],
        fields("strasseLieferung").convertTo[Option[String]],
        fields("hausNummerLieferung").convertTo[Option[String]],
        fields("adressZusatzLieferung").convertTo[Option[String]],
        fields("plzLieferung").convertTo[Option[String]],
        fields("ortLieferung").convertTo[Option[String]],
        fields("zusatzinfoLieferung").convertTo[Option[String]],
        fields("latLieferung").convertTo[Option[BigDecimal]],
        fields("longLieferung").convertTo[Option[BigDecimal]],
        fields("typen").convertTo[Set[KundentypId]],
        fields("anzahlAbos").convertTo[Int],
        fields("anzahlAbosAktiv").convertTo[Int],
        fields("anzahlPendenzen").convertTo[Int],
        fields("anzahlPersonen").convertTo[Int],
        fields("paymentType").convertTo[Option[PaymentType]],
        fields("erstelldat").convertTo[DateTime],
        fields("ersteller").convertTo[PersonId],
        fields("modifidat").convertTo[DateTime],
        fields("modifikator").convertTo[PersonId]
      )
    }

    override def write(obj: Kunde): JsValue = JsObject(
      "id" -> obj.id.toJson,
      "aktiv" -> obj.aktiv.toJson,
      "bezeichnung" -> obj.bezeichnung.toJson,
      "strasse" -> obj.strasse.toJson,
      "hausNummer" -> obj.hausNummer.toJson,
      "adressZusatz" -> obj.adressZusatz.toJson,
      "plz" -> obj.plz.toJson,
      "ort" -> obj.ort.toJson,
      "bemerkungen" -> obj.bemerkungen.toJson,
      "abweichendeLieferadresse" -> obj.abweichendeLieferadresse.toJson,
      "bezeichnungLieferung" -> obj.bezeichnungLieferung.toJson,
      "strasseLieferung" -> obj.strasseLieferung.toJson,
      "hausNummerLieferung" -> obj.hausNummerLieferung.toJson,
      "adressZusatzLieferung" -> obj.adressZusatzLieferung.toJson,
      "plzLieferung" -> obj.plzLieferung.toJson,
      "ortLieferung" -> obj.ortLieferung.toJson,
      "zusatzinfoLieferung" -> obj.zusatzinfoLieferung.toJson,
      "latLieferung" -> obj.latLieferung.toJson,
      "longLieferung" -> obj.longLieferung.toJson,
      "typen" -> obj.typen.toJson,
      "anzahlAbos" -> obj.anzahlAbos.toJson,
      "anzahlAbosAktiv" -> obj.anzahlAbosAktiv.toJson,
      "anzahlPendenzen" -> obj.anzahlPendenzen.toJson,
      "anzahlPersonen" -> obj.anzahlPersonen.toJson,
      "paymentType" -> obj.paymentType.toJson,
      "erstelldat" -> obj.erstelldat.toJson,
      "ersteller" -> obj.ersteller.toJson,
      "modifidat" -> obj.modifidat.toJson,
      "modifikator" -> obj.modifikator.toJson
    )
  }

  implicit val personSummaryFormat: RootJsonFormat[PersonSummary] = jsonFormat7(PersonSummary)
  private implicit val kontoDatenFormat: RootJsonFormat[KontoDaten] = jsonFormat16(KontoDaten)

  implicit val kundeUebersichtFormat = new RootJsonFormat[KundeUebersicht] {
    override def read(json: JsValue): KundeUebersicht = {
      val fields = json.asJsObject.fields
      KundeUebersicht(
        fields("id").convertTo[KundeId],
        fields("aktiv").convertTo[Boolean],
        fields("bezeichnung").convertTo[String],
        fields("strasse").convertTo[String],
        fields("hausNummer").convertTo[Option[String]],
        fields("adressZusatz").convertTo[Option[String]],
        fields("plz").convertTo[String],
        fields("ort").convertTo[String],
        fields("bemerkungen").convertTo[Option[String]],
        fields("abweichendeLieferadresse").convertTo[Boolean],
        fields("bezeichnungLieferung").convertTo[Option[String]],
        fields("strasseLieferung").convertTo[Option[String]],
        fields("hausNummerLieferung").convertTo[Option[String]],
        fields("adressZusatzLieferung").convertTo[Option[String]],
        fields("plzLieferung").convertTo[Option[String]],
        fields("ortLieferung").convertTo[Option[String]],
        fields("zusatzinfoLieferung").convertTo[Option[String]],
        fields("latLieferung").convertTo[Option[BigDecimal]],
        fields("longLieferung").convertTo[Option[BigDecimal]],
        fields("typen").convertTo[Set[KundentypId]],
        fields("anzahlAbos").convertTo[Int],
        fields("anzahlAbosAktiv").convertTo[Int],
        fields("ansprechpersonen").convertTo[Seq[PersonSummary]],
        fields("paymentType").convertTo[Option[PaymentType]],
        fields("kontoDaten").convertTo[Option[KontoDaten]],
        fields("erstelldat").convertTo[DateTime],
        fields("ersteller").convertTo[PersonId],
        fields("modifidat").convertTo[DateTime],
        fields("modifikator").convertTo[PersonId]
      )
    }

    override def write(obj: KundeUebersicht): JsValue = JsObject(
      "id" -> obj.id.toJson,
      "aktiv" -> obj.aktiv.toJson,
      "bezeichnung" -> obj.bezeichnung.toJson,
      "strasse" -> obj.strasse.toJson,
      "hausNummer" -> obj.hausNummer.toJson,
      "adressZusatz" -> obj.adressZusatz.toJson,
      "plz" -> obj.plz.toJson,
      "ort" -> obj.ort.toJson,
      "bemerkungen" -> obj.bemerkungen.toJson,
      "abweichendeLieferadresse" -> obj.abweichendeLieferadresse.toJson,
      "bezeichnungLieferung" -> obj.bezeichnungLieferung.toJson,
      "strasseLieferung" -> obj.strasseLieferung.toJson,
      "hausNummerLieferung" -> obj.hausNummerLieferung.toJson,
      "adressZusatzLieferung" -> obj.adressZusatzLieferung.toJson,
      "plzLieferung" -> obj.plzLieferung.toJson,
      "ortLieferung" -> obj.ortLieferung.toJson,
      "zusatzinfoLieferung" -> obj.zusatzinfoLieferung.toJson,
      "latLieferung" -> obj.latLieferung.toJson,
      "longLieferung" -> obj.longLieferung.toJson,
      "typen" -> obj.typen.toJson,
      "anzahlAbos" -> obj.anzahlAbos.toJson,
      "anzahlAbosAktiv" -> obj.anzahlAbosAktiv.toJson,
      "ansprechpersonen" -> obj.ansprechpersonen.toJson,
      "paymentType" -> obj.paymentType.toJson,
      "kontoDaten" -> obj.kontoDaten.toJson,
      "erstelldat" -> obj.erstelldat.toJson,
      "ersteller" -> obj.ersteller.toJson,
      "modifidat" -> obj.modifidat.toJson,
      "modifikator" -> obj.modifikator.toJson
    )
  }

  implicit val personModifyFormat: RootJsonFormat[PersonModify] = jsonFormat12(PersonModify)
  implicit val pendenzModifyFormat: RootJsonFormat[PendenzModify] = jsonFormat4(PendenzModify)

  implicit val kundeModifyFormat = new RootJsonFormat[KundeModify] {
    override def read(json: JsValue): KundeModify = {
      val fields = json.asJsObject.fields
      KundeModify(
        fields("aktiv").convertTo[Boolean],
        fields("bezeichnung").convertTo[Option[String]],
        fields("strasse").convertTo[String],
        fields("hausNummer").convertTo[Option[String]],
        fields("adressZusatz").convertTo[Option[String]],
        fields("plz").convertTo[String],
        fields("ort").convertTo[String],
        fields("bemerkungen").convertTo[Option[String]],
        fields("abweichendeLieferadresse").convertTo[Boolean],
        fields("bezeichnungLieferung").convertTo[Option[String]],
        fields("strasseLieferung").convertTo[Option[String]],
        fields("hausNummerLieferung").convertTo[Option[String]],
        fields("adressZusatzLieferung").convertTo[Option[String]],
        fields("plzLieferung").convertTo[Option[String]],
        fields("ortLieferung").convertTo[Option[String]],
        fields("zusatzinfoLieferung").convertTo[Option[String]],
        fields("latLieferung").convertTo[Option[BigDecimal]],
        fields("longLieferung").convertTo[Option[BigDecimal]],
        fields("typen").convertTo[Set[KundentypId]],
        fields("pendenzen").convertTo[Seq[PendenzModify]],
        fields("ansprechpersonen").convertTo[Seq[PersonModify]],
        fields("paymentType").convertTo[Option[PaymentType]],
        fields("kontoDaten").convertTo[Option[KontoDatenModify]]
      )
    }

    override def write(obj: KundeModify): JsValue = JsObject(
      "aktiv" -> obj.aktiv.toJson,
      "bezeichnung" -> obj.bezeichnung.toJson,
      "strasse" -> obj.strasse.toJson,
      "hausNummer" -> obj.hausNummer.toJson,
      "adressZusatz" -> obj.adressZusatz.toJson,
      "plz" -> obj.plz.toJson,
      "ort" -> obj.ort.toJson,
      "bemerkungen" -> obj.bemerkungen.toJson,
      "abweichendeLieferadresse" -> obj.abweichendeLieferadresse.toJson,
      "bezeichnungLieferung" -> obj.bezeichnungLieferung.toJson,
      "strasseLieferung" -> obj.strasseLieferung.toJson,
      "hausNummerLieferung" -> obj.hausNummerLieferung.toJson,
      "adressZusatzLieferung" -> obj.adressZusatzLieferung.toJson,
      "plzLieferung" -> obj.plzLieferung.toJson,
      "ortLieferung" -> obj.ortLieferung.toJson,
      "zusatzinfoLieferung" -> obj.zusatzinfoLieferung.toJson,
      "latLieferung" -> obj.latLieferung.toJson,
      "longLieferung" -> obj.longLieferung.toJson,
      "typen" -> obj.typen.toJson,
      "pendenzen" -> obj.pendenzen.toJson,
      "ansprechpersonen" -> obj.ansprechpersonen.toJson,
      "paymentType" -> obj.paymentType.toJson,
      "kontoDaten" -> obj.kontoDaten.toJson
    )
  }

  implicit val customKundentypCreateFormat: RootJsonFormat[CustomKundentypCreate] = jsonFormat2(CustomKundentypCreate)
  implicit val customKundentypModifyFormat: RootJsonFormat[CustomKundentypModify] = jsonFormat3(CustomKundentypModify)
  implicit val customKundentypModifyV1Format: RootJsonFormat[CustomKundentypModifyV1] = jsonFormat1(CustomKundentypModifyV1)
  implicit val customKundentypFormat: RootJsonFormat[CustomKundentyp] = jsonFormat8(CustomKundentyp)

  // special report formats
  def enhancedProjektReportFormatDef(defaultFormat: JsonFormat[ProjektReport]): RootJsonFormat[ProjektReport] = new RootJsonFormat[ProjektReport] {
    def write(obj: ProjektReport): JsValue = {
      JsObject(defaultFormat.write(obj)
        .asJsObject.fields +
        (
          "strasseUndNummer" -> JsString(obj.strasseUndNummer.getOrElse("")),
          "plzOrt" -> JsString(obj.plzOrt.getOrElse("")),
          "adresszeilen" -> JsArray(obj.adresszeilen.map(JsString(_)).toVector)
        ))
    }

    def read(json: JsValue): ProjektReport = defaultFormat.read(json)
  }
  implicit val enhancedProjektReportFormat: RootJsonFormat[ProjektReport] = enhancedProjektReportFormatDef(jsonFormat19(ProjektReport))

  implicit val personDetailFormat = new RootJsonFormat[PersonDetail] {
    override def read(json: JsValue): PersonDetail = {
      val fields = json.asJsObject.fields
      PersonDetail(
        fields("id").convertTo[PersonId],
        fields("kundeId").convertTo[KundeId],
        fields("anrede").convertTo[Option[Anrede]],
        fields("name").convertTo[String],
        fields("vorname").convertTo[String],
        fields("email").convertTo[Option[String]],
        fields("emailAlternative").convertTo[Option[String]],
        fields("telefonMobil").convertTo[Option[String]],
        fields("telefonFestnetz").convertTo[Option[String]],
        fields("bemerkungen").convertTo[Option[String]],
        fields("sort").convertTo[Int],
        fields("loginAktiv").convertTo[Boolean],
        fields("letzteAnmeldung").convertTo[Option[DateTime]],
        fields("passwortWechselErforderlich").convertTo[Boolean],
        fields("rolle").convertTo[Option[Rolle]],
        fields("categories").convertTo[Set[PersonCategoryNameId]],
        fields("secondFactorType").convertTo[Option[SecondFactorType]],
        fields("otpReset").convertTo[Boolean],
        fields("contactPermission").convertTo[Boolean],
        fields("erstelldat").convertTo[DateTime],
        fields("ersteller").convertTo[PersonId],
        fields("modifidat").convertTo[DateTime],
        fields("modifikator").convertTo[PersonId]
      )
    }

    override def write(obj: PersonDetail): JsValue = JsObject(
      "id" -> obj.id.toJson,
      "kundeId" -> obj.kundeId.toJson,
      "anrede" -> obj.anrede.toJson,
      "name" -> obj.name.toJson,
      "vorname" -> obj.vorname.toJson,
      "email" -> obj.email.toJson,
      "emailAlternative" -> obj.emailAlternative.toJson,
      "telefonMobil" -> obj.telefonMobil.toJson,
      "telefonFestnetz" -> obj.telefonFestnetz.toJson,
      "bemerkungen" -> obj.bemerkungen.toJson,
      "sort" -> obj.sort.toJson,
      "loginAktiv" -> obj.loginAktiv.toJson,
      "letzteAnmeldung" -> obj.letzteAnmeldung.toJson,
      "passwortWechselErforderlich" -> obj.passwortWechselErforderlich.toJson,
      "rolle" -> obj.rolle.toJson,
      "categories" -> obj.categories.toJson,
      "secondFactorType" -> obj.secondFactorType.toJson,
      "otpReset" -> obj.otpReset.toJson,
      "contactPermission" -> obj.contactPermission.toJson,
      "erstelldat" -> obj.erstelldat.toJson,
      "ersteller" -> obj.ersteller.toJson,
      "modifidat" -> obj.modifidat.toJson,
      "modifikator" -> obj.modifikator.toJson
    )
  }

  implicit val kundeReportFormat = new RootJsonFormat[KundeReport] {
    override def read(json: JsValue): KundeReport = {
      val fields = json.asJsObject.fields
      KundeReport(
        fields("id").convertTo[KundeId],
        fields("aktiv").convertTo[Boolean],
        fields("bezeichnung").convertTo[String],
        fields("strasse").convertTo[String],
        fields("hausNummer").convertTo[Option[String]],
        fields("adressZusatz").convertTo[Option[String]],
        fields("plz").convertTo[String],
        fields("ort").convertTo[String],
        fields("bemerkungen").convertTo[Option[String]],
        fields("abweichendeLieferadresse").convertTo[Boolean],
        fields("bezeichnungLieferung").convertTo[Option[String]],
        fields("strasseLieferung").convertTo[Option[String]],
        fields("hausNummerLieferung").convertTo[Option[String]],
        fields("adressZusatzLieferung").convertTo[Option[String]],
        fields("plzLieferung").convertTo[Option[String]],
        fields("ortLieferung").convertTo[Option[String]],
        fields("zusatzinfoLieferung").convertTo[Option[String]],
        fields("latLieferung").convertTo[Option[BigDecimal]],
        fields("longLieferung").convertTo[Option[BigDecimal]],
        fields("typen").convertTo[Set[KundentypId]],
        fields("personen").convertTo[Seq[PersonDetail]],
        fields("anzahlAbos").convertTo[Int],
        fields("anzahlAbosAktiv").convertTo[Int],
        fields("anzahlPendenzen").convertTo[Int],
        fields("anzahlPersonen").convertTo[Int],
        fields("paymentType").convertTo[Option[PaymentType]],
        fields("erstelldat").convertTo[DateTime],
        fields("ersteller").convertTo[PersonId],
        fields("modifidat").convertTo[DateTime],
        fields("modifikator").convertTo[PersonId]
      )
    }

    override def write(obj: KundeReport): JsValue = JsObject(
      "id" -> obj.id.toJson,
      "aktiv" -> obj.aktiv.toJson,
      "bezeichnung" -> obj.bezeichnung.toJson,
      "strasse" -> obj.strasse.toJson,
      "hausNummer" -> obj.hausNummer.toJson,
      "adressZusatz" -> obj.adressZusatz.toJson,
      "plz" -> obj.plz.toJson,
      "ort" -> obj.ort.toJson,
      "bemerkungen" -> obj.bemerkungen.toJson,
      "abweichendeLieferadresse" -> obj.abweichendeLieferadresse.toJson,
      "bezeichnungLieferung" -> obj.bezeichnungLieferung.toJson,
      "strasseLieferung" -> obj.strasseLieferung.toJson,
      "hausNummerLieferung" -> obj.hausNummerLieferung.toJson,
      "adressZusatzLieferung" -> obj.adressZusatzLieferung.toJson,
      "plzLieferung" -> obj.plzLieferung.toJson,
      "ortLieferung" -> obj.ortLieferung.toJson,
      "zusatzinfoLieferung" -> obj.zusatzinfoLieferung.toJson,
      "latLieferung" -> obj.latLieferung.toJson,
      "longLieferung" -> obj.longLieferung.toJson,
      "typen" -> obj.typen.toJson,
      "personen" -> obj.personen.toJson,
      "anzahlAbos" -> obj.anzahlAbos.toJson,
      "anzahlAbosAktiv" -> obj.anzahlAbosAktiv.toJson,
      "anzahlPendenzen" -> obj.anzahlPendenzen.toJson,
      "anzahlPersonen" -> obj.anzahlPersonen.toJson,
      "paymentType" -> obj.paymentType.toJson,
      "erstelldat" -> obj.erstelldat.toJson,
      "ersteller" -> obj.ersteller.toJson,
      "modifidat" -> obj.modifidat.toJson,
      "modifikator" -> obj.modifikator.toJson
    )
  }

  implicit val personCreateFormat: RootJsonFormat[PersonCreate] = jsonFormat13(PersonCreate)
  implicit val personModifyV1Format: RootJsonFormat[PersonModifyV1] = jsonFormat9(PersonModifyV1)
  implicit val personModifyV2Format: RootJsonFormat[PersonModifyV2] = jsonFormat10(PersonModifyV2)
  implicit val personModifyV3Format: RootJsonFormat[PersonModifyV3] = jsonFormat11(PersonModifyV3)
  implicit val personCategoryCreateFormat: RootJsonFormat[PersonCategoryCreate] = jsonFormat2(PersonCategoryCreate)
  implicit val personCategoryModifyFormat: RootJsonFormat[PersonCategoryModify] = jsonFormat3(PersonCategoryModify)
  implicit val personCategoryFormat: RootJsonFormat[PersonCategory] = jsonFormat7(PersonCategory)
  implicit val personFormat = new RootJsonFormat[Person] {
    private val internalPersonFormat = new RootJsonFormat[Person] {
      override def read(json: JsValue): Person = {
        val fields = json.asJsObject.fields
        Person(
          fields("id").convertTo[PersonId],
          fields("kundeId").convertTo[KundeId],
          fields("anrede").convertTo[Option[Anrede]],
          fields("name").convertTo[String],
          fields("vorname").convertTo[String],
          fields("email").convertTo[Option[String]],
          fields("emailAlternative").convertTo[Option[String]],
          fields("telefonMobil").convertTo[Option[String]],
          fields("telefonFestnetz").convertTo[Option[String]],
          fields("bemerkungen").convertTo[Option[String]],
          fields("sort").convertTo[Int],
          fields("loginAktiv").convertTo[Boolean],
          fields("passwort").convertTo[Option[Array[Char]]],
          fields("letzteAnmeldung").convertTo[Option[DateTime]],
          fields("passwortWechselErforderlich").convertTo[Boolean],
          fields("rolle").convertTo[Option[Rolle]],
          fields("categories").convertTo[Set[PersonCategoryNameId]],
          fields("secondFactorType").convertTo[Option[SecondFactorType]],
          fields("otpSecret").convertTo[String],
          fields("otpReset").convertTo[Boolean],
          fields("contactPermission").convertTo[Boolean],
          fields("erstelldat").convertTo[DateTime],
          fields("ersteller").convertTo[PersonId],
          fields("modifidat").convertTo[DateTime],
          fields("modifikator").convertTo[PersonId]
        )
      }

      override def write(obj: Person): JsValue = JsObject(
        "id" -> obj.id.toJson,
        "kundeId" -> obj.kundeId.toJson,
        "anrede" -> obj.anrede.toJson,
        "name" -> obj.name.toJson,
        "vorname" -> obj.vorname.toJson,
        "email" -> obj.email.toJson,
        "emailAlternative" -> obj.emailAlternative.toJson,
        "telefonMobil" -> obj.telefonMobil.toJson,
        "telefonFestnetz" -> obj.telefonFestnetz.toJson,
        "bemerkungen" -> obj.bemerkungen.toJson,
        "sort" -> obj.sort.toJson,
        "loginAktiv" -> obj.loginAktiv.toJson,
        "passwort" -> obj.passwort.toJson,
        "letzteAnmeldung" -> obj.letzteAnmeldung.toJson,
        "passwortWechselErforderlich" -> obj.passwortWechselErforderlich.toJson,
        "rolle" -> obj.rolle.toJson,
        "categories" -> obj.categories.toJson,
        "secondFactorType" -> obj.secondFactorType.toJson,
        "otpSecret" -> obj.otpSecret.toJson,
        "otpReset" -> obj.otpReset.toJson,
        "contactPermission" -> obj.contactPermission.toJson,
        "erstelldat" -> obj.erstelldat.toJson,
        "ersteller" -> obj.ersteller.toJson,
        "modifidat" -> obj.modifidat.toJson,
        "modifikator" -> obj.modifikator.toJson
      )
    }

    override def read(json: JsValue): Person = internalPersonFormat.read(json)

    override def write(obj: Person): JsValue = JsObject(obj.toJson(internalPersonFormat).asJsObject.fields - "passwort")
  }

  implicit val personUebersichtFormat = new RootJsonFormat[PersonUebersicht] {
    override def read(json: JsValue): PersonUebersicht = {
      val fields = json.asJsObject.fields
      PersonUebersicht(
        fields("id").convertTo[PersonId],
        fields("kundeId").convertTo[KundeId],
        fields("anrede").convertTo[Option[Anrede]],
        fields("name").convertTo[String],
        fields("vorname").convertTo[String],
        fields("email").convertTo[Option[String]],
        fields("emailAlternative").convertTo[Option[String]],
        fields("telefonMobil").convertTo[Option[String]],
        fields("telefonFestnetz").convertTo[Option[String]],
        fields("bemerkungen").convertTo[Option[String]],
        fields("loginAktiv").convertTo[Boolean],
        fields("letzteAnmeldung").convertTo[Option[DateTime]],
        fields("rolle").convertTo[Option[Rolle]],
        fields("categories").convertTo[Set[PersonCategoryNameId]],
        fields("contactPermission").convertTo[Boolean],
        fields("strasse").convertTo[String],
        fields("hausNummer").convertTo[Option[String]],
        fields("adressZusatz").convertTo[Option[String]],
        fields("plz").convertTo[String],
        fields("ort").convertTo[String],
        fields("kundentypen").convertTo[Set[KundentypId]],
        fields("kundenBemerkungen").convertTo[Option[String]],
        fields("erstelldat").convertTo[DateTime],
        fields("ersteller").convertTo[PersonId],
        fields("modifidat").convertTo[DateTime],
        fields("modifikator").convertTo[PersonId]
      )
    }

    override def write(obj: PersonUebersicht): JsValue = JsObject(
      "id" -> obj.id.toJson,
      "kundeId" -> obj.kundeId.toJson,
      "anrede" -> obj.anrede.toJson,
      "name" -> obj.name.toJson,
      "vorname" -> obj.vorname.toJson,
      "email" -> obj.email.toJson,
      "emailAlternative" -> obj.emailAlternative.toJson,
      "telefonMobil" -> obj.telefonMobil.toJson,
      "telefonFestnetz" -> obj.telefonFestnetz.toJson,
      "bemerkungen" -> obj.bemerkungen.toJson,
      "loginAktiv" -> obj.loginAktiv.toJson,
      "letzteAnmeldung" -> obj.letzteAnmeldung.toJson,
      "rolle" -> obj.rolle.toJson,
      "categories" -> obj.categories.toJson,
      "contactPermission" -> obj.contactPermission.toJson,
      "strasse" -> obj.strasse.toJson,
      "hausNummer" -> obj.hausNummer.toJson,
      "adressZusatz" -> obj.adressZusatz.toJson,
      "plz" -> obj.plz.toJson,
      "ort" -> obj.ort.toJson,
      "kundentypen" -> obj.kundentypen.toJson,
      "kundenBemerkungen" -> obj.kundenBemerkungen.toJson,
      "erstelldat" -> obj.erstelldat.toJson,
      "ersteller" -> obj.ersteller.toJson,
      "modifidat" -> obj.modifidat.toJson,
      "modifikator" -> obj.modifikator.toJson
    )
  }

  implicit val abwesenheitCreateFormat: RootJsonFormat[AbwesenheitCreate] = jsonFormat4(AbwesenheitCreate)
  implicit val pendenzFormat: RootJsonFormat[Pendenz] = jsonFormat11(Pendenz)
  implicit val pendenzCreateFormat: RootJsonFormat[PendenzCreate] = jsonFormat5(PendenzCreate)

  implicit val kundeDetailReportFormat = new RootJsonFormat[KundeDetailReport] {
    override def read(json: JsValue): KundeDetailReport = {
      val fields = json.asJsObject.fields
      KundeDetailReport(
        fields("id").convertTo[KundeId],
        fields("aktiv").convertTo[Boolean],
        fields("bezeichnung").convertTo[String],
        fields("strasse").convertTo[String],
        fields("hausNummer").convertTo[Option[String]],
        fields("adressZusatz").convertTo[Option[String]],
        fields("plz").convertTo[String],
        fields("ort").convertTo[String],
        fields("bemerkungen").convertTo[Option[String]],
        fields("abweichendeLieferadresse").convertTo[Boolean],
        fields("bezeichnungLieferung").convertTo[Option[String]],
        fields("strasseLieferung").convertTo[Option[String]],
        fields("hausNummerLieferung").convertTo[Option[String]],
        fields("adressZusatzLieferung").convertTo[Option[String]],
        fields("plzLieferung").convertTo[Option[String]],
        fields("ortLieferung").convertTo[Option[String]],
        fields("zusatzinfoLieferung").convertTo[Option[String]],
        fields("latLieferung").convertTo[Option[BigDecimal]],
        fields("longLieferung").convertTo[Option[BigDecimal]],
        fields("typen").convertTo[Set[KundentypId]],
        fields("anzahlAbos").convertTo[Int],
        fields("anzahlAbosAktiv").convertTo[Int],
        fields("anzahlPendenzen").convertTo[Int],
        fields("anzahlPersonen").convertTo[Int],
        fields("paymentType").convertTo[Option[PaymentType]],
        fields("personen").convertTo[Seq[PersonDetail]],
        fields("abos").convertTo[Seq[Abo]],
        fields("pendenzen").convertTo[Seq[Pendenz]],
        fields("projekt").convertTo[ProjektReport],
        fields("erstelldat").convertTo[DateTime],
        fields("ersteller").convertTo[PersonId],
        fields("modifidat").convertTo[DateTime],
        fields("modifikator").convertTo[PersonId]
      )
    }

    override def write(obj: KundeDetailReport): JsValue = JsObject(
      "id" -> obj.id.toJson,
      "aktiv" -> obj.aktiv.toJson,
      "bezeichnung" -> obj.bezeichnung.toJson,
      "strasse" -> obj.strasse.toJson,
      "hausNummer" -> obj.hausNummer.toJson,
      "adressZusatz" -> obj.adressZusatz.toJson,
      "plz" -> obj.plz.toJson,
      "ort" -> obj.ort.toJson,
      "bemerkungen" -> obj.bemerkungen.toJson,
      "abweichendeLieferadresse" -> obj.abweichendeLieferadresse.toJson,
      "bezeichnungLieferung" -> obj.bezeichnungLieferung.toJson,
      "strasseLieferung" -> obj.strasseLieferung.toJson,
      "hausNummerLieferung" -> obj.hausNummerLieferung.toJson,
      "adressZusatzLieferung" -> obj.adressZusatzLieferung.toJson,
      "plzLieferung" -> obj.plzLieferung.toJson,
      "ortLieferung" -> obj.ortLieferung.toJson,
      "zusatzinfoLieferung" -> obj.zusatzinfoLieferung.toJson,
      "latLieferung" -> obj.latLieferung.toJson,
      "longLieferung" -> obj.longLieferung.toJson,
      "typen" -> obj.typen.toJson,
      "anzahlAbos" -> obj.anzahlAbos.toJson,
      "anzahlAbosAktiv" -> obj.anzahlAbosAktiv.toJson,
      "anzahlPendenzen" -> obj.anzahlPendenzen.toJson,
      "anzahlPersonen" -> obj.anzahlPersonen.toJson,
      "paymentType" -> obj.paymentType.toJson,
      "personen" -> obj.personen.toJson,
      "abos" -> obj.abos.toJson,
      "pendenzen" -> obj.pendenzen.toJson,
      "projekt" -> obj.projekt.toJson,
      "erstelldat" -> obj.erstelldat.toJson,
      "ersteller" -> obj.ersteller.toJson,
      "modifidat" -> obj.modifidat.toJson,
      "modifikator" -> obj.modifikator.toJson,
      // additional fields
      "strasseUndNummer" -> JsString(obj.strasseUndNummer),
      "plzOrt" -> JsString(obj.plzOrt),
      "strasseUndNummerLieferung" -> JsString(obj.strasseUndNummerLieferung),
      "plzOrtLieferung" -> JsString(obj.plzOrtLieferung),
      "adresszeilen" -> JsArray(obj.adresszeilen.map(JsString(_)).toVector),
      "lieferAdresszeilen" -> JsArray(obj.lieferAdresszeilen.map(JsString(_)).toVector),
      "telefonNummern" -> JsString(obj.telefonNummern)
    )
  }

  implicit val kundeDetailFormat = new RootJsonFormat[KundeDetail] {
    override def read(json: JsValue): KundeDetail = {
      val fields = json.asJsObject.fields
      KundeDetail(
        fields("id").convertTo[KundeId],
        fields("aktiv").convertTo[Boolean],
        fields("bezeichnung").convertTo[String],
        fields("strasse").convertTo[String],
        fields("hausNummer").convertTo[Option[String]],
        fields("adressZusatz").convertTo[Option[String]],
        fields("plz").convertTo[String],
        fields("ort").convertTo[String],
        fields("bemerkungen").convertTo[Option[String]],
        fields("abweichendeLieferadresse").convertTo[Boolean],
        fields("bezeichnungLieferung").convertTo[Option[String]],
        fields("strasseLieferung").convertTo[Option[String]],
        fields("hausNummerLieferung").convertTo[Option[String]],
        fields("adressZusatzLieferung").convertTo[Option[String]],
        fields("plzLieferung").convertTo[Option[String]],
        fields("ortLieferung").convertTo[Option[String]],
        fields("zusatzinfoLieferung").convertTo[Option[String]],
        fields("latLieferung").convertTo[Option[BigDecimal]],
        fields("longLieferung").convertTo[Option[BigDecimal]],
        fields("typen").convertTo[Set[KundentypId]],
        fields("anzahlAbos").convertTo[Int],
        fields("anzahlAbosAktiv").convertTo[Int],
        fields("anzahlPendenzen").convertTo[Int],
        fields("anzahlPersonen").convertTo[Int],
        fields("paymentType").convertTo[Option[PaymentType]],
        fields("abos").convertTo[Seq[Abo]],
        fields("pendenzen").convertTo[Seq[Pendenz]],
        fields("ansprechpersonen").convertTo[Seq[PersonDetail]],
        fields("kontoDaten").convertTo[Option[KontoDaten]],
        fields("erstelldat").convertTo[DateTime],
        fields("ersteller").convertTo[PersonId],
        fields("modifidat").convertTo[DateTime],
        fields("modifikator").convertTo[PersonId]
      )
    }

    override def write(obj: KundeDetail): JsValue = JsObject(
      "id" -> obj.id.toJson,
      "aktiv" -> obj.aktiv.toJson,
      "bezeichnung" -> obj.bezeichnung.toJson,
      "strasse" -> obj.strasse.toJson,
      "hausNummer" -> obj.hausNummer.toJson,
      "adressZusatz" -> obj.adressZusatz.toJson,
      "plz" -> obj.plz.toJson,
      "ort" -> obj.ort.toJson,
      "bemerkungen" -> obj.bemerkungen.toJson,
      "abweichendeLieferadresse" -> obj.abweichendeLieferadresse.toJson,
      "bezeichnungLieferung" -> obj.bezeichnungLieferung.toJson,
      "strasseLieferung" -> obj.strasseLieferung.toJson,
      "hausNummerLieferung" -> obj.hausNummerLieferung.toJson,
      "adressZusatzLieferung" -> obj.adressZusatzLieferung.toJson,
      "plzLieferung" -> obj.plzLieferung.toJson,
      "ortLieferung" -> obj.ortLieferung.toJson,
      "zusatzinfoLieferung" -> obj.zusatzinfoLieferung.toJson,
      "latLieferung" -> obj.latLieferung.toJson,
      "longLieferung" -> obj.longLieferung.toJson,
      "typen" -> obj.typen.toJson,
      "anzahlAbos" -> obj.anzahlAbos.toJson,
      "anzahlAbosAktiv" -> obj.anzahlAbosAktiv.toJson,
      "anzahlPendenzen" -> obj.anzahlPendenzen.toJson,
      "anzahlPersonen" -> obj.anzahlPersonen.toJson,
      "paymentType" -> obj.paymentType.toJson,
      "abos" -> obj.abos.toJson,
      "pendenzen" -> obj.pendenzen.toJson,
      "ansprechpersonen" -> obj.ansprechpersonen.toJson,
      "kontoDaten" -> obj.kontoDaten.toJson,
      "erstelldat" -> obj.erstelldat.toJson,
      "ersteller" -> obj.ersteller.toJson,
      "modifidat" -> obj.modifidat.toJson,
      "modifikator" -> obj.modifikator.toJson
    )
  }

  implicit val korbReportFormat: RootJsonFormat[KorbReport] = jsonFormat18(KorbReport)

  def enhancedBestellpositionFormatDef[T <: BestellpositionCalculatedFields](defaultFormat: JsonFormat[T]): RootJsonFormat[T] = new RootJsonFormat[T] {
    def write(obj: T): JsValue = {
      JsObject(defaultFormat.write(obj)
        .asJsObject.fields +
        (
          "mengeTotal" -> JsNumber(obj.mengeTotal)
        ))
    }

    def read(json: JsValue): T = defaultFormat.read(json)
  }
  implicit val enhancedBestellpositionFormat: RootJsonFormat[Bestellposition] = enhancedBestellpositionFormatDef(jsonFormat13(Bestellposition))

  implicit val depotlieferungAboReportFormat = new RootJsonFormat[DepotlieferungAboReport] {
    override def read(json: JsValue): DepotlieferungAboReport = {
      val fields = json.asJsObject.fields
      DepotlieferungAboReport(
        fields("id").convertTo[AboId],
        fields("kundeId").convertTo[KundeId],
        fields("kunde").convertTo[String],
        fields("kundeReport").convertTo[KundeReport],
        fields("vertriebsartId").convertTo[VertriebsartId],
        fields("vertriebId").convertTo[VertriebId],
        fields("vertriebBeschrieb").convertTo[Option[String]],
        fields("abotypId").convertTo[AbotypId],
        fields("abotypName").convertTo[String],
        fields("depotId").convertTo[DepotId],
        fields("depotName").convertTo[String],
        fields("start").convertTo[LocalDate],
        fields("ende").convertTo[Option[LocalDate]],
        fields("price").convertTo[Option[BigDecimal]],
        fields("guthabenVertraglich").convertTo[Option[Int]],
        fields("guthaben").convertTo[Int],
        fields("guthabenInRechnung").convertTo[Int],
        fields("letzteLieferung").convertTo[Option[DateTime]],
        fields("anzahlAbwesenheiten").convertTo[TreeMap[String, Int]],
        fields("anzahlLieferungen").convertTo[TreeMap[String, Int]],
        fields("aktiv").convertTo[Boolean],
        fields("anzahlEinsaetze").convertTo[TreeMap[String, BigDecimal]],
        fields("erstelldat").convertTo[DateTime],
        fields("ersteller").convertTo[PersonId],
        fields("modifidat").convertTo[DateTime],
        fields("modifikator").convertTo[PersonId]
      )
    }

    override def write(obj: DepotlieferungAboReport): JsValue = JsObject(
      "id" -> obj.id.toJson,
      "kundeId" -> obj.kundeId.toJson,
      "kunde" -> obj.kunde.toJson,
      "kundeReport" -> obj.kundeReport.toJson,
      "vertriebsartId" -> obj.vertriebsartId.toJson,
      "vertriebId" -> obj.vertriebId.toJson,
      "vertriebBeschrieb" -> obj.vertriebBeschrieb.toJson,
      "abotypId" -> obj.abotypId.toJson,
      "abotypName" -> obj.abotypName.toJson,
      "depotId" -> obj.depotId.toJson,
      "depotName" -> obj.depotName.toJson,
      "start" -> obj.start.toJson,
      "ende" -> obj.ende.toJson,
      "price" -> obj.price.toJson,
      "guthabenVertraglich" -> obj.guthabenVertraglich.toJson,
      "guthaben" -> obj.guthaben.toJson,
      "guthabenInRechnung" -> obj.guthabenInRechnung.toJson,
      "letzteLieferung" -> obj.letzteLieferung.toJson,
      "anzahlAbwesenheiten" -> obj.anzahlAbwesenheiten.toJson,
      "anzahlLieferungen" -> obj.anzahlLieferungen.toJson,
      "aktiv" -> obj.aktiv.toJson,
      "anzahlEinsaetze" -> obj.anzahlEinsaetze.toJson,
      "erstelldat" -> obj.erstelldat.toJson,
      "ersteller" -> obj.ersteller.toJson,
      "modifidat" -> obj.modifidat.toJson,
      "modifikator" -> obj.modifikator.toJson
    )
  }

  implicit val depotReportFormat = new RootJsonFormat[DepotReport] {
    override def read(json: JsValue): DepotReport = {
      val fields = json.asJsObject.fields
      DepotReport(
        fields("id").convertTo[DepotId],
        fields("name").convertTo[String],
        fields("kurzzeichen").convertTo[String],
        fields("apName").convertTo[Option[String]],
        fields("apVorname").convertTo[Option[String]],
        fields("apTelefon").convertTo[Option[String]],
        fields("apEmail").convertTo[Option[String]],
        fields("vName").convertTo[Option[String]],
        fields("vVorname").convertTo[Option[String]],
        fields("vTelefon").convertTo[Option[String]],
        fields("vEmail").convertTo[Option[String]],
        fields("strasse").convertTo[Option[String]],
        fields("hausNummer").convertTo[Option[String]],
        fields("plz").convertTo[String],
        fields("ort").convertTo[String],
        fields("aktiv").convertTo[Boolean],
        fields("oeffnungszeiten").convertTo[Option[String]],
        fields("farbCode").convertTo[Option[String]],
        fields("iban").convertTo[Option[String]],
        fields("bank").convertTo[Option[String]],
        fields("beschreibung").convertTo[Option[String]],
        fields("anzahlAbonnentenMax").convertTo[Option[Int]],
        fields("anzahlAbonnenten").convertTo[Int],
        fields("anzahlAbonnentenAktiv").convertTo[Int],
        fields("erstelldat").convertTo[DateTime],
        fields("ersteller").convertTo[PersonId],
        fields("modifidat").convertTo[DateTime],
        fields("modifikator").convertTo[PersonId]
      )
    }

    override def write(obj: DepotReport): JsValue = JsObject(
      "id" -> obj.id.toJson,
      "name" -> obj.name.toJson,
      "kurzzeichen" -> obj.kurzzeichen.toJson,
      "apName" -> obj.apName.toJson,
      "apVorname" -> obj.apVorname.toJson,
      "apTelefon" -> obj.apTelefon.toJson,
      "apEmail" -> obj.apEmail.toJson,
      "vName" -> obj.vName.toJson,
      "vVorname" -> obj.vVorname.toJson,
      "vTelefon" -> obj.vTelefon.toJson,
      "vEmail" -> obj.vEmail.toJson,
      "strasse" -> obj.strasse.toJson,
      "hausNummer" -> obj.hausNummer.toJson,
      "plz" -> obj.plz.toJson,
      "ort" -> obj.ort.toJson,
      "aktiv" -> obj.aktiv.toJson,
      "oeffnungszeiten" -> obj.oeffnungszeiten.toJson,
      "farbCode" -> obj.farbCode.toJson,
      "iban" -> obj.iban.toJson,
      "bank" -> obj.bank.toJson,
      "beschreibung" -> obj.beschreibung.toJson,
      "anzahlAbonnentenMax" -> obj.anzahlAbonnentenMax.toJson,
      //Zusatzinformationen
      "anzahlAbonnenten" -> obj.anzahlAbonnenten.toJson,
      "anzahlAbonnentenAktiv" -> obj.anzahlAbonnentenAktiv.toJson,
      "erstelldat" -> obj.erstelldat.toJson,
      "ersteller" -> obj.ersteller.toJson,
      "modifidat" -> obj.modifidat.toJson,
      "modifikator" -> obj.modifikator.toJson,
      // additional fields
      "strasseUndNummer" -> JsString(obj.strasseUndNummer.getOrElse("")),
      "plzOrt" -> JsString(obj.plzOrt),
      "adresszeilen" -> JsArray(obj.adresszeilen.map(JsString(_)).toVector)
    )
  }

  implicit val depotDetailReportFormat = new RootJsonFormat[DepotDetailReport] {
    override def read(json: JsValue): DepotDetailReport = {
      val fields = json.asJsObject.fields
      DepotDetailReport(
        fields("id").convertTo[DepotId],
        fields("name").convertTo[String],
        fields("kurzzeichen").convertTo[String],
        fields("apName").convertTo[Option[String]],
        fields("apVorname").convertTo[Option[String]],
        fields("apTelefon").convertTo[Option[String]],
        fields("apEmail").convertTo[Option[String]],
        fields("vName").convertTo[Option[String]],
        fields("vVorname").convertTo[Option[String]],
        fields("vTelefon").convertTo[Option[String]],
        fields("vEmail").convertTo[Option[String]],
        fields("strasse").convertTo[Option[String]],
        fields("hausNummer").convertTo[Option[String]],
        fields("plz").convertTo[String],
        fields("ort").convertTo[String],
        fields("aktiv").convertTo[Boolean],
        fields("oeffnungszeiten").convertTo[Option[String]],
        fields("farbCode").convertTo[Option[String]],
        fields("iban").convertTo[Option[String]],
        fields("bank").convertTo[Option[String]],
        fields("beschreibung").convertTo[Option[String]],
        fields("anzahlAbonnentenMax").convertTo[Option[Int]],
        fields("anzahlAbonnenten").convertTo[Int],
        fields("anzahlAbonnentenAktiv").convertTo[Int],
        fields("abos").convertTo[Seq[DepotlieferungAboReport]],
        fields("projekt").convertTo[ProjektReport],
        fields("erstelldat").convertTo[DateTime],
        fields("ersteller").convertTo[PersonId],
        fields("modifidat").convertTo[DateTime],
        fields("modifikator").convertTo[PersonId]
      )
    }

    override def write(obj: DepotDetailReport): JsValue = JsObject(
      "id" -> obj.id.toJson,
      "name" -> obj.name.toJson,
      "kurzzeichen" -> obj.kurzzeichen.toJson,
      "apName" -> obj.apName.toJson,
      "apVorname" -> obj.apVorname.toJson,
      "apTelefon" -> obj.apTelefon.toJson,
      "apEmail" -> obj.apEmail.toJson,
      "vName" -> obj.vName.toJson,
      "vVorname" -> obj.vVorname.toJson,
      "vTelefon" -> obj.vTelefon.toJson,
      "vEmail" -> obj.vEmail.toJson,
      "strasse" -> obj.strasse.toJson,
      "hausNummer" -> obj.hausNummer.toJson,
      "plz" -> obj.plz.toJson,
      "ort" -> obj.ort.toJson,
      "aktiv" -> obj.aktiv.toJson,
      "oeffnungszeiten" -> obj.oeffnungszeiten.toJson,
      "farbCode" -> obj.farbCode.toJson,
      "iban" -> obj.iban.toJson,
      "bank" -> obj.bank.toJson,
      "beschreibung" -> obj.beschreibung.toJson,
      "anzahlAbonnentenMax" -> obj.anzahlAbonnentenMax.toJson,
      "anzahlAbonnenten" -> obj.anzahlAbonnenten.toJson,
      "anzahlAbonnentenAktiv" -> obj.anzahlAbonnentenAktiv.toJson,
      "abos" -> obj.abos.toJson,
      "projekt" -> obj.projekt.toJson,
      "erstelldat" -> obj.erstelldat.toJson,
      "ersteller" -> obj.ersteller.toJson,
      "modifidat" -> obj.modifidat.toJson,
      "modifikator" -> obj.modifikator.toJson,
      // additional fields
      "strasseUndNummer" -> JsString(obj.strasseUndNummer.getOrElse("")),
      "plzOrt" -> JsString(obj.plzOrt),
      "adresszeilen" -> JsArray(obj.adresszeilen.map(JsString(_)).toVector)
    )
  }

  implicit val produzentDetailReportFormat = new RootJsonFormat[ProduzentDetailReport] {
    override def read(json: JsValue): ProduzentDetailReport = {
      val fields = json.asJsObject.fields
      ProduzentDetailReport(
        fields("id").convertTo[ProduzentId],
        fields("name").convertTo[String],
        fields("vorname").convertTo[Option[String]],
        fields("kurzzeichen").convertTo[String],
        fields("strasse").convertTo[Option[String]],
        fields("hausNummer").convertTo[Option[String]],
        fields("adressZusatz").convertTo[Option[String]],
        fields("plz").convertTo[String],
        fields("ort").convertTo[String],
        fields("bemerkungen").convertTo[Option[String]],
        fields("email").convertTo[String],
        fields("telefonMobil").convertTo[Option[String]],
        fields("telefonFestnetz").convertTo[Option[String]],
        fields("iban").convertTo[Option[String]],
        fields("bank").convertTo[Option[String]],
        fields("mwst").convertTo[Boolean],
        fields("mwstSatz").convertTo[Option[BigDecimal]],
        fields("mwstNr").convertTo[Option[String]],
        fields("aktiv").convertTo[Boolean],
        fields("projekt").convertTo[ProjektReport],
        fields("erstelldat").convertTo[DateTime],
        fields("ersteller").convertTo[PersonId],
        fields("modifidat").convertTo[DateTime],
        fields("modifikator").convertTo[PersonId]
      )
    }

    override def write(obj: ProduzentDetailReport): JsValue = JsObject(
      "id" -> obj.id.toJson,
      "name" -> obj.name.toJson,
      "vorname" -> obj.vorname.toJson,
      "kurzzeichen" -> obj.kurzzeichen.toJson,
      "strasse" -> obj.strasse.toJson,
      "hausNummer" -> obj.hausNummer.toJson,
      "adressZusatz" -> obj.adressZusatz.toJson,
      "plz" -> obj.plz.toJson,
      "ort" -> obj.ort.toJson,
      "bemerkungen" -> obj.bemerkungen.toJson,
      "email" -> obj.email.toJson,
      "telefonMobil" -> obj.telefonMobil.toJson,
      "telefonFestnetz" -> obj.telefonFestnetz.toJson,
      "iban" -> obj.iban.toJson,
      "bank" -> obj.bank.toJson,
      "mwst" -> obj.mwst.toJson,
      "mwstSatz" -> obj.mwstSatz.toJson,
      "mwstNr" -> obj.mwstNr.toJson,
      "aktiv" -> obj.aktiv.toJson,
      "projekt" -> obj.projekt.toJson,
      "erstelldat" -> obj.erstelldat.toJson,
      "ersteller" -> obj.ersteller.toJson,
      "modifidat" -> obj.modifidat.toJson,
      "modifikator" -> obj.modifikator.toJson,
      // additional fields
      "strasseUndNummer" -> JsString(obj.strasseUndNummer.getOrElse("")),
      "plzOrt" -> JsString(obj.plzOrt),
      "adresszeilen" -> JsArray(obj.adresszeilen.map(JsString(_)).toVector)
    )
  }

  implicit val depotAuslieferungReportFormat: RootJsonFormat[DepotAuslieferungReport] = jsonFormat11(DepotAuslieferungReport)
  implicit val tourAuslieferungReportFormat: RootJsonFormat[TourAuslieferungReport] = jsonFormat11(TourAuslieferungReport)
  implicit val postAuslieferungReportFormat: RootJsonFormat[PostAuslieferungReport] = jsonFormat10(PostAuslieferungReport)

  implicit val auslieferungReportFormat = new RootJsonFormat[AuslieferungReport] {
    def write(obj: AuslieferungReport): JsValue =
      obj match {
        case d: DepotAuslieferungReport => d.toJson
        case h: TourAuslieferungReport  => h.toJson
        case p: PostAuslieferungReport  => p.toJson
        case _                          => JsObject()
      }

    def read(json: JsValue): AuslieferungReport = {
      if (!json.asJsObject.getFields("depotId").isEmpty) {
        json.convertTo[DepotAuslieferungReport]
      } else if (!json.asJsObject.getFields("tourId").isEmpty) {
        json.convertTo[TourAuslieferungReport]
      } else {
        json.convertTo[PostAuslieferungReport]
      }
    }
  }

  implicit val depotFormat = new RootJsonFormat[Depot] {
    override def read(json: JsValue): Depot = {
      val fields = json.asJsObject.fields
      Depot(
        fields("id").convertTo[DepotId],
        fields("name").convertTo[String],
        fields("kurzzeichen").convertTo[String],
        fields("apName").convertTo[Option[String]],
        fields("apVorname").convertTo[Option[String]],
        fields("apTelefon").convertTo[Option[String]],
        fields("apEmail").convertTo[Option[String]],
        fields("vName").convertTo[Option[String]],
        fields("vVorname").convertTo[Option[String]],
        fields("vTelefon").convertTo[Option[String]],
        fields("vEmail").convertTo[Option[String]],
        fields("strasse").convertTo[Option[String]],
        fields("hausNummer").convertTo[Option[String]],
        fields("plz").convertTo[String],
        fields("ort").convertTo[String],
        fields("aktiv").convertTo[Boolean],
        fields("oeffnungszeiten").convertTo[Option[String]],
        fields("farbCode").convertTo[Option[String]],
        fields("iban").convertTo[Option[String]],
        fields("bank").convertTo[Option[String]],
        fields("beschreibung").convertTo[Option[String]],
        fields("anzahlAbonnentenMax").convertTo[Option[Int]],
        fields("anzahlAbonnenten").convertTo[Int],
        fields("anzahlAbonnentenAktiv").convertTo[Int],
        fields("erstelldat").convertTo[DateTime],
        fields("ersteller").convertTo[PersonId],
        fields("modifidat").convertTo[DateTime],
        fields("modifikator").convertTo[PersonId]
      )
    }

    override def write(obj: Depot): JsValue = JsObject(
      "id" -> obj.id.toJson,
      "name" -> obj.name.toJson,
      "kurzzeichen" -> obj.kurzzeichen.toJson,
      "apName" -> obj.apName.toJson,
      "apVorname" -> obj.apVorname.toJson,
      "apTelefon" -> obj.apTelefon.toJson,
      "apEmail" -> obj.apEmail.toJson,
      "vName" -> obj.vName.toJson,
      "vVorname" -> obj.vVorname.toJson,
      "vTelefon" -> obj.vTelefon.toJson,
      "vEmail" -> obj.vEmail.toJson,
      "strasse" -> obj.strasse.toJson,
      "hausNummer" -> obj.hausNummer.toJson,
      "plz" -> obj.plz.toJson,
      "ort" -> obj.ort.toJson,
      "aktiv" -> obj.aktiv.toJson,
      "oeffnungszeiten" -> obj.oeffnungszeiten.toJson,
      "farbCode" -> obj.farbCode.toJson,
      "iban" -> obj.iban.toJson,
      "bank" -> obj.bank.toJson,
      "beschreibung" -> obj.beschreibung.toJson,
      "anzahlAbonnentenMax" -> obj.anzahlAbonnentenMax.toJson,
      "anzahlAbonnenten" -> obj.anzahlAbonnenten.toJson,
      "anzahlAbonnentenAktiv" -> obj.anzahlAbonnentenAktiv.toJson,
      "erstelldat" -> obj.erstelldat.toJson,
      "ersteller" -> obj.ersteller.toJson,
      "modifidat" -> obj.modifidat.toJson,
      "modifikator" -> obj.modifikator.toJson
    )
  }

  implicit val personEmailDataFormat: RootJsonFormat[PersonEmailData] = jsonFormat15(PersonEmailData)
  implicit val einladungCreateFormat: RootJsonFormat[EinladungCreate] = jsonFormat5(EinladungCreate)
  implicit val personMailContextFormat: RootJsonFormat[PersonMailContext] = jsonFormat1(PersonMailContext)
  implicit val kundeMailContextFormat: RootJsonFormat[KundeMailContext] = jsonFormat2(KundeMailContext)
  implicit val aboMailContextFormat: RootJsonFormat[AboMailContext] = jsonFormat2(AboMailContext)
  implicit val abotypMailContextFormat: RootJsonFormat[AbotypMailContext] = jsonFormat2(AbotypMailContext)
  implicit val zusatzabotypMailContextFormat: RootJsonFormat[ZusatzabotypMailContext] = jsonFormat2(ZusatzabotypMailContext)
  implicit val tourMailContextFormat: RootJsonFormat[TourMailContext] = jsonFormat2(TourMailContext)
  implicit val depotMailContextFormat: RootJsonFormat[DepotMailContext] = jsonFormat2(DepotMailContext)
  implicit val lieferungDetailFormat: RootJsonFormat[LieferungDetail] = jsonFormat22(LieferungDetail)
  implicit val bestellungFormat: RootJsonFormat[Bestellung] = jsonFormat13(Bestellung)
  implicit val lieferplanungCreatedFormat: RootJsonFormat[LieferplanungCreated] = jsonFormat1(LieferplanungCreated)
  implicit val depotlieferungFormat: RootJsonFormat[Depotlieferung] = jsonFormat9(Depotlieferung)
  implicit val heimlieferungFormat: RootJsonFormat[Heimlieferung] = jsonFormat9(Heimlieferung)
  implicit val postlieferungFormat: RootJsonFormat[Postlieferung] = jsonFormat8(Postlieferung)
  implicit val produktFormat: RootJsonFormat[Produkt] = jsonFormat13(Produkt)
  implicit val produktekategorieFormat: RootJsonFormat[Produktekategorie] = jsonFormat6(Produktekategorie)
  implicit val projektVorlageFormat: RootJsonFormat[ProjektVorlage] = jsonFormat9(ProjektVorlage)
  implicit val sammelbestellungFormat: RootJsonFormat[Sammelbestellung] = jsonFormat16(Sammelbestellung)

  implicit val projektFormat = new RootJsonFormat[Projekt] {
    override def read(json: JsValue): Projekt = {
      val fields = json.asJsObject.fields
      Projekt(
        fields("id").convertTo[ProjektId],
        fields("bezeichnung").convertTo[String],
        fields("strasse").convertTo[Option[String]],
        fields("hausNummer").convertTo[Option[String]],
        fields("adressZusatz").convertTo[Option[String]],
        fields("plz").convertTo[Option[String]],
        fields("ort").convertTo[Option[String]],
        fields("preiseSichtbar").convertTo[Boolean],
        fields("preiseEditierbar").convertTo[Boolean],
        fields("emailErforderlich").convertTo[Boolean],
        fields("waehrung").convertTo[Waehrung],
        fields("geschaeftsjahrMonat").convertTo[Int],
        fields("geschaeftsjahrTag").convertTo[Int],
        fields("twoFactorAuthentication").convertTo[Map[Rolle, Boolean]],
        fields("defaultSecondFactorType").convertTo[SecondFactorType],
        fields("sprache").convertTo[Locale],
        fields("welcomeMessage1").convertTo[Option[String]],
        fields("welcomeMessage2").convertTo[Option[String]],
        fields("messageForMembers").convertTo[Option[String]],
        fields("maintenanceMode").convertTo[Boolean],
        fields("generierteMailsSenden").convertTo[Boolean],
        fields("einsatzEinheit").convertTo[EinsatzEinheit],
        fields("einsatzAbsageVorlaufTage").convertTo[Int],
        fields("einsatzShowListeKunde").convertTo[Boolean],
        fields("sendEmailToBcc").convertTo[Boolean],
        fields("erstelldat").convertTo[DateTime],
        fields("ersteller").convertTo[PersonId],
        fields("modifidat").convertTo[DateTime],
        fields("modifikator").convertTo[PersonId]
      )
    }

    override def write(obj: Projekt): JsValue = JsObject(
      "id" -> obj.id.toJson,
      "bezeichnung" -> obj.bezeichnung.toJson,
      "strasse" -> obj.strasse.toJson,
      "hausNummer" -> obj.hausNummer.toJson,
      "adressZusatz" -> obj.adressZusatz.toJson,
      "plz" -> obj.plz.toJson,
      "ort" -> obj.ort.toJson,
      "preiseSichtbar" -> obj.preiseSichtbar.toJson,
      "preiseEditierbar" -> obj.preiseEditierbar.toJson,
      "emailErforderlich" -> obj.emailErforderlich.toJson,
      "waehrung" -> obj.waehrung.toJson,
      "geschaeftsjahrMonat" -> obj.geschaeftsjahrMonat.toJson,
      "geschaeftsjahrTag" -> obj.geschaeftsjahrTag.toJson,
      "twoFactorAuthentication" -> obj.twoFactorAuthentication.toJson,
      "defaultSecondFactorType" -> obj.defaultSecondFactorType.toJson,
      "sprache" -> obj.sprache.toJson,
      "welcomeMessage1" -> obj.welcomeMessage1.toJson,
      "welcomeMessage2" -> obj.welcomeMessage2.toJson,
      "messageForMembers" -> obj.messageForMembers.toJson,
      "maintenanceMode" -> obj.maintenanceMode.toJson,
      "generierteMailsSenden" -> obj.generierteMailsSenden.toJson,
      "einsatzEinheit" -> obj.einsatzEinheit.toJson,
      "einsatzAbsageVorlaufTage" -> obj.einsatzAbsageVorlaufTage.toJson,
      "einsatzShowListeKunde" -> obj.einsatzShowListeKunde.toJson,
      "sendEmailToBcc" -> obj.sendEmailToBcc.toJson,
      "erstelldat" -> obj.erstelldat.toJson,
      "ersteller" -> obj.ersteller.toJson,
      "modifidat" -> obj.modifidat.toJson,
      "modifikator" -> obj.modifikator.toJson
    )
  }

  implicit val projektPublikFormat: RootJsonFormat[ProjektPublik] = jsonFormat18(ProjektPublik)

  implicit val produzentFormat = new RootJsonFormat[Produzent] {
    override def read(json: JsValue): Produzent = {
      val fields = json.asJsObject.fields
      Produzent(
        fields("id").convertTo[ProduzentId],
        fields("name").convertTo[String],
        fields("vorname").convertTo[Option[String]],
        fields("kurzzeichen").convertTo[String],
        fields("strasse").convertTo[Option[String]],
        fields("hausNummer").convertTo[Option[String]],
        fields("adressZusatz").convertTo[Option[String]],
        fields("plz").convertTo[String],
        fields("ort").convertTo[String],
        fields("bemerkungen").convertTo[Option[String]],
        fields("email").convertTo[String],
        fields("telefonMobil").convertTo[Option[String]],
        fields("telefonFestnetz").convertTo[Option[String]],
        fields("iban").convertTo[Option[String]],
        fields("bank").convertTo[Option[String]],
        fields("mwst").convertTo[Boolean],
        fields("mwstSatz").convertTo[Option[BigDecimal]],
        fields("mwstNr").convertTo[Option[String]],
        fields("aktiv").convertTo[Boolean],
        fields("erstelldat").convertTo[DateTime],
        fields("ersteller").convertTo[PersonId],
        fields("modifidat").convertTo[DateTime],
        fields("modifikator").convertTo[PersonId]
      )
    }

    override def write(obj: Produzent): JsValue = JsObject(
      "id" -> obj.id.toJson,
      "name" -> obj.name.toJson,
      "vorname" -> obj.vorname.toJson,
      "kurzzeichen" -> obj.kurzzeichen.toJson,
      "strasse" -> obj.strasse.toJson,
      "hausNummer" -> obj.hausNummer.toJson,
      "adressZusatz" -> obj.adressZusatz.toJson,
      "plz" -> obj.plz.toJson,
      "ort" -> obj.ort.toJson,
      "bemerkungen" -> obj.bemerkungen.toJson,
      "email" -> obj.email.toJson,
      "telefonMobil" -> obj.telefonMobil.toJson,
      "telefonFestnetz" -> obj.telefonFestnetz.toJson,
      "iban" -> obj.iban.toJson,
      "bank" -> obj.bank.toJson,
      "mwst" -> obj.mwst.toJson,
      "mwstSatz" -> obj.mwstSatz.toJson,
      "mwstNr" -> obj.mwstNr.toJson,
      "aktiv" -> obj.aktiv.toJson,
      "erstelldat" -> obj.erstelldat.toJson,
      "ersteller" -> obj.ersteller.toJson,
      "modifidat" -> obj.modifidat.toJson,
      "modifikator" -> obj.modifikator.toJson
    )
  }

  implicit val abotypMailRequestFormat: RootJsonFormat[AbotypMailRequest] = jsonFormat4(AbotypMailRequest)
  implicit val zusatzabotypMailRequestFormat: RootJsonFormat[ZusatzabotypMailRequest] = jsonFormat4(ZusatzabotypMailRequest)
  implicit val tourMailRequestFormat: RootJsonFormat[TourMailRequest] = jsonFormat4(TourMailRequest)
  implicit val depotMailRequestFormat: RootJsonFormat[DepotMailRequest] = jsonFormat4(DepotMailRequest)
  implicit val aboMailRequestFormat: RootJsonFormat[AboMailRequest] = jsonFormat4(AboMailRequest)
  implicit val kundeMailRequestFormat: RootJsonFormat[KundeMailRequest] = jsonFormat4(KundeMailRequest)
  implicit val personMailRequestFormat: RootJsonFormat[PersonMailRequest] = jsonFormat4(PersonMailRequest)
  implicit val vertriebVertriebsartenFormat: RootJsonFormat[VertriebVertriebsarten] = jsonFormat13(VertriebVertriebsarten)
  implicit val aboRechnungsPositionBisAnzahlLieferungenCreateFormat: RootJsonFormat[AboRechnungsPositionBisAnzahlLieferungenCreate] = jsonFormat5(AboRechnungsPositionBisAnzahlLieferungenCreate)
  implicit val aboRechnungsPositionBisGuthabenCreateFormat: RootJsonFormat[AboRechnungsPositionBisGuthabenCreate] = jsonFormat4(AboRechnungsPositionBisGuthabenCreate)
  implicit val lieferplanungPositionenModifyFormat: RootJsonFormat[LieferplanungPositionenModify] = jsonFormat2(LieferplanungPositionenModify)
  implicit val bestellungDetailFormat: RootJsonFormat[BestellungDetail] = jsonFormat13(BestellungDetail)
  implicit val sammelbestellungDetailFormat: RootJsonFormat[SammelbestellungDetail] = jsonFormat18(SammelbestellungDetail)
  implicit val sammelbestellungAusgeliefertFormat: RootJsonFormat[SammelbestellungAusgeliefert] = jsonFormat2(SammelbestellungAusgeliefert)
  implicit val zusatzKorbDetailFormat: RootJsonFormat[ZusatzKorbDetail] = jsonFormat13(ZusatzKorbDetail)
  implicit val korbDetailFormat: RootJsonFormat[KorbDetail] = jsonFormat14(KorbDetail)
  implicit val depotAuslieferungDetailFormat: RootJsonFormat[DepotAuslieferungDetail] = jsonFormat10(DepotAuslieferungDetail)
  implicit val tourAuslieferungDetailFormat: RootJsonFormat[TourAuslieferungDetail] = jsonFormat10(TourAuslieferungDetail)
  implicit val postAuslieferungDetailFormat: RootJsonFormat[PostAuslieferungDetail] = jsonFormat9(PostAuslieferungDetail)
  implicit val tourlieferungDetailFormat: RootJsonFormat[TourlieferungDetail] = jsonFormat19(TourlieferungDetail)
  implicit val tourDetailFormat: RootJsonFormat[TourDetail] = jsonFormat10(TourDetail)
  implicit val korbTotalCompositionFormat: RootJsonFormat[KorbTotalComposition] = jsonFormat4(KorbTotalComposition)
  implicit val korbDetailsReportProDepotTourFormat: RootJsonFormat[KorbDetailsReportProDepotTour] = jsonFormat2(KorbDetailsReportProDepotTour)
  implicit val korbUebersichtReportProDepotTourFormat: RootJsonFormat[KorbUebersichtReportProDepotTour] = jsonFormat3(KorbUebersichtReportProDepotTour)
  implicit val korbUebersichtReportProAbotypFormat: RootJsonFormat[KorbUebersichtReportProAbotyp] = jsonFormat4(KorbUebersichtReportProAbotyp)
  implicit val korbUebersichtReportProZusatzabotypFormat: RootJsonFormat[KorbUebersichtReportProZusatzabotyp] = jsonFormat3(KorbUebersichtReportProZusatzabotyp)
  implicit val auslieferungKorbUebersichtReportFormat: RootJsonFormat[AuslieferungKorbUebersichtReport] = jsonFormat6(AuslieferungKorbUebersichtReport)
  implicit val korbDetailsReportProAbotypFormat: RootJsonFormat[KorbDetailsReportProAbotyp] = jsonFormat4(KorbDetailsReportProAbotyp)
  implicit val auslieferungKorbDetailsReportFormat: RootJsonFormat[AuslieferungKorbDetailsReport] = jsonFormat4(AuslieferungKorbDetailsReport)
  implicit val lieferplanungReportFormat: RootJsonFormat[LieferplanungReport] = jsonFormat6(LieferplanungReport)
  implicit val auslieferungReportEntryFormat: RootJsonFormat[AuslieferungReportEntry] = jsonFormat7(AuslieferungReportEntry)
  implicit val produzentenabrechnungReportFormat: RootJsonFormat[ProduzentenabrechnungReport] = jsonFormat9(ProduzentenabrechnungReport)

  implicit def multiReportFormat[T <% JSONSerializable](implicit format: RootJsonFormat[T]): RootJsonFormat[MultiReport[T]] = jsonFormat3(MultiReport.apply[T])

  implicit val geschaeftsjahrStartFormat = jsonFormat3(GeschaeftsjahrStart)

  implicit val kundenSearchFormat = jsonFormat3(KundenSearch)

  // event formats
  implicit val lieferplanungAbschliessenEventFormat: RootJsonFormat[LieferplanungAbschliessenEvent] = jsonFormat2(LieferplanungAbschliessenEvent)
  implicit val lieferplanungAbrechnenEventFormat: RootJsonFormat[LieferplanungAbrechnenEvent] = jsonFormat2(LieferplanungAbrechnenEvent)
  implicit val lieferplanungDataModifiedEventFormat: RootJsonFormat[LieferplanungDataModifiedEvent] = jsonFormat2(LieferplanungDataModifiedEvent)
  implicit val abwesenheitCreateEventFormat: RootJsonFormat[AbwesenheitCreateEvent] = jsonFormat3(AbwesenheitCreateEvent)
  implicit val sammelbestellungVersendenEventFormat: RootJsonFormat[SammelbestellungVersendenEvent] = jsonFormat2(SammelbestellungVersendenEvent)
  implicit val passwortGewechseltEventFormat: RootJsonFormat[PasswortGewechseltEvent] = jsonFormat4(PasswortGewechseltEvent)
  implicit val loginDeaktiviertEventFormat: RootJsonFormat[LoginDeaktiviertEvent] = jsonFormat3(LoginDeaktiviertEvent)
  implicit val loginAktiviertEventFormat: RootJsonFormat[LoginAktiviertEvent] = jsonFormat3(LoginAktiviertEvent)
  implicit val auslieferungAlsAusgeliefertMarkierenEventFormat: RootJsonFormat[AuslieferungAlsAusgeliefertMarkierenEvent] = jsonFormat2(AuslieferungAlsAusgeliefertMarkierenEvent)
  implicit val sammelbestellungAlsAbgerechnetMarkierenEventFormat: RootJsonFormat[SammelbestellungAlsAbgerechnetMarkierenEvent] = jsonFormat3(SammelbestellungAlsAbgerechnetMarkierenEvent)
  implicit val einladungGesendetEventFormat: RootJsonFormat[EinladungGesendetEvent] = jsonFormat2(EinladungGesendetEvent)
  implicit val passwortResetGesendetEventFormat: RootJsonFormat[PasswortResetGesendetEvent] = jsonFormat2(PasswortResetGesendetEvent)
  implicit val rolleGewechseltEventFormat: RootJsonFormat[RolleGewechseltEvent] = jsonFormat4(RolleGewechseltEvent)
  implicit val sendEmailToPersonEventFormat: RootJsonFormat[SendEmailToPersonEvent] = jsonFormat5(SendEmailToPersonEvent)
  implicit val sendEmailToKundeEventFormat: RootJsonFormat[SendEmailToKundeEvent] = jsonFormat5(SendEmailToKundeEvent)
  implicit val sendEmailToAboSubscriberEventFormat: RootJsonFormat[SendEmailToAboSubscriberEvent] = jsonFormat5(SendEmailToAboSubscriberEvent)
  implicit val sendEmailToAbotypSubscriberEventFormat: RootJsonFormat[SendEmailToAbotypSubscriberEvent] = jsonFormat5(SendEmailToAbotypSubscriberEvent)
  implicit val sendEmailToZusatzabotypSubscriberEventFormat: RootJsonFormat[SendEmailToZusatzabotypSubscriberEvent] = jsonFormat5(SendEmailToZusatzabotypSubscriberEvent)
  implicit val sendEmailToTourSubscriberEventFormat: RootJsonFormat[SendEmailToTourSubscriberEvent] = jsonFormat5(SendEmailToTourSubscriberEvent)
  implicit val sendEmailToDepotSubscriberEventFormat: RootJsonFormat[SendEmailToDepotSubscriberEvent] = jsonFormat5(SendEmailToDepotSubscriberEvent)
  implicit val aboAktiviertEventFormat: RootJsonFormat[AboAktiviertEvent] = jsonFormat2(AboAktiviertEvent)
  implicit val aboDeaktiviertEventFormat: RootJsonFormat[AboDeaktiviertEvent] = jsonFormat2(AboDeaktiviertEvent)
  implicit val otpResetEventFormat: RootJsonFormat[OtpResetEvent] = jsonFormat4(OtpResetEvent)
}
