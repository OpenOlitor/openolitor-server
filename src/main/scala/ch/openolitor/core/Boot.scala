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
import akka.http.scaladsl.Http
import akka.pattern.ask
import akka.stream.scaladsl.SourceQueueWithComplete
import akka.util.Timeout
import ch.openolitor.arbeitseinsatz.ArbeitseinsatzEntityStoreView
import ch.openolitor.buchhaltung.{ BuchhaltungDBEventEntityListener, BuchhaltungEntityStoreView, BuchhaltungReportEventListener }
import ch.openolitor.core.batch.BatchJobs.InitializeBatchJob
import ch.openolitor.core.batch.OpenOlitorBatchJobs
import ch.openolitor.core.db._
import ch.openolitor.core.db.evolution.{ DBEvolutionActor, Evolution }
import ch.openolitor.core.db.evolution.scripts.Scripts
import ch.openolitor.core.domain._
import ch.openolitor.core.domain.SystemEvents.SystemStarted
import ch.openolitor.core.filestore.{ DefaultFileStoreComponent, FileStoreComponent, S3FileStore }
import ch.openolitor.core.jobs.JobQueueService
import ch.openolitor.core.mailservice.MailService
import ch.openolitor.core.models.PersonId
import ch.openolitor.core.reporting._
import ch.openolitor.core.security.Subject
import ch.openolitor.core.ws.{ ClientMessagesActor, DefaultClientMessagesRouteService }
import ch.openolitor.mailtemplates.MailTemplateEntityStoreView
import ch.openolitor.reports.{ ReportsDBEventEntityListener, ReportsEntityStoreView }
import ch.openolitor.stammdaten._
import ch.openolitor.util.AirbrakeNotifier
import ch.openolitor.util.ConfigUtil._
import com.tegonal.CFEnvConfigLoader.ConfigLoader
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import org.joda.time.DateTime
import scalaz._
import scalaz.Scalaz._
import scalikejdbc.ConnectionPoolContext

import java.net.ServerSocket
import scala.collection.concurrent.TrieMap
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.StdIn
import scala.jdk.CollectionConverters._
import scala.util.Using

case class SystemConfig(mandantConfiguration: MandantConfiguration, cpContext: ConnectionPoolContext, asyncCpContext: MultipleAsyncConnectionPoolContext)

trait SystemConfigReference {
  val sysConfig: SystemConfig

  lazy val config = sysConfig.mandantConfiguration.config
}

case class MandantConfiguration(key: String, name: String, interface: String, port: Integer, wsPort: Integer, dbSeeds: scala.collection.Map[Class[_ <: ch.openolitor.core.models.BaseId], Long], config: Config) {
  val configKey = s"openolitor.${key}"

  def wsUri = s"ws://$interface:$wsPort"
  def uri = s"http://$interface:$port"
}

object Boot extends App with LazyLogging {
  case class MandantSystem(config: MandantConfiguration, system: ActorSystem)

  def freePort: Int = synchronized {
    Using(new ServerSocket(0)) { socket =>
      socket.setReuseAddress(true)
      socket.getLocalPort()
    }.getOrElse(sys.error(s"Couldn't aquire new free server port"))
  }

  logger.debug(s"application_name: " + sys.env.get("application_config"))
  logger.debug(s"config-file java prop: " + sys.props.get("config-file"))
  logger.debug(s"port: " + sys.env.get("PORT"))

  // This config represents the whole configuration and therefore includes the http configuration
  val config = ConfigLoader.loadConfig

  val systemPersonId = PersonId(0)

  // instanciate actor system per mandant, with mandantenspecific configuration
  // This config is a subset of config (i.e. without http configuration)
  val ooConfig = config.getConfig("openolitor")
  val configs = getMandantConfiguration(ooConfig)
  implicit val timeout = Timeout(5.seconds)

  lazy val mandanten = startServices(configs)

  val nonConfigPort = Option(System.getenv("PORT")).getOrElse("8080")

  lazy val rootPort = config.getStringOption("openolitor.port").getOrElse(nonConfigPort).toInt

  logger.debug(s"rootPort: " + rootPort)

  lazy val rootInterface = config.getStringOption("openolitor.interface").getOrElse("0.0.0.0")
  val proxyService = config.getBooleanOption("openolitor.run-proxy-service").getOrElse(false)

  //start proxy service
  if (proxyService) {
    startProxyService(mandanten, config)
  }

