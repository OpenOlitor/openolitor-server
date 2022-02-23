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
package ch.openolitor.arbeitseinsatz.eventsourcing

import ch.openolitor.arbeitseinsatz._
import ch.openolitor.arbeitseinsatz.ArbeitseinsatzCommandHandler.SendEmailToArbeitsangebotPersonenEvent
import ch.openolitor.arbeitseinsatz.models._
import ch.openolitor.core.JSONSerializable
import ch.openolitor.core.Macros.copyTo
import ch.openolitor.core.domain.EntityStoreJsonProtocol
import ch.openolitor.core.eventsourcing.CoreEventStoreSerializer
import ch.openolitor.mailtemplates.eventsourcing.MailTemplateEventStoreSerializer
import ch.openolitor.stammdaten.StammdatenJsonProtocol
import ch.openolitor.stammdaten.models.{ Person, PersonContactPermissionModify }
import spray.json.JsValue
import spray.json.lenses.JsonLenses._
import stamina.{ V1, V2, V3 }
import stamina.json._
import zangelo.spray.json.AutoProductFormats
import spray.json.lenses.JsonLenses._

trait ArbeitseinsatzEventStoreSerializer extends ArbeitseinsatzJsonProtocol with EntityStoreJsonProtocol with CoreEventStoreSerializer with AutoProductFormats[JSONSerializable] {
  val False = false
  // V1 persisters
  implicit val arbeitskategorieModifyPersister = persister[ArbeitskategorieModify]("arbeitskategorie-modify")
  implicit val arbeitskategorieIdPersister = persister[ArbeitskategorieId]("arbeitskategorie-id")

  val arbeitseinsatzModifyPersister = persister[ArbeitseinsatzModify]("arbeitseinsatz-modify")
  implicit val arbeitseinsatzModifyV2Persister = persister[ArbeitseinsatzModify, V2]("arbeitseinsatz-modify", from[V1]
    .to[V2](_.update('contactPermission ! set[Boolean](false))))
  implicit val arbeitseinsatzIdPersister = persister[ArbeitseinsatzId]("arbeitseinsatz-id")
  implicit val arbeitsangebotModifyPersister = persister[ArbeitsangebotModify]("arbeitsangebot-modify")
  implicit val arbeitsangebotIdPersister = persister[ArbeitsangebotId]("arbeitsangebot-id")

  implicit val arbeitsangeboteDuplicatePersister = persister[ArbeitsangeboteDuplicate]("arbeitsangebote-duplicate")
  implicit val arbeitsangeboteDuplicatPersister = persister[ArbeitsangebotDuplicate]("arbeitsangebot-duplicate")

  implicit val personContactPermissionModifyPersister = persister[PersonContactPermissionModify]("person-contact-permission-modify")

  val sendEmailToArbeitsangebotPersonenEventPersister = persister[SendEmailToArbeitsangebotPersonenEvent]("send-email-arbeitsangebot")

  val arbeitseinsatzPersisters = List(
    arbeitskategorieModifyPersister,
    arbeitskategorieIdPersister,
    arbeitseinsatzModifyV2Persister,
    arbeitseinsatzIdPersister,
    arbeitsangebotModifyPersister,
    arbeitsangebotIdPersister,
    arbeitsangeboteDuplicatePersister,
    arbeitsangeboteDuplicatPersister,
    sendEmailToArbeitsangebotPersonenEventPersister,
    personContactPermissionModifyPersister
  )
}
