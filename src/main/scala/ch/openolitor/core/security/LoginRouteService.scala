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

import spray.routing._
import spray.http._
import spray.httpx.marshalling.ToResponseMarshallable._
import spray.httpx.SprayJsonSupport._
import spray.routing.Directive._
import spray.caching._
import ch.openolitor.core._
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.stammdaten.repositories.StammdatenReadRepositoryAsyncComponent
import ch.openolitor.stammdaten.repositories.DefaultStammdatenReadRepositoryAsyncComponent
import akka.actor.ActorRef
import ch.openolitor.core.filestore.FileStore
import akka.actor.ActorRefFactory
import ch.openolitor.stammdaten.models.{ Einladung, EinladungId, EmailSecondFactorType, OtpSecondFactorType, Person, PersonDetail, PersonSummary, Projekt, SecondFactorType }
import org.mindrot.jbcrypt.BCrypt

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import ch.openolitor.core.db.AsyncConnectionPoolContextAware
import ch.openolitor.core.models.PersonId
import java.util.UUID

import ch.openolitor.core.Macros._

import scala.util.Random
import scalaz._
import Scalaz._
import ch.openolitor.util.ConfigUtil._
import ch.openolitor.core.domain.{ SystemEvents }
import spray.routing.authentication.UserPass
import ch.openolitor.stammdaten.StammdatenCommandHandler._
import akka.pattern.ask
import akka.actor.ActorSystem
import ch.openolitor.core.mailservice.MailService._
import ch.openolitor.core.mailservice.Mail

import scala.concurrent.duration._
import org.joda.time.DateTime
import ch.openolitor.util.OtpUtil

