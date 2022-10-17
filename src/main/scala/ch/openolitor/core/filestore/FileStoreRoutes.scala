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
package ch.openolitor.core.filestore

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import ch.openolitor.core._
import com.typesafe.scalalogging.LazyLogging

import scala.util._

trait FileStoreRoutes extends BaseRouteService with ActorReferences with AkkaHttpDeserializers with LazyLogging {
  self: FileStoreComponent =>

  lazy val fileStoreRoute =
    pathPrefix("filestore") {
      fileStoreFileTypeRoute
    }

  lazy val fileStoreFileTypeRoute: Route =
    pathPrefix(Segment) { fileTypeName =>
      val fileType = FileType(fileTypeName)
      pathEnd {
        get {
          onSuccess(fileStore.getFileIds(fileType.bucket)) {
            case Left(e)     => complete(StatusCodes.InternalServerError, s"Could not list objects for the given fileType: ${fileType}")
            case Right(list) => complete(s"Result list: $list")
          }
        }
      } ~
        path(Segment) { id =>
          get {
            extractRequestContext { requestContext =>
              implicit val materializer: Materializer = requestContext.materializer
              onSuccess(fileStore.getFile(fileType.bucket, id)) {
                case Left(e) => complete(StatusCodes.NotFound, s"File of file type ${fileType} with id ${id} was not found. Error: ${e}")
                case Right(file) =>
                  logger.debug(s"serving file: $file")
                  stream(file.file)
              }
            }
          }
        }
    }
}