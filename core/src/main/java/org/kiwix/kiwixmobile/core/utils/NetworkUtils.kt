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
import android.net.NetworkInfo
import android.os.Build
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.replace
import java.util.UUID

object NetworkUtils {
  /**
   * check availability of any network
   *
   * @return true if a network is ready to be used
   */
  @JvmStatic fun isNetworkAvailable(context: Context): Boolean {
    val connectivity = context
      .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkInfos = connectivity.allNetworkInfo
    if (networkInfos != null) {
      for (networkInfo in networkInfos) {
        if (isNetworkConnectionOK(networkInfo)) {
          return true
        }
      }
    }
    return false
  }

  fun isNetworkConnectionOK(networkInfo: NetworkInfo): Boolean =
    networkInfo.state == NetworkInfo.State.CONNECTED

  /**
   * check if network of type WIFI is connected
   *
   * @return true if WIFI is connected
   */
  // TODO method isWiFi should be renamed to isWifiConnected to express the state which is checked
  //  (postponed to refactoring deprecated android.net.* usage)
  fun isWiFi(context: Context): Boolean {
    val connectivity = context
      .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      val networkInfo = connectivity.activeNetworkInfo ?: return false
      networkInfo.type == ConnectivityManager.TYPE_WIFI && networkInfo.isConnected
    } else {
      val wifi = connectivity.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
      wifi != null && wifi.isConnected
    }
  }

  fun getFileNameFromUrl(url: String): String {
    val index = url.lastIndexOf('?')
    var filename = if (index > 1) {
      url.substring(url.lastIndexOf('/') + 1, index)
    } else {
      url.substring(url.lastIndexOf('/') + 1)
    }
    if ("" == filename.trim { it <= ' ' }) {
      filename = UUID.randomUUID().toString()
    }
    return filename
  }

  fun parseURL(context: Context, url: String?): String {
    return if (url == null) {
      ""
    } else {
      val details = url.substring(url.lastIndexOf("/") + 1)
      val beginIndex = details.indexOf("_", details.indexOf("_") + 1) + 1
      val endIndex = details.lastIndexOf("_")
      return if (beginIndex < 0 || endIndex > details.length || beginIndex > endIndex) {
        ""
      } else {
        details.substring(beginIndex, endIndex).replace(
          "_" to " ",
          "all" to "",
          "nopic" to context.getString(R.string.zim_no_pic),
          "novid" to context.getString(R.string.zim_no_vid),
          "simple" to context.getString(R.string.zim_simple),
          " +" to " "
        ).trim { it <= ' ' }
      }
    }
  }
}
