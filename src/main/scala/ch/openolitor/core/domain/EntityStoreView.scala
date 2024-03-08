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

import java.util.concurrent.TimeUnit
import org.apache.pekko.actor.SupervisorStrategy.Restart
import DefaultMessages._

import scala.concurrent.duration._
import org.apache.pekko.actor._
import org.apache.pekko.persistence.jdbc.query.scaladsl.JdbcReadJournal
import org.apache.pekko.persistence.query.{ EventEnvelope, PersistenceQuery }
import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.stream.Materializer

import scala.util._
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.{ AirbrakeNotifierReference, DBEvolutionReference }
import ch.openolitor.core.models.BaseId
import ch.openolitor.util.AirbrakeNotifier.AirbrakeNotificationTermination

trait EventService[E <: PersistentEvent] {
  type Handle = PartialFunction[E, Unit]
  val handle: Handle
}

/**
 * Component mit Referenzen auf weitere Dienste
 */
trait EntityStoreViewComponent extends Actor {
  import EntityStore._
  val insertService: EventService[EntityInsertedEvent[_ <: BaseId, _ <: AnyRef]]
  val updateService: EventService[EntityUpdatedEvent[_ <: BaseId, _ <: AnyRef]]
  val deleteService: EventService[EntityDeletedEvent[_ <: BaseId]]

  val aktionenService: EventService[PersistentEvent]

  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
    case _: Exception => Restart
  }
}

object EntityStoreView

/**
 * Diese generische EntityStoreView delelegiert die Events an die jeweilige modulspezifische ActorRef
 */
trait EntityStoreView extends Actor with DBEvolutionReference with LazyLogging with PersistenceEventStateSupport with AirbrakeNotifierReference {
  self: EntityStoreViewComponent =>
  import EntityStore._

  final case object PrepareTerminate
  final case object Terminate
  var failures = 0

  lazy val readJournal: JdbcReadJournal =
    PersistenceQuery(context.system)
      .readJournalFor[JdbcReadJournal](JdbcReadJournal.Identifier, sysConfig.mandantConfiguration.config)

  def journalSource(fromSequenceNr: Long): Source[Any, NotUsed] =
    readJournal
      .eventsByPersistenceId(
        persistenceId,
        fromSequenceNr = fromSequenceNr,
        toSequenceNr = Long.MaxValue
      )

  def replayJournalSource(fromSequenceNr: Long): Unit = {
    log.debug(s"replayJournalSource: $fromSequenceNr")
    implicit val materializer = Materializer.matFromSystem(context.system)
    journalSource(fromSequenceNr).runForeach {
      case event: EventEnvelope => context.self ! event.event
    }
  }

  def prepareTerminate(): Unit = {
    if (failures > 200) {
      airbrakeNotifier ! AirbrakeNotificationTermination
      log.error("The system was not able to recover. A forced termination is called")
      context.system.scheduler.scheduleOnce(Duration.create(1, TimeUnit.MINUTES), context.self, Terminate)
    } else {
      log.warning("The system recovered from errors on reaching the db")
      failures = 0
    }
  }

  val module: String

  val persistenceId = EntityStore.persistenceId
  def viewId: String = s"$module-entity-store"

  override def persistenceStateStoreId: String = viewId

  /**
   * Delegate to
   */
  val receive: Receive = {
    case Startup =>
      log.debug("Received Startup command")
      startup()
      sender() ! Started
    case PrepareTerminate =>
      log.debug("The prepareTerminate was called")
      prepareTerminate()
    case Terminate =>
      log.debug("The Terminate was called")
      System.exit('R')
    case e: PersistentEvent if e.meta.transactionNr < lastProcessedTransactionNr =>
    // ignore already processed event

    case e: PersistentEvent if e.meta.transactionNr == lastProcessedTransactionNr && e.meta.seqNr <= lastProcessedSequenceNr =>
    // ignore already processed event

    case e: PersistentEvent =>
      processNewEvents(e)
  }

  def startup(): Unit = {
    replayJournalSource(lastProcessedSequenceNr)
  }

  val processNewEvents: Receive = {
    case _: EntityStoreInitialized =>
      log.debug(s"Received EntityStoreInitialized")
    case e: EntityInsertedEvent[_, _] =>
      runSafe(insertService.handle, e)
    case e: EntityUpdatedEvent[_, _] =>
      runSafe(updateService.handle, e)
    case e: EntityDeletedEvent[_] =>
      runSafe(deleteService.handle, e)
    case e: PersistentEvent =>
      // handle custom events
      runSafe(aktionenService.handle, e)
  }

  private def runSafe[E <: PersistentEvent](handle: (E => Unit), event: E) = {
    Try(handle(event)) match {
      case Success(_) =>
        // update last processed sequence number of event if event could get processed successfully
        setLastProcessedSequenceNr(event.meta)
      case Failure(e) =>
        log.error(s"Couldn't execute event:$e, error: {}", e)
        // forward exception which should get handled outside of this code
        throw e
    }
  }
}
