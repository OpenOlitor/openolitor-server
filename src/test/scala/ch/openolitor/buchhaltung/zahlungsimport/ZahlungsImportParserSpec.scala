/*                                                                           *\
*    ____                   ____  ___ __                                      *
*   / __ \____  ___  ____  / __ \/ (_) /_____  _____                          *
*  / /_/ / /_/ /  __/ / / / /_/ / / / /_/ /_/ / /       contributed by tegonal *
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
package ch.openolitor.buchhaltung.zahlungsimport

import org.specs2.mutable._
import java.nio.file.{ Files, Paths }

import ch.openolitor.buchhaltung.zahlungsimport.iso20022.Camt054Record
import ch.openolitor.stammdaten.models.CHF
import org.joda.time.format.ISODateTimeFormat

class ZahlungsImportParserSpec extends Specification {
  "ZahlungsImportParser" should {

    "parse example esr file" in {
      val bytes = Files.readAllBytes(Paths.get(getClass.getResource("/esrimport.esr").toURI()))

      val result = ZahlungsImportParser.parse(bytes)

      beSuccessfulTry(result)

      result.get.records.size === 225
    }

    "parse example esr file with blank lines" in {
      val bytes = Files.readAllBytes(Paths.get(getClass.getResource("/esrimport_with_blank_lines.esr").toURI()))

      val result = ZahlungsImportParser.parse(bytes)

      beSuccessfulTry(result)

      result.get.records.size === 5
    }

    "parse example camt.054 file (generic existing)" in {
      val bytes = Files.readAllBytes(Paths.get(getClass.getResource("/camt_054_Beispiel_ZA1_ESR_ZE.xml").toURI()))

      val result = ZahlungsImportParser.parse(bytes)

      beSuccessfulTry(result)

      result.get.records.size === 1
    }

    // New: test camt.054 v06 sample
    "parse camt.054.001.06 sample" in {
      val bytes = Files.readAllBytes(Paths.get(getClass.getResource("/camt_054_001_06_example.xml").toURI()))

      val result = ZahlungsImportParser.parse(bytes)

      beSuccessfulTry(result)

      result.get.records.size === 1

      val expected = Camt054Record(
        Some("010391391"),
        Some("CH160077401231234567"),
        Some("Pia Rutschmann"),
        "210000000003139471430009017",
        3949.75,
        CHF,
        Gutschrift,
        "",
        ISODateTimeFormat.dateOptionalTimeParser.parseDateTime("2015-01-15T09:30:47Z"),
        ISODateTimeFormat.dateOptionalTimeParser.parseDateTime("2015-01-07"),
        ISODateTimeFormat.dateOptionalTimeParser.parseDateTime("2015-01-07"),
        "",
        0.0
      )

      result.get.records.head mustEqual expected
    }

    // New: test camt.054 v08 sample
    "parse camt.054.001.08 sample" in {
      val bytes = Files.readAllBytes(Paths.get(getClass.getResource("/camt_054_001_80_example.xml").toURI()))

      val result = ZahlungsImportParser.parse(bytes)

      beSuccessfulTry(result)

      result.get.records.size === 1

      val expected = Camt054Record(
        Some("010391391"),
        Some("CH160077401231234567"),
        Some("Pia Rutschmann"),
        "210000000003139471430009017",
        3949.75,
        CHF,
        Gutschrift,
        "",
        ISODateTimeFormat.dateOptionalTimeParser.parseDateTime("2015-01-15T09:30:47Z"),
        ISODateTimeFormat.dateOptionalTimeParser.parseDateTime("2015-01-07"),
        ISODateTimeFormat.dateOptionalTimeParser.parseDateTime("2015-01-07"),
        "",
        0.0
      )

      result.get.records.head mustEqual expected
    }

  }
}
