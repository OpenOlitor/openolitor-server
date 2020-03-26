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

sealed trait VisitedIterator {
  def hasNext: Boolean
}
final case class VisitedObject(it: Iterator[(String, JsValue)]) extends VisitedIterator {
  override def hasNext: Boolean = it.hasNext
}
final case class VisitedArray(it: Iterator[JsValue]) extends VisitedIterator {
  override def hasNext: Boolean = it.hasNext
}

/**
 * Collect all first nodes in a branch with a given name
 * @param root the tree root
 * @param name the searched name
 *
 *  Originally token from gatlin-jsonpath and converted to spray-json
 *  https://github.com/gatling/gatling/tree/master/gatling-jsonpath
 */
class RecursiveFieldIterator(root: JsValue, name: String) extends RecursiveIterator[VisitedIterator](root) {

  override def visit(t: VisitedIterator): Unit = t match {
    case VisitedObject(it) => visitObject(it)
    case VisitedArray(it)  => visitArray(it)
  }

  private def visitObject(it: Iterator[(String, JsValue)]): Unit = {
    while (it.hasNext && !pause) {
      val e = it.next()
      if (e._1 == name) {
        nextNode = e._2
        pause = true
      } else {
        visitNode(e._2)
      }
    }
    if (!pause) {
      stack = stack.tail
    }
  }

  private def visitArray(it: Iterator[JsValue]): Unit = {
    while (it.hasNext && !pause) {
      visitNode(it.next())
    }
    if (!pause) {
      stack = stack.tail
    }
  }

  protected def visitNode(node: JsValue): Unit =
    node match {
      case JsObject(fields) =>
        val it = fields.iterator
        stack = VisitedObject(it) :: stack
        visitObject(it)
      case JsArray(elements) =>
        val it = elements.iterator
        stack = VisitedArray(it) :: stack
        visitArray(it)
      case _ =>
    }
}