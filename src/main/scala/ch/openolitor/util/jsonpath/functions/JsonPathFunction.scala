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

import ch.openolitor.util.jsonpath.JsonPath
import com.typesafe.scalalogging.LazyLogging
import spray.json._

import scala.util.Try

sealed trait UnaryJsonPathFunction extends LazyLogging {
  def evaluate(jsValue: Vector[JsValue]): Option[Vector[JsValue]]
}

sealed trait Param1JsonPathFunction extends LazyLogging {
  def evaluate(params1: String, jsValue: Vector[JsValue]): Option[Vector[JsValue]]
}

/**
 * functions applied to jsonpath
 */
object JsonPathFunctions {

  abstract class BaseStringFunction extends LazyLogging {
    def extractNumber(jsValue: JsValue): Option[String] = {
      jsValue match {
        case JsObject(_) =>
          logger.debug(s"Cannot extract string from JsObject: $jsValue")
          None
        case JsNull =>
          logger.debug(s"Cannot convert null value to number")
          None
        case JsString(stringValue) =>
          Some(stringValue)
        case JsNumber(number) =>
          Some(number.toString())
        case JsBoolean(value) =>
          Some(value.toString)
        case JsArray(values) =>
          logger.debug(s"Cannot extract string from array:$values")
          None
      }
    }

  }

  abstract class BaseUnaryStringFunction extends BaseStringFunction with UnaryJsonPathFunction {
    def evaluate(jsValue: Vector[JsValue]): Option[Vector[JsValue]] = {
      val result = applyNumberFunction(jsValue.map(extractNumber))
      result.map(x => Vector(JsString(x)))
    }

    protected def applyNumberFunction(values: Vector[Option[String]]): Option[String]
  }

  abstract class BaseParam1StringFunction extends BaseStringFunction with Param1JsonPathFunction {
    def evaluate(param1: String, jsValue: Vector[JsValue]): Option[Vector[JsValue]] = {
      val result = applyNumberFunction(param1, jsValue.map(extractNumber))
      result.map(x => Vector(JsString(x)))
    }

    protected def applyNumberFunction(params1: String, values: Vector[Option[String]]): Option[String]
  }

  /**
   * Concatenate all paths into a single string with a defined separator
   */
  object MkString extends BaseParam1StringFunction {
    protected def applyNumberFunction(separator: String, values: Vector[Option[String]]): Option[String] = {
      values match {
        case Vector() => None
        case _        => Some(values.flatten.mkString(separator))
      }
    }
  }

  abstract class BaseNumberFunction extends UnaryJsonPathFunction {
    def extractNumber(jsValue: JsValue): Option[BigDecimal] = {
      jsValue match {
        case JsObject(_) =>
          logger.debug(s"Cannot extract number from JsObject: $jsValue")
          None
        case JsNull =>
          logger.debug(s"Cannot convert null value to number")
          None
        case JsString(stringValue) =>
          logger.debug(s"Extract number from string => $stringValue")
          Try(BigDecimal(stringValue)).toOption
        case JsNumber(number) =>
          Some(number)
        case JsBoolean(value) =>
          logger.debug(s"Cannot extract number from boolean:$value")
          None
        case JsArray(values) =>
          logger.debug(s"Cannot extract number from array:$values")
          None
      }
    }

    def evaluate(jsValue: Vector[JsValue]): Option[Vector[JsValue]] = {
      val result = applyNumberFunction(jsValue.map(extractNumber))
      result.map(x => Vector(JsNumber(x)))
    }

    protected def applyNumberFunction(values: Vector[Option[BigDecimal]]): Option[BigDecimal]
  }

  /**
   * Sum of all numeric value
   */
  object Sum extends BaseNumberFunction {
    protected def applyNumberFunction(values: Vector[Option[BigDecimal]]): Option[BigDecimal] =
      Some(values.flatten.sum)
  }

  /**
   * Min numeric value
   */
  object Min extends BaseNumberFunction {
    protected def applyNumberFunction(values: Vector[Option[BigDecimal]]): Option[BigDecimal] = values.flatten match {
      case Vector()                     => None
      case elements: Vector[BigDecimal] => Some(elements.min)
    }
  }

  /**
   * Max numeric value
   */
  object Max extends BaseNumberFunction {
    protected def applyNumberFunction(values: Vector[Option[BigDecimal]]): Option[BigDecimal] = values.flatten match {
      case Vector()                     => None
      case elements: Vector[BigDecimal] => Some(elements.max)
    }
  }

  /**
   * Calculate average of all number elements
   */
  object Average extends BaseNumberFunction {

    protected def applyNumberFunction(values: Vector[Option[BigDecimal]]): Option[BigDecimal] = values match {
      case Vector() => None
      case _ =>
        // calculate average considering null values as well
        Some(values.flatten.sum / values.length)
    }
  }

  /**
   * Count all elements
   */
  object Count extends UnaryJsonPathFunction {
    def evaluate(jsValue: Vector[JsValue]): Option[Vector[JsValue]] = {
      Some(Vector(JsNumber(jsValue.length)))
    }
  }

  /**
   * Group jsValue33
   */
  object GroupBy extends Param1JsonPathFunction {
    def evaluate(property: String, jsValue: Vector[JsValue]): Option[Vector[JsValue]] = {
      val groups = jsValue.groupBy(jsValue => JsonPath.query("$." + property, jsValue).right.toOption.flatMap {
        // if property was not found, JsonPath evaluated to empty vector, map this case to None
        case Vector() => None
        case x        => Some(x)
      })
      // filter out not matched properties and map vector into a jsarray
      val result = groups.filterNot(_._1.isEmpty).map(entries => JsArray(entries._2)).toVector
      logger.debug(s"Result of $$groupBy function is:$result")
      Some(result)
    }
  }

  /**
   * Pretty-Print the current json-tree as a way of debugging into the document
   */
  object Debug extends UnaryJsonPathFunction {
    def evaluate(jsValues: Vector[JsValue]): Option[Vector[JsValue]] = {
      logger.debug(s"+++++++++++++++++ Debug:")
      val result = JsArray(jsValues).prettyPrint
      Some(Vector(JsString(result)))
    }
  }
}
