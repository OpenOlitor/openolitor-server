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
package ch.openolitor.core

import akka.actor.{ ActorRef, ActorSystem }
import akka.http.caching.scaladsl.{ Cache, CachingSettings }
import akka.http.caching.LfuCache
import akka.pattern.ask
import akka.stream.scaladsl.SourceQueueWithComplete
import akka.util.Timeout
import ch.openolitor.arbeitseinsatz.ArbeitseinsatzEntityStoreView
import ch.openolitor.buchhaltung.{ BuchhaltungDBEventEntityListener, BuchhaltungEntityStoreView, BuchhaltungReportEventListener }
import ch.openolitor.core.batch.OpenOlitorBatchJobs
import ch.openolitor.core.db.{ AsyncMandantDBs, MandantDBs }
import ch.openolitor.core.db.evolution.{ DBEvolutionActor, Evolution }
import ch.openolitor.core.db.evolution.scripts.Scripts
import ch.openolitor.core.domain.{ DefaultMessages, EntityStore, SystemEventStore }
import ch.openolitor.core.filestore.S3FileStore
import ch.openolitor.core.jobs.JobQueueService
import ch.openolitor.core.mailservice.MailService
import ch.openolitor.core.models.PersonId
import ch.openolitor.core.reporting.ReportSystem
import ch.openolitor.core.security.Subject
import ch.openolitor.core.ws.ClientMessagesActor
import ch.openolitor.mailtemplates.MailTemplateEntityStoreView
import ch.openolitor.reports.{ ReportsDBEventEntityListener, ReportsEntityStoreView }
import ch.openolitor.stammdaten.{ StammdatenDBEventEntityListener, StammdatenEntityStoreView, StammdatenGeneratedEventsListener, StammdatenMailListener }
import ch.openolitor.util.AirbrakeNotifier
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging

import scala.collection.concurrent.TrieMap
import scala.concurrent.duration._
import scala.concurrent.Await

case class StartedServices(dbEvolutionActor: ActorRef, entityStore: ActorRef, eventStore: ActorRef, mailService: ActorRef, reportSystem: ActorRef, batchJobs: ActorRef, fileStore: S3FileStore, airbrakeNotifier: ActorRef, jobQueueService: ActorRef, sysCfg: SystemConfig, app: ActorSystem, loginTokenCache: Cache[String, Subject], streamsByUser: TrieMap[PersonId, scala.collection.concurrent.Map[String, SourceQueueWithComplete[String]]])

