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

import org.kiwix.kiwixmobile.core.utils.files.Log
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException

object ServerUtils {
  @JvmField var port = 0
  @JvmField var isServerStarted = false
  const val INVALID_IP = "-1" // To remove extra characters from IP for Android Pie
  private const val TAG = "ServerUtils"

  // get Ip address of the device's wireless access point i.e. wifi hotspot OR wifi network
  @Suppress("MagicNumber")
  @JvmStatic fun getIpAddress(): String? {
    var ip = ""
    try {
      for (networkInterface in NetworkInterface.getNetworkInterfaces()) {
        for (inetAddress in networkInterface.inetAddresses)
          ip += formatLocalAddress(inetAddress)
      }
      // To remove extra characters from IP for Android Pie
      ip = formatIpForAndroidPie(ip)
    } catch (socketException: SocketException) {
      Log.e(TAG, "$socketException")
      ip += "Something Wrong! $socketException\n"
    } catch (invalidIpException: IllegalArgumentException) {
      Log.e(TAG, "$invalidIpException")
      ip += "Something Wrong! $invalidIpException\n"
    }
    return ip
  }

  private fun formatLocalAddress(inetAddress: InetAddress): String =
    (inetAddress.hostAddress + "\n").takeIf { inetAddress.isSiteLocalAddress } ?: ""

  @Suppress("MagicNumber")
  fun formatIpForAndroidPie(ip: String): String {
    // regex from OneCricketeer @ https://stackoverflow.com/a/15875500/14053602
    val ipRegex =
      "(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)"
        .toRegex()
    val ipMatch = ipRegex.find(ip, 0) ?: throw IllegalArgumentException()
    return ipMatch.value
  }

  @JvmStatic fun getSocketAddress(): String =
    "http://${getIpAddress()}:$port".replace("\n", "")

  @JvmStatic fun getIp(): String? =
    getIpAddress()?.replace("\n", "")?.takeIf(String::isNotEmpty) ?: INVALID_IP
}
