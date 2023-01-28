/*
 * Kiwix Android
 * Copyright (c) 2023 Kiwix <android.kiwix.org>
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
package org.kiwix.kiwixmobile.core.main

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.main.ReadAloudNotificationManger.ReadAloud.ACTION_PAUSE
import org.kiwix.kiwixmobile.core.main.ReadAloudNotificationManger.ReadAloud.ACTION_STOP

class ReadAloudNotificationManger {
  fun makeNotification(message: String, context: Context) {
    val actionIntent_stop: Intent = Intent(context, ReadAloudService::class.java)
      .setAction(ACTION_STOP)
    val actionPendingIntent_stop: PendingIntent = PendingIntent.getService(
      context, 0,
      actionIntent_stop, PendingIntent.FLAG_ONE_SHOT
    )
    val actionIntent_pause_play: Intent = Intent(context, ReadAloudService::class.java)
      .setAction(ACTION_PAUSE)
    val actionPendingIntent_pause_play: PendingIntent = PendingIntent.getService(
      context, 0,
      actionIntent_pause_play, PendingIntent.FLAG_ONE_SHOT
    )
    val builder: NotificationCompat.Builder = Builder(context)
      .setSmallIcon(R.drawable.ic_home_kiwix_banner)
      //.setContentTitle(context.getString(R.string.tts_stop))
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .addAction(
        R.drawable.ic_close_white_24dp, context.getString(R.string.tts_stop),
        actionPendingIntent_stop
      )
      .setPriority(Notification.PRIORITY_MAX)
    if (message == context.getString(R.string.tts_resume)) {
      builder.addAction(
        R.drawable.ic_close_white_24dp, message,
        actionPendingIntent_pause_play
      )
    } else {
      builder.addAction(
        R.drawable.ic_close_white_24dp, message,
        actionPendingIntent_pause_play
      )
    }
    val notificationManager: NotificationManager =
      context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.notify(TTS_NOTIFICATION_ID, builder.build())
  }

  object ReadAloud {
    const val ACTION_STOP = "ACTION_STOP"
    const val ACTION_RESUME = "ACTION_RESUME"
    const val ACTION_PAUSE = "ACTION_PAUSE"
  }
}
