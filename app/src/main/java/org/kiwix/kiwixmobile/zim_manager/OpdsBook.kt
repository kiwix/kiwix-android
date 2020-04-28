/*
 * Kiwix Android
 * Copyright (c) 2020 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.zim_manager

import org.kiwix.kiwixmobile.JniBook

data class OpdsBook(
  val id: String,
  val path: String,
  val isPathValid: Boolean,
  val title: String,
  val description: String,
  val language: String,
  val creator: String,
  val publisher: String,
  val date: String,
  val url: String,
  val name: String,
  val flavour: String,
  val tags: String,
  val articleCount: Long,
  val mediaCount: Long,
  val size: Long,
  val favicon: String,
  val faviconUrl: String,
  val faviconMimeType: String
) {

  constructor(book: JniBook) : this(
    book.id,
    book.path,
    book.isPathValid,
    book.title,
    book.description,
    book.language,
    book.creator,
    book.publisher,
    book.date,
    book.url,
    book.name,
    book.flavour,
    book.tags,
    book.articleCount,
    book.mediaCount,
    book.size,
    book.favicon,
    book.faviconUrl,
    book.faviconMimeType
  )
}
