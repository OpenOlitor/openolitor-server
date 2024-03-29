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

import ch.openolitor.core.models._
import ch.openolitor.stammdaten.models._
import ch.openolitor.stammdaten.repositories._
import ch.openolitor.util.IdUtil
import ch.openolitor.core.repositories.EventPublisher
import ch.openolitor.core.exceptions._
import scala.collection._
import org.joda.time.DateTime
import com.github.nscala_time.time.Imports._
import scalikejdbc._
import ch.openolitor.core.Macros._

trait KorbHandler extends KorbStatusHandler
  with StammdatenDBMappings {
  this: StammdatenWriteRepositoryComponent =>

  /**
   * insert or update Korb
   * @return (created/updated, existing)
   */
  def upsertKorb(lieferung: Lieferung, abo: Abo, abotyp: IAbotyp)(implicit personId: PersonId, session: DBSession, publisher: EventPublisher): (Option[Korb], Option[Korb]) = {
    logger.debug(s"upsertKorb lieferung: $Lieferung abo: $abo abotyp: $abotyp")
    stammdatenWriteRepository.getKorb(lieferung.id, abo.id) match {
      case None if (lieferung.lieferplanungId.isDefined) =>
        val (status, guthaben) = calculateStatusGuthaben(abo, lieferung, abotyp)
        val korbId = KorbId(IdUtil.positiveRandomId)
        val korb = Korb(
          korbId,
          lieferung.id,
          abo.id,
          status,
          guthaben,
          None,
          None,
          DateTime.now,
          personId,
          DateTime.now,
          personId
        )
        (stammdatenWriteRepository.insertEntity[Korb, KorbId](korb), None)
      case None =>
        // do nothing (lieferung hast not been planned yet)
        (None, None)
      case Some(korb) =>
        val abwCount = stammdatenWriteRepository.countAbwesend(lieferung.id, abo.id)
        val (status, guthaben) = calculateStatusGuthaben(abo, lieferung, abotyp)

        val copy = korb.copy(
          status = status,
          guthabenVorLieferung = guthaben
        )

        // only update if changed
        if (korb != copy) {
          (stammdatenWriteRepository.updateEntity[Korb, KorbId](korb.id)(
            korbMapping.column.status -> status,
            korbMapping.column.guthabenVorLieferung -> guthaben
          ), Some(korb))
        } else {
          (Some(korb), Some(korb))
        }
    }
  }

  private def calculateStatusGuthaben(abo: Abo, lieferung: Lieferung, abotyp: IAbotyp)(implicit personId: PersonId, session: DBSession, publisher: EventPublisher): (KorbStatus, Int) = {
    abo match {
      case zusatzAbo: ZusatzAbo =>
        stammdatenWriteRepository.getHauptAbo(zusatzAbo.id) match {
          case Some(mainAbo) =>
            val hauptabotyp = stammdatenWriteRepository.getAbotypDetail(zusatzAbo.hauptAbotypId)
            val abwCount = stammdatenWriteRepository.countAbwesend(mainAbo.id, lieferung.datum.toLocalDate)
            val guthabenVorLieferung = stammdatenWriteRepository.getKorbLatestWirdGeliefert(zusatzAbo.id, lieferung.datum) match {
              case Some(korb) => (korb.guthabenVorLieferung - 1)
              case None       => mainAbo.guthaben
            }
            (calculateKorbStatus(abwCount, mainAbo.guthaben, hauptabotyp.get.guthabenMindestbestand), guthabenVorLieferung)
          case None =>
            logger.error(s"There is no main subscription for the  : ${abo.id}")
            throw new InvalidStateException(s"calculateStatusGuthaben: There is no main subscription for the  ${abo.id}")
        }
      case abo: HauptAbo =>
        val abwCount = stammdatenWriteRepository.countAbwesend(lieferung.id, abo.id)
        abotyp match {
          case hauptabotyp: Abotyp =>
            val guthabenVorLieferung = stammdatenWriteRepository.getKorbLatestWirdGeliefert(abo.id, lieferung.datum) match {
              case Some(korb) =>
                (korb.guthabenVorLieferung - 1)
              case None => abo.guthaben
            }
            (calculateKorbStatus(abwCount, abo.guthaben, hauptabotyp.guthabenMindestbestand), guthabenVorLieferung)
          case _ =>
            logger.error(s"calculateStatusGuthaben: Abotype of Hauptabo must never be a ZusatzAbotyp. Is the case for abo: ${abo.id}")
            throw new InvalidStateException(s"calculateStatusGuthaben: Abotype of Hauptabo must never be a ZusatzAbotyp. Is the case for abo: ${abo.id}")
        }
      case abo =>
        throw new InvalidStateException(s"Unexpected abo:$abo")
    }
  }

  def vertriebInDelivery(datum: DateTime)(implicit personId: PersonId, session: DBSession, publisher: EventPublisher): List[Vertrieb] = {
    stammdatenWriteRepository.getVertriebByDate(datum)
  }

  def adjustOpenLieferplanung(zusatzAboId: AboId)(implicit personId: PersonId, session: DBSession, publisher: EventPublisher): Unit = {
    logger.debug(s"adjustOpenLieferplanung => zusatzAboId = $zusatzAboId")
    val dateFormat = DateTimeFormat.forPattern("dd.MM.yyyy")
    val project = stammdatenWriteRepository.getProjekt
    // for all the lieferplanung that are opened
    stammdatenWriteRepository.getOpenLieferplanung map { lieferplanung =>
      val abotypDepotTour = stammdatenWriteRepository.getLieferungen(lieferplanung.id) map { lieferung =>
        stammdatenWriteRepository.getAbo(zusatzAboId) match {
          case Some(zusatzabo: ZusatzAbo) => {
            // in case there is not programmed korb for the hauptAbo or it is not plan to be delivered
            stammdatenWriteRepository.getKorb(lieferung.id, zusatzabo.hauptAboId) match {
              case Some(hauptAboKorb) => {
                stammdatenWriteRepository.getExistingZusatzaboLieferung(zusatzabo.abotypId, lieferplanung.id, lieferung.datum) match {
                  case None => {
                    // Using positiveRandomId because the lieferung cannot be created in commandHandler.
                    createLieferungInner(LieferungId(IdUtil.positiveRandomId), LieferungAbotypCreate(zusatzabo.abotypId, lieferung.vertriebId, lieferung.datum), Some(lieferplanung.id)).map { zusatzLieferung =>
                      offenLieferung(lieferplanung.id, project, zusatzLieferung, None)
                    }
                  }
                  case Some(zusatzLieferung) => {
                    offenLieferung(lieferplanung.id, project, zusatzLieferung, None)
                  }
                }
              }
              case None =>
            }
          }
          case Some(_) =>
          case None    =>
        }
        (dateFormat.print(lieferung.datum), lieferung.abotypBeschrieb)
      }
      val abotypDates = (abotypDepotTour.groupBy(_._1).view.mapValues(_ map { _._2 }) map {
        case (datum, abotypBeschrieb) =>
          datum + ": " + abotypBeschrieb.mkString(", ")
      }).mkString("; ")

      //update lieferplanung
      stammdatenWriteRepository.updateEntity[Lieferplanung, LieferplanungId](lieferplanung.id)(
        lieferplanungMapping.column.abotypDepotTour -> abotypDates
      )
    }
  }

  def defineLieferplanungDescription(lieferplanung: Lieferplanung)(implicit personId: PersonId, session: DBSession, publisher: EventPublisher) = {
    val dateFormat = DateTimeFormat.forPattern("dd.MM.yyyy")
    val abotypDepotTour = stammdatenWriteRepository.getLieferungenNext() map { lieferung =>
      (dateFormat.print(lieferung.datum), lieferung.abotypBeschrieb)
    }

    val abotypDates = (abotypDepotTour.groupBy(_._1).view.mapValues(_ map { _._2 }) map {
      case (datum, abotypBeschrieb) =>
        datum + ": " + abotypBeschrieb.mkString(", ")
    }).mkString("; ")

    //update lieferplanung
    stammdatenWriteRepository.updateEntity[Lieferplanung, LieferplanungId](lieferplanung.id)(
      lieferplanungMapping.column.abotypDepotTour -> abotypDates
    )
  }

  def createKoerbe(lieferung: Lieferung, vertriebId: Option[VertriebId])(implicit personId: PersonId, session: DBSession, publisher: EventPublisher): Option[Lieferung] = {
    logger.debug(s"Create Koerbe => lieferung : ${lieferung}")
    val vertriebList = vertriebInDelivery(lieferung.datum)
    val ret: Option[Option[Lieferung]] = stammdatenWriteRepository.getAbotypById(lieferung.abotypId) map { abotyp =>
      lieferung.lieferplanungId.map { lieferplanungId =>
        val abos = vertriebId match {
          case Some(vId) =>
            stammdatenWriteRepository.getAktiveZusatzAbos(lieferung.abotypId, vId, lieferung.datum, lieferplanungId)
          case None =>
            stammdatenWriteRepository.getAktiveAbos(lieferung.abotypId, lieferung.vertriebId, lieferung.datum, lieferplanungId)
        }
        abos map { abo =>
          if (vertriebList.exists { vertrieb => vertrieb.id == abo.vertriebId }) {
            upsertKorb(lieferung, abo, abotyp)
          } else { (None, None) }
        }
        recalculateNumbersLieferung(lieferung)
      }
    }
    ret.flatten match {
      case Some(lieferung) =>
        if (lieferung.anzahlSaldoZuTief == 0 && lieferung.anzahlAbwesenheiten == 0 && lieferung.anzahlKoerbeZuLiefern == 0) {
          stammdatenWriteRepository.deleteEntity[Lieferung, LieferungId](lieferung.id)
          None
        } else Some(lieferung)
      case None => None
    }
  }

  def updateLieferungUndZusatzLieferung(lieferplanungId: LieferplanungId, project: Option[Projekt], hauptAboLieferung: Lieferung)(implicit personId: PersonId, session: DBSession, publisher: EventPublisher): Option[Lieferung] = {
    offenLieferung(lieferplanungId, project, hauptAboLieferung, None) match {
      case Some(adjustedLieferung) =>
        stammdatenWriteRepository.getExistingZusatzAbotypen(adjustedLieferung.id).map { zusatzAbotyp =>
          stammdatenWriteRepository.getExistingZusatzaboLieferung(zusatzAbotyp.id, lieferplanungId, hauptAboLieferung.datum) match {
            case None => {
              // Using positiveRandomId because the lieferung cannot be created in commandHandler.
              createLieferungInner(LieferungId(IdUtil.positiveRandomId), LieferungAbotypCreate(zusatzAbotyp.id, adjustedLieferung.vertriebId, adjustedLieferung.datum), Some(lieferplanungId)).map { zusatzLieferung =>
                offenLieferung(lieferplanungId, project, zusatzLieferung, None)
              }
            }
            case Some(zusatzLieferung) => offenLieferung(lieferplanungId, project, zusatzLieferung, Some(adjustedLieferung.vertriebId))
          }
        }
        Some(adjustedLieferung)
      case None => None
    }
  }

  private def offenLieferung(lieferplanungId: LieferplanungId, project: Option[Projekt], lieferung: Lieferung, hauptAboVertriebId: Option[VertriebId])(implicit personId: PersonId, session: DBSession, publisher: EventPublisher): Option[Lieferung] = {
    logger.debug(s" offenLieferung : lieferplanungId : $lieferplanungId project : $project lieferung : $lieferung")
    val (newDurchschnittspreis, newAnzahlLieferungen) = stammdatenWriteRepository.getGeplanteLieferungVorher(lieferung.vertriebId, lieferung.abotypId, lieferung.datum) match {
      case Some(lieferungVorher) if project.get.geschaftsjahr.isInSame(lieferungVorher.datum.toLocalDate(), lieferung.datum.toLocalDate()) =>
        val sum = stammdatenWriteRepository.sumPreisTotalGeplanteLieferungenVorher(lieferung.vertriebId, lieferung.abotypId, lieferung.datum, project.get.geschaftsjahr.start(lieferung.datum.toLocalDate()).toDateTimeAtCurrentTime()).getOrElse(BigDecimal(0))

        val durchschnittspreisBisher: BigDecimal = lieferungVorher.anzahlLieferungen match {
          case 0 => BigDecimal(0)
          case _ => sum / lieferungVorher.anzahlLieferungen
        }
        val anzahlLieferungenNeu = lieferungVorher.anzahlLieferungen + 1
        (durchschnittspreisBisher, anzahlLieferungenNeu)
      case _ =>
        (BigDecimal(0), 1)
    }

    val now = DateTime.now
    val updatedLieferung = lieferung.copy(
      lieferplanungId = Some(lieferplanungId),
      status = Offen,
      durchschnittspreis = newDurchschnittspreis,
      anzahlLieferungen = newAnzahlLieferungen,
      modifidat = now,
      modifikator = personId
    )

    //create koerbe
    createKoerbe(updatedLieferung, hauptAboVertriebId) match {
      case Some(adjustedLieferung) =>
        stammdatenWriteRepository.updateEntity[Lieferung, LieferungId](adjustedLieferung.id)(
          lieferungMapping.column.status -> adjustedLieferung.status,
          lieferungMapping.column.durchschnittspreis -> adjustedLieferung.durchschnittspreis,
          lieferungMapping.column.anzahlLieferungen -> adjustedLieferung.anzahlLieferungen,
          lieferungMapping.column.anzahlKoerbeZuLiefern -> adjustedLieferung.anzahlKoerbeZuLiefern,
          lieferungMapping.column.anzahlAbwesenheiten -> adjustedLieferung.anzahlAbwesenheiten,
          lieferungMapping.column.anzahlSaldoZuTief -> adjustedLieferung.anzahlSaldoZuTief,
          lieferungMapping.column.lieferplanungId -> lieferplanungId
        )
        Some(adjustedLieferung)
      case None => None
    }
  }

  def createLieferungInner(id: LieferungId, lieferung: LieferungAbotypCreate, lieferplanungId: Option[LieferplanungId])(implicit personId: PersonId, session: DBSession, publisher: EventPublisher): Option[Lieferung] = {
    logger.debug(s"createLieferungInner LieferungId : $id lieferung : $lieferung lieferplanungId : $lieferplanungId")
    stammdatenWriteRepository.getAbotypById(lieferung.abotypId) flatMap { abotyp =>
      stammdatenWriteRepository.getById(vertriebMapping, lieferung.vertriebId) flatMap {
        vertrieb =>
          val vBeschrieb = vertrieb.beschrieb
          val atBeschrieb = abotyp.name
          val now = DateTime.now
          val ZERO = 0

          val insert = copyTo[LieferungAbotypCreate, Lieferung](lieferung, "id" -> id,
            "abotypBeschrieb" -> atBeschrieb,
            "vertriebId" -> vertrieb.id,
            "vertriebBeschrieb" -> vBeschrieb,
            "anzahlAbwesenheiten" -> ZERO,
            "durchschnittspreis" -> ZERO,
            "anzahlLieferungen" -> ZERO,
            "anzahlKoerbeZuLiefern" -> ZERO,
            "anzahlSaldoZuTief" -> ZERO,
            "zielpreis" -> abotyp.zielpreis,
            "preisTotal" -> ZERO,
            "status" -> Ungeplant,
            "lieferplanungId" -> lieferplanungId,
            "erstelldat" -> now,
            "ersteller" -> personId,
            "modifidat" -> now,
            "modifikator" -> personId)

          stammdatenWriteRepository.insertEntity[Lieferung, LieferungId](insert)
      }
    }
  }

  def deleteKorb(lieferung: Lieferung, abo: Abo)(implicit personId: PersonId, session: DBSession, publisher: EventPublisher): Option[Korb] = {
    logger.debug(s"deleteKorb lieferung: $lieferung abo: $abo")
    abo match {
      case _: ZusatzAbo =>
      case _ => {
        lieferung.lieferplanungId map { lieferplanungId =>
          stammdatenWriteRepository.getZusatzAbosByHauptAbo(abo.id) flatMap { zusatzabo =>
            stammdatenWriteRepository.getExistingZusatzaboLieferung(zusatzabo.abotypId, lieferplanungId, lieferung.datum) map { zusatzAboLieferung =>
              stammdatenWriteRepository.getKorb(zusatzAboLieferung.id, zusatzabo.id) flatMap { korb =>
                stammdatenWriteRepository.deleteEntity[Korb, KorbId](korb.id)
              }
              recalculateNumbersLieferung(zusatzAboLieferung)
            }
          }
        }
      }
    }
    stammdatenWriteRepository.getKorb(lieferung.id, abo.id) flatMap { korb =>
      stammdatenWriteRepository.deleteEntity[Korb, KorbId](korb.id)
    }
  }

  def modifyKoerbeForAboVertriebChange(abo: HauptAbo, orig: Option[HauptAbo])(implicit personId: PersonId, session: DBSession, publisher: EventPublisher): Unit = {
    logger.debug(s"modifyKoerbeForAboVertriebChange abo: $abo orig: $orig")
    for {
      originalAbo <- orig
      if (abo.vertriebId != originalAbo.vertriebId)
      abotyp <- stammdatenWriteRepository.getAbotypById(abo.abotypId)
    } yield {
      /* create or delete basket for the main abo*/
      val offenLieferungenForMainAbo = stammdatenWriteRepository.getLieferungenOffenByAbotyp(abo.abotypId)
      offenLieferungenForMainAbo map { lieferung =>
        if (lieferung.vertriebId == originalAbo.vertriebId) {
          deleteKorb(lieferung, originalAbo)
        }
        if (lieferung.vertriebId == abo.vertriebId) {
          upsertKorb(lieferung, abo, abotyp)
        }
      }

      /* create or delete basket for the zusatzabo*/
      stammdatenWriteRepository.getZusatzAbosByHauptAbo(abo.id).filter(z => z.aktiv) map { zusatzabo =>
        stammdatenWriteRepository.getZusatzAbotypDetail(zusatzabo.abotypId) map {
          zusatzabotyp =>
            val offenLieferungenForZusatzabo = stammdatenWriteRepository.getLieferungenOffenByAbotyp(zusatzabo.abotypId)
            offenLieferungenForZusatzabo map { lieferung =>
              if (lieferung.vertriebId == originalAbo.vertriebId) {
                deleteKorb(lieferung, zusatzabo)
              }
              //check if the zusatzlieferung is programmed for the main lieferung. In other cases we don't even try to create a basket for the zusatzabo
              offenLieferungenForMainAbo.filter(l => l.lieferplanungId == lieferung.lieferplanungId) map {
                mainAboLieferung =>
                  stammdatenWriteRepository.getKorb(mainAboLieferung.id, abo.id) match {
                    case Some(_) =>
                      if (lieferung.abotypId == zusatzabo.abotypId) {
                        upsertKorb(lieferung, zusatzabo, zusatzabotyp)
                      } else if (offenLieferungenForZusatzabo.filter(l => l.abotypId == zusatzabo.abotypId).isEmpty) {
                        adjustOpenLieferplanung(zusatzabo.id)
                      }
                      recalculateNumbersLieferung(lieferung)
                    case None =>
                  }
              }
            }
        }
      }
      offenLieferungenForMainAbo.map(recalculateNumbersLieferung(_))
    }
  }

  def modifyKoerbeForAboDatumChange(abo: Abo, orig: Option[Abo])(implicit personId: PersonId, session: DBSession, publisher: EventPublisher): Unit = {
    logger.debug(s"modifyKoerbeForAboDatumChange abo: $abo orig: $orig")
    for {
      originalAbo <- orig
      // only modify koerbe if the start or end of this abo has changed or we're creating them for a new abo
      if (abo.start != originalAbo.start || abo.ende != originalAbo.ende)
      abotyp <- stammdatenWriteRepository.getAbotypById(abo.abotypId)
    } yield {
      stammdatenWriteRepository.getLieferungenOffenByAbotyp(abo.abotypId).map { lieferung =>
        if ((abo.start > lieferung.datum.toLocalDate || (abo.ende map (_ <= (lieferung.datum.toLocalDate - 1.day)) getOrElse false))) {
          deleteKorb(lieferung, abo)
        } else if ((abo.start <= lieferung.datum.toLocalDate) &&
          (abo.ende map (_ >= lieferung.datum.toLocalDate) getOrElse true)) {
          if (abo.vertriebId != lieferung.vertriebId) {
            deleteKorb(lieferung, originalAbo)
          } else {
            upsertKorb(lieferung, abo, abotyp)
            stammdatenWriteRepository.getZusatzAbosByHauptAbo(abo.id) map { zusatzabo =>
              adjustOpenLieferplanung(zusatzabo.id)
            }
          }
        }
        recalculateNumbersLieferung(lieferung)
      }
    }
  }

  def recalculateNumbersLieferung(lieferung: Lieferung)(implicit personId: PersonId, session: DBSession, publisher: EventPublisher): Lieferung = {
    logger.debug(s"recalculateNumbersLieferung lieferung: $lieferung")
    val stati: List[KorbStatus] = stammdatenWriteRepository.getKoerbe(lieferung.id).map(_.status)
    val counts: Map[KorbStatus, Int] = stati.groupBy {
      s => s
    }.view.mapValues(_.size).toMap

    val zuLiefern: Int = counts.getOrElse(WirdGeliefert, 0)
    val abwesenheiten: Int = counts.getOrElse(FaelltAusAbwesend, 0)
    val saldoZuTief: Int = counts.getOrElse(FaelltAusSaldoZuTief, 0)
    stammdatenWriteRepository.updateEntity[Lieferung, LieferungId](lieferung.id)(
      lieferungMapping.column.anzahlKoerbeZuLiefern -> zuLiefern,
      lieferungMapping.column.anzahlAbwesenheiten -> abwesenheiten,
      lieferungMapping.column.anzahlSaldoZuTief -> saldoZuTief
    )

    lieferung.copy(
      anzahlKoerbeZuLiefern = zuLiefern,
      anzahlAbwesenheiten = abwesenheiten,
      anzahlSaldoZuTief = saldoZuTief
    )
  }

  def createKoerbeForNewAbo(abo: Abo)(implicit personId: PersonId, session: DBSession, publisher: EventPublisher) = {
    stammdatenWriteRepository.getById(abotypMapping, abo.abotypId) map { abotyp =>
      stammdatenWriteRepository.getLieferungenOffenByVertrieb(abo.vertriebId) map { lieferung =>
        if (abo.abotypId == lieferung.abotypId) {
          if (abo.start <= lieferung.datum.toLocalDate && (abo.ende map (_ >= lieferung.datum.toLocalDate) getOrElse true)) {
            upsertKorb(lieferung, abo, abotyp) match {
              case (Some(created), None) =>
                // nur im created Fall muss eins dazu gezählt werden
                // bei Statuswechsel des Korbs wird handleKorbStatusChanged die Counts justieren
                recalculateNumbersLieferung(lieferung)
              case _ =>
              // counts werden andersweitig angepasst
            }
          }
        }
      }
    }
  }
}
