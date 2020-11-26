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
package ch.openolitor.core.db.evolution.scripts.v2

import ch.openolitor.core.db.evolution._
import ch.openolitor.core.repositories.CoreDBMappings
import ch.openolitor.core.db.evolution.scripts.DefaultDBScripts
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.SystemConfig
import scalikejdbc._
import scala.util.{ Try, Success }
import org.joda.time.DateTime
import ch.openolitor.core.Boot
import ch.openolitor.core.repositories.CoreRepositoryQueries
import ch.openolitor.core.models.PersistenceEventState
import ch.openolitor.core.models.PersistenceEventStateId
import akka.actor.ActorSystem
import ch.openolitor.stammdaten.StammdatenDBMappings
import ch.openolitor.arbeitseinsatz.ArbeitseinsatzDBMappings

object V2Scripts {

  def oo656(sys: ActorSystem) = new Script with LazyLogging with CoreDBMappings with DefaultDBScripts with CoreRepositoryQueries {
    lazy val system: ActorSystem = sys

    def execute(sysConfig: SystemConfig)(implicit session: DBSession): Try[Boolean] = {
      logger.debug(s"creating PersistenceEventState")

      sql"""drop table if exists ${persistenceEventStateMapping.table}""".execute.apply()

      sql"""create table ${persistenceEventStateMapping.table}  (
        id BIGINT not null,
        persistence_id varchar(100) not null,
        last_transaction_nr BIGINT default 0,
        last_sequence_nr BIGINT default 0,
        erstelldat datetime not null,
        ersteller BIGINT not null,
        modifidat datetime not null,
        modifikator BIGINT not null)""".execute.apply()

      logger.debug(s"store last sequence number for actors and persistence views")
      val persistentActorStates = queryLatestPersistenceMessageByPersistenceIdQuery.apply() map { messagePerPersistenceId =>
        //find latest sequence nr
        logger.debug(s"OO-656: latest persistence id of persistentactor:${messagePerPersistenceId.persistenceId}, sequenceNr:${messagePerPersistenceId.sequenceNr}")
        PersistenceEventState(PersistenceEventStateId(), messagePerPersistenceId.persistenceId, messagePerPersistenceId.sequenceNr, 0L, DateTime.now, Boot.systemPersonId, DateTime.now, Boot.systemPersonId)
      }

      // append persistent views
      val persistentViewStates = persistentActorStates filter (_.persistenceId == "entity-store") flatMap (newState =>
        Seq("buchhaltung", "stammdaten") map { module =>
          logger.debug(s"OO-656: latest persistence id of persistentview:$module-entity-store, sequenceNr:${newState.lastTransactionNr}")
          PersistenceEventState(PersistenceEventStateId(), s"$module-entity-store", newState.lastTransactionNr, 0L, DateTime.now, Boot.systemPersonId, DateTime.now, Boot.systemPersonId)
        })

      implicit val personId = Boot.systemPersonId
      (persistentActorStates ++ persistentViewStates) map { entity =>
        val params = persistenceEventStateMapping.parameterMappings(entity)
        withSQL(insertInto(persistenceEventStateMapping).values(params: _*)).update.apply()
      }

      // stop all entity-store snapshots due to class incompatiblity
      sql"""truncate persistence_snapshot""".execute.apply()

      Success(true)
    }
  }

  val oo688 = new Script with LazyLogging with StammdatenDBMappings with DefaultDBScripts {

    def execute(sysConfig: SystemConfig)(implicit session: DBSession): Try[Boolean] = {
      sql"""create table ${zusatzAbotypMapping.table} (
        id BIGINT not null,
        name varchar(50) not null,
        beschreibung varchar(256),
        aktiv_von datetime default null,
        aktiv_bis datetime default null,
        preis DECIMAL(7,2) not null,
        preiseinheit varchar(20) not null,
        laufzeit int,
        laufzeiteinheit varchar(50),
        vertragslaufzeit varchar(50),
        kuendigungsfrist varchar(50),
        anzahl_abwesenheiten int,
        anzahl_einsaetze DECIMAL(5,2),
        farb_code varchar(20),
        zielpreis DECIMAL(7,2),
        guthaben_mindestbestand int,
        admin_prozente DECIMAL(5,2),
        wird_geplant varchar(1) not null,
        anzahl_abonnenten INT not null,
        anzahl_abonnenten_aktiv INT not null,
        letzte_lieferung datetime default null,
        waehrung varchar(10),
        erstelldat datetime not null,
        ersteller BIGINT not null,
        modifidat datetime not null,
        modifikator BIGINT not null,
        KEY `id_index` (`id`))""".execute.apply()

      sql"""create table ${zusatzAboMapping.table}  (
        id BIGINT not null,
        haupt_abo_id BIGINT not null,
        haupt_abotyp_id BIGINT not null,
        abotyp_id BIGINT not null,
        abotyp_name varchar(50),
        kunde_id BIGINT not null,
        kunde varchar(100),
        vertriebsart_id BIGINT not null,
        vertrieb_id BIGINT not null,
        vertrieb_beschrieb varchar(2000),
        start datetime not null,
        ende datetime,
        guthaben_vertraglich int,
        guthaben int not null default 0,
        guthaben_in_rechnung int not null default 0,
        letzte_lieferung datetime,
        anzahl_abwesenheiten varchar(500),
        anzahl_lieferungen varchar(500),
        anzahl_einsaetze varchar(500),
        aktiv varchar(1),
        erstelldat datetime not null,
        ersteller BIGINT not null,
        modifidat datetime not null,
        modifikator BIGINT not null,
        KEY `id_index` (`id`))""".execute.apply()

      Success(true)
    }
  }

