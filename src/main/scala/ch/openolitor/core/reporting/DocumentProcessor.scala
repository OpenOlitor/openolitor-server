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

import java.io._
import java.text.DecimalFormat
import java.util.Locale
import java.util.UUID.randomUUID

import ch.openolitor.core.reporting.odf.{ NestedImageIterator, NestedTextboxIterator }
import ch.openolitor.util.jsonpath.JsonPath
import ch.openolitor.util.jsonpath.functions.{ JsonPathFunctions, Param1JsonPathFunction, UnaryJsonPathFunction }
import com.typesafe.scalalogging.LazyLogging
import org.apache.batik.transcoder.image.PNGTranscoder
import org.apache.batik.transcoder.{ TranscoderInput, TranscoderOutput }
import org.joda.time.format.{ DateTimeFormat, DateTimeFormatter, ISODateTimeFormat }
import org.odftoolkit.odfdom.`type`.Color
import org.odftoolkit.odfdom.dom.element.draw.DrawTextBoxElement
import org.odftoolkit.odfdom.pkg.OdfElement
import org.odftoolkit.odfdom.pkg.manifest.OdfFileEntry
import org.odftoolkit.simple._
import org.odftoolkit.simple.common.field._
import org.odftoolkit.simple.draw._
import org.odftoolkit.simple.table._
import org.odftoolkit.simple.text._
import org.odftoolkit.simple.text.list._
import spray.json._

import scala.annotation.tailrec
import scala.collection.JavaConversions._
import scala.util.Try
import scala.util.matching.Regex

case class Value(jsValue: JsValue, value: String)

class ReportException(msg: String) extends Exception(msg)

trait DocumentProcessor extends LazyLogging {

  import OdfToolkitUtils._

  val qrCodeFilename = "./qrCode"

  val dateFormatPattern: Regex = """date:\s*"(.*)"(-\w*)?""".r
  val numberFormatPattern: Regex = """number:\s*"(\[([#@]?[\w\.]+)\])?([#,.0]+)(;((\[([#@]?[\w\.]+)\])?-([#,.0]+)))?"(-\w*)?""".r
  val orderByPattern: Regex = """orderBy:\s*"([\w\.]+)"(:ASC|:DESC)?(-\w*)?""".r
  val backgroundColorFormatPattern: Regex = """bg-color:\s*"(.+)"(-\w*)?""".r
  val foregroundColorFormatPattern: Regex = """fg-color:\s*"(.+)"(-\w*)?""".r
  val replaceFormatPattern: Regex = """replace:\s*((\w+\s*\-\>\s*\w+\,?\s?)*)(-\w*)?""".r
  val dateFormatter = ISODateTimeFormat.dateTime
  val libreOfficeDateFormat: DateTimeFormatter = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss")

  val unaryFunctionsPattern: Regex = """\$(sum|avg|min|max|count|debug)\("?([^"]+)"?\)""".r
  val param1FunctionsPattern: Regex = """\$(groupBy|mkString|debug)\("?([^"]+)"?,\s*"?(.*)"?\)""".r

  val parentPathPattern: Regex = """\$parent\.(.*)""".r
  val absoluteJsonPathPattern: Regex = """\$(.*)""".r
  val resolvePropertyPattern: Regex = """@(.*)""".r
  val staticTextPattern: Regex = """\"(.*)\"""".r
  val noFillTextPattern: Regex = """noFill-.*""".r
  val propertyPattern: Regex = """(.*)-\w+""".r

  val unaryFunctionMap: Map[String, UnaryJsonPathFunction] = Map[String, UnaryJsonPathFunction](
    "sum" -> JsonPathFunctions.Sum,
    "count" -> JsonPathFunctions.Count,
    "min" -> JsonPathFunctions.Min,
    "max" -> JsonPathFunctions.Max,
    "avg" -> JsonPathFunctions.Average,
    "debug" -> JsonPathFunctions.Debug
  )

