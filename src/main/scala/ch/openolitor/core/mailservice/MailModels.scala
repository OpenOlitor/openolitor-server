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
package ch.openolitor.core.mailservice

import ch.openolitor.core.domain.EventMetadata
import ch.openolitor.core.JSONSerializable
import org.joda.time.DateTime

case class MailPayload(subject: String, content: String) {
  def toMail(priority: Int, to: String, cc: Option[String], bcc: Option[String], replyTo: Option[String], attachmentReference: Option[String]): Mail =
    Mail(priority, to, cc, bcc, replyTo, subject, content, attachmentReference)
}

case class Mail(
  priority: Int,
  to: String,
  cc: Option[String],
  bcc: Option[String],
  replyTo: Option[String],
  subject: String,
  content: String,
  attachmentReference: Option[String]
)
  extends JSONSerializable

case class MailEnqueued(
  meta: EventMetadata,
  uid: String,
  mail: Mail,
  commandMeta: Option[AnyRef],
  nextTry: DateTime,
  expires: DateTime,
  retries: Int
)
  extends Ordered[MailEnqueued] {
  import scala.math.Ordered.orderingToOrdered
  implicit def dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isBefore _)

  def compare(that: MailEnqueued): Int = (this.mail.priority, this.nextTry) compare (that.mail.priority, that.nextTry)
}
