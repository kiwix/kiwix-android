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

package org.kiwix.kiwixmobile.core.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class ServerUtils {
  public static int port;
  public static boolean isServerStarted;
  public static final String INVALID_IP = "-1";

  // get Ip address of the device's wireless access point i.e. wifi hotspot OR wifi network
  @Nullable public static String getIpAddress() {
    String ip = "";
    try {
      Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
        .getNetworkInterfaces();
      while (enumNetworkInterfaces.hasMoreElements()) {
        NetworkInterface networkInterface = enumNetworkInterfaces
          .nextElement();
        Enumeration<InetAddress> enumInetAddress = networkInterface
          .getInetAddresses();
        while (enumInetAddress.hasMoreElements()) {
          InetAddress inetAddress = enumInetAddress.nextElement();

          if (inetAddress.isSiteLocalAddress()) {
            ip += inetAddress.getHostAddress() + "\n";
          }
        }
      }
      //To remove extra characters from IP for Android Pie
      if (ip.length() > 14) {
        for (int i = 15; i < 18; i++) {
          if ((ip.charAt(i) == '.')) {
            ip = ip.substring(0, i - 2);
            break;
          }
        }
      }
    } catch (SocketException e) {
      e.printStackTrace();
      ip += "Something Wrong! " + e.toString() + "\n";
    }
    return ip;
  }

  @NonNull public static String getSocketAddress() {
    String address = "http://" + getIpAddress() + ":" + port;
    address = address.replaceAll("\n", "");
    return address;
  }

  @Nullable public static String getIp() {
    String ip = getIpAddress();
    ip = ip.replaceAll("\n", "");
    return ip.length() == 0 ? INVALID_IP : ip;
  }
}