  val param1FunctionsMap: Map[String, Param1JsonPathFunction] = Map[String, Param1JsonPathFunction](
    "groupBy" -> JsonPathFunctions.GroupBy,
    "mkString" -> JsonPathFunctions.MkString,
    "debug" -> JsonPathFunctions.Debug1Param
  )

  val colorMap: Map[String, Color] = Map(
    // english words
    "AQUA" -> Color.AQUA,
    "BLACK" -> Color.BLACK,
    "BLUE" -> Color.BLUE,
    "FUCHSIA" -> Color.FUCHSIA,
    "GRAY" -> Color.GRAY,
    "GREEN" -> Color.GREEN,
    "LIME" -> Color.LIME,
    "MAROON" -> Color.MAROON,
    "NAVY" -> Color.NAVY,
    "OLIVE" -> Color.OLIVE,
    "ORANGE" -> Color.ORANGE,
    "PURPLE" -> Color.PURPLE,
    "RED" -> Color.RED,
    "SILVER" -> Color.SILVER,
    "TEAL" -> Color.TEAL,
    "WHITE" -> Color.WHITE,
    "YELLOW" -> Color.YELLOW,

    // german words
    "SCHWARZ" -> Color.BLACK,
    "BLAU" -> Color.BLUE,
    "GRAU" -> Color.GRAY,
    "GRUEN" -> Color.GREEN,
    "VIOLETT" -> Color.PURPLE,
    "ROT" -> Color.RED,
    "SILBER" -> Color.SILVER,
    "WEISS" -> Color.WHITE,
    "GELB" -> Color.YELLOW,

    // french words
    "NOIR" -> Color.BLACK,
    "BLEU" -> Color.BLUE,
    "GRIS" -> Color.GRAY,
    "VERT" -> Color.GREEN,
    "VIOLET" -> Color.PURPLE,
    "ROUGE" -> Color.RED,
    "BLANC" -> Color.WHITE,
    "JAUNE" -> Color.YELLOW
  )

  def processDocument(doc: TextDocument, data: JsValue, locale: Locale = Locale.getDefault): Try[Boolean] = {
    logger.debug(s"processDocument with data: ${data.prettyPrint}")
    doc.setLocale(locale)

    val rootPath = Seq(data)

    for {
      props <- Try(extractProperties(data))
      // process header
      _ <- Try(processVariables(doc.getHeader, props))
      _ <- Try(processTables(doc.getHeader, locale, rootPath, withinContainer = false))
      //_ <- Try(processLists(doc.getHeader, props, locale, ""))
      headerContainer = new GenericParagraphContainerImpl(doc.getHeader.getOdfElement)
      _ <- Try(processTextboxes(headerContainer, locale, rootPath))

      // process footer
      _ <- Try(processVariables(doc.getFooter, props))
      _ <- Try(processTables(doc.getFooter, locale, rootPath, withinContainer = false))
      //_ <- Try(processLists(doc.getFooter, props, locale, ""))
      footerContainer = new GenericParagraphContainerImpl(doc.getFooter.getOdfElement)
      _ <- Try(processTextboxes(footerContainer, locale, rootPath))

      // process content, order is important
      _ <- Try(processVariables(doc, props))
      _ <- Try(processTables(doc, locale, rootPath, withinContainer = false))
      _ <- Try(processLists(doc, locale, rootPath))
      _ <- Try(processFrames(doc, locale, rootPath))
      _ <- Try(processSections(doc, locale, rootPath))
      _ <- Try(processTextboxes(doc, locale, rootPath))
      _ <- Try(processImages(doc, locale, rootPath))
      //TODO Reactivate a smarter way (i.e. only boolean and number fields?)
      //_ <- Try(registerVariables(doc, props))
    } yield true
  }

