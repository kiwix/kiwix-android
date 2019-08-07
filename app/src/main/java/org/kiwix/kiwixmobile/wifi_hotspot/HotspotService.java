package org.kiwix.kiwixmobile.wifi_hotspot;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.main.MainActivity;
import org.kiwix.kiwixmobile.utils.Constants;
import org.kiwix.kiwixmobile.webserver.ServerStateListener;

import static org.kiwix.kiwixmobile.webserver.ZimHostActivity.ACTION_TURN_OFF_AFTER_O;
import static org.kiwix.kiwixmobile.webserver.ZimHostActivity.ACTION_TURN_ON_AFTER_O;
import static org.kiwix.kiwixmobile.webserver.WebServerHelper.stopAndroidWebServer;

/**
 * HotspotService is used to add a foreground service for the wifi hotspot.
 * Created by Adeel Zafar on 07/01/2019.
 */

public class HotspotService extends Service {
  private static final int HOTSPOT_NOTIFICATION_ID = 666;
  private static final String ACTION_STOP = "hotspot_stop";
  private WifiHotspotManager hotspotManager;
  private BroadcastReceiver stopReceiver;
  private NotificationManager notificationManager;
  private NotificationCompat.Builder builder;
  ServerStateListener serverStateListener;
  IBinder serviceBinder = new HotspotBinder();
  String TAG = HotspotService.this.getClass().getSimpleName();

  @Override public void onCreate() {

    super.onCreate();

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      stopReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          if (intent != null && intent.getAction().equals(ACTION_STOP)) {
            stopHotspot();
          }
        }
      };
    }
    registerReceiver(stopReceiver, new IntentFilter(ACTION_STOP));
    notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    startForeground(HOTSPOT_NOTIFICATION_ID,
        buildForegroundNotification(getString(R.string.hotspot_start), false));
  }

  @Override public int onStartCommand(Intent intent, int flags, int startId) {
    switch (intent.getAction()) {

      case ACTION_TURN_ON_AFTER_O:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          hotspotManager.turnOnHotspot();
          updateNotification(getString(R.string.hotspot_running), true);
        }
        break;

      case ACTION_TURN_OFF_AFTER_O:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          stopHotspot();
        }
        break;
      default:
        break;
    }
    return START_NOT_STICKY;
  }

  @Nullable @Override public IBinder onBind(Intent intent) {
    return serviceBinder;
  }

  private Notification buildForegroundNotification(String status, boolean showStopButton) {
    Log.v(TAG, "Building notification " + status);
    builder = new NotificationCompat.Builder(this);
    builder.setContentTitle("Kiwix Hotspot").setContentText(status);
    Intent targetIntent = new Intent(this, MainActivity.class);
    targetIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    PendingIntent contentIntent =
        PendingIntent.getActivity(this, 0, targetIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    builder.setContentIntent(contentIntent)
        .setSmallIcon(R.mipmap.kiwix_icon)
        .setWhen(System.currentTimeMillis());

    hotspotNotificationChannel();

    if (showStopButton) {
      Intent stopIntent = new Intent(ACTION_STOP);
      PendingIntent stopHotspot =
          PendingIntent.getBroadcast(this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);
      builder.addAction(R.drawable.ic_close_white_24dp, getString(R.string.tts_stop), stopHotspot);
    }
    return (builder.build());
  }

  private void updateNotification(String status, boolean stopAction) {
    notificationManager.notify(HOTSPOT_NOTIFICATION_ID,
        buildForegroundNotification(status, stopAction));
  }

  @RequiresApi(Build.VERSION_CODES.O)
  void stopHotspot() {
    hotspotManager.turnOffHotspot();
    stopForeground(true);
    stopSelf();
    stopAndroidWebServer();
  }

  @Override
  public void onDestroy() {
    if (stopReceiver != null) {
      unregisterReceiver(stopReceiver);
    }
    super.onDestroy();
  }

  private void hotspotNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      NotificationChannel hotspotServiceChannel = new NotificationChannel(
          Constants.HOTSPOT_SERVICE_CHANNEL_ID, getString(R.string.hotspot_service_channel_name),
          NotificationManager.IMPORTANCE_DEFAULT);
      hotspotServiceChannel.setDescription("Sample hotspot description");
      hotspotServiceChannel.setSound(null, null);
      builder.setChannelId(Constants.HOTSPOT_SERVICE_CHANNEL_ID);
      notificationManager.createNotificationChannel(hotspotServiceChannel);
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.O)
  public boolean checkHotspotState(Context context) {
    if (hotspotManager == null) {
      hotspotManager = new WifiHotspotManager(context);
    }
    return hotspotManager.checkHotspotState();
  }

  public class HotspotBinder extends Binder {

    public HotspotService getService() {
      return HotspotService.this;
    }
  }

  public void registerCallBack(ServerStateListener myCallback) {
    serverStateListener = myCallback;
  }
}
