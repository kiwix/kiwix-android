/*
 * Kiwix Android
 * Copyright (c) 2026 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.search.viewmodel

import org.kiwix.libzim.Search
import org.kiwix.libzim.SuggestionSearch

/**
 * The result of a search inside a ZIM file, independent of whether the search
 * ran over the article titles or over the full page content.
 */
sealed class ZimSearchResultSet {
  /** Results of a title (suggestion) search. See [SearchMode.TITLE]. */
  data class Title(val suggestionSearch: SuggestionSearch) : ZimSearchResultSet()

  /** Results of a full-text search over the page content. See [SearchMode.PAGE_CONTENT]. */
  data class PageContent(val search: Search) : ZimSearchResultSet()
}
