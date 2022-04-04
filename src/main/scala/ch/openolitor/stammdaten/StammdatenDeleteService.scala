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

import akka.actor._
import scalikejdbc._
import ch.openolitor.core._
import ch.openolitor.core.models._
import ch.openolitor.core.db._
import ch.openolitor.core.domain._
import ch.openolitor.stammdaten.models._
import ch.openolitor.stammdaten.repositories._
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.domain.EntityStore._
import ch.openolitor.core.repositories.EventPublishingImplicits._
import ch.openolitor.core.repositories.EventPublisher
import org.joda.time.LocalDate

object StammdatenDeleteService {
  def apply(implicit sysConfig: SystemConfig, system: ActorSystem): StammdatenDeleteService = new DefaultStammdatenDeleteService(sysConfig, system)
}

class DefaultStammdatenDeleteService(sysConfig: SystemConfig, override val system: ActorSystem)
  extends StammdatenDeleteService(sysConfig: SystemConfig) with DefaultStammdatenWriteRepositoryComponent {
}

/**
 * Actor zum Verarbeiten der Delete Anweisungen für das Stammdaten Modul
 */
class StammdatenDeleteService(override val sysConfig: SystemConfig) extends EventService[EntityDeletedEvent[_ <: BaseId]]
  with LazyLogging with AsyncConnectionPoolContextAware with StammdatenDBMappings with KorbHandler {
  self: StammdatenWriteRepositoryComponent =>
  import EntityStore._

  // implicitly expose the eventStream
  implicit lazy val stammdatenRepositoryImplicit = stammdatenWriteRepository

  val ZERO = 0

  val stammdatenDeleteHandle: Handle = {
    case EntityDeletedEvent(meta, id: AbotypId) => deleteAbotyp(meta, id)
    case EntityDeletedEvent(meta, id: AbwesenheitId) => deleteAbwesenheit(meta, id)
    case EntityDeletedEvent(meta, id: PersonId) => deletePerson(meta, id)
    case EntityDeletedEvent(meta, id: PersonCategoryId) => deletePersonCategory(meta, id)
    case EntityDeletedEvent(meta, id: PendenzId) => deletePendenz(meta, id)
    case EntityDeletedEvent(meta, id: KundeId) => deleteKunde(meta, id)
    case EntityDeletedEvent(meta, id: DepotId) => deleteDepot(meta, id)
    case EntityDeletedEvent(meta, id: AboId) => deleteAbo(meta, id)
    case EntityDeletedEvent(meta, id: VertriebsartId) => deleteVertriebsart(meta, id)
    case EntityDeletedEvent(meta, id: LieferungId) => deleteLieferung(meta, id)
    case EntityDeletedEvent(meta, id: CustomKundentypId) => deleteKundentyp(meta, id)
    case EntityDeletedEvent(meta, id: ProduktId) => deleteProdukt(meta, id)
    case EntityDeletedEvent(meta, id: ProduktekategorieId) => deleteProduktekategorie(meta, id)
    case EntityDeletedEvent(meta, id: ProduktProduktekategorieId) => deleteProduktProduktekategorie(meta, id)
    case EntityDeletedEvent(meta, id: ProduzentId) => deleteProduzent(meta, id)
    case EntityDeletedEvent(meta, id: TourId) => deleteTour(meta, id)
    case EntityDeletedEvent(meta, id: VertriebId) => deleteVertrieb(meta, id)
    case EntityDeletedEvent(meta, id: LieferplanungId) => deleteLieferplanung(meta, id)
    case EntityDeletedEvent(meta, id: ProjektVorlageId) => deleteProjektVorlage(meta, id)
    case EntityDeletedEvent(meta, id: LieferungOnLieferplanungId) => removeLieferungPlanung(meta, id)
    case EntityDeletedEvent(meta, id: KorbId) => deleteKorb(meta, id)
    case e =>
  }

  val handle: Handle = stammdatenDeleteHandle

  def deleteAbotyp(meta: EventMetadata, id: AbotypId)(implicit personId: PersonId = meta.originator) = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      val maybeAbotyp: Option[IAbotyp] = stammdatenWriteRepository.deleteEntity[Abotyp, AbotypId](id, { abotyp: Abotyp => abotyp.anzahlAbonnenten == 0 && abotyp.anzahlAbonnentenAktiv == 0 }) orElse
        stammdatenWriteRepository.deleteEntity[ZusatzAbotyp, AbotypId](id, { zusatzabotyp: ZusatzAbotyp => zusatzabotyp.anzahlAbonnenten == 0 && zusatzabotyp.anzahlAbonnentenAktiv == 0 })

      maybeAbotyp match {
        case Some(abotyp) =>
          stammdatenWriteRepository.getVertriebe(abotyp.id) map { vertrieb =>
            noSessionDeleteVertrieb(vertrieb.id)
          }
        case None =>
      }
    }
  }

  def deleteAbwesenheit(meta: EventMetadata, id: AbwesenheitId)(implicit personId: PersonId = meta.originator) = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.deleteEntity[Abwesenheit, AbwesenheitId](id)
    }
  }

  def deletePerson(meta: EventMetadata, id: PersonId)(implicit personId: PersonId = meta.originator) = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      noSessionDeletePerson(id)
    }
  }

  def deletePersonCategory(meta: EventMetadata, id: PersonCategoryId)(implicit personId: PersonId = meta.originator) = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.deleteEntity[PersonCategory, PersonCategoryId](id)
    }
  }

  private def noSessionDeletePerson(id: PersonId)(implicit personId: PersonId, session: DBSession, publisher: EventPublisher) = {
    stammdatenWriteRepository.deleteEntity[Person, PersonId](id)
  }

  def deleteKontoDaten(meta: EventMetadata, id: KontoDatenId)(implicit personId: PersonId = meta.originator) = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.deleteEntity[KontoDaten, KontoDatenId](id)
    }
  }

  private def noSessionDeleteKontoDaten(id: KontoDatenId)(implicit personId: PersonId, session: DBSession, publisher: EventPublisher) = {
    stammdatenWriteRepository.deleteEntity[KontoDaten, KontoDatenId](id)
  }

  def deleteKunde(meta: EventMetadata, kundeId: KundeId)(implicit personId: PersonId = meta.originator) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.deleteEntity[Kunde, KundeId](kundeId, { kunde: Kunde => kunde.anzahlAbos == 0 && kunde.anzahlAbosAktiv == 0 }) match {
        case Some(kunde) =>
          //delete all personen as well
          stammdatenWriteRepository.getPersonen(kundeId).map(person => noSessionDeletePerson(person.id))
          stammdatenWriteRepository.getKontoDatenKunde(kundeId).map(kontoDaten => noSessionDeleteKontoDaten(kontoDaten.id))
          //delete all pendenzen as well
          stammdatenWriteRepository.getPendenzen(kundeId).map(pendenz => noSessionDeletePendenz(pendenz.id))
        case None =>
      }

    }
  }

  def deleteDepot(meta: EventMetadata, id: DepotId)(implicit personId: PersonId = meta.originator) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.deleteEntity[Depot, DepotId](id, { depot: Depot => depot.anzahlAbonnenten == 0 && depot.anzahlAbonnentenAktiv == 0 }) map { depot =>
        stammdatenWriteRepository.getDepotlieferung(depot.id) map { dl =>
          stammdatenWriteRepository.deleteEntity[Depotlieferung, VertriebsartId](dl.id, { vertriebsart: Vertriebsart => vertriebsart.anzahlAbos == 0 && vertriebsart.anzahlAbosAktiv == 0 })
        }
      }
    }
  }

  def deletePendenz(meta: EventMetadata, id: PendenzId)(implicit personId: PersonId = meta.originator) = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      noSessionDeletePendenz(id)
    }
  }

  private def noSessionDeletePendenz(id: PendenzId)(implicit personId: PersonId, session: DBSession, publisher: EventPublisher) = {
    stammdatenWriteRepository.deleteEntity[Pendenz, PendenzId](id)
  }

  def deleteAbo(meta: EventMetadata, id: AboId)(implicit personId: PersonId = meta.originator) = {
    logger.debug(s"deleteAbo => aboId: $id")
    DB localTxPostPublish { implicit session => implicit publisher =>
      val maybeAbo: Option[Abo] = stammdatenWriteRepository.deleteEntity[DepotlieferungAbo, AboId](id) orElse
        stammdatenWriteRepository.deleteEntity[HeimlieferungAbo, AboId](id) orElse
        stammdatenWriteRepository.deleteEntity[PostlieferungAbo, AboId](id) orElse
        stammdatenWriteRepository.deleteEntity[ZusatzAbo, AboId](id)

      // also delete mapped zusatzabos if it's a main abo
      maybeAbo map {
        case abo: ZusatzAbo => deleteKoerbeForDeletedAbo(abo)
        case _ => {
          stammdatenWriteRepository.getZusatzAbosByHauptAbo(id).map(zusatzabo =>
            deleteKoerbeForDeletedAbo(zusatzabo))

          stammdatenWriteRepository.deleteZusatzAbos(id)
        }
      }

      // also delete corresponding Tourlieferung
      stammdatenWriteRepository.deleteEntity[Tourlieferung, AboId](id)

      // also delete related Korbe
      maybeAbo map (deleteKoerbeForDeletedAbo)
      maybeAbo
    }
  }

  private def deleteKoerbeForDeletedAbo(abo: Abo)(implicit personId: PersonId, session: DBSession, publisher: EventPublisher) = {
    logger.debug(s"deleteKoerbeForDeletedAbo => abo: $abo")
    // koerbe der offenen lieferungen loeschen
    val plannedLieferung = stammdatenWriteRepository.getLieferungenOffenByAbotyp(abo.abotypId).filter(_.lieferplanungId != None)
    plannedLieferung map { lieferung =>
      deleteKorb(lieferung, abo)
      recalculateNumbersLieferung(lieferung)
      val lieferplanung = stammdatenWriteRepository.getById[Lieferplanung, LieferplanungId](lieferplanungMapping, lieferung.lieferplanungId.get)
      defineLieferplanungDescription(lieferplanung.get)
    }
  }

  def deleteKundentyp(meta: EventMetadata, id: CustomKundentypId)(implicit personId: PersonId = meta.originator) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.deleteEntity[CustomKundentyp, CustomKundentypId](id) match {
        case Some(kundentyp) =>
          if (kundentyp.anzahlVerknuepfungen > 0) {
            //remove kundentyp from kunden
            stammdatenWriteRepository.getKunden.filter(_.typen.contains(kundentyp.kundentyp)).map { kunde =>
              stammdatenWriteRepository.updateEntity[Kunde, KundeId](kunde.id)(
                kundeMapping.column.typen -> (kunde.typen - kundentyp.kundentyp)
              )
            }
          }
        case None =>
      }
    }
  }

  def deleteVertriebsart(meta: EventMetadata, id: VertriebsartId)(implicit personId: PersonId = meta.originator) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.deleteEntity[Depotlieferung, VertriebsartId](id, { vertriebsart: Vertriebsart => vertriebsart.anzahlAbos == 0 && vertriebsart.anzahlAbosAktiv == 0 })
      stammdatenWriteRepository.deleteEntity[Heimlieferung, VertriebsartId](id, { vertriebsart: Vertriebsart => vertriebsart.anzahlAbos == 0 && vertriebsart.anzahlAbosAktiv == 0 })
      stammdatenWriteRepository.deleteEntity[Postlieferung, VertriebsartId](id, { vertriebsart: Vertriebsart => vertriebsart.anzahlAbos == 0 && vertriebsart.anzahlAbosAktiv == 0 })
    }
  }

  def deleteLieferplanung(meta: EventMetadata, id: LieferplanungId)(implicit personId: PersonId = meta.originator) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      val lieferungenOld = stammdatenWriteRepository.getLieferungen(id)
      stammdatenWriteRepository.deleteEntity[Lieferplanung, LieferplanungId](id, { lieferplanung: Lieferplanung => lieferplanung.status == Offen }) map {
        lieferplanung =>
          stammdatenWriteRepository.getLieferungen(lieferplanung.id) map { lieferung =>
            stammdatenWriteRepository.deleteKoerbe(lieferung.id)
            stammdatenWriteRepository.deleteLieferpositionen(lieferung.id)

            //detach lieferung
            logger.debug(s"detach Lieferung:${lieferung.id}:${lieferung}")
            stammdatenWriteRepository.updateEntity[Lieferung, LieferungId](lieferung.id)(
              lieferungMapping.column.lieferplanungId -> Option.empty[LieferplanungId]
            )
          }
      }
    }
  }

  def removeLieferungPlanung(meta: EventMetadata, lieferungId: LieferungOnLieferplanungId)(implicit personId: PersonId = meta.originator) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.deleteLieferpositionen(lieferungId.getLieferungId())
      stammdatenWriteRepository.getById[Lieferung, LieferungId](lieferungMapping, lieferungId.getLieferungId) map { lieferung =>
        // remove zusatzabo baskets and update the lieferung numbers
        lieferung.lieferplanungId match {
          case Some(lieferplanungId) =>
            val activeZusatzAbos = stammdatenWriteRepository.getKoerbe(lieferungId.getLieferungId()).map { korb: Korb =>
              stammdatenWriteRepository.getZusatzAbosByHauptAbo(korb.aboId)
            }.flatten
            val koerbeToDelete = activeZusatzAbos.map { zusatzabo =>
              stammdatenWriteRepository.getLieferungen(lieferplanungId).map { l =>
                stammdatenWriteRepository.getKorb(l.id, zusatzabo.id)
              }.flatten
            }.flatten
            koerbeToDelete map { korbToDelete =>
              deleteKorb(meta, korbToDelete.id) match {
                case Some(deletedKorbLieferung) =>
                  if (deletedKorbLieferung.anzahlAbwesenheiten == 0 && deletedKorbLieferung.anzahlKoerbeZuLiefern == 0 && deletedKorbLieferung.anzahlSaldoZuTief == 0) {
                    stammdatenWriteRepository.deleteEntity[Lieferung, LieferungId](deletedKorbLieferung.id)
                  }
                case None => //do nothing
              }
            }
          case None => // do nothing
        }
        stammdatenWriteRepository.deleteKoerbe(lieferung.id)
        stammdatenWriteRepository.getAbotypById(lieferung.abotypId) collect {
          case abotyp: ZusatzAbotyp =>
            stammdatenWriteRepository.deleteEntity[Lieferung, LieferungId](lieferungId.getLieferungId)
          case _ =>
            stammdatenWriteRepository.updateEntity[Lieferung, LieferungId](lieferungId.getLieferungId())(
              lieferungMapping.column.durchschnittspreis -> ZERO,
              lieferungMapping.column.anzahlLieferungen -> ZERO,
              lieferungMapping.column.anzahlKoerbeZuLiefern -> ZERO,
              lieferungMapping.column.anzahlAbwesenheiten -> ZERO,
              lieferungMapping.column.anzahlSaldoZuTief -> ZERO,
              lieferungMapping.column.lieferplanungId -> Option.empty[LieferplanungId],
              lieferungMapping.column.status -> Ungeplant
            )
        }
      }
    }
  }

  def deleteKorb(meta: EventMetadata, id: KorbId)(implicit personId: PersonId = meta.originator) = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      val korb = stammdatenWriteRepository.getById(korbMapping, id)
      stammdatenWriteRepository.deleteEntity[Korb, KorbId](id)
      korb match {
        case Some(k) => {
          stammdatenWriteRepository.getById(lieferungMapping, k.lieferungId) match {
            case Some(lieferung) => Some(recalculateNumbersLieferung(lieferung))
            case None            => None
          }
        }
        case None => None
      }
    }
  }

  def deleteLieferung(meta: EventMetadata, id: LieferungId)(implicit personId: PersonId = meta.originator) = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      noSessionDeleteLieferung(id)
    }
  }

  def noSessionDeleteLieferung(lieferungId: LieferungId)(implicit personId: PersonId, session: DBSession, publisher: EventPublisher) = {
    stammdatenWriteRepository.getById(lieferungMapping, lieferungId) match {
      case Some(lieferung) =>
        lieferung.lieferplanungId match {
          case Some(lieferplanungId) =>
            val activeZusatzAbos = stammdatenWriteRepository.getKoerbe(lieferungId).map { korb: Korb =>
              stammdatenWriteRepository.getZusatzAbosByHauptAbo(korb.aboId)
            }.flatten.filter(_.aktiv == true)
            val koerbeToDelete = activeZusatzAbos.map { zusatzabo =>
              stammdatenWriteRepository.getLieferungen(lieferplanungId).map { l =>
                stammdatenWriteRepository.getKorb(l.id, zusatzabo.id)
              }.flatten
            }.flatten
            koerbeToDelete.map(korbToDelete => {
              stammdatenWriteRepository.getById(lieferungMapping, korbToDelete.lieferungId) match {
                case Some(lieferung) => recalculateNumbersLieferung(lieferung)
                case None            => //do nothing
              }
              stammdatenWriteRepository.deleteEntity[Korb, KorbId](korbToDelete.id)
            })
            stammdatenWriteRepository.deleteEntity[Lieferung, LieferungId](lieferungId, { lieferung: Lieferung => lieferung.lieferplanungId == None })
            recalculateNumbersLieferung(lieferung)
          case None => // do nothing
        }
      case None => // do nothing
    }
  }

  def deleteProdukt(meta: EventMetadata, id: ProduktId)(implicit personId: PersonId = meta.originator) = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.deleteEntity[Produkt, ProduktId](id)
    }
  }

  def deleteProduzent(meta: EventMetadata, id: ProduzentId)(implicit personId: PersonId = meta.originator) = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.deleteEntity[Produzent, ProduzentId](id)
    }
  }

  def deleteProduktekategorie(meta: EventMetadata, id: ProduktekategorieId)(implicit personId: PersonId = meta.originator) = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.deleteEntity[Produktekategorie, ProduktekategorieId](id)
    }
  }

  def deleteProduktProduktekategorie(meta: EventMetadata, id: ProduktProduktekategorieId)(implicit personId: PersonId = meta.originator) = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.deleteEntity[ProduktProduktekategorie, ProduktProduktekategorieId](id)
    }
  }

  def deleteTour(meta: EventMetadata, id: TourId)(implicit personId: PersonId = meta.originator) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.deleteEntity[Tour, TourId](id, { tour: Tour => tour.anzahlAbonnenten == 0 }) map { tour =>
        stammdatenWriteRepository.getHeimlieferung(tour.id) map { hl =>
          stammdatenWriteRepository.deleteEntity[Heimlieferung, VertriebsartId](hl.id, { vertriebsart: Vertriebsart => vertriebsart.anzahlAbos == 0 && vertriebsart.anzahlAbosAktiv == 0 })
        }
      }
    }
  }

  def deleteVertrieb(meta: EventMetadata, id: VertriebId)(implicit personId: PersonId = meta.originator) = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      noSessionDeleteVertrieb(id)
    }
  }

  def noSessionDeleteVertrieb(id: VertriebId)(implicit personId: PersonId, session: DBSession, publisher: EventPublisher) = {
    stammdatenWriteRepository.deleteEntity[Vertrieb, VertriebId](id, { vertrieb: Vertrieb => vertrieb.anzahlAbos == 0 && vertrieb.anzahlAbosAktiv == 0 }) map { vertrieb =>
      stammdatenWriteRepository.getLieferungen(vertrieb.id) map { lieferung =>
        noSessionDeleteLieferung(lieferung.id)
      }
    }
  }

  def deleteProjektVorlage(meta: EventMetadata, id: ProjektVorlageId)(implicit personId: PersonId = meta.originator) = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.deleteEntity[ProjektVorlage, ProjektVorlageId](id)
    }
  }
}
