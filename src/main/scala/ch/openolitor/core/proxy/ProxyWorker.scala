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

import akka.util.ByteString
import ch.openolitor.core.Boot.MandantSystem
import scala.util.{ Failure, Success, Try }

/*
object ProxyWorker {
  case class Push(msg: String)
  case class BinaryPush(msg: Array[Byte])

  def props(serverConnection: ActorRef, routeMap: Map[String, MandantSystem], wsHandler: WebsocketHandler) = Props(classOf[ProxyWorker], serverConnection, routeMap, wsHandler)
}

/**
 * This Proxy Worker proxies either websocket messages to a websocket server or
 * normal httprequest to a httpserver
 */
class ProxyWorker(val serverConnection: ActorRef, val routeMap: Map[String, MandantSystem], val wsHandler: WebsocketHandler)
  extends HttpServiceActor
  with websocket.WebSocketServerWorker
  with Proxy {
  //Use system's dispatcher as ExecutionContext

  var url: Option[String] = None

  import ProxyWorker._
  var wsClient: WebSocket = wsHandler.wsClient

  var cancellable: Option[Cancellable] = None

  override def receive = businessLogicNoUpgrade orElse closeLogic orElse other

  override def postStop() {
    processCloseDown
    super.postStop()
  }

  def processCloseDown = {
    if (wsClient.isOpen) {
      wsClient.close
    }
    cancellable.map(c => if (!c.isCancelled) c.cancel)
    cancellable = None
  }

  val other: Receive = {
    case x =>
      log.error(s"Unmatched request: $x")
  }

  val unsupported: Receive = {
    case x =>
      log.debug(s"Unsupported request state: $x")
  }

  val requestContextHandshake: Receive = {
    case ctx: RequestContext =>
      Try(handshaking(ctx.request)) match {
        case Success(result) => result
        case Failure(error) => {
          log.error(s"Handshake failure: $error")
          serverConnection ! HttpResponse(426).withHeaders(List(Connection("Upgrade")))
          context become (closeLogic orElse unsupported)
        }
      }
  }

  lazy val textMessageListener = new TextListener {
    override def onMessage(message: String) {
      message match {
        case msg =>
          self ! Push(message)
      }
    }
    /**
     * Called when the {@link WebSocket} is opened
     */
    override def onOpen {
    }

    /**
     * Called when the {@link WebSocket} is closed
     */
    override def onClose {
    }

    /**
     * Called when the {@link WebSocket} is closed with its assic
     */
    override def onClose(code: Int, reason: String) {
    }

    /**
     * Called when an unexpected error occurd on a {@link WebSocket}
     */
    override def onError(t: Throwable) {
      log.error(s"messageListener:onError", t)
    }
  }

  lazy val binaryMessageListener = new BinaryListener {

    override def onMessage(message: Array[Byte]) {
      self ! BinaryPush(message)
    }

    /**
     * Called when the {@link WebSocket} is opened
     */
    override def onOpen {
    }

    /**
     * Called when the {@link WebSocket} is closed
     */
    override def onClose {
    }

    /**
     * Called when the {@link WebSocket} is closed with its assic
     */
    override def onClose(code: Int, reason: String) {
    }

    /**
     * Called when an unexpected error occurd on a {@link WebSocket}
     */
    override def onError(t: Throwable) {
      log.error(s"binaryMessageListener:onError", t)
    }
  }

  val proxyRoute: Route = {
    path(routeMap / "ws") { mandant =>
      url = Some(mandant.config.wsUri)
      openWsClient
      context become (handshaking orElse closeLogic)
      //grab request from already instanciated requestcontext to perform handshake
      requestContextHandshake
    } ~
      pathPrefix(routeMap) { mandant =>
        proxyToUnmatchedPath(mandant.config.uri)(mandant.system)
      }
  }

  def openWsClient: Option[WebSocket] = {
    url.map { url =>
      wsClient = wsClient.open(url).listener(textMessageListener).listener(binaryMessageListener)
      wsClient
    }

  }

  /**
   * In case we did get a websocket upgrade
   * Websocket handling logic
   */
  def businessLogic: Receive = {
    case Push(msg) =>
      send(TextFrame(msg))
    // just bounce frames back for Autobahn testsuite
    case BinaryPush(msg) =>
      send(BinaryFrame(ByteString(msg)))
    // just bounce frames back for Autobahn testsuite
    case x: BinaryFrame =>
    case x: TextFrame =>
      val msg = x.payload.decodeString("UTF-8")
      wsClient.send(msg)
    case x: FrameCommandFailed =>
      log.error("frame command failed", x)
    case x: HttpRequest      => // do something
    case UpgradedToWebSocket =>
    case akka.io.Tcp.Closed =>
      log.debug(s"Closed")
      processCloseDown
    case akka.io.Tcp.PeerClosed =>
      log.debug(s"PeerClosed")
      processCloseDown
    case akka.io.Tcp.Aborted =>
      log.debug(s"Aborted")
      processCloseDown
    case x =>
      log.warning(s"Got unmatched message:$x:" + x.getClass)
  }

  /**
   * In case we didn't get a websocket upgrade notification
   */
  def businessLogicNoUpgrade: Receive = runRoute(proxyRoute) orElse other
}
*/ 