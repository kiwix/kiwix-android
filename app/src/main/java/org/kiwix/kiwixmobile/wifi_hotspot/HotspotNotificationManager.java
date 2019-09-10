/*
 * Copyright (c) 2019 Kiwix
 * All rights reserved.
 */

package org.kiwix.kiwixmobile.wifi_hotspot;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import javax.inject.Inject;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.utils.Constants;
import org.kiwix.kiwixmobile.webserver.ZimHostActivity;

import static org.kiwix.kiwixmobile.wifi_hotspot.HotspotService.ACTION_STOP;

public class HotspotNotificationManager {

  public static final int HOTSPOT_NOTIFICATION_ID = 666;
  private Context context;

  @Inject
  NotificationManager notificationManager;

  @Inject
  public HotspotNotificationManager(@NonNull NotificationManager notificationManager,
    @NonNull Context context) {
    this.notificationManager = notificationManager;
    this.context = context;
  }

  private void hotspotNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      NotificationChannel hotspotServiceChannel = new NotificationChannel(
        Constants.HOTSPOT_SERVICE_CHANNEL_ID,
        context.getString(R.string.hotspot_service_channel_name),
        NotificationManager.IMPORTANCE_DEFAULT);
      hotspotServiceChannel.setDescription(context.getString(R.string.hotspot_channel_description));
      hotspotServiceChannel.setSound(null, null);
      notificationManager.createNotificationChannel(hotspotServiceChannel);
    }
  }

  @NonNull public Notification buildForegroundNotification() {
    Intent targetIntent = new Intent(context, ZimHostActivity.class);
    targetIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    PendingIntent contentIntent =
      PendingIntent.getActivity(context, 0, targetIntent, PendingIntent.FLAG_UPDATE_CURRENT);

    hotspotNotificationChannel();

    Intent stopIntent = new Intent(context, HotspotService.class).setAction(ACTION_STOP);
    PendingIntent stopHotspot =
      PendingIntent.getService(context, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);

    return new NotificationCompat.Builder(context)
      .setContentTitle(context.getString(R.string.hotspot_notification_content_title))
      .setContentText(context.getString(R.string.hotspot_running))
      .setContentIntent(contentIntent)
      .setSmallIcon(R.mipmap.kiwix_icon)
      .setWhen(System.currentTimeMillis())
      .addAction(R.drawable.ic_close_white_24dp,
        context.getString(R.string.stop_hotspot_button),
        stopHotspot)
      .setChannelId(Constants.HOTSPOT_SERVICE_CHANNEL_ID)
      .build();
  }

  public void dismissNotification() {
    notificationManager.cancel(HOTSPOT_NOTIFICATION_ID);
  }
}
