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
package ch.openolitor.stammdaten.aboCreationTest

import com.typesafe.scalalogging.LazyLogging
import org.specs2.mutable._
import ch.openolitor.stammdaten.models._
import org.joda.time.LocalDateTime

class calculateAktivTest extends Specification with LazyLogging {

  "An abo starting yesterday before midnight and with no end date" should {
    "be active" in {
      val startsBeforeMidnight = LocalDateTime.now.minusDays(1).withTime(23, 59, 59, 999)
      IAbo.calculateAktiv(startsBeforeMidnight.toLocalDate, None) === true
    }
  }
  "An abo starting today after midnight and with no end date" should {
    "be active" in {
      val startsAfterMidnight = LocalDateTime.now.withTime(0, 0, 0, 1)
      IAbo.calculateAktiv(startsAfterMidnight.toLocalDate, None) === true
    }
  }
  "An abo starting today at midnight and with no end date" should {
    "be active" in {
      val startsAtMidnight = LocalDateTime.now.withTime(0, 0, 0, 0)
      IAbo.calculateAktiv(startsAtMidnight.toLocalDate, None) === true
    }
  }
  "An abo starting today before midday and with no end date" should {
    "be active" in {
      val startsBeforeMidday = LocalDateTime.now.withTime(11, 59, 59, 999)
      IAbo.calculateAktiv(startsBeforeMidday.toLocalDate, None) === true
    }
  }
  "An abo starting today after midday and with no end date" should {
    "be active" in {
      val startsAfterMidday = LocalDateTime.now.withTime(12, 0, 0, 1)
      IAbo.calculateAktiv(startsAfterMidday.toLocalDate, None) === true
    }
  }
  "An abo starting today at midday and with no end date" should {
    "be active" in {
      var startsAtMidday = LocalDateTime.now.withTime(12, 0, 0, 0)
      IAbo.calculateAktiv(startsAtMidday.toLocalDate, None) === true
    }
  }
  "An abo starting tomorrow after midnight and with no end date" should {
    "be inactive" in {
      var startsTomorrowAfterMidnight = LocalDateTime.now.plusDays(1).withTime(12, 0, 0, 0)
      IAbo.calculateAktiv(startsTomorrowAfterMidnight.toLocalDate, None) === false
    }
  }
  "An abo starting yesterday and finishihg today after midnight" should {
    "be active" in {
      val startsBeforeMidnight = LocalDateTime.now.minusDays(1).withTime(23, 59, 59, 999)
      val stopsBeforeMidnight = LocalDateTime.now.withTime(0, 0, 0, 1)
      IAbo.calculateAktiv(startsBeforeMidnight.toLocalDate, Some(stopsBeforeMidnight.toLocalDate)) === true
    }
  }
  "An abo starting yesterday midday and finishing yesterday midnight " should {
    "be inactive" in {
      val startsBeforeMidnight = LocalDateTime.now.minusDays(1).withTime(12, 0, 0, 0)
      val stopsBeforeMidnight = LocalDateTime.now.minusDays(1).withTime(23, 59, 59, 999)
      IAbo.calculateAktiv(startsBeforeMidnight.toLocalDate, Some(stopsBeforeMidnight.toLocalDate)) === false
    }
  }
}