  /**
   * Resolve property from json and apply optionally additional functions after resolving the property
   *
   */
  private def resolvePropertyFromJson(property: String, paths: Seq[JsValue]): Option[Vector[JsValue]] = {
    def resolveProperty(property: String, path: Seq[JsValue]) = {
      val json = path.last
      JsonPath.query(property, json) match {
        case Right(values) => values match {
          case Vector() =>
            // an empty vectors indicates that the property was not found
            None
          case values: Any =>
            // flatten value because query might be a list of JsValue or a single value from a JsArray proprty
            val flattenedValues: Vector[JsValue] = values.flatMap {
              case JsArray(elements) => elements
              case x: Any            => Vector(x)
            }
            Some(flattenedValues)
        }
        case Left(error) =>
          logger.warn(s"Could not resolve json path:$property on json:$json", error)
          None
      }
    }

    property match {
      case unaryFunctionsPattern(function, path) =>
        logger.debug(s"Found unary function:$function:$path")
        // evaluate unary function
        val (propertyKey, propertyPath) = parsePropertyKey(path, paths)
        resolveProperty(propertyKey, propertyPath).flatMap(result => unaryFunctionMap.get(function).flatMap(_.evaluate(result)))
      case param1FunctionsPattern(function, param1, path) =>
        logger.debug(s"Found function with one param:$function:$param1:$path")
        // evaluate function with one parameter
        val (propertyKey, propertyPath) = parsePropertyKey(path, paths)
        resolveProperty(propertyKey, propertyPath).flatMap(result => param1FunctionsMap.get(function).flatMap(_.evaluate(param1, result)))
      case _ =>
        val (propertyKey, propertyPath) = parsePropertyKey(property, paths)
        logger.debug(s"Resolved property:$property:$propertyKey")
        resolveProperty(propertyKey, propertyPath)
    }
  }

  /**
   * Register all values as variables to conditional field might react on them
   */
  protected def registerVariables(doc: TextDocument, props: Map[String, Value]): Unit = {
    props.foreach {
      case (_, Value(JsObject(_), _)) =>
      //ignore object properties
      case (property, Value(JsArray(values), _)) =>
        //register length of array as variable
        val field = Fields.createSimpleVariableField(doc, property + "_length")
        field.updateField(values.length.toString, doc.getContentRoot)
      case (property, value) =>
        logger.debug(s"Register variable:$property")
        val field = Fields.createSimpleVariableField(doc, property)
        value match {
          case Value(JsNull, _) =>
            field.updateField("", doc.getContentRoot)
          case Value(JsString(str), _) =>
            field.updateField(str, doc.getContentRoot)
          case Value(JsBoolean(bool), _) =>
            field.updateField(if (bool) "1" else "0", doc.getContentRoot)
          case Value(JsNumber(number), _) =>
            field.updateField(number.toString, doc.getContentRoot)
        }
    }
  }

  /**
   * Update variables in variablecontainer based on a data object. Every variables name represents a property
   * accessor which should resolve nested properties as well (with support for arrays .index notation i.e. 'adressen.0.strasse').
   */
  protected def processVariables(cont: VariableContainer, props: Map[String, Value]): Unit = {
    props foreach {
      case (property, Value(_, value)) =>
        Option(cont.getVariableFieldByName(property)) map { variable =>
          logger.debug(s"Update variable:property -> $value")
          variable.updateField(value, null)
        }
    }
  }

  protected def processTables(doc: TableContainer, locale: Locale, paths: Seq[JsValue], withinContainer: Boolean): Unit = {
    doc.getTableList foreach (table => processTable(table, locale, paths, withinContainer))
  }

