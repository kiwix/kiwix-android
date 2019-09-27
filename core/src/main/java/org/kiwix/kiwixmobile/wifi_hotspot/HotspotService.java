package org.kiwix.kiwixmobile.wifi_hotspot;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import javax.inject.Inject;
import org.kiwix.kiwixmobile.KiwixApplication;
import org.kiwix.kiwixmobile.core.R;
import org.kiwix.kiwixmobile.extensions.ContextExtensionsKt;
import org.kiwix.kiwixmobile.utils.ServerUtils;
import org.kiwix.kiwixmobile.webserver.WebServerHelper;
import org.kiwix.kiwixmobile.webserver.ZimHostCallbacks;

import static org.kiwix.kiwixmobile.webserver.ZimHostActivity.SELECTED_ZIM_PATHS_KEY;
import static org.kiwix.kiwixmobile.wifi_hotspot.HotspotNotificationManager.HOTSPOT_NOTIFICATION_ID;

/**
 * HotspotService is used to add a foreground service for the wifi hotspot.
 * Created by Adeel Zafar on 07/01/2019.
 */

public class HotspotService extends Service
  implements IpAddressCallbacks, HotspotStateReceiver.Callback {

  public static final String ACTION_START_SERVER = "start_server";
  public static final String ACTION_STOP_SERVER = "stop_server";
  public static final String ACTION_CHECK_IP_ADDRESS = "check_ip_address";

  private ZimHostCallbacks zimHostCallbacks;
  private final IBinder serviceBinder = new HotspotBinder();

  @Inject
  WebServerHelper webServerHelper;
  @Inject
  HotspotNotificationManager hotspotNotificationManager;
  @Inject
  HotspotStateReceiver hotspotStateReceiver;

  @Override public void onCreate() {
    KiwixApplication.getApplicationComponent()
      .serviceComponent()
      .service(this)
      .build()
      .inject(this);
    super.onCreate();
    ContextExtensionsKt.registerReceiver(this, hotspotStateReceiver);
  }

  @Override public void onDestroy() {
    unregisterReceiver(hotspotStateReceiver);
    super.onDestroy();
  }

  @Override public int onStartCommand(@NonNull Intent intent, int flags, int startId) {
    switch (intent.getAction()) {

      case ACTION_START_SERVER:
        if (webServerHelper.startServerHelper(
          intent.getStringArrayListExtra(SELECTED_ZIM_PATHS_KEY))) {
          zimHostCallbacks.onServerStarted(ServerUtils.getSocketAddress());
          startForegroundNotificationHelper();
          Toast.makeText(this, R.string.server_started__successfully_toast_message,
            Toast.LENGTH_SHORT).show();
        } else {
          zimHostCallbacks.onServerFailedToStart();
        }

        break;

      case ACTION_STOP_SERVER:
        stopHotspotAndDismissNotification();
        break;

      case ACTION_CHECK_IP_ADDRESS:
        webServerHelper.pollForValidIpAddress();
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
    webServerHelper.stopAndroidWebServer();
    if (zimHostCallbacks != null) {
      zimHostCallbacks.onServerStopped();
    }
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

  @Override public void onIpAddressValid() {
    if (zimHostCallbacks != null) {
      zimHostCallbacks.onIpAddressValid();
    }
  }

  @Override public void onIpAddressInvalid() {
    if (zimHostCallbacks != null) {
      zimHostCallbacks.onIpAddressInvalid();
    }
  }

  @Override public void onHotspotDisabled() {
    stopHotspotAndDismissNotification();
  }

  public class HotspotBinder extends Binder {

    @NonNull public HotspotService getService() {
      return HotspotService.this;
    }
  }
}
