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

import spray.json.{ JsArray, JsObject, JsValue }

/**
 * Collect all nodes data (objects and leaves)
 *
 * @param root the tree root
 *
 * Originally token from gatlin-jsonpath and converted to spray-json
 */
class RecursiveDataIterator(root: JsValue) extends RecursiveIterator[Iterator[JsValue]](root) {

  override protected def visit(it: Iterator[JsValue]): Unit = {
    while (it.hasNext && !pause) {
      visitNode(it.next())
    }
    if (!pause) {
      stack = stack.tail
    }
  }

  override protected def visitNode(node: JsValue): Unit =
    node match {
      case JsObject(fields) =>
        if (fields.size > 0) {
          // only non empty objects
          val it = fields.values.iterator
          stack = it :: stack
          nextNode = node
          pause = true
        }

      case JsArray(elements) =>
        val it = elements.iterator
        stack = it :: stack
        visit(it)
      case _ =>
        nextNode = node
        pause = true
    }
}
