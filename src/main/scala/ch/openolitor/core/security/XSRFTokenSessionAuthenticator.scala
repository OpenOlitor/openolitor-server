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

import akka.http.scaladsl.server.{ Directive1, Rejection, RequestContext }
import akka.http.scaladsl.server.Directives._
import ch.openolitor.core.ExecutionContextAware
import com.typesafe.scalalogging.LazyLogging
import org.joda.time.{ DateTime, DateTimeZone }
import scalaz._
import scalaz.Scalaz._

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.util.{ Try, Failure => TryFailure, Success => TrySuccess }

object AuthCookies {
  val CsrfTokenCookieName = "XSRF-TOKEN"
  val CsrfTokenHeaderName = "XSRF-TOKEN"
}

/** If anything goes wrong during authentication, this is the rejection to use. */
case class AuthenticatorRejection(reason: String) extends Rejection

/**
 * Dieser Authenticator authentisiert den Benutzer anhand folgender Kriteren:
 * 1. Im Cookie header sowie im HttpHeader muss das Token mitgeliefert werden
 * 2. Im HttpHeader wird das Token mit der Request Zeit im ISO Format und dem Separator :: mitgeliefert
 * 3. Token im Header und Cookie müssen übereinstimmen
 * 4. Request Zeit darf eine maximale Duration nicht überschreiten (Request kann nicht zu einem späteren Zeitpunkt erneut ausgeführt werden)
 * 5. Im lokalen Cache muss eine PersonId für das entsprechende token gespeichert sein
 */
trait XSRFTokenSessionAuthenticatorProvider extends LazyLogging with TokenCache with ExecutionContextAware {
  import AuthCookies._

  type Authentication = Either[AuthenticatorRejection, Subject]

  /*
   * Configure max time since
   */
  val maxRequestDelay: Option[Duration]

  def authenticate(implicit context: RequestContext): Directive1[Subject] =
    onSuccess(new XSRFTokenSessionAuthenticatorImpl()(context)) flatMap {
      case Left(rejection: AuthenticatorRejection) =>
        reject(rejection): Directive1[Subject]
      case Right(s: Subject) =>
        provide(s)
    }

  private class XSRFTokenSessionAuthenticatorImpl {
    val noDateTimeValue = new DateTime(0, 1, 1, 0, 0, 0, DateTimeZone.UTC)

    type RequestValidation[V] = EitherT[AuthenticatorRejection, Future, V]

    def apply(ctx: RequestContext): Future[Authentication] = {
      logger.debug(s"${ctx.request.uri}:${ctx.request.method}")
      (for {
        headerToken <- findHeaderToken(ctx)
        pair <- extractTimeFromHeaderToken(headerToken)
        (token, requestTime) = pair
        _ <- validateCookieAndHeaderToken(token, token)
        _ <- compareRequestTime(requestTime)
        subject <- findSubjectInCache(token)
      } yield subject).run.map(_.toEither)
    }

    private def findCookieToken(ctx: RequestContext): RequestValidation[String] = EitherT {
      Future {
        logger.debug(s"Check cookies:${ctx.request.cookies}")
        ctx.request.cookies.find(_.name.toLowerCase == CsrfTokenCookieName.toLowerCase).map(_.value.right[AuthenticatorRejection]).getOrElse(AuthenticatorRejection("Kein XSRF-Token im Cookie gefunden").left)
      }
    }

    private def findHeaderToken(ctx: RequestContext): RequestValidation[String] = EitherT {
      Future {
        logger.debug(s"Headers present: ${ctx.request.headers}")
        ctx.request.headers.find(_.name.toLowerCase == CsrfTokenHeaderName.toLowerCase).map(_.value.right[AuthenticatorRejection]).getOrElse(AuthenticatorRejection("Kein XSRF-Token im Header gefunden").left)
      }
    }

    private def extractTimeFromHeaderToken(headerToken: String): RequestValidation[(String, DateTime)] = EitherT {
      Future {
        headerToken.split("::").toList match {
          case token :: timeString :: Nil =>
            Try(DateTime.parse(timeString)) match {
              case TrySuccess(dateTime) => (token, dateTime).right
              case TryFailure(e)        => AuthenticatorRejection(s"Ungültiges Datumsformat im Header:$timeString").left
            }
          case token :: Nil => (token, noDateTimeValue).right
          case x            => AuthenticatorRejection(s"Ungüliges Token im Header: $x").left
        }
      }
    }

    private def validateCookieAndHeaderToken(cookieToken: String, headerToken: String): RequestValidation[Boolean] = EitherT {
      Future {
        if (cookieToken == headerToken) true.right
        else AuthenticatorRejection(s"Cookie und Header Token weichen voneinander ab '$cookieToken' != '$headerToken'").left
      }
    }

    private def findSubjectInCache(token: String): RequestValidation[Subject] = EitherT {
      loginTokenCache.get(token) match {
        case Some(subjectFuture) =>
          subjectFuture.map(_.right[AuthenticatorRejection])
        case None =>
          Future.successful(AuthenticatorRejection(s"Keine Person gefunden für token: $token").left)
      }
    }

    private def compareRequestTime(requestTime: DateTime): RequestValidation[Boolean] = EitherT {
      Future {
        maxRequestDelay match {
          case Some(maxTime) =>
            val now = DateTime.now
            val min = now.minus(maxTime.toMillis)
            if (min.isAfter(requestTime)) {
              //request took too long
              AuthenticatorRejection(s"Zeitstempel stimmt nicht überein. Aktuell: $now, Min: $min, Zeitstempel: $requestTime").left
            } else {
              true.right[AuthenticatorRejection]
            }
          case None =>
            true.right[AuthenticatorRejection]
        }
      }
    }
  }
}