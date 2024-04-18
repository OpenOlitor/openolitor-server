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

import ch.openolitor.buchhaltung.models.{ RechnungenContainer, RechnungsPositionenCreateRechnungen, _ }
import ch.openolitor.buchhaltung.BuchhaltungCommandHandler.{ MahnungPDFStoredEvent, RechnungBezahltEvent, RechnungDeleteEvent, RechnungMahnungVerschicktEvent, RechnungPDFStoredEvent, RechnungStorniertEvent, RechnungVerschicktEvent, SendEmailToInvoiceSubscribersEvent, ZahlungsEingangErledigtEvent, ZahlungsEingangIgnoreEvent, ZahlungsExportCreatedEvent, ZahlungsImportCreatedEvent }
import ch.openolitor.core.BaseJsonProtocol
import ch.openolitor.core.models.PersonId
import ch.openolitor.stammdaten.StammdatenJsonProtocol
import ch.openolitor.stammdaten.models._
import com.typesafe.scalalogging.LazyLogging
import org.joda.time.DateTime
import spray.json._
import spray.json.DefaultJsonProtocol.immSeqFormat

/**
 * JSON Format deklarationen für das Modul Buchhaltung
 */
trait BuchhaltungJsonProtocol extends BaseJsonProtocol with LazyLogging with StammdatenJsonProtocol {

  implicit val rechnungStatusFormat = enumFormat(RechnungStatus.apply)
  implicit val rechnungsPositionStatusFormat = enumFormat(RechnungsPositionStatus.apply)
  implicit val rechnungsPositionTypFormat = enumFormat(RechnungsPositionTyp.apply)
  implicit val zahlungsEingangStatusFormat = enumFormat(ZahlungsEingangStatus.apply)
  implicit val zahlungsExportStatusFormat = enumFormat(ZahlungsExportStatus.apply)

  //id formats
  implicit val rechnungIdFormat = baseIdFormat(RechnungId)
  implicit val rechnungsPositionIdFormat = baseIdFormat(RechnungsPositionId)
  implicit val zahlungsImportIdFormat = baseIdFormat(ZahlungsImportId)
  implicit val zahlungsEingangIdFormat = baseIdFormat(ZahlungsEingangId)
  implicit val zahlungsExportIdFormat = baseIdFormat(ZahlungsExportId)

  implicit val kontoDatenFormat = jsonFormat16(KontoDaten)
  implicit val rechnungsPositionFormat = jsonFormat16(RechnungsPosition)
  implicit val rechnungsPositionDetailFormat = jsonFormat14(RechnungsPositionDetail)
  implicit val rechnungCreateFromRechnungsPositionenFormat = jsonFormat13(RechnungCreateFromRechnungsPositionen)
  implicit val rechnungModifyFormat = jsonFormat13(RechnungModify)
  implicit val rechnungsPositionCreateFormat = jsonFormat9(RechnungsPositionCreate)
  implicit val rechnungsPositionModifyFormat = jsonFormat4(RechnungsPositionModify)
  implicit val rechnungsPositionAssignToRechnungFormat = jsonFormat2(RechnungsPositionAssignToRechnung)

