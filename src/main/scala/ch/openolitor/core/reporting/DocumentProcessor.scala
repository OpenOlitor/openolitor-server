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
import com.typesafe.scalalogging.LazyLogging
import org.apache.batik.transcoder.image.PNGTranscoder
import org.apache.batik.transcoder.{ TranscoderInput, TranscoderOutput }
import org.joda.time.format.{ DateTimeFormat, ISODateTimeFormat }
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

case class Value(jsValue: JsValue, value: String)

class ReportException(msg: String) extends Exception(msg)

trait DocumentProcessor extends LazyLogging {
  import OdfToolkitUtils._

  val qrCodeFilename = "./qrCode"

  val dateFormatPattern = """date:\s*"(.*)"(-\w*)?""".r
  val numberFormatPattern = """number:\s*"(\[([#@]?[\w\.]+)\])?([#,.0]+)(;((\[([#@]?[\w\.]+)\])?-([#,.0]+)))?"(-\w*)?""".r
  val orderByPattern = """orderBy:\s*"([\w\.]+)"(:ASC|:DESC)?(-\w*)?""".r
  val backgroundColorFormatPattern = """bg-color:\s*"(.+)"(-\w*)?""".r
  val foregroundColorFormatPattern = """fg-color:\s*"(.+)"(-\w*)?""".r
  val replaceFormatPattern = """replace:\s*((\w+\s*\-\>\s*\w+\,?\s?)*)(-\w*)?""".r
  val dateFormatter = ISODateTimeFormat.dateTime
  val libreOfficeDateFormat = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss")

  val parentPathPattern = """\$parent\.(.*)""".r
  val absoluteJsonPathPattern = """\$[\.\[](.*)""".r
  val resolvePropertyPattern = """@(.*)""".r
  val staticTextPattern = """\"(.*)\"""".r
  val noFillTextPattern = """noFill-.*""".r
  val propertyPattern = """(.*)-\w+""".r

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

    val rootPathPrefix = Seq("$")

