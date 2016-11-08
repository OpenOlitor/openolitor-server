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

import ch.openolitor.core._
import ch.openolitor.core.db._
import ch.openolitor.core.domain._
import ch.openolitor.arbeitseinsatz._
import ch.openolitor.arbeitseinsatz.models._
import ch.openolitor.arbeitseinsatz.repositories._
import java.util.UUID
import scalikejdbc.DB
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.domain.EntityStore._
import akka.actor.ActorSystem
import ch.openolitor.core.Macros._
import scala.concurrent.ExecutionContext.Implicits.global
import ch.openolitor.core.models._
import org.joda.time.DateTime
import ch.openolitor.core.Macros._
import scala.collection.immutable.TreeMap
import scalaz._
import Scalaz._
import ch.openolitor.util.IdUtil
import scalikejdbc.DBSession

object ArbeitseinsatzInsertService {
  def apply(implicit sysConfig: SystemConfig, system: ActorSystem): ArbeitseinsatzInsertService = new DefaultArbeitseinsatzInsertService(sysConfig, system)
}

class DefaultArbeitseinsatzInsertService(sysConfig: SystemConfig, override val system: ActorSystem)
  extends ArbeitseinsatzInsertService(sysConfig) with DefaultArbeitseinsatzWriteRepositoryComponent

/**
 * Actor zum Verarbeiten der Insert Anweisungen für das Arbeitseinsatz Modul
 */
class ArbeitseinsatzInsertService(override val sysConfig: SystemConfig) extends EventService[EntityInsertedEvent[_, _]]
    with LazyLogging
    with AsyncConnectionPoolContextAware
    with ArbeitseinsatzDBMappings {
  self: ArbeitseinsatzWriteRepositoryComponent =>

  val ZERO = 0
  val FALSE = false

  val handle: Handle = {
    case EntityInsertedEvent(meta, id: ArbeitskategorieId, arbeitskategorie: ArbeitskategorieModify) =>
      createArbeitskategorie(meta, id, arbeitskategorie)
    case EntityInsertedEvent(meta, id: ArbeitsangebotId, arbeitsangebot: ArbeitsangebotModify) =>
      createArbeitsangebot(meta, id, arbeitsangebot)
    case EntityInsertedEvent(meta, id: ArbeitseinsatzId, arbeitseinsatz: ArbeitseinsatzModify) =>
      createArbeitseinsatz(meta, id, arbeitseinsatz)
    case e =>
  }

  def createArbeitskategorie(meta: EventMetadata, id: ArbeitskategorieId, arbeitskategorie: ArbeitskategorieModify)(implicit personId: PersonId = meta.originator) = {
    val ak = copyTo[ArbeitskategorieModify, Arbeitskategorie](
      arbeitskategorie,
      "id" -> id,
      "erstelldat" -> meta.timestamp,
      "ersteller" -> meta.originator,
      "modifidat" -> meta.timestamp,
      "modifikator" -> meta.originator
    )

    DB autoCommit { implicit session =>
      //create arbeitskategorie
      arbeitseinsatzWriteRepository.insertEntity[Arbeitskategorie, ArbeitskategorieId](ak)
    }
  }

  def createArbeitsangebot(meta: EventMetadata, id: ArbeitsangebotId, arbeitsangebot: ArbeitsangebotModify)(implicit personId: PersonId = meta.originator) = {
    val aa = copyTo[ArbeitsangebotModify, Arbeitsangebot](
      arbeitsangebot,
      "id" -> id,
      "status" -> InVorbereitung,
      "erstelldat" -> meta.timestamp,
      "ersteller" -> meta.originator,
      "modifidat" -> meta.timestamp,
      "modifikator" -> meta.originator
    )

    DB autoCommit { implicit session =>
      //create arbeitsangebot
      arbeitseinsatzWriteRepository.insertEntity[Arbeitsangebot, ArbeitsangebotId](aa)
    }
  }

  def createArbeitseinsatz(meta: EventMetadata, id: ArbeitseinsatzId, arbeitseinsatz: ArbeitseinsatzModify)(implicit personId: PersonId = meta.originator) = {
    val ae = copyTo[ArbeitseinsatzModify, Arbeitseinsatz](
      arbeitseinsatz,
      "id" -> id,
      "erstelldat" -> meta.timestamp,
      "ersteller" -> meta.originator,
      "modifidat" -> meta.timestamp,
      "modifikator" -> meta.originator
    )

    DB autoCommit { implicit session =>
      //create arbeitseinsatz
      arbeitseinsatzWriteRepository.insertEntity[Arbeitseinsatz, ArbeitseinsatzId](ae)
    }
  }

}
