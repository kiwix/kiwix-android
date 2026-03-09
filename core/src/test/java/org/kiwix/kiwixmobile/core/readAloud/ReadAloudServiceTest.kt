/*
 * Kiwix Android
 * Copyright (c) 2026 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.readAloud

import android.app.Notification
import android.app.Service
import android.content.Intent
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.readAloud.ReadAloudService.Companion.ACTION_PAUSE_OR_RESUME_TTS
import org.kiwix.kiwixmobile.core.readAloud.ReadAloudService.Companion.ACTION_STOP_TTS
import org.kiwix.kiwixmobile.core.readAloud.ReadAloudService.Companion.IS_TTS_PAUSE_OR_RESUME

class ReadAloudServiceTest {
  private val notificationManager: ReadAloudNotificationManager = mockk(relaxed = true)
  private val readAloudCallbacks: ReadAloudCallbacks = mockk(relaxed = true)
  private val notification: Notification = mockk(relaxed = true)
  private lateinit var readAloudService: ReadAloudService

  @BeforeEach
  fun setUp() {
    clearAllMocks()
    readAloudService = spyk(ReadAloudService())
    readAloudService.readAloudNotificationManager = notificationManager
    readAloudService.registerCallBack(readAloudCallbacks)
    every { readAloudService.startForeground(any(), any()) } returns Unit
    every { readAloudService.stopForeground(any<Int>()) } returns Unit
    every { readAloudService.stopSelf() } returns Unit
  }

  private fun createIntent(action: String, isPauseTTS: Boolean = false): Intent {
    val intent: Intent = mockk(relaxed = true)
    every { intent.action } returns action
    every { intent.getBooleanExtra(IS_TTS_PAUSE_OR_RESUME, false) } returns isPauseTTS
    return intent
  }

  @Test
  fun `pause TTS when pause action received with isPause true`() {
    val pauseIntent = createIntent(ACTION_PAUSE_OR_RESUME_TTS, isPauseTTS = true)
    every {
      notificationManager.buildForegroundNotification(true)
    } returns notification

    readAloudService.onStartCommand(pauseIntent, 0, 1)

    verify { readAloudCallbacks.onReadAloudPauseOrResume(true) }
  }

  @Test
  fun `resume TTS when pause or resume action received with isPause false`() {
    val resumeIntent = createIntent(ACTION_PAUSE_OR_RESUME_TTS, isPauseTTS = false)
    every {
      notificationManager.buildForegroundNotification(false)
    } returns notification

    readAloudService.onStartCommand(resumeIntent, 0, 1)

    verify { readAloudCallbacks.onReadAloudPauseOrResume(false) }
  }

  @Test
  fun `start foreground notification when pause or resume action received`() {
    val pauseIntent = createIntent(ACTION_PAUSE_OR_RESUME_TTS, isPauseTTS = true)
    every {
      notificationManager.buildForegroundNotification(true)
    } returns notification

    readAloudService.onStartCommand(pauseIntent, 0, 1)

    verify {
      readAloudService.startForeground(
        ReadAloudNotificationManager.Companion.READ_ALOUD_NOTIFICATION_ID,
        notification
      )
    }
  }

  @Test
  fun `stops TTS and dismisses notification when stop action received`() {
    val stopIntent = createIntent(ACTION_STOP_TTS)

    readAloudService.onStartCommand(stopIntent, 0, 1)

    verify { readAloudCallbacks.onReadAloudStop() }
    verify { readAloudService.stopForeground(Service.STOP_FOREGROUND_REMOVE) }
    verify { readAloudService.stopSelf() }
    verify { notificationManager.dismissNotification() }
  }

  @Test
  fun `does not crash when callbacks are null and stop action received`() {
    readAloudService.registerCallBack(null)
    val stopIntent = createIntent(ACTION_STOP_TTS)

    readAloudService.onStartCommand(stopIntent, 0, 1)

    verify { readAloudService.stopForeground(Service.STOP_FOREGROUND_REMOVE) }
    verify { readAloudService.stopSelf() }
    verify { notificationManager.dismissNotification() }
  }

  @Test
  fun `does not crash when notification manager is null and pause action received`() {
    readAloudService.readAloudNotificationManager = null

    val pauseIntent = createIntent(ACTION_PAUSE_OR_RESUME_TTS, isPauseTTS = false)
    readAloudService.onStartCommand(pauseIntent, 0, 1)
    verify { readAloudCallbacks.onReadAloudPauseOrResume(false) }
  }

  @Test
  fun `returns START_NOT_STICKY from onStartCommand`() {
    val stopIntent = createIntent(ACTION_STOP_TTS)

    val result = readAloudService.onStartCommand(stopIntent, 0, 1)

    assertThat(result).isEqualTo(Service.START_NOT_STICKY)
  }

  @Test
  fun `returns service binder on bind`() {
    val binder = readAloudService.onBind(null)

    assertThat(binder).isInstanceOf(ReadAloudService.ReadAloudBinder::class.java)

    val readAloudBinder = binder as ReadAloudService.ReadAloudBinder
    assertThat(readAloudBinder.service.get()).isNotNull
  }

  @Test
  fun `replaces previous callback when registering a new one`() {
    val newCallback: ReadAloudCallbacks = mockk(relaxed = true)
    readAloudService.registerCallBack(newCallback)
    val stopIntent = createIntent(ACTION_STOP_TTS)

    readAloudService.onStartCommand(stopIntent, 0, 1)

    verify { newCallback.onReadAloudStop() }
    verify(exactly = 0) { readAloudCallbacks.onReadAloudStop() }
  }

  @Test
  fun `stop service and dismiss notification on timeout`() {
    readAloudService.onTimeout(1)

    verify { readAloudCallbacks.onReadAloudStop() }
    verify { readAloudService.stopForeground(Service.STOP_FOREGROUND_REMOVE) }
    verify { readAloudService.stopSelf() }
    verify { notificationManager.dismissNotification() }
  }
}
