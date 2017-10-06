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
package ch.openolitor.core.templates.model

import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.models._
import org.joda.time.DateTime

sealed trait MailTemplateType
case object ProduzentenBestellungMailTemplateType extends MailTemplateType
case object CustomerMailTemplateType extends MailTemplateType
case object UnknownMailTemplateType extends MailTemplateType

object MailTemplateType extends LazyLogging {
  val AllTemplateTypes = List(
    ProduzentenBestellungMailTemplateType,
    CustomerMailTemplateType
  )

  def apply(value: String): MailTemplateType = {
    logger.debug(s"MailTemplateType.apply:$value")
    AllTemplateTypes.find(_.toString.toLowerCase == value.toLowerCase).getOrElse(UnknownMailTemplateType)
  }
}

case class MailTemplateId(id: Long) extends BaseId
case class MailTemplate(
  id: MailTemplateId,
  templateType: MailTemplateType,
  templateName: String,
  description: Option[String],
  subject: String,
  body: String,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[MailTemplateId]

case class SharedTemplateId(id: Long) extends BaseId
case class SharedTemplate(
  id: SharedTemplateId,
  templateName: String,
  description: Option[String],
  template: String,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[SharedTemplateId]