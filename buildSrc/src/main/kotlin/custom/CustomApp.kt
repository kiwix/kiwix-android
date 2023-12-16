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

import org.json.simple.JSONObject
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val dateFormat = "YYYY-MM"

data class CustomApp(
  val name: String,
  val url: String,
  val enforcedLanguage: String,
  val displayName: String,
  val versionName: String,
  val disableSideBar: Boolean = false,
  val disableTabs: Boolean = false,
  val disableReadAloud: Boolean = false,
  val disableTitle: Boolean = false,
  val disableExternalLinks: Boolean = false,
  val aboutAppUrl: String = "",
  val supportUrl: String = ""
) {
  constructor(name: String, parsedJson: JSONObject) : this(
    name,
    parsedJson.getAndCast("zim_url"),
    parsedJson.getAndCast("enforced_lang"),
    parsedJson.getAndCast("app_name"),
    readVersionOrInfer(parsedJson),
    parsedJson.getAndCast("disable_sidebar") ?: false,
    parsedJson.getAndCast("disable_tabs") ?: false,
    parsedJson.getAndCast("disable_read_aloud") ?: false,
    parsedJson.getAndCast("disable_title") ?: false,
    parsedJson.getAndCast("disable_external_links") ?: false,
    parsedJson.getAndCast("about_app_url") ?: "",
    parsedJson.getAndCast("support_url") ?: ""
  )

  val versionCode: Int = formatCurrentDate("YYDDD0").toInt()

  companion object {
    private fun readVersionOrInfer(parsedJson: JSONObject) =
      parsedJson.getAndCast("version_name")
        ?: versionNameFromUrl(parsedJson.getAndCast("zim_url"))
        ?: formatCurrentDate()
  }
}

private fun versionNameFromUrl(url: String) =
  url.substringAfterLast("_")
    .substringBeforeLast(".")
    .takeIf {
      try {
        SimpleDateFormat(dateFormat, Locale.ROOT).parse(it) != null
      } catch (parseException: ParseException) {
        false
      }
    }

private fun formatCurrentDate(pattern: String = dateFormat) =
  Date().let(SimpleDateFormat(pattern, Locale.ROOT)::format)
