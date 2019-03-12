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

import scala.concurrent.{ ExecutionContext, Future }
import ch.openolitor.arbeitseinsatz.models._
import ch.openolitor.core.db.AsyncConnectionPoolContextAware
import ch.openolitor.core.filestore._
import ch.openolitor.core.ActorReferences
import ch.openolitor.core.reporting._
import ch.openolitor.stammdaten.repositories.StammdatenReadRepositoryAsyncComponent
import ch.openolitor.core.models.PersonId
import ch.openolitor.arbeitseinsatz.ArbeitseinsatzJsonProtocol
import ch.openolitor.arbeitseinsatz.repositories.ArbeitseinsatzReadRepositoryAsyncComponent
import ch.openolitor.core.jobs.JobQueueService.JobId

trait ArbeitsangebotReportService extends AsyncConnectionPoolContextAware with ReportService with ArbeitseinsatzJsonProtocol with ArbeitsangebotReportData {
  self: ArbeitseinsatzReadRepositoryAsyncComponent with ActorReferences with FileStoreComponent with StammdatenReadRepositoryAsyncComponent =>

  def generateArbeitsangebotReports(config: ReportConfig[ArbeitsangebotId])(implicit personId: PersonId, executionContext: ExecutionContext): Future[Either[ServiceFailed, ReportServiceResult[ArbeitsangebotId]]] = {
    generateReports[ArbeitsangebotId, ArbeitseinsatzDetailReport](
      config,
      arbeitseinsaetzeByArbeitsangebote,
      VorlageArbeitseinsatz,
      None,
      _.id,
      TemporaryData,
      x => Some(x.id.id.toString),
      name,
      _.projekt.sprache,
      JobId("Arbeitsangebote")
    )
  }

  private def name(arbeitseinsatz: ArbeitseinsatzDetailReport) = {
    s"arbeitsangebot_${arbeitseinsatz.arbeitsangebot.id.id}_angebot_${arbeitseinsatz.id.id}";
  }
}
