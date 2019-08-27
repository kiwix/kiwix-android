package org.kiwix.kiwixmobile.webserver;

import android.util.Log;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import javax.inject.Inject;
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
  private static final String TAG = "WebServerHelper";
  private JNIKiwixLibrary kiwixLibrary;
  private JNIKiwixServer kiwixServer;

  @Inject public WebServerHelper(@NonNull JNIKiwixLibrary kiwixLibrary,
      @NonNull JNIKiwixServer kiwixServer) {
    this.kiwixLibrary = kiwixLibrary;
    this.kiwixServer = kiwixServer;
  }

  public boolean startServerHelper(@NonNull ArrayList<String> selectedBooksPath) {

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
      int DEFAULT_PORT = 8080;
      ServerUtils.port = DEFAULT_PORT;
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
