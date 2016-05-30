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
import ch.openolitor.core.repositories.BaseRepository
import ch.openolitor.core.repositories.BaseRepository._
import ch.openolitor.stammdaten.models._
import scalikejdbc._
import scalikejdbc.TypeBinder._
import ch.openolitor.core.repositories.DBMappings
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.stammdaten.models.PendenzStatus
import ch.openolitor.core.repositories.BaseEntitySQLSyntaxSupport
import ch.openolitor.core.scalax._
import scala.collection.immutable.TreeMap

//DB Model bindig
trait StammdatenDBMappings extends DBMappings with LazyLogging {
  import TypeBinder._

  val fristeinheitPattern = """(\d+)(M|W)""".r

  //DB binders
  implicit val pendenzStatusBinder: Binders[PendenzStatus] = Binders.string.xmap(PendenzStatus.apply, _.productPrefix)
  implicit val rhytmusSqlBinder: Binders[Rhythmus] = Binders.string.xmap(Rhythmus.apply, _.productPrefix)
  implicit val preiseinheitSqlBinder: Binders[Preiseinheit] = Binders.string.xmap(Preiseinheit.apply, _.productPrefix)
  implicit val waehrungSqlBinder: Binders[Waehrung] = Binders.string.xmap(Waehrung.apply, _.productPrefix)
  implicit val lieferungStatusSqlBinder: Binders[LieferungStatus] = Binders.string.xmap(LieferungStatus.apply, _.productPrefix)
  implicit val korbStatusSqlBinder: Binders[KorbStatus] = Binders.string.xmap(KorbStatus.apply, _.productPrefix)
  implicit val lieferzeitpunktSqlBinder: Binders[Lieferzeitpunkt] = Binders.string.xmap(Lieferzeitpunkt.apply, _.productPrefix)
  implicit val lieferzeitpunktSetSqlBinder = setParameterBinderFactory[Lieferzeitpunkt](Lieferzeitpunkt.apply, _.productPrefix)
  implicit val laufzeiteinheitSqlBinder: Binders[Laufzeiteinheit] = Binders.string.xmap(Laufzeiteinheit.apply, _.productPrefix)
  implicit val liefereinheitSqlBinder: Binders[Liefereinheit] = Binders.string.xmap(Liefereinheit.apply, _.productPrefix)
  implicit val liefersaisonSqlBinder: Binders[Liefersaison] = Binders.string.xmap(Liefersaison.apply, _.productPrefix)
  implicit val anredeSqlBinder: Binders[Anrede] = Binders.string.xmap(Anrede.apply, _.productPrefix)

