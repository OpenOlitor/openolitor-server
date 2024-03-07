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

import akka.http.scaladsl.model.headers.{ BasicHttpCredentials, HttpChallenges }
import akka.http.scaladsl.server.{ AuthenticationFailedRejection, Directive1 }
import akka.http.scaladsl.server.AuthenticationFailedRejection.{ CredentialsMissing, CredentialsRejected }
import akka.http.scaladsl.server.Directives._

import scala.concurrent.Future

trait BasicAuthenticatorProvider {
  def authenticateBasicAsync(realm: String, authenticator: (String, String) => Future[Option[Subject]]): Directive1[Subject] = {
    extractCredentials.flatMap {
      case Some(BasicHttpCredentials(username, password)) =>
        onSuccess(authenticator(username, password)).flatMap {
          case Some(subject) => provide(subject)
          case None          => reject(AuthenticationFailedRejection(CredentialsRejected, HttpChallenges.basic(realm)))
        }
      case _: Any => reject(AuthenticationFailedRejection(CredentialsMissing, HttpChallenges.basic(realm)))
    }
  }
}
