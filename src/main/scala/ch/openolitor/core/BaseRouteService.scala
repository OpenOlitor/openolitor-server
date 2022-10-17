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
package ch.openolitor.core

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.{ Marshaller, ToResponseMarshaller }
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.MarshallingDirectives.{ as, entity }
import akka.http.scaladsl.server.{ RequestContext, Route, RouteResult }
import akka.http.scaladsl.unmarshalling.FromRequestUnmarshaller
import akka.pattern.ask
import akka.stream.Materializer
import akka.stream.scaladsl.{ Sink, Source, StreamConverters }
import akka.util.{ ByteString, Timeout }
import ch.openolitor.core.BaseJsonProtocol.IdResponse
import ch.openolitor.core.domain.EntityStore
import ch.openolitor.core.domain.EntityStore.EntityInsertedEvent
import ch.openolitor.core.filestore._
import ch.openolitor.core.models.{ BaseId, BaseStringId }
import ch.openolitor.core.reporting.ReportSystem.{ AsyncReportResult, ReportDataResult }
import ch.openolitor.core.reporting._
import ch.openolitor.core.security.Subject
import ch.openolitor.core.ws.{ ExportFormat, ODS }
import ch.openolitor.stammdaten.models.ProjektVorlageId
import ch.openolitor.util.ZipBuilderWithFile
import com.tegonal.CFEnvConfigLoader.ConfigLoader
import com.typesafe.scalalogging.LazyLogging
import org.apache.pdfbox.multipdf.PDFMergerUtility
import org.apache.pdfbox.pdmodel.PDDocument
import org.odftoolkit.simple.SpreadsheetDocument
import org.odftoolkit.simple.style.StyleTypeDefinitions
import org.odftoolkit.simple.table.{ Row, Table }
import stamina.Persister

import java.io._
import java.nio.file.Files
import java.util.{ Locale, UUID }
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters._
import scala.reflect.ClassTag
import scala.util.{ Either, Failure, Left, Right, Success, Try }

trait BaseRouteService extends ExecutionContextAware with SprayJsonSupport with AkkaHttpDeserializers with BaseJsonProtocol with EntityStoreReference with FileStoreReference with LazyLogging {

  implicit val timeout = Timeout(5 seconds)
  lazy val DefaultChunkSize = ConfigLoader.loadConfig.getBytes("akka.http.parsing.max-chunk-size").toInt

  implicit val exportFormatPath = enumPathMatcher(path =>
    path.head match {
      case '.' => ExportFormat.apply(path) match {
        case x => Some(x)
      }
      case _ => None
    })

  protected def create[E <: AnyRef: ClassTag, I <: BaseId](idFactory: Long => I)(implicit
    um: FromRequestUnmarshaller[E],
    tr: ToResponseMarshaller[I], persister: Persister[E, _], subject: Subject
  ): Route = {
    extractRequest { request =>
      entity(as[E]) { entity =>
        created(request)(entity)
      }
    }
  }

  protected def created[E <: AnyRef: ClassTag, I <: BaseId](request: HttpRequest)(entity: E)(implicit persister: Persister[E, _], subject: Subject): Route = {
    //create entity
    onSuccess(entityStore ? EntityStore.InsertEntityCommand(subject.personId, entity)) {
      case event: EntityInsertedEvent[_, _] =>
        respondWithHeaders(Location(request.uri.withPath(request.uri.path / event.id.toString))) {
          complete(StatusCodes.Created, IdResponse(event.id.id))
        }
      case x =>
        complete(StatusCodes.BadRequest, s"No id generated or CommandHandler not triggered:$x")
    }
  }

  protected def update[E <: AnyRef: ClassTag, I <: BaseId](id: I)(implicit
    um: FromRequestUnmarshaller[E],
    tr: ToResponseMarshaller[I], idPersister: Persister[I, _], entityPersister: Persister[E, _], subject: Subject
  ): Route = {
    entity(as[E]) { entity => updated(id, entity) }
  }

