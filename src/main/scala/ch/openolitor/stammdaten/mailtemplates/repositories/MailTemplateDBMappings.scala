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
package ch.openolitor.stammdaten.mailtemplates.repositories

import ch.openolitor.core.repositories.DBMappings
import ch.openolitor.stammdaten.mailtemplates.model._
import scalikejdbc._
import ch.openolitor.core.repositories._

trait MailTemplateDBMappings extends DBMappings {
  implicit val mailTemplateIdBinder: Binders[MailTemplateId] = baseIdBinders(MailTemplateId.apply _)
  implicit val mailTemplateTypeBinder: Binders[MailTemplateType] = toStringBinder(MailTemplateType.apply)

  implicit val mailTemplateMapping = new BaseEntitySQLSyntaxSupport[MailTemplate] {
    override val tableName = "MailTemplate"

    override lazy val columns = autoColumns[MailTemplate]()

    def apply(rn: ResultName[MailTemplate])(rs: WrappedResultSet): MailTemplate =
      autoConstruct(rs, rn)

    def parameterMappings(entity: MailTemplate): Seq[ParameterBinder] =
      parameters(MailTemplate.unapply(entity).get)

    override def updateParameters(entity: MailTemplate) = {
      super.updateParameters(entity) ++
        Seq(
          column.templateType -> entity.templateType,
          column.templateName -> entity.templateName,
          column.description -> entity.description,
          column.subject -> entity.subject,
          column.body -> entity.body
        )
    }
  }
}
