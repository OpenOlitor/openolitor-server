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

import ch.openolitor.stammdaten.StammdatenJsonProtocol
import spray.json._

trait LoginJsonProtocol extends StammdatenJsonProtocol {
  implicit val loginStatusFormat = new JsonFormat[RequestStatus] {
    def write(obj: RequestStatus): JsValue =
      JsString(obj.productPrefix)

    def read(json: JsValue): RequestStatus =
      json match {
        case JsString(value) => RequestStatus(value).getOrElse(sys.error(s"Unknown LoginStatus:$value"))
        case pt              => sys.error(s"Unknown LoginStatus:$pt")
      }
  }

  implicit val emailSecondFactorFormat = jsonFormat3(EmailSecondFactor)
  implicit val otpSecondFactorFormat = jsonFormat2(OtpSecondFactor)
  implicit val otpSecretResetRequestFormat = jsonFormat1(OtpSecretResetRequest)
  implicit val otpSecretResetConfirmFormat = jsonFormat2(OtpSecretResetConfirm)

  implicit val subjectFormat = jsonFormat5(Subject)
  implicit val userFormat = jsonFormat2(User)
  implicit val loginFormFormat = jsonFormat2(LoginForm)
  implicit val secondFactorLoginFormFormat = jsonFormat2(SecondFactorAuthentication)
  implicit val changePasswordFormFormat = jsonFormat3(ChangePasswordForm)
  implicit val setPasswordFormFormat = jsonFormat3(SetPasswordForm)
  implicit val passwordResetFormFormat = jsonFormat1(PasswordResetForm)

  implicit val requestFailedFormat = jsonFormat1(RequestFailed)
  implicit val loginResultFormat = jsonFormat5(LoginResult)
  implicit val formResultFormat = jsonFormat3(FormResult)
  implicit val loginSettingsFormat = jsonFormat3(LoginSettings)
  implicit val loginSettingsFormFormat = jsonFormat3(LoginSettingsForm)
  implicit val otpSecretResetResponseFormat = jsonFormat3(OtpSecretResetResponse)
}