  protected def update[E <: AnyRef: ClassTag, I <: BaseId](id: I, entity: E)(implicit
    um: FromRequestUnmarshaller[E],
    tr: ToResponseMarshaller[I], idPersister: Persister[I, _], entityPersister: Persister[E, _], subject: Subject
  ): Route = {
    updated(id, entity)
  }

  protected def updated[E <: AnyRef: ClassTag, I <: BaseId](id: I, entity: E)(implicit idPersister: Persister[I, _], entityPersister: Persister[E, _], subject: Subject): Route = {
    //update entity
    onSuccess(entityStore ? EntityStore.UpdateEntityCommand(subject.personId, id, entity)) { result =>
      complete(StatusCodes.Accepted, "")
    }
  }

  protected def list[R](f: => Future[R])(implicit tr: ToResponseMarshaller[R]): Route = {
    //fetch list of something
    onSuccess(f) { result =>
      complete(result)
    }
  }

  protected def list[R](f: => Future[R], exportFormat: Option[ExportFormat])(implicit tr: ToResponseMarshaller[R], materializer: Materializer): Route = {
    //fetch list of something
    onSuccess(f) { result =>
      exportFormat match {
        case Some(ODS) => {
          val dataDocument = SpreadsheetDocument.newSpreadsheetDocument()
          val sheet = dataDocument.getSheetByIndex(0)
          sheet.setCellStyleInheritance(false)

          def createNewSheet(name: String): Table = {
            val newSheet = dataDocument.appendSheet(name)
            newSheet.setCellStyleInheritance(false)
            newSheet
          }

          def writeToRow(row: Row, element: Any, cellIndex: Int): Unit = {
            element match {
              case null                        =>
              case some: Some[Any]             => writeToRow(row, some.value, cellIndex)
              case None                        =>
              case ite: Iterable[Any]          => ite map { item => writeToRow(row, item, cellIndex) }
              case id: BaseId                  => row.getCellByIndex(cellIndex).setDoubleValue(id.id)
              case stringId: BaseStringId      => row.getCellByIndex(cellIndex).setStringValue((row.getCellByIndex(cellIndex).getStringValue + " " + stringId.id).trim)
              case str: String                 => row.getCellByIndex(cellIndex).setStringValue((row.getCellByIndex(cellIndex).getStringValue + " " + str).trim)
              case dat: org.joda.time.DateTime => row.getCellByIndex(cellIndex).setDateTimeValue(dat.toCalendar(Locale.GERMAN))
              case nbr: Number                 => row.getCellByIndex(cellIndex).setDoubleValue(nbr.doubleValue())
              case x                           => row.getCellByIndex(cellIndex).setStringValue((row.getCellByIndex(cellIndex).getStringValue + " " + x.toString).trim)
            }
          }

          result match {
            case genericList: List[Any] => {
              if (genericList.nonEmpty) {
                genericList.groupBy(_.getClass) foreach {
                  case (clazz, clazzSortedList) => {
                    var clazzSheet = createNewSheet(clazz.getSimpleName())
                    clazzSortedList.head match {
                      case firstMapEntry: Map[_, _] =>
                        val listOfMaps = clazzSortedList.asInstanceOf[List[Map[String, Any]]]
                        val row = clazzSheet.getRowByIndex(0);

                        listOfMaps.head.zipWithIndex foreach {
                          case ((fieldName, value), index) =>
                            row.getCellByIndex(index).setStringValue(fieldName)
                            val font = row.getCellByIndex(index).getFont
                            font.setFontStyle(StyleTypeDefinitions.FontStyle.BOLD)
                            font.setSize(10)
                            row.getCellByIndex(index).setFont(font)
                        }

                        listOfMaps.zipWithIndex foreach {
                          case (entry, index) =>
                            val row = clazzSheet.getRowByIndex(index + 1);

                            entry.zipWithIndex foreach {
                              case ((fieldName, value), colIndex) =>
                                fieldName match {
                                  case "passwort" => writeToRow(row, "Not available", colIndex)
                                  case _          => writeToRow(row, value, colIndex)
                                }
                            }
                        }

                      case firstProductEntry: Product =>
                        val listOfProducts = clazzSortedList.asInstanceOf[List[Product]]
                        val row = clazzSheet.getRowByIndex(0);

                        def getCCParams(cc: Product) = cc.getClass.getDeclaredFields.map(_.getName) // all field names
                          .zip(cc.productIterator).toMap // zipped with all values

                        getCCParams(listOfProducts.head).zipWithIndex foreach {
                          case ((fieldName, value), index) =>
                            row.getCellByIndex(index).setStringValue(fieldName)
                            val font = row.getCellByIndex(index).getFont
                            font.setFontStyle(StyleTypeDefinitions.FontStyle.BOLD)
                            font.setSize(10)
                            row.getCellByIndex(index).setFont(font)
                        }

                        listOfProducts.zipWithIndex foreach {
                          case (entry, index) =>
                            val row = clazzSheet.getRowByIndex(index + 1);

                            getCCParams(entry).zipWithIndex foreach {
                              case ((fieldName, value), colIndex) =>
                                writeToRow(row, value, colIndex)
                            }
                        }
                    }
                  }
                }
              }
              dataDocument.removeSheet(0)
            }
            case x => sheet.getRowByIndex(0).getCellByIndex(0).setStringValue("Data of type" + x.toString + " could not be transfered to ODS file.")
          }

          sheet.getColumnList.asScala map {
            _.setUseOptimalWidth(true)
          }

          val outputStream = new ByteArrayOutputStream
          dataDocument.save(outputStream)
          streamOds("Daten_" + System.currentTimeMillis + ".ods", outputStream.toByteArray())
        }
        //matches "None" and "Some(Json)"
        case None    => complete(result)
        case Some(x) => complete(result)
      }
    }

  }

