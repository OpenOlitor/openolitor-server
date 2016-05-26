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
package ch.openolitor.stammdaten.repositories

import ch.openolitor.core.models._
import scalikejdbc._
import sqls.{ distinct, count }
import ch.openolitor.core.db._
import ch.openolitor.core.repositories._
import ch.openolitor.core.repositories.BaseRepository._
import ch.openolitor.stammdaten.models._
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.buchhaltung.models._
import ch.openolitor.core.Macros._
import ch.openolitor.util.DateTimeUtil._
import org.joda.time.DateTime
import ch.openolitor.stammdaten.StammdatenDBMappings

trait StammdatenRepositoryQueries extends LazyLogging with StammdatenDBMappings {

  lazy val aboTyp = abotypMapping.syntax("atyp")
  lazy val person = personMapping.syntax("pers")
  lazy val lieferplanung = lieferplanungMapping.syntax("lieferplanung")
  lazy val lieferung = lieferungMapping.syntax("lieferung")
  lazy val lieferposition = lieferpositionMapping.syntax("lieferposition")
  lazy val bestellung = bestellungMapping.syntax("bestellung")
  lazy val bestellposition = bestellpositionMapping.syntax("bestellposition")
  lazy val kunde = kundeMapping.syntax("kunde")
  lazy val pendenz = pendenzMapping.syntax("pendenz")
  lazy val kundentyp = customKundentypMapping.syntax("kundentyp")
  lazy val postlieferung = postlieferungMapping.syntax("postlieferung")
  lazy val depotlieferung = depotlieferungMapping.syntax("depotlieferung")
  lazy val heimlieferung = heimlieferungMapping.syntax("heimlieferung")
  lazy val depot = depotMapping.syntax("depot")
  lazy val tour = tourMapping.syntax("tour")
  lazy val depotlieferungAbo = depotlieferungAboMapping.syntax("depotlieferungAbo")
  lazy val heimlieferungAbo = heimlieferungAboMapping.syntax("heimlieferungAbo")
  lazy val postlieferungAbo = postlieferungAboMapping.syntax("postlieferungAbo")
  lazy val produkt = produktMapping.syntax("produkt")
  lazy val produktekategorie = produktekategorieMapping.syntax("produktekategorie")
  lazy val produzent = produzentMapping.syntax("produzent")
  lazy val projekt = projektMapping.syntax("projekt")
  lazy val produktProduzent = produktProduzentMapping.syntax("produktProduzent")
  lazy val produktProduktekategorie = produktProduktekategorieMapping.syntax("produktProduktekategorie")
  lazy val abwesenheit = abwesenheitMapping.syntax("abwesenheit")
  lazy val korb = korbMapping.syntax("korb")
  lazy val vertrieb = vertriebMapping.syntax("vertrieb")

  lazy val lieferpositionShort = lieferpositionMapping.syntax
  lazy val korbShort = korbMapping.syntax

  protected def getAbotypenQuery = {
    withSQL {
      select
        .from(abotypMapping as aboTyp)
        .orderBy(aboTyp.name)
    }.map(abotypMapping(aboTyp)).list
  }

  protected def getKundenQuery = {
    withSQL {
      select
        .from(kundeMapping as kunde)
        .orderBy(kunde.bezeichnung)
    }.map(kundeMapping(kunde)).list
  }

  protected def getCustomKundentypenQuery = {
    withSQL {
      select
        .from(customKundentypMapping as kundentyp)
        .orderBy(kundentyp.id)
    }.map(customKundentypMapping(kundentyp)).list
  }

