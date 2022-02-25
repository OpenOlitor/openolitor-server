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
package ch.openolitor.buchhaltung.zahlungsexport.iso20022

import ch.openolitor.buchhaltung.models.{ Rechnung, RechnungId, RechnungStatus }
import ch.openolitor.buchhaltung.rechnungsexport.iso20022.Pain008_001_07_Export
import ch.openolitor.core.models.PersonId
import ch.openolitor.stammdaten.models._
import org.joda.time.DateTime
import org.specs2.mutable._
import java.util.Locale

import scala.io.Source.fromInputStream

class pain008_001_07_ExportSpec extends Specification {
  "Pain008_001_07_Export" should {
    "export pain008_001_07 XML file" in {
      val exampleFileInputStream = getClass.getResourceAsStream("/pain_008_001_07_Sunu_Beispiel.xml")
      val exampleFileString = fromInputStream(exampleFileInputStream).mkString
      val iban = "AD12000120302003591001000000000001"
      val rechnung = Rechnung(
        RechnungId(1),
        KundeId(1),
        "",
        Waehrung("EUR"),
        BigDecimal(99.99),
        None,
        DateTime.parse("2017-08-07T14:48:06"),
        DateTime.parse("2017-08-07T14:48:06"),
        None,
        RechnungStatus("done"),
        "referenzNummer",
        "esrNummer",
        None,
        27,
        Set(),
        "strasse",
        Some("2"),
        Some("addressZusatz"),
        "plz",
        "ort",
        PaymentType("DirectDebit"),
        DateTime.parse("2017-08-07T14:48:06"),
        PersonId(1),
        DateTime.parse("2017-08-07T14:48:06"),
        PersonId(1)
      )

      val kontoDatenKunde = KontoDaten(
        KontoDatenId(1),
        Some(iban),
        None,
        None,
        None,
        Some("bankName"),
        Some("Musterfrau, Birgit"),
        None,
        Some(KundeId(1)),
        None,
        None,
        None,
        DateTime.parse("2017-08-07T14:48:06"),
        PersonId(1),
        DateTime.parse("2017-08-07T14:48:06"),
        PersonId(1)
      )

      val kontoDatenProjekt = KontoDaten(
        KontoDatenId(1),
        Some(iban),
        None,
        Some("referenzNummerPrefix"),
        Some("teilnehmerNummer"),
        None,
        None,
        None,
        None,
        Some("Musterfirma"),
        None,
        None,
        DateTime.parse("2017-08-07T14:48:06"),
        PersonId(1),
        DateTime.parse("2017-08-07T14:48:06"),
        PersonId(1)
      )

      val projekt = Projekt(
        ProjektId(1),
        "Project name",
        None,
        None,
        None,
        None,
        None,
        false,
        false,
        false,
        Waehrung("CHF"),
        1,
        1,
        Map(),
        Locale.ENGLISH,
        None,
        None,
        false,
        false,
        EinsatzEinheit("Tage"), 1, false, false,
        None,
        DateTime.parse("2017-08-07T14:48:06"),
        PersonId(1),
        DateTime.parse("2017-08-07T14:48:06"),
        PersonId(1)
      )

      val exampleFileStringNoSpaces = exampleFileString.split('\n').map(_.trim.filter(_ >= ' ')).mkString
      val result = Pain008_001_07_Export.exportPain008_001_07(List[(Rechnung, KontoDaten)]((rechnung, kontoDatenKunde)), kontoDatenProjekt, "1", projekt)
      //delete the dates and id from the result and the expected xml
      val exampleFileNoDates = exampleFileStringNoSpaces.replaceAll("<CreDtTm>.*</CreDtTm>", "<CreDtTm></CreDtTm>")
        .replaceAll("<ReqdColltnDt>.*</ReqdColltnDt>", "<ReqdColltnDt></ReqdColltnDt>")
        .replaceAll("<DtOfSgntr>.*</DtOfSgntr>", "<DtOfSgntr></DtOfSgntr>")
      val resultNoDates = result.replaceAll("<CreDtTm>.*</CreDtTm>", "<CreDtTm></CreDtTm>").replaceAll("<ReqdColltnDt>.*</ReqdColltnDt>", "<ReqdColltnDt></ReqdColltnDt>").replaceAll("<DtOfSgntr>.*</DtOfSgntr>", "<DtOfSgntr></DtOfSgntr>")
        .replaceAll("<MsgId>.*</MsgId>", "<MsgId>" + iban.slice(0, 15) + "</MsgId>")
        .replaceAll("<PmtInfId>.*</PmtInfId>", "<PmtInfId>" + iban.slice(0, 15) + "</PmtInfId>")
      resultNoDates === exampleFileNoDates
    }
  }
}