  protected def detail[R](f: => Future[Option[R]])(implicit tr: ToResponseMarshaller[R]): Route = {
    //fetch detail of something
    onSuccess(f) { result =>
      result.map(complete(_)).getOrElse(complete(StatusCodes.NotFound))
    }
  }

  /**
   * @persister declare format to ensure that format exists for persising purposes
   */
  protected def remove[I <: BaseId](id: I)(implicit persister: Persister[I, _], subject: Subject): Route = {
    onSuccess(entityStore ? EntityStore.DeleteEntityCommand(subject.personId, id)) { _ =>
      complete("")
    }
  }

  protected def downloadAll(pdfFileName: String, fileType: FileType, ids: Seq[FileStoreFileId], pdfMerge: Boolean)(implicit materializer: Materializer): Route = {
    ids match {
      case Seq()            => complete(StatusCodes.BadRequest)
      case Seq(fileStoreId) => download(fileType, fileStoreId.id)
      case _ =>
        val refs = ids.map(id => FileStoreFileReference(fileType, id))
        if (pdfMerge) downloadMergedPDFs(pdfFileName + ".pdf", refs)
        else downloadAsZip(pdfFileName + ".zip", refs)
    }
  }

  protected def fetch(fileType: FileType, id: String): Route = {
    tryDownload(fileType, id, Fetch)(_ => complete(StatusCodes.NotFound, s"File of file type ${fileType} with id ${id} was not found."))
  }

  protected def download(fileType: FileType, id: String): Route = {
    tryDownload(fileType, id, Download)(_ => complete(StatusCodes.NotFound, s"File of file type ${fileType} with id ${id} was not found."))
  }

  protected def tryDownload(fileType: FileType, id: String, responseType: ResponseType = Download)(errorFunction: FileStoreError => RequestContext => Future[RouteResult]): Route = extractRequestContext { requestContext =>
    onSuccess(fileStore.getFile(fileType.bucket, id)) {
      case Left(e) => errorFunction(e)
      case Right(file) =>
        val name = if (file.metaData.name.isEmpty) id else file.metaData.name
        implicit val materializer: Materializer = requestContext.materializer
        responseType match {
          case Download => respondWithHeader(`Content-Disposition`(ContentDispositionTypes.attachment, Map(("filename", name)))) {
            stream(file.file)
          }
          case Fetch =>
            stream(file.file, ContentType(MediaTypes.`text/css`, HttpCharsets.`UTF-8`))
        }
    }
  }