  protected def getKundeDetailQuery(id: KundeId) = {
    withSQL {
      select
        .from(kundeMapping as kunde)
        .leftJoin(depotlieferungAboMapping as depotlieferungAbo).on(kunde.id, depotlieferungAbo.kundeId)
        .leftJoin(heimlieferungAboMapping as heimlieferungAbo).on(kunde.id, heimlieferungAbo.kundeId)
        .leftJoin(postlieferungAboMapping as postlieferungAbo).on(kunde.id, postlieferungAbo.kundeId)
        .leftJoin(personMapping as person).on(kunde.id, person.kundeId)
        .leftJoin(pendenzMapping as pendenz).on(kunde.id, pendenz.kundeId)
        .where.eq(kunde.id, id)
        .orderBy(person.sort)
    }.one(kundeMapping(kunde))
      .toManies(
        rs => postlieferungAboMapping.opt(postlieferungAbo)(rs),
        rs => heimlieferungAboMapping.opt(heimlieferungAbo)(rs),
        rs => depotlieferungAboMapping.opt(depotlieferungAbo)(rs),
        rs => personMapping.opt(person)(rs),
        rs => pendenzMapping.opt(pendenz)(rs)
      )
      .map({ (kunde, pl, hl, dl, personen, pendenzen) =>
        val abos = pl ++ hl ++ dl
        val personenWihoutPwd = personen.map(p => copyTo[Person, PersonDetail](p))

        copyTo[Kunde, KundeDetail](kunde, "abos" -> abos, "pendenzen" -> pendenzen, "ansprechpersonen" -> personenWihoutPwd)
      }).single
  }

  protected def getPersonenQuery(kundeId: KundeId) = {
    withSQL {
      select
        .from(personMapping as person)
        .where.eq(person.kundeId, kundeId)
        .orderBy(person.sort)
    }.map(personMapping(person)).list
  }

  protected def getAbotypDetailQuery(id: AbotypId) = {
    withSQL {
      select
        .from(abotypMapping as aboTyp)
        .where.eq(aboTyp.id, id)
    }.map(abotypMapping(aboTyp)).single
  }

  protected def getDepotlieferungQuery(vertriebId: VertriebId) = {
    withSQL {
      select
        .from(depotlieferungMapping as depotlieferung)
        .leftJoin(depotMapping as depot).on(depotlieferung.depotId, depot.id)
        .where.eq(depotlieferung.vertriebId, vertriebId)
    }.one(depotlieferungMapping(depotlieferung)).toOne(depotMapping.opt(depot)).map { (vertriebsart, depot) =>
      val depotSummary = DepotSummary(depot.head.id, depot.head.name)
      copyTo[Depotlieferung, DepotlieferungDetail](vertriebsart, "depot" -> depotSummary)
    }.list
  }

  protected def getHeimlieferungQuery(vertriebId: VertriebId) = {
    withSQL {
      select
        .from(heimlieferungMapping as heimlieferung)
        .leftJoin(tourMapping as tour).on(heimlieferung.tourId, tour.id)
        .where.eq(heimlieferung.vertriebId, vertriebId)
    }.one(heimlieferungMapping(heimlieferung)).toOne(tourMapping.opt(tour)).map { (vertriebsart, tour) =>
      copyTo[Heimlieferung, HeimlieferungDetail](vertriebsart, "tour" -> tour.get)
    }.list
  }

  protected def getPostlieferungQuery(vertriebId: VertriebId) = {
    withSQL {
      select
        .from(postlieferungMapping as postlieferung)
        .where.eq(postlieferung.vertriebId, vertriebId)
    }.map { rs =>
      val pl = postlieferungMapping(postlieferung)(rs)
      copyTo[Postlieferung, PostlieferungDetail](pl)
    }.list
  }

  protected def getDepotlieferungQuery(vertriebsartId: VertriebsartId) = {
    withSQL {
      select
        .from(depotlieferungMapping as depotlieferung)
        .leftJoin(depotMapping as depot).on(depotlieferung.depotId, depot.id)
        .where.eq(depotlieferung.id, vertriebsartId)
    }.one(depotlieferungMapping(depotlieferung)).toOne(depotMapping.opt(depot)).map { (vertriebsart, depot) =>
      val depotSummary = DepotSummary(depot.head.id, depot.head.name)
      copyTo[Depotlieferung, DepotlieferungDetail](vertriebsart, "depot" -> depotSummary)
    }.single
  }

