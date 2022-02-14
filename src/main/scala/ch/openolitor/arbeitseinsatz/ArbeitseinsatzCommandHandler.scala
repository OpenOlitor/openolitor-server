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

import ch.openolitor.arbeitseinsatz.models._
import ch.openolitor.core.Macros._
import ch.openolitor.arbeitseinsatz.repositories._
import ch.openolitor.core._
import ch.openolitor.core.exceptions.InvalidStateException
import ch.openolitor.core.db.ConnectionPoolContextAware
import ch.openolitor.core.domain._
import ch.openolitor.core.models.PersonId
import ch.openolitor.stammdaten.models.{ Person, PersonContactPermissionModify }
import ch.openolitor.mailtemplates.engine.MailTemplateService
import akka.actor.ActorSystem
import ch.openolitor.core.security.Subject
import scalikejdbc._

import scala.concurrent.ExecutionContext.Implicits._
import scala.util._

object ArbeitseinsatzCommandHandler {
  case class ArbeitsangebotArchivedCommand(id: ArbeitsangebotId, originator: PersonId = PersonId(100)) extends UserCommand
  case class SendEmailToArbeitsangebotPersonenCommand(originator: PersonId, subject: String, body: String, replyTo: Option[String], ids: Seq[ArbeitsangebotId]) extends UserCommand
  case class SendEmailToArbeitsangebotPersonenEvent(meta: EventMetadata, subject: String, body: String, replyTo: Option[String], person: Person, context: ArbeitsangebotMailContext) extends PersistentGeneratedEvent with JSONSerializable
  case class ChangeContactPermissionForUserCommand(originator: PersonId, subject: Subject, personId: PersonId) extends UserCommand
}

trait ArbeitseinsatzCommandHandler extends CommandHandler with ArbeitseinsatzDBMappings with ConnectionPoolContextAware with MailTemplateService {
  self: ArbeitseinsatzReadRepositorySyncComponent =>
  import ArbeitseinsatzCommandHandler._
  import EntityStore._

  override val handle: PartialFunction[UserCommand, IdFactory => EventTransactionMetadata => Try[Seq[ResultingEvent]]] = {

    /*
    * Custom update command handling
    */
    case ArbeitsangebotArchivedCommand(id, personId) => idFactory => meta =>
      DB readOnly { implicit session =>
        arbeitseinsatzReadRepository.getById(arbeitsangebotMapping, id) map { arbeitsangebot =>
          arbeitsangebot.status match {
            case (Bereit) =>
              val copy = arbeitsangebot.copy(status = Archiviert)
              Success(Seq(EntityUpdateEvent(id, copy)))
            case _ =>
              Failure(new InvalidStateException("Der Arbeitseinsatz muss 'Bereit' sein."))
          }
        } getOrElse Failure(new InvalidStateException(s"Keine Arbeitseinsatz zu Id $id gefunden"))
      }

    case SendEmailToArbeitsangebotPersonenCommand(personId, subject, body, replyTo, ids) => idFactory => meta =>
      DB readOnly { implicit session =>
        if (checkTemplateArbeitsangebot(body, subject, ids)) {
          val events = ids flatMap { arbeitsangebotId: ArbeitsangebotId =>
            arbeitseinsatzReadRepository.getById(arbeitsangebotMapping, arbeitsangebotId) map { arbeitsangebot =>
              arbeitseinsatzReadRepository.getPersonenByArbeitsangebot(arbeitsangebotId) map { person =>
                val mailContext = ArbeitsangebotMailContext(person, arbeitsangebot)
                DefaultResultingEvent(factory => SendEmailToArbeitsangebotPersonenEvent(factory.newMetadata(), subject, body, replyTo, person, mailContext))
              }
            }
          }
          Success(events.flatten)
        } else {
          Failure(new InvalidStateException("The template is not valid"))
        }
      }

    case ChangeContactPermissionForUserCommand(originator, subject, personId) => idFactory => meta =>
      DB readOnly { implicit session =>
        val entityToSave = arbeitseinsatzReadRepository.getById(personMapping, personId) map { user =>
          copyTo[Person, PersonContactPermissionModify](user, "contactPermission" -> !user.contactPermission)
        }
        entityToSave match {
          case Some(personContactPermissionModify) => Success(Seq(EntityUpdateEvent(personId, personContactPermissionModify)))
          case _                                   => Failure(new InvalidStateException(s"This person was not found."))
        }
      }

    /*
     * Insert command handling
     */
    case e @ InsertEntityCommand(personId, entity: ArbeitskategorieModify) => idFactory => meta =>
      handleEntityInsert[ArbeitskategorieModify, ArbeitskategorieId](idFactory, meta, entity, ArbeitskategorieId.apply)
    case e @ InsertEntityCommand(personId, entity: ArbeitsangebotModify) => idFactory => meta =>
      handleEntityInsert[ArbeitsangebotModify, ArbeitsangebotId](idFactory, meta, entity, ArbeitsangebotId.apply)
    case e @ InsertEntityCommand(personId, entity: ArbeitseinsatzModify) => idFactory => meta =>
      handleEntityInsert[ArbeitseinsatzModify, ArbeitseinsatzId](idFactory, meta, entity, ArbeitseinsatzId.apply)
    case e @ InsertEntityCommand(personId, entity: ArbeitsangeboteDuplicate) => idFactory => meta =>
      val events = entity.daten.map { datum =>
        val arbeitsangebotDuplicate = copyTo[ArbeitsangeboteDuplicate, ArbeitsangebotDuplicate](entity, "zeitVon" -> datum)
        insertEntityEvent[ArbeitsangebotDuplicate, ArbeitsangebotId](idFactory, meta, arbeitsangebotDuplicate, ArbeitsangebotId.apply)
      }
      Success(events)
  }

  private def checkTemplateArbeitsangebot(body: String, subject: String, ids: Seq[ArbeitsangebotId])(implicit session: DBSession): Boolean = {
    val templateCorrect = ids flatMap { arbeitsangebotId: ArbeitsangebotId =>
      arbeitseinsatzReadRepository.getPersonenByArbeitsangebot(arbeitsangebotId) flatMap { person =>
        arbeitseinsatzReadRepository.getById(arbeitsangebotMapping, arbeitsangebotId) map { arbeitsangebot =>
          val mailContext = ArbeitsangebotMailContext(person, arbeitsangebot)
          generateMail(subject, body, mailContext) match {
            case Success(mailPayload) => true
            case Failure(e)           => false
          }
        }
      }
    }
    templateCorrect.forall(x => x == true)
  }
}

class DefaultArbeitseinsatzCommandHandler(override val sysConfig: SystemConfig, override val system: ActorSystem) extends ArbeitseinsatzCommandHandler
  with DefaultArbeitseinsatzReadRepositorySyncComponent {

}
