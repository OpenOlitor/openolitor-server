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

import ch.openolitor.generated.xsd.camt054_001_04._
import ch.openolitor.generated.xsd.pain008_001_07._
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

class Pain008_003_02_Export extends LazyLogging {

  private def exportPain008_003_02(rechnungen: List[(Rechnung, KontoDaten)], kontoDatenProjekt: KontoDaten, NbOfTxs: String): String = {
    //val paymentInstructionInformationSDD = rechnungen map { rechnung =>
    //  getPaymentInstructionInformationSDD(rechnung._1, rechnung._2, kontoDatenProjekt, NbOfTxs)
    // }

    //"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
    //  scalaxb.toXML[ch.openolitor.generated.xsd.pain008_003_02.Document](ch.openolitor.generated.xsd.pain008_003_02.Document(CustomerDirectDebitInitiationV02(getGroupHeaderSDD(rechnungen.map(_._1), kontoDatenProjekt, NbOfTxs), paymentInstructionInformationSDD)), "Document", defineNamespaceBinding()).toString()
    ???
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
    val nsb2 = NamespaceBinding("schemaLocation", "urn:iso:std:iso:20022:tech:xsd:pain.008.003.02 pain.008.003.02.xsd", TopScope)
    val nsb3 = NamespaceBinding("xsi", "http://www.w3.org/2001/XMLSchema-instance", nsb2)
    NamespaceBinding(null, "urn:iso:std:iso:20022:tech:xsd:pain.008.003.02", nsb3)
  }

  //private def getGroupHeaderSDD(rechnungen: List[Rechnung], kontoDatenProjekt: KontoDaten, nbTransactions: String): GroupHeaderSDD = {
  //val MsgId = kontoDatenProjekt.iban.get.slice(0, 15) + getSimpleDateTimeString(getDateTime())
  //val CreDtTm = getDateTime
  //jval NbOfTxs = nbTransactions
  //jval CtrlSum = None
  //jval partyIdentificationSepa1 = kontoDatenProjekt.creditorIdentifier.getOrElse("Initiator")

  //jGroupHeaderSDD(MsgId, CreDtTm, NbOfTxs, CtrlSum, PartyIdentificationSEPA1(Some(partyIdentificationSepa1), None))
  //}

  /*private def getPaymentInstructionInformationSDD(rechnung: Rechnung, kontoDatenKunde: KontoDaten, kontoDatenProjekt: KontoDaten, NbOfTxs: String): PaymentInstructionInformationSDD = {
    (kontoDatenKunde.iban, kontoDatenKunde.nameAccountHolder, kontoDatenProjekt.creditorIdentifier) match {
      case (Some(iban), Some(nameAccountHolder), Some(creditorIdentifier)) => {
        val PmtInfId = iban.slice(0, 15) + getSimpleDateTimeString(getDateTime())
        val CtrlSum = Some(rechnung.betrag)
        val PmtMtd = DD
        val BtchBookg = None
        val PmtTpInf = PaymentTypeInformationSDD(ServiceLevelSEPA("SEPA"), LocalInstrumentSEPA("Core"), FRST, None)
        val ReqdColltnDt = getDate
        val Cdtr = PartyIdentificationSEPA5(creditorIdentifier, None)

        val CdtrAcct = CashAccountSEPA1(AccountIdentificationSEPA(iban))
        val CdtrAgt = BranchAndFinancialInstitutionIdentificationSEPA3(FinancialInstitutionIdentificationSEPA3(DataRecord[String](None, Some("BIC"), "BFSWDE88KRL")))
        val UltmtCdtr = None
        val ChrgBr = Some(ch.openolitor.generated.xsd.pain008_003_02.SLEV)
        val CdtrSchmeId = Some(PartyIdentificationSEPA3(PartySEPA2(PersonIdentificationSEPA2(RestrictedPersonIdentificationSEPA(iban, RestrictedPersonIdentificationSchemeNameSEPA(SEPA))))))

        // direct debit parameters
        val PmtId = PaymentIdentificationSEPA(None, "NOTPROVIDED")
        val InstdAmt = ActiveOrHistoricCurrencyAndAmountSEPA(rechnung.betrag, Map[String, DataRecord[String]]("Ccy" -> DataRecord(None, Some("Ccy"), "EUR")))
        val ChrgBr_l3 = None
        val DrctDbtTx = DirectDebitTransactionSDD(MandateRelatedInformationSDD(rechnung.kundeId.id.toString, getDate(), None, None, None), None)
        val DbtrAgt = BranchAndFinancialInstitutionIdentificationSEPA3(FinancialInstitutionIdentificationSEPA3(DataRecord[OthrIdentification](None, Some("Othr"), OthrIdentification(NOTPROVIDED))))
        val Dbtr = PartyIdentificationSEPA2(nameAccountHolder, None, None)
        val DbtrAcct = CashAccountSEPA2(AccountIdentificationSEPA(iban))
        val UltmtDbtr = None
        val Purp = None
        val RmtInf = Some(RemittanceInformationSEPA1Choice(DataRecord[String](None, Some("Ustrd"), "Text auf Kontoauszug Musterfrau")))

        val DrctDbtTxInf = DirectDebitTransactionInformationSDD(PmtId, InstdAmt, ChrgBr_l3, DrctDbtTx, UltmtCdtr, DbtrAgt, Dbtr, DbtrAcct, UltmtDbtr, Purp, RmtInf)

        PaymentInstructionInformationSDD(PmtInfId, PmtMtd, BtchBookg, Some(NbOfTxs), CtrlSum,
          PmtTpInf, ReqdColltnDt, Cdtr, CdtrAcct, CdtrAgt, UltmtCdtr,
          ChrgBr, CdtrSchmeId, Seq(DrctDbtTxInf))
      }
    }
    ??
  }*/

  private def getSimpleDateTimeString(date: XMLGregorianCalendar): String = {
    date.getYear.toString +
      date.getMonth.toString +
      date.getDay.toString +
      date.getHour.toString +
      date.getMinute.toString +
      date.getSecond.toString
  }
}

object Pain008_003_02_Export {
  def exportPain008_003_02(rechnungen: List[(Rechnung, KontoDaten)], kontoDatenProjekt: KontoDaten, NbOfTxs: String): String = {
    new Pain008_003_02_Export().exportPain008_003_02(rechnungen, kontoDatenProjekt, NbOfTxs)
  }

}
