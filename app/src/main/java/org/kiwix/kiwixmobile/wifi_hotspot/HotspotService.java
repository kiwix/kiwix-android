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

package org.kiwix.kiwixmobile.wifi_hotspot;

import android.app.Service;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import javax.inject.Inject;
import org.kiwix.kiwixmobile.KiwixApplication;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.utils.ServerUtils;
import org.kiwix.kiwixmobile.webserver.WebServerHelper;
import org.kiwix.kiwixmobile.webserver.ZimHostCallbacks;

import static org.kiwix.kiwixmobile.webserver.ZimHostActivity.SELECTED_ZIM_PATHS_KEY;
import static org.kiwix.kiwixmobile.wifi_hotspot.HotspotNotificationManager.HOTSPOT_NOTIFICATION_ID;

/**
 * HotspotService is used to add a foreground service for the wifi hotspot.
 * Created by Adeel Zafar on 07/01/2019.
 */

public class HotspotService extends Service implements HotspotStateListener, IpAddressCallbacks {

  public static final String ACTION_TOGGLE_HOTSPOT = "toggle_hotspot";
  public static final String ACTION_LOCATION_ACCESS_GRANTED = "location_access_granted";
  public static final String ACTION_START_SERVER = "start_server";
  public static final String ACTION_STOP_SERVER = "stop_server";
  public static final String ACTION_CHECK_IP_ADDRESS = "check_ip_address";

  public static final String ACTION_STOP = "hotspot_stop";
  private ZimHostCallbacks zimHostCallbacks;
  private final IBinder serviceBinder = new HotspotBinder();

  @Inject
  WebServerHelper webServerHelper;

  @Inject
  WifiHotspotManager hotspotManager;

  @Inject
  HotspotNotificationManager hotspotNotificationManager;

  @Override public void onCreate() {
    KiwixApplication.getApplicationComponent()
      .serviceComponent()
      .service(this)
      .build()
      .inject(this);
    super.onCreate();
  }

  @Override public int onStartCommand(@NonNull Intent intent, int flags, int startId) {
    switch (intent.getAction()) {

      case ACTION_TOGGLE_HOTSPOT:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          if (hotspotManager.isHotspotStarted()) {
            stopHotspotAndDismissNotification();
          } else {
            zimHostCallbacks.requestLocationAccess();
          }
        }
        break;

      case ACTION_LOCATION_ACCESS_GRANTED:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          hotspotManager.turnOnHotspot();
        }
        break;

      case ACTION_START_SERVER:
        if (webServerHelper.startServerHelper(
          intent.getStringArrayListExtra(SELECTED_ZIM_PATHS_KEY))) {
          zimHostCallbacks.onServerStarted(ServerUtils.getSocketAddress());
          if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            startForegroundNotificationHelper();
          }
          Toast.makeText(this, R.string.server_started__successfully_toast_message,
            Toast.LENGTH_SHORT).show();
        } else {
          zimHostCallbacks.onServerFailedToStart();
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true);
            stopSelf();
            hotspotNotificationManager.dismissNotification();
          }
        }

        break;

      case ACTION_STOP_SERVER:
        stopHotspotAndDismissNotification();
        break;

      case ACTION_CHECK_IP_ADDRESS:
        webServerHelper.pollForValidIpAddress();
        break;

      case ACTION_STOP:
        stopHotspotAndDismissNotification();
        break;

      default:
        break;
    }
    return START_NOT_STICKY;
  }

  @Nullable @Override public IBinder onBind(@Nullable Intent intent) {
    return serviceBinder;
  }

  //Dismiss notification and turn off hotspot for devices>=O
  private void stopHotspotAndDismissNotification() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      hotspotManager.turnOffHotspot();
    } else {
      webServerHelper.stopAndroidWebServer();
      zimHostCallbacks.onServerStopped();
      stopForeground(true);
      stopSelf();
      hotspotNotificationManager.dismissNotification();
    }
  }

  public void registerCallBack(@Nullable ZimHostCallbacks myCallback) {
    zimHostCallbacks = myCallback;
  }

  private void startForegroundNotificationHelper() {
    startForeground(HOTSPOT_NOTIFICATION_ID,
      hotspotNotificationManager.buildForegroundNotification());
  }

  @Override public void onHotspotTurnedOn(@NonNull WifiConfiguration wifiConfiguration) {
    startForegroundNotificationHelper();
    zimHostCallbacks.onHotspotTurnedOn(wifiConfiguration);
  }

  @Override public void onHotspotFailedToStart() {
    zimHostCallbacks.onHotspotFailedToStart();
  }

  @Override public void onHotspotStopped() {
    webServerHelper.stopAndroidWebServer();
    zimHostCallbacks.onServerStopped();
    stopForeground(true);
    stopSelf();
    hotspotNotificationManager.dismissNotification();
  }

  @Override public void onIpAddressValid() {
    zimHostCallbacks.onIpAddressValid();
  }

  @Override public void onIpAddressInvalid() {
    zimHostCallbacks.onIpAddressInvalid();
  }

  public class HotspotBinder extends Binder {

    @NonNull public HotspotService getService() {
      return HotspotService.this;
    }
  }
}
