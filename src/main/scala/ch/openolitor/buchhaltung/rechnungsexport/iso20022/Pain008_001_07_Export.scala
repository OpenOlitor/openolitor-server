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

import ch.openolitor.generated.xsd.pain008_001_07._
import ch.openolitor.generated.xsd.pain008_001_07
import ch.openolitor.generated.xsd.camt054_001_04._
import ch.openolitor.buchhaltung.models.Rechnung
import ch.openolitor.stammdaten.models.KontoDaten

import scala.xml.TopScope
import scala.xml.NamespaceBinding
import javax.xml.datatype.XMLGregorianCalendar

import scalaxb.DataRecord
import java.util.GregorianCalendar
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.DatatypeConstants

import com.typesafe.scalalogging.LazyLogging

class Pain008_001_07_Export extends LazyLogging {

  private def exportPain008_001_07(rechnungen: List[(Rechnung, KontoDaten)], kontoDatenProjekt: KontoDaten, NbOfTxs: String): String = {
    val paymentInstructionInformationSDD = rechnungen map { rechnung =>
      getPaymentInstructionInformationSDD(rechnung._1, rechnung._2, kontoDatenProjekt, NbOfTxs)
    }

    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
      scalaxb.toXML[ch.openolitor.generated.xsd.pain008_001_07.Document](ch.openolitor.generated.xsd.pain008_001_07.Document(CustomerDirectDebitInitiationV07(getGroupHeaderSDD(rechnungen.map(_._1), kontoDatenProjekt, NbOfTxs), paymentInstructionInformationSDD)), "Document", defineNamespaceBinding()).toString()
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
    val nsb2 = NamespaceBinding("schemaLocation", "urn:iso:std:iso:20022:tech:xsd:pain.008.001.07.xsd pain.008.001.07", TopScope)
    val nsb3 = NamespaceBinding("xsi", "http://www.w3.org/2001/XMLSchema-instance", nsb2)
    NamespaceBinding(null, "urn:iso:std:iso:20022:tech:xsd:pain.008.001.07", nsb3)
  }

  private def getGroupHeaderSDD(rechnungen: List[Rechnung], kontoDatenProjekt: KontoDaten, nbTransactions: String): GroupHeader55 = {
    val MsgId = kontoDatenProjekt.iban.get.slice(0, 15) + getSimpleDateTimeString(getDateTime())
    val CreDtTm = getDateTime
    val NbOfTxs = nbTransactions
    val CtrlSum = getSumAllRechnungs(rechnungen)
    val partyIdentification43 = kontoDatenProjekt.creditorIdentifier.getOrElse("Initiator")

    GroupHeader55(MsgId, CreDtTm, Seq(), NbOfTxs, Some(CtrlSum), pain008_001_07.PartyIdentification43(Some(partyIdentification43)))
  }

  private def getPaymentInstructionInformationSDD(rechnung: Rechnung, kontoDatenKunde: KontoDaten, kontoDatenProjekt: KontoDaten, NbOfTxs: String): PaymentInstruction21 = {
    (kontoDatenKunde.iban, kontoDatenKunde.nameAccountHolder) match {
      case (Some(iban), Some(nameAccountHolder)) => {
        getPaymentInstruction(iban, nameAccountHolder, rechnung, NbOfTxs)
      }
    }
  }