  /**
   * Process table:
   * duplicate all rows except header rows. Try to replace textbox values with value from property map
   */
  protected def processTable(table: Table, locale: Locale, paths: Seq[JsValue], withinContainer: Boolean): Unit = {
    val propertyName = table.getDotTableName match {
      case propertyPattern(tableName) => tableName
      case fullName: Any              => fullName
    }
    val (name, structuring) = parseFormats(propertyName)

    resolvePropertyFromJson(name, paths) map { values =>
      val valuesStruct = applyStructure(values, structuring)
      logger.debug(s"processTable (dynamic): $name")
      processTableWithValues(table, valuesStruct, locale, paths, withinContainer, name)

    } getOrElse {
      //static proccesing
      logger.debug(s"processTable (static): $name: $name")
      processStaticTable(table, locale, paths)
    }
  }

  protected def processStaticTable(table: Table, locale: Locale = Locale.getDefault, paths: Seq[JsValue]): Unit = {
    for (r <- 0 until table.getRowCount) {
      //replace textfields
      for (c <- 0 until table.getColumnCount) {
        val cell = table.getCellByPosition(c, r)
        processTextboxes(cell, locale, paths)
      }
    }
  }

  protected def processLists(doc: ListContainer, locale: Locale, paths: Seq[JsValue]): Unit = {
    for {
      list <- doc.getListIterator
    } processList(list, locale, paths)
  }

  /**
   * Process list:
   * process content of every list item as paragraph container
   */
  protected def processList(list: List, locale: Locale, paths: Seq[JsValue]): Unit = {
    for {
      item <- list.getItems
    } yield {
      val container = new GenericParagraphContainerImpl(item.getOdfElement)
      processTextboxes(container, locale, paths)
    }
  }

  protected def processTableWithValues(table: Table, values: Vector[(JsValue, Int)], locale: Locale, paths: Seq[JsValue], withinContainer: Boolean, tableName: String): Unit = {
    val startIndex = Math.max(table.getHeaderRowCount, 0)
    val rows = table.getRowList.toList
    val nonHeaderRows = rows.takeRight(rows.length - startIndex)

    logger.debug(s"processTable: $values, $tableName -> Header rows: ${table.getHeaderRowCount}")

    for (index <- values) {
      val rowPath = paths :+ index._1

      //copy rows
      val newRows = if (withinContainer) table.appendRow() :: Nil else table.appendRows(nonHeaderRows.length).toList
      logger.debug(s"processTable: $index, $tableName $withinContainer -> Appended rows: ${newRows.length}, nonHeaderRows:${nonHeaderRows.length}")
      for (r <- newRows.indices) {
        val row = nonHeaderRows.get(r)
        val newRow = newRows.get(r)
        //replace textfields
        for (cell <- 0 until table.getColumnCount) {
          val origCell = row.getCellByIndex(cell)
          val newCell = newRow.getCellByIndex(cell)

          // copy cell content
          copyCell(origCell, newCell)

          // replace textfields
          processTextboxes(newCell, locale, rowPath)
        }
      }
    }

    //remove template rows
    logger.debug(s"processTable: $tableName -> Remove template rows from:$startIndex, count: ${nonHeaderRows.length}.")
    if (nonHeaderRows.length == table.getRowCount) {
      //remove whole table
      table.remove()
    } else {
      table.removeRowsByIndex(startIndex, nonHeaderRows.length)
    }
  }

  private def copyCell(source: Cell, dest: Cell): Unit = {
    //clone cell
    //clean nodes in copies cell
    val childNodes = dest.getOdfElement.getChildNodes
    for (c <- 0 until childNodes.getLength) {
      dest.getOdfElement.removeChild(childNodes.item(c))
    }

    val sourceChildNodes = source.getOdfElement.getChildNodes
    for (c <- 0 until sourceChildNodes.getLength) {
      dest.getOdfElement.appendChild(sourceChildNodes.item(c).cloneNode(true))
    }
  }

  /**
   * Process section which are part of the property map and append it to the document
   */
  private def processSections(doc: TextDocument, locale: Locale, paths: Seq[JsValue]): Unit = {
    for {
      s <- doc.getSectionIterator
    } processSection(doc, s, locale, paths)
  }

