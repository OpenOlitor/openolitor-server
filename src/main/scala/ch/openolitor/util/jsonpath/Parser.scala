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

import ch.openolitor.util.jsonpath.AST._

import scala.util.parsing.combinator.RegexParsers

class StringBuilderPool extends ThreadLocal[StringBuilder] {

  override def initialValue() = new StringBuilder(512)

  override def get(): StringBuilder = {
    val sb = super.get()
    sb.setLength(0)
    sb
  }
}

object FastStringOps {

  private val stringBuilderPool = new StringBuilderPool

  implicit class RichString(val text: String) extends AnyVal {
    def fastReplaceAll(replaced: String, replacement: String): String =
      if (replaced.isEmpty || replacement.isEmpty) {
        text
      } else {
        var end = text.indexOf(replaced)
        if (end == -1) {
          text
        } else {
          var start = 0
          val replacedLength = replaced.length
          val buf = stringBuilderPool.get()
          while (end != -1) {
            buf.append(text, start, end).append(replacement)
            start = end + replacedLength
            end = text.indexOf(replaced, start)
          }
          buf.append(text, start, text.length).toString
        }
      }
  }
}

/**
 * Originally token from gatlin-jsonpath and converted to spray-json
 */
object Parser extends RegexParsers {

  private val stringBuilderPool = new StringBuilderPool

  private[jsonpath] def fastReplaceAll(text: String, replaced: String, replacement: String): String = {
    var end = text.indexOf(replaced)
    if (end == -1) {
      text
    } else {
      var start = 0
      val replacedLength = replaced.length
      val buf = stringBuilderPool.get()
      while (end != -1) {
        buf.append(text, start, end).append(replacement)
        start = end + replacedLength
        end = text.indexOf(replaced, start)
      }
      buf.append(text, start, text.length).toString
    }
  }

  private val NumberRegex = """-?\d+""".r
  private val FieldRegex = """[^\*\.\[\]\(\)=!<>\s]+""".r
  private val SingleQuotedFieldRegex = """(\\.|[^'])+""".r
  private val DoubleQuotedFieldRegex = """(\\.|[^"])+""".r
  private val SingleQuotedValueRegex = """(\\.|[^'])*""".r
  private val DoubleQuotedValueRegex = """(\\.|[^"])*""".r
  private val NumberValueRegex = """-?\d+(\.\d*)?""".r

  /// general purpose parsers ///////////////////////////////////////////////

  private def number: Parser[Int] = NumberRegex ^^ (_.toInt)

  private def field: Parser[String] = FieldRegex

  private def singleQuotedField = "'" ~> SingleQuotedFieldRegex <~ "'" ^^ (fastReplaceAll(_, "\\'", "'"))
  private def doubleQuotedField = "\"" ~> DoubleQuotedFieldRegex <~ "\"" ^^ (fastReplaceAll(_, "\\\"", "\""))
  private def singleQuotedValue = "'" ~> SingleQuotedValueRegex <~ "'" ^^ (fastReplaceAll(_, "\\'", "'"))
  private def doubleQuotedValue = "\"" ~> DoubleQuotedValueRegex <~ "\"" ^^ (fastReplaceAll(_, "\\\"", "\""))
  private def quotedField: Parser[String] = singleQuotedField | doubleQuotedField
  private def quotedValue: Parser[String] = singleQuotedValue | doubleQuotedValue

  /// array parsers /////////////////////////////////////////////////////////

  private def arraySliceStep: Parser[Option[Int]] = ":" ~> number.?

  private def arraySlice: Parser[ArraySlice] =
    (":" ~> number.?) ~ arraySliceStep.? ^^ {
      case end ~ step => ArraySlice(None, end, step.flatten.getOrElse(1))
    }

  private def arrayRandomAccess: Parser[Option[ArrayRandomAccess]] =
    rep1("," ~> number).? ^^ (_.map(ArrayRandomAccess))

  private def arraySlicePartial: Parser[ArrayAccessor] =
    number ~ arraySlice ^^ {
      case i ~ as => as.copy(start = Some(i))
    }

  private def arrayRandomAccessPartial: Parser[ArrayAccessor] =
    number ~ arrayRandomAccess ^^ {
      case i ~ Some(ArrayRandomAccess(indices)) => ArrayRandomAccess(i :: indices)
      case i ~ _                                => ArrayRandomAccess(i :: Nil)
    }

  private def arrayPartial: Parser[ArrayAccessor] =
    arraySlicePartial | arrayRandomAccessPartial

  private def arrayAll: Parser[ArraySlice] =
    "*" ^^^ ArraySlice.All

  private[jsonpath] def arrayAccessors: Parser[ArrayAccessor] =
    "[" ~> (arrayAll | arrayPartial | arraySlice) <~ "]"

