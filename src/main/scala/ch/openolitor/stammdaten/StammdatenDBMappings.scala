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
package ch.openolitor.stammdaten

import java.util.UUID

import ch.openolitor.core.models._
import ch.openolitor.core.models.VorlageTyp
import ch.openolitor.core.repositories.ParameterBinderMapping
import ch.openolitor.stammdaten.models._
import scalikejdbc._
import scalikejdbc.TypeBinder._
import ch.openolitor.core.repositories.DBMappings
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.repositories.SqlBinder
import ch.openolitor.stammdaten.models.PendenzStatus
import ch.openolitor.core.repositories.BaseEntitySQLSyntaxSupport
import ch.openolitor.core.scalax._
import ch.openolitor.core.Macros._
import scala.collection.immutable.TreeMap
import ch.openolitor.core.filestore.VorlageRechnung

//DB Model bindig
trait StammdatenDBMappings extends DBMappings with LazyLogging {
  import TypeBinder._

  val fristeinheitPattern = """(\d+)(M|W)""".r

  // DB type binders for read operations
  implicit val tourIdBinder: TypeBinder[TourId] = baseIdTypeBinder(TourId.apply _)
  implicit val depotIdBinder: TypeBinder[DepotId] = baseIdTypeBinder(DepotId.apply _)
  implicit val aboTypIdBinder: TypeBinder[AbotypId] = baseIdTypeBinder(AbotypId.apply _)
  implicit val vertriebIdBinder: TypeBinder[VertriebId] = baseIdTypeBinder(VertriebId.apply _)
  implicit val vertriebsartIdBinder: TypeBinder[VertriebsartId] = baseIdTypeBinder(VertriebsartId.apply _)
  implicit val vertriebsartIdSetBinder: TypeBinder[Set[VertriebsartId]] = string.map(s => s.split(",").map(_.toLong).map(VertriebsartId.apply _).toSet)
  implicit val vertriebsartIdSeqBinder: TypeBinder[Seq[VertriebsartId]] = string.map(s => if (s != null) s.split(",").map(_.toLong).map(VertriebsartId.apply _).toSeq else Nil)
  implicit val kundeIdBinder: TypeBinder[KundeId] = baseIdTypeBinder(KundeId.apply _)
  implicit val pendenzIdBinder: TypeBinder[PendenzId] = baseIdTypeBinder(PendenzId.apply _)
  implicit val aboIdBinder: TypeBinder[AboId] = baseIdTypeBinder(AboId.apply _)
  implicit val lierferungIdBinder: TypeBinder[LieferungId] = baseIdTypeBinder(LieferungId.apply _)
  implicit val lieferplanungIdBinder: TypeBinder[LieferplanungId] = baseIdTypeBinder(LieferplanungId.apply _)
  implicit val optionLieferplanungIdBinder: TypeBinder[Option[LieferplanungId]] = optionBaseIdTypeBinder(LieferplanungId.apply _)
  implicit val lieferpositionIdBinder: TypeBinder[LieferpositionId] = baseIdTypeBinder(LieferpositionId.apply _)
  implicit val bestellungIdBinder: TypeBinder[BestellungId] = baseIdTypeBinder(BestellungId.apply _)
  implicit val sammelbestellungIdBinder: TypeBinder[SammelbestellungId] = baseIdTypeBinder(SammelbestellungId.apply _)
  implicit val bestellpositionIdBinder: TypeBinder[BestellpositionId] = baseIdTypeBinder(BestellpositionId.apply _)
  implicit val customKundentypIdBinder: TypeBinder[CustomKundentypId] = baseIdTypeBinder(CustomKundentypId.apply _)
  implicit val kundentypIdBinder: TypeBinder[KundentypId] = string.map(KundentypId)
  implicit val produktekategorieIdBinder: TypeBinder[ProduktekategorieId] = baseIdTypeBinder(ProduktekategorieId.apply _)
  implicit val baseProduktekategorieIdBinder: TypeBinder[BaseProduktekategorieId] = string.map(BaseProduktekategorieId)
  implicit val produktIdBinder: TypeBinder[ProduktId] = baseIdTypeBinder(ProduktId.apply _)
  implicit val produzentIdBinder: TypeBinder[ProduzentId] = baseIdTypeBinder(ProduzentId.apply _)
  implicit val baseProduzentIdBinder: TypeBinder[BaseProduzentId] = string.map(BaseProduzentId)
  implicit val projektIdBinder: TypeBinder[ProjektId] = baseIdTypeBinder(ProjektId.apply _)
  implicit val produktProduzentIdBinder: TypeBinder[ProduktProduzentId] = baseIdTypeBinder(ProduktProduzentId.apply _)
  implicit val produktProduktekategorieIdBinder: TypeBinder[ProduktProduktekategorieId] = baseIdTypeBinder(ProduktProduktekategorieId.apply _)
  implicit val abwesenheitIdBinder: TypeBinder[AbwesenheitId] = baseIdTypeBinder(AbwesenheitId.apply _)
  implicit val korbIdBinder: TypeBinder[KorbId] = baseIdTypeBinder(KorbId.apply _)
  implicit val auslieferungIdBinder: TypeBinder[AuslieferungId] = baseIdTypeBinder(AuslieferungId.apply _)
  implicit val projektVorlageIdBinder: TypeBinder[ProjektVorlageId] = baseIdTypeBinder(ProjektVorlageId.apply _)
  implicit val optionAuslieferungIdBinder: TypeBinder[Option[AuslieferungId]] = optionBaseIdTypeBinder(AuslieferungId.apply _)
  implicit val einladungIdBinder: TypeBinder[EinladungId] = baseIdTypeBinder(EinladungId.apply _)
  implicit val kontoDatenIdBinder: TypeBinder[KontoDatenId] = baseIdTypeBinder(KontoDatenId.apply _)