  private def processSection(doc: TextDocument, section: Section, locale: Locale, paths: Seq[JsValue]): Unit = {
    val propertyName = section.getName match {
      case propertyPattern(sectionName) => sectionName
      case fullName: Any                => fullName
    }
    val (name, structuring) = parseFormats(propertyName)
    resolvePropertyFromJson(name, paths) map { values =>
      val valuesStruct = applyStructure(values, structuring)
      processSectionWithValues(doc, section, valuesStruct, locale, paths, name)
    } getOrElse logger.debug(s"Section not mapped to property, will be processed statically:$name")
  }

  private def processSectionWithValues(doc: TextDocument, section: Section,
    values: Vector[(JsValue, Int)], locale: Locale, paths: Seq[JsValue],
    name: String): Unit = {
    for (index <- values) {
      val sectionKey = s"$name[${index._2}]"
      //append section
      val newSection = doc.appendSection(section, false)
      newSection.setName(s"noFill-copiedSection${randomUUID().toString}")

      logger.debug(s"processSection:$sectionKey")
      val childPath = paths :+ index._1
      processTextboxes(newSection, locale, childPath)
      processTables(newSection, locale, childPath, withinContainer = true)
      processLists(newSection, locale, childPath)
    }

    //remove template section
    section.remove()
  }

  /**
   * Process frames which are part of the property map and append it to the document
   */
  private def processFrames(doc: TextDocument, locale: Locale, paths: Seq[JsValue]): Unit = {
    for {
      p <- doc.getParagraphIterator
      f <- p.getFrameIterator()
    } processFrame(p, f, locale, paths)
  }

  private def processFrame(p: Paragraph, frame: Frame, locale: Locale, paths: Seq[JsValue]) = {

    // only proces real frame. As frames and textboxes are represented the same in the DOM we assume
    // that a frame is represented by having more than one children. Textboxes itself have the following structure
    // <draw:frame><draw:text-box><p:text></p:text></draw:text-box></draw:frame>
    //
    // Where frames have:
    // <draw:frame><draw:text-box><p:text></p:text><p:text></p:text></draw:text-box></draw:frame>
    // just have multiple paragraphs as we want to replace multiple values
    if (frame.getDrawFrameElement.getFirstChild.getChildNodes.getLength > 1) {

      val propertyName = frame.getName match {
        case propertyPattern(frameName) => frameName
        case fullName: Any              => fullName
      }

      val (name, structuring) = parseFormats(propertyName)
      resolvePropertyFromJson(name, paths) map { values =>
        val valuesStruct = applyStructure(values, structuring)
        processFrameWithValues(p, frame, valuesStruct, locale, paths, name)
      } getOrElse logger.debug(s"Frame not mapped to property, will be processed statically:$name")
    } else {
      logger.debug(s"Frame seems to be a textbox:${frame.getName}")
    }
  }

  private def processFrameWithValues(p: Paragraph, frame: Frame, values: Vector[(JsValue, Int)], locale: Locale, paths: Seq[JsValue], name: String) = {
    for (index <- values) {
      val key = s"$name[${index._2}]"
      logger.debug(s"---------------------------processFrame:$key")
      //append section
      val newFrame = p.appendFrame(frame)
      newFrame.setName(s"noFill-copiedFrame${randomUUID().toString}")

      // process textboxes starting below first child which has to be a <text-box>
      val firstTextBox = OdfElement.findFirstChildNode(classOf[DrawTextBoxElement], newFrame.getOdfElement)
      val container = new GenericParagraphContainerImpl(firstTextBox)
      processTextboxes(container, locale, paths :+ index._1)
    }
    //remove template
    frame.remove()
  }

