package ch.openolitor.core.security

import org.apache.pekko.http.scaladsl.model.headers.{ BasicHttpCredentials, HttpChallenges }
import org.apache.pekko.http.scaladsl.server.{ AuthenticationFailedRejection, Directive1 }
import org.apache.pekko.http.scaladsl.server.AuthenticationFailedRejection.{ CredentialsMissing, CredentialsRejected }
import org.apache.pekko.http.scaladsl.server.Directives._

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