  implicit val pendenzStatusTypeBinder: TypeBinder[PendenzStatus] = string.map(PendenzStatus.apply)
  implicit val rhythmusTypeBinder: TypeBinder[Rhythmus] = string.map(Rhythmus.apply)
  implicit val waehrungTypeBinder: TypeBinder[Waehrung] = string.map(Waehrung.apply)
  implicit val lieferungStatusTypeBinder: TypeBinder[LieferungStatus] = string.map(LieferungStatus.apply)
  implicit val korbStatusTypeBinder: TypeBinder[KorbStatus] = string.map(KorbStatus.apply)
  implicit val auslieferungStatusTypeBinder: TypeBinder[AuslieferungStatus] = string.map(AuslieferungStatus.apply)
  implicit val preiseinheitTypeBinder: TypeBinder[Preiseinheit] = string.map(Preiseinheit.apply)
  implicit val lieferzeitpunktTypeBinder: TypeBinder[Lieferzeitpunkt] = string.map(Lieferzeitpunkt.apply)
  implicit val lieferzeitpunktSetTypeBinder: TypeBinder[Set[Lieferzeitpunkt]] = string.map(s => s.split(",").map(Lieferzeitpunkt.apply).toSet)
  implicit val kundenTypIdSetBinder: TypeBinder[Set[KundentypId]] = string.map(s => s.split(",").map(KundentypId.apply).toSet)
  implicit val laufzeiteinheitTypeBinder: TypeBinder[Laufzeiteinheit] = string.map(Laufzeiteinheit.apply)
  implicit val liefereinheitypeBinder: TypeBinder[Liefereinheit] = string.map(Liefereinheit.apply)
  implicit val liefersaisonTypeBinder: TypeBinder[Liefersaison] = string.map(Liefersaison.apply)
  implicit val vorlageTypeTypeBinder: TypeBinder[VorlageTyp] = string.map(VorlageTyp.apply)
  implicit val anredeTypeBinder: TypeBinder[Option[Anrede]] = string.map(Anrede.apply)
  implicit val fristOptionTypeBinder: TypeBinder[Option[Frist]] = string.map {
    case fristeinheitPattern(wert, "W") => Some(Frist(wert.toInt, Wochenfrist))
    case fristeinheitPattern(wert, "M") => Some(Frist(wert.toInt, Monatsfrist))
    case _ => None
  }

  implicit val baseProduktekategorieIdSetTypeBinder: TypeBinder[Set[BaseProduktekategorieId]] = string.map(s => s.split(",").map(BaseProduktekategorieId.apply).toSet)
  implicit val baseProduzentIdSetTypeBinder: TypeBinder[Set[BaseProduzentId]] = string.map(s => s.split(",").map(BaseProduzentId.apply).toSet)
  implicit val stringIntTreeMapTypeBinder: TypeBinder[TreeMap[String, Int]] = treeMapTypeBinder(identity, _.toInt)
  implicit val stringBigDecimalTreeMapTypeBinder: TypeBinder[TreeMap[String, BigDecimal]] = treeMapTypeBinder(identity, BigDecimal(_))
  implicit val rolleMapTypeBinder: TypeBinder[Map[Rolle, Boolean]] = mapTypeBinder(r => Rolle(r).getOrElse(KundenZugang), _.toBoolean)
  implicit val rolleTypeBinder: TypeBinder[Option[Rolle]] = string.map(Rolle.apply)

