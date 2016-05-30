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
package ch.openolitor.core.repositories

import scalikejdbc._
import scalikejdbc.TypeBinder._
import ch.openolitor.core.models._
import com.typesafe.scalalogging.LazyLogging

trait CoreDBMappings extends DBMappings with LazyLogging {
  import ParameterBinderFactory._

  implicit val dbSchemaIdSqlBinder = baseIdParameterBinderFactory[DBSchemaId](DBSchemaId.apply)
  implicit val evolutionStatusBinder: Binders[EvolutionStatus] = Binders.string.xmap(EvolutionStatus.apply, { x =>
    logger.error(s"*************** Bind data:$x")
    x.productPrefix
  })

  implicit val dbSchemaMapping = new BaseEntitySQLSyntaxSupport[DBSchema] {
    override val tableName = "DBSchema"

    override lazy val columns = autoColumns[DBSchema]()

    def apply(rn: ResultName[DBSchema])(rs: WrappedResultSet): DBSchema =
      autoConstruct(rs, rn)

    def parameterMappings(entity: DBSchema): Seq[ParameterBinder] =
      parameters(DBSchema.unapply(entity).get)

    override def updateParameters(schema: DBSchema) = {
      super.updateParameters(schema) ++
        Seq(
          column.revision -> schema.revision,
          column.status -> schema.status
        )
    }
  }
}