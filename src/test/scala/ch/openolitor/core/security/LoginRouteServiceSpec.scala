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
import akka.http.caching.LfuCache
import akka.testkit.{ TestActorRef, TestProbe }
import ch.openolitor.core.SystemConfig
import ch.openolitor.core.db.MultipleAsyncConnectionPoolContext
import ch.openolitor.core.domain.SystemEvents.PersonChangedOtpSecret
import ch.openolitor.core.filestore.MockFileStoreComponent
import ch.openolitor.core.mailservice.MailServiceMock
import ch.openolitor.core.models.PersonId
import ch.openolitor.stammdaten.MockStammdatenReadRepositoryComponent
import ch.openolitor.stammdaten.models._
import ch.openolitor.util.OtpUtil
import org.apache.commons.codec.binary.Base32
import org.joda.time.DateTime
import org.mindrot.jbcrypt.BCrypt
import org.mockito.ArgumentMatchers.{ eq => isEq }
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable._

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Locale
import javax.crypto.spec.SecretKeySpec
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._

class LoginRouteServiceSpec(implicit ec: ExecutionEnv) extends Specification with Mockito {
  val email = "info@test.com"
  val pwd = "pwd"
  val pwdHashed = BCrypt.hashpw(pwd, BCrypt.gensalt())
  val personId = PersonId(1)
  val otpSecret = OtpUtil.generateOtpSecretString
  val otpSecretKey =
    new SecretKeySpec(
      new Base32().decode(otpSecret.getBytes(StandardCharsets.US_ASCII)),
      OtpUtil.Totp.getAlgorithm
    )
  val personKundeActive = Person(personId, KundeId(1), None, "Test", "Test", Some(email), None, None, None, None, 1, true, Some(pwdHashed.toCharArray), None,
    false, Some(KundenZugang), Set.empty, None, otpSecret, false, false, DateTime.now, PersonId(1), DateTime.now, PersonId(1))
  val personAdminActive = Person(personId, KundeId(1), None, "Test", "Test", Some(email), None, None, None, None, 1, true, Some(pwdHashed.toCharArray), None,
    false, Some(AdministratorZugang), Set.empty, None, otpSecret, false, false, DateTime.now, PersonId(1), DateTime.now, PersonId(1))
  val personAdminInactive = Person(personId, KundeId(1), None, "Test", "Test", Some(email), None, None, None, None, 1, false, Some(pwdHashed.toCharArray), None,
    false, Some(AdministratorZugang), Set.empty, None, otpSecret, false, false, DateTime.now, PersonId(1), DateTime.now, PersonId(1))
  val projekt = Projekt(ProjektId(1), "Test", None, None, None, None, None, true, true, true, CHF, 1, 1, Map(AdministratorZugang -> true, KundenZugang -> false), EmailSecondFactorType, Locale.GERMAN, None, None, None, false, false, EinsatzEinheit("Tage"), 1, false, false, DateTime.now, PersonId(1), DateTime.now, PersonId(1))
  val adminSubject = Subject("someToken", personId, KundeId(1), None, None)

  implicit val ctx = MultipleAsyncConnectionPoolContext()
  val timeout = 5 seconds
  val retries = 3

