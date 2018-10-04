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

import akka.actor.{ ActorRef, ActorSystem }
import ch.openolitor.arbeitseinsatz.eventsourcing.ArbeitseinsatzEventStoreSerializer
import ch.openolitor.arbeitseinsatz.repositories._
import ch.openolitor.core._
import ch.openolitor.core.db._
import ch.openolitor.core.domain._
import com.typesafe.scalalogging.LazyLogging

object ArbeitseinsatzAktionenService {
  def apply(implicit sysConfig: SystemConfig, system: ActorSystem, mailService: ActorRef): ArbeitseinsatzAktionenService = new DefaultArbeitseinsatzAktionenService(sysConfig, system, mailService)
}

class DefaultArbeitseinsatzAktionenService(sysConfig: SystemConfig, override val system: ActorSystem, override val mailService: ActorRef)
  extends ArbeitseinsatzAktionenService(sysConfig, mailService) with DefaultArbeitseinsatzWriteRepositoryComponent {
}

/**
 * Actor zum Verarbeiten der Aktionen für das Arbeitseinsatz Modul
 */
class ArbeitseinsatzAktionenService(override val sysConfig: SystemConfig, override val mailService: ActorRef) extends EventService[PersistentEvent] with LazyLogging with AsyncConnectionPoolContextAware
  with ArbeitseinsatzDBMappings with MailServiceReference with ArbeitseinsatzEventStoreSerializer {
  self: ArbeitseinsatzWriteRepositoryComponent =>

  lazy val config = sysConfig.mandantConfiguration.config

  val handle: Handle = {
    case e =>
      logger.warn(s"Unknown event:$e")
  }

}
