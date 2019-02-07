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

import ch.openolitor.arbeitseinsatz.models._
import ch.openolitor.core.repositories.{ BaseEntitySQLSyntaxSupport, BaseParameter, DBMappings }
import ch.openolitor.stammdaten.StammdatenDBMappings
import com.typesafe.scalalogging.LazyLogging
import scalikejdbc._

//DB Model bindig
trait ArbeitseinsatzDBMappings extends DBMappings with StammdatenDBMappings with BaseParameter with LazyLogging {

  // DB type binders for read operations
  implicit val arbeitskategorieIdBinder: Binders[ArbeitskategorieId] = baseIdBinders(ArbeitskategorieId.apply _)
  implicit val arbeitskategorieBezBinder: Binders[ArbeitskategorieBez] = baseStringIdBinders(ArbeitskategorieBez.apply)
  implicit val arbeitsangebotIdBinder: Binders[ArbeitsangebotId] = baseIdBinders(ArbeitsangebotId.apply _)
  implicit val arbeitseinsatzIdBinder: Binders[ArbeitseinsatzId] = baseIdBinders(ArbeitseinsatzId.apply _)

  implicit val optionArbeitsangebotIdBinder: Binders[Option[ArbeitsangebotId]] = optionBaseIdBinders(ArbeitsangebotId.apply _)
  implicit val arbeitskategorieBezSeqBinder: Binders[Set[ArbeitskategorieBez]] = setSqlBinder(ArbeitskategorieBez.apply, _.id)

  implicit val arbeitseinsatzStatusBinders: Binders[ArbeitseinsatzStatus] = toStringBinder(ArbeitseinsatzStatus.apply)

  // declare parameterbinderfactories for enum type to allow dynamic type convertion of enum subtypes
  implicit def arbeitseinsatzStatusParameterBinderFactory[A <: ArbeitseinsatzStatus]: ParameterBinderFactory[A] = ParameterBinderFactory.stringParameterBinderFactory.contramap(_.toString)

  implicit val arbeitskategorieMapping = new BaseEntitySQLSyntaxSupport[Arbeitskategorie] {
    override val tableName = "Arbeitskategorie"

    override lazy val columns = autoColumns[Arbeitskategorie]()

    def apply(rn: ResultName[Arbeitskategorie])(rs: WrappedResultSet): Arbeitskategorie = autoConstruct(rs, rn)

    def parameterMappings(entity: Arbeitskategorie): Seq[ParameterBinder] =
      parameters(Arbeitskategorie.unapply(entity).get)

    override def updateParameters(ak: Arbeitskategorie) = {
      super.updateParameters(ak) ++ Seq(
        column.beschreibung -> ak.beschreibung
      )
    }
  }

  implicit val arbeitsangebotMapping = new BaseEntitySQLSyntaxSupport[Arbeitsangebot] {
    override val tableName = "Arbeitsangebot"

    override lazy val columns = autoColumns[Arbeitsangebot]()

    def apply(rn: ResultName[Arbeitsangebot])(rs: WrappedResultSet): Arbeitsangebot = autoConstruct(rs, rn)

    def parameterMappings(entity: Arbeitsangebot): Seq[ParameterBinder] =
      parameters(Arbeitsangebot.unapply(entity).get)

    override def updateParameters(aa: Arbeitsangebot) = {
      super.updateParameters(aa) ++ Seq(
        column.kopieVon -> aa.kopieVon,
        column.titel -> aa.titel,
        column.bezeichnung -> aa.bezeichnung,
        column.ort -> aa.ort,
        column.zeitVon -> aa.zeitVon,
        column.zeitBis -> aa.zeitBis,
        column.arbeitskategorien -> aa.arbeitskategorien,
        column.anzahlEingeschriebene -> aa.anzahlEingeschriebene,
        column.anzahlPersonen -> aa.anzahlPersonen,
        column.mehrPersonenOk -> aa.mehrPersonenOk,
        column.einsatzZeit -> aa.einsatzZeit,
        column.status -> aa.status
      )
    }
  }

  implicit val arbeitseinsatzMapping = new BaseEntitySQLSyntaxSupport[Arbeitseinsatz] {
    override val tableName = "Arbeitseinsatz"

    override lazy val columns = autoColumns[Arbeitseinsatz]()

    def apply(rn: ResultName[Arbeitseinsatz])(rs: WrappedResultSet): Arbeitseinsatz = autoConstruct(rs, rn)

    def parameterMappings(entity: Arbeitseinsatz): Seq[ParameterBinder] =
      parameters(Arbeitseinsatz.unapply(entity).get)

    override def updateParameters(ae: Arbeitseinsatz) = {
      super.updateParameters(ae) ++ Seq(
        column.arbeitsangebotId -> ae.arbeitsangebotId,
        column.arbeitsangebotTitel -> ae.arbeitsangebotTitel,
        column.arbeitsangebotStatus -> ae.arbeitsangebotStatus,
        column.zeitVon -> ae.zeitVon,
        column.zeitBis -> ae.zeitBis,
        column.einsatzZeit -> ae.einsatzZeit,
        column.kundeId -> ae.kundeId,
        column.kundeBezeichnung -> ae.kundeBezeichnung,
        column.personId -> ae.personId,
        column.personName -> ae.personName,
        column.aboId -> ae.aboId,
        column.aboBezeichnung -> ae.aboBezeichnung,
        column.anzahlPersonen -> ae.anzahlPersonen,
        column.bemerkungen -> ae.bemerkungen,
        column.email -> ae.email,
        column.telefonMobil -> ae.telefonMobil
      )
    }
  }

}
