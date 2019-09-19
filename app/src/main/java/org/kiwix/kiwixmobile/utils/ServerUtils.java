package org.kiwix.kiwixmobile.utils;

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
