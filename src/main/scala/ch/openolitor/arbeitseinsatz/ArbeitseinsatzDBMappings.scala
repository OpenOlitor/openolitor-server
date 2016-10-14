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
package ch.openolitor.arbeitseinsatz

import java.util.UUID
import ch.openolitor.core.models._
import ch.openolitor.core.models.VorlageTyp
import ch.openolitor.core.repositories.BaseRepository
import ch.openolitor.core.repositories.BaseRepository._
import ch.openolitor.stammdaten.models._
import scalikejdbc._
import scalikejdbc.TypeBinder._
import ch.openolitor.core.repositories.DBMappings
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.repositories.BaseEntitySQLSyntaxSupport
import ch.openolitor.core.scalax._
import ch.openolitor.stammdaten.StammdatenDBMappings
import ch.openolitor.stammdaten.models._
import ch.openolitor.arbeitseinsatz._
import ch.openolitor.arbeitseinsatz.models._

//DB Model bindig
trait ArbeitseinsatzDBMappings extends DBMappings with StammdatenDBMappings with LazyLogging {
  import TypeBinder._

  // DB type binders for read operations
  implicit val arbeitsangebotIdBinder: TypeBinder[ArbeitsangebotId] = baseIdTypeBinder(ArbeitsangebotId.apply _)
  implicit val arbeitseinsatzIdBinder: TypeBinder[ArbeitseinsatzId] = baseIdTypeBinder(ArbeitseinsatzId.apply _)

  implicit val optionArbeitsangebotIdBinder: TypeBinder[Option[ArbeitsangebotId]] = optionBaseIdTypeBinder(ArbeitsangebotId.apply _)

  implicit val arbeitseinsatzStatusTypeBinder: TypeBinder[ArbeitseinsatzStatus] = string.map(ArbeitseinsatzStatus.apply)

  //DB parameter binders for write and query operationsit
  implicit val arbeitseinsatzStatusBinder = toStringSqlBinder[ArbeitseinsatzStatus]

  implicit val arbeitsangebotIdSqlBinder = baseIdSqlBinder[ArbeitsangebotId]
  implicit val arbeiteinsatzIdSqlBinder = baseIdSqlBinder[ArbeitseinsatzId]

  implicit val arbeitsangebotIdOptionSqlBinder = optionSqlBinder[ArbeitsangebotId]

  implicit val arbeitsangebotMapping = new BaseEntitySQLSyntaxSupport[Arbeitsangebot] {
    override val tableName = "Arbeitsangebot"

    override lazy val columns = autoColumns[Arbeitsangebot]()

    def apply(rn: ResultName[Arbeitsangebot])(rs: WrappedResultSet): Arbeitsangebot = autoConstruct(rs, rn)

    def parameterMappings(entity: Arbeitsangebot): Seq[Any] =
      parameters(Arbeitsangebot.unapply(entity).get)

    override def updateParameters(aa: Arbeitsangebot) = {
      super.updateParameters(aa) ++ Seq(
        column.kopieVon -> parameter(aa.kopieVon),
        column.titel -> parameter(aa.titel),
        column.bezeichnung -> parameter(aa.bezeichnung),
        column.ort -> parameter(aa.ort),
        column.zeitVon -> parameter(aa.zeitVon),
        column.zeitBis -> parameter(aa.zeitBis),
        column.anzahlPersonen -> parameter(aa.anzahlPersonen),
        column.mehrPersonenOk -> parameter(aa.mehrPersonenOk),
        column.einsatzZeit -> parameter(aa.einsatzZeit),
        column.status -> parameter(aa.status)
      )
    }
  }

  implicit val arbeitseinsatzMapping = new BaseEntitySQLSyntaxSupport[Arbeitseinsatz] {
    override val tableName = "Arbeitseinsatz"

    override lazy val columns = autoColumns[Arbeitseinsatz]()

    def apply(rn: ResultName[Arbeitseinsatz])(rs: WrappedResultSet): Arbeitseinsatz = autoConstruct(rs, rn)

    def parameterMappings(entity: Arbeitseinsatz): Seq[Any] =
      parameters(Arbeitseinsatz.unapply(entity).get)

    override def updateParameters(ae: Arbeitseinsatz) = {
      super.updateParameters(ae) ++ Seq(
        column.kundeId -> parameter(ae.kundeId),
        column.kundeBezeichnung -> parameter(ae.kundeBezeichnung),
        column.aboId -> parameter(ae.aboId),
        column.aboBezeichnung -> parameter(ae.aboBezeichnung),
        column.anzahlPersonen -> parameter(ae.anzahlPersonen),
        column.bemerkungen -> parameter(ae.bemerkungen)
      )
    }
  }

}
