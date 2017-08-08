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
import ch.openolitor.core.SystemConfig
import ch.openolitor.core.mailservice.MailService._
import ch.openolitor.stammdaten.models._
import ch.openolitor.stammdaten.repositories._
import ch.openolitor.core.domain._
import ch.openolitor.core.db._
import ch.openolitor.core.models.PersonId
import scalikejdbc._
import ch.openolitor.stammdaten.StammdatenCommandHandler.AboAktiviertEvent
import ch.openolitor.stammdaten.StammdatenCommandHandler.AboDeaktiviertEvent
import ch.openolitor.core.models.BaseEntity
import ch.openolitor.core.models.BaseId
import ch.openolitor.core.repositories.BaseEntitySQLSyntaxSupport
import ch.openolitor.core.repositories.SqlBinder
import ch.openolitor.core.repositories.EventPublisher

trait AboAktivChangeHandler extends StammdatenDBMappings {
  this: StammdatenUpdateRepositoryComponent =>
  def handleAboAktivChange(abo: Abo, change: Int)(implicit session: DBSession, publisher: EventPublisher, personId: PersonId) = {
    abo match {
      case d: DepotlieferungAbo =>
        modifyEntityFields[Depot, DepotId](d.depotId) { depot =>
          depot.copy(anzahlAbonnentenAktiv = depot.anzahlAbonnentenAktiv + change)
        }(depotMapping.column.anzahlAbonnentenAktiv)
      case h: HeimlieferungAbo =>
        modifyEntityFields[Tour, TourId](h.tourId) { tour =>
          tour.copy(anzahlAbonnentenAktiv = tour.anzahlAbonnentenAktiv + change)
        }(tourMapping.column.anzahlAbonnentenAktiv)
      case _ =>
      // nothing to change
    }

    modifyEntityFields[Abotyp, AbotypId](abo.abotypId) { abotyp =>
      abotyp.copy(anzahlAbonnentenAktiv = abotyp.anzahlAbonnentenAktiv + change)
    }(abotypMapping.column.anzahlAbonnentenAktiv)

    modifyEntityFields[Kunde, KundeId](abo.kundeId) { kunde =>
      kunde.copy(anzahlAbosAktiv = kunde.anzahlAbosAktiv + change)
    }(kundeMapping.column.anzahlAbosAktiv)

    modifyEntityFields[Vertrieb, VertriebId](abo.vertriebId) { vertrieb =>
      vertrieb.copy(anzahlAbosAktiv = vertrieb.anzahlAbosAktiv + change)
    }(vertriebMapping.column.anzahlAbosAktiv)

    modifyEntityFields[Depotlieferung, VertriebsartId](abo.vertriebsartId) { vertriebsart =>
      vertriebsart.copy(anzahlAbosAktiv = vertriebsart.anzahlAbosAktiv + change)
    }(depotlieferungMapping.column.anzahlAbosAktiv)

    modifyEntityFields[Heimlieferung, VertriebsartId](abo.vertriebsartId) { vertriebsart =>
      vertriebsart.copy(anzahlAbosAktiv = vertriebsart.anzahlAbosAktiv + change)
    }(heimlieferungMapping.column.anzahlAbosAktiv)

    modifyEntityFields[Postlieferung, VertriebsartId](abo.vertriebsartId) { vertriebsart =>
      vertriebsart.copy(anzahlAbosAktiv = vertriebsart.anzahlAbosAktiv + change)
    }(postlieferungMapping.column.anzahlAbosAktiv)
  }
}