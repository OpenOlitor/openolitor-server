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

import akka.actor._
import akka.http.caching.scaladsl.Cache
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{ ExceptionHandler, RejectionHandler, Route }
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.util.Timeout
import ch.openolitor.arbeitseinsatz.{ ArbeitseinsatzRoutes, DefaultArbeitseinsatzRoutes }
import ch.openolitor.buchhaltung._
import ch.openolitor.core.db.evolution.DBEvolutionActor.CheckDBEvolution
import ch.openolitor.core.filestore._
import ch.openolitor.core.security._
import ch.openolitor.core.system._
import ch.openolitor.helloworld.HelloWorldRoutes
import ch.openolitor.kundenportal.{ DefaultKundenportalRoutes, KundenportalRoutes }
import ch.openolitor.mailtemplates.{ DefaultMailTemplateRoutes, MailTemplateRoutes }
import ch.openolitor.reports._
import ch.openolitor.stammdaten._
import ch.openolitor.stammdaten.models.{ AdministratorZugang, KundenZugang }

import scala.concurrent.duration._
import scala.util._

sealed trait ResponseType
case object Download extends ResponseType
case object Fetch extends ResponseType

trait RouteServiceComponent extends ActorReferences {
  val entityStore: ActorRef
  val eventStore: ActorRef
  val mailService: ActorRef
  val sysConfig: SystemConfig
  val system: ActorSystem
  val fileStore: FileStore

  val stammdatenRouteService: StammdatenRoutes
  val stammdatenRouteOpenService: StammdatenOpenRoutes
  val mailtemplateRouteService: MailTemplateRoutes
  val buchhaltungRouteService: BuchhaltungRoutes
  val reportsRouteService: ReportsRoutes
  val syncReportsRouteService: SyncReportsRoutes
  val kundenportalRouteService: KundenportalRoutes
  val arbeitseinsatzRouteService: ArbeitseinsatzRoutes
  val systemRouteService: SystemRouteService
  val loginRouteService: LoginRouteService
  val nonAuthRessourcesRouteService: NonAuthRessourcesRouteService
}

trait DefaultRouteServiceComponent extends RouteServiceComponent with TokenCache {
  override lazy val stammdatenRouteService = new DefaultStammdatenRoutes(dbEvolutionActor, entityStore, eventStore, mailService, reportSystem, sysConfig, system, airbrakeNotifier, jobQueueService)
  override lazy val stammdatenRouteOpenService = new DefaultStammdatenOpenRoutes(dbEvolutionActor, entityStore, eventStore, mailService, reportSystem, sysConfig, system, airbrakeNotifier, jobQueueService)
  override lazy val mailtemplateRouteService = new DefaultMailTemplateRoutes(dbEvolutionActor, entityStore, eventStore, mailService, reportSystem, sysConfig, system, airbrakeNotifier, jobQueueService)
  override lazy val buchhaltungRouteService = new DefaultBuchhaltungRoutes(dbEvolutionActor, entityStore, eventStore, mailService, reportSystem, sysConfig, system, airbrakeNotifier, jobQueueService)
  override lazy val reportsRouteService = new DefaultReportsRoutes(dbEvolutionActor, entityStore, eventStore, mailService, reportSystem, sysConfig, system, airbrakeNotifier, jobQueueService)
  override lazy val syncReportsRouteService = new DefaultSyncReportsRoutes(dbEvolutionActor, entityStore, eventStore, mailService, reportSystem, sysConfig, system, airbrakeNotifier, jobQueueService)
  override lazy val kundenportalRouteService = new DefaultKundenportalRoutes(dbEvolutionActor, entityStore, eventStore, mailService, reportSystem, sysConfig, system, airbrakeNotifier, jobQueueService)
  override lazy val arbeitseinsatzRouteService = new DefaultArbeitseinsatzRoutes(dbEvolutionActor, entityStore, eventStore, mailService, reportSystem, sysConfig, system, airbrakeNotifier, jobQueueService)
  override lazy val systemRouteService = new DefaultSystemRouteService(dbEvolutionActor, entityStore, eventStore, mailService, reportSystem, sysConfig, system, airbrakeNotifier, jobQueueService)
  override lazy val loginRouteService = new DefaultLoginRouteService(dbEvolutionActor, entityStore, eventStore, mailService, reportSystem, sysConfig, system, airbrakeNotifier, jobQueueService, loginTokenCache)
  override lazy val nonAuthRessourcesRouteService = new DefaultNonAuthRessourcesRouteService(sysConfig, system, airbrakeNotifier, jobQueueService)
}