    for {
      props <- Try(extractProperties(data))
      // process header
      _ <- Try(processVariables(doc.getHeader, props))
      _ <- Try(processTables(data, doc.getHeader, locale, rootPathPrefix, false))
      //_ <- Try(processLists(doc.getHeader, props, locale, ""))
      headerContainer = new GenericParagraphContainerImpl(doc.getHeader.getOdfElement)
      _ <- Try(processTextboxes(data, headerContainer, locale, rootPathPrefix))

      // process footer
      _ <- Try(processVariables(doc.getFooter, props))
      _ <- Try(processTables(data, doc.getFooter, locale, rootPathPrefix, false))
      //_ <- Try(processLists(doc.getFooter, props, locale, ""))
      footerContainer = new GenericParagraphContainerImpl(doc.getFooter.getOdfElement)
      _ <- Try(processTextboxes(data, footerContainer, locale, rootPathPrefix))

      // process content, order is important
      _ <- Try(processVariables(doc, props))
      _ <- Try(processTables(data, doc, locale, rootPathPrefix, false))
      _ <- Try(processLists(data, doc, locale, rootPathPrefix))
      _ <- Try(processFrames(data, doc, locale, rootPathPrefix))
      _ <- Try(processSections(data, doc, locale, rootPathPrefix))
      _ <- Try(processTextboxes(data, doc, locale, rootPathPrefix))
      _ <- Try(processImages(data, doc, locale, rootPathPrefix))
      //TODO Reactivate a smarter way (i.e. only boolean and number fields?)
      //_ <- Try(registerVariables(doc, props))
    } yield true
  }

  private def resolvePropertyFromJson(propertyKey: String, json: JsValue): Option[Vector[JsValue]] =
    JsonPath.query(propertyKey, json).right.toOption.flatMap {
      case Vector() =>
        // an empty vectors indicates that the property was not found
        None
      case x => Some(x)
    }

  /**
   * Register all values as variables to conditional field might react on them
   */
  def registerVariables(doc: TextDocument, props: Map[String, Value]) = {
    props.map {
      case (property, Value(JsObject(_), _)) =>
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
  def processVariables(cont: VariableContainer, props: Map[String, Value]) = {
    props map {
      case (property, Value(_, value)) =>
        cont.getVariableFieldByName(property) match {
          case null =>
          case variable =>
            logger.debug(s"Update variable:property -> $value")
            variable.updateField(value, null)
        }
    }
  }

  def processTables(json: JsValue, doc: TableContainer, locale: Locale, pathPrefixes: Seq[String], withinContainer: Boolean) = {
    doc.getTableList map (table => processTable(json, doc, table, locale, pathPrefixes, withinContainer))
  }

  /**
   * Process table:
   * duplicate all rows except header rows. Try to replace textbox values with value from property map
   */
  def processTable(json: JsValue, doc: TableContainer, table: Table, locale: Locale, pathPrefixes: Seq[String], withinContainer: Boolean) = {
    val propertyName = table.getDotTableName match {
      case propertyPattern(tableName) => tableName
      case fullName                   => fullName
    }
    val (name, structuring) = parseFormats(propertyName)

    val propertyKey = parsePropertyKey(name, pathPrefixes)
    resolvePropertyFromJson(propertyKey, json) map { values =>
      val valuesStruct = applyStructure(values, structuring, pathPrefixes)
      logger.debug(s"processTable (dynamic): ${name}")
      processTableWithValues(json, doc, table, valuesStruct, locale, pathPrefixes, withinContainer, name)

    } getOrElse {
      //static proccesing
      logger.debug(s"processTable (static): ${name}: $pathPrefixes, $propertyKey")
      processStaticTable(json, table, locale, pathPrefixes)
    }
  }

  def processStaticTable(json: JsValue, table: Table, locale: Locale = Locale.getDefault, pathPrefixes: Seq[String]) = {
    for (r <- 0 to table.getRowCount - 1) {
      //replace textfields
      for (c <- 0 to table.getColumnCount - 1) {
        val cell = table.getCellByPosition(c, r)
        processTextboxes(json, cell, locale, pathPrefixes)
      }
    }
  }

  def processLists(json: JsValue, doc: ListContainer, locale: Locale, pathPrefixes: Seq[String]) = {
    for {
      list <- doc.getListIterator
    } processList(json, doc, list, locale, pathPrefixes)
  }

  /**
   * Process list:
   * process content of every list item as paragraph container
   */
  def processList(json: JsValue, doc: ListContainer, list: List, locale: Locale, pathPrefixes: Seq[String]) = {
    for {
      item <- list.getItems
    } yield {
      val container = new GenericParagraphContainerImpl(item.getOdfElement())
      processTextboxes(json, container, locale, pathPrefixes)
    }
  }

  def processTableWithValues(json: JsValue, doc: TableContainer, table: Table, values: Vector[(JsValue, Int)], locale: Locale, pathPrefixes: Seq[String], withinContainer: Boolean, tableName: String) = {
    val startIndex = Math.max(table.getHeaderRowCount, 0)
    val rows = table.getRowList.toList
    val nonHeaderRows = rows.takeRight(rows.length - startIndex)

    logger.debug(s"processTable: $values, ${tableName} -> Header rows: ${table.getHeaderRowCount}")

    for (index <- values) {
      val rowPathPrefix = findPathPrefixes(tableName + s"[${index._2}]", pathPrefixes)

      //copy rows
      val newRows = if (withinContainer) table.appendRow() :: Nil else table.appendRows(nonHeaderRows.length).toList
      logger.debug(s"processTable: $index, ${tableName} $withinContainer -> Appended rows: ${newRows.length}, nonHeaderRows:${nonHeaderRows.length}")
      for (r <- 0 to newRows.length - 1) {
        val row = nonHeaderRows.get(r)
        val newRow = newRows.get(r)
        //replace textfields
        for (cell <- 0 to table.getColumnCount - 1) {
          val origCell = row.getCellByIndex(cell)
          val newCell = newRow.getCellByIndex(cell)

          // copy cell content
          copyCell(origCell, newCell)

          // replace textfields
          processTextboxes(json, newCell, locale, rowPathPrefix)
        }
      }
    }

    //remove template rows
    logger.debug(s"processTable: ${tableName} -> Remove template rows from:$startIndex, count: ${nonHeaderRows.length}.")
    if (nonHeaderRows.length == table.getRowCount) {
      //remove whole table
      table.remove()
    } else {
      table.removeRowsByIndex(startIndex, nonHeaderRows.length)
    }
  }

  private def copyCell(source: Cell, dest: Cell) = {
    //clone cell
    //clean nodes in copies cell
    val childNodes = dest.getOdfElement.getChildNodes
    for (c <- 0 to childNodes.getLength - 1) {
      dest.getOdfElement.removeChild(childNodes.item(c))
    }

    val sourceChildNodes = source.getOdfElement.getChildNodes
    for (c <- 0 to sourceChildNodes.getLength - 1) {
      dest.getOdfElement.appendChild(sourceChildNodes.item(c).cloneNode(true))
    }
  }

  /**
   * Process section which are part of the property map and append it to the document
   */
  private def processSections(json: JsValue, doc: TextDocument, locale: Locale, pathPrefixes: Seq[String]) = {
    for {
      s <- doc.getSectionIterator
    } processSection(json, doc, s, locale, pathPrefixes)
  }

  private def processSection(json: JsValue, doc: TextDocument, section: Section, locale: Locale, pathPrefixes: Seq[String]) = {
    val propertyName = section.getName match {
      case propertyPattern(sectionName) => sectionName
      case fullName                     => fullName
    }
    val (name, structuring) = parseFormats(propertyName)
    val propertyKey = parsePropertyKey(name, pathPrefixes)
    resolvePropertyFromJson(propertyKey, json) map { values =>
      val valuesStruct = applyStructure(values, structuring, pathPrefixes)
      processSectionWithValues(json, doc, section, valuesStruct, locale, pathPrefixes, name)
    } getOrElse logger.debug(s"Section not mapped to property, will be processed statically:${name}")
  }

  private def processSectionWithValues(json: JsValue, doc: TextDocument, section: Section,
    values: Vector[(JsValue, Int)], locale: Locale, pathPrefixes: Seq[String],
    name: String) = {
    for (index <- values) {
      val sectionKey = s"${name}[${index._2}]"
      //append section
      val newSection = doc.appendSection(section, false)
      newSection.setName(s"noFill-copiedSection${randomUUID().toString}")

      logger.debug(s"processSection:$sectionKey")
      processTextboxes(json, newSection, locale, pathPrefixes :+ sectionKey)
      processTables(json, newSection, locale, pathPrefixes :+ sectionKey, true)
      processLists(json, newSection, locale, pathPrefixes :+ sectionKey)
    }

    //remove template section
    section.remove()
  }

  /**
   * Process frames which are part of the property map and append it to the document
   */
  private def processFrames(json: JsValue, doc: TextDocument, locale: Locale, pathPrefixes: Seq[String]) = {
    for {
      p <- doc.getParagraphIterator
      f <- p.getFrameIterator
    } processFrame(json, p, f, locale, pathPrefixes)
  }

  private def processFrame(json: JsValue, p: Paragraph, frame: Frame, locale: Locale, pathPrefixes: Seq[String]) = {
    val propertyName = frame.getName match {
      case propertyPattern(frameName) => frameName
      case fullName                   => fullName
    }
    val (name, structuring) = parseFormats(propertyName)
    val propertyKey = parsePropertyKey(name, pathPrefixes)
    resolvePropertyFromJson(propertyKey, json) collect {
      case Vector(JsArray(values)) =>
        val valuesStruct = applyStructure(values, structuring, pathPrefixes)
        processFrameWithValues(json, p, frame, valuesStruct, locale, pathPrefixes, name)
    } getOrElse (logger.debug(s"Frame not mapped to property, will be processed statically:${name}"))
  }

  private def processFrameWithValues(json: JsValue, p: Paragraph, frame: Frame, values: Vector[(JsValue, Int)], locale: Locale, pathPrefixes: Seq[String], name: String) = {
    for (index <- values) {
      val key = s"${name}[${index._2}]"
      logger.debug(s"---------------------------processFrame:$key")
      //append section
      val newFrame = p.appendFrame(frame)
      newFrame.setName(s"noFill-copiedFrame${randomUUID().toString}")

      // process textboxes starting below first child which has to be a <text-box>
      val firstTextBox = OdfElement.findFirstChildNode(classOf[DrawTextBoxElement], newFrame.getOdfElement())
      val container = new GenericParagraphContainerImpl(firstTextBox)
      processTextboxes(json, container, locale, pathPrefixes :+ key)
    }
    //remove template
    frame.remove()
  }

  /**
   * Process images and fill in content based on
   *
   */
  private def processImages(json: JsValue, cont: TextDocument, locale: Locale, pathPrefixes: Seq[String]) = {
    for {
      p <- cont.getParagraphIterator
      t <- new NestedImageIterator(p.getFrameContainerElement)
    } {
      val propertyName = t.getName match {
        case propertyPattern(imageName) => imageName
        case fullName                   => fullName
      }
      val (name, formats) = parseFormats(propertyName)
      val propertyKey = parsePropertyKey(name, pathPrefixes)
      logger.debug(s"--------------------processImage: ${propertyKey} | formats:$formats")

      // resolve textbox content from properties, otherwise only apply formats to current content
      resolvePropertyFromJson(propertyKey, json) map {
        _.headOption.map {
          case JsString(value) =>
            if (!value.isEmpty) {
              val refProperty = parsePropertyKey("referenzNummer", pathPrefixes)
              resolvePropertyFromJson(refProperty, json) flatMap {
                _.headOption.map {
                  case JsString(referenzNummer) =>
                    // create a new svg file with the svg qrcode content in the value
                    val fileSVG = File.createTempFile("qrcSVG", ".svg")
                    val pw = new PrintWriter(fileSVG)
                    pw.write(value)
                    pw.close

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

  def svg2png(svgFile: File, pngFile: File, referenzNummer: String) {
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
  private def processTextboxes(json: JsValue, cont: ParagraphContainer, locale: Locale, pathPrefixes: Seq[String]) = {
    for {
      p <- cont.getParagraphIterator
      t <- new NestedTextboxIterator(p.getFrameContainerElement)
    } {
      val propertyName = t.getName match {
        case propertyPattern(textboxName) => textboxName
        case fullName                     => fullName
      }
      val (name, formats) = parseFormats(propertyName)
      name match {
        case staticTextPattern(text) =>
          logger.debug(s"-----------------processTextbox with static value:$text | formats:$formats | name:${name}")
          applyFormats(json, t, formats, text, locale, pathPrefixes)
        case noFillTextPattern() => // do nothing
        case property => {
          val propertyKey = parsePropertyKey(property, pathPrefixes)
          logger.debug(s"-----------------processTextbox: ${propertyKey} | formats:$formats | name:$name")

          // resolve textbox content from properties, otherwise only apply formats to current content
          t.removeCommonStyle()
          resolvePropertyFromJson(propertyKey, json) map {
            case Vector(JsString(value)) if Try(dateFormatter.parseDateTime(value)).isSuccess =>
              //it is a date, convert to libreoffice compatible datetime format
              val convertedDate = dateFormatter.parseDateTime(value).toString(libreOfficeDateFormat)
              applyFormats(json, t, formats, convertedDate, locale, pathPrefixes)
            case Vector(JsString(value)) =>
              applyFormats(json, t, formats, value, locale, pathPrefixes)
            case Vector(JsNumber(value)) =>
              applyFormats(json, t, formats, value.toString, locale, pathPrefixes)
            case _ =>
              applyFormats(json, t, formats, "", locale, pathPrefixes)
          } getOrElse {
            if (!pathPrefixes.isEmpty) {
              applyFormats(json, t, formats, "", locale, pathPrefixes)
            }
          }
        }
      }
    }
  }

  @tailrec
  private def applyFormats(json: JsValue, textbox: Textbox, formats: Seq[String], value: String, locale: Locale, pathPrefixes: Seq[String]): String = {
    formats match {
      case Nil => applyFormat(json, textbox, "", value, locale, pathPrefixes)
      case s :: _ if (s.contains("orderBy")) => {
        logger.info(s"---------------------------------------------")
        applyFormat(json, textbox, "", value, locale, pathPrefixes)
      }
      case format :: tail =>
        val formattedValue = applyFormat(json, textbox, format, value, locale, pathPrefixes)
        applyFormats(json, textbox, tail, formattedValue, locale, pathPrefixes)
    }
  }

  private def correctPropertyName(name: String): String = {
    // replace all dot based accesses to arrays as from user fields bracets are not supported
    val adjustedName = name.replaceAll("""\.(\d+)""", "[$1]")
    adjustedName
  }

  private def parsePropertyKey(name: String, pathPrefixes: Seq[String] = Nil): String = {
    // replace all dot based accesses to arrays as from user fields bracets are not supported
    val adjustedName = name.replaceAll("""\.(\d+)""", "[$1]")

    findPathPrefixes(adjustedName, pathPrefixes).mkString(".")
  }

  @tailrec
  private def findPathPrefixes(name: String, pathPrefixes: Seq[String] = Nil): Seq[String] = {
    name match {
      case parentPathPattern(rest) if !pathPrefixes.isEmpty => findPathPrefixes(rest, pathPrefixes.dropRight(1))
      case absoluteJsonPathPattern(rest) => Seq(name)
      case _ => pathPrefixes :+ name
    }
  }

  /**
   *
   */
  private def applyFormat(json: JsValue, textbox: Textbox, format: String, value: String, locale: Locale, pathPrefixes: Seq[String]): String = {
    format match {
      case dateFormatPattern(pattern, _) if !value.isEmpty =>
        // parse date
        val formattedDate = libreOfficeDateFormat.parseDateTime(value).toString(pattern, locale)
        logger.debug(s"Formatted date with pattern $pattern => $formattedDate")
        formattedDate
      case backgroundColorFormatPattern(pattern, _) =>
        // set background to textbox
        val color = resolveColor(json, pattern, pathPrefixes)
        logger.error(s"setBackgroundColor:$color")
        textbox.setBackgroundColorWithNewStyle(color)
        value
      case foregroundColorFormatPattern(pattern, _) =>
        // set foreground to textbox (unfortunately resets font style to default)
        val color = resolveColor(json, pattern, pathPrefixes)
        textbox.setFontColor(color)
        value
      case numberFormatPattern(_, positiveColor, positivePattern, _, _, _, negativeColor, negativeFormat, _) if !value.isEmpty =>
        // lookup color value
        val number = value.toDouble
        if (number < 0 && negativeFormat != null) {
          val formattedValue = decimaleFormatForLocale(negativeFormat, locale).format(value.toDouble)
          if (negativeColor != null) {
            val color = resolveColor(json, negativeColor, pathPrefixes)
            logger.debug(s"Resolved native color:$color")
            textbox.setFontColor(color)
          } else {
            textbox.setFontColor(Some(Color.BLACK))
          }
          formattedValue
        } else {
          val formattedValue = decimaleFormatForLocale(positivePattern, locale).format(value.toDouble)
          if (positiveColor != null) {
            val color = resolveColor(json, positiveColor, pathPrefixes)
            logger.debug(s"Resolved positive color:$color")
            textbox.setFontColor(color)
          } else {
            textbox.setFontColor(Some(Color.BLACK))
          }
          formattedValue
        }
      case replaceFormatPattern(stringMap, _, _) =>
        val replaceMap = (stringMap split ("\\s*,\\s*") map (_ split ("\\s*->\\s*")) map { case Array(k, v) => k -> v }).toMap
        replaceMap find { case (k, v) => value.contains(k) } map { case (k, v) => value.replaceAll(k, v) } getOrElse (value)
      case x if format.length > 0 && !value.isEmpty =>
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
      return ("", Nil)
    }
    name.split('|').toList match {
      case name :: Nil  => (correctPropertyName(name.trim), Nil)
      case name :: tail => (correctPropertyName(name.trim), tail.map(_.trim))
      case _            => (correctPropertyName(name), Nil)
    }
  }

  private def resolveColor(json: JsValue, color: String, pathPrefixes: Seq[String]): Option[Color] = {
    color match {
      case resolvePropertyPattern(property) =>
        val name = correctPropertyName(property)
        //resolve color in props
        val propertyKey = parsePropertyKey(name, pathPrefixes)
        resolvePropertyFromJson(propertyKey, json) flatMap {
          case Vector(JsString(value)) =>
            resolveColor(json, value, pathPrefixes)
          case Vector() => None
          case x =>
            logger.warn(s"Unable to resolve color from property: $x")
            None
        }
      case color =>
        if (Color.isValid(color)) Some(Color.valueOf(color))
        else colorMap.get(color.toUpperCase).orElse {
          logger.debug(s"Unsupported color: $color")
          None
        }
    }
  }

  private def applyStructure(values: Vector[JsValue], structure: Seq[String], pathPrefixes: Seq[String] = Seq.empty[String]): Vector[(JsValue, Int)] = {
    // flatten value because query might be a list of JsValue or a single value from a JsArray proprty
    val flattenedValues: Vector[JsValue] = values.flatMap {
      case JsArray(elements) => elements
      case x                 => Vector(x)
    }
    val indexed = flattenedValues.zipWithIndex
    structure match {
      case first +: tail => {
        first match {
          case orderByPattern(orderProp, direction, _) => {
            val ordering = direction match {
              case ":DESC" => Ordering[String].reverse
              case _       => Ordering[String]
            }
            indexed.sortBy(e => extractProperties(e._1).get(orderProp).headOption.getOrElse(Value(JsNull, "")).value)(ordering)
          }
          case _ => indexed
        }
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
        values.zipWithIndex.map {
          case (v, index) => extractProperties(v, s"$childPrefix$index")
        }.flatten.toMap + (prefix -> Value(j, ""))
      case j @ JsNull => Map(prefix -> Value(j, ""))
      case j @ JsString(value) if Try(dateFormatter.parseDateTime(value)).isSuccess =>
        //it is a date, convert to libreoffice compatible datetime format
        val convertedDate = dateFormatter.parseDateTime(value).toString(libreOfficeDateFormat)
        Map(prefix -> Value(j, convertedDate))
      case j @ JsString(value) => Map(prefix -> Value(j, value))
      case value               => Map(prefix -> Value(value, value.toString))
    }
  }
}

class GenericParagraphContainerImpl(containerElement: OdfElement) extends AbstractParagraphContainer {
  def getParagraphContainerElement(): OdfElement = {
    containerElement
  }
}
