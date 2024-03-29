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

import ch.openolitor.buchhaltung.BuchhaltungDBMappings
import ch.openolitor.core.{ NoPublishEventStream, SystemConfig }
import ch.openolitor.core.db.evolution.Script
import ch.openolitor.core.db.evolution.scripts.DefaultDBScripts
import ch.openolitor.stammdaten.StammdatenDBMappings
import ch.openolitor.stammdaten.models.Person
import ch.openolitor.core.models.PersonId
import ch.openolitor.core.security.SystemSubject
import ch.openolitor.stammdaten.repositories.StammdatenWriteRepositoryImpl
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.util.OtpUtil
import scalikejdbc._

import scala.util.{ Success, Try }

object OO_add_otp_second_factor {

  val updateProjekt = new Script with LazyLogging with BuchhaltungDBMappings with DefaultDBScripts {
    def execute(sysConfig: SystemConfig)(implicit session: DBSession): Try[Boolean] = {
      alterTableAddColumnIfNotExists(projektMapping, "default_second_factor_type", "VARCHAR(10)", "two_factor_authentication")
      sql"""UPDATE Projekt SET default_second_factor_type='email'""".execute.apply()
      Success(true)
    }
  }

  val updatePersonen = new Script with LazyLogging with BuchhaltungDBMappings with DefaultDBScripts with StammdatenDBMappings with StammdatenWriteRepositoryImpl with NoPublishEventStream {
    def execute(sysConfig: SystemConfig)(implicit session: DBSession): Try[Boolean] = {
      alterTableAddColumnIfNotExists(personMapping, "second_factor_type", "VARCHAR(10)", "categories")
      alterTableAddColumnIfNotExists(personMapping, "otp_secret", "VARCHAR(200)", "second_factor_type")
      alterTableAddColumnIfNotExists(personMapping, "otp_reset", "VARCHAR(1)", "otp_secret")

      // update all otpReset to true
      sql"""UPDATE Person SET otp_reset='1'""".execute.apply()

      // generate otpSecret for all persons
      implicit val personId = SystemSubject.systemPersonId

      val persons = getPersonen
      persons map { person =>
        val otpSecret = OtpUtil.generateOtpSecretString
        if (person.otpSecret == null) {
          updateEntity[Person, PersonId](person.id)(personMapping.column.otpSecret -> otpSecret, personMapping.column.otpReset -> true)
        }
      }

      Success(true)
    }
  }

  val scripts = Seq(updateProjekt, updatePersonen)
}