  protected def getHeimlieferungQuery(vertriebsartId: VertriebsartId) = {
    withSQL {
      select
        .from(heimlieferungMapping as heimlieferung)
        .leftJoin(tourMapping as tour).on(heimlieferung.tourId, tour.id)
        .where.eq(heimlieferung.id, vertriebsartId)
    }.one(heimlieferungMapping(heimlieferung)).toOne(tourMapping.opt(tour)).map { (vertriebsart, tour) =>
      copyTo[Heimlieferung, HeimlieferungDetail](vertriebsart, "tour" -> tour.get)
    }.single
  }

  protected def getPostlieferungQuery(vertriebsartId: VertriebsartId) = {
    withSQL {
      select
        .from(postlieferungMapping as postlieferung)
        .where.eq(postlieferung.id, vertriebsartId)
    }.map { rs =>
      val pl = postlieferungMapping(postlieferung)(rs)
      copyTo[Postlieferung, PostlieferungDetail](pl)
    }.single
  }

  protected def getUngeplanteLieferungenQuery(abotypId: AbotypId, vertriebId: VertriebId) = {
    withSQL {
      select
        .from(lieferungMapping as lieferung)
        .leftJoin(vertriebMapping as vertrieb).on(lieferung.vertriebId, vertrieb.id)
        .where.eq(vertrieb.abotypId, abotypId).and.eq(lieferung.vertriebId, vertriebId).and.isNull(lieferung.lieferplanungId)
        .orderBy(lieferung.datum)
    }.map(lieferungMapping(lieferung)).list
  }

  protected def getUngeplanteLieferungenQuery(abotypId: AbotypId) = {
    withSQL {
      select
        .from(lieferungMapping as lieferung)
        .where.eq(lieferung.abotypId, abotypId).and.isNull(lieferung.lieferplanungId)
        .orderBy(lieferung.datum)
    }.map(lieferungMapping(lieferung)).list
  }

  protected def getDepotsQuery = {
    withSQL {
      select
        .from(depotMapping as depot)
        .orderBy(depot.name)
    }.map(depotMapping(depot)).list
  }

  protected def getDepotDetailQuery(id: DepotId) = {
    withSQL {
      select
        .from(depotMapping as depot)
        .where.eq(depot.id, id)
    }.map(depotMapping(depot)).single
  }

  protected def getDepotlieferungAbosQuery = {
    withSQL {
      select
        .from(depotlieferungAboMapping as depotlieferungAbo)
    }.map(depotlieferungAboMapping(depotlieferungAbo)).list
  }

  protected def getHeimlieferungAbosQuery = {
    withSQL {
      select
        .from(heimlieferungAboMapping as heimlieferungAbo)
    }.map(heimlieferungAboMapping(heimlieferungAbo)).list
  }

  protected def getPostlieferungAbosQuery = {
    withSQL {
      select
        .from(postlieferungAboMapping as postlieferungAbo)
    }.map(postlieferungAboMapping(postlieferungAbo)).list
  }

