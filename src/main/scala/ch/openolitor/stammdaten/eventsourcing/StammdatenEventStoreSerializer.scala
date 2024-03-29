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
package ch.openolitor.stammdaten.eventsourcing

import stamina._
import stamina.json._
import spray.json._
import spray.json.lenses.JsonLenses._
import ch.openolitor.core.Macros._
import ch.openolitor.stammdaten._
import ch.openolitor.stammdaten.models._
import ch.openolitor.core.domain.EntityStoreJsonProtocol
import ch.openolitor.stammdaten.StammdatenCommandHandler.{ OtpResetEvent, _ }
import ch.openolitor.core.eventsourcing.CoreEventStoreSerializer

import java.util.Locale
import org.joda.time.DateTime
import org.joda.time.LocalDate
import ch.openolitor.mailtemplates.eventsourcing._

trait StammdatenEventStoreSerializer extends StammdatenJsonProtocol with EntityStoreJsonProtocol with CoreEventStoreSerializer with MailTemplateEventStoreSerializer {
  val False = false
  //V1 persisters
  implicit val depotModifyPersister = persister[DepotModify]("depot-modify")
  implicit val depotIdPersister = persister[DepotId]("depot-id")

  implicit val aboIdPersister = persister[AboId]("abo-id")
  implicit val korbIdPersister = persister[KorbId]("korb-id")

  implicit val abotypModifyPersister = persister[AbotypModify]("abotyp-modify")
  implicit val abotypLetzteLieferungModifyPersister = persister[AbotypLetzteLieferungModify]("abotyp-letzte-lieferung-modify")
  implicit val abotypIdPersister = persister[AbotypId]("abotyp-id")

  implicit val zusatzAbotypModifyPersister = persister[ZusatzAbotypModify]("zusatzabotyp-modify")

  implicit val zusatzAboModifyPersister = persister[ZusatzAboModify]("zusatzabo-modify")
  implicit val zusatzAboCreatePersister = persister[ZusatzAboCreate]("zusatzabo-create")

  implicit val kundeModifyPersister = persister[KundeModify, V7]("kunde-modify", from[V1]
    .to[V2](fixPersonModifyInKundeModifyV2(_, Symbol("ansprechpersonen")))
    .to[V3](_.update(Symbol("paymentType") ! set[Option[PaymentType]](None)))
    .to[V4](_.update(Symbol("longLieferung") ! set[Option[BigDecimal]](None)).update(Symbol("latLieferung") ! set[Option[BigDecimal]](None)))
    .to[V5](fixPersonModifyContactPermissionInKundeModify(_, Symbol("ansprechpersonen")))
    .to[V6](fixPersonModifyInKundeModifyV3(_, Symbol("ansprechpersonen")))
    .to[V7](_.update(Symbol("aktiv") ! set[Boolean](true))))

  implicit val kundeIdPersister = persister[KundeId]("kunde-id")

  val personCreatePersister = persister[PersonCreate]("person-create")
  val personCreateV2Persister = persister[PersonCreate, V2]("person-create", from[V1]
    .to[V2](_.update(Symbol("categories") ! set[Set[PersonModify]](Set()))))
  implicit val personCreateV3Persister = persister[PersonCreate, V3]("person-create", from[V1]
    .to[V2](_.update(Symbol("categories") ! set[Set[PersonModify]](Set())))
    .to[V3](_.update(Symbol("contactPermission") ! set[Boolean](false))))

  implicit val personCategoryIdPersister = persister[PersonCategoryId]("personCategory-id")
  implicit val personCategoryCreatePersister = persister[PersonCategoryCreate]("personCategory-create")
  implicit val personCategoryModifyPersister = persister[PersonCategoryModify]("personCategory-modify")

  implicit val abwesenheitCreatePersister = persister[AbwesenheitCreate, V2]("abwesenheit-create", from[V1]
    .to[V2](fixToLocalDate(_, Symbol("datum"))))

  implicit val abwesenheitIdPersister = persister[AbwesenheitId]("abwesenheit-id")

  implicit val vertriebModifyPersister = persister[VertriebModify]("vertrieb-modify")
  implicit val vertriebRecalculationsModifyPersister = persister[VertriebRecalculationsModify]("vertrieb-recalculations-modify")
  implicit val vertriebIdPersister = persister[VertriebId]("vertrieb-id")

