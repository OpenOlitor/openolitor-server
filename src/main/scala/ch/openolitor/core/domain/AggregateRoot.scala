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

import akka.persistence._
import akka.actor._
import ch.openolitor.core.models.PersonId

trait State

trait Command

trait UserCommand extends Command {
  val originator: PersonId
}

object AggregateRoot {
  case object KillAggregate extends Command

  case object GetState extends Command

  case object Removed extends State
  case object Created extends State
  case object Uninitialized extends State

  case class RestoreViewFromState(originator: PersonId, sequenceNr: Long, state: State) extends Command
  case class InitializeViewLive(originator: PersonId, fromSequenceNr: Long) extends Command
}

trait AggregateRoot extends PersistentActor with ActorLogging with PersistenceEventStateSupport {

  type S <: State
  var state: S
  private var lastAquiredTransactionNr: Long = 0L

  case class Initialize(state: S) extends Command

  def updateState(recovery: Boolean = false)(evt: PersistentEvent): Unit
  def restoreFromSnapshot(metadata: SnapshotMetadata, state: State): Unit

  def afterRecoveryCompleted(sequenceNr: Long, state: State): Unit = {}

  def now: Long = System.currentTimeMillis

  override val persistenceStateStoreId: String = persistenceId

  override def dbInitialized(): Unit = {
    lastAquiredTransactionNr = lastProcessedTransactionNr
    log.debug(s"$persistenceId: initialize aquire transaction nr to $lastAquiredTransactionNr")
  }

  protected final def afterEventPersisted(evt: PersistentEvent): Unit = {
    updateState(recovery = false)(evt)
    publish(evt)

    setLastProcessedSequenceNr(evt.meta)
  }

  protected def aquireTransactionNr(): Long = {
    lastAquiredTransactionNr += 1
    lastAquiredTransactionNr
  }

  protected def publish(event: Object): Unit =
    context.system.eventStream.publish(event)

  override val receiveRecover: Receive = {
    case evt: PersistentEvent =>
      updateState(evt.meta.seqNr >= lastProcessedSequenceNr)(evt)
    case SnapshotOffer(metadata, state: State) =>
      restoreFromSnapshot(metadata, state)
      log.debug("recovering aggregate from snapshot")
    case RecoveryCompleted =>
      afterRecoveryCompleted(lastSequenceNr, state)
  }
}
