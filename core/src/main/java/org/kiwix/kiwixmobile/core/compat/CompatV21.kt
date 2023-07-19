/*
 * Kiwix Android
 * Copyright (c) 2023 Kiwix <android.kiwix.org>
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
@file:Suppress("DEPRECATION")

package org.kiwix.kiwixmobile.core.compat

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.ConnectivityManager
import android.net.ConnectivityManager.TYPE_WIFI
import android.net.NetworkInfo.State.CONNECTED

open class CompatV21 : Compat {
  override fun queryIntentActivities(
    packageManager: PackageManager,
    intent: Intent,
    flags: ResolveInfoFlagsCompat
  ): List<ResolveInfo> = packageManager.queryIntentActivities(intent, flags.value.toInt())

  override fun getPackageInformation(
    packageName: String,
    packageManager: PackageManager,
    flag: Int
  ): PackageInfo = packageManager.getPackageInfo(packageName, 0)

  /**
   * Checks if the device has a network connection with internet access.
   *
   * @param connectivity The ConnectivityManager instance.
   * @return True if a network connection with internet access is available, false otherwise.
   */
  override fun isNetworkAvailable(connectivity: ConnectivityManager): Boolean =
    connectivity.allNetworkInfo.any { it.state == CONNECTED }

  /**
   * Checks if the device is connected to a Wi-Fi network.
   *
   * @param connectivity The ConnectivityManager instance.
   * @return True if connected to a Wi-Fi network, false otherwise.
   */
  override fun isWifi(connectivity: ConnectivityManager): Boolean =
    connectivity.getNetworkInfo(TYPE_WIFI)?.isConnected == true
}