  implicit val abotypIdSqlBinder = baseIdParameterBinderFactory[AbotypId](AbotypId.apply)
  implicit val depotIdSqlBinder = baseIdParameterBinderFactory[DepotId](DepotId.apply)
  implicit val tourIdSqlBinder = baseIdParameterBinderFactory[TourId](TourId.apply)
  implicit val vertriebIdSqlBinder = baseIdParameterBinderFactory[VertriebId](VertriebId.apply)
  implicit val vertriebsartIdSqlBinder = baseIdParameterBinderFactory[VertriebsartId](VertriebsartId.apply)
  implicit val kundeIdSqlBinder = baseIdParameterBinderFactory[KundeId](KundeId.apply)
  implicit val pendenzIdSqlBinder = baseIdParameterBinderFactory[PendenzId](PendenzId.apply)
  implicit val customKundentypIdSqlBinder = baseIdParameterBinderFactory[CustomKundentypId](CustomKundentypId.apply)
  implicit val kundentypIdSqlBinder: Binders[KundentypId] = Binders.string.xmap(KundentypId.apply, _.id)
  implicit val kundentypIdSetSqlBinder = setParameterBinderFactory[KundentypId](KundentypId.apply, _.id)
  implicit val aboIdSqlBinder = baseIdParameterBinderFactory[AboId](AboId.apply)
  implicit val lieferungIdSqlBinder = baseIdParameterBinderFactory[LieferungId](LieferungId.apply)
  implicit val lieferplanungIdSqlBinder = baseIdParameterBinderFactory[LieferplanungId](LieferplanungId.apply)
  implicit val lieferpositionIdSqlBinder = baseIdParameterBinderFactory[LieferpositionId](LieferpositionId.apply)
  implicit val bestellungIdSqlBinder = baseIdParameterBinderFactory[BestellungId](BestellungId.apply)
  implicit val bestellpositionIdSqlBinder = baseIdParameterBinderFactory[BestellpositionId](BestellpositionId.apply)
  implicit val korbIdSqlBinder = baseIdParameterBinderFactory[KorbId](KorbId.apply)
  implicit val produktIdSqlBinder = baseIdParameterBinderFactory[ProduktId](ProduktId.apply)
  implicit val produktekategorieIdSqlBinder = baseIdParameterBinderFactory[ProduktekategorieId](ProduktekategorieId.apply)
  implicit val baseProduktekategorieIdSqlBinder: Binders[BaseProduktekategorieId] = Binders.string.xmap(BaseProduktekategorieId.apply, _.id)
  implicit val baseProduktekategorieIdSetSqlBinder = setParameterBinderFactory[BaseProduktekategorieId](BaseProduktekategorieId.apply, _.id)
  implicit val produzentIdSqlBinder = baseIdParameterBinderFactory[ProduzentId](ProduzentId.apply)
  implicit val baseProduzentIdSqlBinder: Binders[BaseProduzentId] = Binders.string.xmap(BaseProduzentId.apply, _.id)
  implicit val baseProduzentIdSetSqlBinder = setParameterBinderFactory[BaseProduzentId](BaseProduzentId.apply, _.id)
  implicit val projektIdSqlBinder = baseIdParameterBinderFactory[ProjektId](ProjektId.apply)
  implicit val abwesenheitIdSqlBinder = baseIdParameterBinderFactory[AbwesenheitId](AbwesenheitId.apply)
  implicit val produktProduzentIdIdSqlBinder = baseIdParameterBinderFactory[ProduktProduzentId](ProduktProduzentId.apply)
  implicit val produktProduktekategorieIdIdSqlBinder = baseIdParameterBinderFactory[ProduktProduktekategorieId](ProduktProduktekategorieId.apply)
  implicit val stringIntTreeMapSqlBinder = treeMapParameterBinderFactory[String, Int](identity, _.toInt, identity, _.toString)
  val string2RolleConverter: String => Rolle = x => Rolle.apply(x).getOrElse(sys.error(s"Unknown rolle:$x"))
  implicit val rolleSqlBinder: Binders[Rolle] = Binders.string.xmap(string2RolleConverter, _.productPrefix)
  implicit val rolleMapSqlBinder = mapParameterBinderFactory[Rolle, Boolean](string2RolleConverter, _.toBoolean, _.productPrefix, _.toString)
  implicit val fristSqlBinder: Binders[Frist] = Binders.string.xmap(
    _ match {
      case fristeinheitPattern(wert, "W") => Frist(wert.toInt, Wochenfrist)
      case fristeinheitPattern(wert, "M") => Frist(wert.toInt, Monatsfrist)
    },
    { frist =>
      val einheit = frist.einheit match {
        case Wochenfrist => "W"
        case Monatsfrist => "M"
      }
      s"${frist.wert}$einheit"
    }
  )

  implicit val abotypMapping = new BaseEntitySQLSyntaxSupport[Abotyp] {
    override val tableName = "Abotyp"

    override lazy val columns = autoColumns[Abotyp]()

    def apply(rn: ResultName[Abotyp])(rs: WrappedResultSet): Abotyp = autoConstruct(rs, rn)

    def parameterMappings(entity: Abotyp): Seq[ParameterBinder] =
      parameters(Abotyp.unapply(entity).get)

    override def updateParameters(abotyp: Abotyp) = {
      super.updateParameters(abotyp) ++ Seq(
        column.name -> abotyp.name,
        column.beschreibung -> abotyp.beschreibung,
        column.lieferrhythmus -> abotyp.lieferrhythmus,
        column.aktivVon -> abotyp.aktivVon,
        column.aktivBis -> abotyp.aktivBis,
        column.laufzeit -> abotyp.laufzeit,
        column.laufzeiteinheit -> abotyp.laufzeiteinheit,
        column.kuendigungsfrist -> abotyp.kuendigungsfrist,
        column.anzahlAbwesenheiten -> abotyp.anzahlAbwesenheiten,
        column.preis -> abotyp.preis,
        column.preiseinheit -> abotyp.preiseinheit,
        column.farbCode -> abotyp.farbCode,
        column.zielpreis -> abotyp.zielpreis,
        column.anzahlAbonnenten -> abotyp.anzahlAbonnenten,
        column.letzteLieferung -> abotyp.letzteLieferung,
        column.waehrung -> abotyp.waehrung,
        column.guthabenMindestbestand -> abotyp.guthabenMindestbestand
      )
    }
  }

