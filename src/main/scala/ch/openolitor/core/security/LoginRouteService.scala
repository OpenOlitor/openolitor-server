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

import akka.actor.{ ActorRef, ActorSystem }
import akka.http.caching.scaladsl.Cache
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.MarshallingDirectives.{ as, entity }
import ch.openolitor.core._
import ch.openolitor.core.Macros._
import ch.openolitor.core.filestore.{ DefaultFileStoreComponent, FileStoreComponent }
import ch.openolitor.stammdaten.models.{ Person, PersonDetail }
import ch.openolitor.stammdaten.repositories.{ DefaultStammdatenReadRepositoryAsyncComponent, StammdatenReadRepositoryAsyncComponent }
import com.typesafe.scalalogging.LazyLogging
import scalaz._

trait LoginRouteService
  extends BaseRouteService
  with SprayDeserializers
  with LazyLogging
  with LoginJsonProtocol
  with LoginService {
  self: StammdatenReadRepositoryAsyncComponent with FileStoreComponent =>

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
          extractRequest { _ =>
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
        extractRequest { request =>
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
          extractRequest { _ =>
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
        extractRequestContext { requestContext =>
          authenticate(requestContext) { implicit subject =>
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
        }
      } ~
      path("zugangaktivieren") {
        post {
          extractRequest { _ =>
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
          extractRequest { _ =>
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
        extractRequestContext { requestContext =>
          authenticate(requestContext) { implicit subject =>
            path("requestSecret") {
              post {
                extractRequest { _ =>
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
                  extractRequest { _ =>
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
}

class DefaultLoginRouteService(
  override val dbEvolutionActor: ActorRef,
  override val entityStore: ActorRef,
  override val eventStore: ActorRef,
  override val mailService: ActorRef,
  override val reportSystem: ActorRef,
  override val sysConfig: SystemConfig,
  override val system: ActorSystem,
  override val airbrakeNotifier: ActorRef,
  override val jobQueueService: ActorRef,
  override val loginTokenCache: Cache[String, Subject]
)
  extends LoginRouteService
  with DefaultStammdatenReadRepositoryAsyncComponent
  with DefaultFileStoreComponent {
  override implicit protected val executionContext = system.dispatcher
}
