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

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.kiwix.kiwixmobile.core.data.remote.KiwixService.Companion.OPDS_LIBRARY_ENDPOINT
import org.kiwix.kiwixmobile.core.downloader.downloadManager.ZERO
import org.kiwix.kiwixmobile.core.entity.LibkiwixBook
import org.kiwix.kiwixmobile.di.modules.ONLINE_BOOKS_LIBRARY
import org.kiwix.kiwixmobile.di.modules.ONLINE_BOOKS_MANAGER
import org.kiwix.libkiwix.Library
import org.kiwix.libkiwix.Manager
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import javax.inject.Inject
import javax.inject.Named

@Suppress("UnusedPrivateProperty")
class OnlineLibraryManager @Inject constructor(
  @Named(ONLINE_BOOKS_LIBRARY) private val library: Library,
  @Named(ONLINE_BOOKS_MANAGER) private val manager: Manager,
) {
  var totalResult = 0
  suspend fun parseOPDSStreamAndGetBooks(
    content: String?,
    urlHost: String
  ): ArrayList<LibkiwixBook>? =
    runCatching {
      if (content == null) return null
      totalResult = extractTotalResults(content)
      val onlineBooksList = arrayListOf<LibkiwixBook>()
      val tempLibrary = Library()
      val tempManager = Manager(tempLibrary)
      tempManager.readOpds(content, urlHost)
      tempLibrary.booksIds.forEach { bookId ->
        val book = tempLibrary.getBookById(bookId)
        onlineBooksList.add(LibkiwixBook(book))
      }
      onlineBooksList
    }.onFailure {
      it.printStackTrace()
    }.getOrNull()

  /**
   * Builds the URL for fetching the OPDS library entries with pagination and optional filters.
   *
   * @param baseUrl The base URL of the Kiwix library server (e.g., "https://opds.library.kiwix.org/").
   * @param start The index from which to start fetching entries (default is 0).
   * @param count The number of entries to fetch per page (default is 50).
   * @param query Optional search query for filtering results by text.
   * @param lang Optional language code filter (e.g., "en", "ita").
   * @param category Optional category filter (e.g., "wikipedia", "books").
   * @return A full URL string with query parameters applied.
   *
   * Example:
   * buildLibraryUrl("https://library.kiwix.org", start = 100, count = 50, lang = "en")
   * returns: "https://library.kiwix.org/v2/entries?start=100&count=50&lang=en"
   */
  fun buildLibraryUrl(
    baseUrl: String,
    start: Int,
    count: Int,
    query: String? = null,
    lang: String? = null,
    category: String? = null
  ): String {
    val params = mutableListOf("start=$start", "count=$count")
    query?.takeIf { it.isNotBlank() }?.let { params += "q=$it" }
    lang?.takeIf { it.isNotBlank() }?.let { params += "lang=$it" }
    category?.takeIf { it.isNotBlank() }?.let { params += "category=$it" }

    return "$baseUrl/$OPDS_LIBRARY_ENDPOINT?${params.joinToString("&")}"
  }

  /**
   * Calculates the total number of pages needed for pagination.
   *
   * @param totalResults Total number of items available (e.g., 3408).
   * @param pageSize Number of items per page (e.g., 50).
   * @return The total number of pages required to show all items.
   *
   * Example:
   * calculateTotalPages(3408, 50) returns 69
   */
  fun calculateTotalPages(totalResults: Int, pageSize: Int): Int =
    (totalResults + pageSize - 1) / pageSize

  /**
   * Calculates the starting index (offset) for a given page number.
   *
   * @param pageIndex The page number starting from 0 (e.g., pageIndex = 2 means page 3).
   * @param pageSize Number of items per page (e.g., 50).
   * @return The offset index to be used in a paginated request (e.g., start=100).
   *
   * Example:
   * getStartOffset(2, 50) returns 100
   */
  fun getStartOffset(pageIndex: Int, pageSize: Int): Int = pageIndex * pageSize

  private suspend fun extractTotalResults(
    xml: String,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
  ): Int = withContext(dispatcher) {
    val factory = XmlPullParserFactory.newInstance()
    val parser = factory.newPullParser()
    parser.setInput(StringReader(xml))

    var eventType = parser.eventType
    while (eventType != XmlPullParser.END_DOCUMENT) {
      if (eventType == XmlPullParser.START_TAG && parser.name == "totalResults") {
        return@withContext parser.nextText().toIntOrNull() ?: ZERO
      }
      eventType = parser.next()
    }
    return@withContext ZERO
  }
}
