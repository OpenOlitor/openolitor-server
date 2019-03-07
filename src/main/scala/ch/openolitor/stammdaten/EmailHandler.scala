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

import ch.openolitor.core.models._
import ch.openolitor.stammdaten.models._
import ch.openolitor.core.domain.EventMetadata
import ch.openolitor.mailtemplates.engine.MailTemplateService
import ch.openolitor.core.mailservice.MailService._
import ch.openolitor.core.repositories.EventPublishingImplicits._
import ch.openolitor.core.db.AsyncConnectionPoolContextAware
import ch.openolitor.stammdaten.eventsourcing.StammdatenEventStoreSerializer
import ch.openolitor.core.EventStream

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import scalikejdbc._
import com.typesafe.scalalogging.LazyLogging
import scala.util._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

trait EmailHandler extends MailTemplateService with AsyncConnectionPoolContextAware with StammdatenEventStoreSerializer with LazyLogging {

  def sendEmail(meta: EventMetadata, emailSubject: String, body: String, person: Person, mailContext: Product, mailService: ActorRef)(implicit originator: PersonId = meta.originator, timeout: Timeout, executionContext: ExecutionContext, eventStream: EventStream): Unit = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      generateMail(emailSubject, body, mailContext) match {
        case Success(mailPayload) =>
          person.email map { email =>
            val mail = mailPayload.toMail(1, email, None, None, None)
            mailService ? SendMailCommandWithCallback(originator, mail, Some(5 minutes), person.id) map {
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
}
