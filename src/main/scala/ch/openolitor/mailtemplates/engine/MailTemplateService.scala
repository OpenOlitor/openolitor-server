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
package ch.openolitor.mailtemplates.engine

import scala.concurrent._
import scala.util.Try
import ch.openolitor.core.mailservice.MailPayload
import ch.openolitor.core._

import ch.openolitor.util.ProductUtil._
import de.zalando.beard.renderer._
import com.typesafe.scalalogging.LazyLogging
import org.joda.time.format.DateTimeFormat
import org.joda.time.DateTime

/**
 * This trait provides functionality to generate mail payloads based on either custom or a default template
 */
trait MailTemplateService extends LazyLogging {

  val format = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ssZ")
  /**
   * Type converter is used to convert single values when generating the map out of the provided data object
   */
  val typeConverter: PartialFunction[Any, Any] = {
    case x: DateTime => format.print(x)
  }

  /**
   * Generate emails for a list of contexts.
   * @param contexts list of contexts to use when rendering mails using beard template engine
   */
  def generateMail[P <: Product](subject: String, body: String, context: P)(implicit ec: ExecutionContext): Try[MailPayload] = {
    generateMails(subject, body, Seq(context)) map (_.head)
  }

  /**
   * Generate emails for a list of contexts.
   * @param context list of contexts to use when rendering mails using beard template engine
   */
  def generateMails[P <: Product](subject: String, body: String, contexts: Seq[P])(implicit ec: ExecutionContext): Try[Seq[MailPayload]] = {
    // first try to resolve mail template
    val (subjectTemplate, bodyTemplate) = (subject, body)
    // prepare renderer and compiler for the whole run
    val subjectCompiler = new CustomizableTemplateCompiler(templateLoader = new MapTemplateLoader(Map("Subject" -> subjectTemplate)))
    val subjectRenderer = new BeardTemplateRenderer(subjectCompiler)

    val bodyCompiler = new CustomizableTemplateCompiler(templateLoader = new MapTemplateLoader(Map("Body" -> bodyTemplate)))
    val bodyRenderer = new BeardTemplateRenderer(bodyCompiler)

    for {
      subjectTempl <- subjectCompiler.compile(TemplateName("Subject"))
      bodyTempl <- bodyCompiler.compile(TemplateName("Body"))
    } yield {

      //render mails for every context

      contexts.map { context =>
        val contextMap = context.toMap(typeConverter)

        val subjectResult = subjectRenderer.render(
          subjectTempl,
          StringWriterRenderResult(),
          contextMap
        )

        val bodyResult = bodyRenderer.render(
          bodyTempl,
          StringWriterRenderResult(),
          contextMap
        )

        MailPayload(subjectResult.toString, bodyResult.toString)
      }
    }
  }
}
