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
package ch.openolitor.stammdaten.models

import ch.openolitor.stammdaten._
import ch.openolitor.core.models._
import java.util.UUID

sealed trait Liefersaison extends Product
sealed trait Monat extends Liefersaison

object Liefersaison {
  def apply(value: String): Liefersaison = Monat.apply(value).get
}

case object Januar extends Monat
case object Februar extends Monat
case object Maerz extends Monat
case object April extends Monat
case object Mai extends Monat
case object Juni extends Monat
case object Juli extends Monat
case object August extends Monat
case object September extends Monat
case object Oktober extends Monat
case object November extends Monat
case object Dezember extends Monat

object Monat {
  def apply(value: String): Option[Monat] = {
    Vector(Januar, Februar, Maerz, April, Mai, Juni, Juli, August, September, Oktober, November, Dezember).find(_.toString == value)
  }
}

sealed trait Liefereinheit extends Product
case object Stueck extends Liefereinheit
case object Bund extends Liefereinheit
case object Gramm extends Liefereinheit
case object Kilogramm extends Liefereinheit

object Liefereinheit {
  def apply(value: String): Liefereinheit = {
    Vector(Stueck, Bund, Gramm, Kilogramm).find(_.toString == value).getOrElse(Kilogramm)
  }
}

case class BaseProduktekategorieId(id: String)

case class ProduktekategorieId(id: UUID) extends BaseId

case class Produktekategorie(id: ProduktekategorieId,
  val beschreibung: String) extends BaseEntity[ProduktekategorieId]  {
}

case class ProduktekategorieModify(beschreibung: String) extends Product 

case class ProduktId(id: UUID) extends BaseId

case class Produkt(
  id: ProduktId,
  name: String,
  verfuegbarVon: Liefersaison,
  verfuegbarBis: Liefersaison,
  kategorien: Seq[String],
  einheit: Liefereinheit,
  preis: BigDecimal,
  produzenten: Seq[String]
) extends BaseEntity[ProduktId]

case class ProduktModify(
  name: String,
  verfuegbarVon: Liefersaison,
  verfuegbarBis: Liefersaison,
  kategorien: Seq[String],
  einheit: Liefereinheit,
  preis: BigDecimal,
  produzenten: Seq[String]
) extends Product 

case class ProduktProduzentId(id: UUID) extends BaseId

case class ProduktProduzent(
  id: ProduktProduzentId, 
  produktId: ProduktId, 
  produzentId: ProduzentId
) extends BaseEntity[ProduktProduzentId]

case class ProduktProduktekategorieId(id: UUID) extends BaseId

case class ProduktProduktekategorie(
  id: ProduktProduktekategorieId, 
  produktId: ProduktId, 
  produktekategorieId: ProduktekategorieId
) extends BaseEntity[ProduktProduktekategorieId]

