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
import ch.openolitor.core.repositories.ReportReadRepository
import ch.openolitor.core.reporting.models._
import scalikejdbc._
import scalikejdbc.async.{ makeSQLToOptionAsync => _, makeSQLToListAsync => _, _ }

import scala.concurrent.ExecutionContext
import ch.openolitor.core.db._
import ch.openolitor.core.db.OOAsyncDB._
import ch.openolitor.core.repositories._

import scala.concurrent._
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.stammdaten.models._
import ch.openolitor.core.Macros._
import org.joda.time.DateTime
import ch.openolitor.util.IdUtil
import ch.openolitor.util.parsing.{ QueryFilter, GeschaeftsjahrFilter, FilterExpr }

trait StammdatenReadRepositoryAsync extends ReportReadRepository {

  def getAbotypen(implicit asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr], queryString: Option[QueryFilter]): Future[List[Abotyp]]
  def getAbotypDetail(id: AbotypId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Abotyp]]

  def getZusatzAbotypen(implicit asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr], queryString: Option[QueryFilter]): Future[List[ZusatzAbotyp]]
  def getZusatzAbotypDetail(id: AbotypId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[ZusatzAbotyp]]

  def getUngeplanteLieferungen(abotypId: AbotypId, vertriebId: VertriebId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Lieferung]]
  def getUngeplanteLieferungen(abotypId: AbotypId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Lieferung]]

  def getVertrieb(vertriebId: VertriebId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Vertrieb]]
  def getVertriebe(abotypId: AbotypId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[VertriebVertriebsarten]]
  def getVertriebe(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Vertrieb]]

  def getVertriebsart(vertriebsartId: VertriebsartId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[VertriebsartDetail]]
  def getVertriebsarten(vertriebId: VertriebId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[VertriebsartDetail]]

  def getKunden(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Kunde]]
  def getKundenUebersicht(implicit asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr], queryString: Option[QueryFilter]): Future[List[KundeUebersicht]]
  def getKundenSearch(implicit asyncCpContext: MultipleAsyncConnectionPoolContext, queryString: Option[QueryFilter]): Future[List[KundenSearch]]
  def getKundeDetail(id: KundeId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[KundeDetail]]
  def getKundeDetailReport(kundeId: KundeId, projekt: ProjektReport)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[KundeDetailReport]]
  def getKundeDetailsArbeitseinsatzReport(projekt: ProjektReport)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[KundeDetailArbeitseinsatzReport]]

  def getCustomKundentypen(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[CustomKundentyp]]

  def getPersonen(kundeId: KundeId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Person]]
  def getPersonByEmail(email: String)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Person]]
  def getPerson(id: PersonId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Person]]
  def getPersonCategory(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[PersonCategory]]
  def getPersonenUebersicht(implicit asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr], queryString: Option[QueryFilter]): Future[List[PersonUebersicht]]
  def getPersonenByDepots(implicit asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr]): Future[List[PersonSummary]]
  def getPersonenAboAktivByDepots(implicit asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr]): Future[List[PersonSummary]]
  def getPersonenByTouren(implicit asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr]): Future[List[PersonSummary]]
  def getPersonenAboAktivByTouren(implicit asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr]): Future[List[PersonSummary]]
  def getPersonenByAbotypen(implicit asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr]): Future[List[PersonSummary]]
  def getPersonenAboAktivByAbotypen(implicit asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr]): Future[List[PersonSummary]]
  def getPersonenZusatzAboAktivByZusatzAbotypen(implicit asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr], queryString: Option[QueryFilter]): Future[List[PersonSummary]]

  def getDepots(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Depot]]
  def getDepotDetail(id: DepotId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Depot]]
  def getDepotDetailReport(id: DepotId, projekt: ProjektReport)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[DepotDetailReport]]

  def getAbos(xFlags: Option[AbosComplexFlags])(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr], gjFilter: Option[GeschaeftsjahrFilter], queryString: Option[QueryFilter]): Future[List[Abo]]
  def getZusatzAbos(xFlags: Option[AbosComplexFlags])(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr], gjFilter: Option[GeschaeftsjahrFilter], queryString: Option[QueryFilter]): Future[List[ZusatzAbo]]
  def getAboDetail(id: AboId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[AboDetail]]
  def getZusatzAboDetail(id: AboId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[ZusatzAbo]]
  def getZusatzaboPerAbo(id: AboId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[ZusatzAbo]]

  def countAbwesend(lieferungId: LieferungId, aboId: AboId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Int]]

  def getPendenzen(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Pendenz]]
  def getPendenzen(id: KundeId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Pendenz]]
  def getPendenzDetail(id: PendenzId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Pendenz]]

  def getProdukte(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Produkt]]
  def getProdukteByProduktekategorieBezeichnung(bezeichnung: String)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Produkt]]
  def getProduktProduzenten(id: ProduktId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[ProduktProduzent]]
  def getProduktProduktekategorien(id: ProduktId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[ProduktProduktekategorie]]

  def getProduktekategorien(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Produktekategorie]]

  def getProduzenten(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr], queryString: Option[QueryFilter]): Future[List[Produzent]]
  def getProduzentDetail(id: ProduzentId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Produzent]]
  def getProduzentDetailReport(id: ProduzentId, projekt: ProjektReport)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[ProduzentDetailReport]]
  def getProduzentDetailByKurzzeichen(kurzzeichen: String)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Produzent]]
  def getProduktekategorieByBezeichnung(bezeichnung: String)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Produktekategorie]]

  def getProduzentenabrechnungReport(sammelbestellungIds: Seq[SammelbestellungId], projekt: ProjektReport)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[ProduzentenabrechnungReport]]

  def getTouren(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Tour]]
  def getTourDetail(id: TourId, aktiveOnly: Boolean)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[TourDetail]]

  def getProjekt(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Projekt]]
  def getProjektPublik(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[ProjektPublik]]
  def getGeschaeftsjahre(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[GeschaeftsjahrStart]]

  def getKontoDatenProjekt(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[KontoDaten]]
  def getKontoDatenKunde(kundeId: KundeId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[KontoDaten]]

  def getLieferplanungen(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext, gjFilter: Option[GeschaeftsjahrFilter], queryString: Option[QueryFilter]): Future[List[Lieferplanung]]
  def getLieferplanung(id: LieferplanungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Lieferplanung]]
  def getLatestLieferplanung(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Lieferplanung]]
  def getLieferungenNext()(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Lieferung]]
  def getLastGeplanteLieferung(abotypId: AbotypId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Lieferung]]
  def getLieferungenDetails(id: LieferplanungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[LieferungDetail]]
  def getVerfuegbareLieferungen(id: LieferplanungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[LieferungDetail]]
  def getLieferpositionen(id: LieferungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Lieferposition]]
  def getLieferpositionenByLieferplan(id: LieferplanungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Lieferposition]]
  def getLieferpositionenByLieferant(id: ProduzentId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Lieferposition]]
  def getAboIds(lieferungId: LieferungId, korbStatus: KorbStatus)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[AboId]]
  def getAboIds(lieferplanungId: LieferplanungId, korbStatus: KorbStatus)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[AboId]]
  def getZusatzaboIds(lieferungId: LieferungId, korbStatus: KorbStatus)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[AboId]]

  def getLieferplanungReport(lieferplanungId: LieferplanungId, projekt: ProjektReport)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[LieferplanungReport]]

  def getKorb(lieferungId: LieferungId, aboId: AboId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Korb]]
  def getKoerbe(aboId: AboId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[KorbLieferung]]

  def getBestellpositionen(id: BestellungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Bestellposition]]
  def getSammelbestellungDetail(id: SammelbestellungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[SammelbestellungDetail]]
  def getBestellpositionByBestellungProdukt(bestellungId: BestellungId, produktId: ProduktId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Bestellposition]]
  def getSammelbestellungen(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr], gjFilter: Option[GeschaeftsjahrFilter], queryString: Option[QueryFilter]): Future[List[Sammelbestellung]]
  def getSammelbestellungen(id: LieferplanungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[SammelbestellungDetail]]
  def getSammelbestellungByProduzentLieferplanungDatum(produzentId: ProduzentId, lieferplanungId: LieferplanungId, datum: DateTime)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Sammelbestellung]]

  def getDepotAuslieferungen(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr], gjFilter: Option[GeschaeftsjahrFilter], queryString: Option[QueryFilter]): Future[List[DepotAuslieferung]]
  def getTourAuslieferungen(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr], gjFilter: Option[GeschaeftsjahrFilter], queryString: Option[QueryFilter]): Future[List[TourAuslieferung]]
  def getPostAuslieferungen(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr], gjFilter: Option[GeschaeftsjahrFilter], queryString: Option[QueryFilter]): Future[List[PostAuslieferung]]
  def getAuslieferungReport(auslieferungId: AuslieferungId, projekt: ProjektReport)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[AuslieferungReport]]
  def getMultiAuslieferungReport(auslieferungIds: Seq[AuslieferungId], projekt: ProjektReport)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[MultiReport[AuslieferungReportEntry]]

  def getDepotAuslieferungDetail(auslieferungId: AuslieferungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[DepotAuslieferungDetail]]
  def getTourAuslieferungDetail(auslieferungId: AuslieferungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[TourAuslieferungDetail]]
  def getPostAuslieferungDetail(auslieferungId: AuslieferungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[PostAuslieferungDetail]]

  def getAuslieferungen(id: LieferplanungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Auslieferung]]

  def getProjektVorlagen(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[ProjektVorlage]]
  def getProjektVorlage(id: ProjektVorlageId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[ProjektVorlage]]

  def getEinladung(token: String)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Einladung]]

  def getLastClosedLieferplanungenDetail(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[LieferplanungOpenDetail]]
}

