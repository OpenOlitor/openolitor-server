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
import ch.openolitor.stammdaten.models._
import scalikejdbc._
import ch.openolitor.core.repositories.DBMappings
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.stammdaten.models.PendenzStatus
import ch.openolitor.core.repositories.BaseEntitySQLSyntaxSupport
import ch.openolitor.core.scalax._
import ch.openolitor.core.Macros._
import scala.collection.immutable.TreeMap
import ch.openolitor.core.filestore.VorlageRechnung
import scala.math.Ordering.StringOrdering
import ch.openolitor.core.repositories.BaseParameter

//DB Model bindig
trait StammdatenDBMappings extends DBMappings with LazyLogging with BaseParameter {

  val fristeinheitPattern = """(\d+)(M|W)""".r

  // DB type binders for read operations
  implicit val tourIdBinder: Binders[TourId] = baseIdBinders(TourId.apply _)
  implicit val depotIdBinder: Binders[DepotId] = baseIdBinders(DepotId.apply _)
  implicit val aboTypIdBinder: Binders[AbotypId] = baseIdBinders(AbotypId.apply _)
  implicit val vertriebIdBinder: Binders[VertriebId] = baseIdBinders(VertriebId.apply _)
  implicit val vertriebsartIdBinder: Binders[VertriebsartId] = baseIdBinders(VertriebsartId.apply _)
  implicit val vertriebsartIdSetBinder: Binders[Set[VertriebsartId]] = setBaseIdBinders(VertriebsartId.apply _)
  implicit val vertriebsartIdSeqBinder: Binders[Seq[VertriebsartId]] = seqBaseIdBinders(VertriebsartId.apply _)
  implicit val kundeIdBinder: Binders[KundeId] = baseIdBinders(KundeId.apply _)
  implicit val pendenzIdBinder: Binders[PendenzId] = baseIdBinders(PendenzId.apply _)
  implicit val aboIdBinder: Binders[AboId] = baseIdBinders(AboId.apply _)
  implicit val lierferungIdBinder: Binders[LieferungId] = baseIdBinders(LieferungId.apply _)
  implicit val lieferplanungIdBinder: Binders[LieferplanungId] = baseIdBinders(LieferplanungId.apply _)
  implicit val optionLieferplanungIdBinder: Binders[Option[LieferplanungId]] = optionBaseIdBinders(LieferplanungId.apply _)
  implicit val lieferpositionIdBinder: Binders[LieferpositionId] = baseIdBinders(LieferpositionId.apply _)
  implicit val bestellungIdBinder: Binders[BestellungId] = baseIdBinders(BestellungId.apply _)
  implicit val sammelbestellungIdBinder: Binders[SammelbestellungId] = baseIdBinders(SammelbestellungId.apply _)
  implicit val bestellpositionIdBinder: Binders[BestellpositionId] = baseIdBinders(BestellpositionId.apply _)
  implicit val customKundentypIdBinder: Binders[CustomKundentypId] = baseIdBinders(CustomKundentypId.apply _)
  implicit val kundentypIdBinder: Binders[KundentypId] = Binders.string.xmap(KundentypId.apply _, _.id)
  implicit val produktekategorieIdBinder: Binders[ProduktekategorieId] = baseIdBinders(ProduktekategorieId.apply _)
  implicit val baseProduktekategorieIdBinder: Binders[BaseProduktekategorieId] = Binders.string.xmap(BaseProduktekategorieId.apply _, _.id)
  implicit val produktIdBinder: Binders[ProduktId] = baseIdBinders(ProduktId.apply _)
  implicit val optionProduktIdBinder: Binders[Option[ProduktId]] = optionBaseIdBinders(ProduktId.apply _)
  implicit val produzentIdBinder: Binders[ProduzentId] = baseIdBinders(ProduzentId.apply _)
  implicit val baseProduzentIdBinder: Binders[BaseProduzentId] = Binders.string.xmap(BaseProduzentId.apply _, _.id)
  implicit val projektIdBinder: Binders[ProjektId] = baseIdBinders(ProjektId.apply _)
  implicit val produktProduzentIdBinder: Binders[ProduktProduzentId] = baseIdBinders(ProduktProduzentId.apply _)
  implicit val produktProduktekategorieIdBinder: Binders[ProduktProduktekategorieId] = baseIdBinders(ProduktProduktekategorieId.apply _)
  implicit val abwesenheitIdBinder: Binders[AbwesenheitId] = baseIdBinders(AbwesenheitId.apply _)
  implicit val korbIdBinder: Binders[KorbId] = baseIdBinders(KorbId.apply _)
  implicit val auslieferungIdBinder: Binders[AuslieferungId] = baseIdBinders(AuslieferungId.apply _)
  implicit val projektVorlageIdBinder: Binders[ProjektVorlageId] = baseIdBinders(ProjektVorlageId.apply _)
  implicit val optionAuslieferungIdBinder: Binders[Option[AuslieferungId]] = optionBaseIdBinders(AuslieferungId.apply _)
  implicit val einladungIdBinder: Binders[EinladungId] = baseIdBinders(EinladungId.apply _)
  implicit val kontoDatenIdBinder: Binders[KontoDatenId] = baseIdBinders(KontoDatenId.apply _)