  "Direct login" should {

    "Succeed" in {
      val service = new MockLoginRouteService(false)

      service.stammdatenReadRepository.getProjekt(any[ExecutionContext], any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(projekt))
      service.stammdatenReadRepository.getPersonByEmail(isEq(email))(any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(personAdminActive))

      val result = service.validateLogin(LoginForm(email, pwd)).run

      result.map { x =>
        x.toEither.map(_.status).toOption.get === Ok
        val token = x.toEither.map(_.token).toOption.get
        service.loginTokenCache.get(token.get) must beSome
      }.await(3, timeout)
    }

    "Fail when login not active" in {
      val service = new MockLoginRouteService(false)

      service.stammdatenReadRepository.getProjekt(any[ExecutionContext], any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(projekt))
      service.stammdatenReadRepository.getPersonByEmail(any[String])(any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(personAdminInactive))

      val result = service.validateLogin(LoginForm(email, pwd)).run

      result.map { _.toEither must beLeft(RequestFailed("Login wurde deaktiviert")) }.await(3, timeout)
    }

    "Fail on password mismatch" in {
      val service = new MockLoginRouteService(false)

      service.stammdatenReadRepository.getProjekt(any[ExecutionContext], any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(projekt))
      service.stammdatenReadRepository.getPersonByEmail(any[String])(any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(personAdminActive))

      val result = service.validateLogin(LoginForm(email, "wrongPwd")).run

      result.map { _.toEither must beLeft(RequestFailed("Benutzername oder Passwort stimmen nicht überein")) }.await(3, timeout)
    }

    "Fail when no person was found" in {
      val service = new MockLoginRouteService(false)

      service.stammdatenReadRepository.getProjekt(any[ExecutionContext], any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(projekt))
      service.stammdatenReadRepository.getPersonByEmail(any[String])(any[MultipleAsyncConnectionPoolContext]) returns Future.successful(None)

      val result = service.validateLogin(LoginForm("anyEmail", pwd)).run

      result.map { _.toEither must beLeft(RequestFailed("Person konnte nicht gefunden werden")) }.await(3, timeout)
    }
  }

  "Require second factor" should {
    "be disabled when disabled in project settings" in {
      val service = new MockLoginRouteService(true)

      service.stammdatenReadRepository.getProjekt(any[ExecutionContext], any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(projekt))
      service.stammdatenReadRepository.getPersonByEmail(any[String])(any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(personKundeActive))

      val result = service.validateLogin(LoginForm(email, pwd)).run

      result.map { _.toEither.map(_.status).toOption.get === Ok }.await(3, timeout)
    }

    "be enabled by project settings, default type email" in {
      val service = new MockLoginRouteService(true)

      service.stammdatenReadRepository.getProjekt(any[ExecutionContext], any[MultipleAsyncConnectionPoolContext]) returns
        Future.successful(Some(projekt.copy(defaultSecondFactorType = EmailSecondFactorType)))
      service.stammdatenReadRepository.getPersonByEmail(any[String])(any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(personAdminActive))

      val result = service.validateLogin(LoginForm(email, pwd)).run

      result.map { _.toEither.map(_.status).toOption.get === SecondFactorRequired }.await(3, timeout)
      result.map { _.toEither.map(_.secondFactorType).toOption.get === Some(EmailSecondFactorType) }.await(3, timeout)
    }

    "be enabled by project settings, default type otp" in {
      val service = new MockLoginRouteService(true)

      service.stammdatenReadRepository.getProjekt(any[ExecutionContext], any[MultipleAsyncConnectionPoolContext]) returns
        Future.successful(Some(projekt.copy(defaultSecondFactorType = OtpSecondFactorType)))
      service.stammdatenReadRepository.getPersonByEmail(any[String])(any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(personAdminActive))

      val result = service.validateLogin(LoginForm(email, pwd)).run

      result.map { _.toEither.map(_.status).toOption.get === SecondFactorRequired }.await(3, timeout)
      result.map { _.toEither.map(_.secondFactorType).toOption.get === Some(OtpSecondFactorType) }.await(3, timeout)
    }

    "be enabled by project settings, overridden type otp" in {
      val service = new MockLoginRouteService(true)

      service.stammdatenReadRepository.getProjekt(any[ExecutionContext], any[MultipleAsyncConnectionPoolContext]) returns
        Future.successful(Some(projekt.copy(defaultSecondFactorType = EmailSecondFactorType)))
      service.stammdatenReadRepository.getPersonByEmail(any[String])(any[MultipleAsyncConnectionPoolContext]) returns
        Future.successful(Some(personAdminActive.copy(secondFactorType = Some(OtpSecondFactorType))))

      val result = service.validateLogin(LoginForm(email, pwd)).run

      result.map { _.toEither.map(_.status).toOption.get === SecondFactorRequired }.await(3, timeout)
      result.map { _.toEither.map(_.secondFactorType).toOption.get === Some(OtpSecondFactorType) }.await(3, timeout)
    }

    "be enabled by project settings, overridden type email" in {
      val service = new MockLoginRouteService(true)

      service.stammdatenReadRepository.getProjekt(any[ExecutionContext], any[MultipleAsyncConnectionPoolContext]) returns
        Future.successful(Some(projekt.copy(defaultSecondFactorType = OtpSecondFactorType)))
      service.stammdatenReadRepository.getPersonByEmail(any[String])(any[MultipleAsyncConnectionPoolContext]) returns
        Future.successful(Some(personAdminActive.copy(secondFactorType = Some(EmailSecondFactorType))))

      val result = service.validateLogin(LoginForm(email, pwd)).run

      result.map { _.toEither.map(_.status).toOption.get === SecondFactorRequired }.await(3, timeout)
      result.map { _.toEither.map(_.secondFactorType).toOption.get === Some(EmailSecondFactorType) }.await(3, timeout)
    }

    "be enabled by project settings, default type otp, otpReset required" in {
      val service = new MockLoginRouteService(true)

      service.stammdatenReadRepository.getProjekt(any[ExecutionContext], any[MultipleAsyncConnectionPoolContext]) returns
        Future.successful(Some(projekt.copy(defaultSecondFactorType = OtpSecondFactorType)))
      service.stammdatenReadRepository.getPersonByEmail(any[String])(any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(personAdminActive.copy(otpReset = true)))

      val result = service.validateLogin(LoginForm(email, pwd)).run

      result.map { _.toOption.flatMap(_.otpSecret) must beSome(otpSecret) }.await(3, timeout)
    }
  }