  /**
   * Process images and fill in content based on
   *
   */
  private def processImages(cont: TextDocument, locale: Locale, paths: Seq[JsValue]): Unit = {
    for {
      p <- cont.getParagraphIterator
      t <- new NestedImageIterator(p.getFrameContainerElement)
    } {
      val propertyName = t.getName match {
        case propertyPattern(imageName) => imageName
        case fullName: Any              => fullName
      }
      val (name, formats) = parseFormats(propertyName)
      logger.debug(s"--------------------processImage: $name | formats:$formats")

      // resolve textbox content from properties, otherwise only apply formats to current content
      resolvePropertyFromJson(name, paths) map {
        _.headOption.map {
          case JsString(value) =>
            if (!value.isEmpty) {
              resolvePropertyFromJson("referenzNummer", paths) flatMap {
                _.headOption.map {
                  case JsString(referenzNummer) =>
                    // create a new svg file with the svg qrcode content in the value
                    val fileSVG = File.createTempFile("qrcSVG", ".svg")
                    val pw = new PrintWriter(fileSVG)
                    pw.write(value)
                    pw.close()

                    val filePNG = File.createTempFile("qrcPNG", ".png")
                    //transform the svg in a png and getting the reference of the file
                    svg2png(fileSVG, filePNG, referenzNummer)

                    val templateFilename = t.getInternalPath
                    //remove the template image
                    cont.getPackage().remove(templateFilename)
                    cont.newImage(filePNG.toURI)
                    //insert the new image
                    val mediaType = OdfFileEntry.getMediaTypeString(filePNG.toURI.toString)
                    cont.getPackage.insert(filePNG.toURI, templateFilename, mediaType)
                    //delete both svg and png files
                    fileSVG.delete
                    filePNG.delete
                  case _ =>
                }
              }
            }
          case _ =>
        }
      }
    }
  }

  private def svg2png(svgFile: File, pngFile: File, referenzNummer: String) {
    val svg_URI_input = svgFile.toURL.toString
    val input_svg_image = new TranscoderInput(svg_URI_input)
    val png_ostream = new FileOutputStream(pngFile)
    val output_png_image = new TranscoderOutput(png_ostream)
    val my_converter = new PNGTranscoder()
    my_converter.transcode(input_svg_image, output_png_image)
    png_ostream.flush()
    png_ostream.close()
  }

  /**
   * Process textboxes and fill in content based on
   *
   * If pathPrefixes is Nil, applyFormats will not be executed. If property value is not found, bind text to empty string value
   * to overwrite the previously copied dynamic value
   */
  private def processTextboxes(cont: ParagraphContainer, locale: Locale, paths: Seq[JsValue]): Unit = {
    for {
      p <- cont.getParagraphIterator
      t <- new NestedTextboxIterator(p.getFrameContainerElement)
    } {
      val propertyName = t.getName match {
        case propertyPattern(textboxName) => textboxName
        case fullName: Any                => fullName
      }
      val (name, formats) = parseFormats(propertyName)
      name match {
        case staticTextPattern(text) =>
          logger.debug(s"-----------------processTextbox with static value:$text | formats:$formats | name:$name")
          applyFormats(t, formats, text, locale, paths)
        case noFillTextPattern() => // do nothing
        case property: Any =>
          logger.debug(s"-----------------processTextbox: $property | formats:$formats | name:$name")

          // resolve textbox content from properties, otherwise only apply formats to current content
          t.removeCommonStyle()
          resolvePropertyFromJson(property, paths) map {
            case Vector(JsString(value)) if Try(dateFormatter.parseDateTime(value)).isSuccess =>
              //it is a date, convert to libreoffice compatible datetime format
              val convertedDate = dateFormatter.parseDateTime(value).toString(libreOfficeDateFormat)
              applyFormats(t, formats, convertedDate, locale, paths)
            case Vector(JsString(value)) =>
              applyFormats(t, formats, value, locale, paths)
            case Vector(JsNumber(value)) =>
              applyFormats(t, formats, value.toString, locale, paths)
            case _: Any =>
              applyFormats(t, formats, "", locale, paths)
          } getOrElse {
            if (paths.nonEmpty) {
              applyFormats(t, formats, "", locale, paths)
            }
          }
      }
    }
  }

