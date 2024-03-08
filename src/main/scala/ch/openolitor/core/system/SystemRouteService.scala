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

import org.apache.pekko.actor._
import org.apache.pekko.http.scaladsl.model._
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.pattern.ask
import org.apache.pekko.stream.Materializer
import org.apache.pekko.util.Timeout
import ch.openolitor.core.{ ActorReferences, BaseRouteService, AkkaHttpDeserializers, SystemConfig }
import ch.openolitor.core.data.DataImportService
import ch.openolitor.core.data.DataImportService.{ ImportData, ImportResult }
import ch.openolitor.core.db.{ AsyncConnectionPoolContextAware, ConnectionPoolContextAware }
import ch.openolitor.core.eventsourcing._
import ch.openolitor.core.filestore.{ DefaultFileStoreComponent, FileStoreComponent }
import ch.openolitor.core.jobs.JobQueueRoutes
import ch.openolitor.core.repositories._
import ch.openolitor.core.security.Subject
import ch.openolitor.util.parsing.UriQueryParamFilterParser
import com.typesafe.scalalogging.LazyLogging
import spray.json._

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

trait SystemRouteService extends BaseRouteService with ActorReferences
  with ConnectionPoolContextAware with AkkaHttpDeserializers
  with LazyLogging
  with StatusRoutes
  with SystemJsonProtocol
  with AsyncConnectionPoolContextAware
  with PersistenceJsonProtocol
  with JobQueueRoutes {
  self: CoreReadRepositoryComponent with FileStoreComponent =>

  private var error: Option[Throwable] = None
  val system: ActorSystem
  def importService(implicit subject: Subject) = {
    val serviceName = s"oo-import-service-${subject.personId.id}"
    val identifyId = 1
    (system.actorSelection(system.child(serviceName)) ? Identify(identifyId)) map {
      case ActorIdentity(`identifyId`, Some(ref)) => ref
      case ActorIdentity(`identifyId`, None)      => system.actorOf(DataImportService.props(sysConfig, entityStore, system, subject.personId), serviceName)
    }
  }

  def adminRoutes(implicit subject: Subject) = pathPrefix("admin") {
    adminRoute
  }

  def handleError(er: Throwable) = {
    error = Some(er)
  }

  def adminRoute(implicit subject: Subject): Route =
    path("status") {
      get {
        error map { e =>
          complete(StatusCodes.BadRequest, e)
        } getOrElse {
          complete("Ok")
        }
      }
    } ~
      path("import") {
        post {
          extractRequestContext { requestContext =>
            implicit val materializer: Materializer = requestContext.materializer
            uploadUndispatchedConsume() {
              case (content, fileName) =>
                formFieldMap { fields =>
                  implicit val timeout = Timeout(300 seconds)

                  val clearBeforeImport = fields.get("clear").map(_.toBoolean).getOrElse(false)

                  logger.debug(s"File:${fileName}, clearBeforeImport:$clearBeforeImport: ")

                  onSuccess(importService.flatMap(_ ? ImportData(clearBeforeImport, content))) {
                    case ImportResult(Some(error), _) =>
                      logger.warn(s"Couldn't import data, received error:$error")
                      complete(StatusCodes.BadRequest, error)
                    case r @ ImportResult(None, _) =>
                      complete(r.toJson.compactPrint)
                    case x =>
                      logger.warn(s"Couldn't import data, unexpected result:$x")
                      complete(StatusCodes.BadRequest)
                  }
                }
            }
          }
        }
      } ~
      path("events") {
        get {
          parameters("f".?, "limit" ? 100) { (f: Option[String], limit: Int) =>
            implicit val filter = f flatMap { filterString =>
              UriQueryParamFilterParser.parse(filterString)
            }
            onSuccess(coreReadRepository.queryPersistenceJournal(limit)) {
              case result =>
                complete(result)
            }
          }
        }
      }
}

class DefaultSystemRouteService(
  override val dbEvolutionActor: ActorRef,
  override val entityStore: ActorRef,
  override val eventStore: ActorRef,
  override val mailService: ActorRef,
  override val reportSystem: ActorRef,
  override val sysConfig: SystemConfig,
  override val system: ActorSystem,
  override val airbrakeNotifier: ActorRef,
  override val jobQueueService: ActorRef
) extends SystemRouteService
  with DefaultCoreReadRepositoryComponent
  with DefaultFileStoreComponent {
  override implicit protected val executionContext: ExecutionContext = system.dispatcher
}