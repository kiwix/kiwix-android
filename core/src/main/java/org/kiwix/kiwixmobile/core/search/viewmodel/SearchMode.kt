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

/**
 * Defines where a search term should be looked up inside the opened ZIM file.
 *
 * [TITLE] searches only in the article titles (the classic suggestion search),
 * whereas [PAGE_CONTENT] runs a full-text search over the content of the pages
 * using the Xapian full-text index of the ZIM file.
 */
enum class SearchMode {
  TITLE,
  PAGE_CONTENT
}
