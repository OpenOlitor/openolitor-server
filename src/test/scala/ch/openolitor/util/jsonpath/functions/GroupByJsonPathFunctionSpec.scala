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
import org.specs2.mutable.Specification
import spray.json.{ JsArray, JsNumber, JsObject, JsString }

class GroupByJsonPathFunctionSpec extends Specification with Matchers {
  "Group by of json elements" should {
    "Group elements by same property" in {
      val input = Vector(
        JsObject("id" -> JsNumber(1), "productId" -> JsString("product1")),
        JsObject("id" -> JsNumber(2), "productId" -> JsString("product2")),
        JsObject("id" -> JsNumber(3), "productId" -> JsString("product2")),
        JsObject("id" -> JsNumber(4), "productId" -> JsString("product3"))
      )
      val result = JsonPathFunctions.GroupBy.evaluate("productId", input)
      result shouldNotEqual None

      result.get must containTheSameElementsAs(
        Vector(
          JsArray(JsObject("id" -> JsNumber(1), "productId" -> JsString("product1"))),
          JsArray(
            JsObject("id" -> JsNumber(2), "productId" -> JsString("product2")),
            JsObject("id" -> JsNumber(3), "productId" -> JsString("product2"))
          ),
          JsArray(JsObject("id" -> JsNumber(4), "productId" -> JsString("product3")))
        )
      )
    }

    "Group elements by same nested property" in {
      val input = Vector(
        JsObject("id" -> JsNumber(1), "product" -> JsObject("productId" -> JsString("product1"))),
        JsObject("id" -> JsNumber(2), "product" -> JsObject("productId" -> JsString("product2"))),
        JsObject("id" -> JsNumber(3), "product" -> JsObject("productId" -> JsString("product2"))),
        JsObject("id" -> JsNumber(4), "product" -> JsObject("productId" -> JsString("product3")))
      )
      val result = JsonPathFunctions.GroupBy.evaluate("product.productId", input)
      result shouldNotEqual None

      result.get must containTheSameElementsAs(
        Vector(
          JsArray(JsObject("id" -> JsNumber(1), "product" -> JsObject("productId" -> JsString("product1")))),
          JsArray(
            JsObject("id" -> JsNumber(2), "product" -> JsObject("productId" -> JsString("product2"))),
            JsObject("id" -> JsNumber(3), "product" -> JsObject("productId" -> JsString("product2")))
          ),
          JsArray(JsObject("id" -> JsNumber(4), "product" -> JsObject("productId" -> JsString("product3"))))
        )
      )
    }

    "Group only elements where the property exists" in {
      val input = Vector(
        JsObject("id" -> JsNumber(1), "productId" -> JsString("product1")),
        JsObject("id" -> JsNumber(2), "productId" -> JsString("product2")),
        JsObject("id" -> JsNumber(3), "productId" -> JsString("product2")),
        JsObject("id" -> JsNumber(4))
      )
      val result = JsonPathFunctions.GroupBy.evaluate("productId", input)
      result shouldNotEqual None

      result.get must containTheSameElementsAs(
        Vector(
          JsArray(JsObject("id" -> JsNumber(1), "productId" -> JsString("product1"))),
          JsArray(
            JsObject("id" -> JsNumber(2), "productId" -> JsString("product2")),
            JsObject("id" -> JsNumber(3), "productId" -> JsString("product2"))
          )
        )
      )
    }

    "Return empty vector if property does not exist" in {
      val input = Vector(
        JsObject("id" -> JsNumber(1), "productId" -> JsString("product1")),
        JsObject("id" -> JsNumber(2), "productId" -> JsString("product2")),
        JsObject("id" -> JsNumber(3), "productId" -> JsString("product2")),
        JsObject("id" -> JsNumber(4), "productId" -> JsString("product3"))
      )
      val result = JsonPathFunctions.GroupBy.evaluate("productId2", input)
      result shouldEqual Some(Vector())
    }
  }
}
