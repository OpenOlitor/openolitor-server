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

import akka.http.caching.scaladsl.{ Cache, CachingSettings }
import akka.http.caching.LfuCache
import akka.pattern.ask
import akka.util.Timeout
import ch.openolitor.core.{ ActorReferences, ExecutionContextAware, SystemConfigReference }
import ch.openolitor.core.Macros.copyTo
import ch.openolitor.core.db.AsyncConnectionPoolContextAware
import ch.openolitor.core.domain.SystemEvents
import ch.openolitor.core.mailservice.Mail
import ch.openolitor.core.mailservice.MailService.{ MailServiceState, SendMailCommand, SendMailEvent }
import ch.openolitor.core.models.PersonId
import ch.openolitor.stammdaten.StammdatenCommandHandler.{ PasswortGewechseltEvent, PasswortResetCommand, PasswortResetGesendetEvent, PasswortWechselCommand }
import ch.openolitor.stammdaten.models._
import ch.openolitor.stammdaten.repositories.StammdatenReadRepositoryAsyncComponent
import ch.openolitor.util.ConfigUtil._
import ch.openolitor.util.OtpUtil
import com.typesafe.scalalogging.LazyLogging
import org.joda.time.DateTime
import org.mindrot.jbcrypt.BCrypt
import scalaz.EitherT
import scalaz.Scalaz._

import java.util.UUID
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.Random

