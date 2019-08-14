package org.kiwix.kiwixmobile.webserver;

import android.content.Context;
import android.util.Log;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import org.kiwix.kiwixlib.JNIKiwixException;
import org.kiwix.kiwixlib.JNIKiwixLibrary;
import org.kiwix.kiwixlib.JNIKiwixServer;

/**
 * WebServerHelper class is used to set up the suitable environment i.e. getting the
 * ip address and port no. before starting the WebServer
 * Created by Adeel Zafar on 18/07/2019.
 */

public class WebServerHelper {
  Context context;
  public static boolean isServerStarted;
  static int port;
  JNIKiwixLibrary kiwixLibrary = new JNIKiwixLibrary();
  JNIKiwixServer kiwixServer = new JNIKiwixServer(kiwixLibrary);
  private static final String TAG = "WebServerHelper";

  public WebServerHelper(Context context) {
    this.context = context;
  }

  public boolean startServerHelper(ServerStateListener stateListener,
      ArrayList<String> selectedBooksPath) {

    // 1. Get port from settings screen
    // 2. Ask user to change port in settings if port is in use.
    // OR
    // Always use 8080 and when its not available then iterate this number.
    String ip = getIpAddress();
    ip = ip.replaceAll("\n", "");
    if (ip.length() == 0) {
      stateListener.serverFailed();
    } else if (!isServerStarted && startAndroidWebServer(selectedBooksPath)) {
      stateListener.serverStarted("http://" + ip + ":" + port);
    }
    return isServerStarted;
  }

  public boolean stopAndroidWebServer(ServerStateListener stateListener) {
    if (isServerStarted) {
      kiwixServer.stop();
      isServerStarted = false;
      stateListener.serverStopped();
      return true;
    }
    return false;
  }

  boolean startAndroidWebServer(ArrayList<String> selectedBooksPath) {
    if (!isServerStarted) {
      port = 8080;
      //Call to start server
      for (String path : selectedBooksPath) {
        try {
          boolean isBookAdded = kiwixLibrary.addBook(path);
          Log.v(TAG, "Book added:" + path);
        } catch (JNIKiwixException e) {
          Log.v(TAG, "Couldn't add book " + path);
        }
      }
      kiwixServer.setPort(port);
      isServerStarted = kiwixServer.start();
      Log.v(TAG, "Server status" + isServerStarted);
    }
    return isServerStarted;
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
      e.printStackTrace();
      ip += "Something Wrong! " + e.toString() + "\n";
    }

    return ip;
  }

  public static String getAddress() {
    return "http://" + getIpAddress() + ":" + port;
  }
}