  "Email Second factor login" should {
    "Succeed" in {
      val service = new MockLoginRouteService(true)
      val token = "asdasad"
      val code = "sadfasd"
      val secondFactor = EmailSecondFactor(token, code, personId)

      //store token in cache
      service.secondFactorTokenCache(token, () => Future.successful(secondFactor))

      service.stammdatenReadRepository.getPerson(isEq(personId))(any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(personAdminActive))

      val result = service.validateSecondFactorLogin(SecondFactorAuthentication(token, code)).run

      result.map { _.toEither.map(_.status).toOption.get === Ok }.await(3, timeout)
    }

    "Fail when login not active" in {
      val service = new MockLoginRouteService(true)
      val token = "asdasad"
      val code = "sadfasd"
      val secondFactor = EmailSecondFactor(token, code, personId)

      //store token in cache
      service.secondFactorTokenCache(token, () => Future.successful(secondFactor))

      service.stammdatenReadRepository.getPerson(isEq(personId))(any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(personAdminInactive))

      val result = service.validateSecondFactorLogin(SecondFactorAuthentication(token, code)).run

      result.map { _.toEither must beLeft(RequestFailed("Login wurde deaktiviert")) }.await(3, timeout)
    }

    "Fail when code does not match" in {
      val service = new MockLoginRouteService(true)
      val token = "asdasad"
      val code = "sadfasd"
      val secondFactor = EmailSecondFactor(token, code, personId)

      //store token in cache
      service.secondFactorTokenCache(token, () => Future.successful(secondFactor))

      service.stammdatenReadRepository.getPerson(isEq(personId))(any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(personAdminActive))

      val result = service.validateSecondFactorLogin(SecondFactorAuthentication(token, "anyCode")).run

      result.map { _.toEither must beLeft(RequestFailed("Code stimmt nicht überein")) }.await(3, timeout)
    }

    "Fail when token does not match" in {
      val service = new MockLoginRouteService(true)
      val token = "asdasad"
      val code = "sadfasd"
      val secondFactor = EmailSecondFactor(token, code, personId)

      //store token in cache
      service.secondFactorTokenCache(token, () => Future.successful(secondFactor))

      service.stammdatenReadRepository.getPerson(isEq(personId))(any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(personAdminActive))

      val result = service.validateSecondFactorLogin(SecondFactorAuthentication("anyToken", code)).run

      result.map { _.toEither must beLeft(RequestFailed("Code stimmt nicht überein")) }.await(3, timeout)
    }

    "Fail when person not found" in {
      val service = new MockLoginRouteService(true)
      val token = "asdasad"
      val code = "sadfasd"
      val secondFactor = EmailSecondFactor(token, code, personId)

      //store token in cache
      service.secondFactorTokenCache(token, () => Future.successful(secondFactor))

      service.stammdatenReadRepository.getPerson(isEq(personId))(any[MultipleAsyncConnectionPoolContext]) returns Future.successful(None)

      val result = service.validateSecondFactorLogin(SecondFactorAuthentication(token, code)).run

      result.map { _.toEither must beLeft(RequestFailed("Person konnte nicht gefunden werden")) }.await(3, timeout)
    }

    "Ensure token gets deleted after successful login" in {
      val service = new MockLoginRouteService(true)
      val token = "asdasad"
      val code = "sadfasd"
      val secondFactor = EmailSecondFactor(token, code, personId)

      //store token in cache
      service.secondFactorTokenCache(token, () => Future.successful(secondFactor))

      service.stammdatenReadRepository.getPerson(isEq(personId))(any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(personAdminActive))
      val result1 = service.validateSecondFactorLogin(SecondFactorAuthentication(token, code)).run
      result1.map { _.toEither.map(_.status).toOption.get === Ok }.await(3, timeout)

      service.secondFactorTokenCache.get(token) must beNone

      //second try
      val result2 = service.validateSecondFactorLogin(SecondFactorAuthentication(token, code)).run
      result2.map { _.toEither must beLeft(RequestFailed("Code stimmt nicht überein")) }.await(3, timeout)
    }
  }

