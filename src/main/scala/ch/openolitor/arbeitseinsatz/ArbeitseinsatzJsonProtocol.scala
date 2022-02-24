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
package ch.openolitor.arbeitseinsatz

import spray.json._
import ch.openolitor.arbeitseinsatz.models._
import ch.openolitor.core.{ BaseJsonProtocol, JSONSerializable }
import ch.openolitor.stammdaten.StammdatenJsonProtocol
import ch.openolitor.stammdaten.models.PersonContactPermissionModify
import com.typesafe.scalalogging.LazyLogging
import zangelo.spray.json.AutoProductFormats

/**
 * JSON Format deklarationen für das Modul rbeitseinsatz
 */
trait ArbeitseinsatzJsonProtocol extends BaseJsonProtocol with LazyLogging with AutoProductFormats[JSONSerializable] with StammdatenJsonProtocol {

  //enum formats
  implicit val arbeitseinsatzStatusFormat = enumFormat(ArbeitseinsatzStatus.apply)

  //id formats
  implicit val arbeitskategorieIdFormat = baseIdFormat(ArbeitskategorieId)
  implicit val arbeitsangebotIdFormat = baseIdFormat(ArbeitsangebotId)
  implicit val arbeitseinsatzIdFormat = baseIdFormat(ArbeitseinsatzId)

  implicit val arbeitskategorieBezIdFormat = new RootJsonFormat[ArbeitskategorieBez] {
    def write(obj: ArbeitskategorieBez): JsValue =
      JsString(obj.id)

    def read(json: JsValue): ArbeitskategorieBez =
      json match {
        case JsString(id) => ArbeitskategorieBez(id)
        case kt           => sys.error(s"Unknown ArbeitskategorieBez:$kt")
      }
  }

  implicit val arbeitskategorieFormat = autoProductFormat[Arbeitskategorie]
  implicit val arbeitsangebotFormat = autoProductFormat[Arbeitsangebot]
  implicit val arbeitseinsatzFormat = autoProductFormat[Arbeitseinsatz]
  implicit val arbeitseinsatzDetailFormat = autoProductFormat[ArbeitseinsatzDetail]
  implicit val arbeitseinsatzDetailReportFormat = autoProductFormat[ArbeitseinsatzDetailReport]

  implicit val arbeitskategorieModifyFormat = autoProductFormat[ArbeitskategorieModify]
  implicit val arbeitsangebotModifyFormat = autoProductFormat[ArbeitsangebotModify]
  implicit val arbeitseinsatzModifyFormat = autoProductFormat[ArbeitseinsatzModify]

  implicit val arbeitseinsatzAbrechnungFormat = autoProductFormat[ArbeitseinsatzAbrechnung]

  implicit val arbeitsangebotDuplicateFormat = autoProductFormat[ArbeitsangebotDuplicate]
  implicit val arbeitsComplexFlagsFormat = jsonFormat1(ArbeitsComplexFlags)
  implicit val optionArbeitsComplexFlagsFormat = new OptionFormat[ArbeitsComplexFlags]

}
