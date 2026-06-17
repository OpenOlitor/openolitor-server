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
package ch.openolitor.core.eventsourcing

import com.typesafe.scalalogging.LazyLogging
import stamina._
import stamina.json._
import org.apache.pekko.serialization.Serializer

/**
 * Base serializer for stamina-based serialization using Pekko.
 * This is a Pekko-compatible version of StaminaAkkaSerializer.
 */
abstract class StaminaPekkoSerializer(persisters: Persisters) extends Serializer with LazyLogging {

  override def identifier: Int = 1001 // Custom identifier for stamina serializer

  override def includeManifest: Boolean = true

  override def toBinary(obj: AnyRef): Array[Byte] = {
    if (persisters.canPersist(obj)) {
      val persisted = persisters.persist(obj)
      logger.debug(s"StaminaPekkoSerializer: toBinary: $obj")
      persisted.bytes.toArray
    } else {
      throw new IllegalArgumentException(s"Cannot persist object of type: ${obj.getClass.getName}")
    }
  }

  override def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): AnyRef = {
    val manifestStr = manifest.map(_.getName).getOrElse("")
    val persisted = stamina.Persisted(manifestStr, 0, bytes)
    val result = persisters.unpersist(persisted)
    logger.debug(s"StaminaPekkoSerializer: fromBinary: $result")
    result
  }
}
