package ch.openolitor.util

import ch.openolitor.stammdaten.models.{ Abotyp, AbotypId, AbotypModify, CHF, Depot, DepotId, DepotModify, Donnerstag, Frist, KundeModify, Monatsfrist, PersonModify, ProLieferung, ProQuartal, Unbeschraenkt, Vertrieb, VertriebId, VertriebModify, Woechentlich, ZusatzAbotypModify }
import org.joda.time.{ DateTime, LocalDate, Months }
import ch.openolitor.core.Macros._
import ch.openolitor.core.SpecSubjects

import scala.collection.immutable.TreeMap

object Fixtures extends SpecSubjects {
  val ZERO = 0
  val now = DateTime.now()

  val emptyIntMap: TreeMap[String, Int] = TreeMap()
  val emptyDecimalMap: TreeMap[String, BigDecimal] = TreeMap()

  val depotWwgId: DepotId = DepotId(1)
  val depotWwgModify = DepotModify("Deep Oh", "DEP", None, None, None, None, None, None, None, None, Some("Wasserwerkgasse"), None, "3011", "Bern", true, None, Some("#00ccff"), None, None, None, None)
  val depotWwg = copyTo[DepotModify, Depot](
    depotWwgModify,
    "id" -> depotWwgId,
    "anzahlAbonnenten" -> ZERO,
    "anzahlAbonnentenAktiv" -> ZERO,
    "erstelldat" -> now,
    "ersteller" -> personId,
    "modifidat" -> now,
    "modifikator" -> personId
  )

  val abotypId = AbotypId(1)
  val abotypVegiModify = AbotypModify("Vegi", None, Woechentlich, Some(LocalDate.now().withDayOfWeek(1).minus(Months.TWO)), None, BigDecimal(17.5), ProQuartal, None, Unbeschraenkt, Some(Frist(6, Monatsfrist)), Some(Frist(1, Monatsfrist)), Some(4),
    None, "#ff0000", Some(BigDecimal(17.0)), 2, BigDecimal(12), true)
  val abotypVegi = copyTo[AbotypModify, Abotyp](
    abotypVegiModify,
    "id" -> abotypId,
    "anzahlAbonnenten" -> ZERO,
    "anzahlAbonnentenAktiv" -> ZERO,
    "waehrung" -> CHF,
    "letzteLieferung" -> None,
    "erstelldat" -> now,
    "ersteller" -> personId,
    "modifidat" -> now,
    "modifikator" -> personId
  )

  val zusatzAbotypEier = ZusatzAbotypModify("Eier", None, Some(LocalDate.now().withDayOfWeek(1).minus(Months.TWO)), None, BigDecimal(5.2), ProLieferung, None, Unbeschraenkt, Some(Frist(2, Monatsfrist)), Some(Frist(1, Monatsfrist)), Some(2), None, "#ffcc00", Some(BigDecimal(5)), BigDecimal(10), true, CHF)

  val kundeCreateUntertorOski = KundeModify(true, None, "Wasserwerkgasse", Some("2"), None, "3011", "Bern", None, false, None, None, None, None, None, None, None, None, None, Set(), Seq(), Seq(PersonModify(
    None, None, "Untertor", "Oski", Some("oski@example.com"), None, None, None, Set(), None, None, false
  )), None, None)

  val vertriebId = VertriebId(1)
  val vertriebDonnerstagModify = VertriebModify(abotypId, Donnerstag, None)
  val vertriebDonnerstag = copyTo[VertriebModify, Vertrieb](
    vertriebDonnerstagModify,
    "id" -> vertriebId,
    "anzahlAbos" -> ZERO,
    "anzahlAbosAktiv" -> ZERO,
    "durchschnittspreis" -> emptyDecimalMap,
    "anzahlLieferungen" -> emptyIntMap,
    "erstelldat" -> now,
    "ersteller" -> personId,
    "modifidat" -> now,
    "modifikator" -> personId
  )
}
