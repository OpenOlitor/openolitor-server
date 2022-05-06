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
package ch.openolitor.stammdaten.reporting

import ch.openolitor.core.ActorReferences
import ch.openolitor.core.Macros._
import ch.openolitor.core.db.AsyncConnectionPoolContextAware
import ch.openolitor.core.filestore._
import ch.openolitor.core.jobs.JobQueueService.JobId
import ch.openolitor.core.models.PersonId
import ch.openolitor.core.reporting._
import ch.openolitor.core.reporting.models._
import ch.openolitor.stammdaten._
import ch.openolitor.stammdaten.models._
import ch.openolitor.stammdaten.repositories.StammdatenReadRepositoryAsyncComponent
import ch.openolitor.util.IdUtil
import org.joda.time.DateTime

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait AuslieferungKorbDetailReportService extends AsyncConnectionPoolContextAware with ReportService with StammdatenJsonProtocol {
  self: StammdatenReadRepositoryAsyncComponent with ActorReferences with FileStoreComponent =>

  def generateAuslieferungKorbDetailReports(fileType: FileType)(config: ReportConfig[AuslieferungId])(implicit personId: PersonId): Future[Either[ServiceFailed, ReportServiceResult[AuslieferungId]]] = {
    generateReports[AuslieferungId, MultiReport[AuslieferungKorbDetailsReport]](
      config,
      auslieferungenByIds,
      fileType,
      None,
      _.id,
      GeneriertAuslieferung,
      x => Some(x.id.id.toString),
      name(fileType),
      _.projekt.sprache,
      JobId("Auslieferungs-KorbDetail")
    )
  }

  private def name(fileType: FileType)(auslieferung: MultiReport[AuslieferungKorbDetailsReport]) = {
    val now = new DateTime()
    s"auslieferung_korbdetail_${now}"
  }

  private def auslieferungenByIds(auslieferungIds: Seq[AuslieferungId]): Future[(Seq[ValidationError[AuslieferungId]], Seq[MultiReport[AuslieferungKorbDetailsReport]])] = {
    stammdatenReadRepository.getProjekt flatMap {
      _ map { projekt =>
        val projektReport = copyTo[Projekt, ProjektReport](projekt)
        stammdatenReadRepository.getMultiAuslieferungReport(auslieferungIds, projektReport) map { auslieferungReport =>
          val proAbotyp = (auslieferungReport.entries groupBy (groupIdentifier) map {
            case (abotypName, auslieferungen) =>
              val totalLieferpositionenList = auslieferungen flatMap { auslieferung => auslieferung.korb.lieferpositionen }
              val factorizedLieferpositionenList = totalLieferpositionenList.distinct map {
                lp: Lieferposition =>
                  {
                    val numberOfTimes = totalLieferpositionenList.filter(_.id == lp.id).length
                    val quantity = lp.menge.getOrElse(BigDecimal(1)).doubleValue
                    KorbTotalComposition(lp.id.toString, lp.produktBeschrieb, Math.round((quantity * numberOfTimes) * 100.0) / 100.0, lp.einheit.toString)
                  }
              }
              (
                abotypName,
                factorizedLieferpositionenList,
                auslieferungen groupBy (auslieferung => auslieferung.depot.map(_.name) orElse (auslieferung.tour map (_.name)) getOrElse "Post") mapValues (_.size)
              )
          }) map {
            case (abotypName, lp, proDepotTour) =>
              KorbDetailsReportProAbotyp(abotypName, proDepotTour.values.sum, (proDepotTour map (p => KorbDetailsReportProDepotTour(p._1, p._2))).toSeq, lp)
          }

          val datum = if (!auslieferungReport.entries.isEmpty) auslieferungReport.entries(0).datum else new DateTime()

          (Seq(), List(MultiReport(MultiReportId(IdUtil.positiveRandomId), Seq(
            AuslieferungKorbDetailsReport(
              projektReport,
              datum,
              auslieferungReport.entries.size,
              proAbotyp.toSeq
            )
          ), projektReport)))
        }
      } getOrElse Future { (Seq(ValidationError[AuslieferungId](null, s"Projekt konnte nicht geladen werden")), Seq()) }
    }
  }

  /**
   * This will result in titles containing Abotyp +Z1, Z2
   */
  private def groupIdentifier(reportEntry: AuslieferungReportEntry) = {
    val zusatzAbos = if (!reportEntry.korb.zusatzAbosString.isEmpty()) s" +${reportEntry.korb.zusatzAbosString}" else ""
    s"${reportEntry.korb.abotyp.name}${zusatzAbos}"
  }
}