  private def getPaymentInstruction(iban: String, nameAccountHolder: String, rechnung: Rechnung, transactionNumber: String): PaymentInstruction21 = {
    val PmtInfId = iban.slice(0, 15) + getSimpleDateTimeString(getDateTime())
    val PmtMtd = DD
    val BtchBookg = None
    val NbOfTxs = Some(transactionNumber)
    val CtrlSum = rechnung.betrag
    val PmtTpInf = Some(PaymentTypeInformation24(None, Some(ServiceLevel8Choice(DataRecord[String](None, Some("Cd"), "SEPA"))), Some(LocalInstrument2Choice(DataRecord[String](None, Some("Cd"), "Core"))), Some(FRST), None))
    val ReqdColltnDt = getDate()
    val Cdtr = pain008_001_07.PartyIdentification43(Some(nameAccountHolder), None, None, None, None)
    val CdtrAcct = pain008_001_07.CashAccount24(pain008_001_07.AccountIdentification4Choice(DataRecord[String](None, Some("iban"), iban)))
    val CdtrAgt = pain008_001_07.BranchAndFinancialInstitutionIdentification5(pain008_001_07.FinancialInstitutionIdentification8(None, None, None, None, None))
    val CdtrAgtAcct = None
    val UltmtCdtr = None
    val ChrgBr = Some(pain008_001_07.SLEV)
    val ChrgsAcct = None
    val ChrgsAcctAgt = None
    //-----------------
    val personIdentification = pain008_001_07.PersonIdentificationSchemeName1Choice(DataRecord[String](None, Some("Prtry"), "SEPA"))
    val genericPerson = pain008_001_07.GenericPersonIdentification1(iban, Some(personIdentification))
    val party11Choice = pain008_001_07.Party11Choice(DataRecord(None, Some("PrvtId"), pain008_001_07.PersonIdentification5(None, Seq(genericPerson))))
    val CdtrSchmeId = Some(pain008_001_07.PartyIdentification43(None, None, Some(party11Choice), None, None))
    //-----------------
    val DrctDbtTxInf = getDirectDebitTransactionInformation(iban, nameAccountHolder, rechnung)

    PaymentInstruction21(PmtInfId, PmtMtd, BtchBookg, NbOfTxs, Some(CtrlSum),
      PmtTpInf, ReqdColltnDt, Cdtr, CdtrAcct, CdtrAgt, CdtrAgtAcct, UltmtCdtr,
      ChrgBr, ChrgsAcct, ChrgsAcctAgt, CdtrSchmeId, Seq(DrctDbtTxInf))
  }

  private def getDirectDebitTransactionInformation(iban: String, nameAccountHolder: String, rechnung: Rechnung): DirectDebitTransactionInformation22 = {
    val PmtId = PaymentIdentification1(None, "NOTPROVIDED")
    //val PmtTpInf = Some(PaymentTypeInformation24(None, Some(ServiceLevel8Choice(DataRecord[String](None, Some("Cd"), "SEPA"))), Some(LocalInstrument2Choice(DataRecord[String](None, Some("Cd"), "Core"))), Some(FRST), None))
    val PmtTpInf = None
    val InstdAmt = pain008_001_07.ActiveOrHistoricCurrencyAndAmount(rechnung.betrag, Map[String, DataRecord[String]]("Ccy" -> DataRecord(None, Some("Ccy"), "EUR")))
    val ChrgBr = None
    val DrctDbtTx = Some(DirectDebitTransaction9(Some(MandateRelatedInformation11(Some(rechnung.kundeId.id.toString), Some(getDate()), None, None, None)), None, None, None))
    val UltmtCdtr = None
    val DbtrAgt = pain008_001_07.BranchAndFinancialInstitutionIdentification5(pain008_001_07.FinancialInstitutionIdentification8(None, None, None, None, Some(pain008_001_07.GenericFinancialIdentification1("NOTPROVIDED", None, None))))
    val DbtrAgtAcct = None
    val Dbtr = pain008_001_07.PartyIdentification43(Option(nameAccountHolder), None, None)
    val DbtrAcct = pain008_001_07.CashAccount24(pain008_001_07.AccountIdentification4Choice(DataRecord(None, Some("iban"), iban)))
    val UltmtDbtr = None
    val InstrForCdtrAgt = None
    val Purp = None
    val RgltryRptg = Nil
    val Tax = None
    val RltdRmtInf = Nil
    val RmtInf = Some(pain008_001_07.RemittanceInformation11(Seq("Text auf Kontoauszug Musterfrau"), Nil))
    val SplmtryData = Nil
    DirectDebitTransactionInformation22(PmtId, PmtTpInf, InstdAmt, ChrgBr, DrctDbtTx, UltmtCdtr, DbtrAgt, DbtrAgtAcct, Dbtr, DbtrAcct, UltmtDbtr, InstrForCdtrAgt, Purp, RgltryRptg, Tax, RltdRmtInf, RmtInf, SplmtryData)
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

object Pain008_001_07_Export {
  def exportPain008_001_07(rechnungen: List[(Rechnung, KontoDaten)], kontoDatenProjekt: KontoDaten, NbOfTxs: String): String = {
    new Pain008_001_07_Export().exportPain008_001_07(rechnungen, kontoDatenProjekt, NbOfTxs)
  }
}
