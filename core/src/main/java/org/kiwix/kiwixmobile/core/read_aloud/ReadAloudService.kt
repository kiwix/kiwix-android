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

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import org.kiwix.kiwixmobile.core.CoreApp
import java.lang.ref.WeakReference
import javax.inject.Inject

class ReadAloudService : Service() {
  @set:Inject
  var readAloudNotificationManager: ReadAloudNotificationManger? = null
  private val serviceBinder: IBinder = ReadAloudBinder(this)
  private var readAloudCallbacks: ReadAloudCallbacks? = null

  override fun onCreate() {
    super.onCreate()
    CoreApp.coreComponent
      .coreServiceComponent()
      .service(this)
      .build()
      .inject(this)
  }

  override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
    when (intent.action) {
      ACTION_PAUSE_OR_RESUME_TTS -> {
        val isPauseTTS = intent.getBooleanExtra(IS_TTS_PAUSE_OR_RESUME, false)
        startForegroundNotificationHelper(isPauseTTS)
        readAloudCallbacks?.onReadAloudPauseOrResume(isPauseTTS)
      }
      ACTION_STOP_TTS -> {
        stopReadAloudAndDismissNotification()
      }
    }
    return START_NOT_STICKY
  }

  private fun stopReadAloudAndDismissNotification() {
    readAloudCallbacks?.onReadAloudStop()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      stopForeground(STOP_FOREGROUND_REMOVE)
    }
    stopSelf()
    readAloudNotificationManager?.dismissNotification()
  }

  private fun startForegroundNotificationHelper(isPauseTTS: Boolean) {
    val notification = readAloudNotificationManager?.buildForegroundNotification(isPauseTTS)
    startForeground(
      ReadAloudNotificationManger.READ_ALOUD_NOTIFICATION_ID,
      notification
    )
  }

  override fun onBind(p0: Intent?): IBinder = serviceBinder

  fun registerCallBack(readAloudCallbacks: ReadAloudCallbacks?) {
    this.readAloudCallbacks = readAloudCallbacks
  }

  class ReadAloudBinder(readAloudService: ReadAloudService) : Binder() {
    val service: WeakReference<ReadAloudService>

    init {
      service = WeakReference<ReadAloudService>(readAloudService)
    }
  }

  companion object {
    const val ACTION_STOP_TTS = "ACTION_STOP_TTS"
    const val ACTION_PAUSE_OR_RESUME_TTS = "ACTION_PAUSE_OR_RESUME_TTS"
    const val IS_TTS_PAUSE_OR_RESUME = "IS_TTS_PAUSE_OR_RESUME"
  }
}