  implicit val vertriebsartDLAbotypPersister = persister[DepotlieferungAbotypModify]("depotlieferungabotyp-modify")
  implicit val vertriebsartPLAbotypPersister = persister[PostlieferungAbotypModify]("postlieferungabotyp-modify")
  implicit val vertriebsartHLAbotypPersister = persister[HeimlieferungAbotypModify]("heimlieferungabotyp-modify")
  implicit val vertriebsartIdPersister = persister[VertriebsartId]("vertriebsart-id")

  implicit val vertriebsartDLPersister = persister[DepotlieferungModify]("depotlieferung-modify")
  implicit val vertriebsartPLPersister = persister[PostlieferungModify]("postlieferung-modify")
  implicit val vertriebsartHLPersister = persister[HeimlieferungModify]("heimlieferung-modify")

  implicit val aboPriceModifyPersister = persister[AboPriceModify]("abo-price-modify")

  implicit val aboGuthabenModifyPersister = persister[AboGuthabenModify, V2]("abo-guthaben-modify", from[V1]
    .to[V2](in => in.update(Symbol("guthabenAlt") ! set[Int](in.extract[Int](Symbol("guthabenNeu"))))))

  // TODO how to set vertriebId?!
  implicit val aboVertriebsartModifyPersister = persister[AboVertriebsartModify, V2]("abo-vertriebsart-modify", from[V1]
    .to[V2](in => in.update(Symbol("vertriebIdNeu") ! set[Int](0))))

  implicit val aboDLPersister = persister[DepotlieferungAboModify, V2]("depotlieferungabo-modify", from[V1]
    .to[V2](in => fixToOptionLocalDate(fixToLocalDate(in, Symbol("start")), Symbol("ende"))))
  implicit val aboPLPersister = persister[PostlieferungAboModify, V2]("postlieferungabo-modify", from[V1]
    .to[V2](in => fixToOptionLocalDate(fixToLocalDate(in, Symbol("start")), Symbol("ende"))))
  implicit val aboHLPersister = persister[HeimlieferungAboModify, V2]("heimlieferungabo-modify", from[V1]
    .to[V2](in => fixToOptionLocalDate(fixToLocalDate(in, Symbol("start")), Symbol("ende"))))

  implicit val aboDLCreatePersister = persister[DepotlieferungAboCreate]("depotlieferungabo-create")
  implicit val aboPLCreatePersister = persister[PostlieferungAboCreate]("postlieferungabo-create")
  implicit val aboHLCreatePersister = persister[HeimlieferungAboCreate]("heimlieferungabo-create")

  implicit val customKundetypCreatePersister = persister[CustomKundentypCreate]("custom-kundetyp-create")
  implicit val customKundetypModifyV1Persister = persister[CustomKundentypModifyV1]("custom-kundetyp-modify")
  implicit val customKundetypModifyPersister = persister[CustomKundentypModify]("custom-kundetyp-modify-v2")
  implicit val customKundetypIdPersister = persister[CustomKundentypId]("custom-kundetyp-id")

  implicit val pendenzModifyPersister = persister[PendenzModify]("pendenz-modify")
  implicit val pendenzIdPersister = persister[PendenzId]("pendenz-id")
  implicit val pendenzCreatePersister = persister[PendenzCreate]("pendenz-create")

