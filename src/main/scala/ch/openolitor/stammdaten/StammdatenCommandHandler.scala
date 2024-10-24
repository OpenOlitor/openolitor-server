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

import ch.openolitor.buchhaltung.models.{ RechnungsPositionStatus, RechnungsPositionTyp }
import ch.openolitor.core.domain._
import ch.openolitor.core.models._
import ch.openolitor.util.ConfigUtil._

import scala.concurrent.ExecutionContext
import scala.util._
import scalikejdbc.DB
import ch.openolitor.stammdaten.models._
import ch.openolitor.stammdaten.repositories._
import ch.openolitor.core.exceptions._
import akka.actor.{ ActorRef, ActorSystem }
import akka.persistence.journal.EmptyEventSeq
import ch.openolitor.core._
import ch.openolitor.core.db.ConnectionPoolContextAware
import ch.openolitor.core.Macros._
import ch.openolitor.mailtemplates.engine.MailTemplateService
import ch.openolitor.buchhaltung.models.RechnungsPositionCreate
import ch.openolitor.buchhaltung.models.RechnungsPositionId
import ch.openolitor.util.OtpUtil
import org.joda.time.{ DateTime, LocalDate, LocalTime }

import java.util.UUID
import scalikejdbc.DBSession

object StammdatenCommandHandler {
  case class LieferplanungAbschliessenCommand(originator: PersonId, id: LieferplanungId) extends UserCommand

  case class LieferplanungModifyCommand(originator: PersonId, lieferplanungModify: LieferplanungPositionenModify) extends UserCommand

  case class LieferplanungAbrechnenCommand(originator: PersonId, id: LieferplanungId) extends UserCommand

  case class AbwesenheitCreateCommand(originator: PersonId, abw: AbwesenheitCreate) extends UserCommand

  case class SammelbestellungAnProduzentenVersendenCommand(originator: PersonId, id: SammelbestellungId) extends UserCommand

  case class PasswortWechselCommand(originator: PersonId, personId: PersonId, passwort: Array[Char], einladung: Option[EinladungId]) extends UserCommand

  case class AuslieferungenAlsAusgeliefertMarkierenCommand(originator: PersonId, ids: Seq[AuslieferungId]) extends UserCommand

  case class CreateAnzahlLieferungenRechnungsPositionenCommand(originator: PersonId, aboRechnungCreate: AboRechnungsPositionBisAnzahlLieferungenCreate)
    extends UserCommand

  case class CreateBisGuthabenRechnungsPositionenCommand(originator: PersonId, aboRechnungCreate: AboRechnungsPositionBisGuthabenCreate) extends UserCommand

  case class LoginDeaktivierenCommand(originator: PersonId, kundeId: KundeId, personId: PersonId) extends UserCommand

  case class LoginAktivierenCommand(originator: PersonId, kundeId: KundeId, personId: PersonId) extends UserCommand

  case class EinladungSendenCommand(originator: PersonId, kundeId: KundeId, personId: PersonId) extends UserCommand

  case class SammelbestellungenAlsAbgerechnetMarkierenCommand(originator: PersonId, datum: DateTime, ids: Seq[SammelbestellungId]) extends UserCommand

  case class PasswortResetCommand(originator: PersonId, personId: PersonId) extends UserCommand

  case class RolleWechselnCommand(originator: PersonId, kundeId: KundeId, personId: PersonId, rolle: Rolle) extends UserCommand

  case class OtpResetCommand(originator: PersonId, kundeId: KundeId, personId: PersonId) extends UserCommand

  case class UpdateKundeCommand(originator: PersonId, kundeId: KundeId, kunde: KundeModify) extends UserCommand

  case class CreateKundeCommand(originator: PersonId, kunde: KundeModify) extends UserCommand

  case class CreateLieferungAbotypCommand(originator: PersonId, lieferungAbotypCreate: LieferungAbotypCreate) extends UserCommand

  case class CreateLieferungenAbotypCommand(originator: PersonId, lieferungenAbotypCreate: LieferungenAbotypCreate) extends UserCommand

  case class RemoveLieferungCommand(originator: PersonId, lieferungId: LieferungId) extends UserCommand

  // TODO person id for calculations
  case class AboAktivierenCommand(aboId: AboId, originator: PersonId = PersonId(100)) extends UserCommand

  case class AboDeaktivierenCommand(aboId: AboId, originator: PersonId = PersonId(100)) extends UserCommand

  case class DeleteAbwesenheitCommand(originator: PersonId, id: AbwesenheitId) extends UserCommand

  case class SendEmailToKundenCommand(originator: PersonId, subject: String, body: String, replyTo: Option[String], ids: Seq[KundeId]) extends UserCommand

  case class SendEmailToPersonenCommand(originator: PersonId, subject: String, body: String, replyTo: Option[String], ids: Seq[PersonId]) extends UserCommand

  case class SendEmailToAbosSubscribersCommand(originator: PersonId, subject: String, body: String, replyTo: Option[String], ids: Seq[AboId]) extends UserCommand

  case class SendEmailToAbotypSubscribersCommand(originator: PersonId, subject: String, body: String, replyTo: Option[String], ids: Seq[AbotypId]) extends UserCommand

  case class SendEmailToZusatzabotypSubscribersCommand(originator: PersonId, subject: String, body: String, replyTo: Option[String], ids: Seq[AbotypId])
    extends UserCommand

  case class SendEmailToTourSubscribersCommand(originator: PersonId, subject: String, body: String, replyTo: Option[String], ids: Seq[TourId]) extends UserCommand

  case class SendEmailToDepotSubscribersCommand(originator: PersonId, subject: String, body: String, replyTo: Option[String], ids: Seq[DepotId]) extends UserCommand

  case class LieferplanungAbschliessenEvent(meta: EventMetadata, id: LieferplanungId) extends PersistentEvent with JSONSerializable

  case class LieferplanungAbrechnenEvent(meta: EventMetadata, id: LieferplanungId) extends PersistentEvent with JSONSerializable

  case class LieferplanungDataModifiedEvent(meta: EventMetadata, result: LieferplanungDataModify) extends PersistentEvent with JSONSerializable

  case class AbwesenheitCreateEvent(meta: EventMetadata, id: AbwesenheitId, abw: AbwesenheitCreate) extends PersistentEvent with JSONSerializable

  @Deprecated
  case class BestellungVersendenEvent(meta: EventMetadata, id: BestellungId) extends PersistentEvent with JSONSerializable

  case class SammelbestellungVersendenEvent(meta: EventMetadata, id: SammelbestellungId) extends PersistentEvent with JSONSerializable

  case class PasswortGewechseltEvent(meta: EventMetadata, personId: PersonId, passwort: Array[Char], einladungId: Option[EinladungId]) extends PersistentEvent with JSONSerializable

  case class LoginDeaktiviertEvent(meta: EventMetadata, kundeId: KundeId, personId: PersonId) extends PersistentEvent with JSONSerializable

  case class LoginAktiviertEvent(meta: EventMetadata, kundeId: KundeId, personId: PersonId) extends PersistentEvent with JSONSerializable

  case class EinladungGesendetEvent(meta: EventMetadata, einladung: EinladungCreate) extends PersistentEvent with JSONSerializable

  case class AuslieferungAlsAusgeliefertMarkierenEvent(meta: EventMetadata, id: AuslieferungId) extends PersistentEvent with JSONSerializable

  @Deprecated
  case class BestellungAlsAbgerechnetMarkierenEvent(meta: EventMetadata, datum: DateTime, id: BestellungId) extends PersistentEvent with JSONSerializable

  case class SammelbestellungAlsAbgerechnetMarkierenEvent(meta: EventMetadata, datum: DateTime, id: SammelbestellungId) extends PersistentEvent with JSONSerializable

  case class PasswortResetGesendetEvent(meta: EventMetadata, einladung: EinladungCreate) extends PersistentEvent with JSONSerializable

  case class RolleGewechseltEvent(meta: EventMetadata, kundeId: KundeId, personId: PersonId, rolle: Rolle) extends PersistentEvent with JSONSerializable

  case class OtpResetEvent(meta: EventMetadata, kundeId: KundeId, personId: PersonId, otpSecret: String) extends PersistentEvent with JSONSerializable

  case class AboAktiviertEvent(meta: EventMetadata, aboId: AboId) extends PersistentGeneratedEvent with JSONSerializable

  case class AboDeaktiviertEvent(meta: EventMetadata, aboId: AboId) extends PersistentGeneratedEvent with JSONSerializable

  case class SendEmailToPersonEvent(meta: EventMetadata, subject: String, body: String, replyTo: Option[String], context: PersonMailContext) extends PersistentGeneratedEvent with JSONSerializable

  case class SendEmailToKundeEvent(meta: EventMetadata, subject: String, body: String, replyTo: Option[String], context: KundeMailContext) extends PersistentGeneratedEvent with JSONSerializable

  case class SendEmailToAboSubscriberEvent(meta: EventMetadata, subject: String, body: String, replyTo: Option[String], context: AboMailContext) extends PersistentGeneratedEvent with JSONSerializable

  case class SendEmailToAbotypSubscriberEvent(meta: EventMetadata, subject: String, body: String, replyTo: Option[String], context: AbotypMailContext)
    extends PersistentGeneratedEvent with JSONSerializable

  case class SendEmailToZusatzabotypSubscriberEvent(meta: EventMetadata, subject: String, body: String, replyTo: Option[String],
    context: ZusatzabotypMailContext) extends PersistentGeneratedEvent with JSONSerializable

  case class SendEmailToTourSubscriberEvent(meta: EventMetadata, subject: String, body: String, replyTo: Option[String], context: TourMailContext) extends PersistentGeneratedEvent with JSONSerializable

