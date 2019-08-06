package org.kiwix.kiwixmobile.webserver;

import android.content.Context;
import android.content.DialogInterface;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.snackbar.Snackbar;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import org.kiwix.kiwixmobile.R;

/**
 * WebServerHelper class is used to set up the suitable environment i.e. getting the
 * ip address and port no. before starting the WebServer
 * Created by Adeel Zafar on 18/07/2019.
 */

public class WebServerHelper {
  Context context;
  private TextView textViewIpAccess;
  private EditText editTextPort;
  public static boolean isStarted;
  int port;
  private static WebServer webServer;
  private CoordinatorLayout coordinatorLayout;
  ServerStateListener listener;

  public WebServerHelper(Context context) {
    this.context = context;
  }

  public void startServerHelper() {
    //TO DO:
    //1. Get port from settings screen
    //2. Ask user to change port in settings if port is in use.
    //OR
    //Always use 8080 and when its not available then iterate this number.
    if (!isStarted && startAndroidWebServer()) {
      isStarted = true;
      listener = (ServerStateListener) context;
      listener.serverStarted(getIpAddress() + port);
    }
  }


  public static boolean stopAndroidWebServer() {
    if (isStarted && webServer != null) {
      webServer.stop();
      isStarted = false;
      return true;
    }
    return false;
  }

  boolean startAndroidWebServer() {
    if (!isStarted) {
      port = 8080;
      try {
        if (port == 0) {
          throw new Exception();
        }
        webServer = new WebServer(port);
        webServer.start();
        return true;
      } catch (Exception e) {
        e.printStackTrace();
        Snackbar.make(coordinatorLayout,
            "The PORT " + port + " doesn't work, please change it between 1000 and 9999.",
            Snackbar.LENGTH_LONG).show();
      }
    }
    return false;
  }

  // get Ip address of the device's wireless access point i.e. wifi hotspot OR wifi network
  String getIpAddress() {
    Log.v("DANG", "Inside getIpAdress()");
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

    Log.v("DANG", "Returning : " + "http://" + ip);
    return "http://" + ip;
  }
}
