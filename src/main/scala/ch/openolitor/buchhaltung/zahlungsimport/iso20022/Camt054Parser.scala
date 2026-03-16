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

import ch.openolitor.buchhaltung.zahlungsimport._
import ch.openolitor.generated.xsd.camt054_001_04._
import scala.util._
import scala.xml.XML
import java.io.InputStream

class Camt054Parser {
  def parse(is: InputStream): Try[ZahlungsImportResult] = {
    Try(XML.load(is)) flatMap { node =>
      // try v08
      val tryV08 = Try {
        scalaxb.fromXML[ch.openolitor.generated.xsd.camt054_001_08.Document](node)
      }
      tryV08 match {
        case scala.util.Success(doc08) => (new Camt054v08ToZahlungsImportTransformer).transform(doc08)
        case scala.util.Failure(err08) => {
          // try v06
          val tryV06 = Try {
            scalaxb.fromXML[ch.openolitor.generated.xsd.camt054_001_06.Document](node)
          }
          tryV06 match {
            case scala.util.Success(doc06) => (new Camt054v06ToZahlungsImportTransformer).transform(doc06)
            case scala.util.Failure(err06) => {
              val tryV04 = Try {
                scalaxb.fromXML[ch.openolitor.generated.xsd.camt054_001_04.Document](node)
              }
              tryV04 match {
                case scala.util.Success(doc04) => (new Camt054v04ToZahlungsImportTransformer).transform(doc04)
                case scala.util.Failure(err04) => Failure(new Exception(s"v08 error: ${err08.getMessage}\n v06 error: ${err06.getMessage}\n v04 error: ${err04.getMessage}"))
              }
            }
          }
        }
      }
    }
  }
}

object Camt054Parser extends ZahlungsImportParser {
  def parse(is: InputStream): Try[ZahlungsImportResult] = {
    new Camt054Parser().parse(is)
  }
}
