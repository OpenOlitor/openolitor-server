package ch.openolitor.core.templates.engine

import scala.concurrent._
import scala.util.Try
import ch.openolitor.core.templates.repositories._
import ch.openolitor.core.mailservice.MailPayload
import ch.openolitor.core.templates.model.MailTemplateType
import java.util.Locale
import ch.openolitor.core.db.AsyncConnectionPoolContextAware
import ch.openolitor.util.ProductUtil._
import ch.openolitor.core.templates.model.MailTemplatePayload
import de.zalando.beard.renderer._

/**
 * This trait provides functionality to generate mail payloads based on either custom or a default template
 */
trait MailTemplateService extends AsyncConnectionPoolContextAware {
  self: TemplateReadRepositoryComponent =>

  /**
   * Generate emails for a list of contexts.
   * @param mailTemplateType type of mailtemplate to generate mails for, mainly used to determine default template
   * @param templateName name of the mail template to generate mails for. If template cannot get resolved, default template will be used
   * @param contexts list of contexts to use when rendering mails using beard template engine
   */
  def generateMail[P <: Product](mailTemplateType: MailTemplateType, templateName: String, context: P)(implicit ec: ExecutionContext): Future[Try[MailPayload]] = {
    generateMails(mailTemplateType, templateName, Seq(context)) map (_.map(_.head))
  }

  /**
   * Generate emails for a list of contexts.
   * @param mailTemplateType type of mailtemplate to generate mails for, mainly used to determine default template
   * @param templateName name of the mail template to generate mails for. If template cannot get resolved, default template will be used
   * @param contexts list of contexts to use when rendering mails using beard template engine
   */
  def generateMails[P <: Product](mailTemplateType: MailTemplateType, templateName: String, contexts: Seq[P])(implicit ec: ExecutionContext): Future[Try[Seq[MailPayload]]] = {
    // first try to resolve mail template
    templateReadRepositoryAsync.getMailTemplateByName(templateName).map {
      _.map { tmpl =>
        MailTemplatePayload(tmpl.subject, tmpl.body)
      } getOrElse {
        mailTemplateType.defaultMailTemplate
      }
    }
      // compile templates
      .map { template =>
        val loader = new DatabaseTemplateLoader(
          template = template,
          readRepository = templateReadRepositorySync,
          sysConfig = sysConfig
        )
        val templateCompiler = new CustomizableTemplateCompiler(templateLoader = loader)
        val renderer = new BeardTemplateRenderer(templateCompiler)

        for {
          subjectTempl <- templateCompiler.compile(TemplateName("Mail/Subject"))
          bodyTempl <- templateCompiler.compile(TemplateName("Mail/Body"))
        } yield {

          //render mails for every context          
          contexts.map { context =>
            val contextMap = context.toMap

            val subjectResult = renderer.render(
              subjectTempl,
              StringWriterRenderResult(),
              contextMap
            )

            val bodyResult = renderer.render(
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