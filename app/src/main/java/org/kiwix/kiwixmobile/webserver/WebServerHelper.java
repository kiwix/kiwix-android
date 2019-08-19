package org.kiwix.kiwixmobile.webserver;

import android.util.Log;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import org.kiwix.kiwixlib.JNIKiwixException;
import org.kiwix.kiwixlib.JNIKiwixLibrary;
import org.kiwix.kiwixlib.JNIKiwixServer;
import org.kiwix.kiwixmobile.utils.ServerUtils;

/**
 * WebServerHelper class is used to set up the suitable environment i.e. getting the
 * ip address and port no. before starting the WebServer
 * Created by Adeel Zafar on 18/07/2019.
 */

public class WebServerHelper {
  private final int DEFAULT_PORT = 8080;
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
    String ip = ServerUtils.getIpAddress();
    ip = ip.replaceAll("\n", "");
    if (ip.length() == 0) {
      return false;
    } else if (startAndroidWebServer(selectedBooksPath)) {
      return true;
    }
    return ServerUtils.isServerStarted;
  }

  public void stopAndroidWebServer() {
    if (ServerUtils.isServerStarted) {
      kiwixServer.stop();
      ServerUtils.isServerStarted = false;
    }
  }

  private boolean startAndroidWebServer(ArrayList<String> selectedBooksPath) {
    if (!ServerUtils.isServerStarted) {
      ServerUtils.port = DEFAULT_PORT;
      //Call to start server
      for (String path : selectedBooksPath) {
        try {
          boolean isBookAdded = kiwixLibrary.addBook(path);
          Log.v(TAG, "isBookAdded: " + isBookAdded + path);
        } catch (JNIKiwixException e) {
          Log.v(TAG, "Couldn't add book " + path);
        }
      }
      kiwixServer.setPort(ServerUtils.port);
      ServerUtils.isServerStarted = kiwixServer.start();
      Log.v(TAG, "Server status" + ServerUtils.isServerStarted);
    }
    return ServerUtils.isServerStarted;
  }
}
