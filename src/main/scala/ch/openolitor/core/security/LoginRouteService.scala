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
package ch.openolitor.core.security

import akka.actor.{ ActorRef, ActorRefFactory, ActorSystem }
import ch.openolitor.core.Macros._
import ch.openolitor.core._
import ch.openolitor.core.filestore.FileStore
import ch.openolitor.stammdaten.models.{ Person, PersonDetail }
import ch.openolitor.stammdaten.repositories.{ DefaultStammdatenReadRepositoryAsyncComponent, StammdatenReadRepositoryAsyncComponent }
import com.typesafe.scalalogging.LazyLogging
import scalaz._
import spray.caching._
import spray.http._
import spray.httpx.SprayJsonSupport._
import spray.httpx.marshalling.ToResponseMarshallable._
import spray.routing.Directive._
import spray.routing._

import scala.concurrent.ExecutionContext.Implicits.global

trait LoginRouteService extends HttpService
  with SprayDeserializers
  with DefaultRouteService
  with LazyLogging
  with LoginJsonProtocol
  with LoginService {
  self: StammdatenReadRepositoryAsyncComponent =>

  lazy val loginRoutes = loginRoute

  def logoutRoute(implicit subject: Subject) = pathPrefix("auth") {
    path("logout") {
      post {
        onSuccess(doLogout) {
          case _ => complete("Logged out")
        }
      }
    } ~
      path("passwd") {
        post {
          requestInstance { request =>
            entity(as[ChangePasswordForm]) { form =>
              logger.debug(s"requested password change")
              onSuccess(validatePasswordChange(form).run) {
                case -\/(error) =>
                  logger.debug(s"Password change failed ${error.msg}")
                  complete(StatusCodes.BadRequest, error.msg)
                case \/-(result) =>
                  complete(result)
              }
            }
          }
        }
      }
  }

  def loginRoute = pathPrefix("auth") {
    path("login") {
      post {
        requestInstance { request =>
          entity(as[LoginForm]) { form =>
            onSuccess(validateLogin(form).run) {
              case -\/(error) =>
                logger.debug(s"Login failed ${error.msg}")
                complete(StatusCodes.BadRequest, error.msg)
              case \/-(result) =>
                complete(result)
            }
          }
        }
      }
    } ~
      path("secondFactor") {
        post {
          requestInstance { _ =>
            entity(as[SecondFactorAuthentication]) { form =>
              onSuccess(validateSecondFactorLogin(form).run) {
                case -\/(error) =>
                  logger.debug(s"Login failed ${error.msg}")
                  complete(StatusCodes.BadRequest, error.msg)
                case \/-(result) =>
                  complete(result)
              }
            }
          }
        }
      } ~
      pathPrefix("user") {
        authenticate(openOlitorAuthenticator) { implicit subject =>
          pathEnd {
            get {
              onSuccess(personById(subject.personId).run) {
                case -\/(error) =>
                  logger.debug(s"get user failed ${error.msg}")
                  complete(StatusCodes.Unauthorized)
                case \/-(person) =>
                  val personDetail = copyTo[Person, PersonDetail](person)
                  complete(User(personDetail, subject))
              }
            }
          } ~
            path("settings") {
              get {
                onSuccess(getLoginSettings(subject.personId).run) {
                  case -\/(error) =>
                    logger.debug(s"get users login settings failed ${error.msg}")
                    complete(StatusCodes.Unauthorized)
                  case \/-(settings) =>
                    complete(settings)
                }
              } ~
                post {
                  entity(as[LoginSettingsForm]) { settings =>
                    onSuccess(validateLoginSettings(subject.personId, settings).run) {
                      case -\/(error) =>
                        logger.debug(s"update users login settings failed ${error.msg}")
                        complete(StatusCodes.BadRequest)
                      case \/-(result) =>
                        complete(result)
                    }
                  }
                }
            }
        }
      } ~
      path("zugangaktivieren") {
        post {
          requestInstance { _ =>
            entity(as[SetPasswordForm]) { form =>
              onSuccess(validateSetPasswordForm(form).run) {
                case -\/(error) =>
                  complete(StatusCodes.BadRequest, error.msg)
                case \/-(_) =>
                  complete("Ok")
              }
            }
          }
        }
      } ~
      path("passwordreset") {
        post {
          requestInstance { _ =>
            entity(as[PasswordResetForm]) { form =>
              onSuccess(validatePasswordResetForm(form).run) {
                case -\/(error) =>
                  complete(StatusCodes.BadRequest, error.msg)
                case \/-(_) =>
                  complete("Ok")
              }
            }
          }
        }
      } ~
      pathPrefix("otp") {
        authenticate(openOlitorAuthenticator) { implicit subject =>
          path("requestSecret") {
            post {
              requestInstance { _ =>
                entity(as[OtpSecretResetRequest]) { form =>
                  onSuccess(validateOtpResetRequest(form).run) {
                    case -\/(error) =>
                      complete(StatusCodes.BadRequest, error.msg)
                    case \/-(result) =>
                      complete(result)
                  }
                }
              }
            }
          } ~
            path("changeSecret") {
              post {
                requestInstance { _ =>
                  entity(as[OtpSecretResetConfirm]) { form =>
                    onSuccess(validateOtpResetConfirm(form).run) {
                      case -\/(error) =>
                        complete(StatusCodes.BadRequest, error.msg)
                      case \/-(_) =>
                        complete("Ok")
                    }
                  }
                }
              }
            }
        }
      }
  }
}

class DefaultLoginRouteService(
  override val dbEvolutionActor: ActorRef,
  override val entityStore: ActorRef,
  override val eventStore: ActorRef,
  override val mailService: ActorRef,
  override val reportSystem: ActorRef,
  override val sysConfig: SystemConfig,
  override val system: ActorSystem,
  override val fileStore: FileStore,
  override val actorRefFactory: ActorRefFactory,
  override val airbrakeNotifier: ActorRef,
  override val jobQueueService: ActorRef,
  override val loginTokenCache: Cache[Subject]
)
  extends LoginRouteService
  with DefaultStammdatenReadRepositoryAsyncComponent
