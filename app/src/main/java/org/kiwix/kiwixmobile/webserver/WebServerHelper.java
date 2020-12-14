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

package org.kiwix.kiwixmobile.webserver;

import android.util.Log;
import androidx.annotation.NonNull;
import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import org.kiwix.kiwixlib.JNIKiwixServer;
import org.kiwix.kiwixmobile.core.utils.ServerUtils;
import org.kiwix.kiwixmobile.webserver.wifi_hotspot.IpAddressCallbacks;

import static org.kiwix.kiwixmobile.core.utils.ServerUtils.INVALID_IP;

/**
 * WebServerHelper class is used to set up the suitable environment i.e. getting the
 * ip address and port no. before starting the WebServer
 * Created by Adeel Zafar on 18/07/2019.
 */

public class WebServerHelper {
  private static final String TAG = "WebServerHelper";
  private KiwixServer kiwixServer;
  private KiwixServer.Factory kiwixServerFactory;
  private IpAddressCallbacks ipAddressCallbacks;
  private boolean isServerStarted;
  private JNIKiwixServer server;

  @Inject public WebServerHelper(@NonNull KiwixServer.Factory kiwixServerFactory,
    @NonNull IpAddressCallbacks ipAddressCallbacks) {
    this.kiwixServerFactory = kiwixServerFactory;
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
      kiwixServer.stopServer();
      updateServerState(false);
    }
  }

  private boolean startAndroidWebServer(ArrayList<String> selectedBooksPath) {
    if (!isServerStarted) {
      int DEFAULT_PORT = 8080;
      ServerUtils.port = DEFAULT_PORT;
      kiwixServer = kiwixServerFactory.createKiwixServer(selectedBooksPath);
      updateServerState(kiwixServer.startServer(ServerUtils.port));
      Log.d(TAG, "Server status" + isServerStarted);
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
