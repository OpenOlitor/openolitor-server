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

import akka.actor.ActorSystem
import ch.openolitor.arbeitseinsatz.models._
import ch.openolitor.arbeitseinsatz.repositories._
import ch.openolitor.core.Macros._
import ch.openolitor.core._
import ch.openolitor.core.db._
import ch.openolitor.core.domain.EntityStore._
import ch.openolitor.core.domain._
import ch.openolitor.core.models.PersonId
import ch.openolitor.core.repositories.EventPublishingImplicits._
import com.typesafe.scalalogging.LazyLogging
import scalikejdbc.DB

object ArbeitseinsatzUpdateService {
  def apply(implicit sysConfig: SystemConfig, system: ActorSystem): ArbeitseinsatzUpdateService = new DefaultArbeitseinsatzUpdateService(sysConfig, system)
}

class DefaultArbeitseinsatzUpdateService(sysConfig: SystemConfig, override val system: ActorSystem)
    extends ArbeitseinsatzUpdateService(sysConfig) with DefaultArbeitseinsatzWriteRepositoryComponent {
}

/**
 * Actor zum Verarbeiten der Update Anweisungen innerhalb des Arbeitseinsatz Moduls
 */
class ArbeitseinsatzUpdateService(override val sysConfig: SystemConfig) extends EventService[EntityUpdatedEvent[_, _]] with LazyLogging with AsyncConnectionPoolContextAware with ArbeitseinsatzDBMappings {
  self: ArbeitseinsatzWriteRepositoryComponent =>

  val FALSE = false

  val handle: Handle = {
    case EntityUpdatedEvent(meta, id: ArbeitskategorieId, entity: ArbeitskategorieModify) => updateArbeitskategorie(meta, id, entity)
    case EntityUpdatedEvent(meta, id: ArbeitsangebotId, entity: ArbeitsangebotModify) => updateArbeitsangebot(meta, id, entity)
    case EntityUpdatedEvent(meta, id: ArbeitseinsatzId, entity: ArbeitseinsatzModify) => updateArbeitseinsatz(meta, id, entity)
    case e =>
  }

  def updateArbeitskategorie(meta: EventMetadata, id: ArbeitskategorieId, update: ArbeitskategorieModify)(implicit personId: PersonId = meta.originator) = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      arbeitseinsatzWriteRepository.getById(arbeitskategorieMapping, id) map { arbeitskategorie =>
        //map all updatable fields
        val copy = copyFrom(arbeitskategorie, update, "modifidat" -> meta.timestamp, "modifikator" -> personId)
        arbeitseinsatzWriteRepository.updateEntityFully[Arbeitskategorie, ArbeitskategorieId](copy)
      }
    }
  }

  def updateArbeitsangebot(meta: EventMetadata, id: ArbeitsangebotId, update: ArbeitsangebotModify)(implicit personId: PersonId = meta.originator) = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      arbeitseinsatzWriteRepository.getById(arbeitsangebotMapping, id) map { arbeitsangebot =>
        //map all updatable fields
        val copy = copyFrom(arbeitsangebot, update, "modifidat" -> meta.timestamp, "modifikator" -> personId)
        arbeitseinsatzWriteRepository.updateEntityFully[Arbeitsangebot, ArbeitsangebotId](copy)
      }
    }
  }

  def updateArbeitseinsatz(meta: EventMetadata, id: ArbeitseinsatzId, update: ArbeitseinsatzModify)(implicit personId: PersonId = meta.originator) = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      arbeitseinsatzWriteRepository.getById(arbeitseinsatzMapping, id) map { arbeitskategorie =>
        //map all updatable fields
        val copy = copyFrom(arbeitskategorie, update, "modifidat" -> meta.timestamp, "modifikator" -> personId)
        arbeitseinsatzWriteRepository.updateEntityFully[Arbeitseinsatz, ArbeitseinsatzId](copy)
      }
    }
  }

}
