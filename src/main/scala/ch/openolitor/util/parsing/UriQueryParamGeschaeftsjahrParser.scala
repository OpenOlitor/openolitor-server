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
package ch.openolitor.util.parsing

import com.typesafe.scalalogging.LazyLogging
import org.joda.time.DateTime

import scala.util.parsing.combinator._

case class DatumVonBisFilter(von: Datum, bis: Datum)

object UriQueryParamGeschaeftsjahrParser with LazyLogging {

  def parse(input: String): Option[DatumVonBisFilter] = {
    parseAll(filterExpression, input) match {
      case Success(result, _) => Some(result)
      case Failure(message, _) =>
        logger.error(s"Could not parse input: $message")
        None
      case Error(message, _) =>
        logger.error(s"Could not parse input: $message")
        None
    }
  }

  def filterExpression: Parser[FilterExpr] =
    repsep(filterAttribute, separator) ~ opt(separator) ^^ { case l ~ _ => FilterAttributeList(l) }

  private def filterAttribute: Parser[FilterAttribute] =
    attribute ~ assignment ~ valueComparison ~ rep("," ~> valueComparison) ^^ { case a ~ _ ~ head ~ rest => FilterAttribute(a, head :: rest) }

  private def valueComparison: Parser[ValueComparison] =
    valueComparator ~ "(" ~ value ~ "-" ~ value ~ ")" ^^ { case c ~ _ ~ from ~ _ ~ to ~ _ => ValueComparison(RangeValue(from, to), Some(c)) } |
      valueComparator ~ "(" ~ value ~ ")" ^^ { case c ~ _ ~ v ~ _ => ValueComparison(v, Some(c)) } |
      value ~ "-" ~ value ^^ { case from ~ _ ~ to => ValueComparison(RangeValue(from, to), None) } |
      value ^^ (v => ValueComparison(v, None))

  private def valueComparator: Parser[ValueComparator] =
    comparatorFunction ^^ (c => ValueComparator(c))

  private def value: Parser[Value] =
    date ^^ (x => DateValue(DateTime.parse(x))) |
      "true" ^^^ BooleanValue(true) |
      "false" ^^^ BooleanValue(false) |
      "null" ^^^ NullValue(null) |
      decimalNumber ^^ (x => DecimalNumberValue(BigDecimal(x))) |
      longNumber ^^ (x => LongNumberValue(x.toLong)) |
      regexLiteral ^^ (x => RegexValue(x))

  private def comparatorFunction: Parser[ComparatorFunction] =
    "~gte" ^^^ GTE |
      "~gt" ^^^ GT |
      "~lte" ^^^ LTE |
      "~lt" ^^^ LT |
      "~!" ^^^ NOT

  private def attribute: Parser[Attribute] =
    """([^=\s]*)""".r ^^ { case value => Attribute(value) }

}
