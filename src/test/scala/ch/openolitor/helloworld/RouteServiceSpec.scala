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
package ch.openolitor.helloworld

import org.apache.pekko.actor.ActorRef
import org.apache.pekko.http.scaladsl.model.StatusCodes.{ MethodNotAllowed, NotFound }
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.http.scaladsl.testkit.Specs2RouteTest
import ch.openolitor.core.filestore.FileStore
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import spray.json._

import scala.concurrent.ExecutionContext

class RouteServiceSpec(implicit ec: ExecutionEnv) extends Specification with Specs2RouteTest {
  import HelloWorldJsonProtocol._

  private val service = new HelloWorldRoutes {
    override val fileStore: FileStore = null
    override implicit protected val executionContext: ExecutionContext = ec.executionContext
    override val entityStore: ActorRef = null
  }

  "HelloWorldService" should {
    "return a greeting for GET requests to the root path as json" in {
      Get("/hello/json") ~> service.helloWorldRoute ~> check {
        responseAs[String].parseJson.convertTo[HelloWorld] must beEqualTo(HelloWorld("Hello World!"))
      }
    }

    "leave GET requests to other paths unhandled" in {
      Get("/kermit") ~> service.helloWorldRoute ~> check {
        handled must beFalse
      }
    }

    "return a MethodNotAllowed error for PUT requests to the root path" in {
      Put("/hello/xml") ~> Route.seal(service.helloWorldRoute) ~> check {
        status === NotFound
      }
    }
  }
}
