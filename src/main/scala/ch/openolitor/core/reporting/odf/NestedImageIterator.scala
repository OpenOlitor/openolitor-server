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
package ch.openolitor.core.reporting.odf

import java.util.Iterator

import org.apache.xerces.dom.ParentNode
import org.odftoolkit.odfdom.dom.element.draw.{ DrawFrameElement, DrawImageElement }
import org.odftoolkit.odfdom.pkg._
import org.odftoolkit.simple.draw.Image
import org.w3c.dom.Node

/**
 */
class NestedImageIterator(containerElement: OdfElement) extends Iterator[Image] {

  var nextElement: Option[(Node, Image)] = None;
  var tempElement: Option[(Node, Image)] = None;

  def hasNext(): Boolean = {
    tempElement = findNext(nextElement)
    tempElement.isDefined
  }

  def next(): Image = {
    tempElement.map { e =>
      nextElement = tempElement
      tempElement = None
      e._2
    } orElse {
      nextElement = findNext(nextElement)
      nextElement.map(_._2)
    } getOrElse null
  }

  def remove(): Unit = {
    throw new IllegalStateException("Unsupported operation")
  }

  private def findNext(lastResult: Option[(Node, Image)]): Option[(Node, Image)] = {
    val node = lastResult map (_._1)
    findDeepFirstChildNode[DrawFrameElement](DrawFrameElement.ELEMENT_NAME, containerElement, node) flatMap {
      case (drawFrameNode, nextFrame) =>
        findDeepFirstChildNode[DrawImageElement](DrawImageElement.ELEMENT_NAME, nextFrame, None) collect {
          case (_, nextimage: DrawImageElement) =>
            (drawFrameNode, Image.getInstanceof(nextimage))
        }
    }
  }

  private def findDeepFirstChildNode[T <: OdfElement](name: OdfName, parent: Node, refNode: Option[Node]): Option[(Node, Node)] = {
    val startingNode = Option(refNode.map { node =>
      node.getNextSibling
    }.getOrElse {
      parent.getFirstChild
    })

    startingNode.map { node =>
      findDeepChildNode(name, node) match {
        case r @ Some(result) => r
        case None             => findDeepFirstChildNode(name, parent, Some(node))
      }
    }.getOrElse(None)
  }

  private def findDeepChildNode[T <: OdfElement](
    name: OdfName,
    refNode: Node
  ): Option[(Node, Node)] = {
    refNode match {
      case node: Node if node.getNodeName == name.getQName => Some((node, node))
      case parent: ParentNode =>
        findDeepFirstChildNode(name, parent, None) match {
          case Some((_, c)) => Some((parent, c))
          case None         => None
        }
      case _ => None
    }
  }
}
