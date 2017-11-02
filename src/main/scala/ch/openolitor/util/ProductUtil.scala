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
package ch.openolitor.util

object ProductUtil {
  import StringUtil._
  implicit class Product2MapSupport(self: Product) {

    /**
     * Converts a product to a nested Map consisting of properties and names as key-value pairs.
     * Root property will be the decapitablized productPrefix of the prodct itself.
     * i.e.
     * SimpleCaseClass(string: String, int: Int)
     * val scc = SimpleCaseClass("anyString", 0)
     * ssc.toMap
     *
     * should result in:
     * Map(
     * 	"simpleCaseClass" -> Map (
     * 		"string" -> "anyString",
     * 		"int" -> 0)
     * )
     *
     * @param customConverter A customConverter partial function can be configured to convert the values before appending to the
     * result map
     */
    def toMap(customConverter: PartialFunction[Any, Any] = Map.empty): Map[String, Any] = {
      Map(self.productPrefix.decapitalize -> asMap(self, customConverter))
    }

    private def asMap(p: Product, customConverter: PartialFunction[Any, Any] = Map.empty): Map[String, Any] = {
      val fieldNames = p.getClass.getDeclaredFields.map(_.getName)

      val defaultMapper: PartialFunction[Any, Any] = { case x => x }
      val converters = customConverter orElse defaultMapper
      def toVals(x: Any): Any = x match {
        case t: Traversable[_] => t.map(toVals(_))
        case p: Product if p.productArity > 0 => asMap(p, customConverter)
        case x => converters(x)
      }

      val vals = p.productIterator.map(toVals).toSeq
      fieldNames.zip(vals).toMap
    }
  }
}