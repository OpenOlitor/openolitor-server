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
package ch.openolitor.stammdaten

import akka.actor.{ ActorRef, ActorSystem }
import akka.pattern.ask
import akka.util.Timeout
import ch.openolitor.core.Macros._
import ch.openolitor.core._
import ch.openolitor.core.db._
import ch.openolitor.core.domain._
import ch.openolitor.core.mailservice.MailService._
import ch.openolitor.core.models.PersonId
import ch.openolitor.mailtemplates.model._
import ch.openolitor.mailtemplates.repositories._
import ch.openolitor.stammdaten.StammdatenCommandHandler._
import ch.openolitor.stammdaten.eventsourcing.StammdatenEventStoreSerializer
import ch.openolitor.stammdaten.models._
import ch.openolitor.stammdaten.repositories._
import com.typesafe.scalalogging.LazyLogging
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import ch.openolitor.util.ConfigUtil._
import scalikejdbc.DBSession
import ch.openolitor.core.repositories.EventPublishingImplicits._
import ch.openolitor.core.repositories.EventPublisher
import scala.concurrent.ExecutionContext.Implicits._
import scalikejdbc.DB

import scala.concurrent.duration._
import scala.util.{ Failure, Success }

object StammdatenAktionenService {
  def apply(implicit sysConfig: SystemConfig, system: ActorSystem, mailService: ActorRef): StammdatenAktionenService = new DefaultStammdatenAktionenService(sysConfig, system, mailService)
}

class DefaultStammdatenAktionenService(sysConfig: SystemConfig, override val system: ActorSystem, override val mailService: ActorRef)
  extends StammdatenAktionenService(sysConfig, mailService) with DefaultStammdatenWriteRepositoryComponent with DefaultMailTemplateReadRepositoryComponent {
}

/**
 * Actor zum Verarbeiten der Aktionen für das Stammdaten Modul
 */
