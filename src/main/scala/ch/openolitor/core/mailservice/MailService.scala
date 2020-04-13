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
package ch.openolitor.core.mailservice

import java.nio.file.{ Files, StandardCopyOption }
import java.io.File

import ch.openolitor.core.domain.AggregateRoot
import akka.actor._
import ch.openolitor.core.models.PersonId
import ch.openolitor.core.domain._
import ch.openolitor.core.SystemConfig
import ch.openolitor.core.db.ConnectionPoolContextAware
import org.joda.time.DateTime
import akka.persistence.SnapshotMetadata

import scala.util.Failure
import scala.util.Success
import scala.concurrent.duration._
import courier._
import javax.mail.internet.InternetAddress

import stamina.Persister
import java.util.UUID
import javax.mail.util.ByteArrayDataSource

import scala.collection.immutable.TreeSet
import ch.openolitor.core.JSONSerializable
import ch.openolitor.util.ConfigUtil._

import scala.concurrent.Await
import ch.openolitor.core.filestore._
import spray.util._

object MailService {

  val VERSION = 1
  val persistenceId = "mail-store"

  case class MailServiceState(startTime: DateTime, mailQueue: TreeSet[MailEnqueued]) extends State

  case class SendMailCommandWithCallback[M <: AnyRef](originator: PersonId, entity: Mail, retryDuration: Option[Duration], commandMeta: M)(implicit p: Persister[M, _]) extends UserCommand
  case class SendMailCommand(originator: PersonId, entity: Mail, retryDuration: Option[Duration]) extends UserCommand

  //events raised by this aggregateroot
  case class MailServiceInitialized(meta: EventMetadata) extends PersistentEvent
  // resulting send mail event
  case class SendMailEvent(meta: EventMetadata, uid: String, mail: Mail, expires: DateTime, commandMeta: Option[AnyRef]) extends PersistentEvent with JSONSerializable
  case class MailSentEvent(meta: EventMetadata, uid: String, commandMeta: Option[AnyRef]) extends PersistentEvent with JSONSerializable
  case class SendMailFailedEvent(meta: EventMetadata, uid: String, numberOfRetries: Int, commandMeta: Option[AnyRef]) extends PersistentEvent with JSONSerializable

  def props(dbEvolutionActor: ActorRef, fileStore: FileStore)(implicit sysConfig: SystemConfig): Props = Props(classOf[DefaultMailService], sysConfig, dbEvolutionActor, fileStore)

  case object CheckMailQueue
}

