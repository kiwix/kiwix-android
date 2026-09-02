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
package org.kiwix.kiwixmobile.core.utils

import android.content.Context
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.utils.files.Log
import java.util.UUID

object NetworkUtils {
  // Compiled once instead of on every parseURL() call.
  private val UNDERSCORE_REGEX = "_".toRegex()
  private val ALL_REGEX = "all".toRegex()
  private val NOPIC_REGEX = "nopic".toRegex()
  private val NOVID_REGEX = "novid".toRegex()
  private val SIMPLE_REGEX = "simple".toRegex()
  private val EXTRA_SPACES_REGEX = " +".toRegex()

  fun getFileNameFromUrl(url: String?): String {
    var filename = ""
    url?.let { url1 ->
      val index = url1.lastIndexOf('?')
      val slashIndex = url1.lastIndexOf('/')
      filename =
        // Only take the "between last '/' and '?'" branch when that range is actually
        // valid - a URL like ".../a?b/file.zim" has a '/' *after* the '?', which made
        // the start offset exceed the end offset and throw StringIndexOutOfBounds.
        if (index > 1 && slashIndex + 1 <= index) {
          url1.substring(slashIndex + 1, index)
        } else {
          url1.substring(slashIndex + 1)
        }
      if ("" == filename.trim { it <= ' ' }) {
        filename = UUID.randomUUID().toString()
      }
    }
    return filename
  }

  @Suppress("TooGenericExceptionCaught")
  fun parseURL(context: Context, url: String?): String {
    return if (url == null) {
      ""
    } else {
      try {
        var details = url.substring(url.lastIndexOf("/") + 1)
        val beginIndex = details.indexOf("_", details.indexOf("_") + 1) + 1
        val endIndex = details.lastIndexOf("_")
        if (beginIndex < 0 || endIndex > details.length || beginIndex > endIndex) {
          return ""
        }
        details = details.substring(beginIndex, endIndex)
        details = details.replace(UNDERSCORE_REGEX, " ")
        details = details.replace(ALL_REGEX, "")
        details = details.replace(NOPIC_REGEX, context.getString(R.string.zim_no_pic))
        details = details.replace(NOVID_REGEX, context.getString(R.string.zim_no_vid))
        details = details.replace(SIMPLE_REGEX, context.getString(R.string.zim_simple))
        details = details.trim { it <= ' ' }.replace(EXTRA_SPACES_REGEX, " ")
        details
      } catch (e: Exception) {
        Log.d(TAG_KIWIX, "Context invalid url: $url", e)
        ""
      }
    }
  }
}