  protected def getDepotlieferungAboQuery(id: AboId) = {
    withSQL {
      select
        .from(depotlieferungAboMapping as depotlieferungAbo)
        .leftJoin(abwesenheitMapping as abwesenheit).on(depotlieferungAbo.id, abwesenheit.aboId)
        .leftJoin(lieferungMapping as lieferung).on(depotlieferungAbo.abotypId, lieferung.abotypId)
        .leftJoin(lieferplanungMapping as lieferplanung).on(lieferung.lieferplanungId, lieferplanung.id)
        .leftJoin(abotypMapping as aboTyp).on(depotlieferungAbo.abotypId, aboTyp.id)
        .leftJoin(vertriebMapping as vertrieb).on(vertrieb.abotypId, aboTyp.id)
        .where.eq(depotlieferungAbo.id, id)
        .and.isNull(lieferung.lieferplanungId)
    }
      .one(depotlieferungAboMapping(depotlieferungAbo))
      .toManies(
        rs => abwesenheitMapping.opt(abwesenheit)(rs),
        rs => lieferungMapping.opt(lieferung)(rs),
        rs => abotypMapping.opt(aboTyp)(rs),
        rs => vertriebMapping.opt(vertrieb)(rs)
      )
      .map((abo, abw, lieferungen, aboTyp, vertriebe) => {
        val sortedAbw = abw.sortBy(_.datum)
        copyTo[DepotlieferungAbo, DepotlieferungAboDetail](abo, "abwesenheiten" -> sortedAbw, "lieferdaten" -> lieferungen,
          "abotyp" -> aboTyp.headOption, "vertrieb" -> vertriebe.headOption)
      }).single
  }

  protected def getHeimlieferungAboQuery(id: AboId) = {
    withSQL {
      select
        .from(heimlieferungAboMapping as heimlieferungAbo)
        .leftJoin(abwesenheitMapping as abwesenheit).on(heimlieferungAbo.id, abwesenheit.aboId)
        .leftJoin(lieferungMapping as lieferung).on(heimlieferungAbo.abotypId, lieferung.abotypId)
        .leftJoin(abotypMapping as aboTyp).on(heimlieferungAbo.abotypId, aboTyp.id)
        .leftJoin(vertriebMapping as vertrieb).on(vertrieb.abotypId, aboTyp.id)
        .where.eq(heimlieferungAbo.id, id).and.isNull(lieferung.lieferplanungId)
    }.one(heimlieferungAboMapping(heimlieferungAbo))
      .toManies(
        rs => abwesenheitMapping.opt(abwesenheit)(rs),
        rs => lieferungMapping.opt(lieferung)(rs),
        rs => abotypMapping.opt(aboTyp)(rs),
        rs => vertriebMapping.opt(vertrieb)(rs)
      )
      .map((abo, abw, lieferungen, aboTyp, vertriebe) => {
        val sortedAbw = abw.sortBy(_.datum)
        copyTo[HeimlieferungAbo, HeimlieferungAboDetail](abo, "abwesenheiten" -> sortedAbw, "lieferdaten" -> lieferungen,
          "abotyp" -> aboTyp.headOption, "vertrieb" -> vertriebe.headOption)
      }).single
  }

  protected def getPostlieferungAboQuery(id: AboId) = {
    withSQL {
      select
        .from(postlieferungAboMapping as postlieferungAbo)
        .leftJoin(abwesenheitMapping as abwesenheit).on(postlieferungAbo.id, abwesenheit.aboId)
        .leftJoin(lieferungMapping as lieferung).on(postlieferungAbo.abotypId, lieferung.abotypId)
        .leftJoin(abotypMapping as aboTyp).on(postlieferungAbo.abotypId, aboTyp.id)
        .leftJoin(vertriebMapping as vertrieb).on(vertrieb.abotypId, aboTyp.id)
        .where.eq(postlieferungAbo.id, id).and.isNull(lieferung.lieferplanungId)
    }.one(postlieferungAboMapping(postlieferungAbo))
      .toManies(
        rs => abwesenheitMapping.opt(abwesenheit)(rs),
        rs => lieferungMapping.opt(lieferung)(rs),
        rs => abotypMapping.opt(aboTyp)(rs),
        rs => vertriebMapping.opt(vertrieb)(rs)
      )
      .map((abo, abw, lieferungen, aboTyp, vertriebe) => {
        val sortedAbw = abw.sortBy(_.datum)
        copyTo[PostlieferungAbo, PostlieferungAboDetail](abo, "abwesenheiten" -> sortedAbw, "lieferdaten" -> lieferungen,
          "abotyp" -> aboTyp.headOption, "vertrieb" -> vertriebe.headOption)
      }).single
  }