  implicit val lieferungAbotypCreatePersister = persister[LieferungAbotypCreate]("lieferung-abotyp-create")
  implicit val lieferungenAbotypCreatePersister = persister[LieferungenAbotypCreate]("lieferungen-abotyp-create")
  implicit val lieferungIdPersister = persister[LieferungId]("lieferung-id")
  implicit val lieferungOnLieferplanungIdPersister = persister[LieferungOnLieferplanungId]("lieferung-on-lieferplanung-id")
  implicit val lieferungModifyPersister = persister[LieferungModify]("lieferung-modify")
  implicit val lieferungAbgeschlossenModifyPersister = persister[LieferungAbgeschlossenModify]("lieferung-abgeschlossen-modify")
  implicit val lieferungPlanungAddPersister = persister[LieferungPlanungAdd]("lieferung-planungadd-modify")
  implicit val lieferungPlanungRemovePersister = persister[LieferungPlanungRemove]("lieferung-planungremove-modify")
  implicit val lieferplanungModifyPersister = persister[LieferplanungModify]("lieferplanung-modify")
  implicit val lieferplanungCreatePersister = persister[LieferplanungCreate]("lieferplanung-create")
  implicit val lieferplanungDataModifyPersister = persister[LieferplanungDataModify]("lieferplanung-data-modify")
  implicit val lieferplanungIdPersister = persister[LieferplanungId]("lieferplanung-id")
  implicit val lieferpositionModifyPersister = persister[LieferpositionModify]("lieferposition-modify")
  implicit val lieferpositionenCreatePersister = persister[LieferpositionenModify]("lieferpositionen-create")
  implicit val lieferpositionIdPersister = persister[LieferpositionId]("lieferposition-id")
  implicit val bestellungIdPersister = persister[BestellungId]("bestellung-id")
  implicit val sammelbestellungIdPersister = persister[SammelbestellungId]("sammelbestellung-id")
  implicit val sammelbestellungModifyPersister = persister[SammelbestellungModify]("bestellung-create") // use the same identifier as before with bestellung; the structure is the same
  implicit val bestellpositionModifyPersister = persister[BestellpositionModify]("bestellposition-modify")
  implicit val bestellpositionIdPersister = persister[BestellpositionId]("bestellposition-id")
  implicit val auslieferungIdPersister = persister[AuslieferungId]("auslieferung-id")

  implicit val produktModifyPersister = persister[ProduktModify]("produkt-modify")
  implicit val produktIdPersister = persister[ProduktId]("produkt-id")

  implicit val produktkategorieModifyPersister = persister[ProduktekategorieModify]("produktekategorie-modify")
  implicit val produktkategorieIdPersister = persister[ProduktekategorieId]("produktekategorie-id")

  implicit val produzentModifyPersister = persister[ProduzentModify]("produzent-modify")
  implicit val produzentIdPersister = persister[ProduzentId]("produzent-id")

  implicit val tourCreatePersiter = persister[TourCreate]("tour-create")
  implicit val tourModifyPersiter = persister[TourModify]("tour-modify")
  implicit val tourIdPersister = persister[TourId]("tour-id")

  implicit val vorlageCreatePersister = persister[ProjektVorlageCreate]("projekt-vorlage-create")
  implicit val vorlageModifyPersister = persister[ProjektVorlageModify]("projekt-vorlage-modify")
  implicit val vorlageUploadPersister = persister[ProjektVorlageUpload]("projekt-vorlage-upload")
  implicit val vorlageIdPersister = persister[ProjektVorlageId]("projekt-vorlage-id")

  implicit val projektModifyPersister = persister[ProjektModify, V7](
    "projekt-modify",
    from[V1]
      .to[V2](_.update(Symbol("sprache") ! set[Locale](Locale.GERMAN)))
      .to[V3](_.update(Symbol("maintenanceMode") ! set[Boolean](false)))
      .to[V4](_.update(Symbol("generierteMailsSenden") ! set[Boolean](false))
        .update(Symbol("einsatzEinheit") ! set[EinsatzEinheit](Halbtage))
        .update(Symbol("einsatzAbsageVorlaufTage") ! set[Int](3))
        .update(Symbol("einsatzShowListeKunde") ! set[Boolean](true)))
      .to[V5](_.update(Symbol("sendEmailToBcc") ! set[Boolean](true)))
      .to[V6](_.update(Symbol("messageForMembers") ! set[Option[String]](None)))
      .to[V7](_.update(Symbol("defaultSecondFactorType") ! set[SecondFactorType](EmailSecondFactorType)))
  )

  implicit val projektIdPersister = persister[ProjektId]("projekt-id")

  // custom events
  import ch.openolitor.core.eventsourcing.events._

