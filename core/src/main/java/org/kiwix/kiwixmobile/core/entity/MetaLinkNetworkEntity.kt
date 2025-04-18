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

import org.simpleframework.xml.Attribute
import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.ElementMap
import org.simpleframework.xml.Root
import org.simpleframework.xml.Text

@Root(strict = false, name = "metalink")
class MetaLinkNetworkEntity {
  @field:Element
  var file: FileElement? = null
  val urls: List<Url>?
    get() = file?.urls
  val relevantUrl: Url
    get() = file?.urls?.get(0) ?: Url()

  @Root(strict = false)
  class FileElement {
    @field:Attribute
    var name: String? = null

    @field:ElementList(inline = true, entry = "url")
    var urls: List<Url>? = null

    @field:Element var size: Long = 0

    @field:ElementMap(
      entry = "hash",
      key = "type",
      attribute = true,
      inline = true,
      required = false
    )
    var hashes: Map<String, String>? = null

    @field:Element(required = false)
    var pieces: Pieces? = null
    val pieceHashes: List<String>?
      get() = pieces?.pieceHashes

    /**
     * Get file hash
     *
     * @param type Hash type as defined in metalink file
     * @return Hash value or `null`
     */
    fun getHash(type: String): String? = hashes?.get(type)
  }

  class Pieces {
    @field:Attribute
    var length = 0

    @field:Attribute(name = "type")
    var hashType: String? = null

    @field:ElementList(inline = true, entry = "hash")
    var pieceHashes: List<String>? = null
  }

  class Url {
    @field:Attribute
    var location: String? = null

    @field:Attribute
    var priority = 0

    @field:Text
    var value: String? = null
  }
}
