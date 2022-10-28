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

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.util.Timeout
import ch.openolitor.core.batch.BatchJobs.InitializeBatchJob
import ch.openolitor.core.db._
import ch.openolitor.core.domain.SystemEvents.SystemStarted
import ch.openolitor.core.filestore.S3FileStore
import ch.openolitor.core.ws.DefaultClientMessagesRouteService
import ch.openolitor.util.ConfigUtil._
import com.tegonal.CFEnvConfigLoader.ConfigLoader
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import org.joda.time.DateTime
import scalaz._
import scalaz.Scalaz._
import scalikejdbc.ConnectionPoolContext

import java.net.ServerSocket
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
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

object Boot extends App with LazyLogging with StartingServices {
  case class MandantSystem(config: MandantConfiguration, system: ActorSystem, fileStore: S3FileStore)

  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  private var proxyActorSystem: Option[ActorSystem] = None
  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  private var proxyServerBinding: Option[Http.ServerBinding] = None
  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  private var mandantServerBindings: Seq[Http.ServerBinding] = Seq.empty

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

  try {
    Await.ready(Future.never, Duration.Inf)
  } catch {
    case _: Throwable => shutdown()
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
    proxyActorSystem = Some(proxySystem)
    implicit val executionContext = proxySystem.dispatcher

    Http().newServerAt(rootInterface, rootPort).bindFlow(proxy.Proxy(mandanten).withWsRoutes).transform[Unit](
      { binding: Http.ServerBinding =>
        logger.debug(s"oo-proxy-system: configured proxy listener on port ${rootPort}")
        proxyServerBinding = Some(binding)
      },
      { t: Throwable =>
        new IllegalStateException("Could not start the proxy server", t)
      }
    )
  }

  /**
   * Jeder Mandant wird in einem eigenen Akka System gestartet.
   */
  def startServices(configs: NonEmptyList[MandantConfiguration]): NonEmptyList[MandantSystem] = {
    configs.map { cfg =>
      val services = startServicesForConfiguration(config, cfg)
      implicit val app = services.app

      // create and start our service actor

      val routeService: RouteService = new DefaultRouteService(services.dbEvolutionActor, services.entityStore, services.eventStore, services.mailService, services.reportSystem, services.fileStore, services.airbrakeNotifier, services.jobQueueService, services.sysCfg, app, services.loginTokenCache)
      Await.result(routeService.initialize(), 1 minute)

      val clientMessagesRouteService = new DefaultClientMessagesRouteService(services.entityStore, services.sysCfg, app, services.loginTokenCache, services.streamsByUser)

      implicit val executionContext = app.dispatcher

      Http().newServerAt(cfg.interface, port = cfg.port).bindFlow(routeService.routes).transform[Unit](
        { binding: Http.ServerBinding =>
          logger.debug(s"oo-system: configured listener on port ${cfg.port}")
          mandantServerBindings :+= binding
        },
        { t: Throwable =>
          new IllegalStateException("Could not start mandant server", t)
        }
      )

      //start new websocket service
      Http().newServerAt(cfg.interface, port = cfg.wsPort).bindFlow(clientMessagesRouteService.routes).transform[Unit](
        { binding: Http.ServerBinding =>
          logger.debug(s"oo-system: configured ws listener on port ${cfg.wsPort}")
          mandantServerBindings :+= binding
        },
        { t: Throwable =>
          new IllegalStateException("Could not start mandant ws server", t)
        }
      )

      services.batchJobs ! InitializeBatchJob

      // persist timestamp of system startup
      services.eventStore ! SystemStarted(DateTime.now)

      MandantSystem(cfg, app, services.fileStore)
    }
  }

  def dbSeeds(config: Config): scala.collection.Map[Class[_ <: ch.openolitor.core.models.BaseId], Long] = {
    val models = config.getStringList("db.default.seed.models")
    val mappings: Seq[(Class[_], Long)] = models.asScala.map { model =>
      Class.forName(model) -> config.getLong(s"db.default.seed.mappings.$model")
    }.toSeq
    mappings.toMap.asInstanceOf[scala.collection.Map[Class[_ <: ch.openolitor.core.models.BaseId], Long]]
  }

  private def shutdown(): Unit = {
    implicit val executionContext = scala.concurrent.ExecutionContext.global

    logger.info("Shutting down OpenOlitor server...")

    val shutdown = for {
      _ <- proxyServerBinding.fold(Future.unit)(_.unbind().map(_ => logger.info("Shutdown of proxy server binding completed.")))
      _ <- proxyActorSystem.fold(Future.unit)(_.terminate().map(_ => logger.info("Shutdown of proxy actor system completed.")))
      _ <- Future.sequence(mandantServerBindings.map(_.unbind())).map(_ => logger.info("Shutdown of mandant server bindings completed."))
      _ <- Future.sequence(mandanten.toList.map(_.system.terminate())).map(_ => logger.info("Shutdown of mandant actor systems completed."))
      _ <- Future { mandanten.foreach(_.fileStore.client.shutdown()) }.map(_ => logger.info("Shutdown of file store clients completed."))
    } yield logger.info("Shutdown of OpenOlitor server completed.")

    Await.ready(shutdown, 20 seconds)
  }
}
