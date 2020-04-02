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

package ch.openolitor.buchhaltung.rechnungsexport.iso20022

import ch.openolitor.buchhaltung.models.Rechnung
import ch.openolitor.generated.xsd.camt054_001_04._
import ch.openolitor.generated.xsd.pain008_001_02
import ch.openolitor.generated.xsd.pain008_001_02._
import ch.openolitor.stammdaten.models.{ KontoDaten, Projekt }
import com.typesafe.scalalogging.LazyLogging
import javax.xml.datatype.{ DatatypeConstants, DatatypeFactory, XMLGregorianCalendar }
import scalaxb.DataRecord
import java.util.GregorianCalendar

import scala.xml.{ NamespaceBinding, TopScope }

class Pain008_001_02_Export extends LazyLogging {

  private def exportPain008_001_02(rechnungen: List[(Rechnung, KontoDaten)], kontoDatenProjekt: KontoDaten, NbOfTxs: String, projekt: Projekt): String = {

    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
      scalaxb.toXML[ch.openolitor.generated.xsd.pain008_001_02.Document](ch.openolitor.generated.xsd.pain008_001_02.Document(
        CustomerDirectDebitInitiationV02(
          getGroupHeaderSDD(rechnungen.map(_._1), kontoDatenProjekt, NbOfTxs, projekt),
          Seq(getPaymentInstructionInformationSDD(projekt, kontoDatenProjekt, rechnungen, NbOfTxs))
        )
      ), "Document", defineNamespaceBinding()).toString()
  }

