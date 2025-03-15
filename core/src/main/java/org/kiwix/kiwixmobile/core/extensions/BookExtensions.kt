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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.downloader.model.Base64String
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity.Book
import org.kiwix.kiwixmobile.core.utils.BookUtils
import org.kiwix.kiwixmobile.core.utils.NetworkUtils

fun Book.calculateSearchMatches(
  filter: String,
  bookUtils: BookUtils
) {
  val searchableText = buildSearchableText(bookUtils)
  searchMatches = filter.split("\\s+")
    .foldRight(
      0,
      { filterWord, acc ->
        if (searchableText.contains(filterWord, true)) {
          acc + 1
        } else {
          acc
        }
      }
    )
}

fun Book.buildSearchableText(bookUtils: BookUtils): String =
  StringBuilder().apply {
    append(title)
    append("|")
    append(description)
    append("|")
    append(NetworkUtils.parseURL(CoreApp.instance, url))
    append("|")
    if (bookUtils.localeMap.containsKey(language)) {
      append(bookUtils.localeMap[language]?.displayLanguage)
      append("|")
    }
  }.toString()

@Composable
fun Book.faviconToPainter(): Painter {
  val base64String = Base64String(favicon)
  val bitmap = remember(base64String) { base64String.toBitmap() }
  return if (bitmap != null) {
    BitmapPainter(bitmap.asImageBitmap())
  } else {
    painterResource(id = R.drawable.default_zim_file_icon)
  }
}