  implicit val lieferplanungAbschliessenEventPersister = persister[LieferplanungAbschliessenEvent, V2]("lieferplanung-abschliessen-event", V1toV2metaDataMigration)
  implicit val lieferplanungAbrechnenEventPersister = persister[LieferplanungAbrechnenEvent, V2]("lieferplanung-abrechnen-event", V1toV2metaDataMigration)
  implicit val lieferplanungDataModifiedEventPersister = persister[LieferplanungDataModifiedEvent, V2]("lieferplanung-data-modified-event", V1toV2metaDataMigration)
  implicit val abwesenheitCreateEventPersister = persister[AbwesenheitCreateEvent, V2]("abwesenheit-create-event", V1toV2metaDataMigration)
  implicit val sammelbestellungVersendenEventPersister = persister[SammelbestellungVersendenEvent, V2]("lieferung-bestellen-event", V1toV2metaDataMigration) // use the same identifier as before with bestellung; the structure is the same
  implicit val passwortGewechseltEventPersister = persister[PasswortGewechseltEvent, V2]("passwort-gewechselt", V1toV2metaDataMigration)
  implicit val loginDeaktiviertEventPersister = persister[LoginDeaktiviertEvent, V2]("login-deaktiviert", V1toV2metaDataMigration)
  implicit val loginAktiviertEventPersister = persister[LoginAktiviertEvent, V2]("login-aktiviert", V1toV2metaDataMigration)

  implicit val kontoDatenModifyPersister = persister[KontoDatenModify]("konto-daten-modify")
  implicit val kontoDatenIdPersister = persister[KontoDatenId]("konto-daten-id")

  implicit val korbCreatePersister = persister[KorbCreate]("korb-create")
  implicit val korbModifyAuslieferungPersister = persister[KorbAuslieferungModify]("korb-modify-auslieferung")

  implicit val tourAuslieferungModifyPersister = persister[TourAuslieferungModify]("tour-auslieferung-modify")
  implicit val depotAuslieferungPersister = persister[DepotAuslieferung]("depot-auslieferung")
  implicit val tourAuslieferungPersister = persister[TourAuslieferung]("tour-auslieferung")
  implicit val postAuslieferungPersister = persister[PostAuslieferung]("post-auslieferung")

  implicit val auslieferungAlsAusgeliefertMarkierenEventPersister = persister[AuslieferungAlsAusgeliefertMarkierenEvent, V2]("auslieferung-als-ausgeliefert-markieren-event", V1toV2metaDataMigration)
  implicit val sammelbestellungAlsAbgerechnetMarkierenEventPersister = persister[SammelbestellungAlsAbgerechnetMarkierenEvent, V2]("bestellung-als-ausgeliefert-markieren-event", V1toV2metaDataMigration) // use the same identifier as before with bestellung; the structure is the same

  implicit val einladungIdPersister = persister[EinladungId]("einladung-id")
  implicit val einladungCreatePersister = persister[EinladungCreate]("einladung-create")
  implicit val einladungGesendetEventPersister = persister[EinladungGesendetEvent, V2]("einladung-gesendet", V1toV2metaDataMigration)
  implicit val passwortResetGesendetEventPersister = persister[PasswortResetGesendetEvent, V2]("passwort-reset-gesendet", V1toV2metaDataMigration)
  implicit val rolleGewechseltEventPersister = persister[RolleGewechseltEvent, V2]("rolle-gewechselt-gesendet", V1toV2metaDataMigration)

  implicit val sendEmailToPersonEventPersister = persister[SendEmailToPersonEvent]("send-email-person")
  implicit val sendEmailToPersonEventV2Persister = persister[SendEmailToPersonEvent, V2]("send-email-person", from[V1]
    .to[V2](_.update(Symbol("person") / Symbol("contactPermission") ! set[Boolean](false))))
  implicit val sendEmailToKundeEventPersister = persister[SendEmailToKundeEvent]("send-email-kunde")
  implicit val sendEmailToKundeEventV2Persister = persister[SendEmailToKundeEvent, V3]("send-email-kunde", from[V1]
    .to[V2](_.update(Symbol("person") / Symbol("contactPermission") ! set[Boolean](false)))
    .to[V3](_.update(Symbol("context") / Symbol("kunde") / Symbol("aktiv") ! set[Boolean](true))))

