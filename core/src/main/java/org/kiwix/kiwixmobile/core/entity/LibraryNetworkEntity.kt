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

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.TypeConverter
import org.simpleframework.xml.Attribute
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root
import java.io.File
import java.io.Serializable
import java.util.LinkedList

@Root(name = "library")
class LibraryNetworkEntity {
  @field:ElementList(name = "book", inline = true, required = false)
  var book: LinkedList<Book>? = null

  @field:Attribute(name = "version", required = false)
  var version: String? = null

  @Root(name = "book", strict = false)
  class Book : Serializable {
    @field:Attribute(name = "id", required = false)
    var id: String = ""

    @field:Attribute(name = "title", required = false)
    var title: String = ""

    @field:Attribute(name = "description", required = false)
    var description: String? = null

    @field:Attribute(name = "language", required = false)
    var language: String = ""

    @field:Attribute(name = "creator", required = false)
    var creator: String = ""

    @field:Attribute(name = "publisher", required = false)
    var publisher: String = ""

    @field:Attribute(name = "favicon", required = false)
    var favicon: String = ""

    @field:Attribute(name = "faviconMimeType", required = false)
    var faviconMimeType: String? = null

    @field:Attribute(name = "date", required = false)
    var date: String = ""

    @field:Attribute(name = "url", required = false)
    var url: String? = null

    @field:Attribute(name = "articleCount", required = false)
    var articleCount: String? = null

    @field:Attribute(name = "mediaCount", required = false)
    var mediaCount: String? = null

    @field:Attribute(name = "size", required = false)
    var size: String = ""

    @field:Attribute(name = "name", required = false)
    var bookName: String? = null

    @field:Attribute(name = "tags", required = false)
    var tags: String? = null
    var searchMatches = 0

    @Deprecated("")
    var file: File? = null

    @Deprecated("")
    var remoteUrl: String? = null

    // Two books are equal if their ids match
    override fun equals(other: Any?): Boolean {
      if (other is Book) {
        if (other.id == id) {
          return true
        }
      }
      return false
    }

    // Only use the book's id to generate a hash code
    override fun hashCode(): Int = id.hashCode()
  }
}