  protected def countAbwesendQuery(lieferungId: LieferungId, aboId: AboId) = {
    withSQL {
      select(count(distinct(abwesenheit.id)))
        .from(abwesenheitMapping as abwesenheit)
        .where.eq(abwesenheit.lieferungId, lieferungId).and.eq(abwesenheit.aboId, aboId)
        .limit(1)
    }.map(_.int(1)).single
  }

  protected def getAktiveDepotlieferungAbosQuery(vertriebId: VertriebId, lieferdatum: DateTime) = {
    withSQL {
      select
        .from(depotlieferungAboMapping as depotlieferungAbo)
        .where.eq(depotlieferungAbo.vertriebId, vertriebId).and.withRoundBracket { _.isNull(depotlieferungAbo.ende).or.gt(depotlieferungAbo.ende, lieferdatum) }
    }.map(depotlieferungAboMapping(depotlieferungAbo)).list
  }

  protected def getAktiveHeimlieferungAbosQuery(vertriebId: VertriebId, lieferdatum: DateTime) = {
    withSQL {
      select
        .from(heimlieferungAboMapping as heimlieferungAbo)
        .where.eq(heimlieferungAbo.vertriebId, vertriebId).and.withRoundBracket { _.isNull(heimlieferungAbo.ende).or.gt(heimlieferungAbo.ende, lieferdatum) }
    }.map(heimlieferungAboMapping(heimlieferungAbo)).list
  }

  protected def getAktivePostlieferungAbosQuery(vertriebId: VertriebId, lieferdatum: DateTime) = {
    withSQL {
      select
        .from(postlieferungAboMapping as postlieferungAbo)
        .where.eq(postlieferungAbo.vertriebId, vertriebId).and.withRoundBracket { _.isNull(postlieferungAbo.ende).or.gt(postlieferungAbo.ende, lieferdatum) }
    }.map(postlieferungAboMapping(postlieferungAbo)).list
  }

  protected def getPendenzenQuery = {
    withSQL {
      select
        .from(pendenzMapping as pendenz)
        .orderBy(pendenz.datum)
    }.map(pendenzMapping(pendenz)).list
  }

  protected def getPendenzenQuery(id: KundeId) = {
    withSQL {
      select
        .from(pendenzMapping as pendenz)
        .where.eq(pendenz.kundeId, id)
    }.map(pendenzMapping(pendenz)).list
  }

  protected def getPendenzDetailQuery(id: PendenzId) = {
    withSQL {
      select
        .from(pendenzMapping as pendenz)
        .where.eq(pendenz.id, id)
    }.map(pendenzMapping(pendenz)).single
  }

  protected def getProdukteQuery = {
    withSQL {
      select
        .from(produktMapping as produkt)
    }.map(produktMapping(produkt)).list
  }

  protected def getProduktekategorienQuery = {
    withSQL {
      select
        .from(produktekategorieMapping as produktekategorie)
    }.map(produktekategorieMapping(produktekategorie)).list
  }

  protected def getProduzentenQuery = {
    withSQL {
      select
        .from(produzentMapping as produzent)
    }.map(produzentMapping(produzent)).list
  }

  protected def getProduzentDetailQuery(id: ProduzentId) = {
    withSQL {
      select
        .from(produzentMapping as produzent)
        .where.eq(produzent.id, id)
    }.map(produzentMapping(produzent)).single
  }

  protected def getTourenQuery = {
    withSQL {
      select
        .from(tourMapping as tour)
    }.map(tourMapping(tour)).list
  }

  protected def getProjektQuery = {
    withSQL {
      select
        .from(projektMapping as projekt)
    }.map(projektMapping(projekt)).single
  }

  protected def getProduktProduzentenQuery(id: ProduktId) = {
    withSQL {
      select
        .from(produktProduzentMapping as produktProduzent)
        .where.eq(produktProduzent.produktId, id)
    }.map(produktProduzentMapping(produktProduzent)).list
  }

