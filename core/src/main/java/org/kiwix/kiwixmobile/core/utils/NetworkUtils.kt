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
import android.net.ConnectivityManager
import org.kiwix.kiwixmobile.core.utils.files.Log
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.isNetworkAvailable
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.isWifi
import java.util.UUID

object NetworkUtils {
  /**
   * check availability of any network
   *
   * @return true if a network is ready to be used
   */
  fun isNetworkAvailable(context: Context): Boolean {
    val connectivity: ConnectivityManager = context
      .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return connectivity.isNetworkAvailable()
  }

  /**
   * check if network of type WIFI is connected
   *
   * @return true if WIFI is connected
   */
  // TODO method isWiFi should be renamed to isWifiConnected to
  //  express the state which is checked
  //  (postponed to refactoring deprecated android.net.* usage)
  fun isWiFi(context: Context): Boolean {
    val connectivity = context
      .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return connectivity.isWifi()
  }

  fun getFileNameFromUrl(url: String?): String {
    var filename = ""
    url?.let { url1 ->
      val index = url1.lastIndexOf('?')
      filename = if (index > 1) {
        url1.substring(url1.lastIndexOf('/') + 1, index)
      } else {
        url1.substring(url.lastIndexOf('/') + 1)
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
    } else try {
      var details = url.substring(url.lastIndexOf("/") + 1)
      val beginIndex = details.indexOf("_", details.indexOf("_") + 1) + 1
      val endIndex = details.lastIndexOf("_")
      if (beginIndex < 0 || endIndex > details.length || beginIndex > endIndex) {
        return ""
      }
      details = details.substring(beginIndex, endIndex)
      details = details.replace("_".toRegex(), " ")
      details = details.replace("all".toRegex(), "")
      details = details.replace("nopic".toRegex(), context.getString(R.string.zim_no_pic))
      details = details.replace("novid".toRegex(), context.getString(R.string.zim_no_vid))
      details = details.replace("simple".toRegex(), context.getString(R.string.zim_simple))
      details = details.trim { it <= ' ' }.replace(" +".toRegex(), " ")
      details
    } catch (e: Exception) {
      Log.d(TAG_KIWIX, "Context invalid url: $url", e)
      ""
    }
  }
}