  @tailrec
  private def applyFormats(textbox: Textbox, formats: Seq[String], value: String, locale: Locale, paths: Seq[JsValue]): String = {
    formats match {
      case Nil => applyFormat(textbox, "", value, locale, paths)
      case s :: _ if s.contains("orderBy") =>
        logger.info(s"---------------------------------------------")
        applyFormat(textbox, "", value, locale, paths)
      case format :: tail =>
        val formattedValue = applyFormat(textbox, format, value, locale, paths)
        applyFormats(textbox, tail, formattedValue, locale, paths)
    }
  }

  private def correctPropertyName(name: String): String = {
    // replace all dot based accesses to arrays as from user fields bracets are not supported
    val adjustedName = name.replaceAll("""\.(\d+)""", "[$1]")
    adjustedName
  }

  private def parsePropertyKey(name: String, paths: Seq[JsValue] = Nil): (String, Seq[JsValue]) = {
    // replace all dot based accesses to arrays as from user fields bracets are not supported
    val adjustedName = name.replaceAll("""\.(\d+)""", "[$1]")

    findJsonPath(adjustedName, paths)
  }

  @tailrec
  private def findJsonPath(name: String, paths: Seq[JsValue] = Nil): (String, Seq[JsValue]) = {
    name match {
      case parentPathPattern(rest) if paths.size > 1 => findJsonPath(rest, paths.dropRight(1))
      case parentPathPattern(rest) =>
        logger.warn(s"Tried to access parent path on root, remove parent reference: $name")
        findJsonPath(rest, paths)
      case absoluteJsonPathPattern(_) => (name, Seq(paths.head))
      case n if n.startsWith("[") =>
        // append root anchor to name
        ("$" + name, paths)
      case _ =>
        // append root anchor to name
        ("$." + name, paths)
    }
  }

  /**
   *
   */
  private def applyFormat(textbox: Textbox, format: String, value: String, locale: Locale, paths: Seq[JsValue]): String = {
    format match {
      case dateFormatPattern(pattern, _) if !value.isEmpty =>
        // parse date
        val formattedDate = libreOfficeDateFormat.parseDateTime(value).toString(pattern, locale)
        logger.debug(s"Formatted date with pattern $pattern => $formattedDate")
        formattedDate
      case backgroundColorFormatPattern(pattern, _) =>
        // set background to textbox
        val color = resolveColor(pattern, paths)
        logger.debug(s"setBackgroundColor:$color")
        textbox.setBackgroundColorWithNewStyle(color)
        value
      case foregroundColorFormatPattern(pattern, _) =>
        // set foreground to textbox (unfortunately resets font style to default)
        val color = resolveColor(pattern, paths)
        textbox.setFontColor(color)
        value
      case numberFormatPattern(_, positiveColor, positivePattern, _, _, _, negativeColor, negativeFormat, _) if !value.isEmpty =>
        // lookup color value
        val number = value.toDouble
        if (number < 0 && negativeFormat != null) {
          val formattedValue = decimaleFormatForLocale(negativeFormat, locale).format(value.toDouble)
          if (negativeColor != null) {
            val color = resolveColor(negativeColor, paths)
            logger.debug(s"Resolved native color:$color")
            textbox.setFontColor(color)
          } else {
            textbox.setFontColor(Some(Color.BLACK))
          }
          formattedValue
        } else {
          val formattedValue = decimaleFormatForLocale(positivePattern, locale).format(value.toDouble)
          if (positiveColor != null) {
            val color = resolveColor(positiveColor, paths)
            logger.debug(s"Resolved positive color:$color")
            textbox.setFontColor(color)
          } else {
            textbox.setFontColor(Some(Color.BLACK))
          }
          formattedValue
        }
      case replaceFormatPattern(stringMap, _, _) =>
        val replaceMap = (stringMap split "\\s*,\\s*" map (_ split "\\s*->\\s*") map { case Array(k, v) => k -> v }).toMap
        replaceMap find { case (k, _) => value.contains(k) } map { case (k, v) => value.replaceAll(k, v) } getOrElse value
      case _ if format.length > 0 && !value.isEmpty =>
        logger.warn(s"Unsupported format:$format")
        textbox.setTextContentStyleAware(value)
        value
      case _ =>
        textbox.setTextContentStyleAware(value)
        value
    }
  }