  protected def getProduktProduktekategorienQuery(id: ProduktId) = {
    withSQL {
      select
        .from(produktProduktekategorieMapping as produktProduktekategorie)
        .where.eq(produktProduktekategorie.produktId, id)
    }.map(produktProduktekategorieMapping(produktProduktekategorie)).list
  }

  protected def getProduzentDetailByKurzzeichenQuery(kurzzeichen: String) = {
    withSQL {
      select
        .from(produzentMapping as produzent)
        .where.eq(produzent.kurzzeichen, kurzzeichen)
    }.map(produzentMapping(produzent)).single
  }

  protected def getProduktekategorieByBezeichnungQuery(bezeichnung: String) = {
    withSQL {
      select
        .from(produktekategorieMapping as produktekategorie)
        .where.eq(produktekategorie.beschreibung, bezeichnung)
    }.map(produktekategorieMapping(produktekategorie)).single
  }

  protected def getProdukteByProduktekategorieBezeichnungQuery(bezeichnung: String) = {
    withSQL {
      select
        .from(produktMapping as produkt)
        .where.like(produkt.kategorien, '%' + bezeichnung + '%')
    }.map(produktMapping(produkt)).list
  }

  protected def getLieferplanungenQuery = {
    withSQL {
      select
        .from(lieferplanungMapping as lieferplanung)
    }.map(lieferplanungMapping(lieferplanung)).list
  }

  protected def getLatestLieferplanungQuery = {
    withSQL {
      select
        .from(lieferplanungMapping as lieferplanung)
        .orderBy(lieferplanung.nr).desc
        .limit(1)
    }.map(lieferplanungMapping(lieferplanung)).single
  }

  protected def getLieferplanungQuery(id: LieferplanungId) = {
    withSQL {
      select
        .from(lieferplanungMapping as lieferplanung)
        .where.eq(lieferplanung.id, id)
    }.map(lieferplanungMapping(lieferplanung)).single
  }

  protected def getLieferungenNextQuery = {
    sql"""
        SELECT
          ${lieferung.result.*}
        FROM
          ${lieferungMapping as lieferung}
        INNER JOIN
		      (SELECT ${lieferung.abotypId} AS abotypId, MIN(${lieferung.datum}) AS MinDateTime
		       FROM ${lieferungMapping as lieferung}
		       INNER JOIN ${abotypMapping as aboTyp}
		       ON ${lieferung.abotypId} = ${aboTyp.id}
		       WHERE ${aboTyp.wirdGeplant} = true
		       AND ${lieferung.lieferplanungId} IS NULL
		       GROUP BY ${lieferung.abotypId}) groupedLieferung
		    ON ${lieferung.abotypId} = groupedLieferung.abotypId 
		    AND ${lieferung.datum} = groupedLieferung.MinDateTime
      """.map(lieferungMapping(lieferung)).list
  }

  protected def getLastGeplanteLieferungQuery(abotypId: AbotypId) = {
    withSQL {
      select
        .from(lieferungMapping as lieferung)
        .where.eq(lieferung.abotypId, abotypId).and.not.eq(lieferung.status, Ungeplant.asInstanceOf[LieferungStatus])
        .orderBy(lieferung.datum).desc
        .limit(1)
    }.map(lieferungMapping(lieferung)).single
  }

  protected def getLieferungenDetailsQuery(id: LieferplanungId) = {
    withSQL {
      select
        .from(lieferungMapping as lieferung)
        .leftJoin(abotypMapping as aboTyp).on(lieferung.abotypId, aboTyp.id)
        .leftJoin(lieferpositionMapping as lieferposition).on(lieferposition.lieferungId, lieferung.id)
        .where.eq(lieferung.lieferplanungId, id)
    }.one(lieferungMapping(lieferung))
      .toManies(
        rs => abotypMapping.opt(aboTyp)(rs),
        rs => lieferpositionMapping.opt(lieferposition)(rs)
      )
      .map { (lieferung, abotyp, positionen) =>
        copyTo[Lieferung, LieferungDetail](lieferung, "abotyp" -> abotyp.headOption, "lieferpositionen" -> positionen)
      }.list
  }

