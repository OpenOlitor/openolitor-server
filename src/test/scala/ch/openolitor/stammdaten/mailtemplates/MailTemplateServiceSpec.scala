package ch.openolitor.stammdaten.mailtemplates

import org.specs2.mutable._
import org.specs2.mock.Mockito
import org.mockito.Matchers.{ eq => eqz, _ }
import ch.openolitor.stammdaten.mailtemplates.repositories._
import ch.openolitor.stammdaten.mailtemplates.model._
import org.specs2.matcher._
import ch.openolitor.core.filestore.FileStore
import ch.openolitor.stammdaten.mailtemplates.engine.MailTemplateService
import org.joda.time.DateTime
import scala.util.Random
import ch.openolitor.core.models.PersonId
import ch.openolitor.core.filestore.MailTemplateBucket
import scalikejdbc.DBSession
import scalikejdbc.ConnectionPoolContext
import scala.concurrent.Future
import com.amazonaws.util.StringInputStream
import ch.openolitor.core.filestore.FileStoreFile
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{ Failure, Success }
import ch.openolitor.core.mailservice.MailPayload
import ch.openolitor.core.SystemConfig
import ch.openolitor.core.db.MultipleAsyncConnectionPoolContext
import com.typesafe.config.ConfigFactory

class MailTemplateServiceSpec extends Specification with Mockito with Matchers with ResultMatchers {
  "MailTemplateService with custom template" should {

    case class Person(name: String, age: Int, birthdate: DateTime, addresses: Seq[Address]) extends Product
    case class Address(street: String)

    "parse template correclty" in {

      val person = Person("Mickey Mouse", 102, new DateTime(1980, 5, 22, 12, 11, 0), Seq(Address("street1"), Address("street2")))

      val templateBody = """
        Person: {{ person.name }}
        Birthdate: {{ person.birthdate | date format="dd.MM.yyyy" }}
        Age: {{ person.age }}        
        Addresses:
        ----------
        {{for address in person.addresses}}
        Street: {{ address.street}}
        {{/for}}
        """
      val resultBody = """
        Person: Mickey Mouse
        Birthdate: 22.05.1980
        Age: 102        
        Addresses:
        ----------
        Street: street1
        Street: street2
        """
      val templateSubject = """Person detail: {{ person.name }}"""
      val resultSubject = """Person detail: Mickey Mouse"""

      val mailTemplate = MailTemplate(
        id = MailTemplateId(Random.nextLong()),
        templateType = UnknownMailTemplateType,
        templateName = "templateName",
        description = None,
        subject = templateSubject,
        body = templateBody,
        erstelldat = DateTime.now(),
        ersteller = PersonId(1L),
        modifidat = DateTime.now(),
        modifikator = PersonId(1L)
      )

      val service = new MailTemplateServiceMock()
      service.mailTemplateReadRepositoryAsync.getMailTemplateByName(eqz("templateName"))(any) returns Future.successful(Some(mailTemplate))

      val result = service.generateMail(UnknownMailTemplateType, Some("templateName"), person)
      result must be_==(Success(MailPayload(resultSubject, resultBody))).await
    }
  }
}

class MailTemplateServiceMock extends MailTemplateService with Mockito with MailTemplateReadRepositoryComponent {
  val mailTemplateWriteRepository: MailTemplateWriteRepository = mock[MailTemplateWriteRepository]
  val mailTemplateReadRepositoryAsync: MailTemplateReadRepositoryAsync = mock[MailTemplateReadRepositoryAsync]
  val mailTemplateReadRepositorySync: MailTemplateReadRepositorySync = mock[MailTemplateReadRepositorySync]
  val sysConfig: SystemConfig = mock[SystemConfig]

  override lazy val config = ConfigFactory.parseString("""mailtemplates.max-file-store-resolve-timeout=1.day""")
}