  implicit val stringSeqTypeBinder: TypeBinder[Seq[String]] = string.map(s => if (s != null) s.split(",").toSeq else Nil)
  implicit val stringSetTypeBinder: TypeBinder[Set[String]] = string.map(s => if (s != null) s.split(",").toSet else Set())

  //DB parameter binders for write and query operationsit
  implicit val pendenzStatusBinder = toStringSqlBinder[PendenzStatus]
  implicit val rhytmusSqlBinder = toStringSqlBinder[Rhythmus]
  implicit val preiseinheitSqlBinder = toStringSqlBinder[Preiseinheit]
  implicit val waehrungSqlBinder = toStringSqlBinder[Waehrung]
  implicit val lieferungStatusSqlBinder = toStringSqlBinder[LieferungStatus]
  implicit val korbStatusSqlBinder = toStringSqlBinder[KorbStatus]
  implicit val auslieferungStatusSqlBinder = toStringSqlBinder[AuslieferungStatus]
  implicit val lieferzeitpunktSqlBinder = toStringSqlBinder[Lieferzeitpunkt]
  implicit val lieferzeitpunktSetSqlBinder = setSqlBinder[Lieferzeitpunkt]
  implicit val laufzeiteinheitSqlBinder = toStringSqlBinder[Laufzeiteinheit]
  implicit val liefereinheitSqlBinder = toStringSqlBinder[Liefereinheit]
  implicit val liefersaisonSqlBinder = toStringSqlBinder[Liefersaison]
  implicit val anredeSqlBinder = toStringSqlBinder[Anrede]
  implicit val optionAnredeSqlBinder = optionSqlBinder[Anrede]
  implicit val vorlageTypeSqlBinder = toStringSqlBinder[VorlageTyp]

  implicit val abotypIdSqlBinder = baseIdSqlBinder[AbotypId]
  implicit val depotIdSqlBinder = baseIdSqlBinder[DepotId]
  implicit val tourIdSqlBinder = baseIdSqlBinder[TourId]
  implicit val vertriebIdSqlBinder = baseIdSqlBinder[VertriebId]
  implicit val vertriebsartIdSqlBinder = baseIdSqlBinder[VertriebsartId]
  implicit val vertriebsartIdSetSqlBinder = setSqlBinder[VertriebsartId]
  implicit val vertriebsartIdSeqSqlBinder = seqSqlBinder[VertriebsartId]
  implicit val kundeIdSqlBinder = baseIdSqlBinder[KundeId]
  implicit val pendenzIdSqlBinder = baseIdSqlBinder[PendenzId]
  implicit val customKundentypIdSqlBinder = baseIdSqlBinder[CustomKundentypId]
  implicit val kundentypIdSqlBinder = new SqlBinder[KundentypId] { def apply(value: KundentypId): Any = value.id }
  implicit val kundentypIdSetSqlBinder = setSqlBinder[KundentypId]
  implicit val aboIdSqlBinder = baseIdSqlBinder[AboId]
  implicit val lieferungIdSqlBinder = baseIdSqlBinder[LieferungId]
  implicit val lieferplanungIdSqlBinder = baseIdSqlBinder[LieferplanungId]
  implicit val lieferpositionIdSqlBinder = baseIdSqlBinder[LieferpositionId]
  implicit val bestellungIdSqlBinder = baseIdSqlBinder[BestellungId]
  implicit val sammelbestellungIdSqlBinder = baseIdSqlBinder[SammelbestellungId]
  implicit val bestellpositionIdSqlBinder = baseIdSqlBinder[BestellpositionId]
  implicit val korbIdSqlBinder = baseIdSqlBinder[KorbId]
  implicit val auslieferungIdSqlBinder = baseIdSqlBinder[AuslieferungId]
  implicit val projektVorlageIdSqlBinder = baseIdSqlBinder[ProjektVorlageId]
  implicit val auslieferungIdOptionSqlBinder = optionSqlBinder[AuslieferungId]
  implicit val produktIdSqlBinder = baseIdSqlBinder[ProduktId]
  implicit val produktIdOptionBinder = optionSqlBinder[ProduktId]
  implicit val produktekategorieIdSqlBinder = baseIdSqlBinder[ProduktekategorieId]
  implicit val baseProduktekategorieIdSqlBinder = new SqlBinder[BaseProduktekategorieId] { def apply(value: BaseProduktekategorieId): Any = value.id }
  implicit val baseProduktekategorieIdSetSqlBinder = setSqlBinder[BaseProduktekategorieId]
  implicit val produzentIdSqlBinder = baseIdSqlBinder[ProduzentId]
  implicit val baseProduzentIdSqlBinder = new SqlBinder[BaseProduzentId] { def apply(value: BaseProduzentId): Any = value.id }
  implicit val baseProduzentIdSetSqlBinder = setSqlBinder[BaseProduzentId]
  implicit val projektIdSqlBinder = baseIdSqlBinder[ProjektId]
  implicit val abwesenheitIdSqlBinder = baseIdSqlBinder[AbwesenheitId]
  implicit val produktProduzentIdIdSqlBinder = baseIdSqlBinder[ProduktProduzentId]
  implicit val produktProduktekategorieIdIdSqlBinder = baseIdSqlBinder[ProduktProduktekategorieId]
  implicit val lieferplanungIdOptionBinder = optionSqlBinder[LieferplanungId]
  implicit val einladungIdSqlBinder = baseIdSqlBinder[EinladungId]
  implicit val kontoDatenIdSqlBinder = baseIdSqlBinder[KontoDatenId]