  def getMandantConfiguration(ooConfig: Config): NonEmptyList[MandantConfiguration] = {
    val mandanten = ooConfig.getStringList("mandanten").asScala.toList

    mandanten.toNel.map(_.zipWithIndex.map {
      case (mandant, index) =>
        val mandantConfig = ooConfig.getConfig(mandant).withFallback(ooConfig)

        val ifc = mandantConfig.getStringOption(s"interface").getOrElse(rootInterface)
        val port = ooConfig.getIntOption(s"$mandant.port").getOrElse(freePort)
        val wsPort = ooConfig.getIntOption(s"$mandant.webservicePort").getOrElse(freePort)
        val name = ooConfig.getStringOption(s"$mandant.name").getOrElse(mandant)

        MandantConfiguration(mandant, name, ifc, port, wsPort, dbSeeds(mandantConfig), mandantConfig)
    }).getOrElse {
      //default if no list of mandanten is configured
      val ifc = rootInterface
      val port = rootPort
      val wsPort = ooConfig.getIntOption("webservicePort").getOrElse(9001)

      NonEmptyList(MandantConfiguration("m1", "openolitor", ifc, port, wsPort, dbSeeds(ooConfig), ooConfig))
    }
  }

  def startProxyService(mandanten: NonEmptyList[MandantSystem], config: Config) = {
    implicit val proxySystem = ActorSystem("oo-proxy", config)
    implicit val executionContext = proxySystem.dispatcher

    Http().newServerAt(rootInterface, rootPort).bind(proxy.Proxy(mandanten).withWsRoutes)

    logger.debug(s"oo-proxy-system: configured proxy listener on port ${rootPort}")

    // TODO: spray-to-akka-http: bind to process
    StdIn.readLine()
  }

  def startAirbrakeService(implicit systemConfig: SystemConfig) = {
    implicit val airbrakeNotifierSystem = ActorSystem(s"oo-airbrake-notifier-system-${systemConfig.mandantConfiguration.name}")
    val airbrakeNotifier = airbrakeNotifierSystem.actorOf(AirbrakeNotifier.props, "oo-airbrake-notifier")
    airbrakeNotifier
  }

