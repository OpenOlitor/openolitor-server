package ch.openolitor.stammdaten.mailtemplates.engine

import scala.concurrent._
import scala.util.Try
import ch.openolitor.stammdaten.mailtemplates.repositories._
import ch.openolitor.stammdaten.mailtemplates.model._
import ch.openolitor.core.mailservice.MailPayload
import ch.openolitor.core.db._
import ch.openolitor.core._
import java.util.Locale
import ch.openolitor.util.ProductUtil._
import ch.openolitor.util.DateTimeUtil._
import de.zalando.beard.renderer._
import ch.openolitor.core.filestore._

/**
 * This trait provides functionality to generate mail payloads based on either custom or a default template
 */
trait MailTemplateService extends AsyncConnectionPoolContextAware with FileStoreReference with SystemConfigReference {
  self: MailTemplateReadRepositoryComponent =>

  lazy val maxFileStoreResolveTimeout = config.getDuration(s"mailtemplates.max-file-store-resolve-timeout")

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

      val (subjectTemplate, bodyTemplateNameOpt) = templateOption.map(t => (t.subject, t.bodyFileStoreId)).getOrElse((mailTemplateType.defaultSubject, None))
      val bodyTemplateName = bodyTemplateNameOpt.getOrElse(mailTemplateType.defaultMailBodyTemplateName)

      // prepare renderer and compiler for the whole run
      val subjectCompiler = new CustomizableTemplateCompiler(templateLoader =
        new MapTemplateLoader(Map("Subject" -> subjectTemplate)))
      val subjectRenderer = new BeardTemplateRenderer(subjectCompiler)

      val bodyCompiler = new CustomizableTemplateCompiler(templateLoader =
        new FallbackTemplateLoader(
          // primary load templates from filestore
          new FileStoreTemplateLoader(fileStore, MailTemplateBucket, maxFileStoreResolveTimeout),
          // fallback to classpath resources to resolve default templates
          new ClasspathTemplateLoader("mailtemplates/", ".txt")
        ))
      val bodyRenderer = new BeardTemplateRenderer(bodyCompiler)

      for {
        subjectTempl <- subjectCompiler.compile(TemplateName("Subject"))
        bodyTempl <- bodyCompiler.compile(TemplateName(bodyTemplateName))
      } yield {

        //render mails for every context          
        contexts.map { context =>
          val contextMap = context.toMap

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