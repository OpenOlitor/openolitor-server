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
import ch.openolitor.core.domain.{ EventMetadata, EventTransactionMetadata }
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

trait MailCommandForwarder {
  def sendEmail(meta: EventTransactionMetadata, emailSubject: String, body: String, replyTo: Option[String], bcc: Option[String], person: PersonEmailData, docReference: Option[String], mailContext: Product)(implicit originator: PersonId = meta.originator, executionContext: ExecutionContext): Unit
}

object MailCommandForwarder {
  def apply(mailService: ActorRef): MailCommandForwarder = {
    new DefaultMailCommandForwarder(mailService)
  }
}