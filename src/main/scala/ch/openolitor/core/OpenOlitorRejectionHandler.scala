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

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ HttpEntity, HttpResponse }
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RejectionHandler
import ch.openolitor.core.security.AuthenticatorRejection
import com.typesafe.scalalogging.LazyLogging

case class RejectionMessage(message: String, cause: String)

/** Custom RejectionHandler for dealing with AuthenticatorRejections. */
object OpenOlitorRejectionHandler extends LazyLogging with BaseJsonProtocol with SprayJsonSupport {
  override implicit val rejectionMessageFormat = jsonFormat2(RejectionMessage)

  def apply(corsSupport: CORSSupport): RejectionHandler = RejectionHandler.newBuilder().handle({
    case AuthenticatorRejection(reason) =>
      logger.debug(s"AuthenticatorRejection: $reason")
      complete(Unauthorized, corsSupport.allowCredentialsHeader :: corsSupport.allowOriginHeader :: corsSupport.exposeHeaders :: corsSupport.optionsCorsHeaders, RejectionMessage("Unauthorized", ""))
  }).result().withFallback(RejectionHandler.default.mapRejectionResponse {
    case response @ HttpResponse(_, _, entity: HttpEntity.Strict, _) =>
      response.withHeaders(corsSupport.allowCredentialsHeader :: corsSupport.allowOriginHeader :: corsSupport.exposeHeaders :: corsSupport.optionsCorsHeaders).withEntity(RejectionMessage(entity.data.utf8String, "").toString)
    case x => x
  })
}