  protected def getLieferungenQuery(id: LieferplanungId) = {
    withSQL {
      select
        .from(lieferungMapping as lieferung)
        .where.eq(lieferung.lieferplanungId, id)
    }.map(lieferungMapping(lieferung)).list
  }

  protected def getVerfuegbareLieferungenQuery(id: LieferplanungId) = {
    sql"""
        SELECT
          ${lieferung.result.*}, ${aboTyp.result.*}
        FROM
          ${lieferungMapping as lieferung}
          INNER JOIN ${abotypMapping as aboTyp} ON ${lieferung.abotypId} = ${aboTyp.id}
        INNER JOIN
		      (SELECT ${lieferung.abotypId} AS abotypId, MIN(${lieferung.datum}) AS MinDateTime
		       FROM ${lieferungMapping as lieferung}
		       INNER JOIN ${abotypMapping as aboTyp}
		       ON ${lieferung.abotypId} = ${aboTyp.id}
		       AND ${lieferung.lieferplanungId} IS NULL
		       AND ${aboTyp.id} NOT IN (
		         SELECT DISTINCT ${lieferung.abotypId}
		         FROM ${lieferungMapping as lieferung}
		         WHERE ${lieferung.lieferplanungId} = ${id.id}
		       )
		       GROUP BY ${lieferung.abotypId}) groupedLieferung
		    ON ${lieferung.abotypId} = groupedLieferung.abotypId 
		    AND ${lieferung.datum} = groupedLieferung.MinDateTime
      """.one(lieferungMapping(lieferung))
      .toOne(abotypMapping.opt(aboTyp))
      .map { (lieferung, abotyp) =>
        val emptyPosition = Seq.empty[Lieferposition]
        copyTo[Lieferung, LieferungDetail](lieferung, "abotyp" -> abotyp, "lieferpositionen" -> emptyPosition)
      }.list
  }

  protected def getBestellungenQuery(id: LieferplanungId) = {
    withSQL {
      select
        .from(bestellungMapping as bestellung)
        .where.eq(bestellung.lieferplanungId, id)
    }.map(bestellungMapping(bestellung)).list
  }

  protected def getBestellungByProduzentLieferplanungDatumQuery(produzentId: ProduzentId, lieferplanungId: LieferplanungId, datum: DateTime) = {
    withSQL {
      select
        .from(bestellungMapping as bestellung)
        .where.eq(bestellung.produzentId, produzentId)
        .and.eq(bestellung.lieferplanungId, lieferplanungId)
        .and.eq(bestellung.datum, datum)
    }.map(bestellungMapping(bestellung)).single
  }

  protected def getBestellpositionenQuery(id: BestellungId) = {
    withSQL {
      select
        .from(bestellpositionMapping as bestellposition)
        .where.eq(bestellposition.bestellungId, id)
    }.map(bestellpositionMapping(bestellposition)).list
  }

  protected def getBestellpositionenByLieferplanQuery(id: LieferplanungId) = {
    withSQL {
      select
        .from(bestellpositionMapping as bestellposition)
        .leftJoin(bestellungMapping as bestellung).on(bestellposition.bestellungId, bestellung.id)
        .where.eq(bestellung.lieferplanungId, id)
    }.map(bestellpositionMapping(bestellposition)).list
  }

  protected def getLieferpositionenQuery(id: LieferungId) = {
    withSQL {
      select
        .from(lieferpositionMapping as lieferposition)
        .where.eq(lieferposition.lieferungId, id)
    }.map(lieferpositionMapping(lieferposition)).list
  }

  protected def getLieferpositionenByLieferantQuery(id: ProduzentId) = {
    withSQL {
      select
        .from(lieferpositionMapping as lieferposition)
        .where.eq(lieferposition.produzentId, id)
    }.map(lieferpositionMapping(lieferposition)).list
  }

