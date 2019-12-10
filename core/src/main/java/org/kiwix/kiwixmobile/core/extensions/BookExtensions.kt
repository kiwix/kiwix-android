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

package org.kiwix.kiwixmobile.core.extensions

import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity.Book
import org.kiwix.kiwixmobile.core.utils.BookUtils
import org.kiwix.kiwixmobile.core.utils.NetworkUtils

fun Book.calculateSearchMatches(
  filter: String,
  bookUtils: BookUtils
) {
  val searchableText = buildSearchableText(bookUtils)
  searchMatches = filter.split("\\s+")
    .foldRight(0,
      { filterWord, acc ->
        if (searchableText.contains(filterWord, true)) acc + 1
        else acc
      })
}

fun Book.buildSearchableText(bookUtils: BookUtils): String =
  StringBuilder().apply {
    append(title)
    append("|")
    append(description)
    append("|")
    append(NetworkUtils.parseURL(CoreApp.getInstance(), url))
    append("|")
    if (bookUtils.localeMap.containsKey(language)) {
      append(bookUtils.localeMap[language]!!.displayLanguage)
      append("|")
    }
  }.toString()
