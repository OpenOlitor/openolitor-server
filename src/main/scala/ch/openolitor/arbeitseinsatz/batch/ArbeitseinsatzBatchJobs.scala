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
package ch.openolitor.arbeitseinsatz.batch

import ch.openolitor.core.SystemConfig
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.actor.Props
import ch.openolitor.arbeitseinsatz.repositories.DefaultArbeitseinsatzWriteRepositoryComponent
import ch.openolitor.core.batch.BaseBatchJobsSupervisor
import org.apache.pekko.actor.ActorRef
import ch.openolitor.arbeitseinsatz.batch.calculations.ArbeitseinsatzStatusUpdater

object ArbeitseinsatzBatchJobs {
  def props(sysConfig: SystemConfig, system: ActorSystem, entityStore: ActorRef): Props = Props(classOf[DefaultArbeitseinsatzBatchJobs], sysConfig, system, entityStore)
}

class ArbeitseinsatzBatchJobs(val sysConfig: SystemConfig, val system: ActorSystem, val entityStore: ActorRef) extends BaseBatchJobsSupervisor {
  override lazy val batchJobs = Set(
    context.actorOf(ArbeitseinsatzStatusUpdater.props(sysConfig, system, entityStore))
  )
}

class DefaultArbeitseinsatzBatchJobs(override val sysConfig: SystemConfig, override val system: ActorSystem, override val entityStore: ActorRef) extends ArbeitseinsatzBatchJobs(sysConfig, system, entityStore) with DefaultArbeitseinsatzWriteRepositoryComponent
