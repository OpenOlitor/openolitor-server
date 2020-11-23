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
import spray.json.{JsNull, JsNumber, JsObject, JsString}

class AverageJsonPathFunctionsSpec extends Specification with Matchers {
  "Average of values" should {
    "calculate correct average if all values are numbers" in {
      JsonPathFunctions.Average.evaluate(Vector(JsNumber(2), JsNumber(3), JsNumber(4))) shouldEqual Some(Vector(JsNumber(3)))
    }

    "Parse numbers from strings" in {
      JsonPathFunctions.Average.evaluate(Vector(JsString("2"), JsNumber(3), JsString("4"))) shouldEqual Some(Vector(JsNumber(3)))
    }

    "average only valid numbers, but divide against all elements in the set" in {
      // valid sum = 8
      // count = 5
      // average = 2
      JsonPathFunctions.Average.evaluate(Vector(JsString("NoString"), JsNumber(3), JsString("5"), JsObject(), JsNull)) shouldEqual Some(Vector(JsNumber(1.6)))
    }

    "evaluate to None for empty lists" in {
      JsonPathFunctions.Average.evaluate(Vector()) shouldEqual None
    }
  }
}
