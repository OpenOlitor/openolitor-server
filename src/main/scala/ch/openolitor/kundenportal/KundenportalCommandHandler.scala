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
package ch.openolitor.kundenportal

import scala.util.{ Failure, Success, Try }

import ch.openolitor.buchhaltung.BuchhaltungDBMappings
import ch.openolitor.core.SystemConfig
import ch.openolitor.core.db.{ AsyncConnectionPoolContextAware, ConnectionPoolContextAware }
import ch.openolitor.core.exceptions.InvalidStateException
import ch.openolitor.core.models.PersonId
import ch.openolitor.core.security.Subject
import ch.openolitor.kundenportal.repositories._
import ch.openolitor.arbeitseinsatz.ArbeitseinsatzDBMappings
import ch.openolitor.core.domain.{ CommandHandler, EntityStore, EventTransactionMetadata, UserCommand, IdFactory }
import ch.openolitor.kundenportal.repositories.{ DefaultKundenportalReadRepositorySyncComponent, KundenportalReadRepositorySyncComponent }
import ch.openolitor.stammdaten.models.{ AboId, AbwesenheitCreate, AbwesenheitId, Offen }
import ch.openolitor.arbeitseinsatz.models._
import ch.openolitor.core.Macros._

import akka.actor.ActorSystem
import scalikejdbc.DB
import com.typesafe.scalalogging.LazyLogging
import org.joda.time.DateTime

object KundenportalCommandHandler {
  case class AbwesenheitErstellenCommand(originator: PersonId, subject: Subject, entity: AbwesenheitCreate) extends UserCommand
  case class AbwesenheitLoeschenCommand(originator: PersonId, subject: Subject, aboId: AboId, abwesenheitId: AbwesenheitId) extends UserCommand
  case class ArbeitseinsatzErstellenCommand(originator: PersonId, subject: Subject, entity: ArbeitseinsatzCreate) extends UserCommand
  case class ArbeitseinsatzModifizierenCommand(originator: PersonId, subject: Subject, id: ArbeitseinsatzId, entity: ArbeitseinsatzCreate) extends UserCommand
  case class ArbeitseinsatzLoeschenCommand(originator: PersonId, subject: Subject, arbeitseinsatzId: ArbeitseinsatzId) extends UserCommand
}

trait KundenportalCommandHandler extends CommandHandler with BuchhaltungDBMappings with ArbeitseinsatzDBMappings with ConnectionPoolContextAware with AsyncConnectionPoolContextAware with LazyLogging {
  self: KundenportalReadRepositorySyncComponent =>
  import KundenportalCommandHandler._
  import EntityStore._

