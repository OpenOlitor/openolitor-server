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
package ch.openolitor.core.proxy

import akka.actor._
import akka.http.scaladsl.model.{ HttpHeader, HttpRequest, HttpResponse }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ RequestContext, Route }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.WebSocketRequest
import akka.stream.scaladsl.{ Flow, Sink, Source }
import ch.openolitor.core.Boot.MandantSystem
import com.typesafe.scalalogging.LazyLogging
import org.apache.http.HttpHeaders
import scalaz.NonEmptyList

import java.net.URI
import scala.concurrent.Future

trait Proxy extends LazyLogging {
  protected val mandanten: NonEmptyList[MandantSystem]
  protected val routeMap = mandanten.list.map(mandantSystem => (mandantSystem.config.key, mandantSystem)).toList.toMap

  private def connectionFlow(system: ActorSystem)(uri: URI): Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] =
    Http(system).outgoingConnection(uri.getHost, uri.getPort)

  private def filterHostHeader(header: HttpHeader) = header is HttpHeaders.HOST.toLowerCase

  private def filterContinueHeader(header: HttpHeader) = header is HttpHeaders.EXPECT.toLowerCase

  private def filterHeaders(headers: Seq[HttpHeader] = Seq.empty) =
    headers filterNot (header => filterHostHeader(header) || filterContinueHeader(header))

  val withWsRoutes = pathPrefix(Segment) { mandantName: String =>
    routeMap.get(mandantName) match {
      case Some(mandantSystem) =>
        path("ws") {
          extractWebSocketUpgrade { upgrade =>
            extractRequestContext { context =>
              val headers = filterHeaders(context.request.headers)
              val webSocketFlowProxy = Http(mandantSystem.system).webSocketClientFlow(WebSocketRequest(mandantSystem.config.wsUri, extraHeaders = headers))
              val handleWebSocketProxy = upgrade.handleMessages(webSocketFlowProxy)
              complete(handleWebSocketProxy)
            }
          }
        } ~ {
          context =>
            val headers = filterHeaders(context.request.headers)

            val query = context.request.uri.rawQueryString.map("?" + _).getOrElse("")
            val path = context.unmatchedPath
            val uri = mandantSystem.config.uri + path.toString + query

            val proxyRequest = context.request.withHeaders(headers).withUri(uri)

            Source.single(proxyRequest)
              .via(connectionFlow(mandantSystem.system)(new URI(mandantSystem.config.uri)))
              .runWith(Sink.head)(context.materializer)
              .flatMap(context.complete(_))(mandantSystem.system.dispatcher)
        }
      case None =>
        reject
    }
  }

  val routes = pathPrefix(Segment) { mandantName =>
    routeMap.get(mandantName) match {
      case Some(mandantSystem) =>
        (context: RequestContext) => {
          // rewrite request accordingly
          val headers = filterHeaders(context.request.headers)

          val query = context.request.uri.rawQueryString.map("?" + _).getOrElse("")
          val path = context.unmatchedPath
          val uri = mandantSystem.config.uri + path.toString + query

          val proxyRequest: HttpRequest = context.request.withHeaders(headers).withUri(uri)

          Source.single(proxyRequest)
            .via(connectionFlow(mandantSystem.system)(new URI(mandantSystem.config.uri)))
            .runWith(Sink.head)(context.materializer)
            .flatMap(context.complete(_))(mandantSystem.system.dispatcher)
        }
      case None =>
        reject
    }
  }
}

class DefaultProxy(override val mandanten: NonEmptyList[MandantSystem]) extends Proxy

object Proxy {
  def apply(mandanten: NonEmptyList[MandantSystem]): Proxy = {
    new DefaultProxy(mandanten)
  }
}