  "OTP Second factor login" should {
    "Succeed" in {
      val service = new MockLoginRouteService(true)
      val token = "asdasad"
      val secondFactor = OtpSecondFactor(token, personId)
      val code = OtpUtil.Totp.generateOneTimePassword(otpSecretKey, Instant.now())

      //store token in cache
      service.secondFactorTokenCache(token, () => Future.successful(secondFactor))

      service.stammdatenReadRepository.getPerson(isEq(personId))(any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(personAdminActive))

      val result = service.validateSecondFactorLogin(SecondFactorAuthentication(token, String.valueOf(code))).run

      result.map { _.toEither.map(_.status).toOption.get === Ok }.await(3, timeout)
    }

    "Fail when login not active" in {
      val service = new MockLoginRouteService(true)
      val token = "asdasad"
      val secondFactor = OtpSecondFactor(token, personId)
      val code = OtpUtil.Totp.generateOneTimePassword(otpSecretKey, Instant.now())

      //store token in cache
      service.secondFactorTokenCache(token, () => Future.successful(secondFactor))

      service.stammdatenReadRepository.getPerson(isEq(personId))(any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(personAdminInactive))

      val result = service.validateSecondFactorLogin(SecondFactorAuthentication(token, String.valueOf(code))).run

      result.map { _.toEither must beLeft(RequestFailed("Login wurde deaktiviert")) }.await(3, timeout)
    }

    "Fail when code does not match" in {
      val service = new MockLoginRouteService(true)
      val token = "asdasad"
      val secondFactor = OtpSecondFactor(token, personId)

      //store token in cache
      service.secondFactorTokenCache(token, () => Future.successful(secondFactor))

      service.stammdatenReadRepository.getPerson(isEq(personId))(any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(personAdminActive))

      val result = service.validateSecondFactorLogin(SecondFactorAuthentication(token, "1234")).run

      result.map { _.toEither must beLeft(RequestFailed("Code stimmt nicht überein")) }.await(3, timeout)
    }

    "Fail when token does not match" in {
      val service = new MockLoginRouteService(true)
      val token = "asdasad"
      val secondFactor = OtpSecondFactor(token, personId)
      val code = OtpUtil.Totp.generateOneTimePassword(otpSecretKey, Instant.now())

      //store token in cache
      service.secondFactorTokenCache(token, () => Future.successful(secondFactor))

      service.stammdatenReadRepository.getPerson(isEq(personId))(any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(personAdminActive))

      val result = service.validateSecondFactorLogin(SecondFactorAuthentication("anyToken", String.valueOf(code))).run

      result.map { _.toEither must beLeft(RequestFailed("Code stimmt nicht überein")) }.await(3, timeout)
    }

    "Fail when person not found" in {
      val service = new MockLoginRouteService(true)
      val token = "asdasad"
      val secondFactor = OtpSecondFactor(token, personId)
      val code = OtpUtil.Totp.generateOneTimePassword(otpSecretKey, Instant.now())

      //store token in cache
      service.secondFactorTokenCache(token, () => Future.successful(secondFactor))

      service.stammdatenReadRepository.getPerson(isEq(personId))(any[MultipleAsyncConnectionPoolContext]) returns Future.successful(None)

      val result = service.validateSecondFactorLogin(SecondFactorAuthentication(token, String.valueOf(code))).run

      result.map { _.toEither must beLeft(RequestFailed("Person konnte nicht gefunden werden")) }.await(3, timeout)
    }

    "Ensure token gets deleted after successful login" in {
      val service = new MockLoginRouteService(true)
      val token = "asdasad"
      val secondFactor = OtpSecondFactor(token, personId)
      val code = OtpUtil.Totp.generateOneTimePassword(otpSecretKey, Instant.now())

      //store token in cache
      service.secondFactorTokenCache(token, () => Future.successful(secondFactor))

      service.stammdatenReadRepository.getPerson(isEq(personId))(any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(personAdminActive))
      val result1 = service.validateSecondFactorLogin(SecondFactorAuthentication(token, String.valueOf(code))).run
      result1.map { _.toEither.map(_.status).toOption.get === Ok }.await(3, timeout)

      service.secondFactorTokenCache.get(token) must beNone

      //second try
      val result2 = service.validateSecondFactorLogin(SecondFactorAuthentication(token, String.valueOf(code))).run
      result2.map { _.toEither must beLeft(RequestFailed("Code stimmt nicht überein")) }.await(3, timeout)
    }
  }

