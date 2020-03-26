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

import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException

object ServerUtils {
  @JvmField var port = 0
  @JvmField var isServerStarted = false
  const val INVALID_IP = "-1" // To remove extra characters from IP for Android Pie

  // get Ip address of the device's wireless access point i.e. wifi hotspot OR wifi network
  @Suppress("MagicNumber")
  @JvmStatic fun getIpAddress(): String? {
    var ip = ""
    try {
      val enumNetworkInterfaces = NetworkInterface.getNetworkInterfaces()
      while (enumNetworkInterfaces.hasMoreElements()) {
        val networkInterface = enumNetworkInterfaces.nextElement()
        val enumInetAddress = networkInterface.inetAddresses
        while (enumInetAddress.hasMoreElements()) {
          ip += formatLocalAddress(enumInetAddress.nextElement())
        }
      }
      // To remove extra characters from IP for Android Pie
      if (ip.length > 14) {
        ip = formatIpForAndroidPie(ip)
      }
    } catch (e: SocketException) {
      e.printStackTrace()
      ip += "Something Wrong! $e\n"
    }
    return ip
  }

  private fun formatLocalAddress(inetAddress: InetAddress): String {
    var result = ""
    if (inetAddress.isSiteLocalAddress) {
      result += inetAddress.hostAddress + "\n"
    }
    return result
  }

  @Suppress("MagicNumber")
  private fun formatIpForAndroidPie(ip: String): String {
    var result: String = ip
    for (i in 15..17) {
      if (ip[i] == '.') {
        result = ip.substring(0, i - 2)
        break
      }
    }
    return result
  }

  @JvmStatic fun getSocketAddress(): String =
    "http://${getIpAddress()}:$port".replace("\n", "")

  @JvmStatic fun getIp(): String? =
    getIpAddress()?.replace("\n", "")?.takeIf(String::isNotEmpty) ?: INVALID_IP
}
