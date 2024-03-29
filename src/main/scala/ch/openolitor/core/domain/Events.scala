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
package ch.openolitor.core.domain

import ch.openolitor.core.models.PersonId
import org.joda.time.DateTime
import ch.openolitor.core.JSONSerializable
import ch.openolitor.stammdaten.models.SecondFactorType

case class EventTransactionMetadata(originator: PersonId, version: Int, timestamp: DateTime, transactionNr: Long, source: String) {
  def toMetadata(seqNr: Long) = {
    EventMetadata(
      originator,
      version,
      timestamp,
      transactionNr,
      seqNr,
      source
    )
  }
}

/**
 * Event metadata managed by OpenOlitor.
 *
 * @param originator    the originator of the command that led to this event metadata.
 * @param version       the version of the event store or more specific aggregate root.
 * @param timestamp     the timestamp when the event has been created by the event store.
 * @param transactionNr the transaction number identifying one or more events that resulted out of a command.
 * @param seqNr         this is not to be confused with the sequenceNr which is managed by akka persistence. It is just representing the sequence nr of
 *                      events within the given transactionNr. The name is misleading and unfortunately often mistaken for akka persistence's sequenceId.
 * @param source        the persistence id of the aggregate root that created the event with this metadata.
 */
case class EventMetadata(originator: PersonId, version: Int, timestamp: DateTime, transactionNr: Long, seqNr: Long, source: String)

trait PersistentEvent extends Serializable {
  val meta: EventMetadata
}

trait PersistentGeneratedEvent extends PersistentEvent

case class PersistentSystemEvent(meta: EventMetadata, event: SystemEvent) extends PersistentEvent

trait SystemEvent extends JSONSerializable

object SystemEvents {

  val SystemPersonId = PersonId(0)

  case class PersonLoggedIn(personId: PersonId, timestamp: DateTime, secondFactorType: Option[SecondFactorType]) extends SystemEvent with JSONSerializable

  case class PersonChangedOtpSecret(personId: PersonId, secret: String) extends SystemEvent with JSONSerializable

  case class PersonChangedSecondFactorType(personId: PersonId, secondFactorType: Option[SecondFactorType]) extends SystemEvent with JSONSerializable

  case class SystemStarted(timestamp: DateTime) extends SystemEvent
}
