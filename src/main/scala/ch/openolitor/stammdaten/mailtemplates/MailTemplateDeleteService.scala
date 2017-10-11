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
package ch.openolitor.stammdaten.mailtemplates

import scalikejdbc._
import ch.openolitor.stammdaten.mailtemplates.model._
import ch.openolitor.stammdaten.mailtemplates.repositories._
import ch.openolitor.core.db.AsyncConnectionPoolContextAware
import ch.openolitor.core.domain.EntityStore._
import ch.openolitor.core.models._
import ch.openolitor.core.domain._
import ch.openolitor.core.repositories.EventPublishingImplicits._
import ch.openolitor.core.repositories.EventPublisher
import ch.openolitor.core.Macros._
import com.typesafe.scalalogging.LazyLogging

trait MailTemplateDeleteService extends EventService[EntityDeletedEvent[_ <: BaseId]]
    with LazyLogging
    with AsyncConnectionPoolContextAware
    with MailTemplateDBMappings {
  self: MailTemplateWriteRepositoryComponent =>

  // implicitly expose the eventStream
  implicit val mailTemplateepositoryImplicit = mailTemplateWriteRepository

  val mailTemplateDeleteHandle: Handle = {
    case EntityDeletedEvent(meta, id: MailTemplateId) =>
      deleteMailTemplate(meta, id)
  }

  def deleteMailTemplate(meta: EventMetadata, id: MailTemplateId)(implicit personId: PersonId = meta.originator) = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      mailTemplateWriteRepository.deleteEntity[MailTemplate, MailTemplateId](id)
    }
  }
}

