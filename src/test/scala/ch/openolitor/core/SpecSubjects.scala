package ch.openolitor.core

import ch.openolitor.core.models.PersonId
import ch.openolitor.core.security.Subject
import ch.openolitor.stammdaten.models._
import ch.openolitor.util.OtpUtil
import org.apache.commons.codec.binary.Base32
import org.joda.time.DateTime
import org.mindrot.jbcrypt.BCrypt

import java.nio.charset.StandardCharsets
import java.util.Locale
import javax.crypto.spec.SecretKeySpec

trait SpecSubjects {
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
  val adminSubjectToken = "someToken"
  val adminSubject = Subject(adminSubjectToken, personId, KundeId(1), None, None)
}
