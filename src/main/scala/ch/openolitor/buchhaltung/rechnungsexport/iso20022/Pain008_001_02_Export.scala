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
import ch.openolitor.generated.xsd.pain008_001_02._
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

class Pain008_001_02_Export extends LazyLogging {

  private def exportPain008_001_02(rechnungen: List[(Rechnung, KontoDaten)], kontoDatenProjekt: KontoDaten, NbOfTxs: String): String = {
    val paymentInstructionInformationSDD = rechnungen map { rechnung =>
      getPaymentInstructionInformationSDD(rechnung._1, rechnung._2, kontoDatenProjekt, NbOfTxs)
    }

    scalaxb.toXML[ch.openolitor.generated.xsd.pain008_001_02.Document](ch.openolitor.generated.xsd.pain008_001_02.Document(CustomerDirectDebitInitiationV02(getGroupHeaderSDD(rechnungen.map(_._1), kontoDatenProjekt, NbOfTxs), paymentInstructionInformationSDD)), "Document", defineNamespaceBinding()).toString()
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
    val nsb2 = NamespaceBinding("xsi", "http://www.w3.org/2001/XMLSchema-instance", TopScope)
    val nsb3 = NamespaceBinding("schemaLocation", "urn:iso:std:iso:20022:tech:xsd:pain.008.001.02.xsd", nsb2)
    NamespaceBinding(null, "urn:iso:std:iso:20022:tech:xsd:pain.008.001.02", nsb3)
  }

  private def getGroupHeaderSDD(rechnungen: List[Rechnung], kontoDatenProjekt: KontoDaten, nbTransactions: String): GroupHeaderSDD = {
    val MsgId = kontoDatenProjekt.iban.get + randomAlphaNumericString(34 - kontoDatenProjekt.iban.get.length)
    val CreDtTm = getDateTime
    val NbOfTxs = nbTransactions
    val CtrlSum = getSumAllRechnungs(rechnungen)
    val partyIdentificationSepa1 = kontoDatenProjekt.creditorIdentifier.getOrElse("Initiator")

    GroupHeaderSDD(MsgId, CreDtTm, NbOfTxs, CtrlSum, PartyIdentificationSEPA1(Some(partyIdentificationSepa1), None))
  }

  private def getPaymentInstructionInformationSDD(rechnung: Rechnung, kontoDatenKunde: KontoDaten, kontoDatenProjekt: KontoDaten, NbOfTxs: String): PaymentInstructionInformationSDD = {
    (kontoDatenKunde.iban, kontoDatenKunde.nameAccountHolder, kontoDatenProjekt.creditorIdentifier) match {
      case (Some(iban), Some(nameAccountHolder), Some(creditorIdentifier)) => {
        val PmtInfId = iban + randomAlphaNumericString(34 - iban.length)
        val CtrlSum = rechnung.betrag
        val PmtMtd = DD
        val BtchBookg = None
        val PmtTpInf = Some(PaymentTypeInformationSDD(ServiceLevel("SEPA"), LocalInstrumentSEPA("Core"), FRST, None))
        val ReqdColltnDt = getDate
        val Cdtr = PartyIdentificationSEPA5(nameAccountHolder, None)

        val CdtrAcct = CashAccountSEPA1(AccountIdentificationSEPA(iban))
        val CdtrAgt = BranchAndFinancialInstitutionIdentificationSEPA3(FinancialInstitutionIdentificationSEPA3(DataRecord[String](None, None, "")))
        val UltmtCdtr = None
        val ChrgBr = Some(ch.openolitor.generated.xsd.pain008_001_02.SLEV)
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
        val RmtInf = None

        val DrctDbtTxInf = DirectDebitTransactionInformationSDD(PmtId, PmtTpInf, InstdAmt, ChrgBr_l3, DrctDbtTx, UltmtCdtr, DbtrAgt, Dbtr, DbtrAcct, UltmtDbtr, Purp, RmtInf)
        PaymentInstructionInformationSDD(PmtInfId, PmtMtd, BtchBookg, NbOfTxs, CtrlSum,
          PmtTpInf, ReqdColltnDt, Cdtr, CdtrAcct, CdtrAgt, UltmtCdtr,
          ChrgBr, CdtrSchmeId, Seq(DrctDbtTxInf))
      }
    }
  }

  private def getSumAllRechnungs(rechnungen: List[Rechnung]): BigDecimal = {
    rechnungen.map(_.betrag).sum
  }

  def randomStringFromCharList(length: Int, chars: Seq[Char]): String = {
    val sb = new StringBuilder
    for (i <- 1 to length) {
      val randomNum = util.Random.nextInt(chars.length)
      sb.append(chars(randomNum))
    }
    sb.toString
  }

  def randomAlphaNumericString(length: Int): String = {
    val chars = ('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9')
    randomStringFromCharList(length, chars)
  }
}

object Pain008_001_02_Export {
  def exportPain008_001_02(rechnungen: List[(Rechnung, KontoDaten)], kontoDatenProjekt: KontoDaten, NbOfTxs: String): String = {
    new Pain008_001_02_Export().exportPain008_001_02(rechnungen, kontoDatenProjekt, NbOfTxs)
  }
}
