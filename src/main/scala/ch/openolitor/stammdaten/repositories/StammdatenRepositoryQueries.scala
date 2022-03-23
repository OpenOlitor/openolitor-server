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

import scalikejdbc._
import sqls.{ count, distinct }
import ch.openolitor.stammdaten.models._
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.Macros._
import org.joda.time.DateTime
import com.github.nscala_time.time.Imports._
import ch.openolitor.util.StringUtil._
import ch.openolitor.stammdaten.StammdatenDBMappings
import ch.openolitor.util.querybuilder.UriQueryParamToSQLSyntaxBuilder
import ch.openolitor.util.parsing.{ FilterExpr, GeschaeftsjahrFilter }
import org.joda.time.LocalDate
import ch.openolitor.arbeitseinsatz.ArbeitseinsatzDBMappings
import ch.openolitor.core.models.{ BaseEntity, BaseId }

trait StammdatenRepositoryQueries extends LazyLogging with StammdatenDBMappings with ArbeitseinsatzDBMappings {

  lazy val aboTyp = abotypMapping.syntax("atyp")
  lazy val zusatzAboTyp = zusatzAbotypMapping.syntax("zatyp")
  lazy val person = personMapping.syntax("pers")
  lazy val personCategory = personCategoryMapping.syntax("persCat")
  lazy val lieferplanung = lieferplanungMapping.syntax("lieferplanung")
  lazy val lieferung = lieferungMapping.syntax("lieferung")
  lazy val hauptLieferung = lieferungMapping.syntax("lieferung")
  lazy val lieferungJoin = lieferungMapping.syntax("lieferungJ")
  lazy val lieferposition = lieferpositionMapping.syntax("lieferposition")
  lazy val bestellung = bestellungMapping.syntax("bestellung")
  lazy val sammelbestellung = sammelbestellungMapping.syntax("sammelbestellung")
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
  lazy val zusatzAbo = zusatzAboMapping.syntax("zusatzAbo")
  lazy val produkt = produktMapping.syntax("produkt")
  lazy val produktekategorie = produktekategorieMapping.syntax("produktekategorie")
  lazy val produzent = produzentMapping.syntax("produzent")
  lazy val projekt = projektMapping.syntax("projekt")
  lazy val projektV1 = projektV1Mapping.syntax("projektV1")
  lazy val kontoDaten = kontoDatenMapping.syntax("kontoDaten")
  lazy val produktProduzent = produktProduzentMapping.syntax("produktProduzent")
  lazy val produktProduktekategorie = produktProduktekategorieMapping.syntax("produktProduktekategorie")
  lazy val abwesenheit = abwesenheitMapping.syntax("abwesenheit")
  lazy val korb = korbMapping.syntax("korb")
  lazy val vertrieb = vertriebMapping.syntax("vertrieb")
  lazy val tourlieferung = tourlieferungMapping.syntax("tourlieferung")
  lazy val depotAuslieferung = depotAuslieferungMapping.syntax("depotAuslieferung")
  lazy val tourAuslieferung = tourAuslieferungMapping.syntax("tourAuslieferung")
  lazy val postAuslieferung = postAuslieferungMapping.syntax("postAuslieferung")
  lazy val projektVorlage = projektVorlageMapping.syntax("projektVorlage")
  lazy val einladung = einladungMapping.syntax("einladung")

  lazy val arbeitseinsatz = arbeitseinsatzMapping.syntax("arbeitseinsatz")

  lazy val lieferpositionShort = lieferpositionMapping.syntax
  lazy val korbShort = korbMapping.syntax
  lazy val zusatzAboShort = zusatzAboMapping.syntax

  lazy val korbSecond = korbMapping.syntax("korbSecond")
  lazy val lieferungSecond = lieferungMapping.syntax("lieferungSecond")

  protected def getAbotypenQuery(filter: Option[FilterExpr]) = {
    withSQL {
      select
        .from(abotypMapping as aboTyp)
        .where(UriQueryParamToSQLSyntaxBuilder.build(filter, aboTyp))
        .orderBy(aboTyp.name)
    }.map(abotypMapping(aboTyp)).list
  }

  protected def getExistingZusatzAbotypenQuery(lieferungId: LieferungId) = {
    withSQL {
      select
        .from(zusatzAbotypMapping as zusatzAboTyp)
        .leftJoin(zusatzAboMapping as zusatzAbo).on(zusatzAbo.abotypId, zusatzAboTyp.id)
        .leftJoin(korbMapping as korb).on(korb.aboId, zusatzAbo.hauptAboId)
        .where.eq(korb.lieferungId, lieferungId)
    }.map(zusatzAbotypMapping(zusatzAboTyp)).list
  }

  protected def getZusatzAbotypenQuery(filter: Option[FilterExpr]) = {
    withSQL {
      select
        .from(zusatzAbotypMapping as zusatzAboTyp)
        .where(UriQueryParamToSQLSyntaxBuilder.build(filter, zusatzAboTyp))
        .orderBy(zusatzAboTyp.name)
    }.map(zusatzAbotypMapping(zusatzAboTyp)).list
  }

  protected def getKundenQuery = {
    withSQL {
      select
        .from(kundeMapping as kunde)
        .orderBy(kunde.bezeichnung)
    }.map(kundeMapping(kunde)).list
  }

  protected def getKundenByKundentypQuery(kundentyp: KundentypId) = {
    // search for kundentyp in typen spalte von Kunde (Komma separierte liste von Kundentypen)
    val kundentypRegex: String = SQLSyntax.createUnsafely(s"""([ ,]|^)${kundentyp.id}([ ,]|$$)+""")
    sql"""
      SELECT ${kunde.result.*} FROM ${kundeMapping as kunde}
      WHERE typen REGEXP ${kundentypRegex}
    """.map(kundeMapping(kunde)).list
  }

