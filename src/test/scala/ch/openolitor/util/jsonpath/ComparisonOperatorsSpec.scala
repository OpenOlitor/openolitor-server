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
package ch.openolitor.util.jsonpath


import org.specs2.mutable.Specification
import org.specs2.matcher._
import org.specs2.ScalaCheck
import spray.json._

/**
 * Originally token from gatlin-jsonpath and converted to spray-json
 * https://github.com/gatling/gatling/tree/master/gatling-jsonpath
 */
class ComparisonOperatorsSpec extends Specification with Matchers with ScalaCheck {

  "comparison operators" should {
    "return false if types aren't compatible" in {
      prop { (string: String, int: Int) =>
        val lhn = JsString(string)
        val rhn = JsNumber(int)
        LessOperator(lhn, rhn) should beFalse
        GreaterOperator(lhn, rhn) should beFalse
        LessOrEqOperator(lhn, rhn) should beFalse
        GreaterOrEqOperator(lhn, rhn) should beFalse
      }

      prop { (bool:Boolean, string:String) =>
        val lhn = JsBoolean(bool)
        val rhn = JsString(string)
        LessOperator(lhn, rhn) should beFalse
        GreaterOperator(lhn, rhn) should beFalse
        LessOrEqOperator(lhn, rhn) should beFalse
        GreaterOrEqOperator(lhn, rhn) should beFalse
      }

      prop { (int: Int, string: String) =>
        val lhn = JsNumber(int)
        val rhn = JsString(string)
        LessOperator(lhn, rhn) should beFalse
        GreaterOperator(lhn, rhn) should beFalse
        LessOrEqOperator(lhn, rhn) should beFalse
        GreaterOrEqOperator(lhn, rhn) should beFalse
      }
    }
  }

