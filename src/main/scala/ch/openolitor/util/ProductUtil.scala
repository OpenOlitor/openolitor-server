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
  implicit class Product2MapSupport(self: Product) {
    /**
     * Converts a product to a nested Map consisting of properties and names as key-value pairs.
     */
    def toMap: Map[String, Any] = {
      val fieldNames = self.getClass.getDeclaredFields.map(_.getName)

      def toVals(seq: TraversableOnce[Any]): Seq[Any] = (seq map {
        case t: Traversable[_] => toVals(t)
        case p: Product if p.productArity > 0 => p.toMap
        case x => x
      }).toSeq

      val vals = toVals(self.productIterator)

      fieldNames.zip(vals).toMap
    }
  }
}