  private def getDate(): XMLGregorianCalendar = {
    val calendar = new GregorianCalendar();
    calendar.getTime
    val date = DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar)
    date.setTime(DatatypeConstants.FIELD_UNDEFINED, DatatypeConstants.FIELD_UNDEFINED, DatatypeConstants.FIELD_UNDEFINED, DatatypeConstants.FIELD_UNDEFINED)
    date
  }

  private def getDateTime(): XMLGregorianCalendar = {
    val calendar = new GregorianCalendar();
    calendar.getTime
    DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar)
  }

  private def defineNamespaceBinding(): NamespaceBinding = {
    val nsb2 = NamespaceBinding("schemaLocation", "urn:iso:std:iso:20022:tech:xsd:pain.008.001.02.xsdpain.008.001.02", TopScope)
    val nsb3 = NamespaceBinding("xsi", "http://www.w3.org/2001/XMLSchema-instance", nsb2)
    NamespaceBinding(null, "urn:iso:std:iso:20022:tech:xsd:pain.008.001.02", nsb3)
  }

  private def getGroupHeaderSDD(rechnungen: List[Rechnung], kontoDatenProjekt: KontoDaten, nbTransactions: String, projekt: Projekt): GroupHeaderSDD = {
    val MsgId = kontoDatenProjekt.iban.get.slice(0, 15) + getSimpleDateTimeString(getDateTime())
    val CreDtTm = getDateTime
    val NbOfTxs = nbTransactions
    val CtrlSum = getSumAllRechnungs(rechnungen)
    val partyIdentificationSEPA1 = pain008_001_02.PartyIdentificationSEPA1(Some(projekt.bezeichnung), None)

    GroupHeaderSDD(MsgId, CreDtTm, NbOfTxs, CtrlSum, partyIdentificationSEPA1)
  }

  private def getPaymentInstructionInformationSDD(projekt: Projekt, kontoDatenProjekt: KontoDaten, rechnungen: List[(Rechnung, KontoDaten)], transactionNumber: String): PaymentInstructionInformationSDD = {
    val PmtInfId = kontoDatenProjekt.iban.slice(0, 15).mkString.concat(getSimpleDateTimeString(getDateTime()))
    val PmtMtd = DD
    val BtchBookg = None
    val NbOfTxs = transactionNumber
    val CtrlSum = getSumAllRechnungs(rechnungen.map(_._1))
    val PmtTpInf = Some(PaymentTypeInformationSDD(
      ServiceLevel("SEPA"),
      LocalInstrumentSEPA("Core"),
      SequenceType1Code.fromString("FRST", defineNamespaceBinding()),
      None
    ))
    val ReqdColltnDt = getDate()
    val Cdtr = pain008_001_02.PartyIdentificationSEPA5(projekt.bezeichnung, None)
    val CdtrAcct = pain008_001_02.CashAccountSEPA1(pain008_001_02.AccountIdentificationSEPA(kontoDatenProjekt.iban.getOrElse("iban from CSA")))
    val CdtrAgt = pain008_001_02.BranchAndFinancialInstitutionIdentificationSEPA3(pain008_001_02.FinancialInstitutionIdentificationSEPA3(DataRecord[String](None, Some("BIC"), "NOTPROVIDED")))
    val UltmtCdtr = None
    val ChrgBr = Some(pain008_001_02.SLEV)
    //-----------------
    val PartySEPA2 = pain008_001_02.PartySEPA2(pain008_001_02.PersonIdentificationSEPA2(pain008_001_02.RestrictedPersonIdentificationSEPA(kontoDatenProjekt.iban.getOrElse("Iban subscriptor"), pain008_001_02.RestrictedPersonIdentificationSchemeNameSEPA(IdentificationSchemeNameSEPA.fromString("SEPA", defineNamespaceBinding())))))
    val CdtrSchmeId = Some(pain008_001_02.PartyIdentificationSEPA3(PartySEPA2))
    //-----------------
    val DrctDbtTxInf = rechnungen map {
      case (rechnung, kontoDaten) =>
        getDirectDebitTransactionInformation(kontoDaten.iban.getOrElse("Iban subscriptor"), kontoDaten.nameAccountHolder.getOrElse("accountHolder subscriptor"), rechnung)
    }

    PaymentInstructionInformationSDD(PmtInfId, PmtMtd, BtchBookg, NbOfTxs, CtrlSum,
      PmtTpInf, ReqdColltnDt, Cdtr, CdtrAcct, CdtrAgt, UltmtCdtr,
      ChrgBr, CdtrSchmeId, DrctDbtTxInf)
  }

  private def getDirectDebitTransactionInformation(iban: String, nameAccountHolder: String, rechnung: Rechnung): DirectDebitTransactionInformationSDD = {
    val PmtId = PaymentIdentificationSEPA(None, "NOTPROVIDED")
    val PmtTpInf = None
    val InstdAmt = pain008_001_02.ActiveOrHistoricCurrencyAndAmountSEPA(rechnung.betrag, Map[String, DataRecord[String]]("Ccy" -> DataRecord(None, Some("Ccy"), "EUR")))
    val ChrgBr = None
    val DrctDbtTx = DirectDebitTransactionSDD(MandateRelatedInformationSDD(rechnung.kundeId.id.toString, getDate(), None, None, None), None)
    val UltmtCdtr = None
    val DbtrAgt = pain008_001_02.BranchAndFinancialInstitutionIdentificationSEPA3(pain008_001_02.FinancialInstitutionIdentificationSEPA3(DataRecord[String](None, Some("BIC"), "NOTPROVIDED")))
    val DbtrAgtAcct = None
    val Dbtr = pain008_001_02.PartyIdentificationSEPA2(nameAccountHolder, None)
    val DbtrAcct = pain008_001_02.CashAccountSEPA2(pain008_001_02.AccountIdentificationSEPA(iban))
    val UltmtDbtr = None
    val Purp = None
    val RmtInf = Option(pain008_001_02.RemittanceInformationSEPA1Choice(DataRecord(None, Some("Ustrd"), s"${rechnung.referenzNummer}${rechnung.titel}")))

    DirectDebitTransactionInformationSDD(
      PmtId, PmtTpInf, InstdAmt, ChrgBr, DrctDbtTx, UltmtCdtr, DbtrAgt,
      Dbtr, DbtrAcct, UltmtDbtr, Purp, RmtInf
    )
  }

  private def getSimpleDateTimeString(date: XMLGregorianCalendar): String = {
    date.getYear.toString +
      date.getMonth.toString +
      date.getDay.toString +
      date.getHour.toString +
      date.getMinute.toString +
      date.getSecond.toString
  }

  private def getSumAllRechnungs(rechnungen: List[Rechnung]): BigDecimal = {
    rechnungen.map(_.betrag).sum
  }
}

object Pain008_001_02_Export {
  def exportPain008_001_02(rechnungen: List[(Rechnung, KontoDaten)], kontoDatenProjekt: KontoDaten, NbOfTxs: String, projekt: Projekt): String = {
    new Pain008_001_02_Export().exportPain008_001_02(rechnungen, kontoDatenProjekt, NbOfTxs, projekt)
  }
}

