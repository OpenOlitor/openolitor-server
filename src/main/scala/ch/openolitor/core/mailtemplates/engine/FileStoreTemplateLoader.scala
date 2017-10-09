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
package ch.openolitor.core.mailtemplates.engine

import de.zalando.beard.renderer._
import scala.io.Source
import scala.concurrent.duration._
import scala.concurrent._
import ch.openolitor.core.filestore._
import com.typesafe.scalalogging.LazyLogging

/**
 * TemplateLoader backed by a filestore storage service. The template loader gets intialized with a filestorebucket to resolve templates from
 */
class FileStoreTemplateLoader(fileStore: FileStore, bucket: FileStoreBucket, maxAwaitTime: Duration)(implicit ec: ExecutionContext) extends TemplateLoader with LazyLogging {

  override def load(templateName: TemplateName): Option[Source] = {
    Await.result(fileStore.getFile(bucket, templateName.name).map(_ match {
      case Right(FileStoreFile(_, is)) => Option(Source.fromInputStream(is))
      case Left(x) =>
        logger.warn(s"Could not resolve template from filestore. TemplateName:${templateName.name}, result:$x")
        None
    }), maxAwaitTime)
  }
}