  /// filters parsers ///////////////////////////////////////////////////////

  private def numberValue: Parser[FilterDirectValue] = NumberValueRegex ^^ { s =>
    if (s.indexOf('.') != -1) FilterDirectValue.double(s.toDouble) else FilterDirectValue.long(s.toLong)
  }

  private def booleanValue: Parser[FilterDirectValue] =
    "true" ^^^ FilterDirectValue.True |
      "false" ^^^ FilterDirectValue.False

  private def nullValue: Parser[FilterValue] =
    "null" ^^^ FilterDirectValue.Null

  private def stringValue: Parser[FilterDirectValue] = quotedValue ^^ { FilterDirectValue.string }
  private def value: Parser[FilterValue] = booleanValue | numberValue | nullValue | stringValue

  private def comparisonOperator: Parser[ComparisonOperator] =
    "==" ^^^ EqOperator |
      "!=" ^^^ NotEqOperator |
      "<=" ^^^ LessOrEqOperator |
      "<" ^^^ LessOperator |
      ">=" ^^^ GreaterOrEqOperator |
      ">" ^^^ GreaterOperator

  private def current: Parser[PathToken] = "@" ^^^ CurrentNode

  private def subQuery: Parser[SubQuery] =
    (current | root) ~ pathSequence ^^ { case c ~ ps => SubQuery(c :: ps) }

  private def expression1: Parser[FilterToken] =
    subQuery ~ (comparisonOperator ~ (subQuery | value)).? ^^ {
      case subq1 ~ None         => HasFilter(subq1)
      case lhs ~ Some(op ~ rhs) => ComparisonFilter(op, lhs, rhs)
    }

  private def expression2: Parser[FilterToken] =
    value ~ comparisonOperator ~ subQuery ^^ {
      case lhs ~ op ~ rhs => ComparisonFilter(op, lhs, rhs)
    }

  private def expression: Parser[FilterToken] = expression1 | expression2

  private def booleanOperator: Parser[BinaryBooleanOperator] = "&&" ^^^ AndOperator | "||" ^^^ OrOperator

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  private def booleanExpression: Parser[FilterToken] =
    expression ~ (booleanOperator ~ booleanExpression).? ^^ {
      case lhs ~ None => lhs
      // Balance the AST tree so that all "Or" operations are always on top of any "And" operation.
      // Indeed, the "And" operations have a higher priority and must be executed first.
      case lhs1 ~ Some(AndOperator ~ BooleanFilter(OrOperator, lhs2, rhs2)) =>
        BooleanFilter(OrOperator, BooleanFilter(AndOperator, lhs1, lhs2), rhs2)
      case lhs ~ Some(op ~ rhs) => BooleanFilter(op, lhs, rhs)
    }

  private def recursiveSubscriptFilter: Parser[RecursiveFilterToken] =
    (("..*" | "..") ~> subscriptFilter) ^^ RecursiveFilterToken

  private[jsonpath] def subscriptFilter: Parser[FilterToken] =
    "[?(" ~> booleanExpression <~ ")]"

  /// child accessors parsers ///////////////////////////////////////////////

  private[jsonpath] def subscriptField: Parser[FieldAccessor] =
    "[" ~> rep1sep(quotedField, ",") <~ "]" ^^ {
      case f1 :: Nil => Field(f1)
      case fields    => MultiField(fields)
    }

  private[jsonpath] def dotField: Parser[FieldAccessor] =
    "." ~> field ^^ Field

  // TODO recursive with `subscriptField`
  private def recursiveField: Parser[FieldAccessor] =
    ".." ~> field ^^ RecursiveField

  private def anyChild: Parser[FieldAccessor] = (".*" | "['*']" | """["*"]""") ^^^ AnyField

  private def recursiveAny: Parser[FieldAccessor] = "..*" ^^^ RecursiveAnyField

  private[jsonpath] def fieldAccessors = (
    dotField
    | recursiveSubscriptFilter
    | recursiveAny
    | recursiveField
    | anyChild
    | subscriptField
  )

  /// Main parsers //////////////////////////////////////////////////////////

  private def childAccess = fieldAccessors | arrayAccessors

  private[jsonpath] def pathSequence: Parser[List[PathToken]] = rep(childAccess | subscriptFilter)

  private[jsonpath] def root: Parser[PathToken] = "$" ^^^ RootNode

  private def query: Parser[List[PathToken]] =
    phrase(root ~ pathSequence) ^^ { case r ~ ps => r :: ps }
}

class Parser {
  private val query = Parser.query
  def compile(jsonpath: String): Parser.ParseResult[List[PathToken]] = Parser.parse(query, jsonpath)
}