  implicit val pendenzStatusBinders: Binders[PendenzStatus] = toStringBinder(PendenzStatus.apply)
  implicit val rhythmusBinders: Binders[Rhythmus] = toStringBinder(Rhythmus.apply)
  implicit val waehrungBinders: Binders[Waehrung] = toStringBinder(Waehrung.apply)
  implicit val lieferungStatusBinders: Binders[LieferungStatus] = toStringBinder(LieferungStatus.apply)
  implicit val korbStatusBinders: Binders[KorbStatus] = toStringBinder(KorbStatus.apply)
  implicit val auslieferungStatusBinders: Binders[AuslieferungStatus] = toStringBinder(AuslieferungStatus.apply)
  implicit val preiseinheitBinders: Binders[Preiseinheit] = toStringBinder(Preiseinheit.apply)
  implicit val lieferzeitpunktBinders: Binders[Lieferzeitpunkt] = toStringBinder(Lieferzeitpunkt.apply)
  implicit val lieferzeitpunktSetBinders: Binders[Set[Lieferzeitpunkt]] = setSqlBinder(Lieferzeitpunkt.apply, _.toString)
  implicit val kundenTypIdSetBinder: Binders[Set[KundentypId]] = setSqlBinder(KundentypId.apply, _.toString)
  implicit val laufzeiteinheitBinders: Binders[Laufzeiteinheit] = toStringBinder(Laufzeiteinheit.apply)
  implicit val liefereinheiBinders: Binders[Liefereinheit] = toStringBinder(Liefereinheit.apply)
  implicit val liefersaisonBinders: Binders[Liefersaison] = toStringBinder(Liefersaison.apply)
  implicit val vorlageTypeBinders: Binders[VorlageTyp] = toStringBinder(VorlageTyp.apply)
  implicit val anredeBinders: Binders[Option[Anrede]] = toStringBinder(Anrede.apply)
  implicit val fristBinders: Binders[Option[Frist]] = Binders.string.xmap(_ match {
    case fristeinheitPattern(wert, "W") => Some(Frist(wert.toInt, Wochenfrist))
    case fristeinheitPattern(wert, "M") => Some(Frist(wert.toInt, Monatsfrist))
    case _ => None
  }, {
    _ match {
      case None => ""
      case Some(frist) =>
        val einheit = frist.einheit match {
          case Wochenfrist => "W"
          case Monatsfrist => "M"
        }
        s"${frist.wert}$einheit"
    }
  })

  implicit val baseProduktekategorieIdSetBinders: Binders[Set[BaseProduktekategorieId]] = setBaseStringIdBinders(BaseProduktekategorieId.apply _)
  implicit val baseProduzentIdSetBinders: Binders[Set[BaseProduzentId]] = setBaseStringIdBinders(BaseProduzentId.apply _)
  implicit val stringIntTreeMapBinders: Binders[TreeMap[String, Int]] = treeMapBinders[String, Int](identity, _.toInt, identity, _.toString)
  implicit val stringBigDecimalTreeMapBinders: Binders[TreeMap[String, BigDecimal]] = treeMapBinders(identity, BigDecimal(_), identity, _.toString)
  implicit val rolleMapBinders: Binders[Map[Rolle, Boolean]] = mapBinders(r => Rolle(r).getOrElse(KundenZugang), _.toBoolean, _.toString, _.toString)
  implicit val rolleBinders: Binders[Option[Rolle]] = toStringBinder(Rolle.apply)

