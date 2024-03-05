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
package ch.openolitor.stammdaten

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import ch.openolitor.core.db.AsyncConnectionPoolContextAware
import ch.openolitor.core.domain.EventTransactionMetadata
import ch.openolitor.core.eventsourcing.CoreEventStoreSerializer
import ch.openolitor.core.mailservice.MailService._
import ch.openolitor.core.models._
import ch.openolitor.mailtemplates.engine.MailTemplateService
import ch.openolitor.mailtemplates.eventsourcing.MailTemplateEventStoreSerializer
import ch.openolitor.stammdaten.eventsourcing.StammdatenEventStoreSerializer
import ch.openolitor.stammdaten.models._
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.util._

class DefaultMailCommandForwarder(mailService: ActorRef) extends MailCommandForwarder with MailTemplateService with CoreEventStoreSerializer with LazyLogging {
  implicit val timeout = Timeout(15.seconds) // sending mails might take a little longer

  def sendEmail(meta: EventTransactionMetadata, emailSubject: String, body: String, replyTo: Option[String], bcc: Option[String], person: PersonEmailData, docReference: Option[String], mailContext: Product)(implicit originator: PersonId = meta.originator, executionContext: ExecutionContext): Unit = {
    generateMail(emailSubject, body, mailContext) match {
      case Success(mailPayload) =>
        person.email map { email =>
          val mail = bcc match {
            case Some(bccAddress) => mailPayload.toMail(1, email, None, Some(bccAddress), replyTo, docReference)
            case None             => mailPayload.toMail(1, email, None, None, replyTo, docReference)
          }
          mailService ? SendMailCommandWithCallback(originator, mail, Some(60 minutes), person.id) map {
            case _: SendMailEvent =>
            //ok
            case other =>
              logger.debug(s"Sending Mail failed resulting in $other")
          }
        }
      case Failure(e) =>
        logger.warn(s"Failed preparing mail", e)
    }
  }
}
