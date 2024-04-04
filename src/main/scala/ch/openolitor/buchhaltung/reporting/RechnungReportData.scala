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
package ch.openolitor.buchhaltung.reporting

import ch.openolitor.buchhaltung.models._
import ch.openolitor.buchhaltung.repositories.BuchhaltungReadRepositoryAsyncComponent
import ch.openolitor.buchhaltung.BuchhaltungJsonProtocol
import ch.openolitor.core.{ ActorReferences, ExecutionContextAware }
import ch.openolitor.core.db.AsyncConnectionPoolContextAware
import ch.openolitor.core.reporting._
import ch.openolitor.core.Macros._
import ch.openolitor.stammdaten.models.{ KontoDaten, Projekt, ProjektReport }
import ch.openolitor.stammdaten.repositories.StammdatenReadRepositoryAsyncComponent
import com.typesafe.scalalogging.LazyLogging
import net.codecrete.qrbill.generator._
import org.joda.time.{ DateTime, DateTimeZone }

import java.time.LocalDate
import java.util.Locale
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

trait RechnungReportData extends AsyncConnectionPoolContextAware with BuchhaltungJsonProtocol with LazyLogging with ExecutionContextAware {
  self: BuchhaltungReadRepositoryAsyncComponent with ActorReferences with StammdatenReadRepositoryAsyncComponent =>

  def rechungenById(rechnungIds: Seq[RechnungId]): Future[(Seq[ValidationError[RechnungId]], Seq[RechnungDetailReport])] = {
    stammdatenReadRepository.getProjekt flatMap { maybeProjekt =>
      stammdatenReadRepository.getKontoDatenProjekt flatMap { maybeKontoDaten =>
        maybeProjekt flatMap { projekt =>
          maybeKontoDaten map { kontoDaten =>
            val results = Future.sequence(rechnungIds.map { rechnungId =>
              buchhaltungReadRepository.getRechnungDetail(rechnungId).map(_.map { rechnung =>
                val qrCode = Some(createQrCode(rechnung, kontoDaten, projekt))
                rechnung.status match {
                  case Storniert =>
                    Left(ValidationError[RechnungId](rechnungId, s"Für stornierte Rechnungen können keine Berichte mehr erzeugt werden"))
                  case Bezahlt =>
                    Left(ValidationError[RechnungId](rechnungId, s"Für bezahlte Rechnungen können keine Berichte mehr erzeugt werden"))
                  case _ =>
                    val projektReport = copyTo[Projekt, ProjektReport](projekt)
                    qrCode match {
                      case Some(error) if error.startsWith("Error: ") => {
                        Left(ValidationError[RechnungId](rechnungId, error))
                      }
                      case Some(_) => Right(copyTo[RechnungDetail, RechnungDetailReport](rechnung, "qrCode" -> qrCode, "projekt" -> projektReport, "kontoDaten" -> kontoDaten))
                    }
                }

              }.getOrElse(Left(ValidationError[RechnungId](rechnungId, s"Rechnung konnte nicht gefunden werden"))))
            })
            results.map(_.partition(_.isLeft) match {
              case (a, b) => (a.map(_.swap.toOption.get), b.map(_.toOption.get))
            })
          }
        } getOrElse Future { (Seq(ValidationError[RechnungId](null, s"Projekt konnte nicht geladen werden")), Seq()) }
      }
    }
  }

  def createQrCode(rechnung: RechnungDetail, kontoDaten: KontoDaten, projekt: Projekt): String = {
    /* the iban is mandatory in order to get a valid qrCode. In case the system does not have one iban setup
    * the qrcode will be empty*/
    kontoDaten.iban match {
      case Some(iban) =>
        val result = stammdatenReadRepository.getPersonen(rechnung.kunde.id) map { personen =>
          val bill = new Bill()
          val billFormat = new BillFormat()
          val language = projekt.sprache match {
            case Locale.FRENCH  => Language.FR
            case Locale.GERMAN  => Language.DE
            case Locale.ITALIAN => Language.IT
            case Locale.ENGLISH => Language.EN
            case _              => Language.DE
          }
          billFormat.setLanguage(language)
          billFormat.setOutputSize(OutputSize.QR_BILL_EXTRA_SPACE)
          billFormat.setSeparatorType(SeparatorType.DASHED_LINE_WITH_SCISSORS)
          bill.setFormat(billFormat)
          //this value is mandatory for the qrCode. In case of generating qrCode
          bill.setAccount(iban)
          bill.setAmount(rechnung.betrag.bigDecimal)
          bill.setCurrency(projekt.waehrung.toString)

          // Set creditor
          val creditor = new Address()
          creditor.setName(kontoDaten.nameAccountHolder.getOrElse(projekt.bezeichnung))
          creditor.setStreet(projekt.strasse.getOrElse(""))
          creditor.setHouseNo(projekt.hausNummer.getOrElse(""))
          creditor.setPostalCode(projekt.plz.getOrElse(""))
          creditor.setTown(projekt.ort.getOrElse(""))
          creditor.setCountryCode("CH")
          bill.setCreditor(creditor)

          // more bill data
          bill.setUnstructuredMessage(rechnung.titel)
          bill.setReference(rechnung.referenzNummer)
          bill.setReferenceType(Bill.REFERENCE_TYPE_QR_REF)

          // Set debtor
          val debtor = new Address()
          val p = personen map { person =>
            person.fullName
          }
          debtor.setName(p.mkString(","))
          debtor.setStreet(rechnung.kunde.strasse)
          debtor.setHouseNo(rechnung.kunde.hausNummer.getOrElse(""))
          debtor.setPostalCode(rechnung.kunde.plz)
          debtor.setTown(rechnung.kunde.ort)
          debtor.setCountryCode("CH")
          bill.setDebtor(debtor)

          try {
            val svg = QRBill.generate(bill)
            new String(svg)
          } catch {
            case e: QRBillValidationError => {
              val listValidation = e.getValidationResult.getValidationMessages
              val message: String = listValidation.asScala.map { m =>
                m.getMessageKey: String
              }.mkString("")
              if (message.equals("account_iban_not_from_ch_or_li")) {
                logger.warn(s"Bei der QR-Code-Validierung wurde festgestellt, dass die IBAN nicht aus der Schweiz oder Liechtenstein stammt")
                s""
              } else {
                logger.warn(s"Error: QR-Code-Validierung wurde folgender Problemcode ausgegeben: $message")
                s"Error: QR-Code-Validierung wurde folgender Problemcode ausgegeben: $message}"
              }
            }
          }
        }
        Await.result(result, 5.seconds)
      case None => {
        logger.warn(s"Error: Die Initiative muss über eine IBAN verfügen, um einen QR-Code erstellen zu können ")
        s"Error: Die Initiative muss über eine IBAN verfügen, um einen QR-Code erstellen zu können"
      }
    }
  }

  def toLocalDate(dateTime: DateTime) = {
    val dateTimeUtc = dateTime.withZone(DateTimeZone.UTC)
    LocalDate.of(dateTimeUtc.getYear(), dateTimeUtc.getMonthOfYear(), dateTimeUtc.getDayOfMonth())
  }
}
