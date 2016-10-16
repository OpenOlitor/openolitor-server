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
package ch.openolitor.arbeitseinsatz

import org.joda.time.DateTime
import spray.routing._
import spray.http._
import spray.http.MediaTypes._
import spray.httpx.marshalling.ToResponseMarshallable._
import spray.httpx.SprayJsonSupport._
import spray.routing.Directive._
import spray.json._
import spray.json.DefaultJsonProtocol._
import ch.openolitor.core._
import ch.openolitor.core.domain._
import ch.openolitor.core.db._
import spray.httpx.unmarshalling.Unmarshaller
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util._
import java.util.UUID
import akka.pattern.ask
import scala.concurrent.duration._
import akka.util.Timeout
import ch.openolitor.stammdaten.models._
import ch.openolitor.core.models._
import spray.httpx.marshalling._
import spray.httpx.unmarshalling._
import scala.concurrent.Future
import ch.openolitor.core.Macros._
import ch.openolitor.arbeitseinsatz.eventsourcing.ArbeitseinsatzEventStoreSerializer
import stamina.Persister
import ch.openolitor.arbeitseinsatz.repositories._
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.filestore._
import akka.actor._
import ch.openolitor.core.security.Subject
import ch.openolitor.arbeitseinsatz.repositories._
import ch.openolitor.util.parsing.UriQueryParamFilterParser
import ch.openolitor.util.parsing.FilterExpr
import ch.openolitor.core.security.RequestFailed
import ch.openolitor.arbeitseinsatz.models._

trait ArbeitseinsatzRoutes extends HttpService with ActorReferences
    with AsyncConnectionPoolContextAware with SprayDeserializers with DefaultRouteService with LazyLogging
    with ArbeitseinsatzJsonProtocol
    with ArbeitseinsatzEventStoreSerializer
    with Defaults {
  self: ArbeitseinsatzReadRepositoryComponent =>

  implicit val arbeitskategorieIdPath = long2BaseIdPathMatcher(ArbeitskategorieId.apply)
  implicit val arbeitsangebotIdPath = long2BaseIdPathMatcher(ArbeitsangebotId.apply)
  implicit val arbeitseinsatzIdPath = long2BaseIdPathMatcher(ArbeitseinsatzId.apply)

  import EntityStore._

  def arbeitseinsatzRoute(implicit subject: Subject) =
    path("arbeitskategorien" ~ exportFormatPath.?) { exportFormat =>
      get(list(arbeitseinsatzReadRepository.getArbeitskategorien, exportFormat)) ~
        post(create[ArbeitskategorieModify, ArbeitskategorieId](ArbeitskategorieId.apply _))
    } ~
      path("arbeitskategorien" / arbeitskategorieIdPath) { id =>
        (put | post)(update[ArbeitskategorieModify, ArbeitskategorieId](id)) ~
          delete(remove(id))
      } ~
      path("arbeitseinsaetze" ~ exportFormatPath.?) { exportFormat =>
        get(list(arbeitseinsatzReadRepository.getArbeitseinsaetze))
      } ~
      path("arbeitseinsaetze" / "zukunft" ~ exportFormatPath.?) { exportFormat =>
        get(list(arbeitseinsatzReadRepository.getFutureArbeitseinsaetze))
      }
}

class DefaultArbeitseinsatzRoutes(
  override val entityStore: ActorRef,
  override val eventStore: ActorRef,
  override val mailService: ActorRef,
  override val reportSystem: ActorRef,
  override val sysConfig: SystemConfig,
  override val system: ActorSystem,
  override val fileStore: FileStore,
  override val actorRefFactory: ActorRefFactory,
  override val airbrakeNotifier: ActorRef
)
    extends ArbeitseinsatzRoutes
    with DefaultArbeitseinsatzReadRepositoryComponent