  "OTP Reset Request" should {
    "Request new secret with current otp code" in {
      val service = new MockLoginRouteService(true)
      val token = "asdasad"
      val secondFactor = OtpSecondFactor(token, personId)
      val code = OtpUtil.Totp.generateOneTimePassword(otpSecretKey, Instant.now())
      implicit val subject = adminSubject

      //store token in cache
      service.secondFactorTokenCache(token, () => Future.successful(secondFactor))

      service.stammdatenReadRepository.getPerson(isEq(personId))(any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(personAdminActive))

      val result = service.validateOtpResetRequest(OtpSecretResetRequest(String.valueOf(code))).run

      result.map { _.toEither must beRight }.await(3, timeout)
    }

    "Fail request new secret with wrong otp code" in {
      val service = new MockLoginRouteService(true)
      val token = "asdasad"
      val secondFactor = OtpSecondFactor(token, personId)
      implicit val subject = adminSubject

      //store token in cache
      service.secondFactorTokenCache(token, () => Future.successful(secondFactor))

      service.stammdatenReadRepository.getPerson(isEq(personId))(any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(personAdminActive))

      val result = service.validateOtpResetRequest(OtpSecretResetRequest("1234")).run

      result.map { _.toEither must beLeft }.await(3, timeout)
    }

    "Fail request new secret with subject not matching a person" in {
      val service = new MockLoginRouteService(true)
      val token = "asdasad"
      val secondFactor = OtpSecondFactor(token, personId)
      val code = OtpUtil.Totp.generateOneTimePassword(otpSecretKey, Instant.now())
      implicit val subject = adminSubject.copy(personId = PersonId(100))

      //store token in cache
      service.secondFactorTokenCache(token, () => Future.successful(secondFactor))

      service.stammdatenReadRepository.getPerson(isEq(PersonId(100)))(any[MultipleAsyncConnectionPoolContext]) returns Future.successful(None)

      val result = service.validateOtpResetRequest(OtpSecretResetRequest(String.valueOf(code))).run

      result.map { _.toEither must beLeft }.await(3, timeout)
    }
  }