  implicit val sendEmailToAboSubscriberEventPersister = persister[SendEmailToAboSubscriberEvent]("send-email-abo-subscriber")
  implicit val sendEmailToAboSubscriberEventV2Persister = persister[SendEmailToAboSubscriberEvent, V2]("send-email-abo-subscriber", from[V1]
    .to[V2](_.update(Symbol("person") / Symbol("contactPermission") ! set[Boolean](false))))
  implicit val sendEmailToAbotypSubscriberEventPersister = persister[SendEmailToAbotypSubscriberEvent]("send-email-abotyp-subscriber")
  implicit val sendEmailToAbotypSubscriberEventV2Persister = persister[SendEmailToAbotypSubscriberEvent, V2]("send-email-abotyp-subscriber", from[V1]
    .to[V2](_.update(Symbol("person") / Symbol("contactPermission") ! set[Boolean](false))))
  implicit val sendEmailToZusatzabotypSubscriberEventPersister = persister[SendEmailToZusatzabotypSubscriberEvent]("send-email-zusatzabotyp-subscriber")
  implicit val sendEmailToZusatzabotypSubscriberEventV2Persister = persister[SendEmailToZusatzabotypSubscriberEvent, V2]("send-email-zusatzabotyp-subscriber", from[V1]
    .to[V2](_.update(Symbol("person") / Symbol("contactPermission") ! set[Boolean](false))))
  implicit val sendEmailToTourSubscriberEventPersister = persister[SendEmailToTourSubscriberEvent]("send-email-tour-subscriber")
  implicit val sendEmailToTourSubscriberEventV2Persister = persister[SendEmailToTourSubscriberEvent, V2]("send-email-tour-subscriber", from[V1]
    .to[V2](_.update(Symbol("person") / Symbol("contactPermission") ! set[Boolean](false))))
  implicit val sendEmailToDepotSubscriberEventPersister = persister[SendEmailToDepotSubscriberEvent]("send-email-depot-subscriber")
  implicit val sendEmailToDepotSubscriberEventV2Persister = persister[SendEmailToDepotSubscriberEvent, V2]("send-email-depot-subscriber", from[V1]
    .to[V2](_.update(Symbol("person") / Symbol("contactPermission") ! set[Boolean](false))))

  implicit val aboAktiviertEventPersister = persister[AboAktiviertEvent, V2]("abo-aktiviert-event", V1toV2metaDataMigration)
  implicit val aboDeaktiviertEventPersister = persister[AboDeaktiviertEvent, V2]("abo-deaktiviert-event", V1toV2metaDataMigration)

  implicit val otpResetEventPersistet = persister[OtpResetEvent]("otp-reset-event")

  val stammdatenPersisters = List(
    depotModifyPersister,
    depotIdPersister,
    aboIdPersister,
    abotypModifyPersister,
    abotypLetzteLieferungModifyPersister,
    abotypIdPersister,
    zusatzAbotypModifyPersister,
    zusatzAboModifyPersister,
    zusatzAboCreatePersister,
    kundeModifyPersister,
    kundeIdPersister,
    personCreateV3Persister,
    personCategoryIdPersister,
    personCategoryCreatePersister,
    personCategoryModifyPersister,
    pendenzIdPersister,
    pendenzCreatePersister,
    vertriebModifyPersister,
    vertriebRecalculationsModifyPersister,
    vertriebIdPersister,
    vertriebsartDLPersister,
    vertriebsartPLPersister,
    vertriebsartHLPersister,
    vertriebsartIdPersister,
    vertriebsartDLAbotypPersister,
    vertriebsartPLAbotypPersister,
    vertriebsartHLAbotypPersister,
    aboDLPersister,
    aboPLPersister,
    aboHLPersister,
    aboPriceModifyPersister,
    aboDLCreatePersister,
    aboPLCreatePersister,
    aboHLCreatePersister,
    aboGuthabenModifyPersister,
    aboVertriebsartModifyPersister,
    customKundetypCreatePersister,
    customKundetypModifyV1Persister,
    customKundetypModifyPersister,
    customKundetypIdPersister,
    pendenzModifyPersister,
    lieferungAbotypCreatePersister,
    lieferungenAbotypCreatePersister,
    lieferungIdPersister,
    lieferungOnLieferplanungIdPersister,
    lieferungModifyPersister,
    lieferungAbgeschlossenModifyPersister,
    lieferungPlanungAddPersister,
    lieferungPlanungRemovePersister,
    lieferplanungModifyPersister,
    lieferplanungIdPersister,
    lieferplanungCreatePersister,
    lieferpositionModifyPersister,
    lieferpositionenCreatePersister,
    lieferpositionIdPersister,
    bestellungIdPersister,
    sammelbestellungIdPersister,
    sammelbestellungModifyPersister,
    bestellpositionModifyPersister,
    bestellpositionIdPersister,
    produktIdPersister,
    produktModifyPersister,
    produktkategorieModifyPersister,
    produktkategorieIdPersister,
    produzentModifyPersister,
    produzentIdPersister,
    tourCreatePersiter,
    tourModifyPersiter,
    tourIdPersister,
    projektModifyPersister,
    projektIdPersister,
    abwesenheitCreatePersister,
    abwesenheitIdPersister,
    korbCreatePersister,
    korbModifyAuslieferungPersister,
    tourAuslieferungModifyPersister,
    depotAuslieferungPersister,
    tourAuslieferungPersister,
    postAuslieferungPersister,
    auslieferungIdPersister,
    vorlageCreatePersister,
    vorlageModifyPersister,
    vorlageUploadPersister,
    vorlageIdPersister,
    einladungIdPersister,
    einladungCreatePersister,
    kontoDatenIdPersister,
    kontoDatenModifyPersister,

    //event persisters
    lieferplanungAbschliessenEventPersister,
    lieferplanungAbrechnenEventPersister,
    lieferplanungDataModifiedEventPersister,
    abwesenheitCreateEventPersister,
    sammelbestellungVersendenEventPersister,
    passwortGewechseltEventPersister,
    loginDeaktiviertEventPersister,
    loginAktiviertEventPersister,
    auslieferungAlsAusgeliefertMarkierenEventPersister,
    sammelbestellungAlsAbgerechnetMarkierenEventPersister,
    einladungGesendetEventPersister,
    passwortResetGesendetEventPersister,
    rolleGewechseltEventPersister,
    sendEmailToPersonEventV2Persister,
    sendEmailToKundeEventV2Persister,
    sendEmailToAboSubscriberEventV2Persister,
    sendEmailToAbotypSubscriberEventV2Persister,
    sendEmailToZusatzabotypSubscriberEventV2Persister,
    sendEmailToTourSubscriberEventV2Persister,
    sendEmailToDepotSubscriberEventV2Persister,
    aboAktiviertEventPersister,
    aboDeaktiviertEventPersister,
    korbIdPersister,
    otpResetEventPersistet
  ) ++ mailTemplatePersisters

