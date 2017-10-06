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
import java.util.Locale

sealed trait MailTemplateType {
  val defaultMailTemplate: MailTemplatePayload
}
case object ProduzentenBestellungMailTemplateType extends MailTemplateType {
  val defaultMailTemplate: MailTemplatePayload = MailTemplatePayload(
    subject = """Bestellung {{ sammelbestellung.datum | date format="dd.MM.yyyy" }}""",
    body = """
          Bestellung von {{ projekt.bezeichnung }} an {{produzent.name}} {{ produzent.vorname }}:

          Lieferung: {{ sammelbestellung.datum | date format="dd.MM.yyyy" }}
          
          Bestellpositionen:
          {{for bestellung in bestellungen}}
          {{if bestellung.adminProzente > 0}}
          Adminprozente: {{ bestellung.adminProzente }}%:
          {{/if}}          
          {{for bestellposition in bestellung.bestellpositionen}}
          {{ bestellposition.produktBeschrieb }}: {{ bestellposition.anzahl }} x {{ bestellposition.menge }} {{bestellposition.einheit }} à {{ bestellposition.preisEinheit }}{{ bestellposition.detail }} = {{ bestellposition.preis }} {{ projekt.waehrung }} ⇒ {{ bestellposition.mengeTotal }} {{ bestellposition.einheit }}
          {{/for}}
          {{/for}}
          
          Summe [{{ projekt.waehrung }}]: {{ sammelbestellung.preisTotal | number format="#.00" }}"""
  )
}
case object InvitationMailTemplateType extends MailTemplateType {
  val defaultMailTemplate: MailTemplatePayload = MailTemplatePayload(
    subject = """OpenOlitor Zugang""",
    body = """
          {{ person.vorname }} {{person.name }},

	        {{ baseText }} {{ baseLink }}?token={{ einladung.uid }}
	        """
  )
}
case object CustomerMailTemplateType extends MailTemplateType {
  val defaultMailTemplate: MailTemplatePayload = MailTemplatePayload(
    subject = "",
    body = ""
  )
}
case object UnknownMailTemplateType extends MailTemplateType {
  val defaultMailTemplate: MailTemplatePayload = MailTemplatePayload(
    subject = "",
    body = ""
  )
}

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

case class MailTemplatePayload(subject: String, body: String)

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