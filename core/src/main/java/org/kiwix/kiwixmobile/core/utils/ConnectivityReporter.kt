/*
 * Kiwix Android
 * Copyright (c) 2020 Kiwix <android.kiwix.org>
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

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import javax.inject.Inject

class ConnectivityReporter @Inject constructor(
  private val wifiManager: WifiManager,
  private val connectivityManager: ConnectivityManager
) {

  fun checkWifi(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      val capabilities =
        connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
      capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    } else {
      wifiManager.isWifiEnabled && wifiManager.connectionInfo.networkId != -1
    }

  fun checkTethering(): Boolean = try {
    val method: Method = wifiManager.javaClass.getDeclaredMethod("isWifiApEnabled")
    method.isAccessible = true
    method.invoke(wifiManager) as Boolean
  } catch (exception: IllegalArgumentException) {
    exception.printStackTrace()
    false
  } catch (exception: IllegalAccessException) {
    exception.printStackTrace()
    false
  } catch (exception: InvocationTargetException) {
    exception.printStackTrace()
    false
  }
}
