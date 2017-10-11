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
package ch.openolitor.stammdaten.mailtemplates.eventsourcing

import stamina._
import stamina.json._
import ch.openolitor.stammdaten.mailtemplates.model._
import ch.openolitor.stammdaten.mailtemplates._
import ch.openolitor.core.domain.EntityStore._
import ch.openolitor.core.domain.EntityStoreJsonProtocol
import ch.openolitor.core.eventsourcing.CoreEventStoreSerializer
import java.util.Locale
import org.joda.time.DateTime
import org.joda.time.LocalDate
import spray.json.JsValue

trait MailTemplateEventStoreSerializer extends MailTemplateJsonProtocol with EntityStoreJsonProtocol with CoreEventStoreSerializer {
  //V1 persisters
  implicit val mailTemplateModifyPersister = persister[MailTemplateModify]("mail-template-modify")
  implicit val mailTemplateIdPersister = persister[MailTemplateId]("mail-template-id")
  implicit val mailTemplateUploadPersister = persister[MailTemplateUpload]("mail-template-upload")

  val mailTemplatePersisters = List(
    mailTemplateModifyPersister,
    mailTemplateIdPersister
  )
}
