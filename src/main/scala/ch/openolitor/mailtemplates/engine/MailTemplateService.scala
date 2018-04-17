package ch.openolitor.mailtemplates.engine

import scala.concurrent._
import scala.util.Try
import ch.openolitor.mailtemplates.repositories._
import ch.openolitor.mailtemplates.model._
import ch.openolitor.core.mailservice.MailPayload
import ch.openolitor.core.db._
import ch.openolitor.core._
import java.util.Locale
import ch.openolitor.util.ProductUtil._
import ch.openolitor.util.DateTimeUtil._
import de.zalando.beard.renderer._
import ch.openolitor.core.filestore._
import org.joda.time.format.DateTimeFormat
import org.joda.time.DateTime

/**
 * This trait provides functionality to generate mail payloads based on either custom or a default template
 */
trait MailTemplateService extends AsyncConnectionPoolContextAware with SystemConfigReference {
  self: MailTemplateReadRepositoryComponent =>

  val format = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ssZ")
  /**
   * Type converter is used to convert single values when generating the map out of the provided data object
   */
  val typeConverter: PartialFunction[Any, Any] = {
    case x: DateTime => format.print(x)
  }

  /**
   * Generate emails for a list of contexts.
   * @param mailTemplateType type of mailtemplate to generate mails for, mainly used to determine default template
   * @param templateName name of the mail template to generate mails for. If template cannot get resolved, default template will be used
   * @param contexts list of contexts to use when rendering mails using beard template engine
   */
  def generateMail[P <: Product](mailTemplateType: MailTemplateType, templateName: Option[String], context: P)(implicit ec: ExecutionContext): Future[Try[MailPayload]] = {
    generateMails(mailTemplateType, templateName, Seq(context)) map (_.map(_.head))
  }

  /**
   * Generate emails for a list of contexts.
   * @param mailTemplateType type of mailtemplate to generate mails for, mainly used to determine default template
   * @param templateName name of the mail template to generate mails for. If template cannot get resolved, default template will be used
   * @param contexts list of contexts to use when rendering mails using beard template engine
   */
  def generateMails[P <: Product](mailTemplateType: MailTemplateType, templateName: Option[String], contexts: Seq[P])(implicit ec: ExecutionContext): Future[Try[Seq[MailPayload]]] = {
    // first try to resolve mail template
    templateName.map(name => mailTemplateReadRepositoryAsync.getMailTemplateByName(name)).getOrElse(Future.successful(None)).map { templateOption =>

      val (subjectTemplate, bodyTemplate) = templateOption.map(t => (t.subject, t.body)).getOrElse((mailTemplateType.defaultSubject, mailTemplateType.defaultBody))

      // prepare renderer and compiler for the whole run
      val subjectCompiler = new CustomizableTemplateCompiler(templateLoader =
        new MapTemplateLoader(Map("Subject" -> subjectTemplate)))
      val subjectRenderer = new BeardTemplateRenderer(subjectCompiler)

      val bodyCompiler = new CustomizableTemplateCompiler(templateLoader =
        new MapTemplateLoader(Map("Body" -> bodyTemplate)))
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
}