trait StartingServices extends LazyLogging {
  def startServicesForConfiguration(baseConfig: Config, cfg: MandantConfiguration): StartedServices = {
    implicit val timeout = Timeout(5 seconds)
    implicit val app = ActorSystem(cfg.name, baseConfig.getConfig(cfg.configKey).withFallback(baseConfig))

    implicit val sysCfg = systemConfig(cfg)

    // declare token cache used in multiple locations in app
    val defaultCachingSettings = CachingSettings(app)
    val lfuCachingSettings = defaultCachingSettings.lfuCacheSettings
      .withMaxCapacity(10000)
      .withTimeToLive(1 day)
      .withTimeToIdle(4 hours)
    val cachingSettings = defaultCachingSettings.withLfuCacheSettings(lfuCachingSettings)
    val loginTokenCache: Cache[String, Subject] = LfuCache(cachingSettings)

    // initialise root actors
    val duration = Duration.create(1, SECONDS)
    val airbrakeNotifier = startAirbrakeService

    val fileStore = S3FileStore(sysCfg.mandantConfiguration, app)

    val evolution = new Evolution(sysCfg, Scripts.current(app))
    val system = app.actorOf(SystemActor.props(airbrakeNotifier), "oo-system")
    logger.debug(s"oo-system:$system")
    val dbEvolutionActor = Await.result(system ? SystemActor.Child(DBEvolutionActor.props(evolution), "db-evolution"), duration).asInstanceOf[ActorRef]
    logger.debug(s"oo-system:$system -> dbEvolutionActor:$dbEvolutionActor")
    val mailService = Await.result(system ? SystemActor.Child(MailService.props(dbEvolutionActor, fileStore), "mail-service"), duration).asInstanceOf[ActorRef]
    logger.debug(s"oo-system:$system -> eventStore:$mailService")

    val entityStore = Await.result(system ? SystemActor.Child(EntityStore.props(dbEvolutionActor, evolution, mailService), "entity-store"), duration).asInstanceOf[ActorRef]
    logger.debug(s"oo-system:$system -> entityStore:$entityStore")
    val eventStore = Await.result(system ? SystemActor.Child(SystemEventStore.props(dbEvolutionActor), "event-store"), duration).asInstanceOf[ActorRef]
    logger.debug(s"oo-system:$system -> eventStore:$eventStore")

    // STAMMDATEN
    val stammdatenEntityStoreView = Await.result(system ? SystemActor.Child(StammdatenEntityStoreView.props(mailService, dbEvolutionActor, airbrakeNotifier), "stammdaten-entity-store-view"), duration).asInstanceOf[ActorRef]
    val reportSystem = Await.result(system ? SystemActor.Child(ReportSystem.props(fileStore, sysCfg), "report-system"), duration).asInstanceOf[ActorRef]
    val jobQueueService = Await.result(system ? SystemActor.Child(JobQueueService.props(cfg), "job-queue"), duration).asInstanceOf[ActorRef]

    // start listeners for stammdaten
    Await.result(system ? SystemActor.Child(StammdatenDBEventEntityListener.props, "stammdaten-dbevent-entity-listener"), duration).asInstanceOf[ActorRef]
    Await.result(system ? SystemActor.Child(StammdatenMailListener.props, "stammdaten-mail-listener"), duration).asInstanceOf[ActorRef]
    Await.result(system ? SystemActor.Child(StammdatenGeneratedEventsListener.props, "stammdaten-generated-events-listener"), duration).asInstanceOf[ActorRef]

    // BUCHHALTUNG
    val buchhaltungEntityStoreView = Await.result(system ? SystemActor.Child(BuchhaltungEntityStoreView.props(dbEvolutionActor, airbrakeNotifier), "buchhaltung-entity-store-view"), duration).asInstanceOf[ActorRef]

    // start listeners for buchhaltung
    Await.result(system ? SystemActor.Child(BuchhaltungDBEventEntityListener.props, "buchhaltung-dbevent-entity-listener"), duration).asInstanceOf[ActorRef]
    Await.result(system ? SystemActor.Child(BuchhaltungReportEventListener.props(entityStore), "buchhaltung-report-event-listener"), duration).asInstanceOf[ActorRef]

    // ARBEITSEINSATZ
    val arbeitseinsatzEntityStoreView = Await.result(system ? SystemActor.Child(ArbeitseinsatzEntityStoreView.props(dbEvolutionActor, airbrakeNotifier), "arbeitseinsatz-entity-store-view"), duration).asInstanceOf[ActorRef]

    // MAILTEMPLATE
    val mailTemplateEntityStoreView = Await.result(system ? SystemActor.Child(MailTemplateEntityStoreView.props(dbEvolutionActor, airbrakeNotifier), "mailtemplate-entity-store-view"), duration).asInstanceOf[ActorRef]

    // REPORT
    val reportsEntityStoreView = Await.result(system ? SystemActor.Child(ReportsEntityStoreView.props(dbEvolutionActor, airbrakeNotifier), "reports-entity-store-view"), duration).asInstanceOf[ActorRef]

    // start listeners for reports
    Await.result(system ? SystemActor.Child(ReportsDBEventEntityListener.props, "reports-dbevent-entity-listener"), duration).asInstanceOf[ActorRef]

    // start websocket service
    // create map of users to streams used by the actor and the service
    val streamsByUser: TrieMap[PersonId, scala.collection.concurrent.Map[String, SourceQueueWithComplete[String]]] = TrieMap[PersonId, scala.collection.concurrent.Map[String, SourceQueueWithComplete[String]]]()
    Await.result(system ? SystemActor.Child(ClientMessagesActor.props(streamsByUser), "ws-client-messages"), duration).asInstanceOf[ActorRef]

    // start actor mapping dbevents to client messages
    Await.result(system ? SystemActor.Child(DBEvent2UserMapping.props(), "db-event-mapper"), duration).asInstanceOf[ActorRef]

    val batchJobs = Await.result(system ? SystemActor.Child(OpenOlitorBatchJobs.props(entityStore, fileStore), "batch-jobs"), duration).asInstanceOf[ActorRef]

    // initialize global persistentviews
    logger.debug(s"oo-system: send Startup to entityStoreview")
    eventStore ? DefaultMessages.Startup
    stammdatenEntityStoreView ? DefaultMessages.Startup
    buchhaltungEntityStoreView ? DefaultMessages.Startup
    arbeitseinsatzEntityStoreView ? DefaultMessages.Startup
    reportsEntityStoreView ? DefaultMessages.Startup
    mailTemplateEntityStoreView ? DefaultMessages.Startup

    StartedServices(dbEvolutionActor, entityStore, eventStore, mailService, reportSystem, batchJobs, fileStore, airbrakeNotifier, jobQueueService, sysCfg, app, loginTokenCache, streamsByUser)
  }

  def systemConfig(mandant: MandantConfiguration) = SystemConfig(mandant, connectionPoolContext(mandant), asyncConnectionPoolContext(mandant))

  def connectionPoolContext(mandantConfig: MandantConfiguration) = MandantDBs(mandantConfig).connectionPoolContext()

  def asyncConnectionPoolContext(mandantConfig: MandantConfiguration) = AsyncMandantDBs(mandantConfig).connectionPoolContext()

  def startAirbrakeService(implicit systemConfig: SystemConfig) = {
    implicit val airbrakeNotifierSystem = ActorSystem(s"oo-airbrake-notifier-system-${systemConfig.mandantConfiguration.name}")
    val airbrakeNotifier = airbrakeNotifierSystem.actorOf(AirbrakeNotifier.props, "oo-airbrake-notifier")
    airbrakeNotifier
  }
}
