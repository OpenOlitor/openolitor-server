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

import scala.collection.immutable.HashSet

object ProductUtil {
  import StringUtil._

  val defaultMethods: HashSet[String] = HashSet(
    "hashCode",
    "toString",
    "productArity",
    "productPrefix",
    "productIterator"
  )

  implicit class Product2MapSupport(self: Product) {

    /**
     * Converts a product to a nested Map consisting of properties and names as key-value pairs.
     * i.e.
     * SimpleCaseClass(string: String, int: Int)
     * val scc = SimpleCaseClass("anyString", 0)
     * ssc.toMap
     *
     * should result in:
     * Map(
     * 		"string" -> "anyString",
     * 		"int" -> 0
     * )
     *
     * @param customConverter A customConverter partial function can be configured to convert the values before appending to the
     * result map
     */
    def toMap(customConverter: PartialFunction[Any, Any] = Map.empty): Map[String, Any] = {
      val defaultMapper: PartialFunction[Any, Any] = { case x => x }
      val converters = customConverter orElse defaultMapper
      def toVals(x: Any): Any = x match {
        case t: Traversable[_]                => t.map(toVals(_))
        case o: Option[_]                     => o.map(toVals(_)).getOrElse(converters(None))
        case p: Product if p.productArity > 0 => p.toMap(customConverter)
        case x                                => converters(x)
      }

      val fieldsAsPairs = for {
        field <- self.getClass.getDeclaredFields
        // ignore system fields
        if (!field.getName.contains("$"))
      } yield {
        field.setAccessible(true)

        val fieldValue = toVals(field.get(self))
        (field.getName, fieldValue)
      }

      // extract values returned from defs
      val methodsAsPairs: Seq[Option[(String, Any)]] = for {
        method <- self.getClass.getDeclaredMethods
        // only conside methods without parameters
        if (method.getParameterCount == 0)
        // ignore system methods
        if (!method.getName.contains("$"))
        // ignore other known default methods
        if (!defaultMethods.contains(method.getName))
      } yield {
        method.setAccessible(true)
        try {
          val returnValue = toVals(method.invoke(self))
          Some((method.getName, returnValue))
        } catch {
          case e: Exception => None
        }
      }
      // concat field values and method return types, append methods after field that hidden methods for
      // lazy vals will overrite field value which will always be set to null
      val allTuples = fieldsAsPairs ++ methodsAsPairs.flatten

      Map(allTuples: _*)
    }
  }
}