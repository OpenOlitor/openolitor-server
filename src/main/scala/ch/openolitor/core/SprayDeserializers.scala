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
package ch.openolitor.core

import akka.http.scaladsl.server.{ PathMatcher1, PathMatchers }
import akka.http.scaladsl.unmarshalling.{ FromStringUnmarshaller, Unmarshaller }
import akka.http.scaladsl.util.FastFuture
import akka.stream.Materializer
import ch.openolitor.core.models.BaseId

import scala.concurrent.{ ExecutionContext, Future }

trait SprayDeserializers {
  implicit val stringToBooleanConverter = new FromStringUnmarshaller[Boolean] {
    override def apply(value: String)(implicit ec: ExecutionContext, materializer: Materializer): Future[Boolean] = {
      value.toLowerCase match {
        case "true" | "yes" | "on"  => FastFuture.successful(true)
        case "false" | "no" | "off" => FastFuture.successful(false)
        case x                      => FastFuture.failed(new IllegalArgumentException("'" + x + "' is not a valid Boolean value"))
      }
    }
  }

  def long2BaseIdPathMatcher[T <: BaseId](implicit f: Long => T): PathMatcher1[T] = {
    PathMatchers.LongNumber.flatMap(id => Some(f(id)))
  }

  def enumPathMatcher[T](implicit f: String => Option[T]): PathMatcher1[T] = {
    PathMatchers.Segment.flatMap(id => f(id))
  }

  def longToBaseIdConverter[T <: BaseId](implicit f: Long => T) = new Unmarshaller[Long, T] {
    override def apply(value: Long)(implicit ec: ExecutionContext, materializer: Materializer): Future[T] =
      try {
        FastFuture.successful(f(value))
      } catch {
        case e: Exception =>
          FastFuture.failed(new IllegalArgumentException(s"'$value' is not a valid id:$e", e))
      }
  }
}
