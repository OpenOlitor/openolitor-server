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

import spray.json.{ JsFalse, JsNull, JsNumber, JsString, JsTrue, JsValue }

/**
 * Originally token from gatlin-jsonpath and converted to spray-json
 */
object AST {
  sealed trait AstToken extends Product with Serializable
  sealed trait PathToken extends AstToken

  sealed trait FieldAccessor extends PathToken
  case object RootNode extends FieldAccessor
  final case class Field(name: String) extends FieldAccessor
  final case class RecursiveField(name: String) extends FieldAccessor
  final case class MultiField(names: List[String]) extends FieldAccessor
  final case object AnyField extends FieldAccessor
  final case object RecursiveAnyField extends FieldAccessor

  sealed trait ArrayAccessor extends PathToken

  /**
   * Slicing of an array, indices start at zero
   *
   * @param start is the first item that you want (of course)
   * @param stop is the first item that you do not want
   * @param step, being positive or negative, defines whether you are moving
   */
  final case class ArraySlice(start: Option[Int], stop: Option[Int], step: Int) extends ArrayAccessor
  object ArraySlice {
    val All: ArraySlice = ArraySlice(None, None, 1)
  }
  final case class ArrayRandomAccess(indices: List[Int]) extends ArrayAccessor

  // JsonPath Filter AST //////////////////////////////////////////////

  final case object CurrentNode extends PathToken
  sealed trait FilterValue extends AstToken

  object FilterDirectValue {
    def long(value: Long): FilterDirectValue = FilterDirectValue(JsNumber(value))
    def double(value: Double): FilterDirectValue = FilterDirectValue(JsNumber(value))
    val True: FilterDirectValue = FilterDirectValue(JsTrue)
    val False: FilterDirectValue = FilterDirectValue(JsFalse)
    def string(value: String): FilterDirectValue = FilterDirectValue(JsString(value))
    val Null: FilterDirectValue = FilterDirectValue(JsNull)
  }

  final case class FilterDirectValue(node: JsValue) extends FilterValue

  final case class SubQuery(path: List[PathToken]) extends FilterValue

  sealed trait FilterToken extends PathToken
  final case class HasFilter(query: SubQuery) extends FilterToken
  final case class ComparisonFilter(operator: ComparisonOperator, lhs: FilterValue, rhs: FilterValue) extends FilterToken
  final case class BooleanFilter(operator: BinaryBooleanOperator, lhs: FilterToken, rhs: FilterToken) extends FilterToken

  final case class RecursiveFilterToken(filter: FilterToken) extends PathToken
}