  implicit val customKundentypMapping = new BaseEntitySQLSyntaxSupport[CustomKundentyp] {
    override val tableName = "Kundentyp"

    override lazy val columns = autoColumns[CustomKundentyp]()

    def apply(rn: ResultName[CustomKundentyp])(rs: WrappedResultSet): CustomKundentyp =
      autoConstruct(rs, rn)

    def parameterMappings(entity: CustomKundentyp): Seq[ParameterBinder] =
      parameters(CustomKundentyp.unapply(entity).get)

    override def updateParameters(typ: CustomKundentyp) = {
      super.updateParameters(typ) ++ Seq(
        column.kundentyp -> typ.kundentyp,
        column.beschreibung -> typ.beschreibung,
        column.anzahlVerknuepfungen -> typ.anzahlVerknuepfungen
      )
    }
  }

  implicit val kundeMapping = new BaseEntitySQLSyntaxSupport[Kunde] {
    override val tableName = "Kunde"

    override lazy val columns = autoColumns[Kunde]()

    def apply(rn: ResultName[Kunde])(rs: WrappedResultSet): Kunde =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Kunde): Seq[ParameterBinder] =
      parameters(Kunde.unapply(entity).get)

    override def updateParameters(kunde: Kunde) = {
      super.updateParameters(kunde) ++ Seq(
        column.bezeichnung -> kunde.bezeichnung,
        column.strasse -> kunde.strasse,
        column.hausNummer -> kunde.hausNummer,
        column.adressZusatz -> kunde.adressZusatz,
        column.plz -> kunde.plz,
        column.ort -> kunde.ort,
        column.abweichendeLieferadresse -> kunde.abweichendeLieferadresse,
        column.bezeichnungLieferung -> kunde.bezeichnungLieferung,
        column.strasseLieferung -> kunde.strasseLieferung,
        column.hausNummerLieferung -> kunde.hausNummerLieferung,
        column.plzLieferung -> kunde.plzLieferung,
        column.ortLieferung -> kunde.ortLieferung,
        column.adressZusatzLieferung -> kunde.adressZusatzLieferung,
        column.zusatzinfoLieferung -> kunde.zusatzinfoLieferung,
        column.typen -> kunde.typen,
        column.bemerkungen -> kunde.bemerkungen,
        column.anzahlAbos -> kunde.anzahlAbos,
        column.anzahlPendenzen -> kunde.anzahlPendenzen,
        column.anzahlPersonen -> kunde.anzahlPersonen
      )
    }
  }

  implicit val personMapping = new BaseEntitySQLSyntaxSupport[Person] {
    override val tableName = "Person"

    override lazy val columns = autoColumns[Person]()

    def apply(rn: ResultName[Person])(rs: WrappedResultSet): Person =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Person): Seq[ParameterBinder] =
      parameters(Person.unapply(entity).get)

    override def updateParameters(person: Person) = {
      super.updateParameters(person) ++ Seq(
        column.kundeId -> person.kundeId,
        column.anrede -> person.anrede,
        column.name -> person.name,
        column.vorname -> person.vorname,
        column.email -> person.email,
        column.emailAlternative -> person.emailAlternative,
        column.telefonMobil -> person.telefonMobil,
        column.telefonFestnetz -> person.telefonFestnetz,
        column.bemerkungen -> person.bemerkungen,
        column.sort -> person.sort,
        column.loginAktiv -> person.loginAktiv,
        column.passwort -> person.passwort,
        column.passwortWechselErforderlich -> person.passwortWechselErforderlich,
        column.rolle -> person.rolle,
        column.letzteAnmeldung -> person.letzteAnmeldung
      )
    }
  }

  implicit val pendenzMapping = new BaseEntitySQLSyntaxSupport[Pendenz] {
    override val tableName = "Pendenz"

    override lazy val columns = autoColumns[Pendenz]()

    def apply(rn: ResultName[Pendenz])(rs: WrappedResultSet): Pendenz =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Pendenz): Seq[ParameterBinder] =
      parameters(Pendenz.unapply(entity).get)

    override def updateParameters(pendenz: Pendenz) = {
      super.updateParameters(pendenz) ++ Seq(
        column.kundeId -> pendenz.kundeId,
        column.kundeBezeichnung -> pendenz.kundeBezeichnung,
        column.datum -> pendenz.datum,
        column.bemerkung -> pendenz.bemerkung,
        column.status -> pendenz.status,
        column.generiert -> pendenz.generiert
      )
    }
  }

  implicit val lieferungMapping = new BaseEntitySQLSyntaxSupport[Lieferung] {
    override val tableName = "Lieferung"

    override lazy val columns = autoColumns[Lieferung]()

    def apply(rn: ResultName[Lieferung])(rs: WrappedResultSet): Lieferung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Lieferung): Seq[ParameterBinder] =
      parameters(Lieferung.unapply(entity).get)

    override def updateParameters(lieferung: Lieferung) = {
      super.updateParameters(lieferung) ++ Seq(
        column.abotypId -> lieferung.abotypId,
        column.abotypBeschrieb -> lieferung.abotypBeschrieb,
        column.vertriebId -> lieferung.vertriebId,
        column.vertriebBeschrieb -> lieferung.vertriebBeschrieb,
        column.status -> lieferung.status,
        column.datum -> lieferung.datum,
        column.durchschnittspreis -> lieferung.durchschnittspreis,
        column.anzahlLieferungen -> lieferung.anzahlLieferungen,
        column.anzahlKoerbeZuLiefern -> lieferung.anzahlKoerbeZuLiefern,
        column.anzahlAbwesenheiten -> lieferung.anzahlAbwesenheiten,
        column.anzahlSaldoZuTief -> lieferung.anzahlSaldoZuTief,
        column.zielpreis -> lieferung.zielpreis,
        column.preisTotal -> lieferung.preisTotal,
        column.lieferplanungId -> lieferung.lieferplanungId,
        column.lieferplanungNr -> lieferung.lieferplanungNr
      )
    }
  }

  implicit val lieferplanungMapping = new BaseEntitySQLSyntaxSupport[Lieferplanung] {
    override val tableName = "Lieferplanung"

    override lazy val columns = autoColumns[Lieferplanung]()

    def apply(rn: ResultName[Lieferplanung])(rs: WrappedResultSet): Lieferplanung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Lieferplanung): Seq[ParameterBinder] = parameters(Lieferplanung.unapply(entity).get)

    override def updateParameters(lieferplanung: Lieferplanung) = {
      super.updateParameters(lieferplanung) ++ Seq(
        column.nr -> lieferplanung.nr,
        column.bemerkungen -> lieferplanung.bemerkungen,
        column.abotypDepotTour -> lieferplanung.abotypDepotTour,
        column.status -> lieferplanung.status
      )
    }
  }

  implicit val lieferpositionMapping = new BaseEntitySQLSyntaxSupport[Lieferposition] {
    override val tableName = "Lieferposition"

    override lazy val columns = autoColumns[Lieferposition]()

    def apply(rn: ResultName[Lieferposition])(rs: WrappedResultSet): Lieferposition =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Lieferposition): Seq[ParameterBinder] = parameters(Lieferposition.unapply(entity).get)

    override def updateParameters(lieferposition: Lieferposition) = {
      super.updateParameters(lieferposition) ++ Seq(
        column.produktId -> lieferposition.produktId,
        column.produktBeschrieb -> lieferposition.produktBeschrieb,
        column.produzentId -> lieferposition.produzentId,
        column.produzentKurzzeichen -> lieferposition.produzentKurzzeichen,
        column.preisEinheit -> lieferposition.preisEinheit,
        column.einheit -> lieferposition.einheit,
        column.menge -> lieferposition.menge,
        column.preis -> lieferposition.preis,
        column.anzahl -> lieferposition.anzahl
      )
    }
  }

  implicit val bestellungMapping = new BaseEntitySQLSyntaxSupport[Bestellung] {
    override val tableName = "Bestellung"

    override lazy val columns = autoColumns[Bestellung]()

    def apply(rn: ResultName[Bestellung])(rs: WrappedResultSet): Bestellung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Bestellung): Seq[ParameterBinder] = parameters(Bestellung.unapply(entity).get)

    override def updateParameters(bestellung: Bestellung) = {
      super.updateParameters(bestellung) ++ Seq(
        column.produzentId -> bestellung.produzentId,
        column.produzentKurzzeichen -> bestellung.produzentKurzzeichen,
        column.lieferplanungId -> bestellung.lieferplanungId,
        column.lieferplanungNr -> bestellung.lieferplanungNr,
        column.status -> bestellung.status,
        column.datum -> bestellung.datum,
        column.datumAbrechnung -> bestellung.datumAbrechnung,
        column.preisTotal -> bestellung.preisTotal
      )
    }
  }

  implicit val bestellpositionMapping = new BaseEntitySQLSyntaxSupport[Bestellposition] {
    override val tableName = "Bestellposition"

    override lazy val columns = autoColumns[Bestellposition]()

    def apply(rn: ResultName[Bestellposition])(rs: WrappedResultSet): Bestellposition =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Bestellposition): Seq[ParameterBinder] = parameters(Bestellposition.unapply(entity).get)

    override def updateParameters(bestellposition: Bestellposition) = {
      super.updateParameters(bestellposition) ++ Seq(
        column.produktId -> bestellposition.produktId,
        column.produktBeschrieb -> bestellposition.produktBeschrieb,
        column.preisEinheit -> bestellposition.preisEinheit,
        column.einheit -> bestellposition.einheit,
        column.menge -> bestellposition.menge,
        column.preis -> bestellposition.preis,
        column.anzahl -> bestellposition.anzahl
      )
    }
  }

  implicit val tourMapping = new BaseEntitySQLSyntaxSupport[Tour] {
    override val tableName = "Tour"

    override lazy val columns = autoColumns[Tour]()

    def apply(rn: ResultName[Tour])(rs: WrappedResultSet): Tour =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Tour): Seq[ParameterBinder] = parameters(Tour.unapply(entity).get)

    override def updateParameters(tour: Tour) = {
      super.updateParameters(tour) ++ Seq(
        column.name -> tour.name,
        column.beschreibung -> tour.beschreibung
      )
    }
  }

  implicit val depotMapping = new BaseEntitySQLSyntaxSupport[Depot] {
    override val tableName = "Depot"

    override lazy val columns = autoColumns[Depot]()

    def apply(rn: ResultName[Depot])(rs: WrappedResultSet): Depot =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Depot): Seq[ParameterBinder] = parameters(Depot.unapply(entity).get)

    override def updateParameters(depot: Depot) = {
      super.updateParameters(depot) ++ Seq(
        column.name -> depot.name,
        column.kurzzeichen -> depot.kurzzeichen,
        column.apName -> depot.apName,
        column.apVorname -> depot.apVorname,
        column.apTelefon -> depot.apTelefon,
        column.apEmail -> depot.apEmail,
        column.vName -> depot.vName,
        column.vVorname -> depot.vVorname,
        column.vTelefon -> depot.vTelefon,
        column.vEmail -> depot.vEmail,
        column.strasse -> depot.strasse,
        column.hausNummer -> depot.hausNummer,
        column.plz -> depot.plz,
        column.ort -> depot.ort,
        column.aktiv -> depot.aktiv,
        column.oeffnungszeiten -> depot.oeffnungszeiten,
        column.farbCode -> depot.farbCode,
        column.iban -> depot.iban,
        column.bank -> depot.bank,
        column.beschreibung -> depot.beschreibung,
        column.anzahlAbonnenten -> depot.anzahlAbonnenten,
        column.anzahlAbonnentenMax -> depot.anzahlAbonnentenMax
      )
    }
  }

  implicit val vertriebMapping = new BaseEntitySQLSyntaxSupport[Vertrieb] {
    override val tableName = "Vertrieb"

    override lazy val columns = autoColumns[Vertrieb]()

    def apply(rn: ResultName[Vertrieb])(rs: WrappedResultSet): Vertrieb =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Vertrieb): Seq[ParameterBinder] = parameters(Vertrieb.unapply(entity).get)

    override def updateParameters(vertrieb: Vertrieb) = {
      super.updateParameters(vertrieb) ++ Seq(
        column.abotypId -> vertrieb.abotypId,
        column.liefertag -> vertrieb.liefertag,
        column.beschrieb -> vertrieb.beschrieb,
        column.anzahlAbos -> vertrieb.anzahlAbos
      )
    }
  }

  trait LieferungMapping[E <: Vertriebsart] extends BaseEntitySQLSyntaxSupport[E] {
    override def updateParameters(lieferung: E) = {
      super.updateParameters(lieferung) ++ Seq(
        column.vertriebId -> lieferung.vertriebId,
        column.anzahlAbos -> lieferung.anzahlAbos
      )
    }
  }

  implicit val heimlieferungMapping = new LieferungMapping[Heimlieferung] {
    override val tableName = "Heimlieferung"

    override lazy val columns = autoColumns[Heimlieferung]()

    def apply(rn: ResultName[Heimlieferung])(rs: WrappedResultSet): Heimlieferung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Heimlieferung): Seq[ParameterBinder] = parameters(Heimlieferung.unapply(entity).get)

    override def updateParameters(lieferung: Heimlieferung) = {
      super.updateParameters(lieferung) ++ Seq(
        column.tourId -> lieferung.tourId
      )
    }
  }

  implicit val depotlieferungMapping = new LieferungMapping[Depotlieferung] {
    override val tableName = "Depotlieferung"

    override lazy val columns = autoColumns[Depotlieferung]()

    def apply(rn: ResultName[Depotlieferung])(rs: WrappedResultSet): Depotlieferung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Depotlieferung): Seq[ParameterBinder] =
      parameters(Depotlieferung.unapply(entity).get)

    override def updateParameters(lieferung: Depotlieferung) = {
      super.updateParameters(lieferung) ++ Seq(
        column.depotId -> lieferung.depotId
      )
    }
  }

  implicit val postlieferungMapping = new LieferungMapping[Postlieferung] {
    override val tableName = "Postlieferung"

    override lazy val columns = autoColumns[Postlieferung]()

    def apply(rn: ResultName[Postlieferung])(rs: WrappedResultSet): Postlieferung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Postlieferung): Seq[ParameterBinder] = parameters(Postlieferung.unapply(entity).get)

    override def updateParameters(lieferung: Postlieferung) = {
      super.updateParameters(lieferung)
    }
  }

  trait BaseAboMapping[A <: Abo] extends BaseEntitySQLSyntaxSupport[A] {
    override def updateParameters(abo: A) = {
      super.updateParameters(abo) ++ Seq(
        column.kundeId -> abo.kundeId,
        column.kunde -> abo.kunde,
        column.vertriebId -> abo.vertriebId,
        column.vertriebsartId -> abo.vertriebsartId,
        column.abotypId -> abo.abotypId,
        column.abotypName -> abo.abotypName,
        column.start -> abo.start,
        column.ende -> abo.ende,
        column.guthabenVertraglich -> abo.guthabenVertraglich,
        column.guthaben -> abo.guthaben,
        column.guthabenInRechnung -> abo.guthabenInRechnung,
        column.letzteLieferung -> abo.letzteLieferung,
        column.anzahlAbwesenheiten -> abo.anzahlAbwesenheiten,
        column.anzahlLieferungen -> abo.anzahlLieferungen
      )
    }
  }

  implicit val depotlieferungAboMapping = new BaseAboMapping[DepotlieferungAbo] {
    override val tableName = "DepotlieferungAbo"

    override lazy val columns = autoColumns[DepotlieferungAbo]()

    def apply(rn: ResultName[DepotlieferungAbo])(rs: WrappedResultSet): DepotlieferungAbo = autoConstruct(rs, rn)

    def parameterMappings(entity: DepotlieferungAbo): Seq[ParameterBinder] = parameters(DepotlieferungAbo.unapply(entity).get)

    override def updateParameters(depotlieferungAbo: DepotlieferungAbo) = {
      super.updateParameters(depotlieferungAbo) ++ Seq(
        column.depotId -> depotlieferungAbo.depotId,
        column.depotName -> depotlieferungAbo.depotName
      )
    }
  }

  implicit val heimlieferungAboMapping = new BaseAboMapping[HeimlieferungAbo] {
    override val tableName = "HeimlieferungAbo"

    override lazy val columns = autoColumns[HeimlieferungAbo]()

    def apply(rn: ResultName[HeimlieferungAbo])(rs: WrappedResultSet): HeimlieferungAbo =
      autoConstruct(rs, rn)

    def parameterMappings(entity: HeimlieferungAbo): Seq[ParameterBinder] = parameters(HeimlieferungAbo.unapply(entity).get)

    override def updateParameters(heimlieferungAbo: HeimlieferungAbo) = {
      super.updateParameters(heimlieferungAbo) ++ Seq(
        column.tourId -> heimlieferungAbo.tourId,
        column.tourName -> heimlieferungAbo.tourName
      )
    }
  }

  implicit val postlieferungAboMapping = new BaseAboMapping[PostlieferungAbo] {
    override val tableName = "PostlieferungAbo"

    override lazy val columns = autoColumns[PostlieferungAbo]()

    def apply(rn: ResultName[PostlieferungAbo])(rs: WrappedResultSet): PostlieferungAbo =
      autoConstruct(rs, rn)

    def parameterMappings(entity: PostlieferungAbo): Seq[ParameterBinder] = parameters(PostlieferungAbo.unapply(entity).get)

    override def updateParameters(postlieferungAbo: PostlieferungAbo) = {
      super.updateParameters(postlieferungAbo)
    }
  }

  implicit val produktMapping = new BaseEntitySQLSyntaxSupport[Produkt] {
    override val tableName = "Produkt"

    override lazy val columns = autoColumns[Produkt]()

    def apply(rn: ResultName[Produkt])(rs: WrappedResultSet): Produkt =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Produkt): Seq[ParameterBinder] = parameters(Produkt.unapply(entity).get)

    override def updateParameters(produkt: Produkt) = {
      super.updateParameters(produkt) ++ Seq(
        column.name -> produkt.name,
        column.verfuegbarVon -> produkt.verfuegbarVon,
        column.verfuegbarBis -> produkt.verfuegbarBis,
        column.kategorien -> produkt.kategorien,
        column.standardmenge -> produkt.standardmenge,
        column.einheit -> produkt.einheit,
        column.preis -> produkt.preis,
        column.produzenten -> produkt.produzenten
      )
    }
  }

  implicit val produzentMapping = new BaseEntitySQLSyntaxSupport[Produzent] {
    override val tableName = "Produzent"

    override lazy val columns = autoColumns[Produzent]()

    def apply(rn: ResultName[Produzent])(rs: WrappedResultSet): Produzent =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Produzent): Seq[ParameterBinder] = parameters(Produzent.unapply(entity).get)

    override def updateParameters(produzent: Produzent) = {
      super.updateParameters(produzent) ++ Seq(
        column.name -> produzent.name,
        column.vorname -> produzent.vorname,
        column.kurzzeichen -> produzent.kurzzeichen,
        column.strasse -> produzent.strasse,
        column.hausNummer -> produzent.hausNummer,
        column.adressZusatz -> produzent.adressZusatz,
        column.plz -> produzent.plz,
        column.ort -> produzent.ort,
        column.bemerkungen -> produzent.bemerkungen,
        column.email -> produzent.email,
        column.telefonMobil -> produzent.telefonMobil,
        column.telefonFestnetz -> produzent.telefonFestnetz,
        column.iban -> produzent.iban,
        column.bank -> produzent.bank,
        column.mwst -> produzent.mwst,
        column.mwstSatz -> produzent.mwstSatz,
        column.mwstNr -> produzent.mwstNr,
        column.aktiv -> produzent.aktiv
      )
    }
  }

  implicit val produktekategorieMapping = new BaseEntitySQLSyntaxSupport[Produktekategorie] {
    override val tableName = "Produktekategorie"

    override lazy val columns = autoColumns[Produktekategorie]()

    def apply(rn: ResultName[Produktekategorie])(rs: WrappedResultSet): Produktekategorie =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Produktekategorie): Seq[ParameterBinder] = parameters(Produktekategorie.unapply(entity).get)

    override def updateParameters(produktekategorie: Produktekategorie) = {
      super.updateParameters(produktekategorie) ++ Seq(
        column.beschreibung -> produktekategorie.beschreibung
      )
    }
  }

  implicit val projektMapping = new BaseEntitySQLSyntaxSupport[Projekt] {
    override val tableName = "Projekt"

    override lazy val columns = autoColumns[Projekt]()

    def apply(rn: ResultName[Projekt])(rs: WrappedResultSet): Projekt =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Projekt): Seq[ParameterBinder] = parameters(Projekt.unapply(entity).get)

    override def updateParameters(projekt: Projekt) = {
      super.updateParameters(projekt) ++ Seq(
        column.bezeichnung -> projekt.bezeichnung,
        column.strasse -> projekt.strasse,
        column.hausNummer -> projekt.hausNummer,
        column.adressZusatz -> projekt.adressZusatz,
        column.plz -> projekt.plz,
        column.ort -> projekt.ort,
        column.preiseSichtbar -> projekt.preiseSichtbar,
        column.preiseEditierbar -> projekt.preiseEditierbar,
        column.emailErforderlich -> projekt.emailErforderlich,
        column.waehrung -> projekt.waehrung,
        column.geschaeftsjahrMonat -> projekt.geschaeftsjahrMonat,
        column.geschaeftsjahrTag -> projekt.geschaeftsjahrTag,
        column.twoFactorAuthentication -> projekt.twoFactorAuthentication
      )
    }
  }

  implicit val produktProduzentMapping = new BaseEntitySQLSyntaxSupport[ProduktProduzent] {
    override val tableName = "ProduktProduzent"

    override lazy val columns = autoColumns[ProduktProduzent]()

    def apply(rn: ResultName[ProduktProduzent])(rs: WrappedResultSet): ProduktProduzent =
      autoConstruct(rs, rn)

    def parameterMappings(entity: ProduktProduzent): Seq[ParameterBinder] = parameters(ProduktProduzent.unapply(entity).get)

    override def updateParameters(projekt: ProduktProduzent) = {
      super.updateParameters(projekt) ++ Seq(
        column.produktId -> projekt.produktId,
        column.produzentId -> projekt.produzentId
      )
    }
  }

  implicit val produktProduktekategorieMapping = new BaseEntitySQLSyntaxSupport[ProduktProduktekategorie] {
    override val tableName = "ProduktProduktekategorie"

    override lazy val columns = autoColumns[ProduktProduktekategorie]()

    def apply(rn: ResultName[ProduktProduktekategorie])(rs: WrappedResultSet): ProduktProduktekategorie =
      autoConstruct(rs, rn)

    def parameterMappings(entity: ProduktProduktekategorie): Seq[ParameterBinder] = parameters(ProduktProduktekategorie.unapply(entity).get)

    override def updateParameters(produktkat: ProduktProduktekategorie) = {
      super.updateParameters(produktkat) ++ Seq(
        column.produktId -> produktkat.produktId,
        column.produktekategorieId -> produktkat.produktekategorieId
      )
    }
  }

  implicit val abwesenheitMapping = new BaseEntitySQLSyntaxSupport[Abwesenheit] {
    override val tableName = "Abwesenheit"

    override lazy val columns = autoColumns[Abwesenheit]()

    def apply(rn: ResultName[Abwesenheit])(rs: WrappedResultSet): Abwesenheit =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Abwesenheit): Seq[ParameterBinder] = parameters(Abwesenheit.unapply(entity).get)

    override def updateParameters(entity: Abwesenheit) = {
      super.updateParameters(entity) ++ Seq(
        column.aboId -> entity.aboId,
        column.lieferungId -> entity.lieferungId,
        column.datum -> entity.datum,
        column.bemerkung -> entity.bemerkung
      )
    }
  }

  implicit val korbMapping = new BaseEntitySQLSyntaxSupport[Korb] {
    override val tableName = "Korb"

    override lazy val columns = autoColumns[Korb]()

    def apply(rn: ResultName[Korb])(rs: WrappedResultSet): Korb =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Korb): Seq[ParameterBinder] = parameters(Korb.unapply(entity).get)

    override def updateParameters(entity: Korb) = {
      super.updateParameters(entity) ++ Seq(
        column.lieferungId -> entity.lieferungId,
        column.aboId -> entity.aboId,
        column.status -> entity.status
      )
    }
  }
}