  implicit val stringIntTreeMapSqlBinder = treeMapSqlBinder[String, Int]
  implicit val stringBigDecimalTreeMapSqlBinder = treeMapSqlBinder[String, BigDecimal]
  implicit val rolleSqlBinder = toStringSqlBinder[Rolle]
  implicit val optionRolleSqlBinder = optionSqlBinder[Rolle]
  implicit val rolleMapSqlBinder = mapSqlBinder[Rolle, Boolean]
  implicit val fristeSqlBinder = new SqlBinder[Frist] {
    def apply(frist: Frist): Any = {
      val einheit = frist.einheit match {
        case Wochenfrist => "W"
        case Monatsfrist => "M"
      }
      s"${frist.wert}$einheit"
    }
  }
  implicit val fristOptionSqlBinder = optionSqlBinder[Frist]

  implicit val stringSeqSqlBinder = seqSqlBinder[String]
  implicit val stringSetSqlBinder = setSqlBinder[String]

  implicit val abotypMapping = new BaseEntitySQLSyntaxSupport[Abotyp] {
    override val tableName = "Abotyp"

    override lazy val columns = autoColumns[Abotyp]()

    def apply(rn: ResultName[Abotyp])(rs: WrappedResultSet): Abotyp = autoConstruct(rs, rn)

    def parameterMappings(entity: Abotyp): Seq[Any] =
      parameters(Abotyp.unapply(entity).get)

    override def updateParameters(entity: Abotyp): Seq[Tuple2[SQLSyntax, Any]] = autoUpdateParams(entity)
  }

  implicit val customKundentypMapping = new BaseEntitySQLSyntaxSupport[CustomKundentyp] {
    override val tableName = "Kundentyp"

    override lazy val columns = autoColumns[CustomKundentyp]()

    def apply(rn: ResultName[CustomKundentyp])(rs: WrappedResultSet): CustomKundentyp =
      autoConstruct(rs, rn)

    def parameterMappings(entity: CustomKundentyp): Seq[Any] =
      parameters(CustomKundentyp.unapply(entity).get)

    override def updateParameters(entity: CustomKundentyp): Seq[Tuple2[SQLSyntax, Any]] = autoUpdateParams(entity)
  }

  implicit val kundeMapping = new BaseEntitySQLSyntaxSupport[Kunde] {
    override val tableName = "Kunde"

    override lazy val columns = autoColumns[Kunde]()

    def apply(rn: ResultName[Kunde])(rs: WrappedResultSet): Kunde =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Kunde): Seq[Any] =
      parameters(Kunde.unapply(entity).get)

