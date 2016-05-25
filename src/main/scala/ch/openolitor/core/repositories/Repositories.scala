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

import ch.openolitor.core.models._
import java.util.UUID
import scalikejdbc._
import com.typesafe.scalalogging.LazyLogging
import org.joda.time.DateTime
import ch.openolitor.core.EventStream
import scala.util._
import ch.openolitor.core.scalax._

//case class ParameterBindMapping[A](cl: Class[A], binder: ParameterBinderFactory[A])

trait BaseEntitySQLSyntaxSupport[E <: BaseEntity[_]] extends SQLSyntaxSupport[E] with LazyLogging with DBMappings {
  import ParameterBinderFactory._

  //override def columnNames 
  def apply(p: SyntaxProvider[E])(rs: WrappedResultSet): E = apply(p.resultName)(rs)

  def opt(e: SyntaxProvider[E])(rs: WrappedResultSet): Option[E] = try {
    rs.stringOpt(e.resultName.id).map(_ => apply(e)(rs))
  } catch {
    case e: IllegalArgumentException => None
  }

  def apply(rn: ResultName[E])(rs: WrappedResultSet): E

  /**
   * Declare parameter mappings for all parameters used on insert
   */
  def parameterMappings(entity: E): Seq[Any]

  /**
   * Declare update parameters for this entity used on update. Is by default an empty set
   */
  def updateParameters(entity: E): Seq[Tuple2[SQLSyntax, ParameterBinder]] = Seq(
    column.erstelldat -> entity.erstelldat,
    column.ersteller -> entity.ersteller,
    column.modifidat -> entity.modifidat,
    column.modifikator -> entity.modifikator
  )
}

object BaseRepository extends LazyLogging {
}

trait BaseWriteRepository extends DBMappings with LazyLogging {
  self: EventStream =>

  type Validator[E] = E => Boolean
  val TrueValidator: Validator[Any] = x => true

  def getById[E <: BaseEntity[I], I <: BaseId](syntax: BaseEntitySQLSyntaxSupport[E], id: I)(implicit
    session: DBSession,
    binder: ParameterBinderFactory[I]): Option[E] = {
    val alias = syntax.syntax("x")
    val idx = alias.id
    withSQL {
      select
        .from(syntax as alias)
        .where.eq(alias.id, id)
    }.map(syntax.apply(alias)).single.apply()
  }

  def insertEntity[E <: BaseEntity[I], I <: BaseId](entity: E)(implicit
    session: DBSession,
    binder: ParameterBinderFactory[I],
    syntaxSupport: BaseEntitySQLSyntaxSupport[E],
    user: PersonId) = {
    val params = syntaxSupport.parameterMappings(entity)
    logger.debug(s"create entity with values:$entity")
    getById(syntaxSupport, entity.id) match {
      case Some(x) =>
        logger.debug(s"Ignore insert event, entity already exists:${entity.id}")
      case None =>
        withSQL(insertInto(syntaxSupport).values(params: _*)).update.apply()

        //publish event to stream
        publish(EntityCreated(user, entity))
    }
  }
  def updateEntity[E <: BaseEntity[I], I <: BaseId](entity: E)(implicit
    session: DBSession,
    binder: ParameterBinderFactory[I],
    syntaxSupport: BaseEntitySQLSyntaxSupport[E],
    user: PersonId) = {
    getById(syntaxSupport, entity.id).map { orig =>
      val alias = syntaxSupport.syntax("x")
      val id = alias.id
      val updateParams = syntaxSupport.updateParameters(entity)
      withSQL(update(syntaxSupport as alias).set(updateParams: _*).where.eq(id, entity.id)).update.apply()

      //publish event to stream
      publish(EntityModified(user, entity, orig))

      entity
    }
  }

  def deleteEntity[E <: BaseEntity[I], I <: BaseId](id: I, validator: Validator[E])(implicit
    session: DBSession,
    binder: ParameterBinderFactory[I],
    syntaxSupport: BaseEntitySQLSyntaxSupport[E],
    user: PersonId): Option[E] = {
    deleteEntity[E, I](id, Some(validator))
  }

  def deleteEntity[E <: BaseEntity[I], I <: BaseId](id: I, validator: Option[Validator[E]] = None)(implicit
    session: DBSession,
    binder: ParameterBinderFactory[I],
    syntaxSupport: BaseEntitySQLSyntaxSupport[E],
    user: PersonId): Option[E] = {
    logger.debug(s"delete from ${syntaxSupport.tableName}: $id")
    getById(syntaxSupport, id).map { entity =>
      val validation = validator.getOrElse(TrueValidator)
      validation(entity) match {
        case true =>
          withSQL(deleteFrom(syntaxSupport).where.eq(syntaxSupport.column.id, id)).update.apply()

          //publish event to stream
          publish(EntityDeleted(user, entity))
          Some(entity)
        case false =>
          logger.debug(s"Couldn't delete from ${syntaxSupport.tableName}: $id, validation didn't succeed")
          None
      }
    }.getOrElse(None)
  }
}
