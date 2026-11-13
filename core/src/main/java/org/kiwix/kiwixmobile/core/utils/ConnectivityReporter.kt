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
import android.net.wifi.WifiManager
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.isWifi
import java.lang.reflect.Method
import java.net.NetworkInterface
import javax.inject.Inject

class ConnectivityReporter @Inject constructor(
  private val wifiManager: WifiManager,
  private val connectivityManager: ConnectivityManager
) {
  fun checkWifi(): Boolean =
    connectivityManager.isWifi()

  // Reflection still works on devices that don't enforce the non-SDK restriction; fall back otherwise.
  fun checkTethering(): Boolean = checkTetheringViaReflection() ?: checkTetheringViaApInterface()

  private fun checkTetheringViaReflection(): Boolean? = runCatching {
    val method: Method = wifiManager.javaClass.getDeclaredMethod("isWifiApEnabled")
    method.isAccessible = true
    method.invoke(wifiManager) as Boolean
  }.onFailure {
    it.printStackTrace()
  }.getOrNull()

  private fun checkTetheringViaApInterface(): Boolean = runCatching {
    NetworkInterface.getNetworkInterfaces()?.asSequence().orEmpty().any { networkInterface ->
      hotspotInterfaceNamePattern.matches(networkInterface.name) && networkInterface.isUp
    }
  }.onFailure {
    it.printStackTrace()
  }.getOrDefault(false)

  companion object {
    // Soft AP interface naming varies by OEM/chipset: ap0 (AOSP/Pixel), swlan0 (Samsung),
    // softap0, and wlan1+ on dual-radio chips where wlan0 stays the station interface.
    private val hotspotInterfaceNamePattern =
      Regex("^(ap\\d*|softap\\d*|swlan\\d*|wlan[1-9]\\d*)$", RegexOption.IGNORE_CASE)
  }
}
