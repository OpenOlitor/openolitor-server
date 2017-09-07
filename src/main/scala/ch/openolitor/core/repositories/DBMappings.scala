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
package ch.openolitor.core.repositories

import scalikejdbc._
import ch.openolitor.core.models.BaseId
import java.util.UUID
import org.joda.time.DateTime
import org.joda.time.LocalDate
import ch.openolitor.core.models.PersonId
import scala.collection.immutable.TreeMap
import ch.openolitor.core.models.PersonId
import java.util.Locale
import ch.openolitor.core.models.BaseStringId

trait DBMappings extends BaseParameter
    with Parameters
    with Parameters23
    with Parameters24
    with Parameters25
    with Parameters26
    with Parameters27
    with Parameters28
    with LowPriorityImplicitsParameterBinderFactory1 {
  import TypeBinder._
  import ParameterBinderFactory._

  // ParameterBinder
  implicit def baseIdParameterBinderFactory[T <: BaseId]: ParameterBinderFactory[T] = longParameterBinderFactory.contramap(_.id)
  implicit def baseStringIdParameterBinderFactory[T <: BaseStringId]: ParameterBinderFactory[T] = stringParameterBinderFactory.contramap(_.id)
  def toStringParameterBinderFactory[V]: ParameterBinderFactory[V] = stringParameterBinderFactory.contramap(_.toString)
  def seqSqlParameterBinderFactory[V](g: V => String): ParameterBinderFactory[Seq[V]] = stringParameterBinderFactory.contramap({ values => values map (g) mkString (",") })
  def setSqlParameterBinderFactory[V](g: V => String): ParameterBinderFactory[Set[V]] = stringParameterBinderFactory.contramap({ values => values map (g) mkString (",") })
  def seqBaseIdParameterBinderFactory[T <: BaseId](f: Long => T): ParameterBinderFactory[Seq[T]] = seqSqlParameterBinderFactory[T](_.id.toString)
  def setBaseIdParameterBinderFactory[T <: BaseId](f: Long => T): ParameterBinderFactory[Set[T]] = setSqlParameterBinderFactory[T](_.id.toString)
  def setBaseStringIdParameterBinderFactory[T <: BaseStringId]: ParameterBinderFactory[Set[T]] = setSqlParameterBinderFactory[T](_.id)
  def treeMapParameterBinderFactory[K: Ordering, V](kg: K => String, vg: V => String): ParameterBinderFactory[TreeMap[K, V]] =
    stringParameterBinderFactory.contramap({ map =>
      map.toIterable.map { case (k, v) => kg(k) + "=" + vg(v) }.mkString(",")
    })
  def mapParameterBinderFactory[K, V](kg: K => String, vg: V => String): ParameterBinderFactory[Map[K, V]] =
    stringParameterBinderFactory.contramap({ map =>
      map.toIterable.map { case (k, v) => kg(k) + "=" + vg(v) }.mkString(",")
    })

  implicit val localeParameterBinderFactory: ParameterBinderFactory[Locale] = stringParameterBinderFactory.contramap(_.toLanguageTag)
  // implicit val personIdParameterBinderFactory: ParameterBinderFactory[PersonId] = baseIdParameterBinderFactory

  implicit val charArrayParameterBinderFactory: ParameterBinderFactory[Array[Char]] = stringParameterBinderFactory.contramap(x => new String(x))

  implicit val stringSeqParameterBinderFactory: ParameterBinderFactory[Seq[String]] = seqSqlParameterBinderFactory(identity)
  implicit val stringSetParameterBinderFactory: ParameterBinderFactory[Set[String]] = setSqlParameterBinderFactory(identity)

  // TypeBinder
  def baseIdTypeBinder[T <: BaseId](f: Long => T): TypeBinder[T] = TypeBinder.long.map(l => f(l))
  def baseStringIdTypeBinder[T <: BaseStringId](f: String => T): TypeBinder[T] = TypeBinder.string.map(l => f(l))
  def optionBaseIdTypeBinder[T <: BaseId](f: Long => T): TypeBinder[Option[T]] = TypeBinder.optionLong.map(_.map(l => f(l)))
  def toStringTypeBinder[V](f: String => V): TypeBinder[V] = TypeBinder.string.map(f(_))
  def seqSqlTypeBinder[V](f: String => V): TypeBinder[Seq[V]] = TypeBinder.string.map({ x => x.split(",") map (f) })
  def setSqlTypeBinder[V](f: String => V): TypeBinder[Set[V]] = TypeBinder.string.map({ x => x.split(",") map (f) toSet })
  def seqBaseIdTypeBinder[T <: BaseId](f: Long => T): TypeBinder[Seq[T]] = seqSqlTypeBinder[T](l => f(l.toLong))
  def setBaseIdTypeBinder[T <: BaseId](f: Long => T): TypeBinder[Set[T]] = setSqlTypeBinder[T](l => f(l.toLong))
  def setBaseStringIdTypeBinder[T <: BaseStringId](f: String => T): TypeBinder[Set[T]] = setSqlTypeBinder[T](l => f(l))
  def treeMapTypeBinder[K: Ordering, V](kf: String => K, vf: String => V): TypeBinder[TreeMap[K, V]] =
    TypeBinder.string.map({ s =>
      (TreeMap.empty[K, V] /: s.split(",")) { (tree, str) =>
        str.split("=") match {
          case Array(left, right) =>
            tree + (kf(left) -> vf(right))
          case _ =>
            tree
        }
      }
    })
  def mapTypeBinder[K, V](kf: String => K, vf: String => V): TypeBinder[Map[K, V]] =
    TypeBinder.string.map({ s =>
      (Map.empty[K, V] /: s.split(",")) { (tree, str) =>
        str.split("=") match {
          case Array(left, right) =>
            tree + (kf(left) -> vf(right))
          case _ =>
            tree
        }
      }
    })

  implicit val localeTypeBinder: TypeBinder[Locale] = TypeBinder.string.map(l => Locale.forLanguageTag(l))
  implicit val personIdTypeBinder: TypeBinder[PersonId] = baseIdTypeBinder(PersonId.apply _)

  implicit val charArrayBinder: TypeBinder[Array[Char]] = TypeBinder.string.map(_.toCharArray)
  implicit val optionCharArrayBinder: TypeBinder[Option[Array[Char]]] = TypeBinder.string.map(s => Option(s).map(_.toCharArray))

  implicit val stringSeqTypeBinder: TypeBinder[Seq[String]] = seqSqlTypeBinder(identity)
  implicit val stringSetTypeBinder: TypeBinder[Set[String]] = setSqlTypeBinder(identity)

}
