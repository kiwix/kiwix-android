/*
 * Kiwix Android
 * Copyright (c) 2025 Kiwix <android.kiwix.org>
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

import org.kiwix.kiwixmobile.core.extensions.getFavicon
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.libkiwix.Book
import java.io.File

/**
 * Wrapper around libkiwix's [Book] that allows setting custom values (e.g. from DB or UI),
 * while still falling back to the original [nativeBook]'s properties when not provided.
 */
@Suppress("ConstructorParameterNaming")
data class LibkiwixBook(
  private val nativeBook: Book? = null,
  private var _id: String = "",
  private var _title: String = "",
  private var _description: String? = null,
  private var _language: String = "",
  private var _creator: String = "",
  private var _publisher: String = "",
  private var _date: String = "",
  private var _url: String? = null,
  private var _articleCount: String? = null,
  private var _mediaCount: String? = null,
  private var _size: String = "",
  private var _bookName: String? = null,
  private var _favicon: String = "",
  private var _tags: String? = null,
  private var _path: String? = "",
  var searchMatches: Int = 0,
  var file: File? = null
) {
  var id: String
    get() = _id.ifEmpty { nativeBook?.id.orEmpty() }
    set(id) {
      _id = id
    }

  var title: String
    get() = _title.ifEmpty { nativeBook?.title.orEmpty() }
    set(title) {
      _title = title
    }

  var description: String?
    get() = _description ?: nativeBook?.description
    set(description) {
      _description = description
    }

  var language: String
    get() = _language.ifEmpty { nativeBook?.language.orEmpty() }
    set(language) {
      _language = language
    }

  var creator: String
    get() = _creator.ifEmpty { nativeBook?.creator.orEmpty() }
    set(creator) {
      _creator = creator
    }

  var publisher: String
    get() = _publisher.ifEmpty { nativeBook?.publisher.orEmpty() }
    set(publisher) {
      _publisher = publisher
    }

  var date: String
    get() = _date.ifEmpty { nativeBook?.date.orEmpty() }
    set(date) {
      _date = date
    }

  var url: String?
    get() = _url ?: nativeBook?.url
    set(url) {
      _url = url
    }

  var articleCount: String?
    get() = _articleCount ?: nativeBook?.articleCount?.toString()
    set(articleCount) {
      _articleCount = articleCount
    }

  var mediaCount: String?
    get() = _mediaCount ?: nativeBook?.mediaCount?.toString()
    set(mediaCount) {
      _mediaCount = mediaCount
    }

  var size: String
    get() = _size.ifEmpty { nativeBook?.size?.toString().orEmpty() }
    set(size) {
      _size = size
    }

  var bookName: String?
    get() = _bookName ?: nativeBook?.name
    set(bookName) {
      _bookName = bookName
    }

  var favicon: String
    get() = _favicon.ifEmpty { nativeBook?.getFavicon().orEmpty() }
    set(favicon) {
      _favicon = favicon
    }

  var tags: String?
    get() = _tags ?: nativeBook?.tags
    set(tags) {
      _tags = tags
    }

  var path: String?
    get() = _path ?: nativeBook?.path
    set(path) {
      _path = path
    }

  val zimReaderSource: ZimReaderSource
    get() = ZimReaderSource(File(path.orEmpty()))

  // Two books are equal if their ids match
  override fun equals(other: Any?): Boolean {
    if (other is LibkiwixBook) {
      if (other.id == id) {
        return true
      }
    }
    return false
  }

  // Only use the book's id to generate a hash code
  override fun hashCode(): Int = id.hashCode()
}