  override val handle: PartialFunction[UserCommand, IdFactory => EventTransactionMetadata => Try[Seq[ResultingEvent]]] = {
    case AbwesenheitErstellenCommand(personId, subject, entity: AbwesenheitCreate) => idFactory => meta =>
      DB readOnly { implicit session =>
        kundenportalReadRepository.getAbo(entity.aboId) map { abo =>
          if (subject.kundeId == abo.kundeId && abo.id == entity.aboId) {
            handleEntityInsert[AbwesenheitCreate, AbwesenheitId](idFactory, meta, entity, AbwesenheitId.apply)
          } else {
            Failure(new InvalidStateException("Es können nur Abwesenheiten auf eigenen Abos erstellt werden."))
          }
        } getOrElse (Failure(new InvalidStateException(s"Das Abo dieser Abwesenheit wurden nicht gefunden.")))
      }

    case AbwesenheitLoeschenCommand(personId, subject, aboId, abwesenheitId) => idFactory => meta =>
      DB readOnly { implicit session =>
        kundenportalReadRepository.getAbo(aboId) map { abo =>
          if (subject.kundeId == abo.kundeId) {
            kundenportalReadRepository.getAbwesenheit(abwesenheitId) map { abwesenheit =>
              kundenportalReadRepository.getLieferplanungByLieferung(abwesenheit.lieferungId) map { lieferplanung =>
                if (lieferplanung.status == Offen) {
                  Success(Seq(EntityDeleteEvent(abwesenheitId)))
                } else {
                  Failure(new InvalidStateException(s"Lieferplanung bereits abgeschlossen."))
                }
              } getOrElse (Success(Seq(EntityDeleteEvent(abwesenheitId))))
            } getOrElse (Failure(new InvalidStateException(s"Die Abwesenheit konnte wurde nicht gefunden.")))
          } else {
            Failure(new InvalidStateException("Es können nur Abwesenheiten eigener Abos entfernt werden."))
          }
        } getOrElse (Failure(new InvalidStateException(s"Das Abo dieser Abwesenheit wurden nicht gefunden.")))
      }

    case ArbeitseinsatzErstellenCommand(personId, subject, entity: ArbeitseinsatzCreate) => idFactory => meta =>
      DB readOnly { implicit session =>
        kundenportalReadRepository.getArbeitsangebot(entity.arbeitsangebotId) map { arbeitsangebot =>
          //TODO check if kunde may subscribe
          if (arbeitsangebot.status == Bereit) {
            val entityToInsert = new ArbeitseinsatzModify(
              entity.arbeitsangebotId,
              arbeitsangebot.zeitVon,
              arbeitsangebot.zeitBis,
              arbeitsangebot.einsatzZeit,
              entity.kundeId,
              Some(subject.personId),
              None,
              entity.anzahlPersonen,
              entity.bemerkungen
            )
            handleEntityInsert[ArbeitseinsatzModify, ArbeitseinsatzId](idFactory, meta, entityToInsert, ArbeitseinsatzId.apply)
          } else {
            Failure(new InvalidStateException("Es können nur Arbeitseinsätze in Arbeitsangeboten im Status 'Bereit' erstellt werden."))
          }
        } getOrElse (Failure(new InvalidStateException(s"Das Arbeitsangebot wurde nicht gefunden.")))
      }

    case ArbeitseinsatzModifizierenCommand(personId, subject, id: ArbeitseinsatzId, entity: ArbeitseinsatzCreate) => idFactory => meta =>
      DB readOnly { implicit session =>
        kundenportalReadRepository.getProjekt map { projekt =>
          kundenportalReadRepository.getById(arbeitseinsatzMapping, id) map { arbeitseinsatz =>
            kundenportalReadRepository.getArbeitsangebot(entity.arbeitsangebotId) map { arbeitsangebot =>
              if (arbeitsangebot.status == Bereit) {
                if (arbeitsangebot.zeitVon isAfter DateTime.now.plusDays(projekt.einsatzAbsageVorlaufTage)) {
                  val entityToSave = copyTo[Arbeitseinsatz, ArbeitseinsatzModify](arbeitseinsatz, "anzahlPersonen" -> entity.anzahlPersonen, "bemerkungen" -> entity.bemerkungen)
                  Success(Seq(EntityUpdateEvent(id, entityToSave)))
                } else {
                  Failure(new InvalidStateException(s"Arbeitseinsätze können nur bis ${projekt.einsatzAbsageVorlaufTage} Tage vor Start modifiziert werden."))
                }
              } else {
                Failure(new InvalidStateException("Es können nur Arbeitseinsätze in Arbeitsangeboten im Status 'Bereit' modifiziert werden."))
              }
            } getOrElse (Failure(new InvalidStateException(s"Das Arbeitsangebot wurde nicht gefunden.")))
          } getOrElse (Failure(new InvalidStateException(s"Der zu modifizierende Arbeitseinsatz wurde nicht gefunden.")))
        } getOrElse (Failure(new InvalidStateException(s"Projekt konnte nicht geladen werden.")))
      }

    case ArbeitseinsatzLoeschenCommand(personId, subject, arbeitseinsatzId) => idFactory => meta =>
      DB readOnly { implicit session =>
        kundenportalReadRepository.getProjekt map { projekt =>
          kundenportalReadRepository.getArbeitseinsatzDetail(arbeitseinsatzId) map { arbeitseinsatz =>
            if (arbeitseinsatz.arbeitsangebot.status == Bereit) {
              if (arbeitseinsatz.arbeitsangebot.zeitVon isAfter DateTime.now.plusDays(projekt.einsatzAbsageVorlaufTage)) {
                Success(Seq(EntityDeleteEvent(arbeitseinsatzId)))
              } else {
                Failure(new InvalidStateException(s"Arbeitseinsätze können nur bis ${projekt.einsatzAbsageVorlaufTage} Tage vor Start gelöscht werden."))
              }
            } else {
              Failure(new InvalidStateException("Es können nur Arbeitseinsätze in Arbeitsangeboten im Status 'Bereit' entfernt werden."))
            }
          } getOrElse {
            Failure(new InvalidStateException(s"Das Arbeitsangebot oder der Arbeitseinsatz wurden nicht gefunden."))
          }
        } getOrElse {
          Failure(new InvalidStateException(s"Projekt konnte nicht geladen werden."))
        }
      }

  }
}

class DefaultKundenportalCommandHandler(override val sysConfig: SystemConfig, override val system: ActorSystem) extends KundenportalCommandHandler
  with DefaultKundenportalReadRepositorySyncComponent {
}