  implicit val rechnungFormat = new RootJsonFormat[Rechnung] {
    override def read(json: JsValue): Rechnung = {
      val fields = json.asJsObject.fields
      Rechnung(
        fields("id").convertTo[RechnungId],
        fields("kundeId").convertTo[KundeId],
        fields("titel").convertTo[String],
        fields("waehrung").convertTo[Waehrung],
        fields("betrag").convertTo[BigDecimal],
        fields.get("einbezahlterBetrag").fold(Option.empty[BigDecimal])(_.convertTo[Option[BigDecimal]]),
        fields("rechnungsDatum").convertTo[DateTime],
        fields("faelligkeitsDatum").convertTo[DateTime],
        fields.get("eingangsDatum").fold(Option.empty[DateTime])(_.convertTo[Option[DateTime]]),
        fields("status").convertTo[RechnungStatus],
        fields("referenzNummer").convertTo[String],
        fields("esrNummer").convertTo[String],
        fields.get("fileStoreId").fold(Option.empty[String])(_.convertTo[Option[String]]),
        fields("anzahlMahnungen").convertTo[Int],
        fields.get("mahnungFileStoreIds").fold(Set.empty[String])(_.convertTo[Set[String]]),
        fields("strasse").convertTo[String],
        fields.get("hausNummer").fold(Option.empty[String])(_.convertTo[Option[String]]),
        fields.get("adressZusatz").fold(Option.empty[String])(_.convertTo[Option[String]]),
        fields("plz").convertTo[String],
        fields("ort").convertTo[String],
        fields.get("paymentType").fold(Option.empty[PaymentType])(_.convertTo[Option[PaymentType]]),
        fields("erstelldat").convertTo[DateTime],
        fields("ersteller").convertTo[PersonId],
        fields("modifidat").convertTo[DateTime],
        fields("modifikator").convertTo[PersonId]
      )
    }

    override def write(obj: Rechnung): JsValue = JsObject(
      Map(
        "id" -> obj.id.toJson,
        "kundeId" -> obj.kundeId.toJson,
        "titel" -> obj.titel.toJson,
        "waehrung" -> obj.waehrung.toJson,
        "betrag" -> obj.betrag.toJson,
        "einbezahlterBetrag" -> obj.einbezahlterBetrag.toJson,
        "rechnungsDatum" -> obj.rechnungsDatum.toJson,
        "faelligkeitsDatum" -> obj.faelligkeitsDatum.toJson,
        "eingangsDatum" -> obj.eingangsDatum.toJson,
        "status" -> obj.status.toJson,
        "referenzNummer" -> obj.referenzNummer.toJson,
        "esrNummer" -> obj.esrNummer.toJson,
        "fileStoreId" -> obj.fileStoreId.toJson,
        "anzahlMahnungen" -> obj.anzahlMahnungen.toJson,
        "mahnungFileStoreIds" -> obj.mahnungFileStoreIds.toJson,
        "strasse" -> obj.strasse.toJson,
        "hausNummer" -> obj.hausNummer.toJson,
        "adressZusatz" -> obj.adressZusatz.toJson,
        "plz" -> obj.plz.toJson,
        "ort" -> obj.ort.toJson,
        "paymentType" -> obj.paymentType.toJson,
        "erstelldat" -> obj.erstelldat.toJson,
        "ersteller" -> obj.ersteller.toJson,
        "modifidat" -> obj.modifidat.toJson,
        "modifikator" -> obj.modifikator.toJson
      ).filterNot(_._2 == JsNull)
    )
  }

