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
  import Binders._

  def baseIdBinders[T <: BaseId](f: Long => T): Binders[T] = Binders.long.xmap(l => f(l), _.id)
  def baseStringIdBinders[T <: BaseStringId](f: String => T): Binders[T] = Binders.string.xmap(l => f(l), _.id)
  def optionBaseIdBinders[T <: BaseId](f: Long => T): Binders[Option[T]] = Binders.optionLong.xmap(_.map(l => f(l)), _.map(_.id))
  def toStringBinder[V](f: String => V): Binders[V] = Binders.string.xmap(f(_), _.toString)
  def seqSqlBinder[V](f: String => V, g: V => String): Binders[Seq[V]] = Binders.string.xmap({
    Option(_) match {
      case None => Seq() // TODO change all the seq fields to not null default ""
      case Some(x) =>
        if (x.isEmpty) {
          Seq()
        } else {
          x.split(",") map (f) toSeq
        }
    }
  }, { values => values map (g) mkString (",") })
  def setSqlBinder[V](f: String => V, g: V => String): Binders[Set[V]] = Binders.string.xmap({
    Option(_) match {
      case None => Set() // TODO change all the seq fields to not null default ""
      case Some(x) =>
        if (x.isEmpty) {
          Set()
        } else {
          x.split(",") map (f) toSet
        }
    }
  }, { values => values map (g) mkString (",") })
  def seqBaseIdBinders[T <: BaseId](f: Long => T): Binders[Seq[T]] = seqSqlBinder[T](l => f(l.toLong), _.id.toString)
  def setBaseIdBinders[T <: BaseId](f: Long => T): Binders[Set[T]] = setSqlBinder[T](l => f(l.toLong), _.id.toString)
  def setBaseStringIdBinders[T <: BaseStringId](f: String => T): Binders[Set[T]] = setSqlBinder[T](l => f(l), _.id)
  def treeMapBinders[K: Ordering, V](kf: String => K, vf: String => V, kg: K => String, vg: V => String): Binders[TreeMap[K, V]] =
    Binders.string.xmap({
      Option(_) match {
        case None => TreeMap.empty[K, V]
        case Some(s) =>
          (TreeMap.empty[K, V] /: s.split(",")) { (tree, str) =>
            str.split("=") match {
              case Array(left, right) =>
                tree + (kf(left) -> vf(right))
              case _ =>
                tree
            }
          }
      }
    }, { map =>
      map.toIterable.map { case (k, v) => kg(k) + "=" + vg(v) }.mkString(",")
    })
  def mapBinders[K, V](kf: String => K, vf: String => V, kg: K => String, vg: V => String): Binders[Map[K, V]] =
    Binders.string.xmap({
      Option(_) match {
        case None => Map.empty[K, V]
        case Some(s) =>
          (Map.empty[K, V] /: s.split(",")) { (tree, str) =>
            str.split("=") match {
              case Array(left, right) =>
                tree + (kf(left) -> vf(right))
              case _ =>
                tree
            }
          }
      }
    }, { map =>
      map.toIterable.map { case (k, v) => kg(k) + "=" + vg(v) }.mkString(",")
    })

  implicit val localeBinder: Binders[Locale] = Binders.string.xmap(l => Locale.forLanguageTag(l), _.toLanguageTag)
  implicit val personIdBinder: Binders[PersonId] = baseIdBinders(PersonId.apply _)
  implicit val optionPersonIdBinder: Binders[Option[PersonId]] = optionBaseIdBinders(PersonId.apply _)

  implicit val charArrayBinder: Binders[Array[Char]] = Binders.string.xmap(_.toCharArray, x => new String(x))
  implicit val optionCharArrayBinder: Binders[Option[Array[Char]]] = Binders.string.xmap(s => Option(s).map(_.toCharArray), _.map(x => new String(x)).getOrElse(null))

  implicit val stringSeqBinders: Binders[Seq[String]] = seqSqlBinder(identity, identity)
  implicit val stringSetBinders: Binders[Set[String]] = setSqlBinder(identity, identity)

  // low level binders
  import TypeBinder._
  implicit val stringBinder = Binders.string
  implicit val optionStringBinder = Binders.option[String]
  implicit val intBinder = Binders.int
  implicit val optionIntBinder = Binders.optionInt
  implicit val longBinder = Binders.long
  implicit val optionLongBinder = Binders.optionLong
  implicit val shortBinder = Binders.short
  implicit val optionShortBinder = Binders.optionShort
  implicit val floatBinder = Binders.float
  implicit val optionFloatBinder = Binders.optionFloat
  implicit val doubleBinder = Binders.double
  implicit val optionDoubleBinder = Binders.optionDouble
  implicit val booleanBinder = Binders.boolean
  implicit val optionBooleanBinder = Binders.optionBoolean
  implicit val localDateBinder = Binders.jodaLocalDate
  implicit val optionLocalDateBinder = Binders.option[LocalDate]
  implicit val datetimeBinder = Binders.jodaDateTime
  implicit val optionDatetimeBinder = Binders.option[DateTime]
  implicit val bigDecimalBinder = Binders.bigDecimal
  implicit val optionBigDecimalBinder = Binders.option[BigDecimal]

  // low level parameterbinderfactories

  implicit class OneToXSQLPlus[A, E <: WithExtractor, Z](oneToXSQL: OneToXSQL[A, E, Z]) {
    def toManies[B1, B2, B3, B4, B5, B6, B7, B8, B9, B10](
      to1: WrappedResultSet => Option[B1],
      to2: WrappedResultSet => Option[B2],
      to3: WrappedResultSet => Option[B3],
      to4: WrappedResultSet => Option[B4],
      to5: WrappedResultSet => Option[B5],
      to6: WrappedResultSet => Option[B6],
      to7: WrappedResultSet => Option[B7],
      to8: WrappedResultSet => Option[B8],
      to9: WrappedResultSet => Option[B9],
      to10: WrappedResultSet => Option[B10]
    ): OneToManies10SQL[A, B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, E, Z] = {
      val q: OneToManies10SQL[A, B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, E, Z] = new OneToManies10SQL(
        oneToXSQL.statement, oneToXSQL.rawParameters
      )(oneToXSQL.one)(to1, to2, to3, to4, to5, to6, to7, to8, to9, to10)((a, bs1, bs2, bs3, bs4, bs5, bs6, bs7, bs8, bs9, bs10) => a.asInstanceOf[Z])
      q.queryTimeout(oneToXSQL.queryTimeout)
      q.fetchSize(oneToXSQL.fetchSize)
      q.tags(oneToXSQL.tags: _*)
    }

    def toManies[B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11](
      to1: WrappedResultSet => Option[B1],
      to2: WrappedResultSet => Option[B2],
      to3: WrappedResultSet => Option[B3],
      to4: WrappedResultSet => Option[B4],
      to5: WrappedResultSet => Option[B5],
      to6: WrappedResultSet => Option[B6],
      to7: WrappedResultSet => Option[B7],
      to8: WrappedResultSet => Option[B8],
      to9: WrappedResultSet => Option[B9],
      to10: WrappedResultSet => Option[B10],
      to11: WrappedResultSet => Option[B11]

    ): OneToManies11SQL[A, B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11, E, Z] = {
      val q: OneToManies11SQL[A, B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11, E, Z] = new OneToManies11SQL(
        oneToXSQL.statement, oneToXSQL.rawParameters
      )(oneToXSQL.one)(to1, to2, to3, to4, to5, to6, to7, to8, to9, to10, to11)((a, bs1, bs2, bs3, bs4, bs5, bs6, bs7, bs8, bs9, bs10, bs11) => a.asInstanceOf[Z])
      q.queryTimeout(oneToXSQL.queryTimeout)
      q.fetchSize(oneToXSQL.fetchSize)
      q.tags(oneToXSQL.tags: _*)
    }

    def toManies[B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11, B12](
      to1: WrappedResultSet => Option[B1],
      to2: WrappedResultSet => Option[B2],
      to3: WrappedResultSet => Option[B3],
      to4: WrappedResultSet => Option[B4],
      to5: WrappedResultSet => Option[B5],
      to6: WrappedResultSet => Option[B6],
      to7: WrappedResultSet => Option[B7],
      to8: WrappedResultSet => Option[B8],
      to9: WrappedResultSet => Option[B9],
      to10: WrappedResultSet => Option[B10],
      to11: WrappedResultSet => Option[B11],
      to12: WrappedResultSet => Option[B12]

    ): OneToManies12SQL[A, B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11, B12, E, Z] = {
      val q: OneToManies12SQL[A, B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11, B12, E, Z] = new OneToManies12SQL(
        oneToXSQL.statement, oneToXSQL.rawParameters
      )(oneToXSQL.one)(to1, to2, to3, to4, to5, to6, to7, to8, to9, to10, to11, to12)((a, bs1, bs2, bs3, bs4, bs5, bs6, bs7, bs8, bs9, bs10, bs11, bs12) => a.asInstanceOf[Z])
      q.queryTimeout(oneToXSQL.queryTimeout)
      q.fetchSize(oneToXSQL.fetchSize)
      q.tags(oneToXSQL.tags: _*)
    }

    def toManies[B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11, B12, B13](
      to1: WrappedResultSet => Option[B1],
      to2: WrappedResultSet => Option[B2],
      to3: WrappedResultSet => Option[B3],
      to4: WrappedResultSet => Option[B4],
      to5: WrappedResultSet => Option[B5],
      to6: WrappedResultSet => Option[B6],
      to7: WrappedResultSet => Option[B7],
      to8: WrappedResultSet => Option[B8],
      to9: WrappedResultSet => Option[B9],
      to10: WrappedResultSet => Option[B10],
      to11: WrappedResultSet => Option[B11],
      to12: WrappedResultSet => Option[B12],
      to13: WrappedResultSet => Option[B13]

    ): OneToManies13SQL[A, B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11, B12, B13, E, Z] = {
      val q: OneToManies13SQL[A, B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11, B12, B13, E, Z] = new OneToManies13SQL(
        oneToXSQL.statement, oneToXSQL.rawParameters
      )(oneToXSQL.one)(to1, to2, to3, to4, to5, to6, to7, to8, to9, to10, to11, to12, to13)((a, bs1, bs2, bs3, bs4, bs5, bs6, bs7, bs8, bs9, bs10, bs11, bs12, bs13) => a.asInstanceOf[Z])
      q.queryTimeout(oneToXSQL.queryTimeout)
      q.fetchSize(oneToXSQL.fetchSize)
      q.tags(oneToXSQL.tags: _*)
    }

    def toManies[B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11, B12, B13, B14](
      to1: WrappedResultSet => Option[B1],
      to2: WrappedResultSet => Option[B2],
      to3: WrappedResultSet => Option[B3],
      to4: WrappedResultSet => Option[B4],
      to5: WrappedResultSet => Option[B5],
      to6: WrappedResultSet => Option[B6],
      to7: WrappedResultSet => Option[B7],
      to8: WrappedResultSet => Option[B8],
      to9: WrappedResultSet => Option[B9],
      to10: WrappedResultSet => Option[B10],
      to11: WrappedResultSet => Option[B11],
      to12: WrappedResultSet => Option[B12],
      to13: WrappedResultSet => Option[B13],
      to14: WrappedResultSet => Option[B14]

    ): OneToManies14SQL[A, B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11, B12, B13, B14, E, Z] = {
      val q: OneToManies14SQL[A, B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11, B12, B13, B14, E, Z] = new OneToManies14SQL(
        oneToXSQL.statement, oneToXSQL.rawParameters
      )(oneToXSQL.one)(to1, to2, to3, to4, to5, to6, to7, to8, to9, to10, to11, to12, to13, to14)((a, bs1, bs2, bs3, bs4, bs5, bs6, bs7, bs8, bs9, bs10, bs11, bs12, bs13, bs14) => a.asInstanceOf[Z])
      q.queryTimeout(oneToXSQL.queryTimeout)
      q.fetchSize(oneToXSQL.fetchSize)
      q.tags(oneToXSQL.tags: _*)
    }

    def toManies[B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11, B12, B13, B14, B15](
      to1: WrappedResultSet => Option[B1],
      to2: WrappedResultSet => Option[B2],
      to3: WrappedResultSet => Option[B3],
      to4: WrappedResultSet => Option[B4],
      to5: WrappedResultSet => Option[B5],
      to6: WrappedResultSet => Option[B6],
      to7: WrappedResultSet => Option[B7],
      to8: WrappedResultSet => Option[B8],
      to9: WrappedResultSet => Option[B9],
      to10: WrappedResultSet => Option[B10],
      to11: WrappedResultSet => Option[B11],
      to12: WrappedResultSet => Option[B12],
      to13: WrappedResultSet => Option[B13],
      to14: WrappedResultSet => Option[B14],
      to15: WrappedResultSet => Option[B15]

    ): OneToManies15SQL[A, B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11, B12, B13, B14, B15, E, Z] = {
      val q: OneToManies15SQL[A, B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11, B12, B13, B14, B15, E, Z] = new OneToManies15SQL(
        oneToXSQL.statement, oneToXSQL.rawParameters
      )(oneToXSQL.one)(to1, to2, to3, to4, to5, to6, to7, to8, to9, to10, to11, to12, to13, to14, to15)((a, bs1, bs2, bs3, bs4, bs5, bs6, bs7, bs8, bs9, bs10, bs11, bs12, bs13, bs14, bs15) => a.asInstanceOf[Z])
      q.queryTimeout(oneToXSQL.queryTimeout)
      q.fetchSize(oneToXSQL.fetchSize)
      q.tags(oneToXSQL.tags: _*)
    }

    def toManies[B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11, B12, B13, B14, B15, B16](
      to1: WrappedResultSet => Option[B1],
      to2: WrappedResultSet => Option[B2],
      to3: WrappedResultSet => Option[B3],
      to4: WrappedResultSet => Option[B4],
      to5: WrappedResultSet => Option[B5],
      to6: WrappedResultSet => Option[B6],
      to7: WrappedResultSet => Option[B7],
      to8: WrappedResultSet => Option[B8],
      to9: WrappedResultSet => Option[B9],
      to10: WrappedResultSet => Option[B10],
      to11: WrappedResultSet => Option[B11],
      to12: WrappedResultSet => Option[B12],
      to13: WrappedResultSet => Option[B13],
      to14: WrappedResultSet => Option[B14],
      to15: WrappedResultSet => Option[B15],
      to16: WrappedResultSet => Option[B16]

    ): OneToManies16SQL[A, B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11, B12, B13, B14, B15, B16, E, Z] = {
      val q: OneToManies16SQL[A, B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11, B12, B13, B14, B15, B16, E, Z] = new OneToManies16SQL(
        oneToXSQL.statement, oneToXSQL.rawParameters
      )(oneToXSQL.one)(to1, to2, to3, to4, to5, to6, to7, to8, to9, to10, to11, to12, to13, to14, to15, to16)((a, bs1, bs2, bs3, bs4, bs5, bs6, bs7, bs8, bs9, bs10, bs11, bs12, bs13, bs14, bs15, bs16) => a.asInstanceOf[Z])
      q.queryTimeout(oneToXSQL.queryTimeout)
      q.fetchSize(oneToXSQL.fetchSize)
      q.tags(oneToXSQL.tags: _*)
    }

    def toManies[B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11, B12, B13, B14, B15, B16, B17](
      to1: WrappedResultSet => Option[B1],
      to2: WrappedResultSet => Option[B2],
      to3: WrappedResultSet => Option[B3],
      to4: WrappedResultSet => Option[B4],
      to5: WrappedResultSet => Option[B5],
      to6: WrappedResultSet => Option[B6],
      to7: WrappedResultSet => Option[B7],
      to8: WrappedResultSet => Option[B8],
      to9: WrappedResultSet => Option[B9],
      to10: WrappedResultSet => Option[B10],
      to11: WrappedResultSet => Option[B11],
      to12: WrappedResultSet => Option[B12],
      to13: WrappedResultSet => Option[B13],
      to14: WrappedResultSet => Option[B14],
      to15: WrappedResultSet => Option[B15],
      to16: WrappedResultSet => Option[B16],
      to17: WrappedResultSet => Option[B17]

    ): OneToManies17SQL[A, B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11, B12, B13, B14, B15, B16, B17, E, Z] = {
      val q: OneToManies17SQL[A, B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11, B12, B13, B14, B15, B16, B17, E, Z] = new OneToManies17SQL(
        oneToXSQL.statement, oneToXSQL.rawParameters
      )(oneToXSQL.one)(to1, to2, to3, to4, to5, to6, to7, to8, to9, to10, to11, to12, to13, to14, to15, to16, to17)((a, bs1, bs2, bs3, bs4, bs5, bs6, bs7, bs8, bs9, bs10, bs11, bs12, bs13, bs14, bs15, bs16, bs17) => a.asInstanceOf[Z])
      q.queryTimeout(oneToXSQL.queryTimeout)
      q.fetchSize(oneToXSQL.fetchSize)
      q.tags(oneToXSQL.tags: _*)
    }

    def toManies[B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11, B12, B13, B14, B15, B16, B17, B18](
      to1: WrappedResultSet => Option[B1],
      to2: WrappedResultSet => Option[B2],
      to3: WrappedResultSet => Option[B3],
      to4: WrappedResultSet => Option[B4],
      to5: WrappedResultSet => Option[B5],
      to6: WrappedResultSet => Option[B6],
      to7: WrappedResultSet => Option[B7],
      to8: WrappedResultSet => Option[B8],
      to9: WrappedResultSet => Option[B9],
      to10: WrappedResultSet => Option[B10],
      to11: WrappedResultSet => Option[B11],
      to12: WrappedResultSet => Option[B12],
      to13: WrappedResultSet => Option[B13],
      to14: WrappedResultSet => Option[B14],
      to15: WrappedResultSet => Option[B15],
      to16: WrappedResultSet => Option[B16],
      to17: WrappedResultSet => Option[B17],
      to18: WrappedResultSet => Option[B18]

    ): OneToManies18SQL[A, B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11, B12, B13, B14, B15, B16, B17, B18, E, Z] = {
      val q: OneToManies18SQL[A, B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11, B12, B13, B14, B15, B16, B17, B18, E, Z] = new OneToManies18SQL(
        oneToXSQL.statement, oneToXSQL.rawParameters
      )(oneToXSQL.one)(to1, to2, to3, to4, to5, to6, to7, to8, to9, to10, to11, to12, to13, to14, to15, to16, to17, to18)((a, bs1, bs2, bs3, bs4, bs5, bs6, bs7, bs8, bs9, bs10, bs11, bs12, bs13, bs14, bs15, bs16, bs17, bs18) => a.asInstanceOf[Z])
      q.queryTimeout(oneToXSQL.queryTimeout)
      q.fetchSize(oneToXSQL.fetchSize)
      q.tags(oneToXSQL.tags: _*)
    }

    def toManies[B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11, B12, B13, B14, B15, B16, B17, B18, B19](
      to1: WrappedResultSet => Option[B1],
      to2: WrappedResultSet => Option[B2],
      to3: WrappedResultSet => Option[B3],
      to4: WrappedResultSet => Option[B4],
      to5: WrappedResultSet => Option[B5],
      to6: WrappedResultSet => Option[B6],
      to7: WrappedResultSet => Option[B7],
      to8: WrappedResultSet => Option[B8],
      to9: WrappedResultSet => Option[B9],
      to10: WrappedResultSet => Option[B10],
      to11: WrappedResultSet => Option[B11],
      to12: WrappedResultSet => Option[B12],
      to13: WrappedResultSet => Option[B13],
      to14: WrappedResultSet => Option[B14],
      to15: WrappedResultSet => Option[B15],
      to16: WrappedResultSet => Option[B16],
      to17: WrappedResultSet => Option[B17],
      to18: WrappedResultSet => Option[B18],
      to19: WrappedResultSet => Option[B19]

    ): OneToManies19SQL[A, B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11, B12, B13, B14, B15, B16, B17, B18, B19, E, Z] = {
      val q: OneToManies19SQL[A, B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11, B12, B13, B14, B15, B16, B17, B18, B19, E, Z] = new OneToManies19SQL(
        oneToXSQL.statement, oneToXSQL.rawParameters
      )(oneToXSQL.one)(to1, to2, to3, to4, to5, to6, to7, to8, to9, to10, to11, to12, to13, to14, to15, to16, to17, to18, to19)((a, bs1, bs2, bs3, bs4, bs5, bs6, bs7, bs8, bs9, bs10, bs11, bs12, bs13, bs14, bs15, bs16, bs17, bs18, bs19) => a.asInstanceOf[Z])
      q.queryTimeout(oneToXSQL.queryTimeout)
      q.fetchSize(oneToXSQL.fetchSize)
      q.tags(oneToXSQL.tags: _*)
    }

    def toManies[B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11, B12, B13, B14, B15, B16, B17, B18, B19, B20](
      to1: WrappedResultSet => Option[B1],
      to2: WrappedResultSet => Option[B2],
      to3: WrappedResultSet => Option[B3],
      to4: WrappedResultSet => Option[B4],
      to5: WrappedResultSet => Option[B5],
      to6: WrappedResultSet => Option[B6],
      to7: WrappedResultSet => Option[B7],
      to8: WrappedResultSet => Option[B8],
      to9: WrappedResultSet => Option[B9],
      to10: WrappedResultSet => Option[B10],
      to11: WrappedResultSet => Option[B11],
      to12: WrappedResultSet => Option[B12],
      to13: WrappedResultSet => Option[B13],
      to14: WrappedResultSet => Option[B14],
      to15: WrappedResultSet => Option[B15],
      to16: WrappedResultSet => Option[B16],
      to17: WrappedResultSet => Option[B17],
      to18: WrappedResultSet => Option[B18],
      to19: WrappedResultSet => Option[B19],
      to20: WrappedResultSet => Option[B20]

    ): OneToManies20SQL[A, B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11, B12, B13, B14, B15, B16, B17, B18, B19, B20, E, Z] = {
      val q: OneToManies20SQL[A, B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11, B12, B13, B14, B15, B16, B17, B18, B19, B20, E, Z] = new OneToManies20SQL(
        oneToXSQL.statement, oneToXSQL.rawParameters
      )(oneToXSQL.one)(to1, to2, to3, to4, to5, to6, to7, to8, to9, to10, to11, to12, to13, to14, to15, to16, to17, to18, to19, to20)((a, bs1, bs2, bs3, bs4, bs5, bs6, bs7, bs8, bs9, bs10, bs11, bs12, bs13, bs14, bs15, bs16, bs17, bs18, bs19, bs20) => a.asInstanceOf[Z])
      q.queryTimeout(oneToXSQL.queryTimeout)
      q.fetchSize(oneToXSQL.fetchSize)
      q.tags(oneToXSQL.tags: _*)
    }

    def toManies[B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11, B12, B13, B14, B15, B16, B17, B18, B19, B20, B21](
      to1: WrappedResultSet => Option[B1],
      to2: WrappedResultSet => Option[B2],
      to3: WrappedResultSet => Option[B3],
      to4: WrappedResultSet => Option[B4],
      to5: WrappedResultSet => Option[B5],
      to6: WrappedResultSet => Option[B6],
      to7: WrappedResultSet => Option[B7],
      to8: WrappedResultSet => Option[B8],
      to9: WrappedResultSet => Option[B9],
      to10: WrappedResultSet => Option[B10],
      to11: WrappedResultSet => Option[B11],
      to12: WrappedResultSet => Option[B12],
      to13: WrappedResultSet => Option[B13],
      to14: WrappedResultSet => Option[B14],
      to15: WrappedResultSet => Option[B15],
      to16: WrappedResultSet => Option[B16],
      to17: WrappedResultSet => Option[B17],
      to18: WrappedResultSet => Option[B18],
      to19: WrappedResultSet => Option[B19],
      to20: WrappedResultSet => Option[B20],
      to21: WrappedResultSet => Option[B21]

    ): OneToManies21SQL[A, B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11, B12, B13, B14, B15, B16, B17, B18, B19, B20, B21, E, Z] = {
      val q: OneToManies21SQL[A, B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11, B12, B13, B14, B15, B16, B17, B18, B19, B20, B21, E, Z] = new OneToManies21SQL(
        oneToXSQL.statement, oneToXSQL.rawParameters
      )(oneToXSQL.one)(to1, to2, to3, to4, to5, to6, to7, to8, to9, to10, to11, to12, to13, to14, to15, to16, to17, to18, to19, to20, to21)((a, bs1, bs2, bs3, bs4, bs5, bs6, bs7, bs8, bs9, bs10, bs11, bs12, bs13, bs14, bs15, bs16, bs17, bs18, bs19, bs20, bs21) => a.asInstanceOf[Z])
      q.queryTimeout(oneToXSQL.queryTimeout)
      q.fetchSize(oneToXSQL.fetchSize)
      q.tags(oneToXSQL.tags: _*)
    }
  }

}
