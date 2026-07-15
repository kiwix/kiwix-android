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

package org.kiwix.kiwixmobile.core.search

sealed class SearchListItem {
  abstract val value: String
  abstract val url: String?

  data class RecentSearchListItem(override val value: String, override val url: String?) :
    SearchListItem()

  data class ZimSearchResultListItem constructor(
    override val value: String,
    override val url: String?,
    /**
     * A short quote of the sentence where the search term was found (full text
     * search only), with the matched words wrapped in `<b>` tags as returned
     * by the Xapian index.
     */
    val snippet: String? = null,
    /**
     * The title of the book this result belongs to. Only set for results of a
     * search across all books, where it is shown so the user knows which ZIM
     * file each result came from.
     */
    val bookTitle: String? = null,
    /**
     * The [org.kiwix.kiwixmobile.core.reader.ZimReaderSource.toDatabase] value
     * of the book this result belongs to. Only set for cross-book search
     * results, so the reader can switch to the right book before opening.
     */
    val zimReaderSourceDatabaseValue: String? = null
  ) : SearchListItem()
}