trait MailService extends AggregateRoot
  with ConnectionPoolContextAware
  with FileStoreComponent

  with MailRetryHandler {

  import MailService._
  import AggregateRoot._

  override val fileStore: FileStore = null
  override def persistenceId: String = MailService.persistenceId
  type S = MailServiceState
  lazy val fromAddress = sysConfig.mandantConfiguration.config.getString("smtp.from")
  lazy val maxNumberOfRetries = sysConfig.mandantConfiguration.config.getInt("smtp.number-of-retries")
  lazy val sendEmailOutbound = sysConfig.mandantConfiguration.config.getBooleanOption("smtp.send-email").getOrElse(true)
  lazy val DefaultChunkSize = sysConfig.mandantConfiguration.config.getIntBytes("smtp.max-chunk-size")
  lazy val security = sysConfig.mandantConfiguration.config.getString("smtp.security")
  lazy val mailer = if (security.equals("STARTTLS")) {
    Mailer(sysConfig.mandantConfiguration.config.getString("smtp.endpoint"), sysConfig.mandantConfiguration.config.getInt("smtp.port")).auth(true)
    .as(sysConfig.mandantConfiguration.config.getString("smtp.user"), sysConfig.mandantConfiguration.config.getString("smtp.password")).startTls(true)()
  } else {
    Mailer(sysConfig.mandantConfiguration.config.getString("smtp.endpoint"), sysConfig.mandantConfiguration.config.getInt("smtp.port")).auth(true)
      .as(sysConfig.mandantConfiguration.config.getString("smtp.user"), sysConfig.mandantConfiguration.config.getString("smtp.password")).ssl(true)()
  }

  override var state: MailServiceState = MailServiceState(DateTime.now, TreeSet.empty[MailEnqueued])

  override protected def afterEventPersisted(evt: PersistentEvent): Unit = {
    updateState(false)(evt)
    publish(evt)
  }

  def initialize(): Unit = {
    // start mail queue checker
    context.system.scheduler.schedule(0 seconds, 10 seconds, self, CheckMailQueue)(context.system.dispatcher)
  }

  def checkMailQueue(): Unit = {
    if (!state.mailQueue.isEmpty) {
      state.mailQueue map { enqueued =>
        // sending a mail has to be blocking, otherwise there will be concurrent mail queue access
        sendMail(enqueued.meta, enqueued.uid, enqueued.mail, enqueued.commandMeta) match {
          case Right(event) =>
            persist(event)(afterEventPersisted)
          case Left(e) =>
            log.warning(s"Failed to send mail ${e}. Trying again later.")

            calculateRetryEnqueued(enqueued).fold(
              _ => {
                persist(SendMailFailedEvent(metadata(enqueued.meta.originator), enqueued.uid, enqueued.retries, enqueued.commandMeta))(afterEventPersisted)
              },
              maybeRequeue =>
                maybeRequeue map { result =>
                  state = state.copy(mailQueue = state.mailQueue - enqueued + result)
                }
            )
        }
      }
    }
  }

  def sendMail(meta: EventMetadata, uid: String, mail: Mail, commandMeta: Option[AnyRef]): Either[String, MailSentEvent] = {
    val inputStreamfile = mail.attachmentReference map { attachment: String =>
      Await.result(fileStore.getFile(GeneriertBucket, attachment), 5 seconds)
    } getOrElse (Left(FileStoreError("Error")))
    if (sendEmailOutbound) {
      val envelope = mail.attachmentReference match {
        case Some(attachment) => {
          inputStreamfile match {
            case Right(f) => {
              Right(Envelope.from(new InternetAddress(fromAddress))
                .to(InternetAddress.parse(mail.to): _*)
                .subject(mail.subject)
                .content(Multipart()
                  .attachBytes(f.file.toByteArrayStream(DefaultChunkSize).flatten.toArray, "rechnung.pdf", "application/pdf")
                  .html(s"${mail.content}")))
            }
            case Left(e) => Left(e)
          }
        }
        case None =>
          {
            Right(
              Envelope.from(new InternetAddress(fromAddress))
                .to(InternetAddress.parse(mail.to): _*)
                .subject(mail.subject)
                .content(Text(mail.content))
            )
          }
      }

      mail.cc map { cc =>
        envelope match {
          case Right(e) => e.cc(InternetAddress.parse(cc): _*)
          case Left(e)  => Left(e)
        }
      }

      mail.bcc map { bcc =>
        envelope match {
          case Right(e) => e.bcc(InternetAddress.parse(bcc): _*)
          case Left(e)  => Left(e)
        }
      }

      // we have to await the result, maybe switch to standard javax.mail later
      try {
        val result = envelope match {
          case Right(e) => Await.ready(mailer(e), 5 seconds).value match {
            case Some(e) => Right(e)
            case None    => Left("Error sending the email")
          }
          case Left(e) => Left(e)
        }

        result match {
          case Right(mailer) => mailer match {
            case Success(_) => Right(MailSentEvent(metadata(meta.originator), uid, commandMeta))
            case Failure(e) => Left(e.toString)
          }
          case Left(e) => Left(e.toString)
        }
      } catch {
        case e: Exception =>
          Left(e.toString)
      }
    } else {
      log.debug(s"=====================================================================")
      log.debug(s"| Sending Email: ${mail}")
      log.debug(s"=====================================================================")

      Right(MailSentEvent(metadata(meta.originator), uid, commandMeta))
    }
  }

  def enqueueMail(meta: EventMetadata, uid: String, mail: Mail, expires: DateTime, commandMeta: Option[AnyRef]): Unit = {
    state = state.copy(mailQueue = state.mailQueue + MailEnqueued(meta, uid, mail, commandMeta, DateTime.now(), expires, 0))
  }

  def dequeueMail(uid: String): Unit = {
    state.mailQueue.find(_.uid == uid) map { dequeue =>
      state = state.copy(mailQueue = state.mailQueue - dequeue)
    }
  }

  override def updateState(recovery: Boolean)(evt: PersistentEvent): Unit = {
    if (!recovery) {
      log.debug(s"updateState:$evt")
    }
    evt match {
      case MailServiceInitialized(_) =>
      case SendMailEvent(meta, uid, mail, expires, commandMeta) if !recovery =>
        enqueueMail(meta, uid, mail, expires, commandMeta)
        self ! CheckMailQueue
      case MailSentEvent(_, uid, _) if !recovery =>
        dequeueMail(uid)
      case SendMailFailedEvent(_, uid, _, _) if !recovery =>
        dequeueMail(uid)
      case _ =>
    }
  }

  override def restoreFromSnapshot(metadata: SnapshotMetadata, state: State) = {
    log.debug(s"restoreFromSnapshot:$state")
    state match {
      case Removed             => context become removed
      case Created             => context become uninitialized
      case s: MailServiceState => this.state = s
      case other               => log.error(s"Received unsupported state:$other")
    }
  }

  val uninitialized: Receive = {
    case GetState =>
      log.debug(s"uninitialized => GetState: $state")
      sender ! state
    case Initialize(state) =>
      // testing
      log.debug(s"uninitialized => Initialize: $state")
      this.state = state
      initialize()
      context become created
    case CheckMailQueue =>
  }

  val created: Receive = {
    case KillAggregate =>
      log.debug(s"created => KillAggregate")
      context.stop(self)
    case GetState =>
      log.debug(s"created => GetState")
      sender ! state
    case CheckMailQueue =>
      checkMailQueue()
    case SendMailCommandWithCallback(personId, mail, retryDuration, commandMeta) =>
      val meta = metadata(personId)
      val id = newId
      val event = SendMailEvent(meta, id, mail, calculateExpires(retryDuration), Some(commandMeta))
      persist(event) { result =>
        afterEventPersisted(result)
        sender ! result
      }
    case SendMailCommand(personId, mail, retryDuration) =>
      val meta = metadata(personId)
      val id = newId
      val event = SendMailEvent(meta, id, mail, calculateExpires(retryDuration), None)
      persist(event) { result =>
        afterEventPersisted(result)
        sender ! result
      }
    case other =>
      log.warning(s"Received unknown command:$other")
  }

  val removed: Receive = {
    case GetState =>
      log.warning(s"Received GetState in state removed")
      sender() ! state
    case KillAggregate =>
      log.warning(s"Received KillAggregate in state removed")
      context.stop(self)
  }

  def metadata(personId: PersonId) = {
    EventMetadata(personId, VERSION, DateTime.now, aquireTransactionNr(), 1L, persistenceId)
  }

  def newId: String = UUID.randomUUID.toString

  override def afterRecoveryCompleted(): Unit = {
    context become created
    initialize()
  }

  override val receiveCommand = uninitialized
}

class DefaultMailService(override val sysConfig: SystemConfig, override val dbEvolutionActor: ActorRef, override val fileStore: FileStore) extends MailService
  with DefaultCommandHandlerComponent
  with DefaultMailRetryHandler {
  lazy val system = context.system
}
