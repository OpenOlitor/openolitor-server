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
package ch.openolitor.mailtemplates

import scalikejdbc._
import ch.openolitor.mailtemplates.model._
import ch.openolitor.mailtemplates.repositories._
import ch.openolitor.core.db.AsyncConnectionPoolContextAware
import ch.openolitor.core.domain.EntityStore._
import ch.openolitor.core.models._
import ch.openolitor.core.domain._
import ch.openolitor.core.repositories.EventPublishingImplicits._
import ch.openolitor.core.repositories.EventPublisher
import ch.openolitor.core.Macros._
import com.typesafe.scalalogging.LazyLogging
import akka.actor.ActorSystem
import ch.openolitor.core.SystemConfig

object MailTemplateUpdateService {
  def apply(implicit sysConfig: SystemConfig, system: ActorSystem): MailTemplateUpdateService = new DefaultMailTemplateUpdateService(sysConfig, system)
}

class DefaultMailTemplateUpdateService(sysConfig: SystemConfig, val system: ActorSystem) extends MailTemplateUpdateService(sysConfig) with DefaultMailTemplateWriteRepositoryComponent {
}

class MailTemplateUpdateService(override val sysConfig: SystemConfig) extends EventService[EntityUpdatedEvent[_ <: BaseId, _ <: AnyRef]]
  with LazyLogging
  with AsyncConnectionPoolContextAware
  with MailTemplateDBMappings {
  self: MailTemplateWriteRepositoryComponent =>

  // implicitly expose the eventStream
  implicit lazy val mailTemplateepositoryImplicit = mailTemplateWriteRepository

  val mailTemplateUpdateHandle: Handle = {
    case EntityUpdatedEvent(meta, id: MailTemplateId, update: MailTemplateModify) => updateMailTemplate(meta, id, update)
    case e =>
  }

  def updateMailTemplate(meta: EventMetadata, id: MailTemplateId, update: MailTemplateModify)(implicit personId: PersonId = meta.originator) = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      mailTemplateWriteRepository.getById(mailTemplateMapping, id) map { template =>
        val copy = copyFrom(template, update,
          "modifidat" -> meta.timestamp, "modifikator" -> personId)
        mailTemplateWriteRepository.updateEntityFully[MailTemplate, MailTemplateId](copy)
      }
    }
  }

  val handle: Handle = mailTemplateUpdateHandle
}