  implicit val rechnungDetailFormat = new RootJsonFormat[RechnungDetail] {
    override def read(json: JsValue): RechnungDetail = {
      val fields = json.asJsObject.fields
      RechnungDetail(
        fields("id").convertTo[RechnungId],
        fields("kunde").convertTo[Kunde],
        fields("titel").convertTo[String],
        fields("waehrung").convertTo[Waehrung],
        fields("betrag").convertTo[BigDecimal],
        fields.get("rechnungsPositionen").fold(Seq.empty[RechnungsPositionDetail])(_.convertTo[Seq[RechnungsPositionDetail]]),
        fields.get("einbezahlterBetrag").fold(Option.empty[BigDecimal])(_.convertTo[Option[BigDecimal]]),
        fields("rechnungsDatum").convertTo[DateTime],
        fields("faelligkeitsDatum").convertTo[DateTime],
        fields.get("eingangsDatum").fold(Option.empty[DateTime])(_.convertTo[Option[DateTime]]),
        fields("status").convertTo[RechnungStatus],
        fields("referenzNummer").convertTo[String],
        fields("esrNummer").convertTo[String],
        fields.get("fileStoreId").fold(Option.empty[String])(_.convertTo[Option[String]]),
        fields("anzahlMahnungen").convertTo[Int],
        fields.get("mahnungFileStoreIds").fold(Set.empty[String])(_.convertTo[Set[String]]),
        fields("strasse").convertTo[String],
        fields.get("hausNummer").fold(Option.empty[String])(_.convertTo[Option[String]]),
        fields.get("adressZusatz").fold(Option.empty[String])(_.convertTo[Option[String]]),
        fields("plz").convertTo[String],
        fields("ort").convertTo[String],
        fields.get("paymentType").fold(Option.empty[PaymentType])(_.convertTo[Option[PaymentType]]),
        fields("kundeKontoDaten").convertTo[KontoDaten],
        fields("erstelldat").convertTo[DateTime],
        fields("ersteller").convertTo[PersonId],
        fields("modifidat").convertTo[DateTime],
        fields("modifikator").convertTo[PersonId]
      )
    }

    override def write(obj: RechnungDetail): JsValue = JsObject(
      Map(
        "id" -> obj.id.toJson,
        "kunde" -> obj.kunde.toJson,
        "titel" -> obj.titel.toJson,
        "waehrung" -> obj.waehrung.toJson,
        "betrag" -> obj.betrag.toJson,
        "rechnungsPositionen" -> obj.rechnungsPositionen.toJson,
        "einbezahlterBetrag" -> obj.einbezahlterBetrag.toJson,
        "rechnungsDatum" -> obj.rechnungsDatum.toJson,
        "faelligkeitsDatum" -> obj.faelligkeitsDatum.toJson,
        "eingangsDatum" -> obj.eingangsDatum.toJson,
        "status" -> obj.status.toJson,
        "referenzNummer" -> obj.referenzNummer.toJson,
        "esrNummer" -> obj.esrNummer.toJson,
        "fileStoreId" -> obj.fileStoreId.toJson,
        "anzahlMahnungen" -> obj.anzahlMahnungen.toJson,
        "mahnungFileStoreIds" -> obj.mahnungFileStoreIds.toJson,
        "strasse" -> obj.strasse.toJson,
        "hausNummer" -> obj.hausNummer.toJson,
        "adressZusatz" -> obj.adressZusatz.toJson,
        "plz" -> obj.plz.toJson,
        "ort" -> obj.ort.toJson,
        "paymentType" -> obj.paymentType.toJson,
        "kundeKontoDaten" -> obj.kundeKontoDaten.toJson,
        "erstelldat" -> obj.erstelldat.toJson,
        "ersteller" -> obj.ersteller.toJson,
        "modifidat" -> obj.modifidat.toJson,
        "modifikator" -> obj.modifikator.toJson
      ).filterNot(_._2 == JsNull)
    )
  }

  private implicit val rechnungDetailReportFormat = new RootJsonFormat[RechnungDetailReport] {
    override def read(json: JsValue): RechnungDetailReport = {
      val fields = json.asJsObject.fields
      RechnungDetailReport(
        fields("id").convertTo[RechnungId],
        fields("kunde").convertTo[Kunde],
        fields("kontoDaten").convertTo[KontoDaten],
        fields("titel").convertTo[String],
        fields("waehrung").convertTo[Waehrung],
        fields("betrag").convertTo[BigDecimal],
        fields.get("rechnungsPositionen").fold(Seq.empty[RechnungsPositionDetail])(_.convertTo[Seq[RechnungsPositionDetail]]),
        fields.get("einbezahlterBetrag").fold(Option.empty[BigDecimal])(_.convertTo[Option[BigDecimal]]),
        fields("rechnungsDatum").convertTo[DateTime],
        fields("faelligkeitsDatum").convertTo[DateTime],
        fields.get("eingangsDatum").fold(Option.empty[DateTime])(_.convertTo[Option[DateTime]]),
        fields("status").convertTo[RechnungStatus],
        fields("referenzNummer").convertTo[String],
        fields("esrNummer").convertTo[String],
        fields("anzahlMahnungen").convertTo[Int],
        fields("strasse").convertTo[String],
        fields.get("hausNummer").fold(Option.empty[String])(_.convertTo[Option[String]]),
        fields.get("adressZusatz").fold(Option.empty[String])(_.convertTo[Option[String]]),
        fields("plz").convertTo[String],
        fields("ort").convertTo[String],
        fields.get("paymentType").fold(Option.empty[PaymentType])(_.convertTo[Option[PaymentType]]),
        fields.get("qrCode").fold(Option.empty[String])(_.convertTo[Option[String]]),
        fields("kundeKontoDaten").convertTo[KontoDaten],
        fields("erstelldat").convertTo[DateTime],
        fields("ersteller").convertTo[PersonId],
        fields("modifidat").convertTo[DateTime],
        fields("modifikator").convertTo[PersonId],
        fields("projekt").convertTo[ProjektReport]
      )
    }

    override def write(obj: RechnungDetailReport): JsValue = JsObject(
      Map(
        "id" -> obj.id.toJson,
        "kunde" -> obj.kunde.toJson,
        "kontoDaten" -> obj.kontoDaten.toJson,
        "titel" -> obj.titel.toJson,
        "waehrung" -> obj.waehrung.toJson,
        "betrag" -> obj.betrag.toJson,
        "rechnungsPositionen" -> obj.rechnungsPositionen.toJson,
        "einbezahlterBetrag" -> obj.einbezahlterBetrag.toJson,
        "rechnungsDatum" -> obj.rechnungsDatum.toJson,
        "faelligkeitsDatum" -> obj.faelligkeitsDatum.toJson,
        "eingangsDatum" -> obj.eingangsDatum.toJson,
        "status" -> obj.status.toJson,
        "referenzNummer" -> obj.referenzNummer.toJson,
        "esrNummer" -> obj.esrNummer.toJson,
        "anzahlMahnungen" -> obj.anzahlMahnungen.toJson,
        // rechnungsadresse
        "strasse" -> obj.strasse.toJson,
        "hausNummer" -> obj.hausNummer.toJson,
        "adressZusatz" -> obj.adressZusatz.toJson,
        "plz" -> obj.plz.toJson,
        "ort" -> obj.ort.toJson,
        "paymentType" -> obj.paymentType.toJson,
        "qrCode" -> obj.qrCode.toJson,
        "kundeKontoDaten" -> obj.kundeKontoDaten.toJson,
        "erstelldat" -> obj.erstelldat.toJson,
        "ersteller" -> obj.ersteller.toJson,
        "modifidat" -> obj.modifidat.toJson,
        "modifikator" -> obj.modifikator.toJson,
        "projekt" -> obj.projekt.toJson
      ).filterNot(_._2 == JsNull)
    )
  }

