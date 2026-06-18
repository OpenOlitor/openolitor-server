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
import org.apache.pekko.serialization.Serializer

/**
 * Base serializer for stamina-based serialization using Pekko.
 * This is a Pekko-compatible version of StaminaAkkaSerializer.
 */
abstract class StaminaPekkoSerializer private(persisters: Persisters, codec: PersistedCodec) extends Serializer with LazyLogging {
  def this(persisters: List[Persister[_, _]], codec: PersistedCodec = DefaultPersistedCodec) = this(Persisters(persisters), codec)
  def this(persister: Persister[_, _], persisters: Persister[_, _]*) = this(Persisters(persister :: persisters.toList), DefaultPersistedCodec)

  private val akkaSerializer = new StaminaAkkaSerializer(persisters.persisters, codec) {}
  val includeManifest: Boolean = akkaSerializer.includeManifest
  val identifier: Int = akkaSerializer.identifier

  override def toBinary(obj: AnyRef): Array[Byte] = {
    akkaSerializer.toBinary(obj)
  }

  override def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): AnyRef = {
    akkaSerializer.fromBinary(bytes, manifest)
  }
}