class StammdatenReadRepositoryAsyncImpl extends BaseReadRepositoryAsync with StammdatenReadRepositoryAsync with LazyLogging with StammdatenRepositoryQueries {
  def getAbotypen(implicit asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr], queryString: Option[QueryFilter]): Future[List[Abotyp]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getAbotypenQuery(filter, queryString).future()
  }

  def getZusatzAbotypen(implicit asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr], queryString: Option[QueryFilter]): Future[List[ZusatzAbotyp]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getZusatzAbotypenQuery(filter, queryString).future()
  }

  def getKunden(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Kunde]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getKundenQuery.future()
  }

  def getKundenUebersicht(implicit asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr], queryString: Option[QueryFilter]): Future[List[KundeUebersicht]] = {
    getKundenUebersichtQuery(filter, queryString).future()
  }

  def getKundenSearch(implicit asyncCpContext: MultipleAsyncConnectionPoolContext, queryString: Option[QueryFilter]): Future[List[KundenSearch]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getKundenSearchQuery(queryString).future()
  }

  def getCustomKundentypen(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[CustomKundentyp]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getCustomKundentypenQuery.future()
  }

  def getKundeDetail(id: KundeId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[KundeDetail]] = {
    getKundeDetailQuery(id).future()
  }

  def getKundeDetailReport(kundeId: KundeId, projekt: ProjektReport)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[KundeDetailReport]] = {
    getKundeDetailReportQuery(kundeId, projekt).future()
  }

  def getKundeDetailsArbeitseinsatzReport(projekt: ProjektReport)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[KundeDetailArbeitseinsatzReport]] = {
    getKundeDetailsArbeitseinsatzReportQuery(projekt).future()
  }

  def getPersonen(kundeId: KundeId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Person]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getPersonenQuery(kundeId).future()
  }

  def getPersonByEmail(email: String)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Person]] = {
    import scalikejdbc.async.makeSQLToOptionAsync
    withSQL {
      select
        .from(personMapping as person)
        .where.eq(person.email, email)
    }.map(personMapping(person)).single.future()
  }

  def getPerson(id: PersonId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Person]] = {
    import scalikejdbc.async.makeSQLToOptionAsync
    withSQL {
      select
        .from(personMapping as person)
        .where.eq(person.id, id)
    }.map(personMapping(person)).single.future()
  }

  def getPersonenUebersicht(implicit asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr], queryString: Option[QueryFilter]): Future[List[PersonUebersicht]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getPersonenUebersichtQuery(filter, queryString).future()
  }

  def getPersonenByDepots(implicit asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr]): Future[List[PersonSummary]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getPersonenByDepotsQuery(filter).future()
  }

  def getPersonenAboAktivByDepots(implicit asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr]): Future[List[PersonSummary]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getPersonenAboAktivByDepotsQuery(filter).future()
  }

  def getPersonenByTouren(implicit asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr]): Future[List[PersonSummary]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getPersonenByTourenQuery(filter).future()
  }

  def getPersonenAboAktivByTouren(implicit asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr]): Future[List[PersonSummary]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getPersonenAboAktivByTourenQuery(filter).future()
  }

  def getPersonenByAbotypen(implicit asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr]): Future[List[PersonSummary]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getPersonenByAbotypenQuery(filter).future()
  }

  def getPersonenAboAktivByAbotypen(implicit asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr]): Future[List[PersonSummary]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getPersonenAboAktivByAbotypenQuery(filter).future()
  }

  def getPersonenZusatzAboAktivByZusatzAbotypen(implicit asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr], queryString: Option[QueryFilter]): Future[List[PersonSummary]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getPersonenZusatzAboAktivByZusatzAbotypenQuery(filter, queryString).future()
  }

  def getPersonCategory(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[PersonCategory]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getPersonCategoryQuery.future()
  }

  override def getAbotypDetail(id: AbotypId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Abotyp]] = {
    import scalikejdbc.async.makeSQLToOptionAsync
    getAbotypDetailQuery(id).future()
  }

  override def getZusatzAbotypDetail(id: AbotypId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[ZusatzAbotyp]] = {
    import scalikejdbc.async.makeSQLToOptionAsync
    getZusatzAbotypDetailQuery(id).future()
  }

  def getVertrieb(vertriebId: VertriebId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Vertrieb]] = {
    import scalikejdbc.async.makeSQLToOptionAsync
    getVertriebQuery(vertriebId).future()
  }

  def getVertriebe(abotypId: AbotypId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[VertriebVertriebsarten]] = {
    getVertriebeQuery(abotypId).future()
  }

  def getVertriebe(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Vertrieb]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getVertriebeQuery().future()
  }

  def getVertriebsarten(vertriebId: VertriebId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext) = {
    for {
      d <- getDepotlieferung(vertriebId)
      h <- getHeimlieferung(vertriebId)
      p <- getPostlieferung(vertriebId)
    } yield {
      d ++ h ++ p
    }
  }

  def getDepotlieferung(vertriebId: VertriebId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[DepotlieferungDetail]] = {
    getDepotlieferungQuery(vertriebId).future()
  }

  def getHeimlieferung(vertriebId: VertriebId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[HeimlieferungDetail]] = {
    getHeimlieferungQuery(vertriebId).future()
  }

  def getPostlieferung(vertriebId: VertriebId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[PostlieferungDetail]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getPostlieferungQuery(vertriebId).future()
  }

  def getVertriebsart(vertriebsartId: VertriebsartId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext) = {
    for {
      d <- getDepotlieferung(vertriebsartId)
      h <- getHeimlieferung(vertriebsartId)
      p <- getPostlieferung(vertriebsartId)
    } yield {
      Seq(d, h, p).flatten.headOption
    }
  }

  def getDepotlieferung(vertriebsartId: VertriebsartId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[DepotlieferungDetail]] = {
    getDepotlieferungQuery(vertriebsartId).future()
  }

  def getHeimlieferung(vertriebsartId: VertriebsartId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[HeimlieferungDetail]] = {
    getHeimlieferungQuery(vertriebsartId).future()
  }

  def getPostlieferung(vertriebsartId: VertriebsartId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[PostlieferungDetail]] = {
    import scalikejdbc.async.makeSQLToOptionAsync
    getPostlieferungQuery(vertriebsartId).future()
  }

  def getUngeplanteLieferungen(abotypId: AbotypId, vertriebId: VertriebId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Lieferung]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getUngeplanteLieferungenQuery(abotypId, vertriebId).future()
  }

  def getUngeplanteLieferungen(abotypId: AbotypId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Lieferung]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getUngeplanteLieferungenQuery(abotypId).future()
  }

  def getDepots(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Depot]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getDepotsQuery.future()
  }

  def getDepotDetail(id: DepotId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Depot]] = {
    import scalikejdbc.async.makeSQLToOptionAsync
    getDepotDetailQuery(id).future()
  }

  def getDepotDetailReport(id: DepotId, projekt: ProjektReport)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[DepotDetailReport]] = {
    getDepotDetailReportQuery(id, projekt).future()
  }

  def getProduzentDetailReport(id: ProduzentId, projekt: ProjektReport)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[ProduzentDetailReport]] = {
    import scalikejdbc.async.makeSQLToOptionAsync
    getProduzentDetailReportQuery(id, projekt).future()
  }

  def getDepotlieferungAbos(xFlags: Option[AbosComplexFlags], gjFilter: Option[GeschaeftsjahrFilter], queryString: Option[QueryFilter])(implicit asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr]): Future[List[DepotlieferungAbo]] = {
    xFlags match {
      case Some(acf) if acf.zusatzAbosAktiv =>
        import scalikejdbc.async.makeSQLToListAsync
        getDepotlieferungAbosOnlyAktiveZusatzabosQuery(filter, queryString).future()
      case _ =>
        import scalikejdbc.async.makeSQLToListAsync
        getDepotlieferungAbosQuery(filter, gjFilter, queryString).future()
    }
  }

  def getHeimlieferungAbos(xFlags: Option[AbosComplexFlags], gjFilter: Option[GeschaeftsjahrFilter], queryString: Option[QueryFilter])(implicit asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr]): Future[List[HeimlieferungAbo]] = {
    xFlags match {
      case Some(acf) if acf.zusatzAbosAktiv =>
        import scalikejdbc.async.makeSQLToListAsync
        getHeimlieferungAbosOnlyAktiveZusatzabosQuery(filter, queryString).future()
      case _ =>
        import scalikejdbc.async.makeSQLToListAsync
        getHeimlieferungAbosQuery(filter, gjFilter, queryString).future()
    }
  }

  def getPostlieferungAbos(xFlags: Option[AbosComplexFlags], gjFilter: Option[GeschaeftsjahrFilter], queryString: Option[QueryFilter])(implicit asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr]): Future[List[PostlieferungAbo]] = {
    xFlags match {
      case Some(acf) if acf.zusatzAbosAktiv =>
        import scalikejdbc.async.makeSQLToListAsync
        getPostlieferungAbosOnlyAktiveZusatzabosQuery(filter, queryString).future()
      case _ =>
        import scalikejdbc.async.makeSQLToListAsync
        getPostlieferungAbosQuery(filter, gjFilter, queryString).future()
    }
  }

  def getAbos(xFlags: Option[AbosComplexFlags])(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr], gjFilter: Option[GeschaeftsjahrFilter], queryString: Option[QueryFilter]): Future[List[Abo]] = {
    for {
      d <- getDepotlieferungAbos(xFlags, gjFilter, queryString)
      h <- getHeimlieferungAbos(xFlags, gjFilter, queryString)
      p <- getPostlieferungAbos(xFlags, gjFilter, queryString)
    } yield {
      d ::: h ::: p
    }
  }

  def getZusatzAbos(xFlags: Option[AbosComplexFlags])(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr], gjFilter: Option[GeschaeftsjahrFilter], queryString: Option[QueryFilter]): Future[List[ZusatzAbo]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getZusatzAbosQuery(filter, gjFilter, queryString).future()
  }

  def getDepotlieferungAbo(id: AboId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[DepotlieferungAboDetail]] = {
    getDepotlieferungAboAusstehendQuery(id).future()
  }

  def getHeimlieferungAbo(id: AboId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[HeimlieferungAboDetail]] = {
    getHeimlieferungAboAusstehendQuery(id).future()
  }

  def getPostlieferungAbo(id: AboId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[PostlieferungAboDetail]] = {
    getPostlieferungAboAusstehendQuery(id).future()
  }

  def getAboDetail(id: AboId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[AboDetail]] = {
    for {
      d <- getDepotlieferungAbo(id)
      h <- getHeimlieferungAbo(id)
      p <- getPostlieferungAbo(id)
    } yield (d orElse h orElse p)
  }

  def getZusatzAboDetail(id: AboId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[ZusatzAbo]] = {
    import scalikejdbc.async.makeSQLToOptionAsync
    getZusatzAboDetailQuery(id).future()
  }

  def getZusatzaboPerAbo(id: AboId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[ZusatzAbo]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getZusatzAboPerAboQuery(id).future()
  }

  def countAbwesend(lieferungId: LieferungId, aboId: AboId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Int]] = {
    import scalikejdbc.async.makeSQLToOptionAsync
    countAbwesendQuery(lieferungId, aboId).future()
  }

  def getPendenzen(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Pendenz]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getPendenzenQuery.future()
  }

  def getPendenzen(id: KundeId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Pendenz]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getPendenzenQuery(id).future()
  }

  def getPendenzDetail(id: PendenzId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Pendenz]] = {
    import scalikejdbc.async.makeSQLToOptionAsync
    getPendenzDetailQuery(id).future()
  }

  def getProdukte(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Produkt]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getProdukteQuery.future()
  }

  def getProduktekategorien(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Produktekategorie]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getProduktekategorienQuery.future()
  }

  def getProduzenten(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr], queryString: Option[QueryFilter]): Future[List[Produzent]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getProduzentenQuery(filter, queryString).future()
  }

  def getProduzentDetail(id: ProduzentId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Produzent]] = {
    import scalikejdbc.async.makeSQLToOptionAsync
    getProduzentDetailQuery(id).future()
  }

  def getTouren(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Tour]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getTourenQuery.future()
  }

  def getTourDetail(id: TourId, aktiveOrPlanned: Boolean)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[TourDetail]] = {
    getTourDetailQuery(id, aktiveOrPlanned).future()
  }

  def getProjekt(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Projekt]] = {
    import scalikejdbc.async.makeSQLToOptionAsync
    getProjektQuery.future()
  }

  def getProjektPublik(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[ProjektPublik]] = {
    import scalikejdbc.async.makeSQLToOptionAsync
    getProjektQuery.future map (_ map (projekt => {
      copyTo[Projekt, ProjektPublik](projekt)
    }))
  }

  def getGeschaeftsjahre(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[GeschaeftsjahrStart]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getGeschaeftsjahreQuery.future()
  }

  def getKontoDatenProjekt(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[KontoDaten]] = {
    import scalikejdbc.async.makeSQLToOptionAsync
    getKontoDatenProjektQuery.future()
  }

  def getKontoDatenKunde(kundeId: KundeId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[KontoDaten]] = {
    import scalikejdbc.async.makeSQLToOptionAsync
    getKontoDatenKundeQuery(kundeId).future()
  }

  def getProduktProduzenten(id: ProduktId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[ProduktProduzent]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getProduktProduzentenQuery(id).future()
  }

  def getProduktProduktekategorien(id: ProduktId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[ProduktProduktekategorie]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getProduktProduktekategorienQuery(id).future()
  }

  def getProduzentDetailByKurzzeichen(kurzzeichen: String)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Produzent]] = {
    import scalikejdbc.async.makeSQLToOptionAsync
    getProduzentDetailByKurzzeichenQuery(kurzzeichen).future()
  }

  def getProduktekategorieByBezeichnung(bezeichnung: String)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Produktekategorie]] = {
    import scalikejdbc.async.makeSQLToOptionAsync
    getProduktekategorieByBezeichnungQuery(bezeichnung).future()
  }

  def getProduzentenabrechnungReport(sammelbestellungIds: Seq[SammelbestellungId], projekt: ProjektReport)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[ProduzentenabrechnungReport]] = {
    getProduzentenabrechnungQuery(sammelbestellungIds).future flatMap { result =>
      Future.sequence((result.groupBy(_.produzentId) map {
        case (produzentId, sammelbestellungen) => {
          val sumPreis = sammelbestellungen.map(_.preisTotal).sum
          val sumSteuer = sammelbestellungen.map(_.steuer).sum
          val sumTotal = sammelbestellungen.map(_.totalSteuer).sum

          getProduzentDetailReport(sammelbestellungen.head.produzent.id, projekt) collect {
            case Some(produzentDetailReport) =>
              val produzentId = produzentDetailReport.id
              val prodKurzzeichen = produzentDetailReport.kurzzeichen
              val prodSteuersatz = produzentDetailReport.mwstSatz
              ProduzentenabrechnungReport(
                produzentId,
                prodKurzzeichen,
                produzentDetailReport,
                sammelbestellungen,
                sumPreis,
                prodSteuersatz,
                sumSteuer,
                sumTotal,
                projekt
              )
          }
        }
      }).toList)
    }
  }

  def getProdukteByProduktekategorieBezeichnung(bezeichnung: String)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Produkt]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getProdukteByProduktekategorieBezeichnungQuery(bezeichnung).future()
  }

  def getLieferplanungen(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext, gjFilter: Option[GeschaeftsjahrFilter], queryString: Option[QueryFilter]): Future[List[Lieferplanung]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getLieferplanungenQuery(gjFilter, queryString).future()
  }

  def getLatestLieferplanung(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Lieferplanung]] = {
    import scalikejdbc.async.makeSQLToOptionAsync
    getLatestLieferplanungQuery.future()
  }

  def getLieferplanung(id: LieferplanungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Lieferplanung]] = {
    import scalikejdbc.async.makeSQLToOptionAsync
    getLieferplanungQuery(id).future()
  }

  def getLieferungenNext()(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Lieferung]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getLieferungenNextQuery.future()
  }

  def getLastGeplanteLieferung(abotypId: AbotypId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Lieferung]] = {
    import scalikejdbc.async.makeSQLToOptionAsync
    getLastGeplanteLieferungQuery(abotypId).future()
  }

  def getLieferungenDetails(id: LieferplanungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[LieferungDetail]] = {
    getLieferungenDetailsQuery(id).future()
  }

  def getVerfuegbareLieferungen(id: LieferplanungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[LieferungDetail]] = {
    getVerfuegbareLieferungenQuery(id).future()
  }

  def getLieferplanungReport(lieferplanungId: LieferplanungId, projekt: ProjektReport)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[LieferplanungReport]] = {
    getLieferplanungReportQuery(lieferplanungId, projekt).future()
  }

  def getSammelbestellungDetail(id: SammelbestellungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[SammelbestellungDetail]] = {
    getSammelbestellungDetailQuery(id).future()
  }

  def getSammelbestellungen(id: LieferplanungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[SammelbestellungDetail]] = {
    getSammelbestellungDetailsQuery(id).future()
  }

  def getSammelbestellungen(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr], gjFilter: Option[GeschaeftsjahrFilter], queryString: Option[QueryFilter]): Future[List[Sammelbestellung]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getSammelbestellungenQuery(filter, gjFilter, queryString).future()
  }

  def getSammelbestellungByProduzentLieferplanungDatum(produzentId: ProduzentId, lieferplanungId: LieferplanungId, datum: DateTime)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Sammelbestellung]] = {
    import scalikejdbc.async.makeSQLToOptionAsync
    getSammelbestellungByProduzentLieferplanungDatumQuery(produzentId, lieferplanungId, datum).future()
  }

  def getBestellpositionen(id: BestellungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Bestellposition]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getBestellpositionenQuery(id).future()
  }

  def getLieferpositionen(id: LieferungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Lieferposition]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getLieferpositionenQuery(id).future()
  }

  def getLieferpositionenByLieferant(id: ProduzentId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Lieferposition]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getLieferpositionenByLieferantQuery(id).future()
  }

  def getAboIds(lieferungId: LieferungId, korbStatus: KorbStatus)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[AboId]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getAboIdsQuery(lieferungId, korbStatus).future()
  }

  def getAboIds(lieferplanungId: LieferplanungId, korbStatus: KorbStatus)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[AboId]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getAboIdsQuery(lieferplanungId, korbStatus).future()
  }

  def getZusatzaboIds(lieferungId: LieferungId, korbStatus: KorbStatus)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[AboId]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getZusatzaboIdsQuery(lieferungId, korbStatus).future()
  }

  def getBestellpositionByBestellungProdukt(bestellungId: BestellungId, produktId: ProduktId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Bestellposition]] = {
    import scalikejdbc.async.makeSQLToOptionAsync
    getBestellpositionByBestellungProduktQuery(bestellungId, produktId).future()
  }

  def getLieferpositionenByLieferplan(id: LieferplanungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Lieferposition]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getLieferpositionenByLieferplanQuery(id).future()
  }

  def getKorb(lieferungId: LieferungId, aboId: AboId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Korb]] = {
    import scalikejdbc.async.makeSQLToOptionAsync
    getKorbQuery(lieferungId, aboId).future()
  }

  def getKoerbe(aboId: AboId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[KorbLieferung]] = {
    getKoerbeQuery(aboId).future()
  }

  def getDepotAuslieferungen(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr], gjFilter: Option[GeschaeftsjahrFilter], queryString: Option[QueryFilter]): Future[List[DepotAuslieferung]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getDepotAuslieferungenQuery(filter, gjFilter, queryString).future()
  }
  def getTourAuslieferungen(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr], gjFilter: Option[GeschaeftsjahrFilter], queryString: Option[QueryFilter]): Future[List[TourAuslieferung]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getTourAuslieferungenQuery(filter, gjFilter, queryString).future()
  }
  def getPostAuslieferungen(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr], gjFilter: Option[GeschaeftsjahrFilter], queryString: Option[QueryFilter]): Future[List[PostAuslieferung]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getPostAuslieferungenQuery(filter, gjFilter, queryString).future()
  }

  def getDepotAuslieferungDetail(auslieferungId: AuslieferungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[DepotAuslieferungDetail]] = {
    getDepotAuslieferungDetailQuery(auslieferungId).future()
  }

  def getTourAuslieferungDetail(auslieferungId: AuslieferungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[TourAuslieferungDetail]] = {
    getTourAuslieferungDetailQuery(auslieferungId).future()
  }

  def getPostAuslieferungDetail(auslieferungId: AuslieferungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[PostAuslieferungDetail]] = {
    getPostAuslieferungDetailQuery(auslieferungId).future()
  }

  def getDepotAuslieferungen(lieferplanungId: LieferplanungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[DepotAuslieferung]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getDepotAuslieferungenQuery(lieferplanungId).future()
  }

  def getTourAuslieferungen(lieferplanungId: LieferplanungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[TourAuslieferung]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getTourAuslieferungenQuery(lieferplanungId).future()
  }

  def getPostAuslieferungen(lieferplanungId: LieferplanungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[PostAuslieferung]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getPostAuslieferungenQuery(lieferplanungId).future()
  }

  def getAuslieferungen(id: LieferplanungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Auslieferung]] = {
    for {
      d <- getDepotAuslieferungen(id)
      h <- getTourAuslieferungen(id)
      p <- getPostAuslieferungen(id)
    } yield (d.distinct ::: h.distinct ::: p.distinct)
  }

  def getAuslieferungReport(id: AuslieferungId, projekt: ProjektReport)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[AuslieferungReport]] = {
    for {
      d <- getDepotAuslieferungReport(id, projekt)
      h <- getTourAuslieferungReport(id, projekt)
      p <- getPostAuslieferungReport(id, projekt)
    } yield (d orElse h orElse p)
  }

  def getMultiAuslieferungReport(ids: Seq[AuslieferungId], projekt: ProjektReport)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[MultiReport[AuslieferungReportEntry]] = {
    for {
      d <- getDepotAuslieferungReports(ids, projekt)
      h <- getTourAuslieferungReports(ids, projekt)
      p <- getPostAuslieferungReports(ids, projekt)
    } yield {
      val entriesD = (d flatMap {
        _ map { report =>
          report.koerbe map { korb =>
            AuslieferungReportEntry(
              report.id,
              report.status,
              report.datum,
              projekt,
              korb,
              Some(report.depot),
              None
            )
          }
        }
      }).flatten
      val entriesH = (h flatMap {
        _ map { report =>
          report.koerbe map { korb =>
            AuslieferungReportEntry(
              report.id,
              report.status,
              report.datum,
              projekt,
              korb,
              None,
              Some(report.tour)
            )
          }
        }
      }).flatten
      val entriesP = (p flatMap {
        _ map { report =>
          report.koerbe map { korb =>
            AuslieferungReportEntry(
              report.id,
              report.status,
              report.datum,
              projekt,
              korb,
              None,
              None
            )
          }
        }
      }).flatten
      val entries = entriesD ++ entriesH ++ entriesP

      MultiReport(MultiReportId(IdUtil.positiveRandomId), entries, projekt)
    }
  }

  def getDepotAuslieferungReport(auslieferungId: AuslieferungId, projekt: ProjektReport)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[AuslieferungReport]] = {
    getDepotAuslieferungReportQuery(auslieferungId, projekt).future()
  }

  def getTourAuslieferungReport(auslieferungId: AuslieferungId, projekt: ProjektReport)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[AuslieferungReport]] = {
    getTourAuslieferungReportQuery(auslieferungId, projekt).future()
  }

  def getPostAuslieferungReport(auslieferungId: AuslieferungId, projekt: ProjektReport)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[AuslieferungReport]] = {
    getPostAuslieferungReportQuery(auslieferungId, projekt).future()
  }

  def getDepotAuslieferungReports(auslieferungIds: Seq[AuslieferungId], projekt: ProjektReport)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Seq[Option[DepotAuslieferungReport]]] = {
    Future.sequence(auslieferungIds map { auslieferungId =>
      getDepotAuslieferungReport(auslieferungId, projekt) map (_.asInstanceOf[Option[DepotAuslieferungReport]])
    })
  }

  def getTourAuslieferungReports(auslieferungIds: Seq[AuslieferungId], projekt: ProjektReport)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Seq[Option[TourAuslieferungReport]]] = {
    Future.sequence(auslieferungIds map { auslieferungId =>
      getTourAuslieferungReport(auslieferungId, projekt) map (_.asInstanceOf[Option[TourAuslieferungReport]])
    })
  }

  def getPostAuslieferungReports(auslieferungIds: Seq[AuslieferungId], projekt: ProjektReport)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Seq[Option[PostAuslieferungReport]]] = {
    Future.sequence(auslieferungIds map { auslieferungId =>
      getPostAuslieferungReport(auslieferungId, projekt) map (_.asInstanceOf[Option[PostAuslieferungReport]])
    })
  }

  def getProjektVorlagen(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[ProjektVorlage]] = {
    import scalikejdbc.async.makeSQLToListAsync
    getProjektVorlagenQuery.future()
  }

  def getProjektVorlage(id: ProjektVorlageId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[ProjektVorlage]] = {
    import scalikejdbc.async.makeSQLToOptionAsync
    getByIdQuery(projektVorlageMapping, id).future()
  }

  def getEinladung(token: String)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Einladung]] = {
    import scalikejdbc.async.makeSQLToOptionAsync
    getEinladungQuery(token).future()
  }

  def getLastClosedLieferplanungenDetail(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[LieferplanungOpenDetail]] = {
    getLastClosedLieferplanungenDetailQuery.future map (_.take(5))
  }
}
