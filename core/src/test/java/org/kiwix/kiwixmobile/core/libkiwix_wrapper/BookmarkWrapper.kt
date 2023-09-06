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

package org.kiwix.kiwixmobile.core.libkiwix_wrapper

import org.kiwix.libkiwix.Bookmark

class BookmarkWrapper : Bookmark() {
  override fun getBookId(): String = super.getBookId()
  override fun getDate(): String = super.getDate()
  override fun getBookTitle(): String = super.getBookTitle()
  override fun getLanguage(): String = super.getLanguage()
  override fun getTitle(): String = super.getTitle()
  override fun getUrl(): String = super.getUrl()

  override fun setBookId(bookId: String?) {
    super.setBookId(bookId)
  }

  override fun setBookTitle(bookTitle: String?) {
    super.setBookTitle(bookTitle)
  }

  override fun setDate(Date: String?) {
    super.setDate(Date)
  }

  override fun setLanguage(language: String?) {
    super.setLanguage(language)
  }

  override fun setUrl(url: String?) {
    super.setUrl(url)
  }

  override fun setTitle(title: String?) {
    super.setTitle(title)
  }
}
