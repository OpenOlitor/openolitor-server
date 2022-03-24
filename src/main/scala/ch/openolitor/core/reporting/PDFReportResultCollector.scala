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
package ch.openolitor.core.reporting

import akka.actor._
import ch.openolitor.buchhaltung.models.RechnungId
import ch.openolitor.core.DateFormats
import ch.openolitor.core.jobs.JobQueueService.FileResultPayload
import ch.openolitor.core.reporting.ReportSystem._
import org.apache.pdfbox.multipdf.PDFMergerUtility
import org.apache.pdfbox.pdmodel.PDDocument
import spray.http.MediaTypes

import java.io.File
import scala.util._

object PDFReportResultCollector {
  def props(reportSystem: ActorRef, jobQueueService: ActorRef): Props = Props(classOf[PDFReportResultCollector], reportSystem, jobQueueService)
}

/**
 * Collect all results into a pdf file. Send back the pdf result when all reports got generated.
 * This ResultCollector stores the generated documents in a local pdf which will eventually cause out of disk space errors.
 */
class PDFReportResultCollector(reportSystem: ActorRef, override val jobQueueService: ActorRef) extends ResultCollector with DateFormats {

  var origSender: Option[ActorRef] = None
  val PDFmerged = new PDFMergerUtility
  val mergedFile = new PDDocument()
  var pdfFiles: List[(RechnungId, PDDocument)] = List()
  var errors: Seq[ReportError] = Seq()

  val receive: Receive = {
    case request: GenerateReports[_] =>
      origSender = Some(sender)
      reportSystem ! request
      context become waitingForResult
  }

  val waitingForResult: Receive = {
    case SingleReportResult(_, stats, Left(error)) =>
      errors = errors :+ error
      notifyProgress(stats)
    case SingleReportResult(id: RechnungId, stats, Right(result: ReportResultWithDocument)) =>
      log.debug(s"Add Pdf Entry:${result.name}")
      pdfFiles = pdfFiles :+ (id, PDDocument.load(result.document))
      notifyProgress(stats)
    case result: GenerateReportsStats if result.numberOfReportsInProgress == 0 =>
      pdfFiles.sortBy(_._1) map { file =>
        PDFmerged.appendDocument(mergedFile, file._2)
      }
      val fileName = "Report_" + filenameDateFormat.print(System.currentTimeMillis())
      val file = File.createTempFile(fileName, ".pdf");
      mergedFile.save(file)
      val payload = FileResultPayload(fileName, MediaTypes.`application/pdf`, file)
      log.debug(s"Send payload as result:${fileName}")
      jobFinished(result, Some(payload))
      log.debug(s"Stop collector PoisonPill")
      self ! PoisonPill
    case stats: GenerateReportsStats =>
      notifyProgress(stats)
  }
}