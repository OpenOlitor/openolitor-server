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

import akka.actor._
import ch.openolitor.core.domain._
import ch.openolitor.core._
import ch.openolitor.core.db.ConnectionPoolContextAware
import ch.openolitor.arbeitseinsatz.repositories._

object ArbeitseinsatzEntityStoreView {
  def props(mailService: ActorRef, dbEvolutionActor: ActorRef)(implicit sysConfig: SystemConfig, system: ActorSystem): Props = Props(classOf[DefaultArbeitseinsatzEntityStoreView], mailService, dbEvolutionActor, sysConfig, system)
}

class DefaultArbeitseinsatzEntityStoreView(override val mailService: ActorRef, val dbEvolutionActor: ActorRef, implicit val sysConfig: SystemConfig, implicit val system: ActorSystem) extends ArbeitseinsatzEntityStoreView
  with DefaultArbeitseinsatzWriteRepositoryComponent

/**
 * Zusammenfügen des Componenten (cake pattern) zu der persistentView
 */
trait ArbeitseinsatzEntityStoreView extends EntityStoreView
    with ArbeitseinsatzEntityStoreViewComponent with ConnectionPoolContextAware {
  self: ArbeitseinsatzWriteRepositoryComponent =>

  override val module = "arbeitseinsatz"

  def initializeEntityStoreView = {
  }
}

/**
 * Instanzieren der jeweiligen Insert, Update und Delete Child Actors
 */
trait ArbeitseinsatzEntityStoreViewComponent extends EntityStoreViewComponent {
  import EntityStore._
  val mailService: ActorRef
  val sysConfig: SystemConfig
  val system: ActorSystem

  override val insertService = ArbeitseinsatzInsertService(sysConfig, system)
  override val updateService = ArbeitseinsatzUpdateService(sysConfig, system)
  override val deleteService = ArbeitseinsatzDeleteService(sysConfig, system)

  override val aktionenService = ArbeitseinsatzAktionenService(sysConfig, system, mailService)
}
