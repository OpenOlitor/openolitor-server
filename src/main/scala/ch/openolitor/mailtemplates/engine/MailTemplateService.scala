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
trait MailTemplateService extends SystemConfigReference with LazyLogging {

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
