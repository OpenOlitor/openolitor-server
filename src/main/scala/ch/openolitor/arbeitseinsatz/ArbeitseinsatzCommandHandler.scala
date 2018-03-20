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
import ch.openolitor.core._
import ch.openolitor.core.db.ConnectionPoolContextAware
import ch.openolitor.core.domain._

import scala.util._

object ArbeitseinsatzCommandHandler {
  //case class LieferplanungAbschliessenCommand(originator: PersonId, id: LieferplanungId) extends UserCommand
}

trait ArbeitseinsatzCommandHandler extends CommandHandler with ArbeitseinsatzDBMappings with ConnectionPoolContextAware {
  self: ArbeitseinsatzWriteRepositoryComponent =>
  import EntityStore._

  override val handle: PartialFunction[UserCommand, IdFactory => EventTransactionMetadata => Try[Seq[ResultingEvent]]] = {
    /*
     * Insert command handling
     */
    case e @ InsertEntityCommand(personId, entity: ArbeitskategorieModify) => idFactory => meta =>
      handleEntityInsert[ArbeitskategorieModify, ArbeitskategorieId](idFactory, meta, entity, ArbeitskategorieId.apply)
    case e @ InsertEntityCommand(personId, entity: ArbeitsangebotModify) => idFactory => meta =>
      handleEntityInsert[ArbeitsangebotModify, ArbeitsangebotId](idFactory, meta, entity, ArbeitsangebotId.apply)
    case e @ InsertEntityCommand(personId, entity: ArbeitseinsatzModify) => idFactory => meta =>
      handleEntityInsert[ArbeitseinsatzModify, ArbeitseinsatzId](idFactory, meta, entity, ArbeitseinsatzId.apply)

    /*
    * Custom update command handling
    */

  }
}

class DefaultArbeitseinsatzCommandHandler(override val sysConfig: SystemConfig, override val system: ActorSystem) extends ArbeitseinsatzCommandHandler
  with DefaultArbeitseinsatzWriteRepositoryComponent {
}
