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
import org.joda.time.DateTime
import com.github.nscala_time.time.Imports._
import scalikejdbc._

trait KorbHandler extends KorbStatusHandler
    with StammdatenDBMappings {
  this: StammdatenWriteRepositoryComponent =>

  /**
   * insert or update Korb
   * @return (created/updated, existing)
   */
  def upsertKorb(lieferung: Lieferung, abo: Abo, abotyp: Abotyp)(implicit personId: PersonId, session: DBSession, publisher: EventPublisher): (Option[Korb], Option[Korb]) = {
    stammdatenWriteRepository.getKorb(lieferung.id, abo.id) match {
      case None if (lieferung.lieferplanungId.isDefined) =>
        val abwCount = stammdatenWriteRepository.countAbwesend(lieferung.id, abo.id)
        val status = calculateKorbStatus(abwCount, abo.guthaben, abotyp.guthabenMindestbestand)
        val korbId = KorbId(IdUtil.positiveRandomId)
        val korb = Korb(
          korbId,
          lieferung.id,
          abo.id,
          status,
          abo.guthaben,
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
        val status = calculateKorbStatus(abwCount, abo.guthaben, abotyp.guthabenMindestbestand)

        val copy = korb.copy(
          status = status,
          guthabenVorLieferung = abo.guthaben
        )

        // only update if changed
        if (korb != copy) {
          (stammdatenWriteRepository.updateEntity[Korb, KorbId](copy, korbMapping.column.status, korbMapping.column.guthabenVorLieferung), Some(korb))
        } else {
          (Some(korb), Some(korb))
        }
    }
  }

  def deleteKorb(lieferung: Lieferung, abo: Abo)(implicit personId: PersonId, session: DBSession, publisher: EventPublisher) = {
    stammdatenWriteRepository.getKorb(lieferung.id, abo.id) flatMap { korb =>
      stammdatenWriteRepository.deleteEntity[Korb, KorbId](korb.id)
    }
  }

  def modifyKoerbeForAbo(abo: Abo, orig: Option[Abo])(implicit personId: PersonId, session: DBSession, publisher: EventPublisher) = {
    // koerbe erstellen, modifizieren, loeschen falls noetig
    stammdatenWriteRepository.getById(abotypMapping, abo.abotypId) map { abotyp =>
      stammdatenWriteRepository.getLieferungenOffenByAbotyp(abo.abotypId) map { lieferung =>
        if (orig.isDefined && (abo.start > lieferung.datum.toLocalDate || (abo.ende map (_ <= (lieferung.datum.toLocalDate - 1.day)) getOrElse false))) {
          deleteKorb(lieferung, abo)
        } else if (abo.start <= lieferung.datum.toLocalDate && (abo.ende map (_ >= lieferung.datum.toLocalDate) getOrElse true)) {
          upsertKorb(lieferung, abo, abotyp)
        }
      }
    }
  }
}
