package ch.openolitor.stammdaten.models

import ch.openolitor.core.models._
import org.joda.time.DateTime
import ch.openolitor.core.JSONSerializable

case class KontoDatenId(id: Long) extends BaseId

case class KontoDaten(
  id: KontoDatenId,
  nameAccountHolder: Option[String],
  addressAccountHolder: Option[String],
  bankName: Option[String],
  iban: Option[String],
  teilnehmerNummer: Option[String],
  referenzNummerPrefix: Option[String],
  creditorIdentifier: Option[String],
  kunde: Option[KundeId],
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[KontoDatenId]

case class KontoDatenModify(
  nameAccountHolder: Option[String],
  addressAccountHolder: Option[String],
  bankName: Option[String],
  iban: Option[String],
  teilnehmerNummer: Option[String],
  referenzNummerPrefix: Option[String],
  creditorIdentifier: Option[String]
  kunde: Option[KundeId]
) extends JSONSerializable
