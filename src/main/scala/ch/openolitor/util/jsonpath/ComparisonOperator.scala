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

import spray.json.{ JsBoolean, JsNull, JsNumber, JsString, JsValue }

sealed trait ComparisonOperator extends Product with Serializable {
  def apply(lhs: JsValue, rhs: JsValue): Boolean
}

// Comparison operators
/**
 * Originally token from gatlin-jsonpath and converted to spray-json
 * https://github.com/gatling/gatling/tree/master/gatling-jsonpath
 */
sealed trait ComparisonWithOrderingOperator extends ComparisonOperator {

  protected def compare[T: Ordering](lhs: T, rhs: T): Boolean

  def apply(lhs: JsValue, rhs: JsValue): Boolean =
    (lhs, rhs) match {
      case (JsString(leftValue), JsString(rightValue)) => compare(leftValue, rightValue)
      case (JsBoolean(leftValue), JsBoolean(rightValue)) => compare(leftValue, rightValue)
      case (JsNumber(leftValue), JsNumber(rightValue)) => compare(leftValue, rightValue)
      case _ => false
    }
}

case object EqWithOrderingOperator extends ComparisonWithOrderingOperator {
  protected def compare[T: Ordering](lhs: T, rhs: T): Boolean = Ordering[T].equiv(lhs, rhs)
}

case object EqOperator extends ComparisonOperator {
  override def apply(lhs: JsValue, rhs: JsValue): Boolean =
    (lhs, rhs) match {
      case (JsNull, JsNull) => true
      case (JsNull, _)      => false
      case (_, JsNull)      => false
      case _                => EqWithOrderingOperator(lhs, rhs)
    }
}

case object NotEqOperator extends ComparisonOperator {
  override def apply(lhs: JsValue, rhs: JsValue): Boolean = !EqOperator(lhs, rhs)
}

case object LessOperator extends ComparisonWithOrderingOperator {
  override protected def compare[T: Ordering](lhs: T, rhs: T): Boolean = Ordering[T].lt(lhs, rhs)
}

case object GreaterOperator extends ComparisonWithOrderingOperator {
  override protected def compare[T: Ordering](lhs: T, rhs: T): Boolean = Ordering[T].gt(lhs, rhs)
}

case object LessOrEqOperator extends ComparisonWithOrderingOperator {
  override protected def compare[T: Ordering](lhs: T, rhs: T): Boolean = Ordering[T].lteq(lhs, rhs)
}

case object GreaterOrEqOperator extends ComparisonWithOrderingOperator {
  override protected def compare[T: Ordering](lhs: T, rhs: T): Boolean = Ordering[T].gteq(lhs, rhs)
}

// Binary boolean operators
sealed trait BinaryBooleanOperator extends Product with Serializable {
  def apply(lhs: Boolean, rhs: Boolean): Boolean
}

case object AndOperator extends BinaryBooleanOperator {
  override def apply(lhs: Boolean, rhs: Boolean): Boolean = lhs && rhs
}

case object OrOperator extends BinaryBooleanOperator {
  override def apply(lhs: Boolean, rhs: Boolean): Boolean = lhs || rhs
}