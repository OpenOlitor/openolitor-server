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

import akka.actor.ActorSystem
import ch.openolitor.core.SystemConfig
import ch.openolitor.core.db.ConnectionPoolContextAware
import ch.openolitor.core.domain.EntityStore.{ InsertEntityCommand, ResultingEvent }
import ch.openolitor.core.domain.{ CommandHandler, EventTransactionMetadata, IdFactory, UserCommand }
import ch.openolitor.mailtemplates.model.{ MailTemplateId, MailTemplateModify }
import ch.openolitor.mailtemplates.repositories.{ MailTemplateDBMappings, MailTemplateReadRepositorySync, MailTemplateReadRepositorySyncImpl }

import scala.util.Try

trait MailTemplateCommandHandler extends CommandHandler with MailTemplateDBMappings with ConnectionPoolContextAware {
  self: MailTemplateReadRepositorySync =>

  override val handle: PartialFunction[UserCommand, IdFactory => EventTransactionMetadata => Try[Seq[ResultingEvent]]] = {
    case e @ InsertEntityCommand(personIdd, entity: MailTemplateModify) => idFactory => meta =>
      handleEntityInsert[MailTemplateModify, MailTemplateId](idFactory, meta, entity, MailTemplateId.apply)
  }
}

class DefaultMailTemplateCommandHanlder(val sysConfig: SystemConfig, val system: ActorSystem)
  extends MailTemplateCommandHandler with MailTemplateReadRepositorySyncImpl {

}