// we don't implement our route structure directly in the service actor because(entityStore, sysConfig, system)
// we want to be able to test it independently, without having to spin up an actor
trait RouteService
  extends BaseRouteService
  with ActorReferences
  with HelloWorldRoutes
  with StatusRoutes
  with FileStoreRoutes
  with FileStoreComponent
  with CORSSupport
  with BaseJsonProtocol
  with RoleBasedAuthorization
  with AirbrakeNotifierReference
  with XSRFTokenSessionAuthenticatorProvider
  with BasicAuthenticatorProvider {
  self: RouteServiceComponent =>

  //initially run db evolution
  def initialize() = {
    runDBEvolution()
  }

  implicit val openolitorRejectionHandler: RejectionHandler = OpenOlitorRejectionHandler(this)

  implicit def exceptionHandler: ExceptionHandler = OpenOlitorExceptionHandler(this)

  val routes: Route = cors {
    // unsecured routes
    helloWorldRoute ~
      systemRouteService.statusRoute ~
      nonAuthRessourcesRouteService.ressourcesRoutes ~
      loginRouteService.loginRoute ~
      stammdatenRouteOpenService.stammdatenOpenRoute ~
      extractRequestContext { requestContext => // secured routes by XSRF token authenticator
        authenticate(requestContext) { implicit subject =>
          systemRouteService.jobQueueRoute ~
            loginRouteService.logoutRoute ~
            authorize(hasRole(AdministratorZugang)) {
              stammdatenRouteService.stammdatenRoute ~
                mailtemplateRouteService.mailRoute ~
                buchhaltungRouteService.buchhaltungRoute ~
                arbeitseinsatzRouteService.arbeitseinsatzRoute ~
                reportsRouteService.reportsRoute ~
                syncReportsRouteService.syncReportsRoute ~
                fileStoreRoute
            } ~
            authorize(hasRole(KundenZugang) || hasRole(AdministratorZugang)) {
              kundenportalRouteService.kundenportalRoute
            }
        }
      } ~
      authenticateBasicAsync("OpenOlitor", loginRouteService.basicAuthValidation) { implicit subject => // routes secured by basicauth mainly used for service accounts
        authorize(hasRole(AdministratorZugang)) {
          systemRouteService.adminRoutes
        }
      } ~
      extractRequestContext { requestContext =>
        authenticate(requestContext) { implicit subject =>
          authorize(hasRole(AdministratorZugang)) {
            systemRouteService.adminRoutes
          }
        }
      }
  }

  val dbEvolutionRoutes =
    pathPrefix("db") {
      dbEvolutionRoute()
    }

  def dbEvolutionRoute(): Route =
    path("recover") {
      post {
        onSuccess(runDBEvolution()) { x => x }
      }
    }

  def runDBEvolution() = {
    logger.debug(s"runDBEvolution:$entityStore")
    implicit val timeout = Timeout(50.seconds)
    (dbEvolutionActor ? CheckDBEvolution map {
      case Success(rev) =>
        logger.debug(s"Successfully check db with revision:$rev")
        complete("")
      case Failure(e) =>
        logger.warn(s"db evolution failed", e)
        systemRouteService.handleError(e)
        complete(StatusCodes.BadRequest, e)
    })(executionContext)
  }
}

final class DefaultRouteService(
  override val dbEvolutionActor: ActorRef,
  override val entityStore: ActorRef,
  override val eventStore: ActorRef,
  override val mailService: ActorRef,
  override val reportSystem: ActorRef,
  override val fileStore: FileStore,
  override val airbrakeNotifier: ActorRef,
  override val jobQueueService: ActorRef,
  override val sysConfig: SystemConfig,
  override val system: ActorSystem,
  override val loginTokenCache: Cache[String, Subject]
) extends RouteService
  with DefaultRouteServiceComponent {
  override implicit protected val executionContext = system.dispatcher
  override lazy val maxRequestDelay: Option[Duration] = loginRouteService.maxRequestDelay
}
