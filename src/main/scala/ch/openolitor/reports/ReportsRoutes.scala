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

import org.apache.pekko.actor._
import org.apache.pekko.http.scaladsl.server.Directives._
import ch.openolitor.core._
import ch.openolitor.core.db._
import ch.openolitor.core.filestore._
import ch.openolitor.core.security.Subject
import ch.openolitor.reports.eventsourcing.ReportsEventStoreSerializer
import ch.openolitor.reports.models._
import ch.openolitor.reports.repositories.{ DefaultReportsReadRepositoryAsyncComponent, ReportsReadRepositoryAsyncComponent }
import ch.openolitor.util.parsing.UriQueryParamFilterParser
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext

trait ReportsRoutes extends BaseRouteService with ActorReferences
  with AsyncConnectionPoolContextAware with AkkaHttpDeserializers with LazyLogging
  with ReportsJsonProtocol
  with ReportsEventStoreSerializer
  with ReportsDBMappings {
  self: ReportsReadRepositoryAsyncComponent with FileStoreComponent =>

  implicit val reportIdPath = long2BaseIdPathMatcher(ReportId.apply)

  def reportsRoute(implicit subect: Subject) =
    parameter("f".?) { f =>
      implicit val filter = f flatMap { filterString =>
        UriQueryParamFilterParser.parse(filterString)
      }
      path("reports" ~ exportFormatPath.?) { exportFormat =>
        extractRequestContext { requestContext =>
          implicit val materializer = requestContext.materializer
          get(list(reportsReadRepository.getReports, exportFormat)) ~
            post(create[ReportCreate, ReportId](ReportId.apply _))
        }
      } ~
        path("reports" / reportIdPath) { id =>
          get(detail(reportsReadRepository.getReport(id))) ~
            (put | post)(update[ReportModify, ReportId](id)) ~
            delete(remove(id))
        }
    }

}

class DefaultReportsRoutes(
  override val dbEvolutionActor: ActorRef,
  override val entityStore: ActorRef,
  override val eventStore: ActorRef,
  override val mailService: ActorRef,
  override val reportSystem: ActorRef,
  override val sysConfig: SystemConfig,
  override val system: ActorSystem,
  override val airbrakeNotifier: ActorRef,
  override val jobQueueService: ActorRef
)
  extends ReportsRoutes
  with DefaultReportsReadRepositoryAsyncComponent
  with DefaultFileStoreComponent {
  override implicit protected val executionContext: ExecutionContext = system.dispatcher
}
