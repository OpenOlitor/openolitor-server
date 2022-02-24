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
package ch.openolitor.arbeitseinsatz

import ch.openolitor.arbeitseinsatz.eventsourcing.ArbeitseinsatzEventStoreSerializer
import ch.openolitor.arbeitseinsatz.repositories._
import ch.openolitor.arbeitseinsatz.ArbeitseinsatzCommandHandler._
import ch.openolitor.core._
import ch.openolitor.core.db._
import ch.openolitor.core.domain._
import ch.openolitor.stammdaten.EmailHandler
import akka.util.Timeout

import scala.concurrent.duration._
import com.typesafe.scalalogging.LazyLogging
import akka.actor.{ ActorRef, ActorSystem }
import ch.openolitor.stammdaten.models.{ Person, PersonEmailData, Projekt }
import scalikejdbc.DB
import ch.openolitor.core.models.PersonId
import ch.openolitor.core.repositories.EventPublishingImplicits._

import scala.concurrent.ExecutionContext.Implicits._

object ArbeitseinsatzAktionenService {
  def apply(implicit sysConfig: SystemConfig, system: ActorSystem, mailService: ActorRef): ArbeitseinsatzAktionenService = new DefaultArbeitseinsatzAktionenService(sysConfig, system, mailService)
}

class DefaultArbeitseinsatzAktionenService(sysConfig: SystemConfig, override val system: ActorSystem, override val mailService: ActorRef)
  extends ArbeitseinsatzAktionenService(sysConfig, mailService) with DefaultArbeitseinsatzWriteRepositoryComponent {
}

/**
 * Actor zum Verarbeiten der Aktionen für das Arbeitseinsatz Modul
 */
class ArbeitseinsatzAktionenService(override val sysConfig: SystemConfig, override val mailService: ActorRef) extends EventService[PersistentEvent] with LazyLogging with AsyncConnectionPoolContextAware with EmailHandler
  with ArbeitseinsatzDBMappings with MailServiceReference with ArbeitseinsatzEventStoreSerializer {
  self: ArbeitseinsatzWriteRepositoryComponent =>

  implicit val timeout = Timeout(15.seconds) //sending mails might take a little longer

  val handle: Handle = {
    case SendEmailToArbeitsangebotPersonenEvent(meta, subject, body, replyTo, context) =>
      checkBccAndSend(meta, subject, body, replyTo, context.person, context, mailService)
    case e =>
      logger.warn(s"Unknown event:$e")
  }

  protected def checkBccAndSend(meta: EventMetadata, subject: String, body: String, replyTo: Option[String], person: PersonEmailData, context: Product, mailService: ActorRef)(implicit originator: PersonId = meta.originator): Unit = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      lazy val bccAddress = config.getString("smtp.bcc")
      arbeitseinsatzWriteRepository.getProjekt map { projekt: Projekt =>
        projekt.sendEmailToBcc match {
          case true  => sendEmail(meta, subject, body, replyTo, Some(bccAddress), person, None, context, mailService)
          case false => sendEmail(meta, subject, body, replyTo, None, person, None, context, mailService)
        }
      }
    }
  }

}
