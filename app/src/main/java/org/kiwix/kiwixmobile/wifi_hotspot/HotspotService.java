package org.kiwix.kiwixmobile.wifi_hotspot;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import javax.inject.Inject;
import org.kiwix.kiwixmobile.KiwixApplication;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.utils.ServerUtils;
import org.kiwix.kiwixmobile.webserver.ZimHostCallbacks;
import org.kiwix.kiwixmobile.webserver.WebServerHelper;

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

  public static final String TAG = "HotspotService";
  private ZimHostCallbacks zimHostCallbacks;
  private final IBinder serviceBinder = new HotspotBinder();
  private BroadcastReceiver hotspotStateReceiver;

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
    setUpHotspotStateReceiver();
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

  private void setUpHotspotStateReceiver() {
    hotspotStateReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if ("android.net.wifi.WIFI_AP_STATE_CHANGED".equals(action)) {
          int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);

          if (WifiManager.WIFI_STATE_ENABLED == state % 10) {
            Log.v(TAG, "Hotspot enabled");
          } else {
            initiateServiceDismissal();
            Log.v(TAG, "Hotspot disabled");
          }
        }
      }
    };

    IntentFilter intentFilter = new IntentFilter("android.net.wifi.WIFI_AP_STATE_CHANGED");
    registerReceiver(hotspotStateReceiver, intentFilter);
  }

  private void initiateServiceDismissal() {
    if (ServerUtils.isServerStarted) {
      ServerUtils.isServerStarted = false;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        hotspotManager.turnOffHotspot();
      } else {
        webServerHelper.stopAndroidWebServer();
        callbackAndDismissService();
      }
    }
  }

  @Override public void onDestroy() {
    super.onDestroy();
    unregisterReceiver(hotspotStateReceiver);
  }

  //Dismiss notification and turn off hotspot for devices>=O
  private void stopHotspotAndDismissNotification() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      hotspotManager.turnOffHotspot();
    } else {
      webServerHelper.stopAndroidWebServer();
      callbackAndDismissService();
    }
  }

  private void callbackAndDismissService() {
    zimHostCallbacks.onServerStopped();
    stopForeground(true);
    stopSelf();
    hotspotNotificationManager.dismissNotification();
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
    callbackAndDismissService();
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
