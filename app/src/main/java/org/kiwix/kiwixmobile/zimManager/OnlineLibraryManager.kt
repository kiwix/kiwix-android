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

package org.kiwix.kiwixmobile.zimManager

import org.kiwix.kiwixmobile.core.entity.LibkiwixBook
import org.kiwix.kiwixmobile.di.modules.ONLINE_BOOKS_LIBRARY
import org.kiwix.kiwixmobile.di.modules.ONLINE_BOOKS_MANAGER
import org.kiwix.libkiwix.Library
import org.kiwix.libkiwix.Manager
import javax.inject.Inject
import javax.inject.Named

class OnlineLibraryManager @Inject constructor(
  @Named(ONLINE_BOOKS_LIBRARY) private val library: Library,
  @Named(ONLINE_BOOKS_MANAGER) private val manager: Manager,
) {
  suspend fun parseOPDSStream(content: String?, urlHost: String): Boolean =
    runCatching {
      manager.readOpds(content, urlHost)
    }.onFailure {
      it.printStackTrace()
    }.isSuccess

  suspend fun getOnlineBooks(): List<LibkiwixBook> {
    val onlineBooksList = arrayListOf<LibkiwixBook>()
    runCatching {
      library.booksIds.forEach { bookId ->
        val book = library.getBookById(bookId)
        onlineBooksList.add(LibkiwixBook(book))
      }
    }.onFailure { it.printStackTrace() }
    return onlineBooksList
  }

  suspend fun getOnlineBooksLanguage(): List<String> {
    return runCatching {
      library.booksLanguages.distinct()
    }.onFailure {
      it.printStackTrace()
    }.getOrDefault(emptyList())
  }
}
