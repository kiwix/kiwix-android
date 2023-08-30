/*
 * Kiwix Android
 * Copyright (c) 2023 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.page.bookmark.adapter

import org.kiwix.kiwixmobile.core.page.adapter.Page
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.libkiwix.Bookmark

data class LibkiwixBookarkItem(
  override val id: Long = 0L,
  override val zimFilePath: String?,
  override val zimId: String,
  val zimName: String,
  val bookMarkUrl: String,
  override val url: String = bookMarkUrl,
  override val title: String,
  override var isSelected: Boolean = false,
  override val favicon: String?
) : Page {
  constructor(libkiwixBookmark: Bookmark) : this(
    zimId = libkiwixBookmark.bookId,
    libkiwixBookmark.url
  )

  constructor(
    title: String,
    url: String,
    zimFileReader: ZimFileReader
  ) : this(
    zimId = zimFileReader.id,
    zimName = zimFileReader.name,
    zimFilePath = zimFileReader.zimFile.canonicalPath,
    bookmarkUrl = url,
    title = title,
    favicon = zimFileReader.favicon
  )
}