    override def updateParameters(entity: Kunde): Seq[Tuple2[SQLSyntax, Any]] = autoUpdateParams(entity)
  }

  implicit val personMapping = new BaseEntitySQLSyntaxSupport[Person] {
    override val tableName = "Person"

    override lazy val columns = autoColumns[Person]()

    def apply(rn: ResultName[Person])(rs: WrappedResultSet): Person =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Person): Seq[Any] =
      parameters(Person.unapply(entity).get)

    override def updateParameters(entity: Person): Seq[Tuple2[SQLSyntax, Any]] = autoUpdateParams(entity)
  }

  implicit val pendenzMapping = new BaseEntitySQLSyntaxSupport[Pendenz] {
    override val tableName = "Pendenz"

    override lazy val columns = autoColumns[Pendenz]()

    def apply(rn: ResultName[Pendenz])(rs: WrappedResultSet): Pendenz =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Pendenz): Seq[Any] =
      parameters(Pendenz.unapply(entity).get)

    override def updateParameters(entity: Pendenz): Seq[Tuple2[SQLSyntax, Any]] = autoUpdateParams(entity)
  }

  implicit val lieferungMapping = new BaseEntitySQLSyntaxSupport[Lieferung] {
    override val tableName = "Lieferung"

    override lazy val columns = autoColumns[Lieferung]()

    def apply(rn: ResultName[Lieferung])(rs: WrappedResultSet): Lieferung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Lieferung): Seq[Any] =
      parameters(Lieferung.unapply(entity).get)

    override def updateParameters(entity: Lieferung): Seq[Tuple2[SQLSyntax, Any]] = autoUpdateParams(entity)
  }

  implicit val lieferplanungMapping = new BaseEntitySQLSyntaxSupport[Lieferplanung] {
    override val tableName = "Lieferplanung"

    override lazy val columns = autoColumns[Lieferplanung]()

    def apply(rn: ResultName[Lieferplanung])(rs: WrappedResultSet): Lieferplanung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Lieferplanung): Seq[Any] = parameters(Lieferplanung.unapply(entity).get)

    override def updateParameters(entity: Lieferplanung): Seq[Tuple2[SQLSyntax, Any]] = autoUpdateParams(entity)
  }

  implicit val lieferpositionMapping = new BaseEntitySQLSyntaxSupport[Lieferposition] {
    override val tableName = "Lieferposition"

    override lazy val columns = autoColumns[Lieferposition]()

    def apply(rn: ResultName[Lieferposition])(rs: WrappedResultSet): Lieferposition =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Lieferposition): Seq[Any] = parameters(Lieferposition.unapply(entity).get)

    override def updateParameters(entity: Lieferposition): Seq[Tuple2[SQLSyntax, Any]] = autoUpdateParams(entity)
  }

  implicit val sammelbestellungMapping = new BaseEntitySQLSyntaxSupport[Sammelbestellung] {
    override val tableName = "Sammelbestellung"

    override lazy val columns = autoColumns[Sammelbestellung]()

    def apply(rn: ResultName[Sammelbestellung])(rs: WrappedResultSet): Sammelbestellung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Sammelbestellung): Seq[Any] = parameters(Sammelbestellung.unapply(entity).get)

    override def updateParameters(entity: Sammelbestellung): Seq[Tuple2[SQLSyntax, Any]] = autoUpdateParams(entity)
  }

  implicit val bestellungMapping = new BaseEntitySQLSyntaxSupport[Bestellung] {
    override val tableName = "Bestellung"

    override lazy val columns = autoColumns[Bestellung]()

    def apply(rn: ResultName[Bestellung])(rs: WrappedResultSet): Bestellung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Bestellung): Seq[Any] = parameters(Bestellung.unapply(entity).get)

    override def updateParameters(entity: Bestellung): Seq[Tuple2[SQLSyntax, Any]] = autoUpdateParams(entity)
  }

  implicit val bestellpositionMapping = new BaseEntitySQLSyntaxSupport[Bestellposition] {
    override val tableName = "Bestellposition"

    override lazy val columns = autoColumns[Bestellposition]()

    def apply(rn: ResultName[Bestellposition])(rs: WrappedResultSet): Bestellposition =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Bestellposition): Seq[Any] = parameters(Bestellposition.unapply(entity).get)

    override def updateParameters(entity: Bestellposition): Seq[Tuple2[SQLSyntax, Any]] = autoUpdateParams(entity)
  }

  implicit val tourMapping = new BaseEntitySQLSyntaxSupport[Tour] {
    override val tableName = "Tour"

    override lazy val columns = autoColumns[Tour]()

    def apply(rn: ResultName[Tour])(rs: WrappedResultSet): Tour =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Tour): Seq[Any] = parameters(Tour.unapply(entity).get)

    override def updateParameters(entity: Tour): Seq[Tuple2[SQLSyntax, Any]] = autoUpdateParams(entity)
  }

  implicit val depotMapping = new BaseEntitySQLSyntaxSupport[Depot] {
    override val tableName = "Depot"

    override lazy val columns = autoColumns[Depot]()

    def apply(rn: ResultName[Depot])(rs: WrappedResultSet): Depot =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Depot): Seq[Any] = parameters(Depot.unapply(entity).get)

    override def updateParameters(entity: Depot): Seq[Tuple2[SQLSyntax, Any]] = autoUpdateParams(entity)
  }

  implicit val vertriebMapping = new BaseEntitySQLSyntaxSupport[Vertrieb] {
    override val tableName = "Vertrieb"

    override lazy val columns = autoColumns[Vertrieb]()

    def apply(rn: ResultName[Vertrieb])(rs: WrappedResultSet): Vertrieb =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Vertrieb): Seq[Any] = parameters(Vertrieb.unapply(entity).get)

    override def updateParameters(entity: Vertrieb): Seq[Tuple2[SQLSyntax, Any]] = autoUpdateParams(entity)
  }

  implicit val heimlieferungMapping = new BaseEntitySQLSyntaxSupport[Heimlieferung] {
    override val tableName = "Heimlieferung"

    override lazy val columns = autoColumns[Heimlieferung]()

    def apply(rn: ResultName[Heimlieferung])(rs: WrappedResultSet): Heimlieferung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Heimlieferung): Seq[Any] = parameters(Heimlieferung.unapply(entity).get)

    override def updateParameters(entity: Heimlieferung): Seq[Tuple2[SQLSyntax, Any]] = autoUpdateParams(entity)
  }

  implicit val depotlieferungMapping = new BaseEntitySQLSyntaxSupport[Depotlieferung] {
    override val tableName = "Depotlieferung"

    override lazy val columns = autoColumns[Depotlieferung]()

    def apply(rn: ResultName[Depotlieferung])(rs: WrappedResultSet): Depotlieferung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Depotlieferung): Seq[Any] =
      parameters(Depotlieferung.unapply(entity).get)

    override def updateParameters(entity: Depotlieferung): Seq[Tuple2[SQLSyntax, Any]] = autoUpdateParams(entity)
  }

  implicit val postlieferungMapping = new BaseEntitySQLSyntaxSupport[Postlieferung] {
    override val tableName = "Postlieferung"

    override lazy val columns = autoColumns[Postlieferung]()

    def apply(rn: ResultName[Postlieferung])(rs: WrappedResultSet): Postlieferung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Postlieferung): Seq[Any] = parameters(Postlieferung.unapply(entity).get)

    override def updateParameters(entity: Postlieferung): Seq[Tuple2[SQLSyntax, Any]] = autoUpdateParams(entity)
  }

  implicit val depotlieferungAboMapping = new BaseEntitySQLSyntaxSupport[DepotlieferungAbo] {
    override val tableName = "DepotlieferungAbo"

    override lazy val columns = autoColumns[DepotlieferungAbo]()

    def apply(rn: ResultName[DepotlieferungAbo])(rs: WrappedResultSet): DepotlieferungAbo = autoConstruct(rs, rn)

    def parameterMappings(entity: DepotlieferungAbo): Seq[Any] = parameters(DepotlieferungAbo.unapply(entity).get)

    override def updateParameters(entity: DepotlieferungAbo): Seq[Tuple2[SQLSyntax, Any]] = autoUpdateParams(entity)
  }

  implicit val heimlieferungAboMapping = new BaseEntitySQLSyntaxSupport[HeimlieferungAbo] {
    override val tableName = "HeimlieferungAbo"

    override lazy val columns = autoColumns[HeimlieferungAbo]()

    def apply(rn: ResultName[HeimlieferungAbo])(rs: WrappedResultSet): HeimlieferungAbo =
      autoConstruct(rs, rn)

    def parameterMappings(entity: HeimlieferungAbo): Seq[Any] = parameters(HeimlieferungAbo.unapply(entity).get)

    override def updateParameters(entity: HeimlieferungAbo): Seq[Tuple2[SQLSyntax, Any]] = autoUpdateParams(entity)
  }

  implicit val postlieferungAboMapping = new BaseEntitySQLSyntaxSupport[PostlieferungAbo] {
    override val tableName = "PostlieferungAbo"

    override lazy val columns = autoColumns[PostlieferungAbo]()

    def apply(rn: ResultName[PostlieferungAbo])(rs: WrappedResultSet): PostlieferungAbo =
      autoConstruct(rs, rn)

    def parameterMappings(entity: PostlieferungAbo): Seq[Any] = parameters(PostlieferungAbo.unapply(entity).get)

    override def updateParameters(entity: PostlieferungAbo): Seq[Tuple2[SQLSyntax, Any]] = autoUpdateParams(entity)
  }

  implicit val produktMapping = new BaseEntitySQLSyntaxSupport[Produkt] {
    override val tableName = "Produkt"

    override lazy val columns = autoColumns[Produkt]()

    def apply(rn: ResultName[Produkt])(rs: WrappedResultSet): Produkt =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Produkt): Seq[Any] = parameters(Produkt.unapply(entity).get)

    override def updateParameters(entity: Produkt): Seq[Tuple2[SQLSyntax, Any]] = autoUpdateParams(entity)
  }

  implicit val produzentMapping = new BaseEntitySQLSyntaxSupport[Produzent] {
    override val tableName = "Produzent"

    override lazy val columns = autoColumns[Produzent]()

    def apply(rn: ResultName[Produzent])(rs: WrappedResultSet): Produzent =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Produzent): Seq[Any] = parameters(Produzent.unapply(entity).get)

    override def updateParameters(entity: Produzent): Seq[Tuple2[SQLSyntax, Any]] = autoUpdateParams(entity)
  }

  implicit val produktekategorieMapping = new BaseEntitySQLSyntaxSupport[Produktekategorie] {
    override val tableName = "Produktekategorie"

    override lazy val columns = autoColumns[Produktekategorie]()

    def apply(rn: ResultName[Produktekategorie])(rs: WrappedResultSet): Produktekategorie =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Produktekategorie): Seq[Any] = parameters(Produktekategorie.unapply(entity).get)

    override def updateParameters(entity: Produktekategorie): Seq[Tuple2[SQLSyntax, Any]] = autoUpdateParams(entity)
  }

  implicit val projektMapping = new BaseEntitySQLSyntaxSupport[Projekt] {
    override val tableName = "Projekt"

    override lazy val columns = autoColumns[Projekt]()

    def apply(rn: ResultName[Projekt])(rs: WrappedResultSet): Projekt =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Projekt): Seq[Any] = parameters(Projekt.unapply(entity).get)

    override def updateParameters(entity: Projekt): Seq[Tuple2[SQLSyntax, Any]] = autoUpdateParams(entity)
  }

  implicit val produktProduzentMapping = new BaseEntitySQLSyntaxSupport[ProduktProduzent] {
    override val tableName = "ProduktProduzent"

    override lazy val columns = autoColumns[ProduktProduzent]()

    def apply(rn: ResultName[ProduktProduzent])(rs: WrappedResultSet): ProduktProduzent =
      autoConstruct(rs, rn)

    def parameterMappings(entity: ProduktProduzent): Seq[Any] = parameters(ProduktProduzent.unapply(entity).get)

    override def updateParameters(entity: ProduktProduzent): Seq[Tuple2[SQLSyntax, Any]] = autoUpdateParams(entity)
  }

  implicit val produktProduktekategorieMapping = new BaseEntitySQLSyntaxSupport[ProduktProduktekategorie] {
    override val tableName = "ProduktProduktekategorie"

    override lazy val columns = autoColumns[ProduktProduktekategorie]()

    def apply(rn: ResultName[ProduktProduktekategorie])(rs: WrappedResultSet): ProduktProduktekategorie =
      autoConstruct(rs, rn)

    def parameterMappings(entity: ProduktProduktekategorie): Seq[Any] = parameters(ProduktProduktekategorie.unapply(entity).get)

    override def updateParameters(entity: ProduktProduktekategorie): Seq[Tuple2[SQLSyntax, Any]] = autoUpdateParams(entity)
  }

  implicit val abwesenheitMapping = new BaseEntitySQLSyntaxSupport[Abwesenheit] {
    override val tableName = "Abwesenheit"

    override lazy val columns = autoColumns[Abwesenheit]()

    def apply(rn: ResultName[Abwesenheit])(rs: WrappedResultSet): Abwesenheit =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Abwesenheit): Seq[Any] = parameters(Abwesenheit.unapply(entity).get)

    override def updateParameters(entity: Abwesenheit): Seq[Tuple2[SQLSyntax, Any]] = autoUpdateParams(entity)
  }

  implicit val tourlieferungMapping = new BaseEntitySQLSyntaxSupport[Tourlieferung] {
    override val tableName = "Tourlieferung"

    override lazy val columns = autoColumns[Tourlieferung]()

    def apply(rn: ResultName[Tourlieferung])(rs: WrappedResultSet): Tourlieferung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Tourlieferung): Seq[Any] = parameters(Tourlieferung.unapply(entity).get)

    override def updateParameters(entity: Tourlieferung): Seq[Tuple2[SQLSyntax, Any]] = autoUpdateParams(entity)
  }

  implicit val korbMapping = new BaseEntitySQLSyntaxSupport[Korb] {
    override val tableName = "Korb"

    override lazy val columns = autoColumns[Korb]()

    def apply(rn: ResultName[Korb])(rs: WrappedResultSet): Korb =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Korb): Seq[Any] = parameters(Korb.unapply(entity).get)

    override def updateParameters(entity: Korb): Seq[Tuple2[SQLSyntax, Any]] = autoUpdateParams(entity)
  }

  implicit val depotAuslieferungMapping = new BaseEntitySQLSyntaxSupport[DepotAuslieferung] {
    override val tableName = "DepotAuslieferung"

    override lazy val columns = autoColumns[DepotAuslieferung]()

    def apply(rn: ResultName[DepotAuslieferung])(rs: WrappedResultSet): DepotAuslieferung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: DepotAuslieferung): Seq[Any] =
      parameters(DepotAuslieferung.unapply(entity).get)

    override def updateParameters(entity: DepotAuslieferung): Seq[Tuple2[SQLSyntax, Any]] = autoUpdateParams(entity)
  }

  implicit val tourAuslieferungMapping = new BaseEntitySQLSyntaxSupport[TourAuslieferung] {
    override val tableName = "TourAuslieferung"

    override lazy val columns = autoColumns[TourAuslieferung]()

    def apply(rn: ResultName[TourAuslieferung])(rs: WrappedResultSet): TourAuslieferung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: TourAuslieferung): Seq[Any] =
      parameters(TourAuslieferung.unapply(entity).get)

    override def updateParameters(entity: TourAuslieferung): Seq[Tuple2[SQLSyntax, Any]] = autoUpdateParams(entity)
  }

  implicit val postAuslieferungMapping = new BaseEntitySQLSyntaxSupport[PostAuslieferung] {
    override val tableName = "PostAuslieferung"

    override lazy val columns = autoColumns[PostAuslieferung]()

    def apply(rn: ResultName[PostAuslieferung])(rs: WrappedResultSet): PostAuslieferung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: PostAuslieferung): Seq[Any] =
      parameters(PostAuslieferung.unapply(entity).get)

    override def updateParameters(entity: PostAuslieferung): Seq[Tuple2[SQLSyntax, Any]] = autoUpdateParams(entity)
  }

  implicit val projektVorlageMapping = new BaseEntitySQLSyntaxSupport[ProjektVorlage] {
    override val tableName = "ProjektVorlage"

    override lazy val columns = autoColumns[ProjektVorlage]()

    def apply(rn: ResultName[ProjektVorlage])(rs: WrappedResultSet): ProjektVorlage =
      autoConstruct(rs, rn)

    def parameterMappings(entity: ProjektVorlage): Seq[Any] =
      parameters(ProjektVorlage.unapply(entity).get)

    override def updateParameters(entity: ProjektVorlage): Seq[Tuple2[SQLSyntax, Any]] = autoUpdateParams(entity)
  }

  implicit val einladungMapping = new BaseEntitySQLSyntaxSupport[Einladung] {
    override val tableName = "Einladung"

    override lazy val columns = autoColumns[Einladung]()

    def apply(rn: ResultName[Einladung])(rs: WrappedResultSet): Einladung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Einladung): Seq[Any] =
      parameters(Einladung.unapply(entity).get)

    override def updateParameters(entity: Einladung): Seq[Tuple2[SQLSyntax, Any]] = autoUpdateParams(entity)
  }

  implicit val kontoDatenMapping = new BaseEntitySQLSyntaxSupport[KontoDaten] {
    override val tableName = "KontoDaten"

    override lazy val columns = autoColumns[KontoDaten]()

    def apply(rn: ResultName[KontoDaten])(rs: WrappedResultSet): KontoDaten =
      autoConstruct(rs, rn)

    def parameterMappings(entity: KontoDaten): Seq[Any] =
      parameters(KontoDaten.unapply(entity).get)

    override def updateParameters(entity: KontoDaten): Seq[Tuple2[SQLSyntax, Any]] = autoUpdateParams(entity)
  }
}