trait LoginService extends LazyLogging
  with AsyncConnectionPoolContextAware
  with XSRFTokenSessionAuthenticatorProvider
  with ActorReferences
  with SystemConfigReference
  with ExecutionContextAware {
  self: StammdatenReadRepositoryAsyncComponent =>

  type EitherFuture[A] = EitherT[RequestFailed, Future, A]

  private lazy val defaultCachingSettings = CachingSettings(system)
  private lazy val lfuCachingSettings = defaultCachingSettings.lfuCacheSettings
    .withMaxCapacity(1000)
    .withTimeToLive(20 minutes)
    .withTimeToIdle(10 minutes)
  private lazy val cachingSettings = defaultCachingSettings.withLfuCacheSettings(lfuCachingSettings)

  lazy val secondFactorTokenCache: Cache[String, SecondFactor] = LfuCache(cachingSettings)

  lazy val secondFactorResetTokenCache: Cache[String, String] = LfuCache(cachingSettings)

  implicit val actorAskTimeout = Timeout(5.seconds)

  import SystemEvents._

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

  /**
   * Route validation methods
   */
  protected def doLogout(implicit subject: Subject): Future[Subject] = {
    logger.debug(s"Logout user:${subject.personId}, invalidate token:${subject.token}")
    //remove token from cache
    loginTokenCache.get(subject.token).map { f =>
      loginTokenCache.remove(subject.token)
      f
    }.getOrElse(Future.successful(subject))
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

  def validateLogin(form: LoginForm): EitherFuture[LoginResult] = {
    for {
      person <- personByEmail(form.email)
      _ <- validatePassword(form.passwort, person)
      _ <- validatePerson(person)
      projekt <- getProjekt()
      result <- processOrRequestSecondFactor(projekt, person, None)(doLogin(person, None))(secondFactor => {
        val personSummary = copyTo[Person, PersonSummary](person)
        val otpSecret = (secondFactor, person.otpReset) match {
          case (_: OtpSecondFactor, true) => Some(person.otpSecret)
          case _                          => None
        }
        LoginResult(SecondFactorRequired, Some(secondFactor.token), personSummary, otpSecret, Some(secondFactor.`type`))
      })
    } yield result
  }

  def validateSecondFactorLogin(form: SecondFactorAuthentication): EitherFuture[LoginResult] = {
    for {
      secondFactor <- readTokenFromCache(form.token)
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

  def validateSetPasswordForm(form: SetPasswordForm): EitherFuture[FormResult] = {
    for {
      einladung <- validateEinladung(form.token)
      person <- personById(einladung.personId)
      _ <- validateNewPassword(form.neu)
      result <- changePassword(person.id, person.id, form.neu, Some(einladung.id))
    } yield result
  }

  def validatePasswordResetForm(form: PasswordResetForm): EitherFuture[Boolean] = {
    for {
      person <- personByEmail(form.email)
      _ <- validatePerson(person)
      result <- resetPassword(person)
    } yield result
  }

  def validatePasswordChange(form: ChangePasswordForm)(implicit subject: Subject): EitherFuture[FormResult] = {
    for {
      projekt <- getProjekt()
      person <- personById(subject.personId)
      _ <- validatePassword(form.alt, person)
      _ <- validateNewPassword(form.neu)
      result <- processOrRequestSecondFactor(projekt, person, form.secondFactorAuth)(changePassword(person, form.neu, None))(secondFactor => FormResult(SecondFactorRequired, Some(secondFactor.token), Some(secondFactor.`type`)))
    } yield result
  }

  /**
   * Validate user password used by basic authentication. Using basic auth we never to a two factor
   * authentication
   */
  def basicAuthValidation(username: String, password: String): Future[Option[Subject]] = {
    logger.debug(s"Perform basic authentication")
    (for {
      person <- personByEmail(username)
      _ <- validatePassword(password, person)
      _ <- validatePerson(person)
      result <- doLogin(person, None)
    } yield Subject(result.token.get, person.id, person.kundeId, person.rolle, person.secondFactorType)).run.map(_.toOption)
  }

  def personById(personId: PersonId): EitherFuture[Person] = {
    EitherT {
      stammdatenReadRepository.getPerson(personId) map (_ map (_.right[RequestFailed]) getOrElse (errorPersonNotFound.left))
    }
  }

  def validateLoginSettings(personId: PersonId, form: LoginSettingsForm): EitherFuture[FormResult] = {
    for {
      person <- personById(personId)
      projekt <- getProjekt()
      result <- processOrRequestSecondFactor(projekt, person, form.secondFactorAuth)(updateLoginSettings(personId, form))(secondFactor => FormResult(SecondFactorRequired, Some(secondFactor.token), Some(secondFactor.`type`)))
    } yield result
  }

  def getLoginSettings(personId: PersonId): EitherFuture[LoginSettings] = {
    for {
      person <- personById(personId)
      projekt <- getProjekt()
    } yield {
      val secondFactorRequired = person.rolle.map(rolle => projekt.twoFactorAuthentication(rolle)).getOrElse(false)
      LoginSettings(
        secondFactorRequired = secondFactorRequired,
        secondFactorEnabled = person.secondFactorType.isDefined || secondFactorRequired,
        secondFactorType = person.secondFactorType.getOrElse(projekt.defaultSecondFactorType)
      )
    }
  }

  /**
   * Helper methods
   */

  private def containsOneOf(src: String, chars: List[String]): Boolean = {
    chars match {
      case Nil                                => false
      case head :: _ if src.indexOf(head) > 0 => true
      case _ :: tail                          => containsOneOf(src, tail)
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

  private def changePassword(person: Person, newPassword: String, einladung: Option[EinladungId])(implicit subject: Subject): EitherFuture[FormResult] =
    changePassword(subject.personId, person.id, newPassword, einladung)

  private def changePassword(subjectPersonId: PersonId, targetPersonId: PersonId, newPassword: String, einladung: Option[EinladungId] = None): EitherFuture[FormResult] = EitherT {
    //hash password
    val hash = BCrypt.hashpw(newPassword, BCrypt.gensalt())

    (entityStore ? PasswortWechselCommand(subjectPersonId, targetPersonId, hash.toCharArray, einladung)) map {
      case _: PasswortGewechseltEvent => FormResult(Ok, None, None).right
      case _                          => RequestFailed(s"Das Passwort konnte nicht gewechselt werden").left
    }
  }

  private def resetPassword(person: Person): EitherFuture[Boolean] = EitherT {
    (entityStore ? PasswortResetCommand(person.id, person.id)) map {
      case _: PasswortResetGesendetEvent => true.right
      case _                             => RequestFailed(s"Das Passwort konnte nicht gewechselt werden").left
    }
  }

  private def generateOtpSecretResponse(person: Person): EitherFuture[OtpSecretResetResponse] = EitherT {
    val token = generateToken
    val secret = generateOtp
    val personSummary = copyTo[Person, PersonSummary](person)
    val response = OtpSecretResetResponse(token, personSummary, secret)
    secondFactorResetTokenCache(token, () => Future.successful(secret)).map(_ => response.right[RequestFailed])
  }

  private def validateOtpSecretFromCache(form: OtpSecretResetConfirm): EitherFuture[String] = {
    EitherT {
      transform(secondFactorResetTokenCache.get(form.token)) map {
        case Some(secret) if OtpUtil.checkCodeWithSecret(form.code, secret) => ToEitherOps(secret).right
        case _ => errorTokenOrCodeMismatch.left
      }
    }
  }

  private def getProjekt(): EitherFuture[Projekt] = {
    EitherT {
      stammdatenReadRepository.getProjekt map (_ map (_.right[RequestFailed]) getOrElse {
        logger.debug(s"Could not load project")
        errorConfigurationError.left
      })
    }
  }

  private def transform[A](o: Option[Future[A]]): Future[Option[A]] = {
    o.map(f => f.map(Option(_))).getOrElse(Future.successful(None))
  }

  private def readTokenFromCache(token: String): EitherFuture[SecondFactor] = {
    EitherT {
      transform(secondFactorTokenCache.get(token)) map {
        case Some(factor @ EmailSecondFactor(token, _, _)) => (factor: SecondFactor).right[RequestFailed]
        case Some(factor @ OtpSecondFactor(token, _)) => (factor: SecondFactor).right[RequestFailed]
        case _ => errorTokenOrCodeMismatch.left
      }
    }
  }

  private def validateSecondFactor(person: Person, code: String, secondFactor: SecondFactor): EitherFuture[Boolean] =
    EitherT {
      Future {
        (secondFactor, person.otpSecret) match {
          case (OtpSecondFactor(_, person.id), secret) if OtpUtil.checkCodeWithSecret(code, secret) => true.right[RequestFailed]
          case (_: OtpSecondFactor, _) => errorTokenOrCodeMismatch.left
          case (EmailSecondFactor(_, `code`, _), _) => true.right[RequestFailed]
          case (_: EmailSecondFactor, _) => errorTokenOrCodeMismatch.left
        }
      }
    }

  private def personByEmail(email: String): EitherFuture[Person] = {
    EitherT {
      stammdatenReadRepository.getPersonByEmail(email) map (_ map (_.right[RequestFailed]) getOrElse {
        logger.debug(s"No person found for email")
        errorPersonNotFound.left
      })
    }
  }

  private def doLogin(person: Person, secondFactorType: Option[SecondFactorType]): EitherFuture[LoginResult] = {
    //generate token
    val token = generateToken
    EitherT {
      loginTokenCache(token, () => Future.successful(Subject(token, person.id, person.kundeId, person.rolle, person.secondFactorType))) map { _ =>
        val personSummary = copyTo[Person, PersonSummary](person)

        eventStore ! PersonLoggedIn(person.id, org.joda.time.DateTime.now, secondFactorType)

        LoginResult(Ok, Some(token), personSummary, None, secondFactorType).right
      }
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
      secondFactorTokenCache(token, () => Future.successful(secondFactor)).map(_.right[RequestFailed])
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
      case _: SendMailEvent | MailServiceState =>
        true.right
      case other =>
        logger.debug(s"Sending Mail failed resulting in $other")
        RequestFailed(s"Mail konnte nicht zugestellt werden").left
    }
  }

  private def requireSecondFactorAuthentifcation(projekt: Projekt, person: Person): EitherFuture[Boolean] = EitherT {
    Future {
      requireSecondFactorAuthentication match {
        case false                          => false.right[RequestFailed]
        case true if (person.rolle.isEmpty) => true.right[RequestFailed]
        case true => projekt.twoFactorAuthentication.get(person.rolle.get).map {
          case true => true.right[RequestFailed]
          case false if person.secondFactorType.isDefined => true.right[RequestFailed]
          case _ => false.right[RequestFailed]
        }.getOrElse(true.right[RequestFailed])
      }
    }
  }

  private def validatePassword(password: String, person: Person): EitherFuture[Boolean] = EitherT {
    Future {
      person.passwort map { pwd =>
        BCrypt.checkpw(password, new String(pwd)) match {
          case true => true.right[RequestFailed]
          case false =>
            logger.debug(s"Password mismatch")
            errorUsernameOrPasswordMismatch.left[Boolean]
        }
      } getOrElse {
        logger.debug(s"No password for user")
        errorUsernameOrPasswordMismatch.left[Boolean]
      }
    }
  }

  private def validatePerson(person: Person): EitherFuture[Boolean] = EitherT {
    Future {
      person.loginAktiv match {
        case true  => true.right[RequestFailed]
        case false => errorPersonLoginNotActive.left
      }
    }
  }

  private def validateEinladung(token: String): EitherFuture[Einladung] = {
    EitherT {
      stammdatenReadRepository.getEinladung(token) map (_ map { einladung =>
        if (einladung.expires.isAfter(DateTime.now)) {
          einladung.right[RequestFailed]
        } else {
          RequestFailed("Die Einladung mit diesem Token ist abgelaufen").left[Einladung]
        }
      } getOrElse {
        logger.debug(s"Token not found in Einladung")
        RequestFailed("Keine Einladung mit diesem Token gefunden").left[Einladung]
      })
    }
  }

  private def updateLoginSettings(personId: PersonId, form: LoginSettingsForm): EitherFuture[FormResult] = EitherT {
    Future {
      eventStore ! PersonChangedSecondFactorType(personId, form.secondFactorEnabled match {
        case false => None
        case true  => Some(form.secondFactorType)
      })
      FormResult(status = Ok, None, None).right[RequestFailed]
    }
  }

  /**
   * Secure requests with second factor validation
   */
  private def processOrRequestSecondFactor[R](projekt: Projekt, person: Person, secondFactorAuth: Option[SecondFactorAuthentication])(onProceed: => EitherFuture[R])(secondFactorResult: SecondFactor => R): EitherFuture[R] = {
    requireSecondFactorAuthentifcation(projekt, person) flatMap {
      case true if secondFactorAuth.isEmpty => requestSecondFactor(projekt, person)(secondFactorResult)
      case true                             => validateSecondFactor(secondFactorAuth.get)(onProceed)
      case false                            => onProceed
    }
  }

  private def validateSecondFactor[R](secondFactorAuthentication: SecondFactorAuthentication)(onProceed: => EitherFuture[R]): EitherFuture[R] = {
    for {
      secondFactor <- readTokenFromCache(secondFactorAuthentication.token)
      person <- personById(secondFactor.personId)
      _ <- validateSecondFactor(person, secondFactorAuthentication.code, secondFactor)
      _ <- validatePerson(person)
      result <- onProceed
    } yield {
      //cleanup code from cache
      secondFactorTokenCache.remove(secondFactorAuthentication.token)

      result
    }
  }

  private def requestSecondFactor[R](projekt: Projekt, person: Person)(secondFactorResult: SecondFactor => R): EitherFuture[R] = {
    for {
      secondFactor <- generateSecondFactor(projekt, person)
      _ <- maybeSendEmail(secondFactor, person)
    } yield secondFactorResult(secondFactor)
  }

  private def generateToken = UUID.randomUUID.toString
  private def generateCode = (Random.alphanumeric take 6).mkString.toLowerCase
  private def generateOtp = OtpUtil.generateOtpSecretString
}
