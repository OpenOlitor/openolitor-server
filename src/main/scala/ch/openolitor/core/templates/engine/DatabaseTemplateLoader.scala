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
package ch.openolitor.core.templates.engine

import de.zalando.beard.renderer._
import ch.openolitor.core.templates.repositories._
import ch.openolitor.core.templates.model._
import ch.openolitor.core.db.ConnectionPoolContextAware
import scalikejdbc._
import scala.io.Source
import ch.openolitor.core.SystemConfig

/**
 * TemplateLoadeer backed by a database. This loader will resolve the templates using the following two patterns:
 * <ol>
 * <li>Mail/<TemplateName>/<Subject|Body></li>
 * <li><TemplateName></li>
 * </ol>
 *
 * The first one will resolve templates from the MailTemplate entity by the TemplateName and either the Subject or the Body Part of it. The
 * second one refers to the generic Template entity use to get included and shared in other templates.
 */
class DatabaseTemplateLoader(readRepository: TemplateReadRepositorySync, override val sysConfig: SystemConfig) extends TemplateLoader with TemplateDBMappings with ConnectionPoolContextAware {

  val mailTemplateSubjectPattern = """Mail/(.*)/Subject""".r
  val mailTemplateBodyPattern = """Mail/(.*)/Body""".r

  override def load(templateName: TemplateName) = {
    DB readOnly { implicit session =>
      // load template
      (templateName.name match {
        case mailTemplateSubjectPattern(templateName) =>
          readRepository.getMailTemplateByName(templateName).map(_.subject)
        case mailTemplateBodyPattern(templateName) =>
          readRepository.getMailTemplateByName(templateName).map(_.body)
        case templateName =>
          readRepository.getSharedTemplateByName(templateName).map(_.template)
      }) map { templateString =>
        Source.fromString(templateString)
      }
    }
  }
}