  // declare parameterbinderfactories for enum type to allow dynamic type convertion of enum subtypes
  implicit def pendenzStatusParameterBinderFactory[A <: PendenzStatus]: ParameterBinderFactory[A] = ParameterBinderFactory.stringParameterBinderFactory.contramap(_.toString)
  implicit def rhythmusParameterBinderFactory[A <: Rhythmus]: ParameterBinderFactory[A] = ParameterBinderFactory.stringParameterBinderFactory.contramap(_.toString)
  implicit def waehrungParameterBinderFactory[A <: Waehrung]: ParameterBinderFactory[A] = ParameterBinderFactory.stringParameterBinderFactory.contramap(_.toString)
  implicit def lieferungStatusParameterBinderFactory[A <: LieferungStatus]: ParameterBinderFactory[A] = ParameterBinderFactory.stringParameterBinderFactory.contramap(_.toString)
  implicit def korbStatusParameterBinderFactory[A <: KorbStatus]: ParameterBinderFactory[A] = ParameterBinderFactory.stringParameterBinderFactory.contramap(_.toString)
  implicit def auslieferungStatusParameterBinderFactory[A <: AuslieferungStatus]: ParameterBinderFactory[A] = ParameterBinderFactory.stringParameterBinderFactory.contramap(_.toString)
  implicit def preiseinheitParameterBinderFactory[A <: Preiseinheit]: ParameterBinderFactory[A] = ParameterBinderFactory.stringParameterBinderFactory.contramap(_.toString)
  implicit def lieferzeitpunktParameterBinderFactory[A <: Lieferzeitpunkt]: ParameterBinderFactory[A] = ParameterBinderFactory.stringParameterBinderFactory.contramap(_.toString)
  implicit def laufzeiteinheitParameterBinderFactory[A <: Laufzeiteinheit]: ParameterBinderFactory[A] = ParameterBinderFactory.stringParameterBinderFactory.contramap(_.toString)
  implicit def liefereinheitParameterBinderFactory[A <: Liefereinheit]: ParameterBinderFactory[A] = ParameterBinderFactory.stringParameterBinderFactory.contramap(_.toString)
  implicit def liefersaisonParameterBinderFactory[A <: Liefersaison]: ParameterBinderFactory[A] = ParameterBinderFactory.stringParameterBinderFactory.contramap(_.toString)
  implicit def vorlageParameterBinderFactory[A <: VorlageTyp]: ParameterBinderFactory[A] = ParameterBinderFactory.stringParameterBinderFactory.contramap(_.toString)

  implicit val abotypMapping = new BaseEntitySQLSyntaxSupport[Abotyp] {
    override val tableName = "Abotyp"

    override lazy val columns = autoColumns[Abotyp]()

    def apply(rn: ResultName[Abotyp])(rs: WrappedResultSet): Abotyp = autoConstruct(rs, rn)

    def parameterMappings(entity: Abotyp): Seq[ParameterBinder] =
      parameters(Abotyp.unapply(entity).get)

    override def updateParameters(entity: Abotyp): Seq[Tuple2[SQLSyntax, ParameterBinder]] = autoUpdateParams(entity)
  }

  implicit val zusatzAbotypMapping = new BaseEntitySQLSyntaxSupport[ZusatzAbotyp] {
    override val tableName = "ZusatzAbotyp"

    override lazy val columns = autoColumns[ZusatzAbotyp]()

    def apply(rn: ResultName[ZusatzAbotyp])(rs: WrappedResultSet): ZusatzAbotyp = autoConstruct(rs, rn)

    def parameterMappings(entity: ZusatzAbotyp): Seq[ParameterBinder] =
      parameters(ZusatzAbotyp.unapply(entity).get)

    override def updateParameters(entity: ZusatzAbotyp): Seq[Tuple2[SQLSyntax, ParameterBinder]] = autoUpdateParams(entity)
  }