  private def decimaleFormatForLocale(pattern: String, locale: Locale): DecimalFormat = {
    val decimalFormat = java.text.NumberFormat.getNumberInstance(locale).asInstanceOf[DecimalFormat]
    decimalFormat.applyPattern(pattern)
    decimalFormat
  }

  private def parseFormats(name: String): (String, Seq[String]) = {
    if (name == null || name.trim.isEmpty) {
      ("", Nil)
    } else {
      name.split('|').toList match {
        case name :: Nil  => (correctPropertyName(name.trim), Nil)
        case name :: tail => (correctPropertyName(name.trim), tail.map(_.trim))
        case _            => (correctPropertyName(name), Nil)
      }
    }
  }

  private def resolveColor(color: String, paths: Seq[JsValue]): Option[Color] = {
    color match {
      case resolvePropertyPattern(property) =>
        //resolve color in props
        resolvePropertyFromJson(property, paths) flatMap {
          case Vector(JsString(value)) =>
            resolveColor(value, paths)
          case Vector() => None
          case x: Any =>
            logger.warn(s"Unable to resolve color from property: $x")
            None
        }
      case color: Any =>
        if (Color.isValid(color)) {
          Some(Color.valueOf(color))
        } else {
          colorMap.get(color.toUpperCase).orElse {
            logger.debug(s"Unsupported color: $color")
            None
          }
        }
    }
  }

  private def applyStructure(values: Vector[JsValue], structure: Seq[String]): Vector[(JsValue, Int)] = {
    val indexed = values.zipWithIndex
    structure match {
      case first +: _ =>
        first match {
          case orderByPattern(orderProp, direction, _) =>
            val ordering = direction match {
              case ":DESC" => Ordering[String].reverse
              case _       => Ordering[String]
            }
            indexed.sortBy(e => extractProperties(e._1).getOrElse(orderProp, Value(JsNull, "")).value)(ordering)
          case _ => indexed
        }
      case _ => indexed
    }
  }

  /**
   * Extract all properties performing a deep lookup on the given jsvalue
   */
  def extractProperties(data: JsValue, prefix: String = ""): Map[String, Value] = {
    val childPrefix = if (prefix == "") prefix else s"$prefix."
    data match {
      case j @ JsObject(value) ⇒ value.map {
        case (k, v) =>
          extractProperties(v, s"$childPrefix$k")
      }.flatten.toMap + (prefix -> Value(j, ""))
      case j @ JsArray(values) =>
        values.zipWithIndex.flatMap {
          case (v, index) => extractProperties(v, s"$childPrefix$index")
        }.toMap + (prefix -> Value(j, ""))
      case j @ JsNull => Map(prefix -> Value(j, ""))
      case j @ JsString(value) if Try(dateFormatter.parseDateTime(value)).isSuccess =>
        //it is a date, convert to libreoffice compatible datetime format
        val convertedDate = dateFormatter.parseDateTime(value).toString(libreOfficeDateFormat)
        Map(prefix -> Value(j, convertedDate))
      case j @ JsString(value) => Map(prefix -> Value(j, value))
      case value: Any          => Map(prefix -> Value(value, value.toString))
    }
  }
}

class GenericParagraphContainerImpl(containerElement: OdfElement) extends AbstractParagraphContainer {
  def getParagraphContainerElement: OdfElement = {
    containerElement
  }
}