trait LoginRouteService extends HttpService with ActorReferences
  with AsyncConnectionPoolContextAware
  with SprayDeserializers
  with DefaultRouteService with LazyLogging with LoginJsonProtocol
  with XSRFTokenSessionAuthenticatorProvider {
  self: StammdatenReadRepositoryAsyncComponent =>
  import SystemEvents._

  type EitherFuture[A] = EitherT[Future, RequestFailed, A]

  lazy val loginRoutes = loginRoute

  val secondFactorTokenCache = LruCache[SecondFactor](
    maxCapacity = 1000,
    timeToLive = 20 minutes,
    timeToIdle = 10 minutes
  )

  val secondFactorResetTokenCache = LruCache[String](
    maxCapacity = 1000,
    timeToLive = 20 minutes,
    timeToIdle = 10 minutes
  )

  lazy val config = sysConfig.mandantConfiguration.config
  lazy val requireSecondFactorAuthentication = config.getBooleanOption(s"security.second-factor-auth.require").getOrElse(true)
  override lazy val maxRequestDelay: Option[Duration] = config.getLongOption(s"security.max-request-delay").map(_ millis)

  //pasword validation options
  lazy val passwordMinLength = config.getIntOption("security.password-validation.min-length").getOrElse(6)
  lazy val passwordMustContainSpecialCharacter = config.getBooleanOption("security.password-validation.special-character-required").getOrElse(false)
  lazy val passwordSpecialCharacterList = config.getStringListOption("security.password-validation.special-characters").getOrElse(List("$", ".", ",", "?", "_", "-"))
  lazy val passwordMustContainLowerAndUpperCase = config.getBooleanOption("security.password-validation.lower-and-upper-case").getOrElse(true)
  lazy val passwordRegex = config.getStringOption("security.password-validation.regex").map(_.r).getOrElse("""(?=.*[a-z])(?=.*[A-Z])(?=.*[^a-zA-Z])""".r)
  lazy val passwordRegexFailMessage = config.getStringOption("security.password-validation.regex-error").getOrElse("Das Password entspricht nicht den konfigurierten Sicherheitsbestimmungen")

  val errorUsernameOrPasswordMismatch = RequestFailed("Benutzername oder Passwort stimmen nicht überein")
  val errorTokenOrCodeMismatch = RequestFailed("Code stimmt nicht überein")
  val errorPersonNotFound = RequestFailed("Person konnte nicht gefunden werden")
  val errorPersonLoginNotActive = RequestFailed("Login wurde deaktiviert")
  val errorConfigurationError = RequestFailed("Konfigurationsfehler. Bitte Administrator kontaktieren.")

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
                  complete("Ok")
              }
            }
          }
        }
      }
  }

  def validatePasswordChange(form: ChangePasswordForm)(implicit subject: Subject): EitherFuture[Boolean] = {
    for {
      person <- personById(subject.personId)
      _ <- validatePassword(form.alt, person)
      _ <- validateNewPassword(form.neu)
      result <- changePassword(person, form.neu, None)
    } yield result
  }

  private def containsOneOf(src: String, chars: List[String]): Boolean = {
    chars match {
      case Nil                                   => false
      case head :: tail if src.indexOf(head) > 0 => true
      case head :: tail                          => containsOneOf(src, tail)
    }
  }

  private def validateNewPassword(password: String): EitherFuture[Boolean] = EitherT {
    Future {
      password match {
        case p if p.length < passwordMinLength => RequestFailed(s"Das Password muss aus mindestens aus $passwordMinLength Zeichen bestehen").left
        case p if passwordMustContainSpecialCharacter && !containsOneOf(p, passwordSpecialCharacterList) => RequestFailed(s"Das Password muss aus mindestens ein Sonderzeichen beinhalten").left
        case p if passwordMustContainLowerAndUpperCase && p == p.toLowerCase => RequestFailed(s"Das Passwort muss mindestens einen Grossbuchstaben enthalten").left
        case p if passwordMustContainLowerAndUpperCase && p == p.toUpperCase => RequestFailed(s"Das Passwort muss mindestens einen Kleinbuchstaben enthalten").left
        case p if passwordRegex.findFirstMatchIn(p).isDefined => true.right
        case _ =>
          logger.debug(s"Password does not match regex:$passwordRegex")
          RequestFailed(passwordRegexFailMessage).left
      }
    }
  }

  private def changePassword(person: Person, newPassword: String, einladung: Option[EinladungId])(implicit subject: Subject): EitherFuture[Boolean] =
    changePassword(subject.personId, person.id, newPassword, einladung)

  private def changePassword(subjectPersonId: PersonId, targetPersonId: PersonId, newPassword: String, einladung: Option[EinladungId] = None): EitherFuture[Boolean] = EitherT {
    //hash password
    val hash = BCrypt.hashpw(newPassword, BCrypt.gensalt())

    (entityStore ? PasswortWechselCommand(subjectPersonId, targetPersonId, hash.toCharArray, einladung)) map {
      case _: PasswortGewechseltEvent => true.right
      case _                          => RequestFailed(s"Das Passwort konnte nicht gewechselt werden").left
    }
  }

  private def resetPassword(person: Person): EitherFuture[Boolean] = EitherT {
    (entityStore ? PasswortResetCommand(person.id, person.id)) map {
      case _: PasswortResetGesendetEvent => true.right
      case _                             => RequestFailed(s"Das Passwort konnte nicht gewechselt werden").left
    }
  }

  private def doLogout(implicit subject: Subject) = {
    logger.debug(s"Logout user:${subject.personId}, invalidate token:${subject.token}")
    //remove token from cache
    loginTokenCache.remove(subject.token).getOrElse(Future.successful(subject))
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
            entity(as[SecondFactorLoginForm]) { form =>
              onSuccess(validateSecondFactorLogin(form).run) {
                case -\/(error) =>
                  complete(StatusCodes.BadRequest, error.msg)
                case \/-(result) =>
                  complete(result)
              }
            }
          }
        }
      } ~
      path("user") {
        authenticate(openOlitorAuthenticator) { implicit subject =>
          onSuccess(personById(subject.personId).run) {
            case -\/(error) =>
              complete(StatusCodes.Unauthorized)
            case \/-(person) =>
              val personDetail = copyTo[Person, PersonDetail](person)
              complete(User(personDetail, subject))
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

  def validateOtpResetRequest(form: OtpSecretResetRequest)(implicit subject: Subject): EitherFuture[OtpSecretResetResponse] = {
    for {
      person <- personById(subject.personId)
      _ <- validateSecondFactor(person, form.code, OtpSecondFactor("", person.id))
      response <- generateOtpSecretResponse(person)
    } yield response
  }

  def validateOtpResetConfirm(form: OtpSecretResetConfirm)(implicit subject: Subject): EitherFuture[Boolean] = {
    for {
      person <- personById(subject.personId)
      secret <- validateOtpSecretFromCache(form)
    } yield {
      eventStore ! PersonChangedOtpSecret(person.id, secret)
      secondFactorResetTokenCache.remove(form.token)
      true
    }
  }

  private def generateOtpSecretResponse(person: Person): EitherFuture[OtpSecretResetResponse] = EitherT {
    val token = generateToken
    val secret = generateOtp
    val personSummary = copyTo[Person, PersonSummary](person)
    val response = OtpSecretResetResponse(token, personSummary, secret)
    secondFactorResetTokenCache(token)(secret) map (_ => response.right)
  }

  private def validateOtpSecretFromCache(form: OtpSecretResetConfirm): EitherFuture[String] = {
    EitherT {
      transform(secondFactorResetTokenCache.get(form.token)) map {
        case Some(secret) if OtpUtil.checkCodeWithSecret(form.code, secret) => ToEitherOps(secret).right
        case _ => errorTokenOrCodeMismatch.left
      }
    }
  }

  def validateLogin(form: LoginForm): EitherFuture[LoginResult] = {
    for {
      person <- personByEmail(form.email)
      _ <- validatePassword(form.passwort, person)
      _ <- validatePerson(person)
      projekt <- getProjekt()
      result <- handleLoggedIn(projekt, person)
    } yield result
  }

  private def getProjekt(): EitherFuture[Projekt] = {
    EitherT {
      stammdatenReadRepository.getProjekt map (_ map (_.right) getOrElse {
        logger.debug(s"Could not load project")
        errorConfigurationError.left
      })
    }
  }

  private def transform[A](o: Option[Future[A]]): Future[Option[A]] = {
    o.map(f => f.map(Option(_))).getOrElse(Future.successful(None))
  }

  def validateSecondFactorLogin(form: SecondFactorLoginForm): EitherFuture[LoginResult] = {
    for {
      secondFactor <- readTokenFromCache(form)
      person <- personById(secondFactor.personId)
      _ <- validateSecondFactor(person, form.code, secondFactor)
      _ <- validatePerson(person)
      result <- doLogin(person, Some(secondFactor.`type`))
    } yield {
      //cleanup code from cache
      secondFactorTokenCache.remove(form.token)

      result
    }
  }

  private def readTokenFromCache(form: SecondFactorLoginForm): EitherFuture[SecondFactor] = {
    EitherT {
      transform(secondFactorTokenCache.get(form.token)) map {
        case Some(factor @ EmailSecondFactor(form.token, _, _)) => factor.right
        case Some(factor @ OtpSecondFactor(form.token, _)) => factor.right
        case _ => errorTokenOrCodeMismatch.left
      }
    }
  }

  private def validateSecondFactor(person: Person, code: String, secondFactor: SecondFactor): EitherFuture[Boolean] =
    EitherT {
      Future {
        (secondFactor, person.otpSecret) match {
          case (OtpSecondFactor(_, person.id), secret) if OtpUtil.checkCodeWithSecret(code, secret) => true.right
          case (_: OtpSecondFactor, _) => errorTokenOrCodeMismatch.left
          case (EmailSecondFactor(_, code, _), _) => true.right
          case (_: EmailSecondFactor, _) => errorTokenOrCodeMismatch.left
        }
      }
    }

  private def personByEmail(email: String): EitherFuture[Person] = {
    EitherT {
      stammdatenReadRepository.getPersonByEmail(email) map (_ map (_.right) getOrElse {
        logger.debug(s"No person found for email")
        errorPersonNotFound.left
      })
    }
  }

  private def personById(personId: PersonId): EitherFuture[Person] = {
    EitherT {
      stammdatenReadRepository.getPerson(personId) map (_ map (_.right) getOrElse (errorPersonNotFound.left))
    }
  }

  private def handleLoggedIn(projekt: Projekt, person: Person): EitherFuture[LoginResult] = {
    requireSecondFactorAuthentifcation(projekt, person) flatMap {
      case false => doLogin(person, None)
      case true  => handleSecondFactorAuthentication(projekt, person)
    }
  }

  private def doLogin(person: Person, secondFactorType: Option[SecondFactorType]): EitherFuture[LoginResult] = {
    //generate token
    val token = generateToken
    EitherT {
      loginTokenCache(token)(Subject(token, person.id, person.kundeId, person.rolle, secondFactorType)) map { _ =>
        val personSummary = copyTo[Person, PersonSummary](person)

        eventStore ! PersonLoggedIn(person.id, org.joda.time.DateTime.now, secondFactorType)

        LoginResult(LoginOk, token, personSummary, None, secondFactorType).right
      }
    }
  }

  private def handleSecondFactorAuthentication(projekt: Projekt, person: Person): EitherFuture[LoginResult] = {
    for {
      secondFactor <- generateSecondFactor(projekt, person)
      _ <- maybeSendEmail(secondFactor, person)
    } yield {
      val personSummary = copyTo[Person, PersonSummary](person)
      val otpSecret = (secondFactor, person.otpReset) match {
        case (_: OtpSecondFactor, true) => Some(person.otpSecret)
        case _                          => None
      }
      LoginResult(LoginSecondFactorRequired, secondFactor.token, personSummary, otpSecret, Some(secondFactor.`type`))
    }
  }

  private def generateSecondFactor(projekt: Projekt, person: Person): EitherFuture[SecondFactor] = {
    generateSecondFactor(person, person.secondFactorType.getOrElse(projekt.defaultSecondFactorType))
  }

  private def generateSecondFactor(person: Person, secondFactorType: SecondFactorType): EitherFuture[SecondFactor] = {
    EitherT {
      val token = generateToken
      val secondFactor = secondFactorType match {
        case EmailSecondFactorType =>
          val code = generateCode
          EmailSecondFactor(token, code, person.id)
        case OtpSecondFactorType => OtpSecondFactor(token, person.id)
      }
      secondFactorTokenCache(token)(secondFactor) map (_.right)
    }
  }

  private def maybeSendEmail(secondFactor: SecondFactor, person: Person): EitherFuture[Boolean] =
    secondFactor match {
      case OtpSecondFactor(token, personId) => EitherT { Future { true.right } }
      case email: EmailSecondFactor         => sendEmail(email, person)
    }

  private def sendEmail(secondFactor: EmailSecondFactor, person: Person): EitherFuture[Boolean] = EitherT {
    // if an email can be sent has to be validated by the corresponding command handler
    val mail = Mail(1, person.email.get, None, None, None, "OpenOlitor Second Factor",
      s"""Code: ${secondFactor.code}""", None)
    mailService ? SendMailCommand(SystemEvents.SystemPersonId, mail, Some(5 minutes)) map {
      case _: SendMailEvent =>
        true.right
      case other =>
        logger.debug(s"Sending Mail failed resulting in $other")
        RequestFailed(s"Mail konnte nicht zugestellt werden").left
    }
  }

  private def requireSecondFactorAuthentifcation(projekt: Projekt, person: Person): EitherFuture[Boolean] = EitherT {
    Future {
      requireSecondFactorAuthentication match {
        case false                          => false.right
        case true if (person.rolle.isEmpty) => true.right
        case true                           => projekt.twoFactorAuthentication.get(person.rolle.get).map(_.right).getOrElse(true.right)
      }
    }
  }

  private def validatePassword(password: String, person: Person): EitherFuture[Boolean] = EitherT {
    Future {
      person.passwort map { pwd =>
        BCrypt.checkpw(password, new String(pwd)) match {
          case true => true.right
          case false =>
            logger.debug(s"Password mismatch")
            errorUsernameOrPasswordMismatch.left
        }
      } getOrElse {
        logger.debug(s"No password for user")
        errorUsernameOrPasswordMismatch.left
      }
    }
  }

  private def validatePerson(person: Person): EitherFuture[Boolean] = EitherT {
    Future {
      person.loginAktiv match {
        case true  => true.right
        case false => errorPersonLoginNotActive.left
      }
    }
  }

  private def validateSetPasswordForm(form: SetPasswordForm): EitherFuture[Boolean] = {
    for {
      einladung <- validateEinladung(form.token)
      person <- personById(einladung.personId)
      _ <- validateNewPassword(form.neu)
      result <- changePassword(person.id, person.id, form.neu, Some(einladung.id))
    } yield result
  }

  private def validatePasswordResetForm(form: PasswordResetForm): EitherFuture[Boolean] = {
    for {
      person <- personByEmail(form.email)
      _ <- validatePerson(person)
      result <- resetPassword(person)
    } yield result
  }

  private def validateEinladung(token: String): EitherFuture[Einladung] = {
    EitherT {
      stammdatenReadRepository.getEinladung(token) map (_ map { einladung =>
        if (einladung.expires.isAfter(DateTime.now)) {
          einladung.right
        } else {
          RequestFailed("Die Einladung mit diesem Token ist abgelaufen").left
        }
      } getOrElse {
        logger.debug(s"Token not found in Einladung")
        RequestFailed("Keine Einladung mit diesem Token gefunden").left
      })
    }
  }

  private def generateToken = UUID.randomUUID.toString
  private def generateCode = (Random.alphanumeric take 6).mkString.toLowerCase
  private def generateOtp = OtpUtil.generateOtpSecretString

  /**
   * Validate user password used by basic authentication. Using basic auth we never to a two factor
   * authentication
   */
  def basicAuthValidation(userPass: Option[UserPass]): Future[Option[Subject]] = {
    logger.debug(s"Perform basic authentication")
    (for {
      up <- validateUserPass(userPass)
      person <- personByEmail(up.user)
      _ <- validatePassword(up.pass, person)
      _ <- validatePerson(person)
      result <- doLogin(person, None)
    } yield Subject(result.token, person.id, person.kundeId, person.rolle, None)).run.map(_.toOption)
  }

  private def validateUserPass(userPass: Option[UserPass]): EitherFuture[UserPass] = EitherT {
    Future {
      userPass map (_.right) getOrElse (errorUsernameOrPasswordMismatch.left)
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
