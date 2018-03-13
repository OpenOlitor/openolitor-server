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

import ch.openolitor.buchhaltung.models.Rechnung
import ch.openolitor.buchhaltung.models.RechnungId
import ch.openolitor.buchhaltung.models.RechnungStatus
import ch.openolitor.buchhaltung.rechnungsexport.iso20022.Pain008_003_02_Export
import ch.openolitor.stammdaten.models.KontoDaten
import ch.openolitor.stammdaten.models.PaymentType
import ch.openolitor.stammdaten.models.KontoDatenId
import ch.openolitor.core.models.PersonId
import ch.openolitor.stammdaten.models.KundeId
import ch.openolitor.stammdaten.models.Waehrung
import org.joda.time.DateTime
import org.specs2.mutable._
import scala.io.Source.fromInputStream

class pain008_003_02_ExportSpec extends Specification {
  "Pain008_003_02_Export" should {
    "export pain008_003_02 XML file" in {
      val exampleFileInputStream = getClass.getResourceAsStream("/pain_008_003_02_Sunu_Beispiel.xml")
      val exampleFileString = fromInputStream(exampleFileInputStream).mkString
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
        Some("AD12000120302003591001000000000001"),
        None,
        None,
        Some("bankName"),
        Some("Musterfrau, Birgit"),
        None,
        Some(KundeId(1)),
        None,
        DateTime.parse("2017-08-07T14:48:06"),
        PersonId(1),
        DateTime.parse("2017-08-07T14:48:06"),
        PersonId(1)
      )

      val kontoDatenProjekt = KontoDaten(
        KontoDatenId(1),
        Some("AD12000120302003591001000000000000"),
        Some("referenzNummerPrefix"),
        Some("teilnehmerNummer"),
        None,
        None,
        None,
        None,
        Some("Musterfirma"),
        DateTime.parse("2017-08-07T14:48:06"),
        PersonId(1),
        DateTime.parse("2017-08-07T14:48:06"),
        PersonId(1)
      )

      val exampleFileStringNoSpaces = exampleFileString.split('\n').map(_.trim.filter(_ >= ' ')).mkString
      val result = Pain008_003_02_Export.exportPain008_003_02(List[(Rechnung, KontoDaten)]((rechnung, kontoDatenKunde)), kontoDatenProjekt, "1")
      //delete the dates from the result and the expected xml
      val exampleFileNoDates = exampleFileStringNoSpaces.replaceAll("<CreDtTm>.*</CreDtTm>", "<CreDtTm></CreDtTm>").replaceAll("<ReqdColltnDt>.*</ReqdColltnDt>", "<ReqdColltnDt></ReqdColltnDt>").replaceAll("<DtOfSgntr>.*</DtOfSgntr>", "<DtOfSgntr></DtOfSgntr>")
      val resultNoDates = result.replaceAll("<CreDtTm>.*</CreDtTm>", "<CreDtTm></CreDtTm>").replaceAll("<ReqdColltnDt>.*</ReqdColltnDt>", "<ReqdColltnDt></ReqdColltnDt>").replaceAll("<DtOfSgntr>.*</DtOfSgntr>", "<DtOfSgntr></DtOfSgntr>")
      resultNoDates === exampleFileNoDates
    }
  }
}