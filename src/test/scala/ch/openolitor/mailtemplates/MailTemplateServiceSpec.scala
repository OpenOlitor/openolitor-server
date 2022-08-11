package ch.openolitor.mailtemplates

import org.specs2.mutable._
import org.specs2.mock.Mockito
import org.mockito.Matchers.{ eq => eqz }
import ch.openolitor.mailtemplates.repositories._
import ch.openolitor.mailtemplates.model._
import org.specs2.matcher._
import ch.openolitor.mailtemplates.engine.MailTemplateService
import org.joda.time.DateTime
import scala.util.Random
import ch.openolitor.core.models.PersonId
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{ Success }
import ch.openolitor.core.mailservice.MailPayload
import ch.openolitor.core.SystemConfig
import com.typesafe.config.ConfigFactory
import ch.openolitor.stammdaten.models._
import java.util.Locale
import scala.concurrent.duration._

class MailTemplateServiceSpec extends Specification with Mockito with Matchers with ResultMatchers {
  sequential =>

  val timeout: FiniteDuration = FiniteDuration(5, SECONDS)

  "MailTemplateService with custom template" should {

    case class RootObject(person: Person)
    case class Person(name: String, age: Int, birthdate: DateTime, addresses: Seq[Address]) extends Product
    case class Address(street: String)

    "parse template correctly" in {

      val person = Person("Mickey Mouse", 102, new DateTime(1980, 5, 22, 12, 11, 0), Seq(Address("street1"), Address("street2")))
      val rootObject = RootObject(person)

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
        templateType = CustomMailTemplateType,
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

      val result = service.generateMail(templateSubject, templateBody, rootObject)
      result must be_==(Success(MailPayload(resultSubject, resultBody)))
    }
  }

  "MailTemplateService with default templates" should {

    implicit val person = PersonId(0)

    val sampleEinladungsMailContext = EinladungMailContext(
      person = Person.build(
        anrede = Some(Herr),
        name = "Muster",
        vorname = "Hans",
        email = Some("hans.muster@email.com")
      ),
      einladung = Einladung.build(
        uid = "12345",
        expires = DateTime.now.plusMonths(1)
      ),
      baseLink = "http://my.openolitor.ch"
    )

    val sampleBestellung = SammelbestellungMailContext(
      Sammelbestellung(SammelbestellungId(0), ProduzentId(0), "", LieferplanungId(0), Offen,
        new DateTime(2017, 1, 15, 0, 0, 0), None, None, BigDecimal(10), None, BigDecimal(10),
        BigDecimal(10), new DateTime(2017, 1, 15, 0, 0, 0), PersonId(0),
        new DateTime(2017, 1, 15, 0, 0, 0), PersonId(0)),
      Projekt(ProjektId(0), "my project", None, None, None, None, None, false, false, false, CHF, 1, 1,
        Map(Rolle("AdministratorZugang").get -> false, Rolle("KundenZugang").get -> false), EmailSecondFactorType,
        Locale.GERMAN, None, None, None, false, false, Stunden, 3, true, false, new DateTime(2017, 1, 15, 0, 0, 0),
        PersonId(0), new DateTime(2017, 1, 15, 0, 0, 0), PersonId(0)),
      Produzent(ProduzentId(0), "TestProduzent", Some("Hans"), "PRZ", None, None, None, "1234", "Bern", None,
        "info@produzent.ch", None, None, None, None, false, None, None, false,
        new DateTime(2017, 1, 15, 0, 0, 0), PersonId(0), new DateTime(2017, 1, 15, 0, 0, 0), PersonId(0)),
      Seq(BestellungMail(
        BestellungId(0), SammelbestellungId(0), BigDecimal(11), Some(BigDecimal(2.1)), BigDecimal(2.35),
        BigDecimal(3), BigDecimal(15),
        Seq(
          BestellpositionMail(BestellpositionId(0), BestellungId(0), None, "Produkt1", Some(BigDecimal(1.4)),
            Kilogramm, BigDecimal(1), Some(BigDecimal(5.2)), 3,
            new DateTime(2017, 1, 15, 0, 0, 0), PersonId(0), new DateTime(2017, 1, 15, 0, 0, 0), PersonId(0)),
          BestellpositionMail(BestellpositionId(1), BestellungId(0), None, "Produkt2", None, Stueck, BigDecimal(2),
            None, 5, new DateTime(2017, 1, 15, 0, 0, 0), PersonId(0), new DateTime(2017, 1, 15, 0, 0, 0), PersonId(0))
        ),
        BigDecimal(1), BigDecimal(10),
        new DateTime(2017, 1, 15, 0, 0, 0), PersonId(0), new DateTime(2017, 1, 15, 0, 0, 0), PersonId(0)
      ))
    )
    "parse template correctly" in {
      //This email is the default mail for invitation in db
      val templateBody =
        """{{ person.anrede }} {{ person.vorname }} {{person.name }},

  Aktivieren Sie Ihren Zugang mit folgendem Link: {{ baseLink }}?token={{ einladung.uid }}"""

      val resultBody =
        """Herr Hans Muster,

  Aktivieren Sie Ihren Zugang mit folgendem Link: http://my.openolitor.ch?token=12345"""

      //this is the default subject for invitation in db
      val templateSubject = "Invitation Mail"
      val resultSubject = "Invitation Mail"

      val service = new MailTemplateServiceMock()

      val result = service.generateMail(templateSubject, templateBody, sampleEinladungsMailContext)
      result must be_==(Success(MailPayload(resultSubject, resultBody)))
    }

    "parse PasswordResetMail correctly" in {

      val templateBody =
        """{{ person.anrede }} {{ person.vorname }} {{person.name }},

  Sie können Ihr Passwort mit folgendem Link neu setzten: {{ baseLink }}?token={{ einladung.uid }}"""
      val resultBody = """Herr Hans Muster,

  Sie können Ihr Passwort mit folgendem Link neu setzten: http://my.openolitor.ch?token=12345"""
      val templateSubject = "Password Reset Mail"
      val resultSubject = "Password Reset Mail"

      val service = new MailTemplateServiceMock()

      val result = service.generateMail(templateSubject, templateBody, sampleEinladungsMailContext)
      result must be_==(Success(MailPayload(resultSubject, resultBody)))
    }

    "parse ProduzentenBestellungMail correctly" in {

      val templateBody = """Bestellung von {{ projekt.bezeichnung }} an {{produzent.name}} {{ produzent.vorname }}: Lieferung: {{ sammelbestellung.datum | date format="dd.MM.yyyy" }}"""

      val resultBody = """Bestellung von my project an TestProduzent Hans: Lieferung: 15.01.2017"""

      val templateSubject = "Produzenten Bestellung Mail"
      val resultSubject = "Produzenten Bestellung Mail"

      val service = new MailTemplateServiceMock()

      val result = service.generateMail(templateSubject, templateBody, sampleBestellung)
      result must be_==(Success(MailPayload(resultSubject, resultBody)))
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
