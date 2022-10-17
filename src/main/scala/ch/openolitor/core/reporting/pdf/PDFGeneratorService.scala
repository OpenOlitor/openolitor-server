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
package ch.openolitor.core.reporting.pdf

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.Http
import akka.stream.scaladsl.FileIO
import ch.openolitor.core.SystemConfig
import com.tegonal.CFEnvConfigLoader.ConfigLoader

import java.io.File
import java.nio.file.Files
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try

trait PDFGeneratorService {
  def sysConfig: SystemConfig
  lazy val DefaultChunkSize: Int = ConfigLoader.loadConfig.getBytes("akka.http.parsing.max-chunk-size").toInt

  lazy val endpointUri: String = sysConfig.mandantConfiguration.config.getString("converttopdf.endpoint")

  implicit val system: ActorSystem
  import system.dispatcher

  def generatePDF(input: File, name: String): Try[File] = synchronized {
    Try {
      val uri = Uri(endpointUri)
      val formData = Multipart.FormData(
        Multipart.FormData.BodyPart.fromFile("data", ContentTypes.`application/octet-stream`, input, DefaultChunkSize),
        Multipart.FormData.BodyPart.Strict("name", name + ".odt")
      )

      val result = Http().singleRequest(HttpRequest(HttpMethods.POST, uri, entity = formData.toEntity)).flatMap {
        case HttpResponse(StatusCodes.OK, _, entity, _) =>
          val file = Files.createTempFile("report_pdf", ".pdf")
          entity.dataBytes.runWith(FileIO.toPath(file)).map(_ => file.toFile)
        case other: Any =>
          throw new IllegalStateException(s"PDF konnte nicht generiert werden $other")
      }

      Await.result(result, 600 seconds)
    }
  }
}
