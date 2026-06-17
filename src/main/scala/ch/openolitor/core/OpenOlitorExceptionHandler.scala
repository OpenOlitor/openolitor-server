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
package ch.openolitor.core

import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import org.apache.pekko.http.scaladsl.model.{ ContentTypes, HttpEntity, HttpResponse, StatusCodes }
import org.apache.pekko.http.scaladsl.server.ExceptionHandler
import ch.openolitor.util.AirbrakeNotifier.AirbrakeNotification
import com.typesafe.scalalogging.LazyLogging
import spray.json.enrichAny

object OpenOlitorExceptionHandler extends LazyLogging with BaseJsonProtocol with SprayJsonSupport {

  def apply(routeService: CORSSupport with AirbrakeNotifierReference): ExceptionHandler = ExceptionHandler {
    case th => ctx =>
      logger.error(s"The following Exception was thrown ${th.getMessage}")
      routeService.airbrakeNotifier ! AirbrakeNotification(th, Some(ctx.request))
      ctx.complete(HttpResponse(StatusCodes.InternalServerError).withHeaders(
        routeService.allowCredentialsHeader :: routeService.allowOriginHeader :: routeService.exposeHeaders :: routeService.optionsCorsHeaders
      ).withEntity(HttpEntity(ContentTypes.`application/json`, RejectionMessage(th.getMessage, s"${th.getCause}").toJson.compactPrint)))
  }
}