  implicit val customKundentypMapping = new BaseEntitySQLSyntaxSupport[CustomKundentyp] {
    override val tableName = "Kundentyp"

    override lazy val columns = autoColumns[CustomKundentyp]()

    def apply(rn: ResultName[CustomKundentyp])(rs: WrappedResultSet): CustomKundentyp =
      autoConstruct(rs, rn)

    def parameterMappings(entity: CustomKundentyp): Seq[ParameterBinder] =
      parameters(CustomKundentyp.unapply(entity).get)

    override def updateParameters(entity: CustomKundentyp): Seq[Tuple2[SQLSyntax, ParameterBinder]] = autoUpdateParams(entity)
  }

  implicit val kundeMapping = new BaseEntitySQLSyntaxSupport[Kunde] {
    override val tableName = "Kunde"

    override lazy val columns = autoColumns[Kunde]()

    def apply(rn: ResultName[Kunde])(rs: WrappedResultSet): Kunde =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Kunde): Seq[ParameterBinder] =
      parameters(Kunde.unapply(entity).get)

    override def updateParameters(entity: Kunde): Seq[Tuple2[SQLSyntax, ParameterBinder]] = autoUpdateParams(entity)
  }

  implicit val personMapping = new BaseEntitySQLSyntaxSupport[Person] {
    override val tableName = "Person"

    override lazy val columns = autoColumns[Person]()

    def apply(rn: ResultName[Person])(rs: WrappedResultSet): Person =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Person): Seq[ParameterBinder] =
      parameters(Person.unapply(entity).get)

    override def updateParameters(entity: Person): Seq[Tuple2[SQLSyntax, ParameterBinder]] = autoUpdateParams(entity)
  }

  implicit val pendenzMapping = new BaseEntitySQLSyntaxSupport[Pendenz] {
    override val tableName = "Pendenz"

    override lazy val columns = autoColumns[Pendenz]()

    def apply(rn: ResultName[Pendenz])(rs: WrappedResultSet): Pendenz =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Pendenz): Seq[ParameterBinder] =
      parameters(Pendenz.unapply(entity).get)

    override def updateParameters(entity: Pendenz): Seq[Tuple2[SQLSyntax, ParameterBinder]] = autoUpdateParams(entity)
  }

  implicit val lieferungMapping = new BaseEntitySQLSyntaxSupport[Lieferung] {
    override val tableName = "Lieferung"

    override lazy val columns = autoColumns[Lieferung]()

    def apply(rn: ResultName[Lieferung])(rs: WrappedResultSet): Lieferung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Lieferung): Seq[ParameterBinder] =
      parameters(Lieferung.unapply(entity).get)

    override def updateParameters(entity: Lieferung): Seq[Tuple2[SQLSyntax, ParameterBinder]] = autoUpdateParams(entity)
  }

  implicit val lieferplanungMapping = new BaseEntitySQLSyntaxSupport[Lieferplanung] {
    override val tableName = "Lieferplanung"

    override lazy val columns = autoColumns[Lieferplanung]()

    def apply(rn: ResultName[Lieferplanung])(rs: WrappedResultSet): Lieferplanung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Lieferplanung): Seq[ParameterBinder] = parameters(Lieferplanung.unapply(entity).get)

    override def updateParameters(entity: Lieferplanung): Seq[Tuple2[SQLSyntax, ParameterBinder]] = autoUpdateParams(entity)
  }

  implicit val lieferpositionMapping = new BaseEntitySQLSyntaxSupport[Lieferposition] {
    override val tableName = "Lieferposition"

    override lazy val columns = autoColumns[Lieferposition]()

    def apply(rn: ResultName[Lieferposition])(rs: WrappedResultSet): Lieferposition =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Lieferposition): Seq[ParameterBinder] = parameters(Lieferposition.unapply(entity).get)

    override def updateParameters(entity: Lieferposition): Seq[Tuple2[SQLSyntax, ParameterBinder]] = autoUpdateParams(entity)
  }

  implicit val sammelbestellungMapping = new BaseEntitySQLSyntaxSupport[Sammelbestellung] {
    override val tableName = "Sammelbestellung"

    override lazy val columns = autoColumns[Sammelbestellung]()

    def apply(rn: ResultName[Sammelbestellung])(rs: WrappedResultSet): Sammelbestellung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Sammelbestellung): Seq[ParameterBinder] = parameters(Sammelbestellung.unapply(entity).get)

    override def updateParameters(entity: Sammelbestellung): Seq[Tuple2[SQLSyntax, ParameterBinder]] = autoUpdateParams(entity)
  }

  implicit val bestellungMapping = new BaseEntitySQLSyntaxSupport[Bestellung] {
    override val tableName = "Bestellung"

    override lazy val columns = autoColumns[Bestellung]()

    def apply(rn: ResultName[Bestellung])(rs: WrappedResultSet): Bestellung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Bestellung): Seq[ParameterBinder] = parameters(Bestellung.unapply(entity).get)

    override def updateParameters(entity: Bestellung): Seq[Tuple2[SQLSyntax, ParameterBinder]] = autoUpdateParams(entity)
  }

  implicit val bestellpositionMapping = new BaseEntitySQLSyntaxSupport[Bestellposition] {
    override val tableName = "Bestellposition"

    override lazy val columns = autoColumns[Bestellposition]()

    def apply(rn: ResultName[Bestellposition])(rs: WrappedResultSet): Bestellposition =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Bestellposition): Seq[ParameterBinder] = parameters(Bestellposition.unapply(entity).get)

    override def updateParameters(entity: Bestellposition): Seq[Tuple2[SQLSyntax, ParameterBinder]] = autoUpdateParams(entity)
  }

  implicit val tourMapping = new BaseEntitySQLSyntaxSupport[Tour] {
    override val tableName = "Tour"

    override lazy val columns = autoColumns[Tour]()

    def apply(rn: ResultName[Tour])(rs: WrappedResultSet): Tour =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Tour): Seq[ParameterBinder] = parameters(Tour.unapply(entity).get)

    override def updateParameters(entity: Tour): Seq[Tuple2[SQLSyntax, ParameterBinder]] = autoUpdateParams(entity)
  }

  implicit val depotMapping = new BaseEntitySQLSyntaxSupport[Depot] {
    override val tableName = "Depot"

    override lazy val columns = autoColumns[Depot]()

    def apply(rn: ResultName[Depot])(rs: WrappedResultSet): Depot =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Depot): Seq[ParameterBinder] = parameters(Depot.unapply(entity).get)

    override def updateParameters(entity: Depot): Seq[Tuple2[SQLSyntax, ParameterBinder]] = autoUpdateParams(entity)
  }

  implicit val vertriebMapping = new BaseEntitySQLSyntaxSupport[Vertrieb] {
    override val tableName = "Vertrieb"

    override lazy val columns = autoColumns[Vertrieb]()

    def apply(rn: ResultName[Vertrieb])(rs: WrappedResultSet): Vertrieb = autoConstruct(rs, rn)

    def parameterMappings(entity: Vertrieb): Seq[ParameterBinder] = parameters(Vertrieb.unapply(entity).get)

    override def updateParameters(entity: Vertrieb): Seq[Tuple2[SQLSyntax, ParameterBinder]] = autoUpdateParams(entity)
  }

  implicit val heimlieferungMapping = new BaseEntitySQLSyntaxSupport[Heimlieferung] {
    override val tableName = "Heimlieferung"

    override lazy val columns = autoColumns[Heimlieferung]()

    def apply(rn: ResultName[Heimlieferung])(rs: WrappedResultSet): Heimlieferung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Heimlieferung): Seq[ParameterBinder] = parameters(Heimlieferung.unapply(entity).get)

    override def updateParameters(entity: Heimlieferung): Seq[Tuple2[SQLSyntax, ParameterBinder]] = autoUpdateParams(entity)
  }

  implicit val depotlieferungMapping = new BaseEntitySQLSyntaxSupport[Depotlieferung] {
    override val tableName = "Depotlieferung"

    override lazy val columns = autoColumns[Depotlieferung]()

    def apply(rn: ResultName[Depotlieferung])(rs: WrappedResultSet): Depotlieferung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Depotlieferung): Seq[ParameterBinder] =
      parameters(Depotlieferung.unapply(entity).get)

    override def updateParameters(entity: Depotlieferung): Seq[Tuple2[SQLSyntax, ParameterBinder]] = autoUpdateParams(entity)
  }

  implicit val postlieferungMapping = new BaseEntitySQLSyntaxSupport[Postlieferung] {
    override val tableName = "Postlieferung"

    override lazy val columns = autoColumns[Postlieferung]()

    def apply(rn: ResultName[Postlieferung])(rs: WrappedResultSet): Postlieferung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Postlieferung): Seq[ParameterBinder] = parameters(Postlieferung.unapply(entity).get)

    override def updateParameters(entity: Postlieferung): Seq[Tuple2[SQLSyntax, ParameterBinder]] = autoUpdateParams(entity)
  }

  implicit val depotlieferungAboMapping = new BaseEntitySQLSyntaxSupport[DepotlieferungAbo] {
    override val tableName = "DepotlieferungAbo"

    override lazy val columns = autoColumns[DepotlieferungAbo]()

    def apply(rn: ResultName[DepotlieferungAbo])(rs: WrappedResultSet): DepotlieferungAbo = autoConstruct(rs, rn)

    def parameterMappings(entity: DepotlieferungAbo): Seq[ParameterBinder] = parameters(DepotlieferungAbo.unapply(entity).get)

    override def updateParameters(entity: DepotlieferungAbo): Seq[Tuple2[SQLSyntax, ParameterBinder]] = autoUpdateParams(entity)
  }

  implicit val heimlieferungAboMapping = new BaseEntitySQLSyntaxSupport[HeimlieferungAbo] {
    override val tableName = "HeimlieferungAbo"

    override lazy val columns = autoColumns[HeimlieferungAbo]()

    def apply(rn: ResultName[HeimlieferungAbo])(rs: WrappedResultSet): HeimlieferungAbo = autoConstruct(rs, rn)

    def parameterMappings(entity: HeimlieferungAbo): Seq[ParameterBinder] = parameters(HeimlieferungAbo.unapply(entity).get)

    override def updateParameters(entity: HeimlieferungAbo): Seq[Tuple2[SQLSyntax, ParameterBinder]] = autoUpdateParams(entity)
  }

  implicit val postlieferungAboMapping = new BaseEntitySQLSyntaxSupport[PostlieferungAbo] {
    override val tableName = "PostlieferungAbo"

    override lazy val columns = autoColumns[PostlieferungAbo]()

    def apply(rn: ResultName[PostlieferungAbo])(rs: WrappedResultSet): PostlieferungAbo = autoConstruct(rs, rn)

    def parameterMappings(entity: PostlieferungAbo): Seq[ParameterBinder] = parameters(PostlieferungAbo.unapply(entity).get)
  }

  implicit val zusatzAboMapping = new BaseEntitySQLSyntaxSupport[ZusatzAbo] {
    override val tableName = "ZusatzAbo"

    override lazy val columns = autoColumns[ZusatzAbo]()

    override def updateParameters(entity: PostlieferungAbo): Seq[Tuple2[SQLSyntax, ParameterBinder]] = autoUpdateParams(entity)
    def apply(rn: ResultName[ZusatzAbo])(rs: WrappedResultSet): ZusatzAbo = autoConstruct(rs, rn)

    def parameterMappings(entity: ZusatzAbo): Seq[ParameterBinder] = {
      parameters(ZusatzAbo.unapply(entity).get)
    }
  }

  implicit val produktMapping = new BaseEntitySQLSyntaxSupport[Produkt] {
    override val tableName = "Produkt"

    override lazy val columns = autoColumns[Produkt]()

    def apply(rn: ResultName[Produkt])(rs: WrappedResultSet): Produkt =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Produkt): Seq[ParameterBinder] = parameters(Produkt.unapply(entity).get)

    override def updateParameters(entity: Produkt): Seq[Tuple2[SQLSyntax, ParameterBinder]] = autoUpdateParams(entity)
  }

  implicit val produzentMapping = new BaseEntitySQLSyntaxSupport[Produzent] {
    override val tableName = "Produzent"

    override lazy val columns = autoColumns[Produzent]()

    def apply(rn: ResultName[Produzent])(rs: WrappedResultSet): Produzent =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Produzent): Seq[ParameterBinder] = parameters(Produzent.unapply(entity).get)

    override def updateParameters(entity: Produzent): Seq[Tuple2[SQLSyntax, ParameterBinder]] = autoUpdateParams(entity)
  }

  implicit val produktekategorieMapping = new BaseEntitySQLSyntaxSupport[Produktekategorie] {
    override val tableName = "Produktekategorie"

    override lazy val columns = autoColumns[Produktekategorie]()

    def apply(rn: ResultName[Produktekategorie])(rs: WrappedResultSet): Produktekategorie =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Produktekategorie): Seq[ParameterBinder] = parameters(Produktekategorie.unapply(entity).get)

    override def updateParameters(entity: Produktekategorie): Seq[Tuple2[SQLSyntax, ParameterBinder]] = autoUpdateParams(entity)
  }

  implicit val projektMapping = new BaseEntitySQLSyntaxSupport[Projekt] {
    override val tableName = "Projekt"

    override lazy val columns = autoColumns[Projekt]()

    def apply(rn: ResultName[Projekt])(rs: WrappedResultSet): Projekt =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Projekt): Seq[ParameterBinder] = parameters(Projekt.unapply(entity).get)

    override def updateParameters(entity: Projekt): Seq[Tuple2[SQLSyntax, ParameterBinder]] = autoUpdateParams(entity)
  }

  implicit val produktProduzentMapping = new BaseEntitySQLSyntaxSupport[ProduktProduzent] {
    override val tableName = "ProduktProduzent"

    override lazy val columns = autoColumns[ProduktProduzent]()

    def apply(rn: ResultName[ProduktProduzent])(rs: WrappedResultSet): ProduktProduzent =
      autoConstruct(rs, rn)

    def parameterMappings(entity: ProduktProduzent): Seq[ParameterBinder] = parameters(ProduktProduzent.unapply(entity).get)

    override def updateParameters(entity: ProduktProduzent): Seq[Tuple2[SQLSyntax, ParameterBinder]] = autoUpdateParams(entity)
  }

  implicit val produktProduktekategorieMapping = new BaseEntitySQLSyntaxSupport[ProduktProduktekategorie] {
    override val tableName = "ProduktProduktekategorie"

    override lazy val columns = autoColumns[ProduktProduktekategorie]()

    def apply(rn: ResultName[ProduktProduktekategorie])(rs: WrappedResultSet): ProduktProduktekategorie =
      autoConstruct(rs, rn)

    def parameterMappings(entity: ProduktProduktekategorie): Seq[ParameterBinder] = parameters(ProduktProduktekategorie.unapply(entity).get)

    override def updateParameters(entity: ProduktProduktekategorie): Seq[Tuple2[SQLSyntax, ParameterBinder]] = autoUpdateParams(entity)
  }

  implicit val abwesenheitMapping = new BaseEntitySQLSyntaxSupport[Abwesenheit] {
    override val tableName = "Abwesenheit"

    override lazy val columns = autoColumns[Abwesenheit]()

    def apply(rn: ResultName[Abwesenheit])(rs: WrappedResultSet): Abwesenheit = autoConstruct(rs, rn)

    def parameterMappings(entity: Abwesenheit): Seq[ParameterBinder] = parameters(Abwesenheit.unapply(entity).get)

    override def updateParameters(entity: Abwesenheit): Seq[Tuple2[SQLSyntax, ParameterBinder]] = autoUpdateParams(entity)
  }

  implicit val tourlieferungMapping = new BaseEntitySQLSyntaxSupport[Tourlieferung] {
    override val tableName = "Tourlieferung"

    override lazy val columns = autoColumns[Tourlieferung]()

    def apply(rn: ResultName[Tourlieferung])(rs: WrappedResultSet): Tourlieferung = autoConstruct(rs, rn)

    def parameterMappings(entity: Tourlieferung): Seq[ParameterBinder] = parameters(Tourlieferung.unapply(entity).get)

    override def updateParameters(entity: Tourlieferung): Seq[Tuple2[SQLSyntax, ParameterBinder]] = autoUpdateParams(entity)
  }

  implicit val korbMapping = new BaseEntitySQLSyntaxSupport[Korb] {
    override val tableName = "Korb"

    override lazy val columns = autoColumns[Korb]()

    def apply(rn: ResultName[Korb])(rs: WrappedResultSet): Korb =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Korb): Seq[ParameterBinder] = parameters(Korb.unapply(entity).get)

    override def updateParameters(entity: Korb): Seq[Tuple2[SQLSyntax, ParameterBinder]] = autoUpdateParams(entity)
  }

  implicit val depotAuslieferungMapping = new BaseEntitySQLSyntaxSupport[DepotAuslieferung] {
    override val tableName = "DepotAuslieferung"

    override lazy val columns = autoColumns[DepotAuslieferung]()

    def apply(rn: ResultName[DepotAuslieferung])(rs: WrappedResultSet): DepotAuslieferung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: DepotAuslieferung): Seq[ParameterBinder] =
      parameters(DepotAuslieferung.unapply(entity).get)

    override def updateParameters(entity: DepotAuslieferung): Seq[Tuple2[SQLSyntax, ParameterBinder]] = autoUpdateParams(entity)
  }

  implicit val tourAuslieferungMapping = new BaseEntitySQLSyntaxSupport[TourAuslieferung] {
    override val tableName = "TourAuslieferung"

    override lazy val columns = autoColumns[TourAuslieferung]()

    def apply(rn: ResultName[TourAuslieferung])(rs: WrappedResultSet): TourAuslieferung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: TourAuslieferung): Seq[ParameterBinder] =
      parameters(TourAuslieferung.unapply(entity).get)

    override def updateParameters(entity: TourAuslieferung): Seq[Tuple2[SQLSyntax, ParameterBinder]] = autoUpdateParams(entity)
  }

  implicit val postAuslieferungMapping = new BaseEntitySQLSyntaxSupport[PostAuslieferung] {
    override val tableName = "PostAuslieferung"

    override lazy val columns = autoColumns[PostAuslieferung]()

    def apply(rn: ResultName[PostAuslieferung])(rs: WrappedResultSet): PostAuslieferung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: PostAuslieferung): Seq[ParameterBinder] =
      parameters(PostAuslieferung.unapply(entity).get)

    override def updateParameters(entity: PostAuslieferung): Seq[Tuple2[SQLSyntax, ParameterBinder]] = autoUpdateParams(entity)
  }

  implicit val projektVorlageMapping = new BaseEntitySQLSyntaxSupport[ProjektVorlage] {
    override val tableName = "ProjektVorlage"

    override lazy val columns = autoColumns[ProjektVorlage]()

    def apply(rn: ResultName[ProjektVorlage])(rs: WrappedResultSet): ProjektVorlage =
      autoConstruct(rs, rn)

    def parameterMappings(entity: ProjektVorlage): Seq[ParameterBinder] =
      parameters(ProjektVorlage.unapply(entity).get)

    override def updateParameters(entity: ProjektVorlage): Seq[Tuple2[SQLSyntax, ParameterBinder]] = autoUpdateParams(entity)
  }

  implicit val einladungMapping = new BaseEntitySQLSyntaxSupport[Einladung] {
    override val tableName = "Einladung"

    override lazy val columns = autoColumns[Einladung]()

    def apply(rn: ResultName[Einladung])(rs: WrappedResultSet): Einladung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Einladung): Seq[ParameterBinder] =
      parameters(Einladung.unapply(entity).get)

    override def updateParameters(entity: Einladung): Seq[Tuple2[SQLSyntax, ParameterBinder]] = autoUpdateParams(entity)
  }

  implicit val kontoDatenMapping = new BaseEntitySQLSyntaxSupport[KontoDaten] {
    override val tableName = "KontoDaten"

    override lazy val columns = autoColumns[KontoDaten]()

    def apply(rn: ResultName[KontoDaten])(rs: WrappedResultSet): KontoDaten =
      autoConstruct(rs, rn)

    def parameterMappings(entity: KontoDaten): Seq[ParameterBinder] =
      parameters(KontoDaten.unapply(entity).get)

    override def updateParameters(entity: KontoDaten): Seq[Tuple2[SQLSyntax, ParameterBinder]] = autoUpdateParams(entity)
  }
}
