/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.kiwix.kiwixmobile.core.entity

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue

@Serializable
@XmlSerialName("metalink", namespace = "urn:ietf:params:xml:ns:metalink", prefix = "")
class MetaLinkNetworkEntity {
  @XmlElement(true)
  @XmlSerialName("file", namespace = "urn:ietf:params:xml:ns:metalink", prefix = "")
  var file: FileElement? = null
  val urls: List<Url>?
    get() = file?.urls
  val relevantUrl: Url
    get() = file?.urls?.get(0) ?: Url()

  @Serializable
  @XmlSerialName("file", namespace = "urn:ietf:params:xml:ns:metalink", prefix = "")
  class FileElement {
    @XmlElement(false)
    var name: String? = null

    @XmlElement(true)
    @XmlSerialName("url", namespace = "urn:ietf:params:xml:ns:metalink", prefix = "")
    var urls: List<Url>? = null

    @XmlElement(true)
    @XmlSerialName("size", namespace = "urn:ietf:params:xml:ns:metalink", prefix = "")
    var size: Long = 0

    @XmlElement(true)
    @XmlSerialName("hash", namespace = "urn:ietf:params:xml:ns:metalink", prefix = "")
    var hashes: List<Hash>? = null

    @XmlElement(true)
    @XmlSerialName("pieces", namespace = "urn:ietf:params:xml:ns:metalink", prefix = "")
    var pieces: Pieces? = null

    val pieceHashes: List<String>?
      get() = pieces?.pieceHashStrings

    fun getHash(type: String): String? = hashes?.find { it.type == type }?.value
  }

  @Serializable
  @XmlSerialName("hash", namespace = "urn:ietf:params:xml:ns:metalink", prefix = "")
  class Hash {
    @XmlElement(false)
    var type: String = ""

    @XmlValue
    var value: String = ""

    constructor()

    constructor(type: String, value: String) {
      this.type = type
      this.value = value
    }
  }

  @Serializable
  @XmlSerialName("pieces", namespace = "urn:ietf:params:xml:ns:metalink", prefix = "")
  class Pieces {
    @XmlElement(false)
    var length = 0

    @XmlElement(false)
    @XmlSerialName("type")
    var hashType: String? = null

    @XmlElement(true)
    @XmlSerialName("hash", namespace = "urn:ietf:params:xml:ns:metalink", prefix = "")
    var pieceHashes: List<PieceHash>? = null

    val pieceHashStrings: List<String>?
      get() = pieceHashes?.map { it.value }
  }

  @Serializable
  @XmlSerialName("hash", namespace = "urn:ietf:params:xml:ns:metalink", prefix = "")
  class PieceHash {
    @XmlValue
    var value: String = ""

    constructor()

    constructor(value: String) {
      this.value = value
    }
  }

  @Serializable
  @XmlSerialName("url", namespace = "urn:ietf:params:xml:ns:metalink", prefix = "")
  class Url {
    @XmlElement(false)
    var location: String? = null

    @XmlElement(false)
    var priority = 0

    @XmlValue
    var value: String? = null
  }
}
