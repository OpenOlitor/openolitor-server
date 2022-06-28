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
package ch.openolitor.arbeitseinsatz.reporting

import scala.concurrent.Future
import ch.openolitor.arbeitseinsatz.models._
import ch.openolitor.core.db.AsyncConnectionPoolContextAware
import scala.concurrent.ExecutionContext.Implicits.global
import ch.openolitor.core.ActorReferences
import ch.openolitor.core.reporting._
import ch.openolitor.core.Macros._
import ch.openolitor.stammdaten.models.Projekt
import ch.openolitor.stammdaten.repositories.StammdatenReadRepositoryAsyncComponent
import ch.openolitor.stammdaten.models.ProjektReport
import ch.openolitor.arbeitseinsatz.ArbeitseinsatzJsonProtocol
import ch.openolitor.arbeitseinsatz.repositories.ArbeitseinsatzReadRepositoryAsyncComponent
import scala.Left
import scala.Right

trait ArbeitsangebotReportData extends AsyncConnectionPoolContextAware with ArbeitseinsatzJsonProtocol {
  self: ArbeitseinsatzReadRepositoryAsyncComponent with ActorReferences with StammdatenReadRepositoryAsyncComponent =>

  def arbeitseinsaetzeByArbeitsangebote(arbeitsangebotIds: Seq[ArbeitsangebotId]): Future[(Seq[ValidationError[ArbeitsangebotId]], Seq[ArbeitseinsatzDetailReport])] = {
    stammdatenReadRepository.getProjekt flatMap {
      _ map { projekt =>
        val results = Future.sequence(arbeitsangebotIds.map { arbeitsangebotId =>
          arbeitseinsatzReadRepository.getArbeitseinsatzDetailByArbeitsangebot(arbeitsangebotId).map {
            _.map { arbeitseinsatz =>
              val projektReport = copyTo[Projekt, ProjektReport](projekt)
              copyTo[ArbeitseinsatzDetail, ArbeitseinsatzDetailReport](arbeitseinsatz, "projekt" -> projektReport)
            }
          }
        })
        results.map(_.flatten).map(c => (Seq(), c))
      } getOrElse Future { (Seq(ValidationError[ArbeitsangebotId](null, s"Projekt konnte nicht geladen werden")), Seq()) }
    }
  }

  def arbeitsangebotByIds(arbeitseinsaetzIds: Seq[ArbeitseinsatzId]): Future[(Seq[ValidationError[ArbeitseinsatzId]], Seq[ArbeitseinsatzDetailReport])] = {
    stammdatenReadRepository.getProjekt flatMap {
      _ map { projekt =>
        val results = Future.sequence(arbeitseinsaetzIds.map { arbeitseinsatzId =>
          arbeitseinsatzReadRepository.getArbeitseinsatzDetail(arbeitseinsatzId).map(_ match {
            case Some(arbeitseinsatz) =>
              val projektReport = copyTo[Projekt, ProjektReport](projekt)
              Right(copyTo[ArbeitseinsatzDetail, ArbeitseinsatzDetailReport](arbeitseinsatz, "projekt" -> projektReport))
            case None =>
              Left(ValidationError[ArbeitseinsatzId](arbeitseinsatzId, s"Arbeitseinsatz konnte nicht gefunden werden"))
          })
        })
        results.map(_.partition(_.isLeft) match {
          case (a, b) => (a.map(_.swap.toOption.get), b.map(_.toOption.get))
        })
      } getOrElse Future { (Seq(ValidationError[ArbeitseinsatzId](null, s"Projekt konnte nicht geladen werden")), Seq()) }
    }
  }
}
