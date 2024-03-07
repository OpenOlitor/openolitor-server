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
package ch.openolitor.core.ws

import akka.actor.{ ActorRef, ActorSystem }
import akka.http.caching.scaladsl.Cache
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.SourceQueueWithComplete
import ch.openolitor.core.{ BaseRouteService, SystemConfig }
import ch.openolitor.core.filestore.DefaultFileStoreComponent
import ch.openolitor.core.models.PersonId
import ch.openolitor.core.security.{ Subject, XSRFTokenSessionAuthenticatorProvider }

import scala.collection.concurrent.TrieMap
import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext

trait ClientMessagesRouteService extends BaseRouteService with XSRFTokenSessionAuthenticatorProvider {
  val clientMessagesService: ClientMessagesService

  lazy val routes: Route = path("") {
    handleWebSocketMessages(clientMessagesService.handler())
  }
}

class DefaultClientMessagesRouteService(
  override val entityStore: ActorRef,
  override val sysConfig: SystemConfig,
  override val system: ActorSystem,
  override val loginTokenCache: Cache[String, Subject],
  val streamsByUser: TrieMap[PersonId, scala.collection.concurrent.Map[String, SourceQueueWithComplete[String]]]
) extends ClientMessagesRouteService
  with DefaultFileStoreComponent {
  override implicit protected val executionContext: ExecutionContext = system.dispatcher
  override val clientMessagesService = new DefaultClientMessagesService(system, loginTokenCache, streamsByUser)
  override val maxRequestDelay = Some(20 seconds)
}