  protected def downloadAsZip(zipFileName: String, fileReferences: Seq[FileStoreFileReference])(implicit materializer: Materializer): Route = {
    val builder = new ZipBuilderWithFile()
    onComplete(Future.sequence(fileReferences map { ref =>
      fileStore.getFile(ref.fileType.bucket, ref.id.id) map {
        case Left(e) =>
          logger.warn(s"Couldn't download file from fileStore '${ref.fileType.bucket}-${ref.id.id}':$e")
        case Right(file) =>
          val name = if (file.metaData.name.isEmpty) ref.id.id else file.metaData.name
          logger.debug(s"Add zip entry:${ref.id.id} => $name")
          builder.addZipEntry(name, file.file)
      }
    })) {
      case Success(_) =>
        builder.close().map(result => streamZip(zipFileName, result)) getOrElse complete(StatusCodes.NotFound)
      case Failure(_) =>
        complete(StatusCodes.NotFound)
    }
  }

  protected def downloadMergedPDFs(pdfFileName: String, fileReferences: Seq[FileStoreFileReference])(implicit materializer: Materializer): Route = {
    val PDFmerged = new PDFMergerUtility
    val mergedFile = new PDDocument
    onComplete(Future.sequence(fileReferences.sortBy(_.id) map { ref =>
      fileStore.getFile(ref.fileType.bucket, ref.id.id) map {
        case Left(e) =>
          logger.warn(s"Couldn't download file from fileStore '${ref.fileType.bucket}-${ref.id.id}':$e")
        case Right(file) =>
          val name = if (file.metaData.name.isEmpty) ref.id.id else file.metaData.name
          logger.debug(s"merge pdf file:${ref.id.id} => $name")
          PDFmerged.appendDocument(mergedFile, PDDocument.load(file.file))
      }
    })) {
      case Success(result) =>
        val file = File.createTempFile(pdfFileName, ".pdf")
        mergedFile.save(file)
        streamPdf(pdfFileName, Files.readAllBytes(file.toPath))
      case Failure(_) =>
        complete(StatusCodes.NotFound)
    }
  }

  protected def stream(input: File, contentType: ContentType, deleteAfterStreaming: Boolean)(implicit materializer: Materializer): Route = {
    stream(new FileInputStream(input), contentType, () => if (deleteAfterStreaming) input.delete())
  }

  protected def stream(input: InputStream, contentType: ContentType = ContentTypes.`application/octet-stream`, onCompleteSuccessFn: () => Unit = () => ())(implicit materializer: Materializer): Route = {
    implicit val toResponseMarshaller: ToResponseMarshaller[Source[ByteString, Any]] =
      Marshaller.opaque { chunks =>
        HttpResponse(entity = HttpEntity.Chunked.fromData(contentType, chunks))
      }

    val source = StreamConverters.fromInputStream(() => input, DefaultChunkSize)

    source.watchTermination() { (_, done) =>
      done.onComplete {
        case Success(_) =>
          onCompleteSuccessFn()

        case Failure(_) =>
      }
    }

    complete(source)
  }

  protected def stream(input: Array[Byte])(implicit materializer: Materializer): Route = {
    stream(new ByteArrayInputStream(input))
  }

  protected def stream(input: ByteString): Route = {
    logger.debug(s"Stream result. Length:${input.size}")
    complete(input)
  }

  protected def streamFile(fileName: String, mediaType: MediaType, file: File, deleteAfterStreaming: Boolean = false)(implicit materializer: Materializer): Route = {
    respondWithHeader(`Content-Disposition`(ContentDispositionTypes.attachment, Map(("filename", fileName)))) {
      stream(file, ContentType(mediaType, () => HttpCharsets.`UTF-8`), deleteAfterStreaming)
    }
  }

