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
import spray.json.{JsArray, JsNull, JsNumber, JsObject, JsString}

class MkStringJsonPathFunctionsSpec extends Specification with Matchers {
  "MKString of values" should {
    "Combine all elements into a single string with the given separator" in {
      JsonPathFunctions.MkString.evaluate(",", Vector(JsString("StringA"), JsNumber(3), JsString("StringB"))) shouldEqual Some(Vector(JsString("StringA,3,StringB")))
    }

    "Consider only valid strings" in {
      JsonPathFunctions.MkString.evaluate(":", Vector(JsString("StringA"), JsNumber(3), JsString("StringB"), JsObject(), JsNull, JsArray(JsString("nestedString")))) shouldEqual Some(Vector(JsString("StringA:3:StringB")))
    }

    "evaluate to empty string for list with no valid strings in it" in {
      JsonPathFunctions.MkString.evaluate(".", Vector(JsObject(), JsNull)) shouldEqual Some(Vector(JsString("")))
    }

    "evaluate to None for empty lists" in {
      JsonPathFunctions.MkString.evaluate(".", Vector()) shouldEqual None
    }
  }
}
