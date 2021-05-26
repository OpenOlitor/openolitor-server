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
import ch.openolitor.generated.xsd.camt052_001_02._
import ch.openolitor.stammdaten.models.Waehrung

import org.joda.time.format.ISODateTimeFormat

import javax.xml.datatype.XMLGregorianCalendar

object Camt052v02Transaktionsart {
  def apply(c: String): Transaktionsart = c match {
    case "CRDT" => Gutschrift
    case _      => throw new ZahlungsImportParseException(s"unable to match $c")
  }
}

class Camt052v02ToZahlungsImportTransformer {
  def transform(input: Document): Try[ZahlungsImportResult] = {
    transform(input.BkToCstmrAcctRpt)
  }

  def transform(input: BankToCustomerAccountReportV02): Try[ZahlungsImportResult] = {
    val groupHeader = input.GrpHdr // Level A

    Try(ZahlungsImportResult(input.Rpt flatMap { notification => // Level B
      //filter by only credit transactions
      notification.Ntry.filter(_.CdtDbtInd.equals(CRDT)) flatMap { entry => // Level C
        entry.NtryDtls flatMap { entryDetail => // Level D.1
          entryDetail.TxDtls map { transactionDetail => // Level D.2
            Camt052Record(
              Some(transactionDetail.Refs.get.Prtry.get.Ref),
              Some(notification.Acct.Id.accountidentification4choiceoption.as[String]),
              (transactionDetail.RltdPties flatMap (_.Dbtr flatMap (_.Nm))),
              transactionDetail.RmtInf map (_.Strd match {
                case Nil        => ""
                case structures => (structures flatMap (_.CdtrRefInf flatMap (_.Ref))).mkString(",")
              }) getOrElse "", // Referenznummer
              (entry.Amt.value),
              (Waehrung.applyUnsafe(entry.Amt.Ccy)),
              Camt052v02Transaktionsart(entry.CdtDbtInd.toString),
              "",
              ISODateTimeFormat.dateOptionalTimeParser.parseDateTime(groupHeader.CreDtTm.toString),
              ISODateTimeFormat.dateOptionalTimeParser.parseDateTime(entry.BookgDt.get.dateanddatetimechoiceoption.as[XMLGregorianCalendar].toString),
              ISODateTimeFormat.dateOptionalTimeParser.parseDateTime(entry.ValDt.get.dateanddatetimechoiceoption.as[XMLGregorianCalendar].toString),
              "",
              0.0
            )
          }
        }
      }
    }))
  }
}