  protected def streamZip(fileName: String, result: File, deleteAfterStreaming: Boolean = false)(implicit materializer: Materializer): Route = {
    respondWithHeader(`Content-Disposition`(ContentDispositionTypes.attachment, Map(("filename", fileName)))) {
      stream(result, ContentType(MediaTypes.`application/zip`), deleteAfterStreaming)
    }
  }

  protected def streamPdf(fileName: String, result: Array[Byte])(implicit materializer: Materializer): Route = {
    respondWithHeader(`Content-Disposition`(ContentDispositionTypes.attachment, Map(("filename", fileName)))) {
      stream(new ByteArrayInputStream(result), ContentType(MediaTypes.`application/pdf`))
    }
  }

  protected def streamOdt(fileName: String, result: Array[Byte])(implicit materializer: Materializer): Route = {
    respondWithHeader(`Content-Disposition`(ContentDispositionTypes.attachment, Map(("filename", fileName)))) {
      stream(new ByteArrayInputStream(result), ContentType(MediaTypes.`application/vnd.oasis.opendocument.text`))
    }
  }

  protected def streamOds(fileName: String, result: Array[Byte])(implicit materializer: Materializer): Route = {
    respondWithHeader(`Content-Disposition`(ContentDispositionTypes.attachment, Map(("filename", fileName)))) {
      stream(new ByteArrayInputStream(result), ContentType(MediaTypes.`application/vnd.oasis.opendocument.spreadsheet`))
    }
  }

  protected def upload[T](fileProperty: String = "file")(onConsume: (InputStream, String) => Future[(T, String)])(onUpload: (T, String) => Route)(implicit materializer: Materializer): Route = {
    fileUpload(fileProperty) {
      case (fileInfo, byteSource) =>
        onSuccess(onConsume(byteSource.runWith(StreamConverters.asInputStream()), fileInfo.fileName))(onUpload)
    }
  }

  /**
   * Make sure that the input stream in `onUpload` is consumed in a separate thread to avoid deadlocks and therefore timeout exceptions.
   * Otherwise you should use `upload` which demands an `onConsumed` to be wrapped in a future.
   *
   * @param fileProperty
   * @param onUpload
   * @param materializer
   * @return the resulting Route
   */
  protected def uploadUndispatchedConsume(fileProperty: String = "file")(onUpload: (InputStream, String) => Route)(implicit materializer: Materializer): Route = {
    fileUpload(fileProperty) {
      case (fileInfo, byteSource) =>
        onUpload(byteSource.runWith(StreamConverters.asInputStream()), fileInfo.fileName)
    }
  }

  protected def uploadOpt(fileProperty: String = "file")(onUpload: Option[(InputStream, String)] => Route)(implicit materializer: Materializer): Route = {
    entity(as[Multipart.FormData]) { formData =>
      onSuccess(formData.parts.filter(_.name == fileProperty).runFold[Option[Multipart.FormData.BodyPart]](None)((_, part) => Some(part))) {
        case Some(_) => uploadUndispatchedConsume(fileProperty) { (stream, name) => onUpload(Some((stream, name))) }
        case _       => onUpload(None)
      }
    }
  }

  protected def storeToFileStore(fileType: FileType, name: Option[String] = None, content: InputStream, fileName: String)(onUpload: (String, FileStoreFileMetadata) => Route, onError: Option[FileStoreError => Route] = None): Route = {
    val id = name.getOrElse(UUID.randomUUID.toString)
    onSuccess(fileStore.putFile(fileType.bucket, Some(id), FileStoreFileMetadata(fileName, fileType), content)) {
      case Left(e)         => onError.map(_(e)).getOrElse(complete(StatusCodes.BadRequest, s"File of file type ${fileType} with id ${id} could not be stored. Error: ${e}"))
      case Right(metadata) => onUpload(id, metadata)
    }
  }

