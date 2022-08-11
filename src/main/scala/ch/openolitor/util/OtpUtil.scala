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
package ch.openolitor.util

import java.nio.charset.StandardCharsets
import java.security.Key
import java.time.{ Duration, Instant }

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator
import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator.TOTP_ALGORITHM_HMAC_SHA1
import javax.crypto.KeyGenerator
import javax.crypto.spec.SecretKeySpec
import org.apache.commons.codec.binary.Base32

object OtpUtil {
  private val SLIDING_WINDOW_MARGIN = 15L
  lazy val Totp = new TimeBasedOneTimePasswordGenerator(Duration.ofSeconds(30), 6, TOTP_ALGORITHM_HMAC_SHA1)

  /**
   * @return a generated TOTP secret Base32 encoded String.
   */
  def generateOtpSecretString: String = {
    val keyGenerator = KeyGenerator.getInstance(Totp.getAlgorithm)

    val key = keyGenerator.generateKey()

    new String(new Base32().encode(key.getEncoded))
  }

  /**
   * The time window is double the size of the time step and therefore slides with -15s, +15s respectively.
   *
   * @param code The code as string.
   * @param secretBase32 The secret as Base32 encoded string.
   * @return true if the given code matches the currently valid code for the given secret.
   */
  def checkCodeWithSecret(code: String, secretBase32: String): Boolean = {
    val key =
      new SecretKeySpec(new Base32().decode(secretBase32.getBytes(StandardCharsets.US_ASCII)), Totp.getAlgorithm)

    slidingWindowTotpCodes(key).contains(code.toInt)
  }

  private def slidingWindowTotpCodes(key: Key) = {
    val now = Instant.now()
    List(now.minusSeconds(SLIDING_WINDOW_MARGIN), now, now.plusSeconds(SLIDING_WINDOW_MARGIN)).map(instant =>
      Totp.generateOneTimePassword(key, instant))
  }
}
