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

import java.util.function.Supplier

import ch.openolitor.util.jsonpath.AST._
import spray.json.{ JsArray, JsObject, JsValue }

import scala.math.abs

final case class JPError(reason: String)

/**
 * Originally token from gatlin-jsonpath and converted to spray-json
 * https://github.com/gatling/gatling/tree/master/gatling-jsonpath
 */
object JsonPath {
  private val JsonPathParser = ThreadLocal.withInitial[OOJsonPathParser](new Supplier[OOJsonPathParser] {
    override def get(): OOJsonPathParser = new OOJsonPathParser()
  })

  def compile(query: String): Either[JPError, JsonPath] =
    JsonPathParser.get.compile(query) match {
      case OOJsonPathParser.Success(q, _) => Right(new JsonPath(q))
      case ns: OOJsonPathParser.NoSuccess => Left(JPError(ns.msg))
    }

  def query(query: String, jsonObject: JsValue): Either[JPError, Vector[JsValue]] = {
    compile(query).map(_.query(jsonObject))
  }
}

class JsonPath(path: List[PathToken]) {
  def query(jsonNode: JsValue): Vector[JsValue] = new JsonPathWalker(jsonNode, path).walk()
}

class JsonPathWalker(rootNode: JsValue, fullPath: List[PathToken]) {

  def walk(): Vector[JsValue] = walk(rootNode, fullPath).toVector

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  private[this] def walk(node: JsValue, path: List[PathToken]): Iterator[JsValue] = {
    path match {
      case head :: tail => walk1(node, head).flatMap(walk(_, tail))
      case _            => Iterator.single(node)
    }
  }

  private[this] def walk1(node: JsValue, query: PathToken): Iterator[JsValue] =
    query match {
      case RootNode    => Iterator.single(rootNode)

      case CurrentNode => Iterator.single(node)

      case Field(name) =>
        node match {
          case JsObject(fields) =>
            fields.get(name).map(child => Iterator.single(child)).getOrElse(Iterator.empty)
          case _ => Iterator.empty
        }

      case RecursiveField(name) => new RecursiveFieldIterator(node, name)

      case MultiField(fieldNames) =>
        node match {
          case JsObject(fields) =>
            // don't use collect on iterator with filter causes (executed twice)
            fieldNames.iterator.flatMap { name =>
              fields.get(name) match {
                case Some(child) => List(child)
                case _           => Nil
              }
            }
          case _ => Iterator.empty
        }

      case AnyField =>
        node match {
          case JsObject(fields) =>
            fields.valuesIterator
          case _ => Iterator.empty
        }

      case ArraySlice(None, None, 1) =>
        node match {
          case JsArray(elements) => elements.iterator
          case _                 => Iterator.empty
        }

      case ArraySlice(start, stop, step) =>
        node match {
          case JsArray(elements) => sliceArray(elements, start, stop, step)
          case _                 => Iterator.empty
        }

      case ArrayRandomAccess(indices) =>
        node match {
          case JsArray(elements) =>
            indices.iterator.collect {
              case i if i >= 0 && i < elements.size  => elements(i)
              case i if i < 0 && i >= -elements.size => elements(i + elements.size)
            }
          case _ => Iterator.empty
        }

      case RecursiveFilterToken(filterToken) => new RecursiveDataIterator(node).flatMap(applyFilter(_, filterToken))

      case filterToken: FilterToken          => applyFilter(node, filterToken)

      case RecursiveAnyField                 => new RecursiveNodeIterator(node)
    }

  private[this] def applyFilter(currentNode: JsValue, filterToken: FilterToken): Iterator[JsValue] = {

    def resolveSubQuery(node: JsValue, q: List[AST.PathToken], nextOp: JsValue => Boolean): Boolean = {
      val it = walk(node, q)
      it.hasNext && nextOp(it.next())
    }

    def applyBinaryOpWithResolvedLeft(node: JsValue, op: ComparisonOperator, lhsNode: JsValue, rhs: FilterValue): Boolean =
      rhs match {
        case FilterDirectValue(valueNode) => op(lhsNode, valueNode)
        case SubQuery(q)                  => resolveSubQuery(node, q, op(lhsNode, _))
      }

    def applyBinaryOp(op: ComparisonOperator, lhs: FilterValue, rhs: FilterValue): JsValue => Boolean =
      lhs match {
        case FilterDirectValue(valueNode) => applyBinaryOpWithResolvedLeft(_, op, valueNode, rhs)
        case SubQuery(q)                  => node => resolveSubQuery(node, q, applyBinaryOpWithResolvedLeft(node, op, _, rhs))
      }

    def elementsToFilter(node: JsValue): Iterator[JsValue] =
      node match {
        case JsArray(elements) => elements.iterator
        case JsObject(_)       => Iterator.single(node)
        case _                 => Iterator.empty
      }

    @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
    def evaluateFilter(filterToken: FilterToken): JsValue => Boolean =
      filterToken match {
        case HasFilter(subQuery) =>
          walk(_, subQuery.path).hasNext

        case ComparisonFilter(op, lhs, rhs) =>
          applyBinaryOp(op, lhs, rhs)

        case BooleanFilter(op, filter1, filter2) =>
          val f1 = evaluateFilter(filter1)
          val f2 = evaluateFilter(filter2)
          node => op(f1(node), f2(node))
      }

    val filterFunction = evaluateFilter(filterToken)
    elementsToFilter(currentNode).filter(filterFunction)
  }

  private[this] def sliceArray(array: Vector[JsValue], start: Option[Int], stop: Option[Int], step: Int): Iterator[JsValue] = {
    val size = array.size

    def lenRelative(x: Int) = if (x >= 0) x else size + x
    def stepRelative(x: Int) = if (step >= 0) x else -1 - x
    def relative(x: Int) = lenRelative(stepRelative(x))

    val absStart = start match {
      case Some(v) => relative(v)
      case _       => 0
    }
    val absEnd = stop match {
      case Some(v) => relative(v)
      case _       => size
    }
    val absStep = abs(step)

    val elements: Iterator[JsValue] = if (step < 0) Iterator.range(array.size - 1, -1, -1).map(array) else array.iterator
    val fromStartToEnd = elements.slice(absStart, absEnd)

    if (absStep == 1) {
      fromStartToEnd
    } else {
      fromStartToEnd.grouped(absStep).map(_.head)
    }
  }
}