  protected def getKundenUebersichtQuery(filter: Option[FilterExpr]) = {
    withSQL {
      select
        .from(kundeMapping as kunde)
        .leftJoin(personMapping as person).on(kunde.id, person.kundeId)
        .leftJoin(kontoDatenMapping as kontoDaten).on(kunde.id, kontoDaten.kunde)
        .where(UriQueryParamToSQLSyntaxBuilder.build(filter, kunde))
        .orderBy(person.sort)
    }.one(kundeMapping(kunde))
      .toManies(
        rs => personMapping.opt(person)(rs),
        rs => kontoDatenMapping.opt(kontoDaten)(rs)
      )
      .map((kunde, personen, kontoDaten) => {
        val personenWihoutPwd = personen.toSet[Person].map(p => copyTo[Person, PersonSummary](p)).toSeq
        val kd = kontoDaten.length match {
          case 0 => None
          case 1 => Some(kontoDaten(0))
          case _ => {
            logger.error(s"The kunde $kunde.id cannot have more than an account")
            None
          }
        }
        copyTo[Kunde, KundeUebersicht](kunde, "ansprechpersonen" -> personenWihoutPwd, "kontoDaten" -> kd)
      }).list
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
        .leftJoin(kontoDatenMapping as kontoDaten).on(kunde.id, kontoDaten.kunde)
        .where.eq(kunde.id, id)
        .orderBy(person.sort)
    }.one(kundeMapping(kunde))
      .toManies(
        rs => postlieferungAboMapping.opt(postlieferungAbo)(rs),
        rs => heimlieferungAboMapping.opt(heimlieferungAbo)(rs),
        rs => depotlieferungAboMapping.opt(depotlieferungAbo)(rs),
        rs => personMapping.opt(person)(rs),
        rs => pendenzMapping.opt(pendenz)(rs),
        rs => kontoDatenMapping.opt(kontoDaten)(rs)
      )
      .map((kunde, pl, hl, dl, personen, pendenzen, kontoDaten) => {
        val abos = pl ++ hl ++ dl
        val personenWihoutPwd = personen.toSet[Person].map(p => copyTo[Person, PersonDetail](p)).toSeq
        val kd = kontoDaten.length match {
          case 0 => None
          case 1 => Some(kontoDaten(0))
          case _ => {
            logger.error(s"The kunde $kunde.id cannot have more than an account")
            None
          }
        }
        copyTo[Kunde, KundeDetail](kunde, "abos" -> abos, "pendenzen" -> pendenzen, "ansprechpersonen" -> personenWihoutPwd, "kontoDaten" -> kd)
      }).single
  }

  protected def getKundeDetailReportQuery(kundeId: KundeId, projekt: ProjektReport) = {
    val x = SubQuery.syntax("x").include(abwesenheit)
    withSQL {
      select
        .from(kundeMapping as kunde)
        .leftJoin(depotlieferungAboMapping as depotlieferungAbo).on(kunde.id, depotlieferungAbo.kundeId)
        .leftJoin(heimlieferungAboMapping as heimlieferungAbo).on(kunde.id, heimlieferungAbo.kundeId)
        .leftJoin(postlieferungAboMapping as postlieferungAbo).on(kunde.id, postlieferungAbo.kundeId)
        .leftJoin(personMapping as person).on(kunde.id, person.kundeId)
        .leftJoin(pendenzMapping as pendenz).on(kunde.id, pendenz.kundeId)
        .leftJoin(kontoDatenMapping as kontoDaten).on(kunde.id, kontoDaten.kunde)
        .where.eq(kunde.id, kundeId)
        .orderBy(person.sort)
    }.one(kundeMapping(kunde))
      .toManies(
        rs => postlieferungAboMapping.opt(postlieferungAbo)(rs),
        rs => heimlieferungAboMapping.opt(heimlieferungAbo)(rs),
        rs => depotlieferungAboMapping.opt(depotlieferungAbo)(rs),
        rs => personMapping.opt(person)(rs),
        rs => pendenzMapping.opt(pendenz)(rs),
        rs => kontoDatenMapping.opt(kontoDaten)(rs)
      )
      .map { (kunde, pl, hl, dl, personen, pendenzen, kontoDaten) =>
        val abos = pl ++ hl ++ dl
        val personenWihoutPwd = personen.toSet[Person].map(p => copyTo[Person, PersonDetail](p)).toSeq
        val kd = kontoDaten.length match {
          case 0 => None
          case 1 => Some(kontoDaten(0))
          case _ => {
            logger.error(s"The kunde $kunde.id cannot have more than an account")
            None
          }
        }

        copyTo[Kunde, KundeDetailReport](kunde, "abos" -> abos, "pendenzen" -> pendenzen,
          "personen" -> personenWihoutPwd, "projekt" -> projekt, "kontoDaten" -> kd)
      }.single
  }

  protected def getKundeDetailsArbeitseinsatzReportQuery(projekt: ProjektReport) = {
    withSQL {
      select
        .from(kundeMapping as kunde)
        .leftJoin(depotlieferungAboMapping as depotlieferungAbo).on(kunde.id, depotlieferungAbo.kundeId)
        .leftJoin(heimlieferungAboMapping as heimlieferungAbo).on(kunde.id, heimlieferungAbo.kundeId)
        .leftJoin(postlieferungAboMapping as postlieferungAbo).on(kunde.id, postlieferungAbo.kundeId)
        .leftJoin(abotypMapping as aboTyp).on(sqls.eq(aboTyp.id, depotlieferungAbo.abotypId).or.eq(aboTyp.id, heimlieferungAbo.abotypId).or.eq(aboTyp.id, postlieferungAbo.abotypId))
        .leftJoin(personMapping as person).on(kunde.id, person.kundeId)
        .leftJoin(pendenzMapping as pendenz).on(kunde.id, pendenz.kundeId)
        .leftJoin(arbeitseinsatzMapping as arbeitseinsatz).on(kunde.id, arbeitseinsatz.kundeId)
        .orderBy(person.sort)
    }.one(kundeMapping(kunde))
      .toManies(
        rs => postlieferungAboMapping.opt(postlieferungAbo)(rs),
        rs => heimlieferungAboMapping.opt(heimlieferungAbo)(rs),
        rs => depotlieferungAboMapping.opt(depotlieferungAbo)(rs),
        rs => abotypMapping.opt(aboTyp)(rs),
        rs => personMapping.opt(person)(rs),
        rs => pendenzMapping.opt(pendenz)(rs),
        rs => arbeitseinsatzMapping.opt(arbeitseinsatz)(rs)
      )
      .map { (kunde, pl, hl, dl, abotypen, personen, pendenzen, arbeitseinsaetze) =>
        val abos = pl ++ hl ++ dl
        val anzahlArbeitseinsaetzeSoll = abotypen map (at => at.anzahlEinsaetze.getOrElse(BigDecimal(0.0))) sum
        val anzahlArbeitseinsaetzeIst = arbeitseinsaetze map (ae => ae.einsatzZeit.getOrElse(BigDecimal(0.0))) sum
        val persL = personen.toSet[Person].map(p => copyTo[Person, PersonDetail](p)).toSeq

        copyTo[Kunde, KundeDetailArbeitseinsatzReport](kunde, "abos" -> abos, "pendenzen" -> pendenzen,
          "personen" -> persL, "projekt" -> projekt, "anzahlArbeitseinsaetzeSoll" -> anzahlArbeitseinsaetzeSoll,
          "anzahlArbeitseinsaetzeIst" -> anzahlArbeitseinsaetzeIst, "arbeitseinsaetze" -> arbeitseinsaetze)
      }.list
  }

  protected def getPersonenQuery = {
    withSQL {
      select
        .from(personMapping as person)
        .orderBy(person.kundeId, person.sort)
    }.map(personMapping(person)).list
  }

  protected def getPersonByCategoryQuery(category: PersonCategoryNameId) = {
    val personCategoryRegex: String = SQLSyntax.createUnsafely(s"""([ ,]|^)${category.id}([ ,]|$$)+""")
    sql"""
      SELECT ${person.result.*} FROM ${personMapping as person}
      WHERE categories REGEXP ${personCategoryRegex}
    """.map(personMapping(person)).list
  }

  protected def getPersonenQuery(kundeId: KundeId) = {
    withSQL {
      select
        .from(personMapping as person)
        .where.eq(person.kundeId, kundeId)
        .orderBy(person.sort)
    }.map(personMapping(person)).list
  }

  protected def getPersonenForAbotypQuery(abotypId: AbotypId) = {
    withSQL {
      select
        .from(personMapping as person)
        .leftJoin(kundeMapping as kunde).on(kunde.id, person.kundeId)
        .leftJoin(depotlieferungAboMapping as depotlieferungAbo).on(depotlieferungAbo.kundeId, kunde.id)
        .leftJoin(heimlieferungAboMapping as heimlieferungAbo).on(heimlieferungAbo.kundeId, kunde.id)
        .leftJoin(postlieferungAboMapping as postlieferungAbo).on(postlieferungAbo.kundeId, kunde.id)
        .where.withRoundBracket {
          _.eq(depotlieferungAbo.aktiv, true).and.eq(depotlieferungAbo.abotypId, abotypId)
        }.or.withRoundBracket {
          _.eq(heimlieferungAbo.aktiv, true).and.eq(heimlieferungAbo.abotypId, abotypId)
        }.or.withRoundBracket {
          _.eq(postlieferungAbo.aktiv, true).and.eq(postlieferungAbo.abotypId, abotypId)
        }
    }.map(personMapping(person)).list
  }

  protected def getPersonenForZusatzabotypQuery(abotypId: AbotypId) = {
    withSQL {
      select
        .from(personMapping as person)
        .leftJoin(kundeMapping as kunde).on(kunde.id, person.kundeId)
        .leftJoin(zusatzAboMapping as zusatzAbo).on(zusatzAbo.kundeId, kunde.id)
        .where.eq(zusatzAbo.abotypId, abotypId).and.eq(zusatzAbo.aktiv, true)
    }.map(personMapping(person)).list
  }

  protected def getPersonenQuery(tourId: TourId) = {
    withSQL {
      select
        .from(personMapping as person)
        .join(kundeMapping as kunde).on(kunde.id, person.kundeId)
        .join(heimlieferungAboMapping as heimlieferungAbo).on(heimlieferungAbo.kundeId, kunde.id)
        .join(tourMapping as tour).on(tour.id, heimlieferungAbo.tourId)
        .where.eq(tour.id, tourId).and.eq(heimlieferungAbo.aktiv, true)
    }.map(personMapping(person)).list
  }

  protected def getPersonenQuery(depotId: DepotId) = {
    withSQL {
      select
        .from(personMapping as person)
        .join(kundeMapping as kunde).on(kunde.id, person.kundeId)
        .join(depotlieferungAboMapping as depotlieferungAbo).on(depotlieferungAbo.kundeId, kunde.id)
        .join(depotMapping as depot).on(depot.id, depotlieferungAbo.depotId)
        .where.eq(depot.id, depotId).and.eq(depotlieferungAbo.aktiv, true)
    }.map(personMapping(person)).list
  }

  protected def getPersonenUebersichtQuery(filter: Option[FilterExpr]) = {
    withSQL {
      select
        .from(personMapping as person)
        .leftJoin(kundeMapping as kunde).on(person.kundeId, kunde.id)
        .where(UriQueryParamToSQLSyntaxBuilder.build(filter, person))
        .orderBy(person.name)
    }.one(personMapping(person))
      .toOne(
        rs => kundeMapping(kunde)(rs)
      ).map { (person, kunde) =>
          copyTo[Person, PersonUebersicht](
            person,
            "strasse" -> kunde.strasse,
            "hausNummer" -> kunde.hausNummer,
            "adressZusatz" -> kunde.adressZusatz,
            "plz" -> kunde.plz,
            "ort" -> kunde.ort,
            "kundentypen" -> kunde.typen,
            "kundenBemerkungen" -> kunde.bemerkungen
          )
        }.list
  }

  protected def getPersonCategoryQuery = {
    withSQL {
      select
        .from(personCategoryMapping as personCategory)
        .orderBy(personCategory.id)
    }.map(personCategoryMapping(personCategory)).list
  }

  protected def getAbotypDetailQuery(id: AbotypId) = {
    withSQL {
      select
        .from(abotypMapping as aboTyp)
        .where.eq(aboTyp.id, id)
    }.map(abotypMapping(aboTyp)).single
  }

  protected def getZusatzAbotypDetailQuery(id: AbotypId) = {
    withSQL {
      select
        .from(zusatzAbotypMapping as zusatzAboTyp)
        .where.eq(zusatzAboTyp.id, id)
    }.map(zusatzAbotypMapping(zusatzAboTyp)).single
  }

  protected def getDepotlieferungAbosQuery(abotypId: AbotypId) = {
    withSQL {
      select
        .from(depotlieferungAboMapping as depotlieferungAbo)
        .where.eq(depotlieferungAbo.abotypId, abotypId)
    }.map(depotlieferungAboMapping(depotlieferungAbo)).list
  }

  protected def getHeimlieferungAbosQuery(abotypId: AbotypId) = {
    withSQL {
      select
        .from(heimlieferungAboMapping as heimlieferungAbo)
        .where.eq(heimlieferungAbo.abotypId, abotypId)
    }.map(heimlieferungAboMapping(heimlieferungAbo)).list
  }

  protected def getPostlieferungAbosQuery(abotypId: AbotypId) = {
    withSQL {
      select
        .from(postlieferungAboMapping as postlieferungAbo)
        .where.eq(postlieferungAbo.abotypId, abotypId)
    }.map(postlieferungAboMapping(postlieferungAbo)).list
  }

  protected def getDepotlieferungAbosByVertriebQuery(vertriebId: VertriebId) = {
    withSQL {
      select
        .from(depotlieferungAboMapping as depotlieferungAbo)
        .where.eq(depotlieferungAbo.vertriebId, vertriebId)
    }.map(depotlieferungAboMapping(depotlieferungAbo)).list
  }

  protected def getHeimlieferungAbosByVertriebQuery(vertriebId: VertriebId) = {
    withSQL {
      select
        .from(heimlieferungAboMapping as heimlieferungAbo)
        .where.eq(heimlieferungAbo.vertriebId, vertriebId)
    }.map(heimlieferungAboMapping(heimlieferungAbo)).list
  }

  protected def getPostlieferungAbosByVertriebQuery(vertriebId: VertriebId) = {
    withSQL {
      select
        .from(postlieferungAboMapping as postlieferungAbo)
        .where.eq(postlieferungAbo.vertriebId, vertriebId)
    }.map(postlieferungAboMapping(postlieferungAbo)).list
  }

  protected def getZusatzAbosByVertriebQuery(vertriebId: VertriebId) = {
    withSQL {
      select
        .from(zusatzAboMapping as zusatzAbo)
        .where.eq(zusatzAbo.vertriebId, vertriebId)
    }.map(zusatzAboMapping(zusatzAbo)).list
  }

  protected def getZusatzAbosQuery(filter: Option[FilterExpr], datumsFilter: Option[GeschaeftsjahrFilter]) = {
    withSQL {
      select
        .from(zusatzAboMapping as zusatzAbo)
        .join(projektMapping as projekt)
        .where.append(
          getAboDatumFilterQuery[ZusatzAbo](zusatzAbo, datumsFilter)
        ).and(
            UriQueryParamToSQLSyntaxBuilder.build(filter, zusatzAbo)
          )
    }.map(zusatzAboMapping(zusatzAbo)).list
  }

  protected def getDepotlieferungQuery(vertriebId: VertriebId) = {
    withSQL {
      select
        .from(depotlieferungMapping as depotlieferung)
        .leftJoin(depotMapping as depot).on(depotlieferung.depotId, depot.id)
        .where.eq(depotlieferung.vertriebId, vertriebId)
    }.one(depotlieferungMapping(depotlieferung)).toOne(depotMapping.opt(depot)).map { (vertriebsart, depot) =>
      val depotSummary = DepotSummary(depot.head.id, depot.head.name, depot.head.kurzzeichen)
      copyTo[Depotlieferung, DepotlieferungDetail](vertriebsart, "depot" -> depotSummary)
    }.list
  }

  protected def getDepotlieferungQuery(depotId: DepotId) = {
    withSQL {
      select
        .from(depotlieferungMapping as depotlieferung)
        .where.eq(depotlieferung.depotId, depotId)
    }.map(depotlieferungMapping(depotlieferung)).list
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

  protected def getHeimlieferungQuery(tourId: TourId) = {
    withSQL {
      select
        .from(heimlieferungMapping as heimlieferung)
        .where.eq(heimlieferung.tourId, tourId)
    }.map(heimlieferungMapping(heimlieferung)).list
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
      val depotSummary = DepotSummary(depot.head.id, depot.head.name, depot.head.kurzzeichen)
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
        .where.eq(lieferung.abotypId, abotypId).and.eq(vertrieb.abotypId, abotypId).and.eq(lieferung.vertriebId, vertriebId).and.isNull(lieferung.lieferplanungId)
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

  protected def getDepotDetailReportQuery(id: DepotId, projekt: ProjektReport) = {
    withSQL {
      select
        .from(depotMapping as depot)
        .leftJoin(depotlieferungAboMapping as depotlieferungAbo).on(depotlieferungAbo.depotId, depot.id)
        .leftJoin(kundeMapping as kunde).on(depotlieferungAbo.kundeId, kunde.id)
        .leftJoin(personMapping as person).on(kunde.id, person.kundeId)
        .where.eq(depot.id, id)
    }
      .one(depotMapping(depot))
      .toManies(
        rs => depotlieferungAboMapping.opt(depotlieferungAbo)(rs),
        rs => kundeMapping.opt(kunde)(rs),
        rs => personMapping.opt(person)(rs)
      )
      .map((depot, abos, kunden, personen) => {
        val personenWithoutPwd = personen map (p => copyTo[Person, PersonDetail](p))
        val abosReport = abos.map { abo =>
          kunden.filter(_.id == abo.kundeId).headOption map { kunde =>
            val ansprechpersonen = personenWithoutPwd filter (_.kundeId == kunde.id)
            val kundeReport = copyTo[Kunde, KundeReport](kunde, "personen" -> ansprechpersonen)
            copyTo[DepotlieferungAbo, DepotlieferungAboReport](abo, "kundeReport" -> kundeReport)
          }
        }.flatten
        copyTo[Depot, DepotDetailReport](depot, "projekt" -> projekt, "abos" -> abosReport)
      }).single
  }

  protected def getDepotlieferungAbosOnlyAktiveZusatzabosQuery(filter: Option[FilterExpr]) = {
    withSQL {
      select
        .from(depotlieferungAboMapping as depotlieferungAbo)
        .leftJoin(zusatzAboMapping as zusatzAbo).on(sqls"${depotlieferungAbo.id} = ${zusatzAbo.hauptAboId} and ${zusatzAbo.aktiv} =  true")
        .where(UriQueryParamToSQLSyntaxBuilder.build(filter, depotlieferungAbo))
    }.one(depotlieferungAboMapping(depotlieferungAbo))
      .toMany(
        rs => zusatzAboMapping.opt(zusatzAbo)(rs)
      )
      .map((depotlieferungAbo, zusatzAbos) => {
        val zusatzAboIds = zusatzAbos.map(_.id).toSet
        val zusatzAbotypNames = zusatzAbos.map(_.abotypName).toSeq
        copyTo[DepotlieferungAbo, DepotlieferungAbo](depotlieferungAbo, "zusatzAboIds" -> zusatzAboIds, "zusatzAbotypNames" -> zusatzAbotypNames)
      }).list
  }

  private def getEntityDatumFilterQuery[E <: BaseEntity[_ <: BaseId]](entity: QuerySQLSyntaxProvider[SQLSyntaxSupport[E], E], attribute: String, datumsFilter: Option[GeschaeftsjahrFilter]) = {
    datumsFilter match {
      case Some(datum) => sqls"""(
        (${entity.column(attribute.toUnderscore)} >= STR_TO_DATE(CONCAT(${datum.inGeschaeftsjahr},LPAD(${projekt.geschaeftsjahrMonat},2,'0'),${projekt.geschaeftsjahrTag}), '%Y%m%e')
        AND
         ${entity.column(attribute.toUnderscore)} < STR_TO_DATE(CONCAT(${datum.inGeschaeftsjahr + 1},LPAD(${projekt.geschaeftsjahrMonat},2,'0'),${projekt.geschaeftsjahrTag}), '%Y%m%e'))
        OR
        ${entity.column(attribute.toUnderscore)} IS NULL
      )"""
      case None => sqls"""1=1"""
    }
  }

  private def getAboDatumFilterQuery[A <: Abo](abo: QuerySQLSyntaxProvider[SQLSyntaxSupport[A], A], datumsFilter: Option[GeschaeftsjahrFilter]) = {
    datumsFilter match {
      case Some(datum) => sqls"""(
        ${abo.start} < STR_TO_DATE(CONCAT(${datum.inGeschaeftsjahr + 1},LPAD(${projekt.geschaeftsjahrMonat},2,'0'),${projekt.geschaeftsjahrTag}), '%Y%m%e')
        AND
          (
            ${abo.ende} IS NULL
            OR
            ${abo.ende} >= STR_TO_DATE(CONCAT(${datum.inGeschaeftsjahr},LPAD(${projekt.geschaeftsjahrMonat},2,'0'),${projekt.geschaeftsjahrTag}), '%Y%m%e')
          )
      )"""
      case None => sqls"""1=1"""
    }
  }

  protected def getDepotlieferungAbosQuery(filter: Option[FilterExpr], datumsFilter: Option[GeschaeftsjahrFilter]) = {
    withSQL {
      select
        .from(depotlieferungAboMapping as depotlieferungAbo)
        .join(projektMapping as projekt)
        .where.append(
          getAboDatumFilterQuery[DepotlieferungAbo](depotlieferungAbo, datumsFilter)
        ).and(
            UriQueryParamToSQLSyntaxBuilder.build(filter, depotlieferungAbo)
          )
    }.map(depotlieferungAboMapping(depotlieferungAbo)).list
  }

  protected def getPersonenByDepotsQuery(filter: Option[FilterExpr]) = {
    withSQL {
      select
        .from(personMapping as person)
        .leftJoin(depotlieferungAboMapping as depotlieferungAbo).on(depotlieferungAbo.kundeId, person.kundeId)
        .leftJoin(depotMapping as depot).on(depotlieferungAbo.depotId, depot.id)
        .where(UriQueryParamToSQLSyntaxBuilder.build(filter, depot))
    }.map { rs =>
      copyTo[Person, PersonSummary](personMapping(person)(rs))
    }.list
  }

  protected def getPersonenAboAktivByDepotsQuery(filter: Option[FilterExpr]) = {
    withSQL {
      select
        .from(personMapping as person)
        .leftJoin(depotlieferungAboMapping as depotlieferungAbo).on(depotlieferungAbo.kundeId, person.kundeId)
        .leftJoin(depotMapping as depot).on(depotlieferungAbo.depotId, depot.id)
        .where.eq(depotlieferungAbo.aktiv, true).and(UriQueryParamToSQLSyntaxBuilder.build(filter, depot))
    }.map { rs =>
      copyTo[Person, PersonSummary](personMapping(person)(rs))
    }.list
  }

  protected def getPersonenByTourenQuery(filter: Option[FilterExpr]) = {
    withSQL {
      select
        .from(personMapping as person)
        .leftJoin(heimlieferungAboMapping as heimlieferungAbo).on(heimlieferungAbo.kundeId, person.kundeId)
        .leftJoin(tourMapping as tour).on(heimlieferungAbo.tourId, tour.id)
        .where(UriQueryParamToSQLSyntaxBuilder.build(filter, tour))
    }.map { rs =>
      copyTo[Person, PersonSummary](personMapping(person)(rs))
    }.list
  }

  protected def getPersonenAboAktivByTourenQuery(filter: Option[FilterExpr]) = {
    withSQL {
      select
        .from(personMapping as person)
        .leftJoin(heimlieferungAboMapping as heimlieferungAbo).on(heimlieferungAbo.kundeId, person.kundeId)
        .leftJoin(tourMapping as tour).on(heimlieferungAbo.tourId, tour.id)
        .where.eq(heimlieferungAbo.aktiv, true).and(UriQueryParamToSQLSyntaxBuilder.build(filter, tour))
    }.map { rs =>
      copyTo[Person, PersonSummary](personMapping(person)(rs))
    }.list
  }

  protected def getPersonenByAbotypenQuery(filter: Option[FilterExpr]) = {
    withSQL {
      select
        .from(personMapping as person)
        .leftJoin(depotlieferungAboMapping as depotlieferungAbo).on(depotlieferungAbo.kundeId, person.kundeId)
        .leftJoin(heimlieferungAboMapping as heimlieferungAbo).on(heimlieferungAbo.kundeId, person.kundeId)
        .leftJoin(postlieferungAboMapping as postlieferungAbo).on(postlieferungAbo.kundeId, person.kundeId)
        .leftJoin(abotypMapping as aboTyp).on(sqls.eq(depotlieferungAbo.abotypId, aboTyp.id).or.eq(heimlieferungAbo.abotypId, aboTyp.id).or.eq(postlieferungAbo.abotypId, aboTyp.id))
        .where(UriQueryParamToSQLSyntaxBuilder.build(filter, aboTyp))
    }.map { rs =>
      copyTo[Person, PersonSummary](personMapping(person)(rs))
    }.list
  }

  protected def getPersonenAboAktivByAbotypenQuery(filter: Option[FilterExpr]) = {
    withSQL {
      select
        .from(personMapping as person)
        .leftJoin(depotlieferungAboMapping as depotlieferungAbo).on(depotlieferungAbo.kundeId, person.kundeId)
        .leftJoin(heimlieferungAboMapping as heimlieferungAbo).on(heimlieferungAbo.kundeId, person.kundeId)
        .leftJoin(postlieferungAboMapping as postlieferungAbo).on(postlieferungAbo.kundeId, person.kundeId)
        .leftJoin(abotypMapping as aboTyp).on(sqls.eq(depotlieferungAbo.abotypId, aboTyp.id).or.eq(heimlieferungAbo.abotypId, aboTyp.id).or.eq(postlieferungAbo.abotypId, aboTyp.id))
        .where(UriQueryParamToSQLSyntaxBuilder.build(filter, aboTyp))
        .having(
          sqls.
            eq(depotlieferungAbo.aktiv, true)
            .or.eq(heimlieferungAbo.aktiv, true)
            .or.eq(postlieferungAbo.aktiv, true)
        )
    }.map { rs =>
      copyTo[Person, PersonSummary](personMapping(person)(rs))
    }.list
  }

  protected def getPersonenZusatzAboAktivByZusatzAbotypenQuery(filter: Option[FilterExpr]) = {
    withSQL {
      select
        .from(personMapping as person)
        .leftJoin(zusatzAboMapping as zusatzAbo).on(zusatzAbo.kundeId, person.kundeId)
        .leftJoin(zusatzAbotypMapping as zusatzAboTyp).on(sqls.eq(zusatzAbo.abotypId, zusatzAboTyp.id))
        .where(UriQueryParamToSQLSyntaxBuilder.build(filter, zusatzAboTyp))
        .having(sqls.eq(zusatzAbo.aktiv, true))
    }.map { rs =>
      copyTo[Person, PersonSummary](personMapping(person)(rs))
    }.list
  }

  protected def getHeimlieferungAbosOnlyAktiveZusatzabosQuery(filter: Option[FilterExpr]) = {
    withSQL {
      select
        .from(heimlieferungAboMapping as heimlieferungAbo)
        .leftJoin(zusatzAboMapping as zusatzAbo).on(sqls"${heimlieferungAbo.id} = ${zusatzAbo.hauptAboId} and ${zusatzAbo.aktiv} = true")
        .where(UriQueryParamToSQLSyntaxBuilder.build(filter, heimlieferungAbo))
    }.one(heimlieferungAboMapping(heimlieferungAbo))
      .toMany(
        rs => zusatzAboMapping.opt(zusatzAbo)(rs)
      )
      .map((heimlieferungAbo, zusatzAbos) => {
        val zusatzAboIds = zusatzAbos.map(_.id).toSet
        val zusatzAbotypNames = zusatzAbos.map(_.abotypName).toSeq
        copyTo[HeimlieferungAbo, HeimlieferungAbo](heimlieferungAbo, "zusatzAboIds" -> zusatzAboIds, "zusatzAbotypNames" -> zusatzAbotypNames)
      }).list
  }

  protected def getHeimlieferungAbosQuery(filter: Option[FilterExpr], datumsFilter: Option[GeschaeftsjahrFilter]) = {
    withSQL {
      select
        .from(heimlieferungAboMapping as heimlieferungAbo)
        .join(projektMapping as projekt)
        .where.append(
          getAboDatumFilterQuery[HeimlieferungAbo](heimlieferungAbo, datumsFilter)
        ).and(
            UriQueryParamToSQLSyntaxBuilder.build(filter, heimlieferungAbo)
          )
    }.map(heimlieferungAboMapping(heimlieferungAbo)).list
  }

  protected def getPostlieferungAbosOnlyAktiveZusatzabosQuery(filter: Option[FilterExpr]) = {
    withSQL {
      select
        .from(postlieferungAboMapping as postlieferungAbo)
        .leftJoin(zusatzAboMapping as zusatzAbo).on(sqls"${postlieferungAbo.id} = ${zusatzAbo.hauptAboId} and ${zusatzAbo.aktiv} =  true")
        .where(UriQueryParamToSQLSyntaxBuilder.build(filter, postlieferungAbo))
    }.one(postlieferungAboMapping(postlieferungAbo))
      .toMany(
        rs => zusatzAboMapping.opt(zusatzAbo)(rs)
      )
      .map((postlieferungAbo, zusatzAbos) => {
        val zusatzAboIds = zusatzAbos.map(_.id).toSet
        val zusatzAbotypNames = zusatzAbos.map(_.abotypName).toSeq
        copyTo[PostlieferungAbo, PostlieferungAbo](postlieferungAbo, "zusatzAboIds" -> zusatzAboIds, "zusatzAbotypNames" -> zusatzAbotypNames)
      }).list
  }

  protected def getPostlieferungAbosQuery(filter: Option[FilterExpr], datumsFilter: Option[GeschaeftsjahrFilter]) = {
    withSQL {
      select
        .from(postlieferungAboMapping as postlieferungAbo)
        .join(projektMapping as projekt)
        .where.append(
          getAboDatumFilterQuery[PostlieferungAbo](postlieferungAbo, datumsFilter)
        ).and(
            UriQueryParamToSQLSyntaxBuilder.build(filter, postlieferungAbo)
          )
    }.map(postlieferungAboMapping(postlieferungAbo)).list
  }

  protected def getDepotlieferungAbosByDepotQuery(id: DepotId) = {
    withSQL {
      select
        .from(depotlieferungAboMapping as depotlieferungAbo)
        .where.eq(depotlieferungAbo.depotId, id)
    }.map(depotlieferungAboMapping(depotlieferungAbo)).list
  }

  protected def getDepotlieferungAboAusstehendQuery(id: AboId) = {
    getDepotlieferungAboBaseQuery(id, Some(()))
  }

  protected def getDepotlieferungAboQuery(id: AboId) = {
    getDepotlieferungAboBaseQuery(id, None)
  }

  private def getDepotlieferungAboBaseQuery(id: AboId, ausstehend: Option[Unit]) = {
    withSQL {
      select
        .from(depotlieferungAboMapping as depotlieferungAbo)
        .leftJoin(vertriebMapping as vertrieb).on(
          sqls.eq(depotlieferungAbo.vertriebId, vertrieb.id)
        )
        .leftJoin(lieferungMapping as lieferung).on(
          sqls.eq(lieferung.vertriebId, vertrieb.id).and.eq(lieferung.abotypId, depotlieferungAbo.abotypId)
        )
        .leftJoin(lieferplanungMapping as lieferplanung).on(
          sqls.eq(lieferung.lieferplanungId, lieferplanung.id).and(sqls.toAndConditionOpt(
            ausstehend map (_ => sqls.isNull(lieferung.lieferplanungId).or.eq(lieferplanung.status, Ungeplant)
              .or.eq(lieferplanung.status, Offen)
              .or.eq(depotlieferungAbo.aktiv, false))
          ))
        )
        .leftJoin(abwesenheitMapping as abwesenheit).on(
          sqls.eq(depotlieferungAbo.id, abwesenheit.aboId).and(sqls.toAndConditionOpt(
            ausstehend map (_ => sqls.isNull(lieferung.lieferplanungId).or.eq(lieferplanung.status, Ungeplant)
              .or.eq(lieferplanung.status, Offen)
              .or.eq(depotlieferungAbo.aktiv, false))
          ))
        )
        .leftJoin(abotypMapping as aboTyp).on(depotlieferungAbo.abotypId, aboTyp.id)
        .where.eq(depotlieferungAbo.id, id)
    }.one(depotlieferungAboMapping(depotlieferungAbo))
      .toManies(
        rs => abwesenheitMapping.opt(abwesenheit)(rs),
        rs => abotypMapping.opt(aboTyp)(rs),
        rs => vertriebMapping.opt(vertrieb)(rs),
        rs => lieferungMapping.opt(lieferung)(rs)
      )
      .map((abo, abw, aboTyp, vertriebe, lieferungen) => {
        val sortedAbw = abw.sortBy(_.datum)
        val sortedLieferungen = lieferungen.sortBy(_.datum)
        copyTo[DepotlieferungAbo, DepotlieferungAboDetail](abo, "abwesenheiten" -> sortedAbw, "lieferdaten" -> sortedLieferungen,
          "abotyp" -> aboTyp.headOption, "vertrieb" -> vertriebe.headOption)
      }).single
  }

  protected def getHeimlieferungAboAusstehendQuery(id: AboId) = {
    getHeimlieferungAboBaseQuery(id, Some(()))
  }

  protected def getHeimlieferungAboQuery(id: AboId) = {
    getHeimlieferungAboBaseQuery(id, None)
  }

  private def getHeimlieferungAboBaseQuery(id: AboId, ausstehend: Option[Unit]) = {
    withSQL {
      select
        .from(heimlieferungAboMapping as heimlieferungAbo)
        .leftJoin(vertriebMapping as vertrieb).on(
          sqls.eq(heimlieferungAbo.vertriebId, vertrieb.id)
        )
        .leftJoin(lieferungMapping as lieferung).on(
          sqls.eq(lieferung.vertriebId, vertrieb.id).and.eq(lieferung.abotypId, heimlieferungAbo.abotypId)
        )
        .leftJoin(lieferplanungMapping as lieferplanung).on(
          sqls.eq(lieferung.lieferplanungId, lieferplanung.id).and(sqls.toAndConditionOpt(
            ausstehend map (_ => sqls.isNull(lieferung.lieferplanungId).or.eq(lieferplanung.status, Ungeplant)
              .or.eq(lieferplanung.status, Offen)
              .or.eq(heimlieferungAbo.aktiv, false))
          ))
        )
        .leftJoin(abwesenheitMapping as abwesenheit).on(
          sqls.eq(heimlieferungAbo.id, abwesenheit.aboId).and(sqls.toAndConditionOpt(
            ausstehend map (_ => sqls.isNull(lieferung.lieferplanungId).or.eq(lieferplanung.status, Ungeplant)
              .or.eq(lieferplanung.status, Offen)
              .or.eq(heimlieferungAbo.aktiv, false))
          ))
        )
        .leftJoin(abotypMapping as aboTyp).on(heimlieferungAbo.abotypId, aboTyp.id)
        .where.eq(heimlieferungAbo.id, id)
    }.one(heimlieferungAboMapping(heimlieferungAbo))
      .toManies(
        rs => abwesenheitMapping.opt(abwesenheit)(rs),
        rs => abotypMapping.opt(aboTyp)(rs),
        rs => vertriebMapping.opt(vertrieb)(rs),
        rs => lieferungMapping.opt(lieferung)(rs)
      )
      .map((abo, abw, aboTyp, vertriebe, lieferungen) => {
        val sortedAbw = abw.sortBy(_.datum)
        val sortedLieferungen = lieferungen.sortBy(_.datum)
        copyTo[HeimlieferungAbo, HeimlieferungAboDetail](abo, "abwesenheiten" -> sortedAbw, "lieferdaten" -> sortedLieferungen,
          "abotyp" -> aboTyp.headOption, "vertrieb" -> vertriebe.headOption)
      }).single
  }

  protected def getPostlieferungAboAusstehendQuery(id: AboId) = {
    getPostlieferungAboBaseQuery(id, Some(()))
  }

  protected def getPostlieferungAboQuery(id: AboId) = {
    getPostlieferungAboBaseQuery(id, None)
  }

  private def getPostlieferungAboBaseQuery(id: AboId, ausstehend: Option[Unit]) = {
    withSQL {
      select
        .from(postlieferungAboMapping as postlieferungAbo)
        .leftJoin(vertriebMapping as vertrieb).on(
          sqls.eq(postlieferungAbo.vertriebId, vertrieb.id)
        )
        .leftJoin(lieferungMapping as lieferung).on(
          sqls.eq(lieferung.vertriebId, vertrieb.id).and.eq(lieferung.abotypId, postlieferungAbo.abotypId)
        )
        .leftJoin(lieferplanungMapping as lieferplanung).on(
          sqls.eq(lieferung.lieferplanungId, lieferplanung.id).and(sqls.toAndConditionOpt(
            ausstehend map (_ => sqls.isNull(lieferung.lieferplanungId).or.eq(lieferplanung.status, Ungeplant)
              .or.eq(lieferplanung.status, Offen)
              .or.eq(postlieferungAbo.aktiv, false))
          ))
        )
        .leftJoin(abwesenheitMapping as abwesenheit).on(
          sqls.eq(postlieferungAbo.id, abwesenheit.aboId).and(sqls.toAndConditionOpt(
            ausstehend map (_ => sqls.isNull(lieferung.lieferplanungId).or.eq(lieferplanung.status, Ungeplant)
              .or.eq(lieferplanung.status, Offen)
              .or.eq(postlieferungAbo.aktiv, false))
          ))
        )
        .leftJoin(abotypMapping as aboTyp).on(postlieferungAbo.abotypId, aboTyp.id)
        .where.eq(postlieferungAbo.id, id)
    }.one(postlieferungAboMapping(postlieferungAbo))
      .toManies(
        rs => abwesenheitMapping.opt(abwesenheit)(rs),
        rs => abotypMapping.opt(aboTyp)(rs),
        rs => vertriebMapping.opt(vertrieb)(rs),
        rs => lieferungMapping.opt(lieferung)(rs)
      )
      .map((abo, abw, aboTyp, vertriebe, lieferungen) => {
        val sortedAbw = abw.sortBy(_.datum)
        val sortedLieferungen = lieferungen.sortBy(_.datum)
        copyTo[PostlieferungAbo, PostlieferungAboDetail](abo, "abwesenheiten" -> sortedAbw, "lieferdaten" -> sortedLieferungen,
          "abotyp" -> aboTyp.headOption, "vertrieb" -> vertriebe.headOption)
      }).single
  }

  protected def getZusatzAboDetailQuery(id: AboId) = {
    withSQL {
      select
        .from(zusatzAboMapping as zusatzAbo)
        .where.eq(zusatzAbo.id, id)
    }.map(zusatzAboMapping(zusatzAbo)).single
  }

  protected def getZusatzAboPerAboQuery(id: AboId) = {
    withSQL {
      select
        .from(zusatzAboMapping as zusatzAbo)
        .where.eq(zusatzAbo.hauptAboId, id)
    }.map(zusatzAboMapping(zusatzAbo)).list
  }

  protected def countKoerbeQuery(auslieferungId: AuslieferungId) = {
    withSQL {
      select(count(distinct(korb.id)))
        .from(korbMapping as korb)
        .where.eq(korb.auslieferungId, auslieferungId)
        .limit(1)
    }.map(_.int(1)).single
  }

  protected def countAbwesendQuery(lieferungId: LieferungId, aboId: AboId) = {
    withSQL {
      select(count(distinct(abwesenheit.id)))
        .from(abwesenheitMapping as abwesenheit)
        .where.eq(abwesenheit.lieferungId, lieferungId).and.eq(abwesenheit.aboId, aboId)
        .limit(1)
    }.map(_.int(1)).single
  }

  protected def countAbwesendQuery(aboId: AboId, datum: LocalDate) = {
    withSQL {
      select(count(distinct(abwesenheit.id)))
        .from(abwesenheitMapping as abwesenheit)
        .where.eq(abwesenheit.datum, datum).and.eq(abwesenheit.aboId, aboId)
        .limit(1)
    }.map(_.int(1)).single
  }

  protected def getAktiveDepotlieferungAbosQuery(abotypId: AbotypId, vertriebId: VertriebId, lieferdatum: DateTime) = {
    withSQL {
      select
        .from(depotlieferungAboMapping as depotlieferungAbo)
        .where.eq(depotlieferungAbo.vertriebId, vertriebId)
        .and.eq(depotlieferungAbo.abotypId, abotypId)
        .and.le(depotlieferungAbo.start, lieferdatum)
        .and.withRoundBracket { _.isNull(depotlieferungAbo.ende).or.ge(depotlieferungAbo.ende, lieferdatum.toLocalDate) }
    }.map(depotlieferungAboMapping(depotlieferungAbo)).list
  }

  protected def getAktiveHeimlieferungAbosQuery(abotypId: AbotypId, vertriebId: VertriebId, lieferdatum: DateTime) = {
    withSQL {
      select
        .from(heimlieferungAboMapping as heimlieferungAbo)
        .where.eq(heimlieferungAbo.vertriebId, vertriebId)
        .and.eq(heimlieferungAbo.abotypId, abotypId)
        .and.le(heimlieferungAbo.start, lieferdatum)
        .and.withRoundBracket { _.isNull(heimlieferungAbo.ende).or.ge(heimlieferungAbo.ende, lieferdatum.toLocalDate) }
    }.map(heimlieferungAboMapping(heimlieferungAbo)).list
  }

  protected def getAktiveZusatzAbosQuery(abotypId: AbotypId, lieferdatum: DateTime, lieferplanungId: LieferplanungId) = {
    // zusätzlich get haupabo, get all Lieferungen where Lieferplanung is equal
    withSQL {
      select
        .from(zusatzAboMapping as zusatzAbo)
        .innerJoin(lieferungMapping as lieferung).on(zusatzAbo.abotypId, lieferung.abotypId)
        .where.eq(zusatzAbo.abotypId, abotypId)
        .and.le(zusatzAbo.start, lieferdatum)
        .and.eq(lieferung.lieferplanungId, lieferplanungId)
        .and.withRoundBracket { _.isNull(zusatzAbo.ende).or.ge(zusatzAbo.ende, lieferdatum.toLocalDate) }
    }.map(zusatzAboMapping(zusatzAbo)).list
  }

  protected def getAktivePostlieferungAbosQuery(abotypId: AbotypId, vertriebId: VertriebId, lieferdatum: DateTime) = {
    withSQL {
      select
        .from(postlieferungAboMapping as postlieferungAbo)
        .where.eq(postlieferungAbo.vertriebId, vertriebId)
        .and.eq(postlieferungAbo.abotypId, abotypId)
        .and.le(postlieferungAbo.start, lieferdatum)
        .and.withRoundBracket { _.isNull(postlieferungAbo.ende).or.ge(postlieferungAbo.ende, lieferdatum.toLocalDate) }
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

  protected def getProduzentDetailReportQuery(id: ProduzentId, projekt: ProjektReport) = {
    withSQL {
      select
        .from(produzentMapping as produzent)
        .where.eq(produzent.id, id)
    }.map { rs =>
      val p = produzentMapping(produzent)(rs)
      copyTo[Produzent, ProduzentDetailReport](p, "projekt" -> projekt)
    }.single
  }

  protected def getTourenQuery = {
    withSQL {
      select
        .from(tourMapping as tour)
    }.map(tourMapping(tour)).list
  }

  protected def getTourDetailQuery(id: TourId, aktiveOrPlanned: Boolean) = {
    val today = LocalDate.now.toDateTimeAtStartOfDay
    withSQL {
      select
        .from(tourMapping as tour)
        .leftJoin(tourlieferungMapping as tourlieferung).on(tour.id, tourlieferung.tourId)
        .leftJoin(heimlieferungAboMapping as heimlieferungAbo).on(sqls"${tourlieferung.id} = ${heimlieferungAbo.id} and (${heimlieferungAbo.aktiv} IN (${aktiveOrPlanned}, true) or (${heimlieferungAbo.start} > ${today})) ")
        .leftJoin(zusatzAboMapping as zusatzAbo).on(sqls"${tourlieferung.id} = ${zusatzAbo.hauptAboId} and (${zusatzAbo.aktiv} IN (${aktiveOrPlanned}, true) or (${zusatzAbo.start} > ${today}))")
        .where.eq(tour.id, id).and.not.isNull(heimlieferungAbo.id)
        .orderBy(tourlieferung.sort)
    }.one(tourMapping(tour))
      .toManies(
        rs => tourlieferungMapping.opt(tourlieferung)(rs),
        rs => heimlieferungAboMapping.opt(heimlieferungAbo)(rs),
        rs => zusatzAboMapping.opt(zusatzAbo)(rs)
      )
      .map({ (tour, tourlieferungen, heimlieferungAbo, zusatzAbos) =>
        val tourlieferungenDetails = tourlieferungen map { t =>
          val z = zusatzAbos filter (_.hauptAboId == t.id)

          copyTo[Tourlieferung, TourlieferungDetail](t, "zusatzAbos" -> z)
        }

        copyTo[Tour, TourDetail](tour, "tourlieferungen" -> tourlieferungenDetails)
      }).single
  }

  protected def getTourlieferungenQuery(tourId: TourId) = {
    withSQL {
      select
        .from(tourlieferungMapping as tourlieferung)
        .where.eq(tourlieferung.tourId, tourId)
        .orderBy(tourlieferung.sort)
    }.map(tourlieferungMapping(tourlieferung)).list
  }

  protected def getTourlieferungenByKundeQuery(kundeId: KundeId) = {
    withSQL {
      select
        .from(tourlieferungMapping as tourlieferung)
        .where.eq(tourlieferung.kundeId, kundeId)
    }.map(tourlieferungMapping(tourlieferung)).list
  }

  protected def getTourlieferungenByTourQuery(tourId: TourId) = {
    withSQL {
      select
        .from(tourlieferungMapping as tourlieferung)
        .where.eq(tourlieferung.tourId, tourId)
    }.map(tourlieferungMapping(tourlieferung)).list
  }

  protected def getGeschaeftsjahreQuery = {
    sql"""WITH RECURSIVE seq AS (SELECT 1990 AS value UNION ALL SELECT value + 1 FROM seq WHERE value < YEAR(CURDATE()))
         SELECT p.geschaeftsjahr_tag as tag, p.geschaeftsjahr_monat as monat, s.value as jahr FROM seq as s, Projekt p where s.value >= (
         select
         if(month(min(abos.start)) < p.geschaeftsjahr_monat, year(min(abos.start)) - 1, year(min(abos.start)))
         from (select da.start as start from DepotlieferungAbo da UNION select ha.start as start from HeimlieferungAbo ha UNION select pa.start as start from PostlieferungAbo pa) as abos
        );""".map(rs => {
      GeschaeftsjahrStart(rs.int(1), rs.int(2), rs.int(3))
    }).list
  }

  protected def getProjektQuery = {
    withSQL {
      select
        .from(projektMapping as projekt)
    }.map(projektMapping(projekt)).single
  }

  @deprecated("Exists for compatibility purposes only", "OO 2.2 (Arbeitseinsatz)")
  protected def getProjektV1Query = {
    withSQL {
      select
        .from(projektV1Mapping as projektV1)
    }.map(projektV1Mapping(projektV1)).single
  }

  protected def getKontoDatenProjektQuery = {
    withSQL {
      select
        .from(kontoDatenMapping as kontoDaten)
        .where.isNull(kontoDaten.kunde)
    }.map(kontoDatenMapping(kontoDaten)).single
  }

  protected def getKontoDatenKundeQuery(kundeId: KundeId) = {
    withSQL {
      select
        .from(kontoDatenMapping as kontoDaten)
        .where.eq(kontoDaten.kunde, kundeId)
    }.map(kontoDatenMapping(kontoDaten)).single
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

  protected def getProduzentenabrechnungQuery(sammelbestellungIds: Seq[SammelbestellungId]) = {
    withSQL {
      select
        .from(sammelbestellungMapping as sammelbestellung)
        .join(produzentMapping as produzent).on(sammelbestellung.produzentId, produzent.id)
        .join(bestellungMapping as bestellung).on(bestellung.sammelbestellungId, sammelbestellung.id)
        .leftJoin(bestellpositionMapping as bestellposition).on(bestellposition.bestellungId, bestellung.id)
        .where.in(sammelbestellung.id, sammelbestellungIds.map(_.id))
    }.one(sammelbestellungMapping(sammelbestellung))
      .toManies(
        rs => produzentMapping.opt(produzent)(rs),
        rs => bestellungMapping.opt(bestellung)(rs),
        rs => bestellpositionMapping.opt(bestellposition)(rs)
      )
      .map((sammelbestellung, produzenten, bestellungen, positionen) => {
        val bestellungenDetails = bestellungen.sortBy(_.steuerSatz) map { b =>
          val p = positionen.filter(_.bestellungId == b.id).sortBy(_.produktBeschrieb)
          copyTo[Bestellung, BestellungDetail](b, "positionen" -> p)
        }
        copyTo[Sammelbestellung, SammelbestellungDetail](sammelbestellung, "produzent" -> produzenten.head, "bestellungen" -> bestellungenDetails)
      }).list
  }

  protected def getProdukteByProduktekategorieBezeichnungQuery(bezeichnung: String) = {
    withSQL {
      select
        .from(produktMapping as produkt)
        .where.like(produkt.kategorien, '%' + bezeichnung + '%')
    }.map(produktMapping(produkt)).list
  }

  protected def getLieferplanungenQuery(datumsFilter: Option[GeschaeftsjahrFilter]) = {
    withSQL {
      select
        .from(lieferplanungMapping as lieferplanung)
        .join(lieferungMapping as lieferung).on(lieferung.lieferplanungId, lieferplanung.id)
        .join(projektMapping as projekt)
        .where.append(
          getEntityDatumFilterQuery[Lieferung](lieferung, "datum", datumsFilter)
        )
    }.map(lieferplanungMapping(lieferplanung)).list
  }

  protected def getLatestLieferplanungQuery = {
    withSQL {
      select
        .from(lieferplanungMapping as lieferplanung)
        .orderBy(lieferplanung.id).desc
        .limit(1)
    }.map(lieferplanungMapping(lieferplanung)).single
  }

  protected def getOpenLieferplanungQuery = {
    withSQL {
      select
        .from(lieferplanungMapping as lieferplanung)
        .where.eq(lieferplanung.status, "Offen")
    }.map(lieferplanungMapping(lieferplanung)).list
  }

  protected def getLieferplanungQuery(id: LieferplanungId) = {
    withSQL {
      select
        .from(lieferplanungMapping as lieferplanung)
        .where.eq(lieferplanung.id, id)
    }.map(lieferplanungMapping(lieferplanung)).single
  }

  protected def getLieferplanungQuery(id: LieferungId) = {
    withSQL {
      select
        .from(lieferplanungMapping as lieferplanung)
        .join(lieferungMapping as lieferung).on(lieferung.lieferplanungId, lieferplanung.id)
        .where.eq(lieferung.id, id)
    }.map(lieferplanungMapping(lieferplanung)).single
  }

  protected def getLieferplanungReportQuery(id: LieferplanungId, projekt: ProjektReport) = {
    withSQL {
      select
        .from(lieferplanungMapping as lieferplanung)
        .join(lieferungMapping as lieferung).on(lieferung.lieferplanungId, lieferplanung.id)
        .leftJoin(abotypMapping as aboTyp).on(lieferung.abotypId, aboTyp.id)
        .leftJoin(zusatzAbotypMapping as zusatzAboTyp).on(lieferung.abotypId, zusatzAboTyp.id)
        .leftJoin(lieferpositionMapping as lieferposition).on(lieferposition.lieferungId, lieferung.id)
        .where.eq(lieferplanung.id, id)
        .and.not.withRoundBracket {
          _.eq(lieferung.anzahlKoerbeZuLiefern, 0)
            .and.isNull(aboTyp.id)
        }
    }.one(lieferplanungMapping(lieferplanung))
      .toManies(
        rs => lieferungMapping.opt(lieferung)(rs),
        rs => abotypMapping.opt(aboTyp)(rs),
        rs => zusatzAbotypMapping.opt(zusatzAboTyp)(rs),
        rs => lieferpositionMapping.opt(lieferposition)(rs)
      )
      .map { (lieferplanung, lieferungen, abotypen, zusatzabotypen, positionen) =>
        val lieferungenDetails = lieferungen map { l =>
          val p = positionen.filter(_.lieferungId == l.id)
          val iabotyp = (abotypen find (_.id == l.abotypId)) orElse (zusatzabotypen find (_.id == l.abotypId))
          copyTo[Lieferung, LieferungDetail](l, "abotyp" -> iabotyp, "lieferpositionen" -> p, "lieferplanungBemerkungen" -> lieferplanung.bemerkungen)
        }

        copyTo[Lieferplanung, LieferplanungReport](lieferplanung, "lieferungen" -> lieferungenDetails, "projekt" -> projekt)
      }.single
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
        .where.eq(lieferung.abotypId, abotypId).and.not.eq(lieferung.status, Ungeplant)
        .orderBy(lieferung.datum).desc
        .limit(1)
    }.map(lieferungMapping(lieferung)).single
  }

  protected def getLieferungenDetailsQuery(id: LieferplanungId) = {
    withSQL {
      select
        .from(lieferplanungMapping as lieferplanung)
        .join(lieferungMapping as lieferung).on(lieferplanung.id, lieferung.lieferplanungId)
        .leftJoin(abotypMapping as aboTyp).on(lieferung.abotypId, aboTyp.id)
        .leftJoin(zusatzAbotypMapping as zusatzAboTyp).on(lieferung.abotypId, zusatzAboTyp.id)
        .leftJoin(lieferpositionMapping as lieferposition).on(lieferposition.lieferungId, lieferung.id)
        .where.eq(lieferung.lieferplanungId, id)
        .and.not.withRoundBracket {
          _.eq(lieferung.anzahlKoerbeZuLiefern, 0)
            .and.eq(lieferung.anzahlAbwesenheiten, 0)
            .and.eq(lieferung.anzahlSaldoZuTief, 0)
            .and.isNull(aboTyp.id)
        }
    }.one(lieferungMapping(lieferung))
      .toManies(
        rs => abotypMapping.opt(aboTyp)(rs),
        rs => zusatzAbotypMapping.opt(zusatzAboTyp)(rs),
        rs => lieferpositionMapping.opt(lieferposition)(rs),
        rs => lieferplanungMapping.opt(lieferplanung)(rs)
      )
      .map { (lieferung, abotyp, zusatzAbotyp, positionen, lieferplanung) =>
        val bemerkung = lieferplanung match {
          case Nil => None
          case x   => x.head.bemerkungen
        }
        val iabotyp = abotyp.headOption orElse zusatzAbotyp.headOption
        copyTo[Lieferung, LieferungDetail](lieferung, "abotyp" -> iabotyp, "lieferpositionen" -> positionen, "lieferplanungBemerkungen" -> bemerkung)
      }.list
  }

  protected def getLieferungQuery(id: AbwesenheitId) = {
    withSQL {
      select
        .from(lieferungMapping as lieferung)
        .join(abwesenheitMapping as abwesenheit)
        .where.eq(lieferung.id, abwesenheit.lieferungId).and.eq(abwesenheit.id, id)
    }.map(lieferungMapping(lieferung)).single
  }

  protected def getExistingZusatzaboLieferungQuery(zusatzAbotypId: AbotypId, lieferplanungId: LieferplanungId, datum: DateTime) = {
    withSQL {
      select
        .from(lieferungMapping as lieferung)
        .where.eq(lieferung.abotypId, zusatzAbotypId)
        .and.eq(lieferung.lieferplanungId, lieferplanungId)
        .and.eq(lieferung.datum, datum)
    }.map(lieferungMapping(lieferung)).single
  }

  protected def getLieferungenQuery(id: LieferplanungId) = {
    withSQL {
      select
        .from(lieferungMapping as lieferung)
        .where.eq(lieferung.lieferplanungId, id)
        .orderBy(lieferung.datum).desc
    }.map(lieferungMapping(lieferung)).list
  }

  protected def getLieferungenQuery(id: VertriebId) = {
    withSQL {
      select
        .from(lieferungMapping as lieferung)
        .where.eq(lieferung.vertriebId, id)
    }.map(lieferungMapping(lieferung)).list
  }

  protected def sumPreisTotalGeplanteLieferungenVorherQuery(vertriebId: VertriebId, abotypId: AbotypId, datum: DateTime, startGeschaeftsjahr: DateTime) = {
    sql"""
      select
        sum(${lieferung.preisTotal})
      from
        ${lieferungMapping as lieferung}
      where
        ${lieferung.vertriebId} = ${vertriebId.id}
        and ${lieferung.abotypId} = ${abotypId.id}
        and ${lieferung.lieferplanungId} IS NOT NULL
        and ${lieferung.datum} < ${datum}
        and ${lieferung.datum} >= ${startGeschaeftsjahr}
      """
      .map(x => BigDecimal(x.bigDecimalOpt(1).getOrElse(java.math.BigDecimal.ZERO))).single
  }

  protected def getGeplanteLieferungVorherQuery(vertriebId: VertriebId, abotypId: AbotypId, datum: DateTime) = {
    withSQL {
      select
        .from(lieferungMapping as lieferung)
        .where.eq(lieferung.vertriebId, vertriebId)
        .and.eq(lieferung.abotypId, abotypId)
        .and.not.isNull(lieferung.lieferplanungId)
        .and.lt(lieferung.datum, datum)
        .orderBy(lieferung.datum).desc
        .limit(1)
    }.map(lieferungMapping(lieferung)).single
  }

  protected def getGeplanteLieferungNachherQuery(vertriebId: VertriebId, abotypId: AbotypId, datum: DateTime) = {
    withSQL {
      select
        .from(lieferungMapping as lieferung)
        .where.eq(lieferung.vertriebId, vertriebId)
        .and.eq(lieferung.abotypId, abotypId)
        .and.not.isNull(lieferung.lieferplanungId)
        .and.gt(lieferung.datum, datum)
        .orderBy(lieferung.datum).asc
        .limit(1)
    }.map(lieferungMapping(lieferung)).single
  }

  protected def countEarlierLieferungOffenQuery(id: LieferplanungId) = {
    sql"""
        SELECT
        count(*)
        FROM ${lieferungMapping as lieferung}
        WHERE ${lieferung.status} = 'Offen'
        AND ${lieferung.lieferplanungId} <> ${id.id}
        AND ${lieferung.datum} <
        (
          SELECT
          MIN(${lieferung.datum})
          FROM ${lieferungMapping as lieferung}
          WHERE ${lieferung.status} = 'Offen'
          AND ${lieferung.lieferplanungId} = ${id.id}
        )
      """
      .map(_.int(1)).single
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
		       INNER JOIN ${vertriebMapping as vertrieb}
		       ON ${lieferung.vertriebId} = ${vertrieb.id}
		       AND ${lieferung.lieferplanungId} IS NULL
		       AND ${vertrieb.id} NOT IN (
		         SELECT DISTINCT ${lieferung.vertriebId}
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
        copyTo[Lieferung, LieferungDetail](lieferung, "abotyp" -> abotyp, "lieferpositionen" -> emptyPosition, "lieferplanungBemerkungen" -> None)
      }.list
  }

  protected def getSammelbestellungDetailsQuery(id: LieferplanungId) = {
    withSQL {
      select
        .from(sammelbestellungMapping as sammelbestellung)
        .join(produzentMapping as produzent).on(sammelbestellung.produzentId, produzent.id)
        .join(bestellungMapping as bestellung).on(bestellung.sammelbestellungId, sammelbestellung.id)
        .leftJoin(bestellpositionMapping as bestellposition).on(bestellposition.bestellungId, bestellung.id)
        .where.eq(sammelbestellung.lieferplanungId, id)
    }.one(sammelbestellungMapping(sammelbestellung))
      .toManies(
        rs => produzentMapping.opt(produzent)(rs),
        rs => bestellungMapping.opt(bestellung)(rs),
        rs => bestellpositionMapping.opt(bestellposition)(rs)
      )
      .map((sammelbestellung, produzenten, bestellungen, positionen) => {
        val bestellungenDetails = bestellungen map { b =>
          val p = positionen.filter(_.bestellungId == b.id)
          copyTo[Bestellung, BestellungDetail](b, "positionen" -> p)
        }
        copyTo[Sammelbestellung, SammelbestellungDetail](sammelbestellung, "produzent" -> produzenten.head, "bestellungen" -> bestellungenDetails)
      }).list
  }

  protected def getSammelbestellungenQuery(id: LieferplanungId) = {
    withSQL {
      select
        .from(sammelbestellungMapping as sammelbestellung)
        .where.eq(sammelbestellung.lieferplanungId, id)
    }.map(sammelbestellungMapping(sammelbestellung)).list
  }

  protected def getSammelbestellungenQuery(id: LieferungId) = {
    withSQL {
      select
        .from(sammelbestellungMapping as sammelbestellung)
        .join(lieferplanungMapping as lieferplanung).on(sammelbestellung.lieferplanungId, lieferplanung.id)
        .join(lieferungMapping as lieferung).on(lieferung.lieferplanungId, lieferplanung.id)
        .where.eq(lieferung.id, id)
    }.map(sammelbestellungMapping(sammelbestellung)).list
  }

  protected def getSammelbestellungenQuery(filter: Option[FilterExpr]) = {
    withSQL {
      select
        .from(sammelbestellungMapping as sammelbestellung)
        .where(UriQueryParamToSQLSyntaxBuilder.build(filter, sammelbestellung))
    }.map(sammelbestellungMapping(sammelbestellung)).list
  }

  protected def getSammelbestellungenByProduzentQuery(produzent: ProduzentId, lieferplanungId: LieferplanungId) = {
    withSQL {
      select
        .from(sammelbestellungMapping as sammelbestellung)
        .where.eq(sammelbestellung.produzentId, produzent)
        .and.eq(sammelbestellung.lieferplanungId, lieferplanungId)
    }.map(sammelbestellungMapping(sammelbestellung)).list
  }

  protected def getBestellungenQuery(filter: Option[FilterExpr]) = {
    withSQL {
      select
        .from(bestellungMapping as bestellung)
        .where(UriQueryParamToSQLSyntaxBuilder.build(filter, bestellung))
    }.map(bestellungMapping(bestellung)).list
  }

  protected def getSammelbestellungByProduzentLieferplanungDatumQuery(produzentId: ProduzentId, lieferplanungId: LieferplanungId, datum: DateTime) = {
    withSQL {
      select
        .from(sammelbestellungMapping as sammelbestellung)
        .where.eq(sammelbestellung.produzentId, produzentId)
        .and.eq(sammelbestellung.lieferplanungId, lieferplanungId)
        .and.eq(sammelbestellung.datum, datum)
    }.map(sammelbestellungMapping(sammelbestellung)).single
  }

  protected def getBestellungenQuery(id: SammelbestellungId) = {
    withSQL {
      select
        .from(bestellungMapping as bestellung)
        .where.eq(bestellung.sammelbestellungId, id)
    }.map(bestellungMapping(bestellung)).list
  }

  protected def getBestellungQuery(id: SammelbestellungId, adminProzente: BigDecimal) = {
    withSQL {
      select
        .from(bestellungMapping as bestellung)
        .where.eq(bestellung.sammelbestellungId, id).and.eq(bestellung.adminProzente, adminProzente)
    }.map(bestellungMapping(bestellung)).single
  }

  protected def getBestellpositionenQuery(id: BestellungId) = {
    withSQL {
      select
        .from(bestellpositionMapping as bestellposition)
        .where.eq(bestellposition.bestellungId, id)
    }.map(bestellpositionMapping(bestellposition)).list
  }

  protected def getBestellpositionenBySammelbestellungQuery(id: SammelbestellungId) = {
    withSQL {
      select
        .from(bestellpositionMapping as bestellposition)
        .join(bestellungMapping as bestellung).on(bestellung.id, bestellposition.bestellungId)
        .where.eq(bestellung.sammelbestellungId, id)
    }.map(bestellpositionMapping(bestellposition)).list
  }

  protected def getSammelbestellungDetailQuery(id: SammelbestellungId) = {
    withSQL {
      select
        .from(sammelbestellungMapping as sammelbestellung)
        .join(produzentMapping as produzent).on(sammelbestellung.produzentId, produzent.id)
        .join(bestellungMapping as bestellung).on(bestellung.sammelbestellungId, sammelbestellung.id)
        .leftJoin(bestellpositionMapping as bestellposition).on(bestellposition.bestellungId, bestellung.id)
        .where.eq(sammelbestellung.id, id)
    }.one(sammelbestellungMapping(sammelbestellung))
      .toManies(
        rs => produzentMapping.opt(produzent)(rs),
        rs => bestellungMapping.opt(bestellung)(rs),
        rs => bestellpositionMapping.opt(bestellposition)(rs)
      )
      .map((sammelbestellung, produzenten, bestellungen, positionen) => {
        val bestellungenDetails = bestellungen map { b =>
          val p = positionen.filter(_.bestellungId == b.id)
          copyTo[Bestellung, BestellungDetail](b, "positionen" -> p)
        }
        copyTo[Sammelbestellung, SammelbestellungDetail](sammelbestellung, "produzent" -> produzenten.head, "bestellungen" -> bestellungenDetails)
      }).single
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

  protected def getAboIdsQuery(lieferungId: LieferungId, korbStatus: KorbStatus) = {

    val statusL = korbStatus match {
      case WirdGeliefert => WirdGeliefert :: Geliefert :: Nil
      case _             => korbStatus :: Nil
    }

    withSQL {
      select(korb.aboId)
        .from(korbMapping as korb)
        .where.eq(korb.lieferungId, lieferungId)
        .and.in(korb.status, statusL)
    }.map(res => AboId(res.long(1))).list
  }

  protected def getAboIdsQuery(lieferplanungId: LieferplanungId, korbStatus: KorbStatus) = {

    val statusL = korbStatus match {
      case WirdGeliefert => WirdGeliefert :: Geliefert :: Nil
      case _             => korbStatus :: Nil
    }

    withSQL {
      select(korb.aboId)
        .from(korbMapping as korb)
        .leftJoin(lieferungMapping as lieferung).on(korb.lieferungId, lieferung.id)
        .where.eq(lieferung.lieferplanungId, lieferplanungId)
        .and.in(korb.status, statusL)
    }.map(res => AboId(res.long(1))).list
  }

  protected def getZusatzaboIdsQuery(lieferungId: LieferungId, korbStatus: KorbStatus) = {

    val statusL = korbStatus match {
      case WirdGeliefert => WirdGeliefert :: Geliefert :: Nil
      case _             => korbStatus :: Nil
    }

    withSQL {
      select(zusatzAbo.hauptAboId)
        .from(zusatzAboMapping as zusatzAbo)
        .join(korbMapping as korb).on(korb.aboId, zusatzAbo.id)
        .where.eq(korb.lieferungId, lieferungId)
        .and.in(korb.status, statusL)
    }.map(res => AboId(res.long(1))).list
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

  protected def getLieferpositionenByLieferplanAndProduzentQuery(id: LieferplanungId, produzentId: ProduzentId, datum: DateTime) = {
    withSQL {
      select
        .from(lieferpositionMapping as lieferposition)
        .leftJoin(lieferungMapping as lieferung).on(lieferposition.lieferungId, lieferung.id)
        .where.eq(lieferung.lieferplanungId, id).and.eq(lieferposition.produzentId, produzentId).and.eq(lieferung.datum, datum)
    }.map(lieferpositionMapping(lieferposition)).list
  }

  protected def getLieferpositionenByLieferungQuery(id: LieferungId) = {
    withSQL {
      select
        .from(lieferpositionMapping as lieferposition)
        .where.eq(lieferposition.lieferungId, id)
    }.map(lieferpositionMapping(lieferposition)).list
  }

  protected def getKorbQuery(lieferungId: LieferungId, aboId: AboId) = {
    withSQL {
      select
        .from(korbMapping as korb)
        .where.eq(korb.lieferungId, lieferungId)
        .and.eq(korb.aboId, aboId).and.not.eq(korb.status, Geliefert)
    }.map(korbMapping(korb)).single
  }

  protected def getZusatzAboKorbQuery(hauptlieferungId: LieferungId, hauptAboId: AboId) = {
    withSQL {
      select
        .from(korbMapping as korb)
        .innerJoin(zusatzAboMapping as zusatzAbo)
        .innerJoin(lieferungMapping as lieferung)
        .innerJoin(lieferungMapping as lieferungJoin).on(lieferungJoin.datum, lieferung.datum)
        .where.eq(korb.aboId, zusatzAbo.id)
        .and.eq(korb.lieferungId, lieferungJoin.id)
        .and.eq(zusatzAbo.hauptAboId, hauptAboId)
        .and.eq(lieferung.id, hauptlieferungId)
    }.map(korbMapping(korb)).list
  }

  protected def getKoerbeQuery(lieferungId: LieferungId) = {
    withSQL {
      select
        .from(korbMapping as korb)
        .where.eq(korb.lieferungId, lieferungId)
    }.map(korbMapping(korb)).list
  }

  protected def getNichtGelieferteKoerbeQuery(lieferungId: LieferungId) = {
    withSQL {
      select
        .from(korbMapping as korb)
        .where.eq(korb.lieferungId, lieferungId)
        .and.not.eq(korb.status, Geliefert)
    }.map(korbMapping(korb)).list
  }

  protected def getKoerbeQuery(aboId: AboId) = {
    withSQL {
      select
        .from(korbMapping as korb)
        .innerJoin(lieferungMapping as lieferung).on(lieferung.id, korb.lieferungId)
        .where.eq(korb.aboId, aboId)
    }.one(korbMapping(korb))
      .toMany(
        rs => lieferungMapping.opt(lieferung)(rs)
      )
      .map((korb, lieferung) => {
        copyTo[Korb, KorbLieferung](korb, "lieferung" -> lieferung.head)
      }).list
  }

  protected def getKoerbeQuery(datum: DateTime, vertriebsartIds: List[VertriebsartId], status: KorbStatus) = {
    withSQL {
      select
        .from(korbMapping as korb)
        .innerJoin(lieferungMapping as lieferung).on(lieferung.id, korb.lieferungId)
        .leftJoin(depotlieferungAboMapping as depotlieferungAbo).on(depotlieferungAbo.id, korb.aboId)
        .leftJoin(heimlieferungAboMapping as heimlieferungAbo).on(heimlieferungAbo.id, korb.aboId)
        .leftJoin(postlieferungAboMapping as postlieferungAbo).on(postlieferungAbo.id, korb.aboId)
        .leftJoin(zusatzAboMapping as zusatzAbo).on(zusatzAbo.id, korb.aboId)
        .where.eq(lieferung.datum, datum).and.eq(korb.status, status).and.
        withRoundBracket(_.in(depotlieferungAbo.vertriebsartId, vertriebsartIds.map(_.id)).
          or.in(heimlieferungAbo.vertriebsartId, vertriebsartIds.map(_.id)).
          or.in(postlieferungAbo.vertriebsartId, vertriebsartIds.map(_.id)).
          or.in(zusatzAbo.vertriebsartId, vertriebsartIds.map(_.id)))
    }.one(korbMapping(korb))
      .toManies(
        rs => lieferungMapping.opt(lieferung)(rs),
        rs => depotlieferungAboMapping.opt(depotlieferungAbo)(rs),
        rs => heimlieferungAboMapping.opt(heimlieferungAbo)(rs),
        rs => postlieferungAboMapping.opt(postlieferungAbo)(rs),
        rs => zusatzAboMapping.opt(zusatzAbo)(rs)
      )
      .map { (korb, _, _, _, _, _) => korb }
      .list
  }

  protected def getKoerbeQuery(auslieferungId: AuslieferungId) = {
    withSQL {
      select
        .from(korbMapping as korb)
        .where.eq(korb.auslieferungId, auslieferungId)
    }.map(korbMapping(korb))
      .list
  }

  protected def getKoerbeNichtAusgeliefertLieferungClosedByAboQuery(aboId: AboId) = {
    withSQL {
      select
        .from(korbMapping as korb)
        .leftJoin(lieferungMapping as lieferung).on(lieferung.id, korb.lieferungId)
        .where.eq(korb.aboId, aboId).and.eq(korb.status, WirdGeliefert).and.not.eq(lieferung.status, Offen)
        .orderBy(lieferung.datum)
    }.map(korbMapping(korb))
      .list
  }

  protected def getKoerbeNichtAusgeliefertByAboQuery(aboId: AboId) = {
    withSQL {
      select
        .from(korbMapping as korb)
        .leftJoin(lieferungMapping as lieferung).on(lieferung.id, korb.lieferungId)
        .where.eq(korb.aboId, aboId).and.eq(lieferung.status, Offen)
        .orderBy(lieferung.datum)
    }.map(korbMapping(korb))
      .list
  }

  protected def getKorbLatestWirdGeliefertQuery(aboId: AboId, beforeDate: DateTime) = {
    withSQL {
      select
        .from(korbMapping as korb)
        .leftJoin(lieferungMapping as lieferung).on(lieferung.id, korb.lieferungId)
        .where.eq(korb.aboId, aboId).and.eq(korb.status, WirdGeliefert)
        .and.lt(lieferung.datum, beforeDate)
        .orderBy(lieferung.datum).desc.limit(1)
    }.map(korbMapping(korb))
      .single
  }

  protected def getKorbeLaterWirdGeliefertQuery(korbId: KorbId) = {
    withSQL {
      select
        .from(korbMapping as korb)
        .leftJoin(korbMapping as korbSecond).on(korb.aboId, korbSecond.aboId)
        .leftJoin(lieferungMapping as lieferung).on(lieferung.id, korb.lieferungId)
        .leftJoin(lieferungMapping as lieferungSecond).on(lieferungSecond.id, korbSecond.lieferungId)
        .where.eq(korbSecond.id, korbId).and.eq(korb.status, WirdGeliefert)
        .and.gt(lieferung.datum, lieferungSecond.datum)
    }.map(korbMapping(korb))
      .list
  }

  protected def getDepotAuslieferungenQuery(filter: Option[FilterExpr], datumsFilter: Option[GeschaeftsjahrFilter]) = {
    withSQL {
      select
        .from(depotAuslieferungMapping as depotAuslieferung)
        .join(projektMapping as projekt)
        .where.append(
          getEntityDatumFilterQuery[DepotAuslieferung](depotAuslieferung, "datum", datumsFilter)
        ).and(
            UriQueryParamToSQLSyntaxBuilder.build(filter, depotAuslieferung)
          )
    }.map(depotAuslieferungMapping(depotAuslieferung)).list
  }

  protected def getTourAuslieferungenQuery(filter: Option[FilterExpr], datumsFilter: Option[GeschaeftsjahrFilter]) = {
    withSQL {
      select
        .from(tourAuslieferungMapping as tourAuslieferung)
        .join(projektMapping as projekt)
        .where.append(
          getEntityDatumFilterQuery[TourAuslieferung](tourAuslieferung, "datum", datumsFilter)
        ).and(
            UriQueryParamToSQLSyntaxBuilder.build(filter, tourAuslieferung)
          )
    }.map(tourAuslieferungMapping(tourAuslieferung)).list
  }

  protected def getPostAuslieferungenQuery(filter: Option[FilterExpr], datumsFilter: Option[GeschaeftsjahrFilter]) = {
    withSQL {
      select
        .from(postAuslieferungMapping as postAuslieferung)
        .join(projektMapping as projekt)
        .where.append(
          getEntityDatumFilterQuery[PostAuslieferung](postAuslieferung, "datum", datumsFilter)
        ).and(
            UriQueryParamToSQLSyntaxBuilder.build(filter, postAuslieferung)
          )
    }.map(postAuslieferungMapping(postAuslieferung)).list
  }

  protected def getDepotAuslieferungenQuery(lieferplanungId: LieferplanungId) = {
    withSQL {
      select
        .from(depotAuslieferungMapping as depotAuslieferung)
        .join(korbMapping as korb).on(korb.auslieferungId, depotAuslieferung.id)
        .join(lieferungMapping as lieferung).on(korb.lieferungId, lieferung.id)
        .where.eq(lieferung.lieferplanungId, lieferplanungId)
    }.map(depotAuslieferungMapping(depotAuslieferung)).list
  }

  protected def getTourAuslieferungenQuery(lieferplanungId: LieferplanungId) = {
    withSQL {
      select
        .from(tourAuslieferungMapping as tourAuslieferung)
        .join(korbMapping as korb).on(korb.auslieferungId, tourAuslieferung.id)
        .join(lieferungMapping as lieferung).on(korb.lieferungId, lieferung.id)
        .where.eq(lieferung.lieferplanungId, lieferplanungId)
    }.map(tourAuslieferungMapping(tourAuslieferung)).list
  }

  protected def getPostAuslieferungenQuery(lieferplanungId: LieferplanungId) = {
    withSQL {
      select
        .from(postAuslieferungMapping as postAuslieferung)
        .join(korbMapping as korb).on(korb.auslieferungId, postAuslieferung.id)
        .join(lieferungMapping as lieferung).on(korb.lieferungId, lieferung.id)
        .where.eq(lieferung.lieferplanungId, lieferplanungId)
    }.map(postAuslieferungMapping(postAuslieferung)).list
  }

  protected def getDepotAuslieferungDetailQuery(auslieferungId: AuslieferungId) = {
    getDepotAuslieferungQuery(auslieferungId) { (auslieferung, depot, koerbe, lieferposition, abos, abotypen, _, kunden, _, zusatzAbos, _) =>
      val korbDetails = getKorbDetails(koerbe, abos, abotypen, kunden, zusatzAbos)

      copyTo[DepotAuslieferung, DepotAuslieferungDetail](auslieferung, "depot" -> depot, "koerbe" -> korbDetails)
    }
  }

  protected def getDepotAuslieferungReportQuery(auslieferungId: AuslieferungId, projekt: ProjektReport) = {
    getDepotAuslieferungQuery(auslieferungId) { (auslieferung, depot, koerbe, lieferpositionen, abos, abotypen, zusatzAbotypen, kunden, personen, zusatzAbos, lieferinfos) =>
      val korbReports = getKorbReports(koerbe, lieferpositionen, abos, abotypen, zusatzAbotypen, kunden, personen, zusatzAbos, lieferinfos).sortBy(_.abotyp.name)

      val depotReport = copyTo[Depot, DepotReport](depot)
      copyTo[DepotAuslieferung, DepotAuslieferungReport](auslieferung, "depot" -> depotReport, "koerbe" -> korbReports, "projekt" -> projekt)
    }
  }

  private def getDepotAuslieferungQuery[A](auslieferungId: AuslieferungId)(f: (DepotAuslieferung, Depot, Seq[Korb], Seq[Lieferposition], Seq[DepotlieferungAbo], Seq[Abotyp], Seq[ZusatzAbotyp], Seq[Kunde], Seq[PersonDetail], Seq[ZusatzAbo], Seq[Pendenz]) => A) = {
    withSQL {
      select
        .from(depotAuslieferungMapping as depotAuslieferung)
        .join(depotMapping as depot).on(depotAuslieferung.depotId, depot.id)
        .leftJoin(korbMapping as korb).on(korb.auslieferungId, depotAuslieferung.id)
        .leftJoin(lieferpositionMapping as lieferposition).on(lieferposition.lieferungId, korb.lieferungId)
        .leftJoin(depotlieferungAboMapping as depotlieferungAbo).on(korb.aboId, depotlieferungAbo.id)
        .leftJoin(abotypMapping as aboTyp).on(depotlieferungAbo.abotypId, aboTyp.id)
        .leftJoin(kundeMapping as kunde).on(depotlieferungAbo.kundeId, kunde.id)
        .leftJoin(personMapping as person).on(kunde.id, person.kundeId)
        .leftJoin(zusatzAboMapping as zusatzAbo).on(sqls"${depotlieferungAbo.id} = ${zusatzAbo.hauptAboId} AND ((${zusatzAbo.ende} IS NULL AND DATE(${depotAuslieferung.datum}) >= ${zusatzAbo.start}) OR DATE(${depotAuslieferung.datum}) between ${zusatzAbo.start} AND ${zusatzAbo.ende})")
        .leftJoin(zusatzAbotypMapping as zusatzAboTyp).on(zusatzAboTyp.id, zusatzAbo.abotypId)
        .leftJoin(pendenzMapping as pendenz).on(sqls"${pendenz.kundeId} = ${kunde.id} AND ${pendenz.status} = 'Lieferinformation'")
        .where.eq(depotAuslieferung.id, auslieferungId)
        .orderBy(kunde.bezeichnung)
    }.one(depotAuslieferungMapping(depotAuslieferung))
      .toManies(
        rs => depotMapping.opt(depot)(rs),
        rs => korbMapping.opt(korb)(rs),
        rs => lieferpositionMapping.opt(lieferposition)(rs),
        rs => depotlieferungAboMapping.opt(depotlieferungAbo)(rs),
        rs => abotypMapping.opt(aboTyp)(rs),
        rs => kundeMapping.opt(kunde)(rs),
        rs => personMapping.opt(person)(rs),
        rs => zusatzAboMapping.opt(zusatzAbo)(rs),
        rs => zusatzAbotypMapping.opt(zusatzAboTyp)(rs),
        rs => pendenzMapping.opt(pendenz)(rs)
      )
      .map((auslieferung, depots, koerbe, lieferpositionen, abos, abotypen, kunden, personen, zusatzAbos, zusatzAbotypen, pendenzen) => {
        val personenDetails = personen map (p => copyTo[Person, PersonDetail](p))
        f(auslieferung, depots.head, koerbe, lieferpositionen, abos, abotypen, zusatzAbotypen, kunden, personenDetails.distinct, zusatzAbos, pendenzen.distinct)
      }).single

  }

  protected def getTourAuslieferungDetailQuery(auslieferungId: AuslieferungId) = {
    getTourAuslieferungQuery(auslieferungId) { (auslieferung, tour, koerbe, lieferpositionen, abos, abotypen, _, kunden, _, zusatzAbos, _) =>
      val korbDetails = getKorbDetails(koerbe, abos, abotypen, kunden, zusatzAbos)
      copyTo[TourAuslieferung, TourAuslieferungDetail](auslieferung, "tour" -> tour, "koerbe" -> korbDetails)
    }
  }

  protected def getTourAuslieferungReportQuery(auslieferungId: AuslieferungId, projekt: ProjektReport) = {
    getTourAuslieferungQuery(auslieferungId) { (auslieferung, tour, koerbe, lieferpositionen, abos, abotypen, zusatzAbotypen, kunden, personen, zusatzAbos, lieferinfos) =>
      val korbReports = getKorbReports(koerbe, lieferpositionen, abos, abotypen, zusatzAbotypen, kunden, personen, zusatzAbos, lieferinfos).sortBy(_.sort)
      copyTo[TourAuslieferung, TourAuslieferungReport](auslieferung, "tour" -> tour, "koerbe" -> korbReports, "projekt" -> projekt)
    }
  }

  private def getTourAuslieferungQuery[A](auslieferungId: AuslieferungId)(f: (TourAuslieferung, Tour, Seq[Korb], Seq[Lieferposition], Seq[HeimlieferungAbo], Seq[Abotyp], Seq[ZusatzAbotyp], Seq[Kunde], Seq[PersonDetail], Seq[ZusatzAbo], Seq[Pendenz]) => A) = {
    withSQL {
      select
        .from(tourAuslieferungMapping as tourAuslieferung)
        .join(tourMapping as tour).on(tourAuslieferung.tourId, tour.id)
        .leftJoin(korbMapping as korb).on(korb.auslieferungId, tourAuslieferung.id)
        .leftJoin(lieferpositionMapping as lieferposition).on(lieferposition.lieferungId, korb.lieferungId)
        .leftJoin(heimlieferungAboMapping as heimlieferungAbo).on(korb.aboId, heimlieferungAbo.id)
        .leftJoin(abotypMapping as aboTyp).on(heimlieferungAbo.abotypId, aboTyp.id)
        .leftJoin(kundeMapping as kunde).on(heimlieferungAbo.kundeId, kunde.id)
        .leftJoin(personMapping as person).on(kunde.id, person.kundeId)
        .leftJoin(zusatzAboMapping as zusatzAbo).on(sqls"${heimlieferungAbo.id} = ${zusatzAbo.hauptAboId} AND ((${zusatzAbo.ende} IS NULL AND DATE(${tourAuslieferung.datum}) >= ${zusatzAbo.start}) OR DATE(${tourAuslieferung.datum}) between ${zusatzAbo.start} AND ${zusatzAbo.ende})")
        .leftJoin(zusatzAbotypMapping as zusatzAboTyp).on(zusatzAboTyp.id, zusatzAbo.abotypId)
        .leftJoin(pendenzMapping as pendenz).on(sqls"${pendenz.kundeId} = ${kunde.id} AND ${pendenz.status} = 'Lieferinformation'")
        .where.eq(tourAuslieferung.id, auslieferungId)
        .orderBy(korb.sort)
    }.one(tourAuslieferungMapping(tourAuslieferung))
      .toManies(
        rs => tourMapping.opt(tour)(rs),
        rs => korbMapping.opt(korb)(rs),
        rs => lieferpositionMapping.opt(lieferposition)(rs),
        rs => heimlieferungAboMapping.opt(heimlieferungAbo)(rs),
        rs => abotypMapping.opt(aboTyp)(rs),
        rs => kundeMapping.opt(kunde)(rs),
        rs => personMapping.opt(person)(rs),
        rs => zusatzAboMapping.opt(zusatzAbo)(rs),
        rs => zusatzAbotypMapping.opt(zusatzAboTyp)(rs),
        rs => pendenzMapping.opt(pendenz)(rs)
      )
      .map((auslieferung, tour, koerbe, lieferposition, abos, abotypen, kunden, personen, zusatzAbos, zusatzAbotypen, pendenzen) => {
        val personenDetails = personen map (p => copyTo[Person, PersonDetail](p))
        f(auslieferung, tour.head, koerbe, lieferposition, abos, abotypen, zusatzAbotypen, kunden, personenDetails.distinct, zusatzAbos, pendenzen.distinct)
      }).single

  }

  protected def getPostAuslieferungDetailQuery(auslieferungId: AuslieferungId) = {
    getPostAuslieferungQuery(auslieferungId) { (auslieferung, koerbe, lieferpositionen, abos, abotypen, _, kunden, _, zusatzAbos, _) =>
      val korbDetails = getKorbDetails(koerbe, abos, abotypen, kunden, zusatzAbos)

      copyTo[PostAuslieferung, PostAuslieferungDetail](auslieferung, "koerbe" -> korbDetails)
    }
  }

  protected def getPostAuslieferungReportQuery(auslieferungId: AuslieferungId, projekt: ProjektReport) = {
    getPostAuslieferungQuery(auslieferungId) { (auslieferung, koerbe, lieferpositionen, abos, abotypen, zusatzAbotypen, kunden, personen, zusatzAbos, lieferinfos) =>
      val korbReports = getKorbReports(koerbe, lieferpositionen, abos, abotypen, zusatzAbotypen, kunden, personen, zusatzAbos, lieferinfos).sortBy(_.abotyp.name)

      copyTo[PostAuslieferung, PostAuslieferungReport](auslieferung, "koerbe" -> korbReports, "projekt" -> projekt)
    }
  }

  private def getPostAuslieferungQuery[A](auslieferungId: AuslieferungId)(f: (PostAuslieferung, Seq[Korb], Seq[Lieferposition], Seq[PostlieferungAbo], Seq[Abotyp], Seq[ZusatzAbotyp], Seq[Kunde], Seq[PersonDetail], Seq[ZusatzAbo], Seq[Pendenz]) => A) = {
    withSQL {
      select
        .from(postAuslieferungMapping as postAuslieferung)
        .leftJoin(korbMapping as korb).on(korb.auslieferungId, postAuslieferung.id)
        .leftJoin(lieferpositionMapping as lieferposition).on(lieferposition.lieferungId, korb.lieferungId)
        .leftJoin(postlieferungAboMapping as postlieferungAbo).on(korb.aboId, postlieferungAbo.id)
        .leftJoin(abotypMapping as aboTyp).on(postlieferungAbo.abotypId, aboTyp.id)
        .leftJoin(kundeMapping as kunde).on(postlieferungAbo.kundeId, kunde.id)
        .leftJoin(personMapping as person).on(kunde.id, person.kundeId)
        .leftJoin(zusatzAboMapping as zusatzAbo).on(sqls"${postlieferungAbo.id} = ${zusatzAbo.hauptAboId} AND ((${zusatzAbo.ende} IS NULL AND DATE(${postAuslieferung.datum}) >= ${zusatzAbo.start}) OR DATE(${postAuslieferung.datum}) between ${zusatzAbo.start} AND ${zusatzAbo.ende})")
        .leftJoin(zusatzAbotypMapping as zusatzAboTyp).on(zusatzAboTyp.id, zusatzAbo.abotypId)
        .leftJoin(pendenzMapping as pendenz).on(sqls"${pendenz.kundeId} = ${kunde.id} AND ${pendenz.status} = 'Lieferinformation'")
        .where.eq(postAuslieferung.id, auslieferungId)
    }.one(postAuslieferungMapping(postAuslieferung))
      .toManies(
        rs => korbMapping.opt(korb)(rs),
        rs => lieferpositionMapping.opt(lieferposition)(rs),
        rs => postlieferungAboMapping.opt(postlieferungAbo)(rs),
        rs => abotypMapping.opt(aboTyp)(rs),
        rs => kundeMapping.opt(kunde)(rs),
        rs => personMapping.opt(person)(rs),
        rs => zusatzAboMapping.opt(zusatzAbo)(rs),
        rs => zusatzAbotypMapping.opt(zusatzAboTyp)(rs),
        rs => pendenzMapping.opt(pendenz)(rs)
      )
      .map((auslieferung, koerbe, lieferpositionen, abos, abotypen, kunden, personen, zusatzAbos, zusatzAbotypen, pendenzen) => {
        val personenDetails = personen map (p => copyTo[Person, PersonDetail](p))
        f(auslieferung, koerbe, lieferpositionen, abos, abotypen, zusatzAbotypen, kunden, personenDetails.distinct, zusatzAbos, pendenzen.distinct)
      }).single
  }

  private def getKorbDetails(koerbe: Seq[Korb], abos: Seq[HauptAbo], abotypen: Seq[Abotyp], kunden: Seq[Kunde], zusatzAbos: Seq[ZusatzAbo]): Seq[KorbDetail] = {
    koerbe.map { korb =>
      for {
        korbAbo <- abos.filter(_.id == korb.aboId).headOption
        abotyp <- abotypen.filter(_.id == korbAbo.abotypId).headOption
        kunde <- kunden.filter(_.id == korbAbo.kundeId).headOption
        zs = (zusatzAbos filter (_.hauptAboId == korbAbo.id))
        zusatzAbotypNames = zs.map(_.abotypName)
        zusatzAbotypIds = zs.map(_.id).toSet
        korbAboZ = korbAbo match {
          case abo: DepotlieferungAbo => copyTo[DepotlieferungAbo, DepotlieferungAbo](abo, "zusatzAbotypNames" -> zusatzAbotypNames, "zusatzAboIds" -> zusatzAbotypIds)
          case abo: HeimlieferungAbo  => copyTo[HeimlieferungAbo, HeimlieferungAbo](abo, "zusatzAbotypNames" -> zusatzAbotypNames, "zusatzAboIds" -> zusatzAbotypIds)
          case abo: PostlieferungAbo  => copyTo[PostlieferungAbo, PostlieferungAbo](abo, "zusatzAbotypNames" -> zusatzAbotypNames, "zusatzAboIds" -> zusatzAbotypIds)
        }
        zusatzKoerbe = koerbe filter (k => zs.contains(k.aboId)) map (zk => copyTo[Korb, ZusatzKorbDetail](zk, "abo" -> korbAbo, "abotyp" -> abotyp, "kunde" -> kunde, "zusatzKoerbe" -> Nil))
      } yield copyTo[Korb, KorbDetail](korb, "abo" -> korbAboZ, "abotyp" -> abotyp, "kunde" -> kunde, "zusatzKoerbe" -> zusatzKoerbe)
    }.flatten
  }

  private def getKorbReports(koerbe: Seq[Korb], lieferpositionen: Seq[Lieferposition], abos: Seq[HauptAbo], abotypen: Seq[Abotyp], zusatzAbotypen: Seq[ZusatzAbotyp], kunden: Seq[Kunde], personen: Seq[PersonDetail], zusatzAbos: Seq[ZusatzAbo], lieferinfos: Seq[Pendenz]): Seq[KorbReport] = {
    koerbe.flatMap { korb =>
      for {
        korbAbo <- abos.filter(_.id == korb.aboId).headOption
        abotyp <- abotypen.filter(_.id == korbAbo.abotypId).headOption
        kunde <- kunden.filter(_.id == korbAbo.kundeId).headOption
      } yield {
        val ansprechpersonen = personen.filter(_.kundeId == kunde.id)
        val zs = (zusatzAbos filter (_.hauptAboId == korbAbo.id))
        val zusatzAboReports = zs map (z => {
          val zat = zusatzAbotypen.find(_.id == z.abotypId).get
          copyTo[ZusatzAbo, ZusatzAboReport](z, "abotyp" -> zat)
        })
        val zusatzAbosString = (zs filter (_.hauptAboId == korbAbo.id) map (_.abotypName)).mkString(", ")
        val zusatzAbosAggregatedString = zs.groupBy(_.abotypName).mapValues(_.size).map(a => a._2 + "x " + a._1).mkString(", ")
        val zusatzAbosList = (zs filter (_.hauptAboId == korbAbo.id))
        val kundeReport = copyTo[Kunde, KundeReport](kunde, "personen" -> ansprechpersonen)
        val lp = lieferpositionen flatMap {
          lieferposition =>
            {
              val zusatzKoerbe = zusatzAbosList flatMap {
                zusatzabo => koerbe.filter(_.aboId == zusatzabo.id)
              }

              val isInZusatzKorb = zusatzKoerbe.filter(_.lieferungId == lieferposition.lieferungId).nonEmpty

              if (lieferposition.lieferungId == korb.lieferungId || isInZusatzKorb) {
                Some(lieferposition)
              } else None
            }
        }
        val zusatzAbotypNames = zs.map(_.abotypName)
        val korbAboZ = korbAbo match {
          case abo: DepotlieferungAbo => copyTo[DepotlieferungAbo, DepotlieferungAbo](abo, "zusatzAbotypNames" -> zusatzAbotypNames)
          case abo: HeimlieferungAbo  => copyTo[HeimlieferungAbo, HeimlieferungAbo](abo, "zusatzAbotypNames" -> zusatzAbotypNames)
          case abo: PostlieferungAbo  => copyTo[PostlieferungAbo, PostlieferungAbo](abo, "zusatzAbotypNames" -> zusatzAbotypNames)
        }
        val li = (lieferinfos filter (_.kundeId == kunde.id))
        copyTo[Korb, KorbReport](korb, "abo" -> korbAboZ, "abotyp" -> abotyp, "kunde" -> kundeReport, "zusatzAbos" -> zusatzAboReports, "zusatzAbosString" -> zusatzAbosString, "zusatzAbosAggregatedString" -> zusatzAbosAggregatedString, "lieferpositionen" -> lp, "lieferinformation" -> li)
      }
    }
  }

  protected def getDepotAuslieferungQuery(depotId: DepotId, datum: DateTime) = {
    withSQL {
      select
        .from(depotAuslieferungMapping as depotAuslieferung)
        .where.eq(depotAuslieferung.depotId, depotId).and.eq(depotAuslieferung.datum, datum)
    }.map(depotAuslieferungMapping(depotAuslieferung)).single
  }

  protected def getTourAuslieferungQuery(tourId: TourId, datum: DateTime) = {
    withSQL {
      select
        .from(tourAuslieferungMapping as tourAuslieferung)
        .where.eq(tourAuslieferung.tourId, tourId).and.eq(tourAuslieferung.datum, datum)
    }.map(tourAuslieferungMapping(tourAuslieferung)).single
  }

  protected def getPostAuslieferungQuery(datum: DateTime) = {
    withSQL {
      select
        .from(postAuslieferungMapping as postAuslieferung)
        .where.eq(postAuslieferung.datum, datum)
    }.map(postAuslieferungMapping(postAuslieferung)).single
  }

  protected def getVertriebQuery(vertriebId: VertriebId) = {
    withSQL {
      select
        .from(vertriebMapping as vertrieb)
        .where.eq(vertrieb.id, vertriebId)
    }.map(vertriebMapping(vertrieb)).single
  }

  protected def getVertriebByDateQuery(datum: DateTime) = {
    withSQL {
      select
        .from(vertriebMapping as vertrieb)
        .where.in(vertrieb.id, (select(lieferung.vertriebId)
          .from(lieferungMapping as lieferung)
          .where.eq(lieferung.datum, datum)))
    }.map(vertriebMapping(vertrieb)).list
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
          depots.find(_.id == lieferung.depotId).headOption map { depot =>
            val summary = copyTo[Depot, DepotSummary](depot)
            copyTo[Depotlieferung, DepotlieferungDetail](lieferung, "depot" -> summary)
          }
        }.flatten
        val hl = hls.map { lieferung =>
          touren.find(_.id == lieferung.tourId).headOption map { tour =>
            copyTo[Heimlieferung, HeimlieferungDetail](lieferung, "tour" -> tour)
          }
        }.flatten

        copyTo[Vertrieb, VertriebVertriebsarten](vertrieb, "depotlieferungen" -> dl, "heimlieferungen" -> hl, "postlieferungen" -> pl)
      }).list
  }

  protected def getVertriebeQuery() = {
    withSQL {
      select
        .from(vertriebMapping as vertrieb)
        .orderBy(vertrieb.beschrieb)
    }.map(vertriebMapping(vertrieb)).list
  }

  protected def getProjektVorlagenQuery() = {
    withSQL {
      select
        .from(projektVorlageMapping as projektVorlage)
        .orderBy(projektVorlage.name)
    }.map(projektVorlageMapping(projektVorlage)).list
  }

  protected def getEinladungQuery(token: String) = {
    withSQL {
      select
        .from(einladungMapping as einladung)
        .where.eq(einladung.uid, token)
    }.map(einladungMapping(einladung)).single
  }

  protected def getSingleDepotlieferungAboQuery(id: AboId) = {
    withSQL {
      select
        .from(depotlieferungAboMapping as depotlieferungAbo)
        .where.eq(depotlieferungAbo.id, id)
    }.map(depotlieferungAboMapping(depotlieferungAbo)).single
  }

  protected def getSingleHeimlieferungAboQuery(id: AboId) = {
    withSQL {
      select
        .from(heimlieferungAboMapping as heimlieferungAbo)
        .where.eq(heimlieferungAbo.id, id)
    }.map(heimlieferungAboMapping(heimlieferungAbo)).single
  }

  protected def getSinglePostlieferungAboQuery(id: AboId) = {
    withSQL {
      select
        .from(postlieferungAboMapping as postlieferungAbo)
        .where.eq(postlieferungAbo.id, id)
    }.map(postlieferungAboMapping(postlieferungAbo)).single
  }

  protected def getSingleZusatzAboQuery(id: AboId) = {
    withSQL {
      select
        .from(zusatzAboMapping as zusatzAbo)
        .where.eq(zusatzAbo.id, id)
    }.map(zusatzAboMapping(zusatzAbo)).single
  }

  protected def getZusatzAbosByHauptAboQuery(hauptaboId: AboId) = {
    withSQL {
      select
        .from(zusatzAboMapping as zusatzAbo)
        .where.eq(zusatzAbo.hauptAboId, hauptaboId)
    }.map(zusatzAboMapping(zusatzAbo)).list
  }

  protected def getZusatzAbosByZusatzabotypQuery(zusatzabotyp: AbotypId) = {
    withSQL {
      select
        .from(zusatzAboMapping as zusatzAbo)
        .where.eq(zusatzAbo.abotypId, zusatzabotyp)
    }.map(zusatzAboMapping(zusatzAbo)).list
  }

  protected def getDepotAbosByZusatzAboIdQuery(zusatzaboId: AboId) = {
    withSQL {
      select
        .from(depotlieferungAboMapping as depotlieferungAbo)
        .where.in(depotlieferungAbo.zusatzAboIds, Seq(zusatzaboId))
    }.map(depotlieferungAboMapping(depotlieferungAbo)).list
  }

  protected def getPostAbosByZusatzAboIdQuery(zusatzaboId: AboId) = {
    withSQL {
      select
        .from(postlieferungAboMapping as postlieferungAbo)
        .where.in(postlieferungAbo.zusatzAboIds, Seq(zusatzaboId))
    }.map(postlieferungAboMapping(postlieferungAbo)).list
  }

  protected def getHeimAbosByZusatzAboIdQuery(zusatzaboId: AboId) = {
    withSQL {
      select
        .from(heimlieferungAboMapping as heimlieferungAbo)
        .where.in(heimlieferungAbo.zusatzAboIds, Seq(zusatzaboId))
    }.map(heimlieferungAboMapping(heimlieferungAbo)).list
  }

  // MODIFY and DELETE Queries

  protected def deleteLieferpositionenQuery(id: LieferungId) = {
    withSQL {
      delete
        .from(lieferpositionMapping as lieferpositionShort)
        .where.eq(lieferpositionShort.lieferungId, id)
    }
  }

  protected def deleteKoerbeQuery(id: LieferungId) = {
    withSQL {
      delete
        .from(korbMapping as korbShort)
        .where.eq(korbShort.lieferungId, id)
    }
  }

  protected def deleteZusatzAbosQuery(hauptAboId: AboId) = {
    withSQL {
      delete
        .from(zusatzAboMapping as zusatzAboShort)
        .where.eq(zusatzAboShort.hauptAboId, hauptAboId)
    }
  }

  protected def getAktivierteAbosQuery = {
    val today = LocalDate.now.toDateTimeAtStartOfDay

    withSQL {
      select(depotlieferungAbo.id)
        .from(depotlieferungAboMapping as depotlieferungAbo)
        .where.le(depotlieferungAbo.start, today)
        .and.withRoundBracket { _.isNull(depotlieferungAbo.ende).or.ge(depotlieferungAbo.ende, today) }
        .and.eq(depotlieferungAbo.aktiv, false)
        .union(select(heimlieferungAbo.id).from(heimlieferungAboMapping as heimlieferungAbo)
          .where.le(heimlieferungAbo.start, today)
          .and.withRoundBracket { _.isNull(heimlieferungAbo.ende).or.ge(heimlieferungAbo.ende, today) }
          .and.eq(heimlieferungAbo.aktiv, false)).union(
          select(postlieferungAbo.id).from(postlieferungAboMapping as postlieferungAbo)
            .where.le(postlieferungAbo.start, today)
            .and.withRoundBracket { _.isNull(postlieferungAbo.ende).or.ge(postlieferungAbo.ende, today) }
            .and.eq(postlieferungAbo.aktiv, false)
        ).union(select(zusatzAbo.id).from(zusatzAboMapping as zusatzAbo)
            .where.le(zusatzAbo.start, today)
            .and.withRoundBracket { _.isNull(zusatzAbo.ende).or.ge(zusatzAbo.ende, today) }
            .and.eq(zusatzAbo.aktiv, false))
    }.map(res => AboId(res.long(1))).list
  }

  protected def getDeaktivierteAbosQuery = {
    val yesterday = LocalDate.now.minusDays(1).toDateTimeAtStartOfDay

    withSQL {
      select(depotlieferungAbo.id)
        .from(depotlieferungAboMapping as depotlieferungAbo)
        .where.le(depotlieferungAbo.start, yesterday)
        .and.withRoundBracket { _.isNotNull(depotlieferungAbo.ende).and.le(depotlieferungAbo.ende, yesterday) }
        .and.eq(depotlieferungAbo.aktiv, true)
        .union(select(heimlieferungAbo.id).from(heimlieferungAboMapping as heimlieferungAbo)
          .where.le(heimlieferungAbo.start, yesterday)
          .and.withRoundBracket { _.isNotNull(heimlieferungAbo.ende).and.le(heimlieferungAbo.ende, yesterday) }
          .and.eq(heimlieferungAbo.aktiv, true)).union(
          select(postlieferungAbo.id).from(postlieferungAboMapping as postlieferungAbo)
            .where.le(postlieferungAbo.start, yesterday)
            .and.withRoundBracket { _.isNotNull(postlieferungAbo.ende).and.le(postlieferungAbo.ende, yesterday) }
            .and.eq(postlieferungAbo.aktiv, true)
        ).union(
            select(zusatzAbo.id).from(zusatzAboMapping as zusatzAbo)
              .where.le(zusatzAbo.start, yesterday)
              .and.withRoundBracket { _.isNotNull(zusatzAbo.ende).and.le(zusatzAbo.ende, yesterday) }
              .and.eq(zusatzAbo.aktiv, true)
          )
    }.map(res => AboId(res.long(1))).list
  }

  protected def getLieferungenOffenOrAbgeschlossenByAbotypQuery(abotypId: AbotypId) = {
    withSQL {
      select
        .from(lieferungMapping as lieferung)
        .where.eq(lieferung.abotypId, abotypId)
        .and.withRoundBracket { _.eq(lieferung.status, Offen).or.eq(lieferung.status, Abgeschlossen) }
    }.map(lieferungMapping(lieferung)).list
  }

  protected def getLieferungenOffenByVertriebQuery(vertriebId: VertriebId) = {
    withSQL {
      select
        .from(lieferungMapping as lieferung)
        .where.eq(lieferung.vertriebId, vertriebId)
        .and.eq(lieferung.status, Offen)
    }.map(lieferungMapping(lieferung)).list
  }

  protected def getLieferungenOffenByAbotypQuery(abotypId: AbotypId) = {
    withSQL {
      select
        .from(lieferungMapping as lieferung)
        .where.eq(lieferung.abotypId, abotypId)
        .and.eq(lieferung.status, Offen)
    }.map(lieferungMapping(lieferung)).list
  }

  protected def getLastClosedLieferplanungenDetailQuery = {
    withSQL {
      select
        .from(lieferplanungMapping as lieferplanung)
        .leftJoin(lieferungMapping as lieferung).on(lieferung.lieferplanungId, lieferplanung.id)
        .leftJoin(lieferpositionMapping as lieferposition).on(lieferposition.lieferungId, lieferung.id)
        .where.withRoundBracket { _.eq(lieferplanung.status, Abgeschlossen).or.eq(lieferplanung.status, Verrechnet) }
        .orderBy(lieferplanung.id).desc
    }
      .one(lieferplanungMapping(lieferplanung))
      .toManies(
        rs => lieferungMapping.opt(lieferung)(rs),
        rs => lieferpositionMapping.opt(lieferposition)(rs)
      )
      .map((lieferplanung, lieferungen, lieferpositionen) => {
        val lieferungenDetails = lieferungen map { l =>
          val p = lieferpositionen.filter(_.lieferungId == l.id).map(p => copyTo[Lieferposition, LieferpositionOpen](p)).toSeq
          copyTo[Lieferung, LieferungOpenDetail](l, "lieferpositionen" -> p)
        }
        copyTo[Lieferplanung, LieferplanungOpenDetail](lieferplanung, "lieferungen" -> lieferungenDetails)
      }).list
  }

}