  // special report formats
  def enhancedRechnungDetailFormatDef(implicit defaultFormat: JsonFormat[RechnungDetailReport]): RootJsonFormat[RechnungDetailReport] = new RootJsonFormat[RechnungDetailReport] {
    def write(obj: RechnungDetailReport): JsValue = {
      JsObject(defaultFormat.write(obj)
        .asJsObject.fields ++
        Map(
          "referenzNummerFormatiert" -> JsString(obj.referenzNummerFormatiert),
          "betragRappen" -> JsNumber(obj.betragRappen),
          "betragFranken" -> JsNumber(obj.betragFranken)
        ))
    }

    def read(json: JsValue): RechnungDetailReport = defaultFormat.read(json)
  }

  implicit val enhancedRechnungDetailFormat = enhancedRechnungDetailFormatDef

  implicit val rechnungModifyBezahltFormat = jsonFormat2(RechnungModifyBezahlt)
  implicit val zahlungsEingangCreateFormat = jsonFormat16(ZahlungsEingangCreate)

  implicit val zahlungsEingangFormat = new RootJsonFormat[ZahlungsEingang] {
    override def read(json: JsValue): ZahlungsEingang = {
      val fields = json.asJsObject.fields
      ZahlungsEingang(
        fields("id").convertTo[ZahlungsEingangId],
        fields("zahlungsImportId").convertTo[ZahlungsImportId],
        fields.get("rechnungId").fold(Option.empty[RechnungId])(_.convertTo[Option[RechnungId]]),
        fields.get("kundeBezeichnung").fold(Option.empty[String])(_.convertTo[Option[String]]),
        fields.get("kundeId").fold(Option.empty[KundeId])(_.convertTo[Option[KundeId]]),
        fields("transaktionsart").convertTo[String],
        fields.get("teilnehmerNummer").fold(Option.empty[String])(_.convertTo[Option[String]]),
        fields.get("iban").fold(Option.empty[String])(_.convertTo[Option[String]]),
        fields("referenzNummer").convertTo[String],
        fields("waehrung").convertTo[Waehrung],
        fields("betrag").convertTo[BigDecimal],
        fields("aufgabeDatum").convertTo[DateTime],
        fields("verarbeitungsDatum").convertTo[DateTime],
        fields("gutschriftsDatum").convertTo[DateTime],
        fields.get("debitor").fold(Option.empty[String])(_.convertTo[Option[String]]),
        fields("status").convertTo[ZahlungsEingangStatus],
        fields("erledigt").convertTo[Boolean],
        fields.get("bemerkung").fold(Option.empty[String])(_.convertTo[Option[String]]),
        fields("erstelldat").convertTo[DateTime],
        fields("ersteller").convertTo[PersonId],
        fields("modifidat").convertTo[DateTime],
        fields("modifikator").convertTo[PersonId]
      )
    }

    override def write(obj: ZahlungsEingang): JsValue = JsObject(
      Map(
        "id" -> obj.id.toJson,
        "zahlungsImportId" -> obj.zahlungsImportId.toJson,
        "rechnungId" -> obj.rechnungId.toJson,
        "kundeBezeichnung" -> obj.kundeBezeichnung.toJson,
        "kundeId" -> obj.kundeId.toJson,
        "transaktionsart" -> obj.transaktionsart.toJson,
        "teilnehmerNummer" -> obj.teilnehmerNummer.toJson,
        "iban" -> obj.iban.toJson,
        "referenzNummer" -> obj.referenzNummer.toJson,
        "waehrung" -> obj.waehrung.toJson,
        "betrag" -> obj.betrag.toJson,
        "aufgabeDatum" -> obj.aufgabeDatum.toJson,
        "verarbeitungsDatum" -> obj.verarbeitungsDatum.toJson,
        "gutschriftsDatum" -> obj.gutschriftsDatum.toJson,
        "debitor" -> obj.debitor.toJson,
        "status" -> obj.status.toJson,
        "erledigt" -> obj.erledigt.toJson,
        "bemerkung" -> obj.bemerkung.toJson,
        "erstelldat" -> obj.erstelldat.toJson,
        "ersteller" -> obj.ersteller.toJson,
        "modifidat" -> obj.modifidat.toJson,
        "modifikator" -> obj.modifikator.toJson
      ).filterNot(_._2 == JsNull)
    )
  }

