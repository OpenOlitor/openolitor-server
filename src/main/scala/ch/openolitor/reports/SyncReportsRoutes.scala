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
package ch.openolitor.reports

import akka.actor._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import ch.openolitor.core._
import ch.openolitor.core.db._
import ch.openolitor.core.filestore._
import ch.openolitor.core.security.Subject
import ch.openolitor.reports.eventsourcing.ReportsEventStoreSerializer
import ch.openolitor.reports.models._
import ch.openolitor.reports.repositories.{ DefaultReportsReadRepositorySyncComponent, ReportsReadRepositorySyncComponent }
import ch.openolitor.util.parsing.UriQueryParamFilterParser
import com.typesafe.scalalogging.LazyLogging
import scalikejdbc.DB

import scala.concurrent.{ ExecutionContext, Future }

/**
 * This is using a Sync-Repository as there is no way to fetch the MetaData on the
 * scalikejdbc-async - RowDataResultSet
 * TODO Revert as soon as possible
 */
trait SyncReportsRoutes extends BaseRouteService with ActorReferences
  with ConnectionPoolContextAware with AkkaHttpDeserializers with LazyLogging
  with ReportsJsonProtocol
  with ReportsEventStoreSerializer
  with ReportsDBMappings {
  self: ReportsReadRepositorySyncComponent with FileStoreComponent =>

  implicit val reportIdPath = long2BaseIdPathMatcher(ReportId.apply)

  def syncReportsRoute(implicit subect: Subject): Route =
    parameter("f".?) { f =>
      implicit val filter = f flatMap { filterString =>
        UriQueryParamFilterParser.parse(filterString)
      }
      path("reports" / reportIdPath / "execute" ~ exportFormatPath.?) { (_, exportFormat) =>
        post {
          extractRequestContext { requestContext =>
            implicit val materializer = requestContext.materializer
            entity(as[ReportExecute]) { reportExecute =>
              try {
                val result = DB readOnly {
                  implicit session => reportsReadRepository.executeReport(reportExecute, exportFormat)
                }
                list(Future.successful {
                  result
                }, exportFormat)
              } catch {
                case e: Exception =>
                  complete(StatusCodes.BadRequest, s"$e")
              }
            }
          }
        }
      }
    }

}

class DefaultSyncReportsRoutes(
  override val dbEvolutionActor: ActorRef,
  override val entityStore: ActorRef,
  override val eventStore: ActorRef,
  override val mailService: ActorRef,
  override val reportSystem: ActorRef,
  override val sysConfig: SystemConfig,
  override val system: ActorSystem,
  override val airbrakeNotifier: ActorRef,
  override val jobQueueService: ActorRef
) extends SyncReportsRoutes
  with DefaultReportsReadRepositorySyncComponent
  with DefaultFileStoreComponent {
  override implicit protected val executionContext: ExecutionContext = system.dispatcher
}
