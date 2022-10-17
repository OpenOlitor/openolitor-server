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

import spray.json.JsValue

import scala.collection.AbstractIterator

/**
 * Originally token from gatlin-jsonpath and converted to spray-json
 * https://github.com/gatling/gatling/tree/master/gatling-jsonpath
 */
abstract class RecursiveIterator[T](root: JsValue) extends AbstractIterator[JsValue] {

  protected var nextNode: JsValue = _
  protected var finished: Boolean = _
  protected var pause: Boolean = _
  protected var stack: List[T] = _

  protected def visitNode(node: JsValue): Unit

  protected def visit(t: T): Unit

  override def hasNext: Boolean =
    (nextNode != null && !finished) || {
      pause = false
      if (stack == null) {
        // first access
        stack = Nil
        visitNode(root)
      } else {
        // resuming
        while (!pause && stack.nonEmpty) {
          visit(stack.head)
        }
      }

      finished = nextNode == null
      !finished
    }

  override def next(): JsValue =
    if (finished) {
      throw new UnsupportedOperationException("Can't call next on empty Iterator")
    } else if (nextNode == null) {
      throw new UnsupportedOperationException("Can't call next without calling hasNext first")
    } else {
      val consumed = nextNode
      nextNode = null
      consumed
    }
}