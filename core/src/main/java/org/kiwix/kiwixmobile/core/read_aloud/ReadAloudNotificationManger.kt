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
package org.kiwix.kiwixmobile.core.read_aloud

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.read_aloud.ReadAloudService.Companion.IS_TTS_PAUSE_OR_RESUME
import org.kiwix.kiwixmobile.core.utils.READ_ALOUD_SERVICE_CHANNEL_ID
import javax.inject.Inject

class ReadAloudNotificationManger @Inject constructor(
  private val notificationManager: NotificationManager,
  private val context: Context
) {

  private fun readAloudNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      notificationManager.createNotificationChannel(
        NotificationChannel(
          READ_ALOUD_SERVICE_CHANNEL_ID,
          context.getString(R.string.read_aloud_service_channel_name),
          NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
          description = context.getString(R.string.read_aloud_channel_description)
          setSound(null, null)
        }
      )
    }
  }

  @SuppressLint("UnspecifiedImmutableFlag")
  fun buildForegroundNotification(isPauseTTS: Boolean): Notification {
    readAloudNotificationChannel()
    val stopIntent = Intent(context, ReadAloudService::class.java).setAction(
      ReadAloudService.ACTION_STOP_TTS
    )
    val stopReadAloud = PendingIntent.getService(
      context,
      0,
      stopIntent,
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    val pauseOrResumeIntent = Intent(context, ReadAloudService::class.java).setAction(
      ReadAloudService.ACTION_PAUSE_OR_RESUME_TTS
    ).putExtra(IS_TTS_PAUSE_OR_RESUME, !isPauseTTS)
    val pauseOrResumeReadAloud = PendingIntent.getService(
      context,
      0,
      pauseOrResumeIntent,
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    return NotificationCompat.Builder(context, READ_ALOUD_SERVICE_CHANNEL_ID)
      .setContentTitle(context.getString(R.string.menu_read_aloud))
      .setContentText(context.getString(R.string.read_aloud_running))
      .setContentIntent(null)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setWhen(System.currentTimeMillis())
      .addAction(
        R.drawable.ic_baseline_stop,
        context.getString(R.string.stop),
        stopReadAloud
      ).addAction(
        getPauseOrResumeIcon(isPauseTTS),
        getPauseOrResumeTitle(isPauseTTS),
        pauseOrResumeReadAloud
      )
      .build()
  }

  private fun getPauseOrResumeTitle(isPauseTTS: Boolean) =
    if (isPauseTTS) context.getString(R.string.tts_resume)
    else context.getString(R.string.tts_pause)

  private fun getPauseOrResumeIcon(isPauseTTS: Boolean) =
    if (isPauseTTS) R.drawable.ic_baseline_play
    else R.drawable.ic_baseline_pause

  fun dismissNotification() {
    notificationManager.cancel(READ_ALOUD_NOTIFICATION_ID)
  }

  companion object {
    const val READ_ALOUD_NOTIFICATION_ID = 777
  }
}