  case class SendEmailToDepotSubscriberEvent(meta: EventMetadata, subject: String, body: String, replyTo: Option[String], context: DepotMailContext) extends PersistentGeneratedEvent with JSONSerializable

  case class UpdateKundeEvent(meta: EventMetadata, kundeId: KundeId, kunde: KundeModify) extends PersistentGeneratedEvent with JSONSerializable
}

trait StammdatenCommandHandler extends CommandHandler
  with StammdatenDBMappings
  with ConnectionPoolContextAware
  with LieferungDurchschnittspreisHandler
  with MailTemplateService
  with MailCommandForwarderComponent
  with ExecutionContextAware
  with ProjektHelper {

  self: StammdatenReadRepositorySyncComponent =>

  import StammdatenCommandHandler._
  import EntityStore._

  override val handle: PartialFunction[UserCommand, IdFactory => EventTransactionMetadata => Try[Seq[ResultingEvent]]] = {

    case DeleteAbwesenheitCommand(_, id) => _ =>
      _ =>
        DB readOnly { implicit session =>
          stammdatenReadRepository.getLieferung(id) map { lieferung =>
            lieferung.status match {
              case (Offen | Ungeplant) =>
                Success(Seq(EntityDeleteEvent(id)))
              case _ =>
                Failure(new InvalidStateException("Die der Abwesenheit zugeordnete Lieferung muss Offen oder Ungeplant sein."))
            }
          } getOrElse Failure(new InvalidStateException(s"Keine Lieferung zu Abwesenheit Nr. $id gefunden"))
        }

    case SendEmailToKundenCommand(personId, subject, body, replyTo, ids) => idFactory =>
      meta =>
        DB readOnly { implicit session =>
          if (checkTemplateKunden(body, subject, ids)) {
            val events = ids flatMap { kundeId =>
              stammdatenReadRepository.getPersonen(kundeId) flatMap { person =>
                val personData = copyTo[Person, PersonData](person)
                stammdatenReadRepository.getById(kundeMapping, kundeId) map { kunde =>
                  val personEmailData = copyTo[Person, PersonEmailData](person)
                  val mailContext = KundeMailContext(personEmailData, kunde)

                  mailCommandForwarder.sendEmail(meta, subject, body, replyTo, determineBcc, personEmailData, None, mailContext)

                  DefaultResultingEvent(factory => SendEmailToKundeEvent(factory.newMetadata(), subject, body, replyTo, mailContext))
                }
              }
            }
            Success(events)
          } else {
            Failure(new InvalidStateException("The template is not valid"))
          }
        }

    case SendEmailToPersonenCommand(personId, subject, body, replyTo, ids) => idFactory =>
      meta =>
        DB readOnly { implicit session =>
          if (checkTemplatePersonen(body, subject, ids)) {
            val events = ids flatMap { id =>
              stammdatenReadRepository.getById(personMapping, id) map { person =>
                val personEmailData = copyTo[Person, PersonEmailData](person)
                val mailContext = PersonMailContext(personEmailData)

                mailCommandForwarder.sendEmail(meta, subject, body, replyTo, determineBcc, personEmailData, None, mailContext)

                DefaultResultingEvent(factory => SendEmailToPersonEvent(factory.newMetadata(), subject, body, replyTo, mailContext))
              }
            }
            Success(events)
          } else {
            Failure(new InvalidStateException("The template is not valid"))
          }
        }

    case SendEmailToAbotypSubscribersCommand(personId, subject, body, replyTo, ids) => idFactory =>
      meta =>
        DB readOnly { implicit session =>
          if (checkTemplateAbotyp(body, subject, ids)) {
            val events = ids flatMap { abotypId =>
              stammdatenReadRepository.getPersonenForAbotyp(abotypId) flatMap { person =>
                val personData = copyTo[Person, PersonData](person)
                stammdatenReadRepository.getAbotypById(abotypId) map { abotypId =>
                  val personEmailData = copyTo[Person, PersonEmailData](person)
                  val mailContext = AbotypMailContext(personEmailData, abotypId)

                  mailCommandForwarder.sendEmail(meta, subject, body, replyTo, determineBcc, personEmailData, None, mailContext)

                  DefaultResultingEvent(factory => SendEmailToAbotypSubscriberEvent(factory.newMetadata(), subject, body, replyTo, mailContext))
                }
              }
            }
            Success(events)
          } else {
            Failure(new InvalidStateException("The template is not valid"))
          }
        }

    case SendEmailToZusatzabotypSubscribersCommand(personId, subject, body, replyTo, ids) => idFactory =>
      meta =>
        DB readOnly { implicit session =>
          if (checkTemplateAbotyp(body, subject, ids)) {
            val events = ids flatMap { abotypId =>
              stammdatenReadRepository.getPersonenForZusatzabotyp(abotypId) flatMap { person =>
                val personData = copyTo[Person, PersonData](person)
                stammdatenReadRepository.getAbotypById(abotypId) map { abotypId =>
                  val personEmailData = copyTo[Person, PersonEmailData](person)
                  val mailContext = AbotypMailContext(personEmailData, abotypId)

                  mailCommandForwarder.sendEmail(meta, subject, body, replyTo, determineBcc, personEmailData, None, mailContext)

                  DefaultResultingEvent(factory => SendEmailToAbotypSubscriberEvent(factory.newMetadata(), subject, body, replyTo, mailContext))
                }
              }
            }
            Success(events)
          } else {
            Failure(new InvalidStateException("The template is not valid"))
          }
        }

    case SendEmailToTourSubscribersCommand(personId, subject, body, replyTo, ids) => idFactory =>
      meta =>
        DB readOnly { implicit session =>
          if (checkTemplateTour(body, subject, ids)) {
            val events = ids flatMap { tourId =>
              stammdatenReadRepository.getPersonen(tourId) flatMap { person =>
                val personData = copyTo[Person, PersonData](person)
                stammdatenReadRepository.getById(tourMapping, tourId) map { tour =>
                  val personEmailData = copyTo[Person, PersonEmailData](person)
                  val mailContext = TourMailContext(personEmailData, tour)

                  mailCommandForwarder.sendEmail(meta, subject, body, replyTo, determineBcc, personEmailData, None, mailContext)

                  DefaultResultingEvent(factory => SendEmailToTourSubscriberEvent(factory.newMetadata(), subject, body, replyTo, mailContext))
                }
              }
            }
            Success(events)
          } else {
            Failure(new InvalidStateException("The template is not valid"))
          }
        }

    case SendEmailToDepotSubscribersCommand(personId, subject, body, replyTo, ids) => idFactory =>
      meta =>
        DB readOnly { implicit session =>
          if (checkTemplateDepot(body, subject, ids)) {
            val events = ids flatMap { depotId =>
              stammdatenReadRepository.getPersonen(depotId) flatMap { person =>
                val personData = copyTo[Person, PersonData](person)
                stammdatenReadRepository.getById(depotMapping, depotId) map { depot =>
                  val personEmailData = copyTo[Person, PersonEmailData](person)
                  val mailContext = DepotMailContext(personEmailData, depot)

                  mailCommandForwarder.sendEmail(meta, subject, body, replyTo, determineBcc, personEmailData, None, mailContext)

                  DefaultResultingEvent(factory => SendEmailToDepotSubscriberEvent(factory.newMetadata(), subject, body, replyTo, mailContext))
                }
              }
            }
            Success(events)
          } else {
            Failure(new InvalidStateException("The template is not valid"))
          }
        }

    case SendEmailToAbosSubscribersCommand(personId, subject, body, replyTo, ids) => idFactory =>
      meta =>
        DB readOnly { implicit session =>
          if (checkTemplateAbosSubscribers(body, subject, ids)) {
            val events = ids flatMap { aboId: AboId =>
              stammdatenReadRepository.getById(zusatzAboMapping, aboId) orElse
                stammdatenReadRepository.getById(depotlieferungAboMapping, aboId) orElse
                stammdatenReadRepository.getById(heimlieferungAboMapping, aboId) orElse
                stammdatenReadRepository.getById(postlieferungAboMapping, aboId) map { abo =>
                  stammdatenReadRepository.getPersonen(abo.kundeId) map { person =>
                    val personEmailData = copyTo[Person, PersonEmailData](person)
                    val mailContext = AboMailContext(personEmailData, abo)

                    mailCommandForwarder.sendEmail(meta, subject, body, replyTo, determineBcc, personEmailData, None, mailContext)

                    DefaultResultingEvent(factory => SendEmailToAboSubscriberEvent(factory.newMetadata(), subject, body, replyTo, mailContext))
                  }
                }
            }
            Success(events.flatten)
          } else {
            Failure(new InvalidStateException("The template is not valid"))
          }
        }

    case LieferplanungAbschliessenCommand(personId, id) => idFactory =>
      meta =>
        DB readOnly { implicit session =>
          stammdatenReadRepository.getById(lieferplanungMapping, id) map { lieferplanung =>
            lieferplanung.status match {
              case Offen =>
                stammdatenReadRepository.countEarlierLieferungOffen(id) match {
                  case Some(0) =>
                    val distinctSammelbestellungen = getDistinctSammelbestellungModifyByLieferplan(lieferplanung.id)

                    val bestellEvents = distinctSammelbestellungen.map { sammelbestellungCreate =>
                      val insertEvent = EntityInsertEvent(idFactory.newId(SammelbestellungId.apply), sammelbestellungCreate)

                      Seq(insertEvent)
                    }.toSeq.flatten

                    val lpAbschliessenEvent = DefaultResultingEvent(factory => LieferplanungAbschliessenEvent(factory.newMetadata(), id))

                    val createAuslieferungHeimEvent = getCreateAuslieferungHeimEvent(idFactory, meta, lieferplanung)(personId, session)
                    val createAuslieferungDepotPostEvent = getCreateDepotAuslieferungAndPostAusliferungEvent(idFactory, meta, lieferplanung)(personId, session)
                    Success(lpAbschliessenEvent +: bestellEvents ++: createAuslieferungHeimEvent ++: createAuslieferungDepotPostEvent)
                  case _ =>
                    Failure(new InvalidStateException("Es dürfen keine früheren Lieferungen in offnen Lieferplanungen hängig sein."))
                }
              case _ =>
                Failure(new InvalidStateException("Eine Lieferplanung kann nur im Status 'Offen' abgeschlossen werden"))
            }
          } getOrElse Failure(new InvalidStateException(s"Keine Lieferplanung mit der Nr. $id gefunden"))
        }

    case LieferplanungModifyCommand(_, lieferplanungPositionenModify) => idFactory =>
      _ =>
        DB readOnly { implicit session =>
          stammdatenReadRepository.getById(lieferplanungMapping, lieferplanungPositionenModify.id) map { lieferplanung =>
            lieferplanung.status match {
              case _@ Offen =>
                Success(DefaultResultingEvent(factory => LieferplanungDataModifiedEvent(factory.newMetadata(), LieferplanungDataModify(lieferplanungPositionenModify.id, Set.empty, lieferplanungPositionenModify.lieferungen))) :: Nil)
              case _@ Abgeschlossen =>
                val allLieferpositionen = lieferplanungPositionenModify.lieferungen flatMap { lieferungPositionenModify =>
                  lieferungPositionenModify.lieferpositionen.lieferpositionen
                }

                val allLieferungpositionPerProduzent = allLieferpositionen.groupBy(_.produzentId)

                val sammelBestellungToCreate = allLieferungpositionPerProduzent flatMap { l =>
                  stammdatenReadRepository.getById(lieferungMapping, l._2.head.lieferungId) map { lieferung: Lieferung =>
                    stammdatenReadRepository.getSammelbestellungenByProduzent(l._1, lieferplanung.id) match {
                      case Nil => Some(SammelbestellungCreate(idFactory.newId(SammelbestellungId.apply), l._1, lieferplanung.id, lieferung.datum))
                      case _   => None
                    }
                  }
                }
                Success(DefaultResultingEvent(factory => LieferplanungDataModifiedEvent(factory.newMetadata(), LieferplanungDataModify(lieferplanungPositionenModify.id, sammelBestellungToCreate.flatten.toSet, lieferplanungPositionenModify.lieferungen))) :: Nil)
              case _ =>
                Failure(new InvalidStateException("Eine Lieferplanung kann nur im Status 'Offen' oder 'Abgeschlossen' aktualisiert werden"))
            }
          } getOrElse Failure(new InvalidStateException(s"Keine Lieferplanung mit der Nr. ${lieferplanungPositionenModify.id} gefunden"))
        }

    case LieferplanungAbrechnenCommand(_, id: LieferplanungId) => _ =>
      _ =>
        DB readOnly { implicit session =>
          stammdatenReadRepository.getById(lieferplanungMapping, id) map { lieferplanung =>
            lieferplanung.status match {
              case Abgeschlossen =>
                Success(Seq(DefaultResultingEvent(factory => LieferplanungAbrechnenEvent(factory.newMetadata(), id))))
              case _ =>
                Failure(new InvalidStateException("Eine Lieferplanung kann nur im Status 'Abgeschlossen' verrechnet werden"))
            }
          } getOrElse Failure(new InvalidStateException(s"Keine Lieferplanung mit der Nr. $id gefunden"))
        }

    case AbwesenheitCreateCommand(_, abw: AbwesenheitCreate) => idFactory =>
      meta =>
        DB readOnly { implicit session =>
          stammdatenReadRepository.countAbwesend(abw.lieferungId, abw.aboId) match {
            case Some(0) =>
              handleEntityInsert[AbwesenheitCreate, AbwesenheitId](idFactory, meta, abw, AbwesenheitId.apply)
            case _ =>
              Failure(new InvalidStateException("Eine Abwesenheit kann nur einmal erfasst werden"))
          }
        }

    case SammelbestellungAnProduzentenVersendenCommand(_, id: SammelbestellungId) => _ =>
      _ =>
        DB readOnly { implicit session =>
          stammdatenReadRepository.getById(sammelbestellungMapping, id) map { sammelbestellung =>
            sammelbestellung.status match {
              case Offen | Abgeschlossen =>
                Success(Seq(DefaultResultingEvent(factory => SammelbestellungVersendenEvent(factory.newMetadata(), id))))
              case _ =>
                Failure(new InvalidStateException("Eine Bestellung kann nur in den Status 'Offen' oder 'Abgeschlossen' versendet werden"))
            }
          } getOrElse Failure(new InvalidStateException(s"Keine Bestellung mit der Nr. $id gefunden"))
        }

    case AuslieferungenAlsAusgeliefertMarkierenCommand(_, ids: Seq[AuslieferungId]) => _ =>
      _ =>
        DB readOnly { implicit session =>
          val (events, _) = ids map { id =>
            stammdatenReadRepository.getById(depotAuslieferungMapping, id) orElse
              stammdatenReadRepository.getById(tourAuslieferungMapping, id) orElse
              stammdatenReadRepository.getById(postAuslieferungMapping, id) map { auslieferung =>
                auslieferung.status match {
                  case Erfasst =>
                    val copy = auslieferung match {
                      case d: DepotAuslieferung =>
                        d.copy(status = Ausgeliefert)
                      case t: TourAuslieferung =>
                        t.copy(status = Ausgeliefert)
                      case p: PostAuslieferung =>
                        p.copy(status = Ausgeliefert)
                    }
                    Success(EntityUpdateEvent(id, copy))
                  case _ =>
                    Failure(new InvalidStateException(s"Eine Auslieferung kann nur im Status 'Erfasst' als ausgeliefert markiert werden. Nr. $id"))
                }
              } getOrElse Failure(new InvalidStateException(s"Keine Auslieferung mit der Nr. $id gefunden"))
          } partition (_.isSuccess)

          if (events.isEmpty) {
            Failure(new InvalidStateException(s"Keine der Auslieferungen konnte abgearbeitet werden"))
          } else {
            Success(events map (_.get))
          }
        }

    case SammelbestellungenAlsAbgerechnetMarkierenCommand(_, datum, ids: Seq[SammelbestellungId]) => _ =>
      _ =>
        DB readOnly { implicit session =>
          val (events, _) = ids map { id =>
            stammdatenReadRepository.getById(sammelbestellungMapping, id) map { sammelbestellung =>
              sammelbestellung.status match {
                case Abgeschlossen =>
                  Success(DefaultResultingEvent(factory => SammelbestellungAlsAbgerechnetMarkierenEvent(factory.newMetadata(), datum, id)))
                case _ =>
                  Failure(new InvalidStateException(s"Eine Sammelbestellung kann nur im Status 'Abgeschlossen' als abgerechnet markiert werden. Nr. $id"))
              }
            } getOrElse Failure(new InvalidStateException(s"Keine Sammelbestellung mit der Nr. $id gefunden"))
          } partition (_.isSuccess)

          if (events.isEmpty) {
            Failure(new InvalidStateException(s"Keine der Sammelbestellung konnte abgearbeitet werden"))
          } else {
            Success(events map (_.get))
          }
        }

    case CreateAnzahlLieferungenRechnungsPositionenCommand(_, aboRechnungCreate) => idFactory =>
      meta =>
        createAboRechnungsPositionenAnzahlLieferungen(idFactory, meta, aboRechnungCreate)

    case CreateBisGuthabenRechnungsPositionenCommand(_, aboRechnungCreate) => idFactory =>
      meta =>
        createAboRechnungsPositionenBisGuthaben(idFactory, meta, aboRechnungCreate)

    case PasswortWechselCommand(_, personId, pwd, einladungId) => _ =>
      _ =>
        Success(Seq(DefaultResultingEvent(factory => PasswortGewechseltEvent(factory.newMetadata(), personId, pwd, einladungId))))

    case LoginDeaktivierenCommand(originator, kundeId, personId) if originator != personId => _ =>
      _ =>
        Success(Seq(DefaultResultingEvent(factory => LoginDeaktiviertEvent(factory.newMetadata(), kundeId, personId))))

    case LoginAktivierenCommand(originator, kundeId, personId) if originator != personId => _ =>
      _ =>
        Success(Seq(DefaultResultingEvent(factory => LoginAktiviertEvent(factory.newMetadata(), kundeId, personId))))

    case EinladungSendenCommand(originator, kundeId, personId) if originator != personId => idFactory =>
      meta =>
        sendEinladung(idFactory, meta, kundeId, personId)

    case PasswortResetCommand(_, personId) => idFactory =>
      meta =>
        sendPasswortReset(idFactory, meta, personId)

    case RolleWechselnCommand(originator, kundeId, personId, rolle) if originator != personId => idFactory =>
      meta =>
        changeRolle(idFactory, meta, kundeId, personId, rolle)
    case OtpResetCommand(_, kundeId, personId) => _ =>
      _ =>
        Success(Seq(DefaultResultingEvent(factory => OtpResetEvent(factory.newMetadata(), kundeId, personId, OtpUtil.generateOtpSecretString))))

    case AboAktivierenCommand(aboId, _) => _ =>
      _ =>
        Success(Seq(DefaultResultingEvent(factory => AboAktiviertEvent(factory.newMetadata(), aboId))))

    case AboDeaktivierenCommand(aboId, _) => _ =>
      _ =>
        Success(Seq(DefaultResultingEvent(factory => AboDeaktiviertEvent(factory.newMetadata(), aboId))))

    case UpdateKundeCommand(_, kundeId, kunde) => idFactory =>
      meta =>
        updateKunde(idFactory, meta, kundeId, kunde)

    case CreateKundeCommand(_, kunde) => idFactory =>
      meta =>
        createKunde(idFactory, meta, kunde)

    case CreateLieferungAbotypCommand(_, lieferungAbotypCreate) => idFactory =>
      meta =>
        createLieferungAbotyp(idFactory, meta, lieferungAbotypCreate)

    case CreateLieferungenAbotypCommand(_, lieferungenAbotypCreate) => idFactory =>
      meta =>
        createLieferungenAbotyp(idFactory, meta, lieferungenAbotypCreate)

    case RemoveLieferungCommand(_, lieferungId) => idFactory =>
      meta =>
        removeLieferung(idFactory, meta, lieferungId)

    /*
       * Insert command handling
       */
    case e @ InsertEntityCommand(_, entity: CustomKundentypCreate) => idFactory =>
      meta =>
        handleEntityInsert[CustomKundentypCreate, CustomKundentypId](idFactory, meta, entity, CustomKundentypId.apply)
    case e @ InsertEntityCommand(_, entity: LieferungenAbotypCreate) => idFactory =>
      meta =>
        val events = entity.daten.map { datum =>
          val lieferungCreate = copyTo[LieferungenAbotypCreate, LieferungAbotypCreate](entity, "datum" -> datum)
          insertEntityEvent[LieferungAbotypCreate, LieferungId](idFactory, meta, lieferungCreate, LieferungId.apply)
        }
        Success(events)
    case e @ InsertEntityCommand(_, entity: LieferungAbotypCreate) => idFactory =>
      meta =>
        handleEntityInsert[LieferungAbotypCreate, LieferungId](idFactory, meta, entity, LieferungId.apply)
    case e @ InsertEntityCommand(_, entity: LieferplanungCreate) => idFactory =>
      meta =>
        handleEntityInsert[LieferplanungCreate, LieferplanungId](idFactory, meta, entity, LieferplanungId.apply)
    case e @ InsertEntityCommand(_, entity: LieferungPlanungAdd) => idFactory =>
      meta =>
        handleEntityInsert[LieferungPlanungAdd, LieferungId](idFactory, meta, entity, LieferungId.apply)
    case e @ InsertEntityCommand(_, entity: LieferpositionenModify) => idFactory =>
      meta =>
        handleEntityInsert[LieferpositionenModify, LieferpositionId](idFactory, meta, entity, LieferpositionId.apply)
    case e @ InsertEntityCommand(_, entity: PendenzModify) => idFactory =>
      meta =>
        handleEntityInsert[PendenzModify, PendenzId](idFactory, meta, entity, PendenzId.apply)
    case e @ InsertEntityCommand(_, entity: PersonCreate) => idFactory =>
      meta =>
        handleEntityInsert[PersonCreate, PersonId](idFactory, meta, entity, PersonId.apply)
    case e @ InsertEntityCommand(_, entity: PersonCategoryCreate) => idFactory =>
      meta =>
        handleEntityInsert[PersonCategoryCreate, PersonCategoryId](idFactory, meta, entity, PersonCategoryId.apply)
    case e @ InsertEntityCommand(_, entity: ProduzentModify) => idFactory =>
      meta =>
        handleEntityInsert[ProduzentModify, ProduzentId](idFactory, meta, entity, ProduzentId.apply)
    case e @ InsertEntityCommand(_, entity: ProduktModify) => idFactory =>
      meta =>
        handleEntityInsert[ProduktModify, ProduktId](idFactory, meta, entity, ProduktId.apply)
    case e @ InsertEntityCommand(_, entity: ProduktProduktekategorie) => idFactory =>
      meta =>
        handleEntityInsert[ProduktProduktekategorie, ProduktProduktekategorieId](idFactory, meta, entity, ProduktProduktekategorieId.apply)
    case e @ InsertEntityCommand(_, entity: ProduktProduzent) => idFactory =>
      meta =>
        handleEntityInsert[ProduktProduzent, ProduktProduzentId](idFactory, meta, entity, ProduktProduzentId.apply)
    case e @ InsertEntityCommand(_, entity: ProduktekategorieModify) => idFactory =>
      meta =>
        handleEntityInsert[ProduktekategorieModify, ProduktekategorieId](idFactory, meta, entity, ProduktekategorieId.apply)
    case e @ InsertEntityCommand(_, entity: ProjektModify) => idFactory =>
      meta =>
        handleEntityInsert[ProjektModify, ProjektId](idFactory, meta, entity, ProjektId.apply)
    case e @ InsertEntityCommand(_, entity: TourCreate) => idFactory =>
      meta =>
        handleEntityInsert[TourCreate, TourId](idFactory, meta, entity, TourId.apply)
    case e @ InsertEntityCommand(_, entity: AbwesenheitCreate) => idFactory =>
      meta =>
        handleEntityInsert[AbwesenheitCreate, AbwesenheitId](idFactory, meta, entity, AbwesenheitId.apply)
    case e @ InsertEntityCommand(_, entity: AbotypModify) => idFactory =>
      meta =>
        handleEntityInsert[AbotypModify, AbotypId](idFactory, meta, entity, AbotypId.apply)
    case e @ InsertEntityCommand(_, entity: ZusatzAbotypModify) => idFactory =>
      meta =>
        handleEntityInsert[ZusatzAbotypModify, AbotypId](idFactory, meta, entity, AbotypId.apply)
    case e @ InsertEntityCommand(_, entity: DepotModify) => idFactory =>
      meta =>
        handleEntityInsert[DepotModify, DepotId](idFactory, meta, entity, DepotId.apply)
    case e @ InsertEntityCommand(_, entity: DepotlieferungModify) => idFactory =>
      meta =>
        handleEntityInsert[DepotlieferungModify, VertriebsartId](idFactory, meta, entity, VertriebsartId.apply)
    case e @ InsertEntityCommand(_, entity: HeimlieferungModify) => idFactory =>
      meta =>
        handleEntityInsert[HeimlieferungModify, VertriebsartId](idFactory, meta, entity, VertriebsartId.apply)
    case e @ InsertEntityCommand(_, entity: PostlieferungModify) => idFactory =>
      meta =>
        handleEntityInsert[PostlieferungModify, VertriebsartId](idFactory, meta, entity, VertriebsartId.apply)
    case e @ InsertEntityCommand(_, entity: DepotlieferungAbotypModify) => idFactory =>
      meta =>
        handleEntityInsert[DepotlieferungAbotypModify, VertriebsartId](idFactory, meta, entity, VertriebsartId.apply)
    case e @ InsertEntityCommand(_, entity: HeimlieferungAbotypModify) => idFactory =>
      meta =>
        handleEntityInsert[HeimlieferungAbotypModify, VertriebsartId](idFactory, meta, entity, VertriebsartId.apply)
    case e @ InsertEntityCommand(_, entity: PostlieferungAbotypModify) => idFactory =>
      meta =>
        handleEntityInsert[PostlieferungAbotypModify, VertriebsartId](idFactory, meta, entity, VertriebsartId.apply)
    case e @ InsertEntityCommand(_, entity: DepotlieferungAboCreate) => idFactory =>
      meta =>
        handleEntityInsert[DepotlieferungAboCreate, AboId](idFactory, meta, entity, AboId.apply)
    case e @ InsertEntityCommand(_, entity: HeimlieferungAboCreate) => idFactory =>
      meta =>
        handleEntityInsert[HeimlieferungAboCreate, AboId](idFactory, meta, entity, AboId.apply)
    case e @ InsertEntityCommand(_, entity: PostlieferungAboCreate) => idFactory =>
      meta =>
        handleEntityInsert[PostlieferungAboCreate, AboId](idFactory, meta, entity, AboId.apply)
    case e @ InsertEntityCommand(_, entity: ZusatzAboModify) => idFactory =>
      meta =>
        handleEntityInsert[ZusatzAboModify, AboId](idFactory, meta, entity, AboId.apply)
    case e @ InsertEntityCommand(_, entity: ZusatzAboCreate) => idFactory =>
      meta =>
        handleEntityInsert[ZusatzAboCreate, AboId](idFactory, meta, entity, AboId.apply)
    case e @ InsertEntityCommand(_, entity: PendenzCreate) => idFactory =>
      meta =>
        handleEntityInsert[PendenzCreate, PendenzId](idFactory, meta, entity, PendenzId.apply)
    case e @ InsertEntityCommand(_, entity: VertriebModify) => idFactory =>
      meta =>
        handleEntityInsert[VertriebModify, VertriebId](idFactory, meta, entity, VertriebId.apply)
    case e @ InsertEntityCommand(_, entity: ProjektVorlageCreate) => idFactory =>
      meta =>
        handleEntityInsert[ProjektVorlageCreate, ProjektVorlageId](idFactory, meta, entity, ProjektVorlageId.apply)

    /*
    * Custom update command handling
    */
    case UpdateEntityCommand(personId, id: KundeId, entity: KundeModify) => idFactory =>
      _ =>
        updateKundeEntity(idFactory, personId, id, entity)

    case UpdateEntityCommand(_, id: AboId, entity: AboGuthabenModify) => idFactory =>
      meta =>
        DB readOnly { implicit session =>
          //TODO: assemble text using gettext
          stammdatenReadRepository.getAboDetail(id) match {
            case Some(abo) => {
              val text = s"Guthaben manuell angepasst. Abo Nr.: ${id.id}; Bisher: ${abo.guthaben}; Neu: ${entity.guthabenNeu}; Grund: ${entity.bemerkung}"
              val pendenzEvent = addKundenPendenz(idFactory, meta, id, text, Erledigt)
              Success(Seq(Some(EntityUpdateEvent(id, entity)), pendenzEvent).flatten)
            }
            case None =>
              Failure(new InvalidStateException(s"UpdateEntityCommand: Abo konnte nicht gefunden werden"))
          }
        }
    case UpdateEntityCommand(_, id: AboId, entity: AboVertriebsartModify) => idFactory =>
      meta =>
        DB readOnly { implicit session =>
          //TODO: assemble text using gettext
          val newLieferungen = stammdatenReadRepository.getLieferungen(entity.vertriebIdNeu)
          stammdatenReadRepository.getAbo(id) match {
            case Some(abo) =>
              val text = s"Vertriebsart angepasst. Abo Nr.: ${id.id}, Neu: ${entity.vertriebsartIdNeu}; Grund: ${entity.bemerkung}"
              var absencesText = ""
              var pendenzStatus: PendenzStatus = Erledigt
              stammdatenReadRepository.getById(vertriebMapping, abo.vertriebId) match {
                case Some(vertrieb) =>
                  stammdatenReadRepository.getLieferungen(vertrieb.id).filter(lOld => (lOld.datum isAfter DateTime.now) && (newLieferungen.count(l => l.datum
                    == lOld.datum) == 0)) map { l =>
                    if (stammdatenReadRepository.getAbwesenheit(abo.id, l.datum).length > 0) {
                      absencesText = s"; Bitte Abwesenheiten prüfen!"
                      pendenzStatus = Ausstehend
                    }
                  }
                case None => Failure(new InvalidStateException(s"UpdateEntityCommand: Some error happened when creating a pendenz"))
              }
              val pendenzEvent = addKundenPendenz(idFactory, meta, id, text + absencesText, pendenzStatus)
              Success(Seq(Some(EntityUpdateEvent(id, entity)), pendenzEvent).flatten)
            case None =>
              Failure(new InvalidStateException(s"UpdateEntityCommand: Some error happened when creating a pendenz"))
          }
        }
  }

  def addKundenPendenz(idFactory: IdFactory, meta: EventTransactionMetadata, id: AboId, bemerkung: String, status: PendenzStatus)(implicit session: DBSession): Option[ResultingEvent] = {
    // zusätzlich eine pendenz erstellen
    ((stammdatenReadRepository.getById(depotlieferungAboMapping, id) map { abo =>
      DepotlieferungAboModify
      abo.kundeId
    }) orElse (stammdatenReadRepository.getById(heimlieferungAboMapping, id) map { abo =>
      abo.kundeId
    }) orElse (stammdatenReadRepository.getById(postlieferungAboMapping, id) map { abo =>
      abo.kundeId
    })) map { kundeId =>
      //TODO: assemble text using gettext
      val title = "Guthaben angepasst: "
      val pendenzCreate = PendenzCreate(kundeId, meta.timestamp, Some(bemerkung), status, true)
      EntityInsertEvent[PendenzId, PendenzCreate](idFactory.newId(PendenzId.apply), pendenzCreate)
    }
  }

  def createAboRechnungsPositionenAnzahlLieferungen(idFactory: IdFactory, meta: EventTransactionMetadata,
    aboRechnungCreate: AboRechnungsPositionBisAnzahlLieferungenCreate) = {
    DB readOnly { implicit session =>
      // HauptAbos
      val abos: List[Abo] = stammdatenReadRepository.getByIds(depotlieferungAboMapping, aboRechnungCreate.ids) :::
        stammdatenReadRepository.getByIds(postlieferungAboMapping, aboRechnungCreate.ids) :::
        stammdatenReadRepository.getByIds(heimlieferungAboMapping, aboRechnungCreate.ids)

      // Zusatzabos
      val zusatzAbos: List[Abo] = stammdatenReadRepository.getByIds(zusatzAboMapping, aboRechnungCreate.ids)

      val allAbos = abos ::: zusatzAbos

      val aboTypen: List[IAbotyp] = stammdatenReadRepository.getByIds(abotypMapping, abos.map(_.abotypId)) :::
        stammdatenReadRepository.getByIds(zusatzAbotypMapping, zusatzAbos.map(_.abotypId))

      val abosWithAboTypen: List[(Abo, IAbotyp)] = allAbos.map { abo =>
        aboTypen.find(_.id == abo.abotypId).map { abotyp => (abo, abotyp) }
      }.flatten

      val (events, failures) = abosWithAboTypen.map {
        case (abo, abotyp) =>

          // TODO check preisEinheit
          if (abotyp.preiseinheit != ProLieferung) {
            Failure(new InvalidStateException(s"Für den Abotyp dieses Abos (${abo.id}) kann keine Anzahl Lieferungen-Rechnungsposition erstellt werden"))
          } else {
            // has to be refactored as soon as more modes are available
            val anzahlLieferungen = aboRechnungCreate.anzahlLieferungen
            if (anzahlLieferungen > 0) {
              val hauptAboBetrag = aboRechnungCreate.betrag.getOrElse(abo.price.getOrElse(abotyp.preis) * anzahlLieferungen)

              val repoType = abotyp match {
                case a: ZusatzAbotyp => RechnungsPositionTyp.ZusatzAbo
                case _               => RechnungsPositionTyp.Abo
              }

              val hauptRechnungPosition = createRechnungPositionEvent(abo, aboRechnungCreate.titel, anzahlLieferungen, hauptAboBetrag, aboRechnungCreate
                .waehrung, None, repoType)
              val parentRechnungPositionId = idFactory.newId(RechnungsPositionId.apply)

              Success(List(EntityInsertEvent(parentRechnungPositionId, hauptRechnungPosition)))

            } else {
              Failure(new InvalidStateException(s"Für das Abo mit der Id ${abo.id} wurde keine RechnungsPositionen erstellt. Anzahl Lieferungen 0"))
            }
          }
      } partition (_.isSuccess)

      if (events.isEmpty) {
        Failure(new InvalidStateException(s"Keine der RechnungsPositionen konnte erstellt werden"))
      } else {
        Success(events flatMap (_.get))
      }
    }
  }

  private def updateKundeEntity(idFactory: IdFactory, personId: PersonId, id: KundeId, entity: KundeModify) = {
    val partitions = entity.ansprechpersonen.partition(_.id.isDefined)
    val newPersons = partitions._2.zipWithIndex.map {
      case (newPerson, index) =>
        //generate persistent id for new person
        val sort = partitions._1.length + index + 1
        val personCreate = copyTo[PersonModify, PersonCreate](newPerson, "kundeId" -> id, "sort" -> sort)
        val event = EntityInsertEvent(idFactory.newId(PersonId.apply), personCreate)
        (event, newPerson.copy(id = Some(event.id)))
    }
    val newPersonsEvents = newPersons.map(_._1)
    val updatePersons = (partitions._1 ++ newPersons.map(_._2))

    val pendenzenPartitions = entity.pendenzen.partition(_.id.isDefined)
    val newPendenzen = pendenzenPartitions._2.map {
      case newPendenz =>
        val pendenzCreate = copyTo[PendenzModify, PendenzCreate](newPendenz, "kundeId" -> id, "generiert" -> FALSE)
        val event = EntityInsertEvent(idFactory.newId(PendenzId.apply), pendenzCreate)
        (event, newPendenz.copy(id = Some(event.id)))
    }
    val newPendenzenEvents = newPendenzen.map(_._1)
    val updatePendenzen = (pendenzenPartitions._1 ++ newPendenzen.map(_._2))

    val updateEntity = entity.copy(ansprechpersonen = updatePersons, pendenzen = updatePendenzen)
    val updateEvent = EntityUpdateEvent(id, updateEntity)
    Success(updateEvent +: (newPersonsEvents ++ newPendenzenEvents))
  }

  private def createRechnungPositionEvent(abo: Abo, titel: String, anzahlLieferungen: Int, betrag: BigDecimal, waehrung: Waehrung,
    parentRechnungsPositionId: Option[RechnungsPositionId] = None, rechnungsPositionTyp: RechnungsPositionTyp.RechnungsPositionTyp = RechnungsPositionTyp.Abo): RechnungsPositionCreate = {
    RechnungsPositionCreate(
      abo.kundeId,
      Some(abo.id),
      parentRechnungsPositionId,
      titel,
      Some(anzahlLieferungen),
      betrag,
      waehrung,
      RechnungsPositionStatus.Offen,
      RechnungsPositionTyp(rechnungsPositionTyp.toString)
    )
  }

  def createAboRechnungsPositionenBisGuthaben(idFactory: IdFactory, meta: EventTransactionMetadata, aboRechnungCreate: AboRechnungsPositionBisGuthabenCreate) = {
    DB readOnly { implicit session =>
      val abos: List[Abo] = stammdatenReadRepository.getByIds(depotlieferungAboMapping, aboRechnungCreate.ids) :::
        stammdatenReadRepository.getByIds(postlieferungAboMapping, aboRechnungCreate.ids) :::
        stammdatenReadRepository.getByIds(heimlieferungAboMapping, aboRechnungCreate.ids)

      val aboTypen: List[Abotyp] = stammdatenReadRepository.getByIds(abotypMapping, abos.map(_.abotypId))

      val abosWithAboTypen: List[(Abo, Abotyp)] = abos.map { abo =>
        aboTypen.find(_.id == abo.abotypId).map { abotyp => (abo, abotyp) }
      }.flatten

      val (events, failures) = abosWithAboTypen.map {
        case (abo, abotyp) =>

          // TODO check preisEinheit
          if (abotyp.preiseinheit != ProLieferung) {
            Failure(new InvalidStateException(s"Für den Abotyp dieses Abos (${abo.id}) kann keine Guthabenrechngsposition erstellt werden"))
          } else {
            // has to be refactored as soon as more modes are available
            val guthaben = abo match {
              case zusatzAbo: ZusatzAbo =>
                val hauptabo = stammdatenReadRepository.getHauptAbo(zusatzAbo.id)
                hauptabo.get.guthaben
              case abo: HauptAbo => abo.guthaben
              case abo           => throw new InvalidStateException(s"Unexpected abo type found:$abo")
            }
            val anzahlLieferungen = math.max((aboRechnungCreate.bisGuthaben - guthaben), 0)

            if (anzahlLieferungen > 0) {
              val hauptAboBetrag = abo.price.getOrElse(abotyp.preis) * anzahlLieferungen

              val hauptRechnungPosition = createRechnungPositionEvent(abo, aboRechnungCreate.titel, anzahlLieferungen, hauptAboBetrag, aboRechnungCreate
                .waehrung)
              val parentRechnungPositionId = idFactory.newId(RechnungsPositionId.apply)

              Success(List(EntityInsertEvent(parentRechnungPositionId, hauptRechnungPosition)))
            } else {
              Failure(new InvalidStateException(s"Für das Abo mit der Id ${abo.id} wurde keine Rechnungsposition erstellt. Anzahl Lieferungen 0"))
            }
          }
      } partition (_.isSuccess)

      if (events.isEmpty) {
        Failure(new InvalidStateException(s"Keine der RechnungsPositionen konnte erstellt werden"))
      } else {
        Success(events flatMap (_.get))
      }
    }
  }

  def sendEinladung(idFactory: IdFactory, meta: EventTransactionMetadata, kundeId: KundeId, personId: PersonId) = {
    DB readOnly { implicit session =>
      stammdatenReadRepository.getById(personMapping, personId) map { person =>
        person.email map { email =>
          Success(Seq(DefaultResultingEvent(factory => EinladungGesendetEvent(factory.newMetadata(), EinladungCreate(
            idFactory.newId(EinladungId.apply),
            personId,
            UUID.randomUUID.toString,
            DateTime.now.plusDays(config.getIntOption(s"mail.invite-expiration-time-in-days").getOrElse(90)),
            None
          )))))
        } getOrElse {
          Failure(new InvalidStateException(s"Dieser Person kann keine Einladung gesendet werden da sie keine Emailadresse besitzt."))
        }
      } getOrElse {
        Failure(new InvalidStateException(s"Person wurde nicht gefunden."))
      }
    }
  }

  def sendPasswortReset(idFactory: IdFactory, meta: EventTransactionMetadata, personId: PersonId) = {
    DB readOnly { implicit session =>
      stammdatenReadRepository.getById(personMapping, personId) map { person =>
        person.email map { email =>
          Success(Seq(DefaultResultingEvent(factory => PasswortResetGesendetEvent(factory.newMetadata(), EinladungCreate(
            idFactory.newId(EinladungId.apply),
            personId,
            UUID.randomUUID.toString,
            DateTime.now.plusMinutes(config.getIntOption(s"mail.password-reset-message-expiration-time-in-minutes").getOrElse(120)),
            None
          )))))
        } getOrElse {
          Failure(new InvalidStateException(s"Dieser Person kann keine Einladung gesendet werden da sie keine Emailadresse besitzt."))
        }
      } getOrElse {
        Failure(new InvalidStateException(s"Person wurde nicht gefunden."))
      }
    }
  }

  def changeRolle(idFactory: IdFactory, meta: EventTransactionMetadata, kundeId: KundeId, personId: PersonId, rolle: Rolle) = {
    DB readOnly { implicit session =>
      stammdatenReadRepository.getById(personMapping, personId) map { person =>
        person.rolle map { existingRolle =>
          if (existingRolle != rolle) {
            Success(Seq(DefaultResultingEvent(factory => RolleGewechseltEvent(factory.newMetadata(), kundeId, personId, rolle))))
          } else {
            Failure(new InvalidStateException(s"Die Person mit der Id: $personId hat bereits die Rolle: $rolle."))
          }
        } getOrElse {
          Success(Seq(DefaultResultingEvent(factory => RolleGewechseltEvent(factory.newMetadata(), kundeId, personId, rolle))))
        }
      } getOrElse {
        Failure(new InvalidStateException(s"Person wurde nicht gefunden."))
      }
    }
  }

  def updateKunde(idFactory: IdFactory, meta: EventTransactionMetadata, kundeId: KundeId, kunde: KundeModify) = {
    DB readOnly { implicit session =>
      if (isEmailUnique(idFactory, meta, Some(kundeId), kunde)) {
        updateKundeEntity(idFactory, meta.originator, kundeId, kunde)
      } else {
        Failure(new InvalidStateException(s"Die übermittelte E-Mail Adresse wird bereits von einer anderen Person verwendet."))
      }
    }
  }

  def createKunde(idFactory: IdFactory, meta: EventTransactionMetadata, kunde: KundeModify) = {
    DB readOnly { implicit session =>
      if (kunde.ansprechpersonen.isEmpty) {
        Failure(new InvalidStateException(s"Zum Erstellen eines Kunden muss mindestens ein Ansprechpartner angegeben werden"))
      } else {
        if (isEmailUnique(idFactory, meta, None, kunde)) {
          logger.debug(s"created => Insert entity:$kunde")
          val kundeId = idFactory.newId(KundeId.apply)
          val kundeEvent = EntityInsertEvent(kundeId, kunde)

          //Konto daten creation
          val kontoDaten = kunde.kontoDaten match {
            case Some(kd) => KontoDatenModify(kd.iban, kd.bic, None, None, kd.bankName, kd.nameAccountHolder, kd.addressAccountHolder, Some(kundeId), None,
              None, None)
            case None => KontoDatenModify(None, None, None, None, None, None, None, Some(kundeId), None, None, None)
          }
          logger.debug(s"created => Insert entity:$kontoDaten")
          val kontoDatenEvent = EntityInsertEvent(KontoDatenId(kundeId.id), kontoDaten)

          val apartnerEvents = kunde.ansprechpersonen.zipWithIndex.map {
            case (newPerson, index) =>
              val sort = index + 1
              val personCreate = copyTo[PersonModify, PersonCreate](newPerson, "kundeId" -> kundeId, "sort" -> sort)
              logger.debug(s"created => Insert entity:$personCreate")
              EntityInsertEvent(idFactory.newId(PersonId.apply), personCreate)
          }
          Success(kundeEvent +: kontoDatenEvent +: apartnerEvents)
        } else {
          Failure(new InvalidStateException(s"Die übermittelte E-Mail Adresse wird bereits von einer anderen Person verwendet."))
        }
      }
    }
  }

  def createLieferungAbotyp(idFactory: IdFactory, meta: EventTransactionMetadata, lieferungAbotypCreate: LieferungAbotypCreate) = {
    DB readOnly { implicit session =>
      stammdatenReadRepository.getLieferungen(lieferungAbotypCreate.abotypId, lieferungAbotypCreate.vertriebId, lieferungAbotypCreate.datum) match {
        case None => Success(Seq(EntityInsertEvent(idFactory.newId(LieferungId.apply), lieferungAbotypCreate)))
        case _    => Failure(new InvalidStateException("This delivery date for this distribution already exists."))
      }
    }
  }

  def createLieferungenAbotyp(idFactory: IdFactory, meta: EventTransactionMetadata, lieferungenAbotypCreate: LieferungenAbotypCreate) = {
    DB readOnly { implicit session =>
      val events = lieferungenAbotypCreate.daten map { datum: DateTime =>
        stammdatenReadRepository.getLieferungen(lieferungenAbotypCreate.abotypId, lieferungenAbotypCreate.vertriebId, datum) match {
          case None    => Some(EntityInsertEvent(idFactory.newId(LieferungId.apply), LieferungAbotypCreate(lieferungenAbotypCreate.abotypId, lieferungenAbotypCreate.vertriebId, datum)))
          case Some(_) => None
        }
      }
      if (!events.flatten.isEmpty) {
        Success(events.flatten)
      } else {
        Failure(new InvalidStateException("All delivery dates for this distribution already exist."))
      }
    }
  }

  def removeLieferung(idFactory: IdFactory, meta: EventTransactionMetadata, lieferungId: LieferungId) = {
    DB readOnly { implicit session =>
      Success(Seq(EntityDeleteEvent(lieferungId.getLieferungOnLieferplanungId())))
    }
  }

  private def isEmailUnique(idFactory: IdFactory, meta: EventTransactionMetadata, kundeId: Option[KundeId], kunde: KundeModify): Boolean = {
    DB readOnly { implicit session =>
      kunde.ansprechpersonen.map { person =>
        person.email match {
          case Some(email) =>
            if (!email.isEmpty) {
              stammdatenReadRepository.getPersonByEmail(email) match {
                case Some(p) =>
                  kundeId match {
                    case Some(kid) =>
                      if (p.kundeId != kid) {
                        Some(p)
                      } else {
                        None
                      }
                    case _ => Some(p)
                  }
                case _ => None
              }
            } else {
              None
            }
          case _ => None
        }
      }.flatten.isEmpty
    }
  }

  private def getCreateAuslieferungHeimEvent(idFactory: IdFactory, meta: EventTransactionMetadata, lieferplanung: Lieferplanung)(implicit
    personId: PersonId,
    session: DBSession
  ): Seq[ResultingEvent] = {
    val lieferungen = stammdatenReadRepository.getLieferungen(lieferplanung.id)

    //handle Tourenlieferungen: Group all entries with the same TourId on the same Date
    val vertriebsartenDaten = (lieferungen flatMap { lieferung =>
      stammdatenReadRepository.getVertriebsarten(lieferung.vertriebId) collect {
        case h: HeimlieferungDetail =>
          stammdatenReadRepository.getById(tourMapping, h.tourId) map { tour =>
            (h.tourId, tour.name, lieferung.datum) -> h.id
          }
      }
    }).flatten.groupBy(_._1).view.mapValues(_ map {
      _._2
    })

    (vertriebsartenDaten flatMap {
      case ((tourId, tourName, lieferdatum), vertriebsartIds) => {
        //create auslieferungen
        if (!isAuslieferungExistingHeim(lieferdatum, tourId)) {
          val koerbe = stammdatenReadRepository.getKoerbe(lieferdatum, vertriebsartIds, WirdGeliefert)
          if (!koerbe.isEmpty) {
            val tourlieferungen = stammdatenReadRepository.getTourlieferungen(tourId)
            val tourAuslieferung = createTourAuslieferungHeim(idFactory, meta, lieferdatum, tourId, tourName, countHauptAbos(koerbe))
            val updates = koerbe map { korb =>
              // also update the sort of the korb according to the settings made in tour detail
              EntityUpdateEvent(korb.id, KorbAuslieferungModify(tourAuslieferung.id, tourlieferungen find (_.id == korb.aboId) flatMap (_.sort)))
            }
            EntityInsertEvent(tourAuslieferung.id, tourAuslieferung) :: updates
          } else {
            Nil
          }
        } else {
          Nil
        }
      }
    }).toSeq
  }

  private def countHauptAbos(koerbe: List[Korb])(implicit personId: PersonId, session: DBSession): Int = {
    val hauptAboKoerbe = koerbe map { korb =>
      stammdatenReadRepository.getAbo(korb.aboId) match {
        case Some(abo: ZusatzAbo) => None
        case None                 => None
        case _                    => Some(korb)
      }
    }
    hauptAboKoerbe.flatten.size
  }

  private def getCreateDepotAuslieferungAndPostAusliferungEvent(idFactory: IdFactory, meta: EventTransactionMetadata, lieferplanung: Lieferplanung)(implicit personId: PersonId, session: DBSession): Seq[ResultingEvent] = {
    val lieferungen = stammdatenReadRepository.getLieferungen(lieferplanung.id)

    val updates1 = handleLieferplanungAbgeschlossen(idFactory, meta, lieferungen)
    val updates2 = recalculateValuesForLieferplanungAbgeschlossen(lieferungen)
    val updates3 = updateSammelbestellungStatus(lieferungen, lieferplanung)

    updates1 ::: updates2 ::: updates3
  }

  private def handleLieferplanungAbgeschlossen(idFactory: IdFactory, meta: EventTransactionMetadata, lieferungen: List[Lieferung])(implicit
    personId: PersonId,
    session: DBSession
  ): List[ResultingEvent] = {
    //handle Depot- and Postlieferungen: Group all entries with the same VertriebId on the same Date
    val vertriebeDaten = lieferungen.map(l => (l.vertriebId, l.datum)).distinct
    getDepotAuslieferungEvents(idFactory, meta, getDepotAuslieferungAsAMapGrouped(vertriebeDaten)) :::
      getPostAuslieferungEvents(idFactory, meta, getPostAuslieferungAsAMapGrouped(vertriebeDaten))
  }

  private def getDepotAuslieferungAsAMapGrouped(vertriebeDaten: List[(VertriebId, DateTime)])(implicit personId: PersonId, session: DBSession): Map[(DepotId, DateTime), List[DepotlieferungDetail]] = {
    val depotAuslieferungMap = (vertriebeDaten map {
      case (vertriebId, lieferungDatum) => {
        logger.debug(s"handleLieferplanungAbgeschlossen Depot: ${vertriebId}:${lieferungDatum}.")
        val auslieferungL = stammdatenReadRepository.getVertriebsarten(vertriebId)
        auslieferungL.collect {
          case d: DepotlieferungDetail => (lieferungDatum, d)
        }
      }
    }).flatten

    depotAuslieferungMap.groupBy(l => (l._2.depotId, l._1)) map { depotAuslieferung =>
      depotAuslieferung._1 -> depotAuslieferung._2.map(_._2)
    }
  }

  private def getDepotAuslieferungEvents(idFactory: IdFactory, meta: EventTransactionMetadata, depotAuslieferungGroupedMap: Map[(DepotId, DateTime), List[DepotlieferungDetail]])(implicit personId: PersonId, session: DBSession): List[ResultingEvent] = {
    val events = for {
      ((depotId, date), listDepotLieferungDetail) <- depotAuslieferungGroupedMap
    } yield {
      getPostAndDepotAuslieferungEvents(idFactory, meta, date, listDepotLieferungDetail)
    }
    events.flatten.toList
  }

  private def getPostAuslieferungAsAMapGrouped(vertriebeDaten: List[(VertriebId, DateTime)])(implicit personId: PersonId, session: DBSession): Map[DateTime, List[PostlieferungDetail]] = {
    val postAuslieferungMap = (vertriebeDaten map {
      case (vertriebId, lieferungDatum) => {
        logger.debug(s"handleLieferplanungAbgeschlossen (Post): ${vertriebId}:${lieferungDatum}.")
        //create auslieferungen
        val auslieferungL = stammdatenReadRepository.getVertriebsarten(vertriebId)
        auslieferungL.collect {
          case d: PostlieferungDetail => (lieferungDatum, d)
        }
      }
    }).flatten

    postAuslieferungMap.groupBy(l => (l._1)) map { postAuslieferung =>
      postAuslieferung._1 -> postAuslieferung._2.map(_._2)
    }
  }

  private def getPostAuslieferungEvents(idFactory: IdFactory, meta: EventTransactionMetadata, postAuslieferungGroupedMap: Map[DateTime, List[PostlieferungDetail]])(implicit personId: PersonId, session: DBSession): List[ResultingEvent] = {
    val events = for {
      (date, listPostLieferungDetail) <- postAuslieferungGroupedMap
    } yield {
      getPostAndDepotAuslieferungEvents(idFactory: IdFactory, meta, date, listPostLieferungDetail)
    }
    events.flatten.toList
  }

  private def getPostAndDepotAuslieferungEvents(idFactory: IdFactory, meta: EventTransactionMetadata, date: DateTime,
    listVertriebsartDetail: List[VertriebsartDetail])(implicit personId: PersonId, session: DBSession): List[ResultingEvent] = {
    val koerbe = getAllKoerbeForDepotOrPost(date, listVertriebsartDetail)
    if (!koerbe.isEmpty) {
      val newAuslieferung = createAuslieferungDepotPost(idFactory, meta, date, listVertriebsartDetail.head, countHauptAbos(koerbe)).get
      val updates = koerbe map {
        korb => EntityUpdateEvent(korb.id, KorbAuslieferungModify(newAuslieferung.id, None))
      }
      EntityInsertEvent(newAuslieferung.id, newAuslieferung) :: updates
    } else {
      Nil
    }
  }

  private def getAllKoerbeForDepotOrPost(date: DateTime, vertriebsartDetailList: List[VertriebsartDetail])(implicit personId: PersonId, session: DBSession): List[Korb] = {
    val koerbe = vertriebsartDetailList map { vertriebsartDetail =>
      stammdatenReadRepository.getKoerbe(date, vertriebsartDetail.id, WirdGeliefert)
    }
    koerbe.flatten
  }

  private def recalculateValuesForLieferplanungAbgeschlossen(lieferungen: List[Lieferung])(implicit personId: PersonId, session: DBSession): List[ResultingEvent] = {
    //calculate new values
    lieferungen flatMap { lieferung =>
      //calculate total of lieferung
      val total = stammdatenReadRepository.getLieferpositionenByLieferung(lieferung.id).map(_.preis.getOrElse(0.asInstanceOf[BigDecimal])).sum
      val lieferungCopy = lieferung.copy(preisTotal = total, status = Abgeschlossen)
      val lieferungModifyCopy = LieferungAbgeschlossenModify(Abgeschlossen, total)
      val updateLetzteLieferungDateEvent = stammdatenReadRepository.getById(abotypMapping, lieferung.abotypId) map { abotyp =>
        val abotypLetzteLieferungModifyCopy = AbotypLetzteLieferungModify(Some(lieferung.datum))
        EntityUpdateEvent(abotyp.id, abotypLetzteLieferungModifyCopy)
      }

      //update durchschnittspreis
      val updates = (stammdatenReadRepository.getProjekt flatMap { projekt =>
        stammdatenReadRepository.getVertrieb(lieferung.vertriebId) map { vertrieb =>
          val gjKey = projekt.geschaftsjahr.key(lieferung.datum.toLocalDate)

          val lieferungen = vertrieb.anzahlLieferungen.get(gjKey).getOrElse(0)
          val durchschnittspreis: BigDecimal = vertrieb.durchschnittspreis.get(gjKey).getOrElse(0)

          val neuerDurchschnittspreis = calcDurchschnittspreis(durchschnittspreis, lieferungen, total)
          val vertriebCopy = vertrieb.copy(
            anzahlLieferungen = vertrieb.anzahlLieferungen.updated(gjKey, lieferungen + 1),
            durchschnittspreis = vertrieb.durchschnittspreis.updated(gjKey, neuerDurchschnittspreis)
          )
          val vertriebModifyCopy = VertriebRecalculationsModify(vertrieb.anzahlLieferungen, vertrieb.durchschnittspreis)
          EntityUpdateEvent(vertrieb.id, vertriebModifyCopy) :: Nil
        }
      }).getOrElse(Nil)

      EntityUpdateEvent(lieferungCopy.id, lieferungModifyCopy) :: updates ++ updateLetzteLieferungDateEvent
    }
  }

  private def updateSammelbestellungStatus(lieferungen: List[Lieferung], lieferplanung: Lieferplanung)(implicit personId: PersonId, session: DBSession): List[ResultingEvent] = {

    (stammdatenReadRepository.getSammelbestellungen(lieferplanung.id) map {
      sammelbestellung =>
        if (Offen == sammelbestellung.status) {
          val sammelbestellungCopy = sammelbestellung.copy(status = Abgeschlossen)
          val sammelbestellungStatusModifyCopy = SammelbestellungStatusModify(sammelbestellungCopy.status)

          Seq(EntityUpdateEvent(sammelbestellungCopy.id, sammelbestellungStatusModifyCopy))
        } else {
          Nil
        }
    }).filter(_.nonEmpty).flatten
  }

  private def createAuslieferungDepotPost(idFactory: IdFactory, meta: EventTransactionMetadata, lieferungDatum: DateTime, vertriebsart: VertriebsartDetail,
    anzahlKoerbe: Int)(implicit personId: PersonId): Option[Auslieferung] = {
    val auslieferungId = idFactory.newId(AuslieferungId.apply)

    vertriebsart match {
      case d: DepotlieferungDetail =>
        val result = DepotAuslieferung(
          auslieferungId,
          Erfasst,
          d.depotId,
          d.depot.name,
          lieferungDatum,
          anzahlKoerbe,
          meta.timestamp,
          personId,
          meta.timestamp,
          personId
        )
        Some(result)

      case p: PostlieferungDetail =>
        val result = PostAuslieferung(
          auslieferungId,
          Erfasst,
          lieferungDatum,
          anzahlKoerbe,
          meta.timestamp,
          personId,
          meta.timestamp,
          personId
        )
        Some(result)

      case _ =>
        None
    }
  }

  private def getAuslieferungDepotPost(datum: DateTime, vertriebsart: VertriebsartDetail)(implicit session: DBSession): Option[Auslieferung] = {
    vertriebsart match {
      case d: DepotlieferungDetail =>
        stammdatenReadRepository.getDepotAuslieferung(d.depotId, datum)
      case p: PostlieferungDetail =>
        stammdatenReadRepository.getPostAuslieferung(datum)
      case _ =>
        None
    }
  }

  private def isAuslieferungExistingHeim(datum: DateTime, tourId: TourId)(implicit session: DBSession): Boolean = {
    stammdatenReadRepository.getTourAuslieferung(tourId, datum).isDefined
  }

  private def createTourAuslieferungHeim(idFactory: IdFactory, meta: EventTransactionMetadata, lieferungDatum: DateTime, tourId: TourId, tourName: String,
    anzahlKoerbe: Int)(implicit personId: PersonId): TourAuslieferung = {
    val auslieferungId = idFactory.newId(AuslieferungId.apply)
    TourAuslieferung(
      auslieferungId,
      Erfasst,
      tourId,
      tourName,
      lieferungDatum,
      anzahlKoerbe,
      meta.timestamp,
      personId,
      meta.timestamp,
      personId
    )
  }

  private def getDistinctSammelbestellungModifyByLieferplan(lieferplanungId: LieferplanungId)(implicit session: DBSession): Set[SammelbestellungModify] = {
    stammdatenReadRepository.getLieferpositionenByLieferplan(lieferplanungId).map { lieferposition =>
      stammdatenReadRepository.getById(lieferungMapping, lieferposition.lieferungId).map { lieferung =>
        SammelbestellungModify(lieferposition.produzentId, lieferplanungId, lieferung.datum)
      }
    }.flatten.toSet
  }

  private def checkTemplateAbosSubscribers(body: String, subject: String, ids: Seq[AboId])(implicit session: DBSession): Boolean = {
    val templateCorrect = ids flatMap { aboId: AboId =>
      stammdatenReadRepository.getById(depotlieferungAboMapping, aboId) orElse
        stammdatenReadRepository.getById(heimlieferungAboMapping, aboId) orElse
        stammdatenReadRepository.getById(postlieferungAboMapping, aboId) map { abo =>
          stammdatenReadRepository.getPersonen(abo.kundeId) map { person =>
            val personEmailData = copyTo[Person, PersonEmailData](person)
            val mailContext = AboMailContext(personEmailData, abo)
            generateMail(subject, body, mailContext) match {
              case Success(mailPayload) => true
              case Failure(e)           => false
            }
          }
        }
    }
    templateCorrect.flatten.forall(x => x == true)
  }

  private def checkTemplateKunden(body: String, subject: String, ids: Seq[KundeId])(implicit session: DBSession): Boolean = {
    val templateCorrect = ids flatMap { kundeId: KundeId =>
      stammdatenReadRepository.getPersonen(kundeId) flatMap { person =>
        val personData = copyTo[Person, PersonData](person)
        stammdatenReadRepository.getById(kundeMapping, kundeId) map { kunde =>
          val personEmailData = copyTo[Person, PersonEmailData](person)
          val mailContext = KundeMailContext(personEmailData, kunde)
          generateMail(subject, body, mailContext) match {
            case Success(mailPayload) => true
            case Failure(e)           => false
          }
        }
      }
    }
    templateCorrect.forall(x => x == true)
  }

  private def checkTemplateAbotyp(body: String, subject: String, ids: Seq[AbotypId])(implicit session: DBSession): Boolean = {
    val templateCorrect = ids flatMap { abotypId: AbotypId =>
      stammdatenReadRepository.getPersonenForAbotyp(abotypId) flatMap { person =>
        val personData = copyTo[Person, PersonData](person)
        stammdatenReadRepository.getAbotypById(abotypId) map { abotyp =>
          val personEmailData = copyTo[Person, PersonEmailData](person)
          val mailContext = AbotypMailContext(personEmailData, abotyp)
          generateMail(subject, body, mailContext) match {
            case Success(mailPayload) => true
            case Failure(e)           => false
          }
        }
      }
    }
    templateCorrect.forall(x => x == true)
  }

  private def checkTemplateZusatzabotyp(body: String, subject: String, ids: Seq[AbotypId])(implicit session: DBSession): Boolean = {
    val templateCorrect = ids flatMap { abotypId: AbotypId =>
      stammdatenReadRepository.getPersonenForZusatzabotyp(abotypId) flatMap { person =>
        val personData = copyTo[Person, PersonData](person)
        stammdatenReadRepository.getAbotypById(abotypId) map { abotyp =>
          val personEmailData = copyTo[Person, PersonEmailData](person)
          val mailContext = AbotypMailContext(personEmailData, abotyp)
          generateMail(subject, body, mailContext) match {
            case Success(mailPayload) => true
            case Failure(e)           => false
          }
        }
      }
    }
    templateCorrect.forall(x => x == true)
  }

  private def checkTemplateTour(body: String, subject: String, ids: Seq[TourId])(implicit session: DBSession): Boolean = {
    val templateCorrect = ids flatMap { tourId: TourId =>
      stammdatenReadRepository.getPersonen(tourId) flatMap { person =>
        val personData = copyTo[Person, PersonData](person)
        stammdatenReadRepository.getById(tourMapping, tourId) map { tour =>
          val personEmailData = copyTo[Person, PersonEmailData](person)
          val mailContext = TourMailContext(personEmailData, tour)
          generateMail(subject, body, mailContext) match {
            case Success(mailPayload) => true
            case Failure(e)           => false
          }
        }
      }
    }
    templateCorrect.forall(x => x == true)
  }

  private def checkTemplateDepot(body: String, subject: String, ids: Seq[DepotId])(implicit session: DBSession): Boolean = {
    val templateCorrect = ids flatMap { depotId: DepotId =>
      stammdatenReadRepository.getPersonen(depotId) flatMap { person =>
        val personData = copyTo[Person, PersonData](person)
        stammdatenReadRepository.getById(depotMapping, depotId) map { depot =>
          val personEmailData = copyTo[Person, PersonEmailData](person)
          val mailContext = DepotMailContext(personEmailData, depot)
          generateMail(subject, body, mailContext) match {
            case Success(mailPayload) => true
            case Failure(e)           => false
          }
        }
      }
    }
    templateCorrect.forall(x => x == true)
  }

  private def checkTemplatePersonen(body: String, subject: String, ids: Seq[PersonId])(implicit session: DBSession): Boolean = {
    val templateCorrect = ids flatMap { personId: PersonId =>
      stammdatenReadRepository.getById(personMapping, personId) map { person =>
        val personEmailData = copyTo[Person, PersonEmailData](person)
        val mailContext = PersonMailContext(personEmailData)
        generateMail(subject, body, mailContext) match {
          case Success(mailPayload) => true
          case Failure(e)           => false
        }
      }
    }
    templateCorrect.forall(x => x == true)
  }
}

class DefaultStammdatenCommandHandler(override val sysConfig: SystemConfig, override val system: ActorSystem, override val mailService: ActorRef) extends StammdatenCommandHandler
  with DefaultStammdatenReadRepositorySyncComponent
  with DefaultMailCommandForwarderComponent
  with MailServiceReference {
  override implicit protected val executionContext: ExecutionContext = system.dispatcher

  override def projektReadRepository = stammdatenReadRepository
}