  "it" should {
    "properly compare Strings" in {
      prop { (val1: String, val2: String) =>
        val lhn = JsString(val1)
        val rhn = JsString(val2)
        LessOperator(lhn, rhn) should beEqualTo(val1 < val2)
        GreaterOperator(lhn, rhn) should beEqualTo(val1 > val2)
        LessOrEqOperator(lhn, rhn) should beEqualTo(val1 <= val2)
        GreaterOrEqOperator(lhn, rhn) should beEqualTo(val1 >= val2)
      }
    }

    "properly compare Booleans" in {
      prop { (val1: Boolean, val2: Boolean) =>
        val lhn = JsBoolean(val1)
        val rhn = JsBoolean(val2)
        LessOperator(lhn, rhn) should beEqualTo(val1 < val2)
        GreaterOperator(lhn, rhn) should beEqualTo(val1 > val2)
        LessOrEqOperator(lhn, rhn) should beEqualTo(val1 <= val2)
        GreaterOrEqOperator(lhn, rhn) should beEqualTo(val1 >= val2)
      }
    }

    "properly compare Int with other numeric types" in {
      prop { (val1: Int, val2: Int) =>
        val lhn = JsNumber(val1)
        val rhn = JsNumber(val1)
        LessOperator(lhn, rhn) should beEqualTo(val1 < val2)
        GreaterOperator(lhn, rhn) should beEqualTo(val1 > val2)
        LessOrEqOperator(lhn, rhn) should beEqualTo(val1 <= val2)
        GreaterOrEqOperator(lhn, rhn) should beEqualTo(val1 >= val2)
      }

      prop { (val1: Int, val2: Long) =>
        val lhn = JsNumber(val1)
        val rhn = JsNumber(val2)
        LessOperator(lhn, rhn) should beEqualTo(val1 < val2)
        GreaterOperator(lhn, rhn) should beEqualTo(val1 > val2)
        LessOrEqOperator(lhn, rhn) should beEqualTo(val1 <= val2)
        GreaterOrEqOperator(lhn, rhn) should beEqualTo(val1 >= val2)
      }

      prop { (val1: Int, val2: Double) =>
        val lhn = JsNumber(val1)
        val rhn = JsNumber(val2)
        LessOperator(lhn, rhn) should beEqualTo(val1 < val2)
        GreaterOperator(lhn, rhn) should beEqualTo(val1 > val2)
        LessOrEqOperator(lhn, rhn) should beEqualTo(val1 <= val2)
        GreaterOrEqOperator(lhn, rhn) should beEqualTo(val1 >= val2)
      }

      prop { (val1: Int, val2: Float) =>
        val lhn = JsNumber(val1)
        val rhn = JsNumber(val2)
        LessOperator(lhn, rhn) should beEqualTo(val1 < val2)
        GreaterOperator(lhn, rhn) should beEqualTo(val1 > val2)
        LessOrEqOperator(lhn, rhn) should beEqualTo(val1 <= val2)
        GreaterOrEqOperator(lhn, rhn) should beEqualTo(val1 >= val2)
      }
    }

    "properly compare Long with other numeric types" in {
      prop { (val1: Long, val2: Int) =>
        val lhn = JsNumber(val1)
        val rhn = JsNumber(val2)
        LessOperator(lhn, rhn) should beEqualTo(val1 < val2)
        GreaterOperator(lhn, rhn) should beEqualTo(val1 > val2)
        LessOrEqOperator(lhn, rhn) should beEqualTo(val1 <= val2)
        GreaterOrEqOperator(lhn, rhn) should beEqualTo(val1 >= val2)
      }

      prop { (val1: Long, val2: Long) =>
        val lhn = JsNumber(val1)
        val rhn = JsNumber(val2)
        LessOperator(lhn, rhn) should beEqualTo(val1 < val2)
        GreaterOperator(lhn, rhn) should beEqualTo(val1 > val2)
        LessOrEqOperator(lhn, rhn) should beEqualTo(val1 <= val2)
        GreaterOrEqOperator(lhn, rhn) should beEqualTo(val1 >= val2)
      }

      prop { (val1: Long, val2: Double) =>
        val lhn = JsNumber(val1)
        val rhn = JsNumber(val2)
        LessOperator(lhn, rhn) should beEqualTo(val1 < val2)
        GreaterOperator(lhn, rhn) should beEqualTo(val1 > val2)
        LessOrEqOperator(lhn, rhn) should beEqualTo(val1 <= val2)
        GreaterOrEqOperator(lhn, rhn) should beEqualTo(val1 >= val2)
      }

      prop { (val1: Long, val2: Float) =>
        val lhn = JsNumber(val1)
        val rhn = JsNumber(val2)
        LessOperator(lhn, rhn) should beEqualTo(val1 < val2)
        GreaterOperator(lhn, rhn) should beEqualTo(val1 > val2)
        LessOrEqOperator(lhn, rhn) should beEqualTo(val1 <= val2)
        GreaterOrEqOperator(lhn, rhn) should beEqualTo(val1 >= val2)
      }
    }

    "properly compare Double with other numeric types" in {
      prop { (val1: Double, val2: Int) =>
        val lhn = JsNumber(val1)
        val rhn = JsNumber(val2)
        LessOperator(lhn, rhn) should beEqualTo(val1 < val2)
        GreaterOperator(lhn, rhn) should beEqualTo(val1 > val2)
        LessOrEqOperator(lhn, rhn) should beEqualTo(val1 <= val2)
        GreaterOrEqOperator(lhn, rhn) should beEqualTo(val1 >= val2)
      }

      prop { (val1: Double, val2: Long) =>
        val lhn = JsNumber(val1)
        val rhn = JsNumber(val2)
        LessOperator(lhn, rhn) should beEqualTo(val1 < val2)
        GreaterOperator(lhn, rhn) should beEqualTo(val1 > val2)
        LessOrEqOperator(lhn, rhn) should beEqualTo(val1 <= val2)
        GreaterOrEqOperator(lhn, rhn) should beEqualTo(val1 >= val2)
      }

      prop { (val1: Double, val2: Double) =>
        val lhn = JsNumber(val1)
        val rhn = JsNumber(val2)
        LessOperator(lhn, rhn) should beEqualTo(val1 < val2)
        GreaterOperator(lhn, rhn) should beEqualTo(val1 > val2)
        LessOrEqOperator(lhn, rhn) should beEqualTo(val1 <= val2)
        GreaterOrEqOperator(lhn, rhn) should beEqualTo(val1 >= val2)
      }

      prop { (val1: Double, val2: Float) =>
        val lhn = JsNumber(val1)
        val rhn = JsNumber(val2)
        LessOperator(lhn, rhn) should beEqualTo(val1 < val2)
        GreaterOperator(lhn, rhn) should beEqualTo(val1 > val2)
        LessOrEqOperator(lhn, rhn) should beEqualTo(val1 <= val2)
        GreaterOrEqOperator(lhn, rhn) should beEqualTo(val1 >= val2)
      }
    }

    "properly compare Float with other numeric types" in {
      prop { (val1: Float, val2: Int) =>
        val lhn = JsNumber(val1)
        val rhn = JsNumber(val2)
        LessOperator(lhn, rhn) should beEqualTo(val1 < val2)
        GreaterOperator(lhn, rhn) should beEqualTo(val1 > val2)
        LessOrEqOperator(lhn, rhn) should beEqualTo(val1 <= val2)
        GreaterOrEqOperator(lhn, rhn) should beEqualTo(val1 >= val2)
      }

      prop { (val1: Float, val2: Long) =>
        val lhn = JsNumber(val1)
        val rhn = JsNumber(val2)
        LessOperator(lhn, rhn) should beEqualTo(val1 < val2)
        GreaterOperator(lhn, rhn) should beEqualTo(val1 > val2)
        LessOrEqOperator(lhn, rhn) should beEqualTo(val1 <= val2)
        GreaterOrEqOperator(lhn, rhn) should beEqualTo(val1 >= val2)
      }

      prop { (val1: Float, val2: Double) =>
        val lhn = JsNumber(val1)
        val rhn = JsNumber(val2)
        LessOperator(lhn, rhn) should beEqualTo(val1 < val2)
        GreaterOperator(lhn, rhn) should beEqualTo(val1 > val2)
        LessOrEqOperator(lhn, rhn) should beEqualTo(val1 <= val2)
        GreaterOrEqOperator(lhn, rhn) should beEqualTo(val1 >= val2)
      }

      prop { (val1: Float, val2: Float) =>
        val lhn = JsNumber(val1)
        val rhn = JsNumber(val2)
        LessOperator(lhn, rhn) should beEqualTo(val1 < val2)
        GreaterOperator(lhn, rhn) should beEqualTo(val1 > val2)
        LessOrEqOperator(lhn, rhn) should beEqualTo(val1 <= val2)
        GreaterOrEqOperator(lhn, rhn) should beEqualTo(val1 >= val2)
      }
    }
  }

  "AndOperator" should {
    "&& the lhs and rhs" in {
      prop { (b1:Boolean, b2: Boolean) =>
        AndOperator(b1, b2) should beEqualTo(b1 && b2)
      }
    }
  }

  "OrOperator" should {
    "|| the lhs and rhs" in {
      prop { (b1:Boolean, b2: Boolean) =>
        OrOperator(b1, b2) should beEqualTo(b1 || b2)
      }
    }
  }
}