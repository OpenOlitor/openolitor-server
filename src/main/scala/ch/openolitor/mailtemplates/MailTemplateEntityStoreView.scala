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

import akka.actor.{ ActorRef, ActorSystem, Props }
import ch.openolitor.core.SystemConfig
import ch.openolitor.core.db.ConnectionPoolContextAware
import ch.openolitor.core.domain._
import ch.openolitor.mailtemplates.repositories.{ DefaultMailTemplateWriteRepositoryComponent, MailTemplateWriteRepositoryComponent }

object MailTemplateEntityStoreView {
  def props(dbEvolutionActor: ActorRef, airbrakeNotifier: ActorRef)(implicit sysConfig: SystemConfig, system: ActorSystem): Props =
    Props(classOf[DefaultMailTemplateEntityStoreView], dbEvolutionActor, sysConfig, system, airbrakeNotifier)
}

class DefaultMailTemplateEntityStoreView(override val dbEvolutionActor: ActorRef, implicit val sysConfig: SystemConfig, implicit val system: ActorSystem, val airbrakeNotifier: ActorRef)
  extends MailTemplateEntityStoreView with DefaultMailTemplateWriteRepositoryComponent

trait MailTemplateEntityStoreView
  extends EntityStoreView with MailTemplateEntityStoreViewComponent with ConnectionPoolContextAware {
  self: MailTemplateWriteRepositoryComponent =>

  override val module = "mailtemplate"
}

trait MailTemplateEntityStoreViewComponent extends EntityStoreViewComponent {
  val sysConfig: SystemConfig
  val system: ActorSystem

  override val updateService = MailTemplateUpdateService(sysConfig, system)
  override val insertService = MailTemplateInsertService(sysConfig, system)
  override val deleteService = MailTemplateDeleteService(sysConfig, system)
  override val aktionenService: EventService[PersistentEvent] = ignore()

  def ignore[A <: ch.openolitor.core.domain.PersistentEvent]() = new EventService[A] {
    override val handle: Handle = {
      case e =>
    }
  }
}
