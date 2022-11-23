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

import java.io.File
import java.io.Serializable
import java.util.LinkedList
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement

@XmlRootElement(name = "library")
class LibraryNetworkEntity {
  @field:XmlElement(name = "book")
  var book: LinkedList<Book>? = null

  @field:XmlAttribute(name = "version")
  var version: String? = null

  @XmlRootElement(name = "book")
  class Book : Serializable {
    @field:XmlAttribute(name = "id")
    var id: String = ""

    @field:XmlAttribute(name = "title")
    var title: String = ""

    @field:XmlAttribute(name = "description")
    var description: String? = null

    @field:XmlAttribute(name = "language")
    var language: String = ""

    @field:XmlAttribute(name = "creator")
    var creator: String = ""

    @field:XmlAttribute(name = "publisher")
    var publisher: String = ""

    @field:XmlAttribute(name = "favicon")
    var favicon: String = ""

    @field:XmlAttribute(name = "faviconMimeType")
    var faviconMimeType: String? = null

    @field:XmlAttribute(name = "date")
    var date: String = ""

    @field:XmlAttribute(name = "url")
    var url: String? = null

    @field:XmlAttribute(name = "articleCount")
    var articleCount: String? = null

    @field:XmlAttribute(name = "mediaCount")
    var mediaCount: String? = null

    @field:XmlAttribute(name = "size")
    var size: String = ""

    @field:XmlAttribute(name = "name")
    var bookName: String? = null

    @field:XmlAttribute(name = "tags")
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
