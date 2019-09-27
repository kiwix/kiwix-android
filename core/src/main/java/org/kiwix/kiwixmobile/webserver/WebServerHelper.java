package org.kiwix.kiwixmobile.webserver;

import android.util.Log;
import androidx.annotation.NonNull;
import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import org.kiwix.kiwixlib.JNIKiwixException;
import org.kiwix.kiwixlib.JNIKiwixLibrary;
import org.kiwix.kiwixlib.JNIKiwixServer;
import org.kiwix.kiwixmobile.utils.ServerUtils;
import org.kiwix.kiwixmobile.wifi_hotspot.IpAddressCallbacks;

import static org.kiwix.kiwixmobile.utils.ServerUtils.INVALID_IP;

/**
 * WebServerHelper class is used to set up the suitable environment i.e. getting the
 * ip address and port no. before starting the WebServer
 * Created by Adeel Zafar on 18/07/2019.
 */

public class WebServerHelper {
  private static final String TAG = "WebServerHelper";
  private JNIKiwixLibrary kiwixLibrary;
  private JNIKiwixServer kiwixServer;
  private IpAddressCallbacks ipAddressCallbacks;
  private boolean isServerStarted;

  @Inject public WebServerHelper(@NonNull JNIKiwixLibrary kiwixLibrary,
    @NonNull JNIKiwixServer kiwixServer, @NonNull IpAddressCallbacks ipAddressCallbacks) {
    this.kiwixLibrary = kiwixLibrary;
    this.kiwixServer = kiwixServer;
    this.ipAddressCallbacks = ipAddressCallbacks;
  }

  public boolean startServerHelper(@NonNull ArrayList<String> selectedBooksPath) {
    String ip = ServerUtils.getIpAddress();
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
      updateServerState(false);
    }
  }

  private boolean startAndroidWebServer(ArrayList<String> selectedBooksPath) {
    if (!isServerStarted) {
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
      updateServerState(kiwixServer.start());
      Log.v(TAG, "Server status" + isServerStarted);
    }
    return isServerStarted;
  }

  private void updateServerState(boolean isStarted) {
    isServerStarted = isStarted;
    ServerUtils.isServerStarted = isStarted;
  }

  //Keeps checking if hotspot has been turned using the ip address with an interval of 1 sec
  //If no ip is found after 15 seconds, dismisses the progress dialog
  public void pollForValidIpAddress() {
    Flowable.interval(1, TimeUnit.SECONDS)
      .map(__ -> ServerUtils.getIp())
      .filter(s -> s != INVALID_IP)
      .timeout(15, TimeUnit.SECONDS)
      .take(1)
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe(
        s -> {
          ipAddressCallbacks.onIpAddressValid();
          Log.d(TAG, "onSuccess:  " + s);
        },
        e -> {
          Log.d(TAG, "Unable to turn on server", e);
          ipAddressCallbacks.onIpAddressInvalid();
        }
      );
  }
}
