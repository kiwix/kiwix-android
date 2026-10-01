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

package org.kiwix.kiwixmobile.core.main.reader.helper

import android.content.Context
import androidx.annotation.VisibleForTesting
import dagger.hilt.android.qualifiers.ApplicationContext
import org.kiwix.kiwixmobile.core.main.MainRepositoryActions
import org.kiwix.kiwixmobile.core.page.history.models.HistoryListItem
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.utils.LanguageUtils.Companion.getCurrentLocale
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

class ReaderHistoryManager @Inject constructor(
  @param:ApplicationContext private val context: Context,
  private val mainRepositoryActions: MainRepositoryActions
) {
  // SimpleDateFormat is not thread-safe, so these are only accessed while holding the lock.
  private val dateFormatLock = Any()
  private var dateFormatLocale: Locale? = null
  private var dateFormat: SimpleDateFormat? = null

  suspend fun saveHistory(
    url: String?,
    title: String?,
    zimFileReader: ZimFileReader?
  ) {
    if (url == null || title == null || zimFileReader == null) {
      return
    }

    val timestamp = System.currentTimeMillis()
    val history = HistoryListItem.HistoryItem(
      url = url,
      title = title,
      dateString = formatDate(timestamp),
      timeStamp = timestamp,
      zimFileReader = zimFileReader
    )

    mainRepositoryActions.saveHistory(history)
  }

  @VisibleForTesting
  internal fun formatDate(timestamp: Long): String {
    val locale = getCurrentLocale(context)
    synchronized(dateFormatLock) {
      val format = dateFormat?.takeIf { dateFormatLocale == locale }
        ?: SimpleDateFormat("d MMM yyyy", locale).also {
          dateFormat = it
          dateFormatLocale = locale
        }
      // A cached instance keeps the time zone it was created with, so pick up any device change.
      format.timeZone = TimeZone.getDefault()
      return format.format(Date(timestamp))
    }
  }
}
