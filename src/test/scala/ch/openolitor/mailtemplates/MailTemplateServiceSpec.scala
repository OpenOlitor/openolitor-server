package ch.openolitor.mailtemplates

import org.specs2.mutable._
import org.specs2.mock.Mockito
import org.mockito.Matchers.{ eq => eqz, _ }
import ch.openolitor.mailtemplates.repositories._
import ch.openolitor.mailtemplates.model._
import org.specs2.matcher._
import ch.openolitor.core.filestore.FileStore
import ch.openolitor.mailtemplates.engine.MailTemplateService
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

      val result = service.generateMail(templateSubject, templateBody, rootObject)
      result must be_==(Success(MailPayload(resultSubject, resultBody)))
    }
  }

  //  "MailTemplateService with default templates" should {
  //
  //    implicit val person = PersonId(0)
  //
  //    val sampleEinladungsMailContext = EinladungMailContext(
  //      person = Person.build(
  //        anrede = Some(Herr),
  //        name = "Muster",
  //        vorname = "Hans",
  //        email = Some("hans.muster@email.com")
  //      ),
  //      einladung = Einladung.build(
  //        uid = "12345",
  //        expires = DateTime.now.plusMonths(1)
  //      ),
  //      baseLink = "http://my.openolitor.ch"
  //    )

  //    val sampleBestellung = SammelbestellungMail.build(
  //      produzentKurzzeichen = "PRZ",
  //      status = Offen,
  //      datum = new DateTime(2017, 1, 15, 0, 0, 0),
  //      preisTotal = BigDecimal(101),
  //      steuerSatz = Some(BigDecimal(2)),
  //      steuer = BigDecimal(20),
  //      totalSteuer = BigDecimal(30),
  //      bestellungen = Seq(
  //        BestellungMail.build(
  //          // Summe der Preise der Bestellpositionen
  //          preisTotal = BigDecimal(11),
  //          steuerSatz = Some(BigDecimal(2.1)),
  //          // Berechnete Steuer nach Abzug (adminProzenteAbzug)
  //          steuer = BigDecimal(2.35),
  //          totalSteuer = BigDecimal(3),
  //          adminProzente = BigDecimal(15),
  //          bestellpositionen = Seq(
  //            BestellpositionMail.build(
  //              produktBeschrieb = "Produkt1",
  //              preisEinheit = Some(BigDecimal(1.4)),
  //              einheit = Kilogramm,
  //              menge = BigDecimal(1),
  //              preis = Some(BigDecimal(5.2)),
  //              anzahl = 3
  //            ),
  //            BestellpositionMail.build(
  //              produktBeschrieb = "Produkt2",
  //              preisEinheit = None,
  //              einheit = Stueck,
  //              menge = BigDecimal(2),
  //              preis = None,
  //              anzahl = 5
  //            )
  //          ),
  //          // Berechneter Abzug auf preisTotal
  //          adminProzenteAbzug = BigDecimal(1),
  //          totalNachAbzugAdminProzente = BigDecimal(10)
  //        )
  //      ),
  //      projekt = Projekt.build(
  //        bezeichnung = "TestProjekt"
  //      ),
  //      produzent = Produzent.build(
  //        name = "TestProduzent",
  //        vorname = Some("Hans"),
  //        kurzzeichen = "PRZ",
  //        plz = "1234",
  //        ort = "Bern",
  //        email = "info@produzent.ch"
  //      )
  //    )
  //
  //    "parse InvitationMail correctly" in {
  //
  //      val resultBody = """Herr Hans Muster,
  //
  //Aktivieren Sie Ihren Zugang mit folgendem Link: http://my.openolitor.ch?token=12345"""
  //      val resultSubject = InvitationMailTemplateType.defaultSubject
  //
  //      val service = new MailTemplateServiceMock()
  //
  //      //val result = service.generateMail(templateSubject, templateBody, sampleEinladungsMailContext)
  //      //val result = service.generateMail(InvitationMailTemplateType, None, sampleEinladungsMailContext)
  //      //result must be_==(Success(MailPayload(resultSubject, resultBody))).await(0, timeout)
  //    }
  //
  //    "parse PasswordResetMail correctly" in {
  //
  //      val resultBody = """Herr Hans Muster,
  //
  //Sie können Ihr Passwort mit folgendem Link neu setzten: http://my.openolitor.ch?token=12345"""
  //      val resultSubject = PasswordResetMailTemplateType.defaultSubject
  //
  //      val service = new MailTemplateServiceMock()
  //
  //      //val result = service.generateMail(PasswordResetMailTemplateType, None, sampleEinladungsMailContext)
  //      //result must be_==(Success(MailPayload(resultSubject, resultBody))).await(0, timeout)
  //    }
  //
  //    "parse ProduzentenBestellungMail correctly" in {
  //
  //      val resultBody = """Bestellung von TestProjekt an TestProduzent Hans:
  //
  //Lieferung: 15.01.2017
  //
  //Bestellpositionen:
  //
  //Adminprozente: 15%:
  //
  //Produkt1: 3 x 1 Kilogramm à 1.4 = 5.2 CHF ⇒ 3 Kilogramm
  //Produkt2: 5 x 2 Stueck à  =  CHF ⇒ 10 Stueck
  //
  //Summe [CHF]: 101.00"""
  //      val resultSubject = "Bestellung 15.01.2017"
  //
  //      val service = new MailTemplateServiceMock()
  //
  //      //val result = service.generateMail(ProduzentenBestellungMailTemplateType, None, sampleBestellung)
  //      //result must be_==(Success(MailPayload(resultSubject, resultBody))).await(0, timeout)
  //    }
  //  }
}

class MailTemplateServiceMock extends MailTemplateService with Mockito with MailTemplateReadRepositoryComponent {
  val mailTemplateWriteRepository: MailTemplateWriteRepository = mock[MailTemplateWriteRepository]
  val mailTemplateReadRepositoryAsync: MailTemplateReadRepositoryAsync = mock[MailTemplateReadRepositoryAsync]
  val mailTemplateReadRepositorySync: MailTemplateReadRepositorySync = mock[MailTemplateReadRepositorySync]
  val sysConfig: SystemConfig = mock[SystemConfig]

  override lazy val config = ConfigFactory.parseString("""mailtemplates.max-file-store-resolve-timeout=1.day""")
}