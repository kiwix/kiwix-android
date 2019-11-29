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

package custom

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CustomApp(
  val name: String,
  val url: String,
  val enforcedLanguage: String,
  val displayName: String,
  val versionName: String = parseVersionNameFromUrlOrUsePattern(url, "YYYY-MM")
) {
  val versionCode: Int = formatDate("YYDDD0").toInt()
}

private fun parseVersionNameFromUrlOrUsePattern(url: String, pattern: String) =
  url.substringAfterLast("_")
    .substringBeforeLast(".")
    .takeIf {
      try {
        SimpleDateFormat(pattern, Locale.ROOT).parse(it) != null
      } catch (parseException: ParseException) {
        false
      }
    }
    ?: formatDate(pattern)

private fun formatDate(pattern: String) =
  Date().let(SimpleDateFormat(pattern, Locale.ROOT)::format)
