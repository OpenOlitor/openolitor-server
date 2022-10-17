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