  def fixToOptionLocalDate(in: JsValue, attribute: Symbol): JsValue = {
    // convert wrong date js values
    val dateTimeOption = in.extract[DateTime](attribute.?)
    dateTimeOption match {
      case Some(dateTime) =>
        val hour = dateTime.getHourOfDay
        if (hour > 12) {
          in.update(attribute ! set[Option[LocalDate]](Some(dateTime.plusHours(24 - hour).toLocalDate)))
        } else {
          in
        }
      case None =>
        in
    }
  }

  def fixToLocalDate(in: JsValue, attribute: Symbol): JsValue = {
    // convert wrong date js values
    val dateTime = in.extract[DateTime](attribute)
    val hour = dateTime.getHourOfDay
    if (hour > 12) {
      in.update(attribute ! set[LocalDate](dateTime.plusHours(24 - hour).toLocalDate))
    } else {
      in
    }
  }

  def fixPersonModifyInKundeModifyV2(in: JsValue, attribute: Symbol): JsValue = {
    val personen = in.extract[Set[PersonModifyV1]](attribute)
    val emptySet = Set[PersonCategoryNameId]()
    val personenV3 = personen map { person =>
      copyTo[PersonModifyV1, PersonModifyV2](person, "categories" -> emptySet)
    }
    in.update(attribute ! set[Set[PersonModifyV2]](personenV3))
  }

  def fixPersonModifyContactPermissionInKundeModify(in: JsValue, attribute: Symbol): JsValue = {
    val personen = in.extract[Set[PersonModifyV2]](attribute)
    val personenV3 = personen map { person =>
      copyTo[PersonModifyV2, PersonModifyV3](person, "contactPermission" -> False)
    }
    in.update(attribute ! set[Set[PersonModifyV3]](personenV3))
  }

  def fixPersonModifyInKundeModifyV3(in: JsValue, attribute: Symbol): JsValue = {
    val personenV3 = in.extract[Set[PersonModifyV3]](attribute)
    val personen = personenV3 map { person =>
      copyTo[PersonModifyV3, PersonModify](person, "secondFactorType" -> None)
    }
    in.update(attribute ! set[Set[PersonModify]](personen))
  }

}
