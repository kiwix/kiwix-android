package org.kiwix.kiwixmobile.webserver;

import android.content.Context;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * WebServerHelper class is used to set up the suitable environment i.e. getting the
 * ip address and port no. before starting the WebServer
 * Created by Adeel Zafar on 18/07/2019.
 */

public class WebServerHelper {
  Context context;
  public static boolean isServerStarted;
  static int port;

  public WebServerHelper(Context context) {
    this.context = context;
  }

  public void startServerHelper(ServerStateListener stateListener) {
    //TO DO:
    //1. Get port from settings screen
    //2. Ask user to change port in settings if port is in use.
    //OR
    //Always use 8080 and when its not available then iterate this number.
    if (!isServerStarted && startAndroidWebServer()) {
      isServerStarted = true;
      stateListener.serverStarted(getIpAddress() + ":" + port);
    }
  }

  public static boolean stopAndroidWebServer(ServerStateListener stateListener) {
    if (isServerStarted) {
      isServerStarted = false;
      stateListener.serverStopped();
      return true;
    }
    return false;
  }

  boolean startAndroidWebServer() {
    if (!isServerStarted) {
      port = 8080;
      //Call to start server
      return true;
    }
    return false;
  }

  // get Ip address of the device's wireless access point i.e. wifi hotspot OR wifi network
  static String getIpAddress() {
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
        for (int i = 15, j = 12; i < 18; i++, j++) {
          if ((ip.charAt(i) == '.')) {
            ip = ip.substring(0, j + 1);
            break;
          }
        }
      }
    } catch (SocketException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      ip += "Something Wrong! " + e.toString() + "\n";
    }

    return "http://" + ip;
  }

  public static String getAddress() {
    return getIpAddress() + ":" + port;
  }
}
