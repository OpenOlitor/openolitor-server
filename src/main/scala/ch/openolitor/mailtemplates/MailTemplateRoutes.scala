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
package ch.openolitor.mailtemplates

import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import ch.openolitor.core._
import ch.openolitor.core.db.AsyncConnectionPoolContextAware
import ch.openolitor.core.filestore.{ DefaultFileStoreComponent, FileStoreComponent }
import ch.openolitor.core.security.Subject
import ch.openolitor.mailtemplates.eventsourcing._
import ch.openolitor.mailtemplates.model._
import ch.openolitor.mailtemplates.repositories.{ DefaultMailTemplateReadRepositoryComponent, MailTemplateDBMappings, MailTemplateReadRepositoryComponent }
import ch.openolitor.util.parsing.{ FilterExpr, UriQueryParamFilterParser }
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext

trait MailTemplateRoutes extends BaseRouteService
  with ActorReferences
  with AsyncConnectionPoolContextAware
  with AkkaHttpDeserializers
  with LazyLogging
  with MailTemplateJsonProtocol
  with MailTemplateDBMappings
  with MailTemplateEventStoreSerializer {
  self: MailTemplateReadRepositoryComponent with FileStoreComponent =>

  implicit val mailTemplateIdPath = long2BaseIdPathMatcher(MailTemplateId.apply)

  def mailRoute(implicit subect: Subject): Route =
    parameter("f".?) { f =>
      implicit val filter = f flatMap { filterString =>
        UriQueryParamFilterParser.parse(filterString)
      }
      mailTemplateRoute
    }

  def mailTemplateRoute(implicit subject: Subject, filter: Option[FilterExpr]): Route =
    path("mailtemplates") {
      get(list(mailTemplateReadRepositoryAsync.getMailTemplates())) ~
        post(create[MailTemplateModify, MailTemplateId](MailTemplateId.apply _))
    } ~
      path("mailtemplates" / mailTemplateIdPath) { mailTemplateId =>
        val impl2 = mailTemplateIdBinder
        val impl = implicitly[scalikejdbc.Binders[MailTemplateId]]

        get(detail(mailTemplateReadRepositoryAsync.getById(mailTemplateMapping, mailTemplateId))) ~
          (put | post)(update[MailTemplateModify, MailTemplateId](mailTemplateId)) ~
          delete(remove(mailTemplateId))
      }

}

class DefaultMailTemplateRoutes(
  override val dbEvolutionActor: ActorRef,
  override val entityStore: ActorRef,
  override val eventStore: ActorRef,
  override val mailService: ActorRef,
  override val reportSystem: ActorRef,
  override val sysConfig: SystemConfig,
  override val system: ActorSystem,
  override val airbrakeNotifier: ActorRef,
  override val jobQueueService: ActorRef
) extends MailTemplateRoutes
  with DefaultMailTemplateReadRepositoryComponent
  with DefaultFileStoreComponent {
  override implicit protected val executionContext: ExecutionContext = system.dispatcher
}
