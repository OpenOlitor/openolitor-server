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
package ch.openolitor.core.system

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route
import ch.openolitor.core.{ ActorReferences, BaseRouteService, AkkaHttpDeserializers, SystemConfig }
import ch.openolitor.core.db.ConnectionPoolContextAware
import ch.openolitor.core.filestore.{ DefaultFileStoreComponent, FileStoreComponent, ProjektStammdaten }
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext

trait NonAuthRessourcesRouteService extends BaseRouteService with ActorReferences
  with ConnectionPoolContextAware with AkkaHttpDeserializers with LazyLogging with SystemJsonProtocol with FileStoreComponent {

  override implicit protected val executionContext: ExecutionContext = system.dispatcher

  // NonAuth-Calls shall not interact with any of the following actor-systems
  override val entityStore: org.apache.pekko.actor.ActorRef = null
  override val eventStore: org.apache.pekko.actor.ActorRef = null
  override val mailService: org.apache.pekko.actor.ActorRef = null
  override val reportSystem: org.apache.pekko.actor.ActorRef = null
  override val dbEvolutionActor: org.apache.pekko.actor.ActorRef = null

  def ressourcesRoutes: Route = pathPrefix("ressource") {
    staticFileRoute
  }

  def staticFileRoute: Route =
    path("logo") {
      get(fetch(ProjektStammdaten, "logo"))
    } ~
      path("style" / "admin") {
        get(fetch(ProjektStammdaten, "style-admin"))
      } ~ path("style" / "admin" / "download") {
        get(download(ProjektStammdaten, "style-admin"))
      } ~ path("style" / "kundenportal") {
        get(fetch(ProjektStammdaten, "style-kundenportal"))
      } ~ path("style" / "kundenportal" / "download") {
        get(download(ProjektStammdaten, "style-kundenportal"))
      }
}

class DefaultNonAuthRessourcesRouteService(
  override val sysConfig: SystemConfig,
  override val system: ActorSystem,
  override val airbrakeNotifier: org.apache.pekko.actor.ActorRef,
  override val jobQueueService: org.apache.pekko.actor.ActorRef
) extends NonAuthRessourcesRouteService
  with DefaultFileStoreComponent {
  override implicit protected val executionContext = system.dispatcher
}