  /**
   * Jeder Mandant wird in einem eigenen Akka System gestartet.
   */
  def startServices(configs: NonEmptyList[MandantConfiguration]): NonEmptyList[MandantSystem] = {
    configs.map { cfg =>
      implicit val app = ActorSystem(cfg.name, config.getConfig(cfg.configKey).withFallback(config))

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
      val entityStore = Await.result(system ? SystemActor.Child(EntityStore.props(dbEvolutionActor, evolution), "entity-store"), duration).asInstanceOf[ActorRef]
      logger.debug(s"oo-system:$system -> entityStore:$entityStore")
      val eventStore = Await.result(system ? SystemActor.Child(SystemEventStore.props(dbEvolutionActor), "event-store"), duration).asInstanceOf[ActorRef]
      logger.debug(s"oo-system:$system -> eventStore:$eventStore")
      val mailService = Await.result(system ? SystemActor.Child(MailService.props(dbEvolutionActor, fileStore), "mail-service"), duration).asInstanceOf[ActorRef]
      logger.debug(s"oo-system:$system -> eventStore:$mailService")

      val stammdatenEntityStoreView = Await.result(system ? SystemActor.Child(StammdatenEntityStoreView.props(mailService, dbEvolutionActor, airbrakeNotifier), "stammdaten-entity-store-view"), duration).asInstanceOf[ActorRef]
      val reportSystem = Await.result(system ? SystemActor.Child(ReportSystem.props(fileStore, sysCfg), "report-system"), duration).asInstanceOf[ActorRef]
      val jobQueueService = Await.result(system ? SystemActor.Child(JobQueueService.props(cfg), "job-queue"), duration).asInstanceOf[ActorRef]

      //start actor listening events
      val stammdatenDBEventListener = Await.result(system ? SystemActor.Child(StammdatenDBEventEntityListener.props, "stammdaten-dbevent-entity-listener"), duration).asInstanceOf[ActorRef]
      val stammdatenMailSentListener = Await.result(system ? SystemActor.Child(StammdatenMailListener.props, "stammdaten-mail-listener"), duration).asInstanceOf[ActorRef]
      val stammdatenGeneratedEventsListener = Await.result(system ? SystemActor.Child(StammdatenGeneratedEventsListener.props, "stammdaten-generated-events-listener"), duration).asInstanceOf[ActorRef]

      val buchhaltungEntityStoreView = Await.result(system ? SystemActor.Child(BuchhaltungEntityStoreView.props(mailService, dbEvolutionActor, airbrakeNotifier), "buchhaltung-entity-store-view"), duration).asInstanceOf[ActorRef]
      val buchhaltungDBEventListener = Await.result(system ? SystemActor.Child(BuchhaltungDBEventEntityListener.props, "buchhaltung-dbevent-entity-listener"), duration).asInstanceOf[ActorRef]
      val buchhaltungReportEventListener = Await.result(system ? SystemActor.Child(BuchhaltungReportEventListener.props(entityStore), "buchhaltung-report-event-listener"), duration).asInstanceOf[ActorRef]

      val arbeitseinsatzEntityStoreView = Await.result(system ? SystemActor.Child(ArbeitseinsatzEntityStoreView.props(mailService, dbEvolutionActor, airbrakeNotifier), "arbeitseinsatz-entity-store-view"), duration).asInstanceOf[ActorRef]
      val mailTemplateEntityStoreView = Await.result(system ? SystemActor.Child(MailTemplateEntityStoreView.props(dbEvolutionActor, airbrakeNotifier), "mailtemplate-entity-store-view"), duration).asInstanceOf[ActorRef]
      val reportsEntityStoreView = Await.result(system ? SystemActor.Child(ReportsEntityStoreView.props(dbEvolutionActor, airbrakeNotifier), "reports-entity-store-view"), duration).asInstanceOf[ActorRef]
      val reportsDBEventListener = Await.result(system ? SystemActor.Child(ReportsDBEventEntityListener.props, "reports-dbevent-entity-listener"), duration).asInstanceOf[ActorRef]

      //start websocket service
      // create map of users to streams used by the actor and the service
      val streamsByUser: TrieMap[PersonId, scala.collection.concurrent.Map[String, SourceQueueWithComplete[String]]] = TrieMap[PersonId, scala.collection.concurrent.Map[String, SourceQueueWithComplete[String]]]()
      val clientMessagesActor = Await.result(system ? SystemActor.Child(ClientMessagesActor.props(streamsByUser), "ws-client-messages"), duration).asInstanceOf[ActorRef]

      //start actor mapping dbevents to client messages
      val dbEventClientMessageMapper = Await.result(system ? SystemActor.Child(DBEvent2UserMapping.props, "db-event-mapper"), duration).asInstanceOf[ActorRef]

      val batchJobs = Await.result(system ? SystemActor.Child(OpenOlitorBatchJobs.props(entityStore, fileStore), "batch-jobs"), duration).asInstanceOf[ActorRef]

      //initialize global persistentviews
      logger.debug(s"oo-system: send Startup to entityStoreview")
      eventStore ? DefaultMessages.Startup
      stammdatenEntityStoreView ? DefaultMessages.Startup
      buchhaltungEntityStoreView ? DefaultMessages.Startup
      arbeitseinsatzEntityStoreView ? DefaultMessages.Startup
      reportsEntityStoreView ? DefaultMessages.Startup

      // create and start our service actor

      val routeService: RouteService = new DefaultRouteService(dbEvolutionActor, entityStore, eventStore, mailService, reportSystem, fileStore, airbrakeNotifier, jobQueueService, sysCfg, app, loginTokenCache)
      Await.result(routeService.initialize(), 1 minute)

      val clientMessagesRouteService = new DefaultClientMessagesRouteService(entityStore, sysCfg, app, loginTokenCache, streamsByUser)

      Http().newServerAt(cfg.interface, port = cfg.port).bind(routeService.routes)
      logger.debug(s"oo-system: configured listener on port ${cfg.port}")

      //start new websocket service
      Http().newServerAt(cfg.interface, port = cfg.wsPort).bind(clientMessagesRouteService.routes)
      logger.debug(s"oo-system: configured ws listener on port ${cfg.wsPort}")

      batchJobs ! InitializeBatchJob

      // persist timestamp of system startup
      eventStore ! SystemStarted(DateTime.now)

      MandantSystem(cfg, app)
    }
  }

  def dbSeeds(config: Config): scala.collection.Map[Class[_ <: ch.openolitor.core.models.BaseId], Long] = {
    val models = config.getStringList("db.default.seed.models")
    val mappings: Seq[(Class[_], Long)] = models.asScala.map { model =>
      Class.forName(model) -> config.getLong(s"db.default.seed.mappings.$model")
    }.toSeq
    mappings.toMap.asInstanceOf[scala.collection.Map[Class[_ <: ch.openolitor.core.models.BaseId], Long]]
  }

  def systemConfig(mandant: MandantConfiguration) = SystemConfig(mandant, connectionPoolContext(mandant), asyncConnectionPoolContext(mandant))

  def connectionPoolContext(mandantConfig: MandantConfiguration) = MandantDBs(mandantConfig).connectionPoolContext()

  def asyncConnectionPoolContext(mandantConfig: MandantConfiguration) = AsyncMandantDBs(mandantConfig).connectionPoolContext()
}
