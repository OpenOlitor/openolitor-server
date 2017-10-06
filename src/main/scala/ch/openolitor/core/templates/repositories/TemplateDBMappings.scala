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
package ch.openolitor.core.templates.repositories

import ch.openolitor.core.repositories.DBMappings
import ch.openolitor.core.templates.model._
import scalikejdbc._
import scalikejdbc.TypeBinder._
import ch.openolitor.core.repositories._

trait TemplateDBMappings extends DBMappings {
  implicit val mailTemplateType: TypeBinder[MailTemplateType] = string.map(MailTemplateType.apply)
  implicit val mailTemplateId: TypeBinder[MailTemplateId] = baseIdTypeBinder(MailTemplateId.apply)
  implicit val sharedTemplateId: TypeBinder[SharedTemplateId] = baseIdTypeBinder(SharedTemplateId.apply)

  implicit val mailTemplateTypeSqlBinder = toStringSqlBinder[MailTemplateType]
  implicit val mailTemplateIdSqlBinder = baseIdSqlBinder[MailTemplateId]
  implicit val sharedTemplateIdSqlBinder = baseIdSqlBinder[SharedTemplateId]

  implicit val mailTemplateMapping = new BaseEntitySQLSyntaxSupport[MailTemplate] {
    override val tableName = "MailTemplate"

    override lazy val columns = autoColumns[MailTemplate]()

    def apply(rn: ResultName[MailTemplate])(rs: WrappedResultSet): MailTemplate =
      autoConstruct(rs, rn)

    def parameterMappings(entity: MailTemplate): Seq[Any] =
      parameters(MailTemplate.unapply(entity).get)

    override def updateParameters(entity: MailTemplate) = {
      super.updateParameters(entity) ++
        Seq(
          column.templateType -> parameter(entity.templateType),
          column.templateName -> parameter(entity.templateName),
          column.description -> parameter(entity.description),
          column.subject -> parameter(entity.subject),
          column.body -> parameter(entity.body)
        )
    }
  }

  implicit val sharedTemplateMapping = new BaseEntitySQLSyntaxSupport[SharedTemplate] {
    override val tableName = "SharedTemplate"

    override lazy val columns = autoColumns[SharedTemplate]()

    def apply(rn: ResultName[SharedTemplate])(rs: WrappedResultSet): SharedTemplate =
      autoConstruct(rs, rn)

    def parameterMappings(entity: SharedTemplate): Seq[Any] =
      parameters(SharedTemplate.unapply(entity).get)

    override def updateParameters(entity: SharedTemplate) = {
      super.updateParameters(entity) ++
        Seq(
          column.templateName -> parameter(entity.templateName),
          column.description -> parameter(entity.description),
          column.template -> parameter(entity.template)
        )
    }
  }
}