  "OTP Reset Confirmation" should {
    "Successfully store new secret" in {
      val service = new MockLoginRouteService(true)
      val token = "asdasad"
      val newOtpSecret = OtpUtil.generateOtpSecretString
      val newOtpSecretKey = new SecretKeySpec(
        new Base32().decode(newOtpSecret.getBytes(StandardCharsets.US_ASCII)),
        OtpUtil.Totp.getAlgorithm
      )
      val code = OtpUtil.Totp.generateOneTimePassword(newOtpSecretKey, Instant.now())
      implicit val subject = adminSubject

      //store token in cache
      service.secondFactorResetTokenCache(token, () => Future.successful(newOtpSecret))

      service.stammdatenReadRepository.getPerson(isEq(personId))(any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(personAdminActive))

      val result = service.validateOtpResetConfirm(OtpSecretResetConfirm(token, String.valueOf(code))).run

      result.map { _.toEither must beRight(true) }.await(3, timeout)

      // expect event
      service.eventStoreProbe.expectMsg(PersonChangedOtpSecret(personId, newOtpSecret))

      // ensure token was cleaned
      service.secondFactorTokenCache.get(token) must beNone
    }

    "Fail storing if secret mismatch" in {
      val service = new MockLoginRouteService(true)
      val token = "asdasad"
      val newOtpSecret = OtpUtil.generateOtpSecretString
      // use old secret key
      val code = OtpUtil.Totp.generateOneTimePassword(otpSecretKey, Instant.now())
      implicit val subject = adminSubject

      //store token in cache
      service.secondFactorResetTokenCache(token, () => Future.successful(newOtpSecret))

      service.stammdatenReadRepository.getPerson(isEq(personId))(any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(personAdminActive))

      val result = service.validateOtpResetConfirm(OtpSecretResetConfirm(token, String.valueOf(code))).run

      result.map { _.toEither must beLeft }.await(3, timeout)
    }

    "Fail if token mismatch" in {
      val service = new MockLoginRouteService(true)
      val token = "asdasad"
      val newOtpSecret = OtpUtil.generateOtpSecretString
      val newOtpSecretKey = new SecretKeySpec(
        new Base32().decode(newOtpSecret.getBytes(StandardCharsets.US_ASCII)),
        OtpUtil.Totp.getAlgorithm
      )
      val code = OtpUtil.Totp.generateOneTimePassword(newOtpSecretKey, Instant.now())
      implicit val subject = adminSubject

      //store token in cache
      service.secondFactorResetTokenCache(token, () => Future.successful(newOtpSecret))

      service.stammdatenReadRepository.getPerson(isEq(personId))(any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(personAdminActive))

      val result = service.validateOtpResetConfirm(OtpSecretResetConfirm("someOtherToken", String.valueOf(code))).run

      result.map { _.toEither must beLeft }.await(3, timeout)
    }
  }
}

class MockLoginRouteService(
  requireSecondFactorAuthenticationP: Boolean
)
  extends LoginRouteService
  with MockFileStoreComponent
  with MockStammdatenReadRepositoryComponent {

  override val entityStore: ActorRef = null
  override val reportSystem: ActorRef = null
  override val jobQueueService: ActorRef = null
  override val dbEvolutionActor: ActorRef = null
  implicit val system = ActorSystem("test")
  val eventStoreProbe = TestProbe()
  override val sysConfig: SystemConfig = SystemConfig(null, null, MultipleAsyncConnectionPoolContext())
  //override val eventStore: ActorRef = TestActorRef(new DefaultSystemEventStore(sysConfig, dbEvolutionActor))
  override val eventStore: ActorRef = eventStoreProbe.ref
  override val mailService: ActorRef = TestActorRef(new MailServiceMock)
  override val loginTokenCache: Cache[String, Subject] = LfuCache(system)
  override val airbrakeNotifier: ActorRef = null

  override lazy val requireSecondFactorAuthentication = requireSecondFactorAuthenticationP
  override implicit protected val executionContext: ExecutionContext = system.dispatcher
}
