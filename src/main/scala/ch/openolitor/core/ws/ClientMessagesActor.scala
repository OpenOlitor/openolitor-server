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

import akka.actor._
import akka.stream.scaladsl._
import ch.openolitor.core._
import ch.openolitor.core.models.PersonId
import ch.openolitor.core.ws.ClientMessages._
import ch.openolitor.core.ws.ControlCommands.SendToClient
import spray.json.RootJsonWriter

import scala.collection.concurrent.TrieMap

trait ClientReceiverComponent {
  val clientReceiver: ClientReceiver
}

object ControlCommands {
  case class SendToClient(senderPersonId: PersonId, msg: String, receivers: List[PersonId] = Nil)
}

trait ClientReceiver extends EventStream {
  import ClientMessagesJsonProtocol._

  def broadcast[M <: ClientMessage](senderPersonId: PersonId, msg: M)(implicit writer: RootJsonWriter[M]) =
    publish(SendToClient(senderPersonId, msgToString(ClientMessageWrapper(msg))))

  /**
   * Send OutEvent to a list of receiving clients exclusing sender itself
   */
  def send[M <: ClientMessage](senderPersonId: PersonId, msg: M, receivers: List[PersonId])(implicit writer: RootJsonWriter[M]) =
    publish(SendToClient(senderPersonId, msgToString(ClientMessageWrapper(msg)), receivers))

  def send[M <: ClientMessage](senderPersonId: PersonId, msg: M)(implicit writer: RootJsonWriter[M]): Unit =
    send(senderPersonId, msg, List(senderPersonId))

  def msgToString[M <: ClientMessage](msg: ClientMessageWrapper[M])(implicit writer: RootJsonWriter[ClientMessageWrapper[M]]) =
    writer.write(msg).compactPrint
}

object ClientMessagesActor {
  def props(streamsByUser: TrieMap[PersonId, scala.collection.concurrent.Map[String, SourceQueueWithComplete[String]]]) = Props(classOf[ClientMessagesActor], streamsByUser)
}

class ClientMessagesActor(streamsByUser: TrieMap[PersonId, scala.collection.concurrent.Map[String, SourceQueueWithComplete[String]]]) extends Actor with ActorLogging {
  override def preStart(): Unit = {
    super.preStart()
    //register ourself as listener to sendtoclient commands
    context.system.eventStream.subscribe(self, classOf[SendToClient])
  }

  override def postStop(): Unit = {
    context.system.eventStream.unsubscribe(self, classOf[SendToClient])
    super.postStop()
  }

  def receive: Receive = {
    case SendToClient(_, msg, Nil) =>
      //broadcast to all
      log.debug(s"Broadcast client message:$msg")
      streamsByUser.foreach(_._2.foreach(_._2.offer(msg)))
    case SendToClient(_, msg, receivers) =>
      //send to specific clients only
      log.debug(s"send client message:$msg:$receivers")
      receivers.foreach(personId => streamsByUser.get(personId).foreach(_.foreach(_._2.offer(msg))))
    case x: Any =>
      log.debug(s"Received unkown event:$x")
  }
}
