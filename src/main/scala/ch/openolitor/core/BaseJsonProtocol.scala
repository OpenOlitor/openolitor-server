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

import ch.openolitor.core.domain.EventMetadata
import ch.openolitor.core.jobs.JobQueueService.JobId
import spray.json.DefaultJsonProtocol
import spray.json._
import org.joda.time._
import org.joda.time.format._
import ch.openolitor.core.models._
import ch.openolitor.core.reporting.AsyncReportServiceResult
import ch.openolitor.stammdaten.models.{ EmailSecondFactorType, OtpSecondFactorType, PersonContactPermissionModify, SecondFactorType }

import java.util.UUID
import java.util.Locale

trait JSONSerializable extends Product

/**
 * Basis JSON Formatter for spray-json serialisierung/deserialisierung
 */
trait BaseJsonProtocol extends DefaultJsonProtocol {
  val defaultConvert: Any => String = x => x.toString

  implicit val uuidFormat: JsonFormat[UUID] = new RootJsonFormat[UUID] {
    def write(obj: UUID): JsValue = JsString(obj.toString)

    def read(json: JsValue): UUID =
      json match {
        case (JsString(value)) => UUID.fromString(value)
        case value             => deserializationError(s"Unrecognized UUID format:$value")
      }
  }

  implicit val localeFormat: JsonFormat[Locale] = new RootJsonFormat[Locale] {
    def write(obj: Locale): JsValue = JsString(obj.toLanguageTag)

    def read(json: JsValue): Locale =
      json match {
        case (JsString(value)) => Locale.forLanguageTag(value)
        case value             => deserializationError(s"Unrecognized locale:$value")
      }
  }

  def enumFormat[E](implicit fromJson: String => E, toJson: E => String = defaultConvert): RootJsonFormat[E] = new RootJsonFormat[E] {
    def write(obj: E): JsValue = JsString(toJson(obj))

    def read(json: JsValue): E =
      json match {
        case (JsString(value)) => fromJson(value)
        case value             => deserializationError(s"Unrecognized enum format:$value")
      }
  }

  def baseIdFormat[I <: BaseId](implicit fromJson: Long => I): RootJsonFormat[I] = new RootJsonFormat[I] {
    def write(obj: I): JsValue = JsNumber(obj.id)

    def read(json: JsValue): I =
      json match {
        case (JsNumber(value)) => fromJson(value.toLong)
        case value             => deserializationError(s"Unrecognized baseId format:$value")
      }
  }

  /*
   * joda datetime format
   */
  implicit val dateTimeFormat: JsonFormat[DateTime] = new JsonFormat[DateTime] {

    val formatter = ISODateTimeFormat.dateTime

    def write(obj: DateTime): JsValue = {
      JsString(formatter.print(obj))
    }

    def read(json: JsValue): DateTime = json match {
      case JsString(s) =>
        try {
          formatter.parseDateTime(s)
        } catch {
          case t: Throwable => error(s)
        }
      case _ =>
        error(json.toString())
    }

    def error(v: Any): DateTime = {
      val example = formatter.print(0)
      deserializationError(f"'$v' is not a valid date value. Dates must be in compact ISO-8601 format, e.g. '$example'")
    }
  }

  implicit val optionDateTimeFormat: JsonFormat[Option[DateTime]] = new OptionFormat[DateTime]

  /*
   * joda LocalDate format
   */
  implicit val localDateFormat: JsonFormat[LocalDate] = new JsonFormat[LocalDate] {

    val formatter = ISODateTimeFormat.dateTime

    def write(obj: LocalDate): JsValue = {
      JsString(formatter.print(obj.toDateTimeAtStartOfDay))
    }

    def read(json: JsValue): LocalDate = json match {
      case JsString(s) =>
        try {
          formatter.parseDateTime(s).toLocalDate
        } catch {
          case t: Throwable => error(s)
        }
      case _ =>
        error(json.toString())
    }

    def error(v: Any): LocalDate = {
      val example = "yyyy-MM-dd"
      deserializationError(f"'$v' is not a valid date value. Dates must be in compact ISO-8601 format '$example'")
    }
  }

  implicit val optionLocalDateFormat: JsonFormat[Option[LocalDate]] = new OptionFormat[LocalDate]

  implicit val personIdFormat: RootJsonFormat[PersonId] = baseIdFormat(PersonId.apply)
  implicit val vorlageTypeFormat: RootJsonFormat[VorlageTyp] = enumFormat(VorlageTyp.apply)

  implicit val secondFactorType: JsonFormat[SecondFactorType] = new JsonFormat[SecondFactorType] {
    def write(obj: SecondFactorType): JsValue =
      obj match {
        case OtpSecondFactorType   => JsString("otp")
        case EmailSecondFactorType => JsString("email")
      }

    def read(json: JsValue): SecondFactorType =
      json match {
        case JsString("otp")   => OtpSecondFactorType
        case JsString("email") => EmailSecondFactorType
        case pe                => sys.error(s"Unknown secondfactor type:$pe")
      }
  }

  implicit val optionalSecondFactorType: JsonFormat[Option[SecondFactorType]] = new OptionFormat[SecondFactorType]

  implicit val idResponseFormat: RootJsonFormat[BaseJsonProtocol.IdResponse] = jsonFormat1(BaseJsonProtocol.IdResponse)

  implicit val rejectionMessageFormat: RootJsonFormat[RejectionMessage] = jsonFormat2(RejectionMessage)
  implicit val personContactPermissionModifyFormat: RootJsonFormat[PersonContactPermissionModify] = jsonFormat1(PersonContactPermissionModify)

  implicit val jobIdFormat: RootJsonFormat[JobId] = jsonFormat3(JobId)
  implicit val asyncReportServiceResultFormat: RootJsonFormat[AsyncReportServiceResult] = jsonFormat2(AsyncReportServiceResult)

  // event formats
  implicit val eventMetadataFormat: RootJsonFormat[EventMetadata] = jsonFormat6(EventMetadata)
}

object BaseJsonProtocol {
  case class IdResponse(id: Long)

  case class RejectionMessage(message: String, cause: String)
}
