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

import akka.actor.ActorSystem
import akka.http.caching.LfuCache
import akka.http.caching.scaladsl.Cache
import akka.http.scaladsl.model.{ HttpMethod, HttpMethods, StatusCodes }
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.testkit.Specs2RouteTest
import ch.openolitor.core.models.PersonId
import ch.openolitor.stammdaten.models.KundeId
import org.joda.time.DateTime
import org.mockito.MockitoSugar
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable._

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }

class XSRFTokenSessionAuthenticatorImplSpec(implicit ec: ExecutionEnv) extends Specification with Specs2RouteTest with MockitoSugar {
  import AuthCookies._

  val timeout = 5 seconds
  val retries = 3
  val SuccessResult = "success"

  def setupRoute(token: String, maybeSubject: Option[Subject], httpMethod: HttpMethod): Route = {
    val provider = new MockXSRFTokenSessionAuthenticatorProvider(None, system)
    // prepare cache
    maybeSubject.foreach { subject =>
      provider.loginTokenCache(token, () => Future.successful(subject))
    }

    implicit val defaultRejectionHandler = RejectionHandler.newBuilder().handle {
      case AuthenticatorRejection(cause) =>
        complete(StatusCodes.Unauthorized, cause)
    }.result()

    Route.seal(method(httpMethod) {
      extractRequestContext { requestContext =>
        provider.authenticate(requestContext) { _ =>
          complete(200, SuccessResult)
        }
      }
    })
  }

  "Authenticate" should {
    val token = "asdasd"
    val personId = PersonId(123)
    val kundeId = KundeId(321)
    val subject = Subject(token, personId, kundeId, None, None)

    "Succeed without time limitation" in {
      // prepare requestcontext
      val cookie = Cookie(CsrfTokenCookieName, token)
      val tokenHeader = RawHeader(CsrfTokenHeaderName, token)
      val headers = List(cookie, tokenHeader)

      Post("/test").withHeaders(headers) ~> setupRoute(token, Some(subject), HttpMethods.POST) ~> check {
        responseAs[String] === SuccessResult
      }
    }

    "Succeed with time limitation" in {
      val delay = 10 seconds

      val time = DateTime.now.toString
      val headerValue = s"$token::$time"

      // prepare requestcontext
      val cookie = Cookie(CsrfTokenCookieName, token)
      val tokenHeader = RawHeader(CsrfTokenHeaderName, headerValue)
      val headers = List(cookie, tokenHeader)

      Post("/test").withHeaders(headers) ~> setupRoute(token, Some(subject), HttpMethods.POST) ~> check {
        responseAs[String] === SuccessResult
      }
    }

    "Fail when missing cookie param" in {
      // prepare requestcontext
      val headers = List()

      Post("/test").withHeaders(headers) ~> setupRoute(token, Some(subject), HttpMethods.POST) ~> check {
        status === StatusCodes.Unauthorized
        responseAs[String] === "Kein XSRF-Token im Cookie gefunden"
      }
    }.pendingUntilFixed("cookie is ignored for now")

    "Fail when missing header param" in {
      val time = DateTime.now.toString
      val headerValue = s"$token::$time"

      // prepare requestcontext
      val cookie = Cookie(CsrfTokenCookieName, token)
      val headers = List(cookie)

      Post("/test").withHeaders(headers) ~> setupRoute(token, Some(subject), HttpMethods.POST) ~> check {
        status === StatusCodes.Unauthorized
        responseAs[String] === "Kein XSRF-Token im Header gefunden"
      }
    }

    "Fail when header param does not contain correct time" in {
      val delay = 10 seconds
      val provider = new MockXSRFTokenSessionAuthenticatorProvider(Some(delay), system)
      val time = DateTime.now.toString
      val headerValue = s"$token::asdasdsd"

      // prepare requestcontext
      val cookie = Cookie(CsrfTokenCookieName, token)
      val tokenHeader = RawHeader(CsrfTokenHeaderName, headerValue)
      val headers = List(cookie, tokenHeader)

      Post("/test").withHeaders(headers) ~> setupRoute(token, Some(subject), HttpMethods.POST) ~> check {
        status === StatusCodes.Unauthorized
        responseAs[String] === "Ungültiges Datumsformat im Header:asdasdsd"
      }
    }

    "Fail when header token and cookie token mismatch" in {
      val delay = 10 seconds
      val provider = new MockXSRFTokenSessionAuthenticatorProvider(Some(delay), system)
      val time = DateTime.now.toString
      val headerValue = s"$token::$time"

      // prepare requestcontext
      val cookie = Cookie(CsrfTokenCookieName, "asasdfas")
      val tokenHeader = RawHeader(CsrfTokenHeaderName, headerValue)
      val headers = List(cookie, tokenHeader)

      Post("/test").withHeaders(headers) ~> setupRoute(token, Some(subject), HttpMethods.POST) ~> check {
        status === StatusCodes.Unauthorized
        responseAs[String] === s"Cookie und Header Token weichen voneinander ab 'asasdfas' != '$token'"
      }
    }.pendingUntilFixed("cookie is ignored for now")

    "Fail when delay exceeded" in {
      val delay = 1 milli
      val provider = new MockXSRFTokenSessionAuthenticatorProvider(Some(delay), system)
      val time = DateTime.now.toString
      val headerValue = s"$token::asdasdsd"

      // prepare requestcontext
      val cookie = Cookie(CsrfTokenCookieName, token)
      val tokenHeader = RawHeader(CsrfTokenHeaderName, headerValue)
      val headers = List(cookie, tokenHeader)

      Post("/test").withHeaders(headers) ~> setupRoute(token, Some(subject), HttpMethods.POST) ~> check {
        status === StatusCodes.Unauthorized
      }
    }

    "Fail when no person match token" in {
      val delay = 10 seconds
      val provider = new MockXSRFTokenSessionAuthenticatorProvider(Some(delay), system)
      val time = DateTime.now.toString
      val headerValue = s"$token::$time"

      // prepare requestcontext
      val cookie = Cookie(CsrfTokenCookieName, token)
      val tokenHeader = RawHeader(CsrfTokenHeaderName, headerValue)
      val headers = List(cookie, tokenHeader)

      Post("/test").withHeaders(headers) ~> setupRoute(token, None, HttpMethods.POST) ~> check {
        status === StatusCodes.Unauthorized
        responseAs[String] === s"Keine Person gefunden für token: $token"
      }
    }
  }
}

class MockXSRFTokenSessionAuthenticatorProvider(override val maxRequestDelay: Option[Duration], actorSystem: ActorSystem) extends XSRFTokenSessionAuthenticatorProvider {
  override val loginTokenCache: Cache[String, Subject] = LfuCache(actorSystem)
  override implicit protected val executionContext: ExecutionContext = actorSystem.dispatcher
}
