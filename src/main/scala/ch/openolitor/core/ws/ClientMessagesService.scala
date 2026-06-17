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
package ch.openolitor.core.ws

import org.apache.pekko.{ Done, NotUsed }
import org.apache.pekko.actor._
import org.apache.pekko.http.caching.scaladsl.Cache
import org.apache.pekko.http.scaladsl.model.ws.{ BinaryMessage, Message, TextMessage }
import org.apache.pekko.stream.{ Materializer, OverflowStrategy }
import org.apache.pekko.stream.scaladsl._
import ch.openolitor.core._
import ch.openolitor.core.models.PersonId
import ch.openolitor.core.security.Subject

import scala.collection.concurrent.TrieMap
import scala.concurrent.duration.DurationInt
import scala.concurrent.Future

object ClientMessagesService {
  val helloServerPattern = """(.*)("type":\s*"HelloServer")(.*)""".r
  val loginPattern = """\{(.*)("type":"Login"),("token":"([\w|-]+)")(.*)\}""".r
  val logoutPattern = """(.*)("type":"Logout")(.*)""".r
  val clientPingPattern = """(.*)("type":"ClientPing")(.*)""".r
}

trait ClientMessagesService extends ActorSystemReference {

  import ClientMessagesService._

  protected val loginTokenCache: Cache[String, Subject]
  protected val streamsByUser: TrieMap[PersonId, scala.collection.concurrent.Map[String, SourceQueueWithComplete[String]]]

  implicit private val executionContext = system.dispatcher
  implicit private val materializer = Materializer.matFromSystem(system)

  def handler(): Flow[Message, Message, Any] = {
    val messageSource = Source.queue[String](10, OverflowStrategy.dropHead)
    val queue: (SourceQueueWithComplete[String], Source[String, NotUsed]) = messageSource.preMaterialize()

    val transformed = queue._2.map(message => TextMessage(message))

    val incoming: Sink[Message, Future[Done]] =
      Sink.foreach {
        case bm: BinaryMessage =>
          // drain with ignore
          bm.dataStream.runWith(Sink.ignore)
        case TextMessage.Strict(text) =>
          handleTextMessage(queue)(text)
        case streamed: TextMessage.Streamed =>
          streamed.toStrict(10 seconds).map(strictTextMessage => handleTextMessage(queue)(strictTextMessage.text))
      }

    Flow.fromSinkAndSourceCoupled(incoming, transformed)
  }

  private def handleTextMessage(queue: (SourceQueueWithComplete[String], Source[String, NotUsed]))(text: String) = {
    text match {
      case helloServerPattern(_, _, _) =>
        queue._1.offer("""{"type":"HelloClient","server":"openolitor"}""")
      case loginPattern(_, _, _, token, _) =>
        loginTokenCache.get(token).map(_.map { implicit subject =>
          streamsByUser
            .getOrElseUpdate(subject.personId, TrieMap())
            .update(subject.token, queue._1)

          // register on complete disconnection to avoid memory leak
          queue._1.watchCompletion().onComplete(_ => disconnectStreamOfSubject)

          queue._1.offer(s"""{"type":"LoggedIn","personId":"${subject.personId.id}"}""")
        })
      case clientPingPattern(_*) =>
        queue._1.offer("""{"type":"ServerPong"}""")
      case logoutPattern(_, _, _) =>
        queue._1.offer("""{"type":"LoggedOut"}""")
    }
  }

  private def disconnectStreamOfSubject(implicit subject: Subject): Unit = {
    streamsByUser.get(subject.personId).foreach { sessions =>
      sessions.remove(subject.token)
    }
  }
}

class DefaultClientMessagesService(
  override val system: ActorSystem,
  override val loginTokenCache: Cache[String, Subject],
  override val streamsByUser: TrieMap[PersonId, scala.collection.concurrent.Map[String, SourceQueueWithComplete[String]]]
) extends ClientMessagesService