  protected def getBestellpositionByBestellungProduktQuery(bestellungId: BestellungId, produktId: ProduktId) = {
    withSQL {
      select
        .from(bestellpositionMapping as bestellposition)
        .where.eq(bestellposition.bestellungId, bestellungId)
        .and.eq(bestellposition.produktId, produktId)
    }.map(bestellpositionMapping(bestellposition)).single
  }

  protected def getLieferpositionenByLieferplanQuery(id: LieferplanungId) = {
    withSQL {
      select
        .from(lieferpositionMapping as lieferposition)
        .leftJoin(lieferungMapping as lieferung).on(lieferposition.lieferungId, lieferung.id)
        .where.eq(lieferung.lieferplanungId, id)
    }.map(lieferpositionMapping(lieferposition)).list
  }

  protected def getKorbQuery(lieferungId: LieferungId, aboId: AboId) = {
    withSQL {
      select
        .from(korbMapping as korb)
        .where.eq(korb.lieferungId, lieferungId)
        .and.eq(korb.aboId, aboId).and.not.eq(korb.status, Geliefert.asInstanceOf[KorbStatus])
    }.map(korbMapping(korb)).single
  }

  protected def getVertriebQuery(vertriebId: VertriebId) = {
    withSQL {
      select
        .from(vertriebMapping as vertrieb)
        .where.eq(vertrieb.id, vertriebId)
    }.map(vertriebMapping(vertrieb)).single
  }

  protected def getVertriebeQuery(abotypId: AbotypId) = {
    withSQL {
      select
        .from(vertriebMapping as vertrieb)
        .leftJoin(depotlieferungMapping as depotlieferung).on(depotlieferung.vertriebId, vertrieb.id)
        .leftJoin(depotMapping as depot).on(depotlieferung.depotId, depot.id)
        .leftJoin(heimlieferungMapping as heimlieferung).on(heimlieferung.vertriebId, vertrieb.id)
        .leftJoin(tourMapping as tour).on(heimlieferung.tourId, tour.id)
        .leftJoin(postlieferungMapping as postlieferung).on(postlieferung.vertriebId, vertrieb.id)
        .where.eq(vertrieb.abotypId, abotypId)
    }.one(vertriebMapping(vertrieb))
      .toManies(
        rs => postlieferungMapping.opt(postlieferung)(rs),
        rs => heimlieferungMapping.opt(heimlieferung)(rs),
        rs => depotlieferungMapping.opt(depotlieferung)(rs),
        rs => depotMapping.opt(depot)(rs),
        rs => tourMapping.opt(tour)(rs)
      )
      .map({ (vertrieb, pl, hls, dls, depots, touren) =>
        val dl = dls.map { lieferung =>
          val depot = depots.find(_.id == lieferung.depotId).head
          val summary = copyTo[Depot, DepotSummary](depot)
          copyTo[Depotlieferung, DepotlieferungDetail](lieferung, "depot" -> summary)
        }
        val hl = hls.map { lieferung =>
          val tour = touren.find(_.id == lieferung.tourId).head
          copyTo[Heimlieferung, HeimlieferungDetail](lieferung, "tour" -> tour)
        }

        copyTo[Vertrieb, VertriebVertriebsarten](vertrieb, "depotlieferungen" -> dl, "heimlieferungen" -> hl, "postlieferungen" -> pl)
      }).list
  }

  // MODIFY and DELETE Queries

  protected def deleteLieferpositionenQuery(id: LieferungId) = {
    withSQL {
      delete
        .from(lieferpositionMapping)
        .where.eq(lieferpositionShort.lieferungId, id)
    }
  }

  protected def deleteKoerbeQuery(id: LieferungId) = {
    withSQL {
      delete
        .from(korbMapping)
        .where.eq(korbShort.lieferungId, id)
    }
  }

}