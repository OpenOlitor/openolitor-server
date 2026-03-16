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
package ch.openolitor.buchhaltung.zahlungsimport.iso20022

import scala.util.Try

import ch.openolitor.buchhaltung.zahlungsimport.{ Gutschrift, Transaktionsart, ZahlungsImportParseException, ZahlungsImportResult }
import ch.openolitor.generated.xsd.camt054_001_08.{ BankToCustomerDebitCreditNotificationV08, Document }
import ch.openolitor.stammdaten.models.Waehrung

import org.joda.time.DateTime

import javax.xml.datatype.XMLGregorianCalendar

object Camt054v08Transaktionsart {
  def apply(c: String): Transaktionsart = c match {
    case "CRDT" => Gutschrift
    case _      => throw new ZahlungsImportParseException(s"unable to match $c")
  }
}

class Camt054v08ToZahlungsImportTransformer {
  // Helper: convert XMLGregorianCalendar to Joda DateTime safely
  private def xmlGcToDateTime(opt: javax.xml.datatype.XMLGregorianCalendar): DateTime = {
    if (opt == null) throw new ZahlungsImportParseException("Missing date")
    new DateTime(opt.toGregorianCalendar.getTime)
  }

  def transform(input: Document): Try[ZahlungsImportResult] = {
    transform(input.BkToCstmrDbtCdtNtfctn)
  }

  def transform(input: BankToCustomerDebitCreditNotificationV08): Try[ZahlungsImportResult] = {
    val groupHeader = input.GrpHdr // Level A

    Try(ZahlungsImportResult(input.Ntfctn flatMap { notification => // Level B
      notification.Ntry flatMap { entry => // Level C
        entry.NtryDtls flatMap { entryDetail => // Level D.1
          entryDetail.TxDtls map { transactionDetail => // Level D.2

            val debtorName: Option[String] = transactionDetail.RltdPties.flatMap(_.Dbtr).flatMap { dbtr =>
              Option(dbtr.party40choiceoption).flatMap { dr =>
                // DataRecord may wrap different concrete party types; try extracting common ones
                val tryPartyNm = scala.util.Try(dr.as[ch.openolitor.generated.xsd.camt054_001_08.PartyIdentification135].Nm).toOption.flatten
                val tryBranchNm = scala.util.Try(dr.as[ch.openolitor.generated.xsd.camt054_001_08.BranchAndFinancialInstitutionIdentification6].FinInstnId.Nm).toOption.flatten
                tryPartyNm.orElse(tryBranchNm).orElse(scala.util.Try(Some(dr.asInstanceOf[Any].toString)).toOption.flatten)
              }
            }

            val refString: String = transactionDetail.RmtInf map (_.Strd match {
              case Nil        => ""
              case structures => structures.flatMap(_.CdtrRefInf.flatMap(_.Ref)).mkString(",")
            }) getOrElse ""

            // Amount extraction: prefer AmtDtls.TxAmt.Amt.value, fallback to direct Amt.value in v08 files
            val amountOpt: Option[BigDecimal] = {
              val fromAmtDtls = Try(transactionDetail.AmtDtls.flatMap(_.TxAmt.map(_.Amt.value))).toOption.flatten
              val fromAmt = Try(transactionDetail.Amt.map(_.value)).toOption.flatten
              fromAmtDtls.orElse(fromAmt)
            }

            val waehrungOpt: Option[ch.openolitor.stammdaten.models.Waehrung] = {
              val fromAmtDtls = Try(transactionDetail.AmtDtls.flatMap(_.TxAmt.map(txAmt => Waehrung.applyUnsafe(txAmt.Amt.Ccy)))).toOption.flatten
              val fromAmt = Try(transactionDetail.Amt.map(amt => Waehrung.applyUnsafe(amt.Ccy))).toOption.flatten
              fromAmtDtls.orElse(fromAmt)
            }

            Camt054Record(
              entry.NtryRef,
              Some(notification.Acct.Id.accountidentification4choiceoption.as[String]),
              debtorName,
              refString, // Referenznummer (non-defensive)
              amountOpt.getOrElse(throw new ZahlungsImportParseException("Missing Betrag")),
              waehrungOpt.getOrElse(throw new ZahlungsImportParseException("Missing Waehrung")),
              Camt054v08Transaktionsart(transactionDetail.CdtDbtInd.getOrElse(throw new ZahlungsImportParseException("Missing credit/debit indicator")).toString),
              "",
              // convert XMLGregorianCalendar to Joda DateTime directly (groupHeader.CreDtTm is an XMLGregorianCalendar, not an Option)
              xmlGcToDateTime(groupHeader.CreDtTm),
              xmlGcToDateTime(entry.BookgDt.get.dateanddatetime2choiceoption.as[XMLGregorianCalendar]),
              xmlGcToDateTime(entry.ValDt.get.dateanddatetime2choiceoption.as[XMLGregorianCalendar]),
              "",
              0.0
            )
          }
        }
      }
    }))
  }
}
