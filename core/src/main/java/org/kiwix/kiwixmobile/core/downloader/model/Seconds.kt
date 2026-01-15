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

package org.kiwix.kiwixmobile.core.downloader.model

import androidx.core.content.ContextCompat
import org.kiwix.kiwixmobile.core.CoreApp.Companion.instance
import org.kiwix.kiwixmobile.core.R
import java.util.Locale
import kotlin.math.roundToLong

@JvmInline
value class Seconds(val seconds: Long) {
  fun toHumanReadableTime(): String {
    val minutes = 60.0
    val hours = 60 * minutes
    val days = 24 * hours

    // Whenever the language changes, it returns the context
    // for the selected language. Since per-app language settings do not update the context
    // outside of AppCompatActivity, we need to retrieve the correct context manually.
    // See: https://issuetracker.google.com/issues/243457462?hl=pl
    val context = ContextCompat.getContextForLanguage(instance)
    return when {
      (seconds / days).roundToLong() > 0 ->
        String.format(
          Locale.getDefault(),
          "%d %s %s",
          (seconds / days).roundToLong(),
          context.getString(R.string.time_day),
          context.getString(R.string.time_left)
        )

      (seconds / hours).roundToLong() > 0 ->
        String.format(
          Locale.getDefault(),
          "%d %s %s",
          (seconds / hours).roundToLong(),
          context.getString(R.string.time_hour),
          context.getString(R.string.time_left)
        )

      (seconds / minutes).roundToLong() > 0 ->
        String.format(
          Locale.getDefault(),
          "%d %s %s",
          (seconds / minutes).roundToLong(),
          context.getString(R.string.time_minute),
          context.getString(R.string.time_left)
        )

      else ->
        String.format(
          Locale.getDefault(),
          "%d %s %s",
          seconds,
          context.getString(R.string.time_second),
          context.getString(R.string.time_left)
        )
    }
  }
}