abstract class StammdatenAktionenService(override val sysConfig: SystemConfig, override val mailService: ActorRef) extends EventService[PersistentEvent]
  with LazyLogging
  with AsyncConnectionPoolContextAware
  with StammdatenDBMappings
  with MailServiceReference
  with StammdatenEventStoreSerializer
  with SammelbestellungenHandler
  with LieferungHandler
  with SystemConfigReference
  with EmailHandler {
  self: StammdatenWriteRepositoryComponent with MailTemplateReadRepositoryComponent =>

  // implicitly expose the eventStream
  implicit lazy val stammdatenRepositoryImplicit = stammdatenWriteRepository

  implicit val timeout = Timeout(15.seconds) //sending mails might take a little longer

  lazy val BaseZugangLink = config.getStringOption(s"security.zugang-base-url").getOrElse("")
  lazy val BasePasswortResetLink = config.getStringOption(s"security.passwort-reset-base-url").getOrElse("")

  val handle: Handle = {
    case LieferplanungAbschliessenEvent(meta, id: LieferplanungId) =>
      lieferplanungAbschliessen(meta, id)
    case LieferplanungAbrechnenEvent(meta, id: LieferplanungId) =>
      lieferplanungVerrechnet(meta, id)
    case LieferplanungDataModifiedEvent(meta, result: LieferplanungDataModify) =>
      lieferplanungDataModified(meta, result)
    case SammelbestellungVersendenEvent(meta, id: SammelbestellungId) =>
      sammelbestellungVersenden(meta, id)
    case AuslieferungAlsAusgeliefertMarkierenEvent(meta, id: AuslieferungId) =>
      auslieferungAusgeliefert(meta, id)
    case SammelbestellungAlsAbgerechnetMarkierenEvent(meta, datum, id: SammelbestellungId) =>
      sammelbestellungAbgerechnet(meta, datum, id)
    case PasswortGewechseltEvent(meta, personId, pwd, einladungId) =>
      updatePasswort(meta, personId, pwd, einladungId)
    case LoginDeaktiviertEvent(meta, _, personId) =>
      disableLogin(meta, personId)
    case LoginAktiviertEvent(meta, _, personId) =>
      enableLogin(meta, personId)
    case EinladungGesendetEvent(meta, einladung) =>
      sendEinladung(meta, einladung)
    case PasswortResetGesendetEvent(meta, einladung) =>
      sendPasswortReset(meta, einladung)
    case RolleGewechseltEvent(meta, _, personId, rolle) =>
      changeRolle(meta, personId, rolle)
    case SendEmailToPersonEvent(meta, subject, body, person, context) =>
      sendEmail(meta, subject, body, person, context, mailService)
    case SendEmailToKundeEvent(meta, subject, body, person, context) =>
      sendEmail(meta, subject, body, person, context, mailService)
    case SendEmailToAbotypSubscriberEvent(meta, subject, body, person, context) =>
      sendEmail(meta, subject, body, person, context, mailService)
    case SendEmailToZusatzabotypSubscriberEvent(meta, subject, body, person, context) =>
      sendEmail(meta, subject, body, person, context, mailService)
    case SendEmailToTourSubscriberEvent(meta, subject, body, person, context) =>
      sendEmail(meta, subject, body, person, context, mailService)
    case SendEmailToDepotSubscriberEvent(meta, subject, body, person, context) =>
      sendEmail(meta, subject, body, person, context, mailService)
    case SendEmailToAboSubscriberEvent(meta, subject, body, person, context) =>
      sendEmail(meta, subject, body, person, context, mailService)
    case UpdateKundeEvent(meta, kundeId, kunde) =>
      updateKunde(meta, kundeId, kunde)
    case e =>
      logger.warn(s"Unknown event:$e")
  }

  def lieferplanungAbschliessen(meta: EventMetadata, id: LieferplanungId)(implicit personId: PersonId = meta.originator) = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.updateEntityIf[Lieferplanung, LieferplanungId](Offen == _.status)(id)(lieferplanungMapping.column.status -> Abgeschlossen)
    }
  }

  def lieferplanungVerrechnet(meta: EventMetadata, id: LieferplanungId)(implicit personId: PersonId = meta.originator) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.updateEntityIf[Lieferplanung, LieferplanungId](Abgeschlossen == _.status)(id) {
        lieferplanungMapping.column.status -> Verrechnet
      }

      stammdatenWriteRepository.getLieferungen(id) map { lieferung =>
        stammdatenWriteRepository.updateEntityIf[Lieferung, LieferungId](Abgeschlossen == _.status)(lieferung.id) {
          lieferungMapping.column.status -> Verrechnet
        }
      }
      stammdatenWriteRepository.getSammelbestellungen(id) map { sammelbestellung =>
        stammdatenWriteRepository.updateEntityIf[Sammelbestellung, SammelbestellungId](Abgeschlossen == _.status)(sammelbestellung.id)(
          sammelbestellungMapping.column.status -> Verrechnet,
          sammelbestellungMapping.column.datumAbrechnung -> Option(DateTime.now)
        )
      }
    }
  }

  def lieferplanungDataModified(meta: EventMetadata, result: LieferplanungDataModify)(implicit personId: PersonId = meta.originator) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.getById(lieferplanungMapping, result.id) map { lieferplanung =>
        if (Offen == lieferplanung.status || Abgeschlossen == lieferplanung.status) {
          // lieferungen mit positionen anpassen
          result.lieferungen map { l =>
            recreateLieferpositionen(meta, l.id, l.lieferpositionen)
          }

          // existierende Sammelbestellungen neu ausrechnen
          stammdatenWriteRepository.getSammelbestellungen(result.id) map { s =>
            createOrUpdateSammelbestellungen(s.id, SammelbestellungModify(s.produzentId, s.lieferplanungId, s.datum))
          }

          // neue sammelbestellungen erstellen
          result.newSammelbestellungen map { s =>
            createOrUpdateSammelbestellungen(s.id, SammelbestellungModify(s.produzentId, s.lieferplanungId, s.datum))
          }
        }
      }
    }
  }

  def sammelbestellungVersenden(meta: EventMetadata, id: SammelbestellungId)(implicit personId: PersonId = meta.originator) = {
    val format = DateTimeFormat.forPattern("dd.MM.yyyy")

    DB localTxPostPublish { implicit session => implicit publisher =>
      //send mails to Produzenten
      stammdatenWriteRepository.getProjekt map { projekt =>
        stammdatenWriteRepository.getById(sammelbestellungMapping, id) map {
          //send mails only if current event timestamp is past the timestamp of last delivered mail
          case sammelbestellung if (sammelbestellung.datumVersendet.isEmpty || sammelbestellung.datumVersendet.get.isBefore(meta.timestamp)) =>
            stammdatenWriteRepository.getProduzentDetail(sammelbestellung.produzentId) map { produzent =>
              // prepare data for mail
              val bestellungen = stammdatenWriteRepository.getBestellungen(sammelbestellung.id) map { bestellung =>
                val bestellpositionen = stammdatenWriteRepository.getBestellpositionen(bestellung.id) map {
                  bestellposition =>
                    copyTo[Bestellposition, BestellpositionMail](bestellposition)
                }
                copyTo[Bestellung, BestellungMail](bestellung, "bestellpositionen" -> bestellpositionen)
              }

              val mailContext = SammelbestellungMailContext(sammelbestellung, projekt, produzent, bestellungen)
              mailTemplateReadRepositorySync.getMailTemplateByTemplateType(ProduzentenBestellungMailTemplateType) match {
                case Some(template: MailTemplate) => {
                  generateMail(template.subject, template.body, mailContext) match {
                    case Success(mailPayload) =>
                      val mail = mailPayload.toMail(1, produzent.email, None, None, None)
                      mailService ? SendMailCommandWithCallback(personId, mail, Some(5 minutes), produzent.id) map
                        {
                          case _: SendMailEvent =>
                          //ok
                          case other =>
                            logger.debug(s"Sending Mail failed resulting in $other")
                        }
                    case Failure(e) =>
                      logger.warn(s"Failed preparing mail", e)
                  }
                }
                case None => logger.warn(s"No mail template was found for the type ProduzentenBestellungMailTemplateType")
              }
            }
          case _ => //ignore
            logger.debug(s"Don't resend Bestellung, already delivered")
        }
      }
    }
  }

  def updatePasswort(meta: EventMetadata, id: PersonId, pwd: Array[Char], einladungId: Option[EinladungId])(implicit personId: PersonId = meta.originator) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.updateEntity[Person, PersonId](id)(personMapping.column.passwort -> Option(pwd))

      einladungId map { id =>
        stammdatenWriteRepository.updateEntity[Einladung, EinladungId](id)(einladungMapping.column.expires -> new DateTime())
      }
    }
  }

  def disableLogin(meta: EventMetadata, personId: PersonId)(implicit originator: PersonId = meta.originator) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.updateEntity[Person, PersonId](personId)(personMapping.column.loginAktiv -> false)
    }
  }

  def enableLogin(meta: EventMetadata, personId: PersonId)(implicit originator: PersonId = meta.originator) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      setLoginAktiv(meta, personId)
    }
  }

  def setLoginAktiv(meta: EventMetadata, personId: PersonId)(implicit originator: PersonId = meta.originator, session: DBSession, publisher: EventPublisher) = {
    stammdatenWriteRepository.updateEntity[Person, PersonId](personId)(personMapping.column.loginAktiv -> true)
  }

  def sendPasswortReset(meta: EventMetadata, einladungCreate: EinladungCreate)(implicit originator: PersonId = meta.originator): Unit = {
    sendEinladung(meta, einladungCreate, BasePasswortResetLink, PasswordResetMailTemplateType)
  }

  def sendEinladung(meta: EventMetadata, einladungCreate: EinladungCreate)(implicit originator: PersonId = meta.originator): Unit = {
    sendEinladung(meta, einladungCreate, BaseZugangLink, InvitationMailTemplateType)
  }

  private def sendEinladung(meta: EventMetadata, einladungCreate: EinladungCreate, baseLink: String, mailTemplateType: TemplateType)(implicit originator: PersonId): Unit = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.getById(personMapping, einladungCreate.personId) map { person =>

        // existierende einladung überprüfen
        val einladung = stammdatenWriteRepository.getById(einladungMapping, einladungCreate.id) getOrElse {
          val inserted = copyTo[EinladungCreate, Einladung](
            einladungCreate,
            "erstelldat" -> meta.timestamp,
            "ersteller" -> meta.originator,
            "modifidat" -> meta.timestamp,
            "modifikator" -> meta.originator
          )
          stammdatenWriteRepository.insertEntity[Einladung, EinladungId](inserted)
          inserted
        }

        if (einladung.erstelldat.isAfter(new DateTime(2017, 3, 2, 12, 0)) && (einladung.datumVersendet.isEmpty || einladung.datumVersendet.get.isBefore(meta.timestamp)) && einladung.expires.isAfter(DateTime.now)) {
          setLoginAktiv(meta, einladung.personId)
          mailTemplateReadRepositorySync.getMailTemplateByTemplateType(mailTemplateType) match {
            case Some(template: MailTemplate) => {
              val mailContext = EinladungMailContext(person, einladung, baseLink)
              generateMail(template.subject, template.body, mailContext) match {
                case Success(mailPayload) =>
                  val mail = mailPayload.toMail(1, person.email.get, None, None, None)
                  mailService ? SendMailCommandWithCallback(originator, mail, Some(5 minutes), person.id) map
                    {
                      case _: SendMailEvent =>
                      //ok
                      case other =>
                        logger.debug(s"Sending Mail failed resulting in $other")
                    }
                case Failure(e) =>
                  logger.warn(s"Failed preparing mail", e)
              }
            }
            case None => logger.warn(s"No mail template was found for the type $mailTemplateType")
          }
        } else {
          logger.debug(s"Don't send Einladung, has been send earlier: ${einladungCreate.id}")
        }
      }
    }
  }

  def changeRolle(meta: EventMetadata, personId: PersonId, rolle: Rolle)(implicit originator: PersonId = meta.originator) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.updateEntity[Person, PersonId](personId)(personMapping.column.rolle -> Option(rolle))
    }
  }

  def updateKunde(meta: EventMetadata, kundeId : KundeId, kunde: KundeModify)(implicit originator: PersonId = meta.originator) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.updateEntity[Kunde, KundeId](kundeId)(updateKunde())
    }
  }

  /**
   * @deprecated handling already persisted events. auslieferungAusgeliefert is now done in update service.
   */
  def auslieferungAusgeliefert(meta: EventMetadata, id: AuslieferungId)(implicit personId: PersonId = meta.originator) = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.updateEntityIf[DepotAuslieferung, AuslieferungId](Erfasst == _.status)(id) {
        depotAuslieferungMapping.column.status -> Ausgeliefert
      } orElse stammdatenWriteRepository.updateEntityIf[TourAuslieferung, AuslieferungId](Erfasst == _.status)(id) {
        tourAuslieferungMapping.column.status -> Ausgeliefert
      } orElse stammdatenWriteRepository.updateEntityIf[PostAuslieferung, AuslieferungId](Erfasst == _.status)(id) {
        tourAuslieferungMapping.column.status -> Ausgeliefert
      }
    }
  }

  def sammelbestellungAbgerechnet(meta: EventMetadata, datum: DateTime, id: SammelbestellungId)(implicit personId: PersonId = meta.originator) = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.updateEntityIf[Sammelbestellung, SammelbestellungId](Abgeschlossen == _.status)(id)(
        sammelbestellungMapping.column.status -> Verrechnet,
        sammelbestellungMapping.column.datumAbrechnung -> Option(datum)
      )
    }
  }
}
