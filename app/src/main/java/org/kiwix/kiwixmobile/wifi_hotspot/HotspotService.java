package org.kiwix.kiwixmobile.wifi_hotspot;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.main.MainActivity;

import static org.kiwix.kiwixmobile.main.MainActivity.ACTION_TURN_OFF_AFTER_O;
import static org.kiwix.kiwixmobile.main.MainActivity.ACTION_TURN_OFF_BEFORE_O;
import static org.kiwix.kiwixmobile.main.MainActivity.ACTION_TURN_ON_AFTER_O;
import static org.kiwix.kiwixmobile.main.MainActivity.ACTION_TURN_ON_BEFORE_O;

public class HotspotService extends Service {
  private static final int HOTSPOT_NOTIFICATION_ID = 666;
  public static final String ACTION_START = "hotspot_start";
  public static final String ACTION_STOP = "hotspot_stop";
  public static final String ACTION_STATUS = "hotspot_status";
  private WifiHotspotManager wifiHotspotManager;
  private BroadcastReceiver stopReceiver;

  @Override public void onCreate() {
    super.onCreate();
    wifiHotspotManager = new WifiHotspotManager(this);
    stopReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        if (intent != null && intent.getAction().equals(ACTION_STOP)) {
          stopHotspot();
        }
      }
    };
    registerReceiver(stopReceiver, new IntentFilter(ACTION_STOP));
    startForeground(HOTSPOT_NOTIFICATION_ID,
        buildForegroundNotification(getString(R.string.hotspot_start), false));
  }

  @Override public int onStartCommand(Intent intent, int flags, int startId) {
    switch (intent.getAction()) {
      case ACTION_TURN_ON_BEFORE_O:
        if (wifiHotspotManager.setWifiEnabled(null, true)) {
          updateNotification(getString(R.string.hotspot_running), true);
        }
        break;
      case ACTION_TURN_ON_AFTER_O:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          wifiHotspotManager.turnOnHotspot();
        }
        break;
      case ACTION_TURN_OFF_BEFORE_O:
        wifiHotspotManager.setWifiEnabled(null, false);
        stopForeground(true);
        stopSelf();
        break;
      case ACTION_TURN_OFF_AFTER_O:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          wifiHotspotManager.turnOffHotspot();
        }
        stopForeground(true);
        stopSelf();

      default:
        break;
    }
    return START_NOT_STICKY;
  }

  @Nullable @Override public IBinder onBind(Intent intent) {
    return null;
  }

  private Notification buildForegroundNotification(String status, boolean showStopButton) {
    Log.v("DANG","Building notification "+status);
    NotificationCompat.Builder b = new NotificationCompat.Builder(this);
    b.setContentTitle("Kiwix Hotspot").setContentText(status);
    Intent targetIntent = new Intent(this, MainActivity.class);
    targetIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    PendingIntent contentIntent =
        PendingIntent.getActivity(this, 0, targetIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    b.setContentIntent(contentIntent)
        .setSmallIcon(R.mipmap.kiwix_icon)
        .setWhen(System.currentTimeMillis());
    if (showStopButton) {
      Intent stopIntent = new Intent(ACTION_STOP);
      PendingIntent stopHotspot =
          PendingIntent.getBroadcast(this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);
      b.addAction(R.drawable.ic_close_white_24dp, getString(R.string.tts_stop), stopHotspot);
    }
    return (b.build());
  }

  private void updateNotification(String status, boolean stopAction) {
    NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    nm.notify(HOTSPOT_NOTIFICATION_ID, buildForegroundNotification(status, stopAction));
  }

  private void stopHotspot() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      wifiHotspotManager.turnOffHotspot();
    } else {
      Log.v("DANG", "Coming yes");
      wifiHotspotManager.setWifiEnabled(null, false);
    }
    stopForeground(true);
    stopSelf();
  }

  @Override
  public void onDestroy() {
    if (stopReceiver != null) {
      unregisterReceiver(stopReceiver);
    }
    super.onDestroy();
  }
}