  val arbeitseinsatzDBInitializationScript = new Script with LazyLogging with ArbeitseinsatzDBMappings {
    def execute(sysConfig: SystemConfig)(implicit session: DBSession): Try[Boolean] = {
      //drop all tables
      logger.debug(s"oo-system: cleanupDatabase - drop tables - arbeitseinsatz")

      sql"drop table if exists ${arbeitskategorieMapping.table}".execute.apply()
      sql"drop table if exists ${arbeitsangebotMapping.table}".execute.apply()
      sql"drop table if exists ${arbeitseinsatzMapping.table}".execute.apply()

      logger.debug(s"oo-system: cleanupDatabase - create tables - arbeitseinsatz")
      //create tables

      sql"""create table ${arbeitskategorieMapping.table} (
        id BIGINT not null,
        beschreibung varchar(200),
        erstelldat datetime not null,
        ersteller BIGINT not null,
        modifidat datetime not null,
        modifikator BIGINT not null,
        KEY `id_index` (`id`))""".execute.apply()

      sql"""create table ${arbeitsangebotMapping.table} (
        id BIGINT not null,
        kopie_von BIGINT,
        titel varchar(200) not null,
        bezeichnung varchar(200),
        ort varchar(500),
        zeit_von datetime not null,
        zeit_bis datetime,
        arbeitskategorien varchar(1000),
        anzahl_eingeschriebene DECIMAL(3,0) not null,
        anzahl_personen DECIMAL(3,0),
        mehr_personen_ok varchar(1) not null,
        einsatz_zeit DECIMAL(5,2),
        status varchar(20) not null,
        erstelldat datetime not null,
        ersteller BIGINT not null,
        modifidat datetime not null,
        modifikator BIGINT not null,
        KEY `id_index` (`id`))""".execute.apply()

      sql"""create table ${arbeitseinsatzMapping.table} (
        id BIGINT not null,
        arbeitsangebot_id BIGINT,
        arbeitsangebot_titel varchar(200) not null,
        arbeitsangebot_status varchar(20) not null,
        zeit_von datetime not null,
        zeit_bis datetime,
        einsatz_zeit DECIMAL(5,2),
        kunde_id BIGINT not null,
        kunde_bezeichnung varchar(200),
        person_id BIGINT,
        person_name varchar(50),
        abo_id BIGINT,
        abo_bezeichnung varchar(50),
        anzahl_personen DECIMAL(3,0),
        bemerkungen varchar(300),
        erstelldat datetime not null,
        ersteller BIGINT not null,
        modifidat datetime not null,
        modifikator BIGINT not null,
        KEY `id_index` (`id`))""".execute.apply()

      logger.debug(s"oo-system: cleanupDatabase - end - arbeitseinsatz")
      Success(true)
    }
  }

  def scripts(system: ActorSystem) = Seq(oo656(system), oo688) ++
    OO686_Add_Rechnungspositionen.scripts ++
    OO697_Zusatzabos_add_modify_delete.scripts ++
    OO731_Reports.scripts ++
    OO760_add_missing_keys.scripts ++
    OO829_add_zusatzabo_info_to_abo.scripts ++
    OO846_delete_unmapped_zusatzabos.scripts ++
    OO854_zusatzinfo_lieferung_200.scripts ++
    OO861_recalculate_lieferung_counts.scripts ++
    OO900_Guthaben.scripts ++
    OO942_EnlargeFileRef.scripts ++
    OOBetrieb2_recalculate_aktive_inaktive_accounts.scripts ++
    OO762_Mail_Templates.scripts ++
    OO_sunu_7_maintenance_mode_zero.scripts ++
    OO_sunu_4_person_category.scripts ++
    OO_sunu_4_2_adding_price_to_abo.scripts ++
    OO_sunu_9_adding_creditor_identifier.scripts ++
    OO_sunu_11_adding_payment_parameters_to_kunde.scripts ++
    OO_sunu_13_adding_payment_type_to_rechnung.scripts ++
    OO_sunu_14_new_zahlung_export_table.scripts ++
    OO_sunu_26_adding_payment_types.scripts ++
    OO_sunu_27_adding_status_to_zahlungs_export.scripts ++
    Seq(arbeitseinsatzDBInitializationScript) ++
    OO109_Arbeitseinsatz.scripts ++
    ArbeitseinsatzExt.scripts ++
    OO199_Coordinates.scripts ++
    LPHTMLEditor.scripts ++
    OO302_Add_Bcc_field.scripts ++
    OO_sunu_adding_bic_to_account.scripts ++
    OO_github_39_adding_mandantId_DateOfSignature.scripts ++
    OO_gitlab_373_ZahlungsEingang_teilnehmer_nummer.scripts ++
    OO_distribution_date_time_should_not_consider_time.scripts ++
    Operations465_EnlargeKundeBez.scripts ++
    OO411_adding_person_contact_permission.scripts ++
    OO86_enlarge_login_message_size.scripts ++
    OO86_create_kunden_specific_message.scripts ++
    OO_add_otp_second_facor.scripts
}
