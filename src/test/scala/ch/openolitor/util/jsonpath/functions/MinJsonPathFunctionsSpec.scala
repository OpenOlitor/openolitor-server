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
package ch.openolitor.util.jsonpath.functions

import org.specs2.matcher.Matchers
import org.specs2.mutable._
import spray.json.{ JsNull, JsNumber, JsObject, JsString }

class MinJsonPathFunctionsSpec extends Specification with Matchers {
  "Min of values" should {
    "calculate correct min if all values are numbers" in {
      JsonPathFunctions.Min.evaluate(Vector(JsNumber(2), JsNumber(3), JsNumber(4))) shouldEqual Some(Vector(JsNumber(2)))
    }

    "Parse numbers from strings" in {
      JsonPathFunctions.Min.evaluate(Vector(JsString("2"), JsNumber(3), JsString("4"))) shouldEqual Some(Vector(JsNumber(2)))
    }

    "min only valid numbers" in {
      JsonPathFunctions.Min.evaluate(Vector(JsString("NoString"), JsNumber(3), JsString("4"), JsObject(), JsNull)) shouldEqual Some(Vector(JsNumber(3)))
    }

    "evaluate to None for empty lists" in {
      JsonPathFunctions.Min.evaluate(Vector()) shouldEqual None
    }
  }
}
