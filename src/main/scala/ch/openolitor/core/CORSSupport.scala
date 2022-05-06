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

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.Directives._
import ch.openolitor.util.ConfigUtil._
import com.typesafe.scalalogging.LazyLogging

// see also https://developer.mozilla.org/en-US/docs/Web/HTTP/Access_control_CORS
trait CORSSupport extends LazyLogging {

  val sysConfig: SystemConfig

  lazy val allowOrigin = sysConfig.mandantConfiguration.config.getStringListOption(s"security.cors.allow-origin").map(list => HttpOriginRange(list.map(HttpOrigin.apply): _*)).getOrElse(HttpOriginRange.*)

  val allowOriginHeader = `Access-Control-Allow-Origin`(allowOrigin)
  val allowCredentialsHeader = `Access-Control-Allow-Credentials`(true)
  val allowHeaders = `Access-Control-Allow-Headers`("Origin, X-Requested-With, Content-Type, Content-Disposition, Content-Length, Accept, Accept-Encoding, Accept-Language, Host, Referer, User-Agent, XSRF-TOKEN")
  val exposeHeaders = `Access-Control-Expose-Headers`("Origin, X-Requested-With, Content-Type, Content-Disposition, Content-Length, Accept, Accept-Encoding, Accept-Language, Host, Referer, User-Agent, XSRF-TOKEN")
  val optionsCorsHeaders = List(
    allowHeaders,
    `Access-Control-Max-Age`(1728000)
  )
  logger.debug(s"$this:allowOriginHeader:$allowOriginHeader")

  protected def corsRejectionHandler(allowOrigin: `Access-Control-Allow-Origin`) = RejectionHandler.newBuilder().handle {
    case MethodRejection(supported) =>
      complete(HttpResponse().withHeaders(
        `Access-Control-Allow-Methods`(OPTIONS, supported) ::
          allowOrigin ::
          optionsCorsHeaders
      ))
  }.result()

  private def allowedOrigin(origin: Origin) =
    if (origin.origins.forall(allowOrigin.matches)) origin.origins.headOption.map(`Access-Control-Allow-Origin`.apply) else None

  def corsDirective: Directive0 = mapInnerRoute { route => context =>
    ((context.request.method, context.request.header[Origin].flatMap(allowedOrigin)) match {
      case (OPTIONS, Some(allowOrigin)) =>
        handleRejections(corsRejectionHandler(allowOrigin)) {
          respondWithHeaders(allowOrigin, allowCredentialsHeader) {
            route
          }
        }
      case (_, Some(allowOrigin)) =>
        respondWithHeaders(allowOrigin, allowCredentialsHeader) {
          route
        }
      case (_, _) =>
        route
    })(context)
  }

  private def preflightRequestHandler: Route = options {
    complete(HttpResponse(200).withHeaders(
      `Access-Control-Allow-Methods`(OPTIONS, POST, PUT, GET, DELETE) ::
        allowCredentialsHeader :: allowOriginHeader :: allowHeaders :: exposeHeaders :: Nil
    ))
  }

  def cors(r: Route) = preflightRequestHandler ~ corsDirective {
    r
  }
}
