package org.kiwix.kiwixmobile.webserver;

import android.util.Log;
import androidx.annotation.NonNull;
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
  public static boolean isServerStarted;
  private static int port;
  private final JNIKiwixLibrary kiwixLibrary = new JNIKiwixLibrary();
  private final JNIKiwixServer kiwixServer = new JNIKiwixServer(kiwixLibrary);
  private static final String TAG = "WebServerHelper";

  public WebServerHelper() {

  }

  public boolean startServerHelper(@NonNull ArrayList<String> selectedBooksPath) {

    // 1. Get port from settings screen
    // 2. Ask user to change port in settings if port is in use.
    // OR
    // Always use 8080 and when its not available then iterate this number.
    String ip = getIpAddress();
    ip = ip.replaceAll("\n", "");
    if (ip.length() == 0) {
      return false;
    } else if (startAndroidWebServer(selectedBooksPath)) {
      return true;
    }
    return isServerStarted;
  }

  public void stopAndroidWebServer() {
    if (isServerStarted) {
      kiwixServer.stop();
      isServerStarted = false;
    }
  }

  private boolean startAndroidWebServer(ArrayList<String> selectedBooksPath) {
    if (!isServerStarted) {
      port = 8080;
      //Call to start server
      for (String path : selectedBooksPath) {
        try {
          boolean isBookAdded = kiwixLibrary.addBook(path);
          Log.v(TAG, "isBookAdded: " + isBookAdded + path);
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
  private static String getIpAddress() {
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

  @NonNull public static String getCompleteAddress() {
    String address = "http://" + getIpAddress() + ":" + port;
    address = address.replaceAll("\n", "");
    return address;

  }

  public static String getIp() {
    String ip = getIpAddress();
    ip = ip.replaceAll("\n", "");
    if (ip.length() == 0) throw new IllegalStateException();
    return ip;
  }
}
