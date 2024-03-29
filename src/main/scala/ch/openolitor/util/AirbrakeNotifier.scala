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
package ch.openolitor.util

import akka.actor._
import akka.http.scaladsl.model._
import akka.http.scaladsl.Http
import ch.openolitor.core.SystemConfig
import ch.openolitor.util.ConfigUtil._

/**
 * Based on https://raw.githubusercontent.com/tegonal/play-airbrake/master/src/main/scala/play/airbrake/Airbrake.scala
 */
object AirbrakeNotifier {
  def props(implicit system: ActorSystem, systemConfig: SystemConfig): Props = Props(classOf[AirbrakeNotifier], system, systemConfig)

  case class AirbrakeNotification(th: Throwable, request: Option[HttpRequest] = None)
  case object AirbrakeNotificationTermination
}

class AirbrakeNotifier(system: ActorSystem, systemConfig: SystemConfig) extends Actor with ActorLogging {
  import AirbrakeNotifier._

  lazy val config = systemConfig.mandantConfiguration.config
  lazy val enabled = config.getBooleanOption("airbrake.enabled") getOrElse (false)
  lazy val apiKey = config.getStringOption("airbrake.api-key") getOrElse { sys.error("Could not find Airbrake api-key in config") }
  lazy val ssl = config.getBooleanOption("airbrake.ssl") getOrElse (false)
  lazy val endpoint = config.getStringOption("airbrake.endpoint") getOrElse ("api.airbrake.io")
  lazy val mandantIdentifier = systemConfig.mandantConfiguration.name

  def receive: Receive = {
    case AirbrakeNotification(th, request) =>
      notify(th, request)

    case AirbrakeNotificationTermination =>
      notify(new UnknownError("The system was not able to recover. A forced termination is called"), None)
      context become waitingForTermination

    case th: Throwable =>
      notify(th, None)

    case _ => // do nothing
  }

  def waitingForTermination: Receive = {
    case _ => // ignoring all messages
  }

  protected def notify(th: Throwable, request: Option[HttpRequest]): Unit = {
    if (enabled) {
      implicit val actorSystem = system
      val scheme = if (ssl) "https" else "http"

      Http().singleRequest(HttpRequest(HttpMethods.POST, uri = s"$scheme://$endpoint/notifier_api/v2/notices", entity = HttpEntity(ContentTypes.`text/xml(UTF-8)`, formatNotice(apiKey, request, th).text)))
    }
  }

  protected def formatNotice(apiKey: String, request: Option[HttpRequest], th: Throwable) = {
    <notice version="2.2">
      <api-key>{ apiKey }</api-key>
      <notifier>
        <name>spray-airbrake</name>
      </notifier>
      <error>
        <class>{ th.getClass }</class>
        <message>{ th.getMessage }</message>
        <backtrace>
          { th.getStackTrace.flatMap(e => formatStacktrace(e)) }
        </backtrace>
      </error>
      { formatRequest(request) }
      <server-environment>
        <environment-name>{ mandantIdentifier }</environment-name>
      </server-environment>
    </notice>
  }

  protected def formatRequest(request: Option[HttpRequest]) = request map { r =>
    <request>
      <url>{ s"${r.method} ${r.uri}" }</url>
      <component/>
      <action/>
      { formatSession(r.headers.map(h => (h.name -> h.value)).toMap) }
    </request>
  } getOrElse {
    Nil
  }

  protected def formatSession(headers: Map[String, String]) = {
    if (!headers.isEmpty) <session>{ formatVars(headers) }</session>
    else Nil
  }

  protected def formatVars(vars: Map[String, String]) = vars.map {
    case (key, value) =>
      <var key={ key }>{ value }</var>
  }

  protected def formatStacktrace(e: StackTraceElement) = {
    val line = s"${e.getClassName}.${e.getMethodName}(${e.getFileName})"
    <line file={ line } number={ e.getLineNumber.toString }/>
  }

}
