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

import ch.openolitor.core.JSONSerializable
import ch.openolitor.core.models.PersonId
import ch.openolitor.stammdaten.models.{ EmailSecondFactorType, KundeId, OtpSecondFactorType, PersonDetail, PersonSummary, Rolle, SecondFactorType }

sealed trait SecondFactor {
  val personId: PersonId;
  val token: String;

  def `type`: SecondFactorType
}
case class EmailSecondFactor(token: String, code: String, personId: PersonId) extends SecondFactor with JSONSerializable {
  def `type` = EmailSecondFactorType
}
case class OtpSecondFactor(token: String, personId: PersonId) extends SecondFactor with JSONSerializable {
  def `type` = OtpSecondFactorType
}

trait AuthenticatedForm extends JSONSerializable {
  val secondFactorAuth: Option[SecondFactorAuthentication]
}

case class SecondFactorAuthentication(token: String, code: String) extends JSONSerializable

case class LoginForm(email: String, passwort: String) extends JSONSerializable
case class ChangePasswordForm(alt: String, neu: String, secondFactorAuth: Option[SecondFactorAuthentication]) extends JSONSerializable
case class SetPasswordForm(token: String, neu: String, secondFactorAuth: Option[SecondFactorAuthentication]) extends JSONSerializable
case class PasswordResetForm(email: String) extends JSONSerializable
case class LoginSettingsForm(secondFactorEnabled: Boolean, secondFactorType: SecondFactorType, secondFactorAuth: Option[SecondFactorAuthentication]) extends AuthenticatedForm

case class OtpDisable(code: String) extends JSONSerializable
case class OtpSecretResetRequest(code: String) extends JSONSerializable
case class OtpSecretResetConfirm(token: String, code: String) extends JSONSerializable
case class OtpSecretResetResponse(token: String, person: PersonSummary, otpSecret: String) extends JSONSerializable

sealed trait RequestStatus extends Product
case object Ok extends RequestStatus
case object SecondFactorRequired extends RequestStatus

object RequestStatus {
  def apply(value: String): Option[RequestStatus] = {
    Vector(Ok, SecondFactorRequired).find(_.toString == value)
  }
}

trait ResultWithSecondFactorValidation {
  val status: RequestStatus
  val token: Option[String]
}

case class FormResult(status: RequestStatus, token: Option[String], secondFactorType: Option[SecondFactorType]) extends JSONSerializable with ResultWithSecondFactorValidation

case class LoginResult(status: RequestStatus, token: Option[String], person: PersonSummary, otpSecret: Option[String], secondFactorType: Option[SecondFactorType]) extends JSONSerializable with ResultWithSecondFactorValidation

case class RequestFailed(msg: String)

case class Subject(token: String, personId: PersonId, kundeId: KundeId, rolle: Option[Rolle], secondFactorType: Option[SecondFactorType]) extends JSONSerializable

case class User(user: PersonDetail, subject: Subject) extends JSONSerializable

case class LoginSettings(secondFactorRequired: Boolean, secondFactorEnabled: Boolean, secondFactorType: SecondFactorType) extends JSONSerializable