  protected def uploadStored(fileType: FileType, name: Option[String] = None)(onUpload: (String, FileStoreFileMetadata) => Route, onError: Option[FileStoreError => Route] = None)(implicit materializer: Materializer): Route = {
    uploadUndispatchedConsume() { (content, fileName) =>
      storeToFileStore(fileType, name, content, fileName)(onUpload, onError)
    }
  }

  protected def generateReport[I](
    id: Option[I],
    reportFunction: ReportConfig[I] => Future[Either[ServiceFailed, ReportServiceResult[I]]]

  )(idFactory: Long => I)(implicit subject: Subject, materializer: Materializer, classTag: ClassTag[I]): Route = {
    uploadOpt("vorlage") { maybeVorlage =>
      formFieldMap { fields =>
        (for {
          vorlageId <- Try(fields.get("projektVorlageId").map(_.toLong).map(ProjektVorlageId.apply))
          datenExtrakt <- Try(fields.get("datenExtrakt").map(_.toBoolean).getOrElse(false))
          vorlage <- loadVorlage(datenExtrakt, maybeVorlage, vorlageId)
          pdfGenerieren <- Try(fields.get("pdfGenerieren").map(_.toBoolean).getOrElse(false))
          pdfAblegen <- Try(fields.get("pdfAblegen").map(_.toBoolean).getOrElse(false))
          downloadFile <- Try(fields.get("pdfDownloaden").map(_.toBoolean).getOrElse(false))
          pdfMerge <- Try(fields.get("pdfMerge").map(_ == "pdfMerge").getOrElse(false))
          ids = id.map(id => Seq(id)).orElse(fields.get("ids").map(_.split(",").map(i => idFactory(i.toLong)).toSeq))
            .getOrElse(Seq.empty[I])
        } yield {
          logger.debug(s"generateReport: ids: $ids, pdfGenerieren: $pdfGenerieren, pdfAblegen: $pdfAblegen, downloadFile: $downloadFile")
          val config = ReportConfig[I](ids, vorlage, pdfGenerieren, pdfAblegen, downloadFile, pdfMerge)

          onSuccess(reportFunction(config)) {
            case Left(serviceError) =>
              complete(StatusCodes.BadRequest, s"Der Bericht konnte nicht erzeugt werden:$serviceError")
            case Right(result) =>
              result.result match {
                case ReportDataResult(id, json) =>
                  respondWithHeader(`Content-Disposition`(ContentDispositionTypes.attachment, Map(("filename", s"$id.json")))) {
                    complete(json)
                  }
                case AsyncReportResult(jobId) =>
                  // async result, return jobId
                  val ayncResult = AsyncReportServiceResult(jobId, result.validationErrors.map(_.asJson))
                  complete(ayncResult)
                case _ if result.hasErrors =>
                  val errorString = result.validationErrors.map(_.message).mkString(",")
                  complete(StatusCodes.BadRequest, s"Der Bericht konnte nicht erzeugt werden: $errorString")
                case x =>
                  logger.error(s"Received unexpected result:$x")
                  complete(StatusCodes.BadRequest, s"Der Bericht konnte nicht erzeugt werden")
              }
          }
        }) match {
          case Success(result) => result
          case Failure(error)  => complete(StatusCodes.BadRequest, s"Der Bericht konnte nicht erzeugt werden: $error")
        }
      }
    }
  }

  private def loadVorlage(datenExtrakt: Boolean, file: Option[(InputStream, String)], vorlageId: Option[ProjektVorlageId]): Try[BerichtsVorlage] = {
    (datenExtrakt, file, vorlageId) match {
      case (true, _, _)                   => Success(DatenExtrakt)
      case (false, Some((is, _)), _)      => Try(EinzelBerichtsVorlage(LazyList.continually(is.read).takeWhile(-1 !=).map(_.toByte).toArray))
      case (false, None, Some(vorlageId)) => Success(ProjektBerichtsVorlage(vorlageId))
      case _                              => Success(StandardBerichtsVorlage)
    }
  }
}