  implicit val zahlungsImportCreateFormat = jsonFormat3(ZahlungsImportCreate)
  implicit val zahlungsImportFormat = jsonFormat8(ZahlungsImport)
  implicit val zahlungsImportDetailFormat = jsonFormat7(ZahlungsImportDetail)
  implicit val zahlungsEingangModifyErledigtFormat = jsonFormat2(ZahlungsEingangModifyErledigt)
  implicit val zahlungsExportCreateFormat = jsonFormat4(ZahlungsExportCreate)
  implicit val zahlungsExportFormat = jsonFormat8(ZahlungsExport)
  implicit val rechnungMailContextFormat = jsonFormat2(RechnungMailContext)
  implicit val rechnungMailRequestFormat = jsonFormat5(RechnungMailRequest)
  implicit val rechnungenContainerFormat = jsonFormat1(RechnungenContainer)
  implicit val rechnungenDownloadContainerFormat = jsonFormat2(RechnungenDownloadContainer)
  implicit val rechnungsPositionenCreateRechnungenFormat = jsonFormat4(RechnungsPositionenCreateRechnungen)

  // event formats
  implicit val rechnungVerschicktEventFormat = jsonFormat2(RechnungVerschicktEvent)
  implicit val rechnungMahnungVerschicktEventFormat = jsonFormat2(RechnungMahnungVerschicktEvent)
  implicit val rechnungBezahltEventFormat = jsonFormat3(RechnungBezahltEvent)
  implicit val rechnungStorniertEventFormat = jsonFormat2(RechnungStorniertEvent)
  implicit val rechnungDeleteEventFormat = jsonFormat2(RechnungDeleteEvent)
  implicit val zahlungsImportCreatedEventFormat = jsonFormat2(ZahlungsImportCreatedEvent)
  implicit val zahlungsEingangErledigtEventFormat = jsonFormat2(ZahlungsEingangErledigtEvent)
  implicit val zahlungsExportCreatedEventFormat = jsonFormat2(ZahlungsExportCreatedEvent)
  implicit val rechnungPDFStoredEventFormat = jsonFormat3(RechnungPDFStoredEvent)
  implicit val mahnungPDFStoredEventFormat = jsonFormat3(MahnungPDFStoredEvent)
  implicit val sendEmailToInvoiceSubscribersEventFormat = jsonFormat6(SendEmailToInvoiceSubscribersEvent)
  implicit val zahlungsEingangIgnoreEventFormat = jsonFormat2(ZahlungsEingangIgnoreEvent)
}
