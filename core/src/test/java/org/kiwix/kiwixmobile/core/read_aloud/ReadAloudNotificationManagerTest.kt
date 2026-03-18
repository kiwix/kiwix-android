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

package org.kiwix.kiwixmobile.core.read_aloud

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.R
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.kiwix.kiwixmobile.core.read_aloud.ReadAloudNotificationManager.Companion.READ_ALOUD_NOTIFICATION_ID
import org.kiwix.kiwixmobile.core.read_aloud.ReadAloudService.Companion.ACTION_PAUSE_OR_RESUME_TTS
import org.kiwix.kiwixmobile.core.read_aloud.ReadAloudService.Companion.ACTION_STOP_TTS
import org.kiwix.kiwixmobile.core.read_aloud.ReadAloudService.Companion.IS_TTS_PAUSE_OR_RESUME
import org.kiwix.kiwixmobile.core.utils.READ_ALOUD_SERVICE_CHANNEL_ID
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])
class ReadAloudNotificationManagerTest {
  private lateinit var context: Context
  private lateinit var notificationManager: NotificationManager
  private lateinit var readAloudNotificationManager: ReadAloudNotificationManager

  @Before
  fun setUp() {
    clearAllMocks()
    context = ApplicationProvider.getApplicationContext()
    notificationManager = mockk(relaxed = true)
    readAloudNotificationManager = ReadAloudNotificationManager(notificationManager, context)
  }

  private fun buildNotification(isPaused: Boolean): Notification =
    readAloudNotificationManager.buildForegroundNotification(isPaused)

  @Test
  fun `shows pause button when TTS is playing`() {
    val notification = buildNotification(false)

    val pauseOrResumeAction = notification.actions[1]

    assertThat(notification.actions).hasSize(2)
    assertThat(pauseOrResumeAction.title)
      .isEqualTo(context.getString(R.string.tts_pause))
    assertThat(pauseOrResumeAction.icon)
      .isEqualTo(R.drawable.ic_baseline_pause)
  }

  @Test
  fun `shows resume button when TTS is paused`() {
    val notification = buildNotification(true)

    val pauseOrResumeAction = notification.actions[1]

    assertThat(notification.actions).hasSize(2)
    assertThat(pauseOrResumeAction.title)
      .isEqualTo(context.getString(R.string.tts_resume))
    assertThat(pauseOrResumeAction.icon)
      .isEqualTo(R.drawable.ic_baseline_play)
  }

  @Test
  fun `includes stop action as first button`() {
    val notification = buildNotification(false)
    val actions = notification.actions
    val stopAction = actions[0]

    assertThat(actions).hasSize(2)
    assertThat(stopAction.title)
      .isEqualTo(context.getString(R.string.stop))
    assertThat(stopAction.icon)
      .isEqualTo(R.drawable.ic_baseline_stop)
  }

  @Test
  fun `has exactly two actions in the notification`() {
    val notification = buildNotification(false)

    assertThat(notification.actions).hasSize(2)
  }

  @Test
  fun `sets correct notification content title and text`() {
    val notification = buildNotification(false)
    val extras = notification.extras
    val contentTitle = extras.getString(Notification.EXTRA_TITLE)
    val contentText = extras.getString(Notification.EXTRA_TEXT)

    assertThat(contentTitle).isEqualTo(context.getString(R.string.menu_read_aloud))
    assertThat(contentText).isEqualTo(context.getString(R.string.read_aloud_running))
    assertThat(notification.contentIntent).isNull()
  }

  @Test
  fun `sets small icon on notification`() {
    val notification = buildNotification(false)

    assertThat(notification.smallIcon.resId)
      .isEqualTo(R.mipmap.ic_launcher)
  }

  @Test
  fun `cancels notification with READ_ALOUD_NOTIFICATION_ID when dismissed`() {
    readAloudNotificationManager.dismissNotification()

    verify(exactly = 1) {
      notificationManager.cancel(READ_ALOUD_NOTIFICATION_ID)
    }
  }

  @Test
  fun `creates notification channel on Android O and above`() {
    val slot = slot<NotificationChannel>()
    every { notificationManager.createNotificationChannel(capture(slot)) } just Runs

    buildNotification(false)

    verify(exactly = 1) {
      notificationManager.createNotificationChannel(any())
    }

    val channel = slot.captured
    assertThat(channel.id).isEqualTo(READ_ALOUD_SERVICE_CHANNEL_ID)
    assertThat(channel.importance)
      .isEqualTo(NotificationManager.IMPORTANCE_DEFAULT)
    assertThat(channel.name).isEqualTo(context.getString(R.string.read_aloud_service_channel_name))
    assertThat(channel.description).isEqualTo(context.getString(R.string.read_aloud_channel_description))
    assertThat(channel.sound).isNull()
  }

  @Test
  fun `stop action sends correct intent`() {
    val notification = buildNotification(false)

    val stopAction = notification.actions[0]
    val intent = shadowOf(stopAction.actionIntent).savedIntent
    assertThat(intent.action)
      .isEqualTo(ACTION_STOP_TTS)
  }

  @Test
  fun `pause action sends correct extra when playing`() {
    val notification = buildNotification(false)

    val action = notification.actions[1]
    val intent = shadowOf(action.actionIntent).savedIntent
    assertThat(intent.action)
      .isEqualTo(ACTION_PAUSE_OR_RESUME_TTS)
    assertThat(intent.getBooleanExtra(IS_TTS_PAUSE_OR_RESUME, false)).isTrue()
  }

  @Test
  fun `resume action sends correct extra when paused`() {
    val notification = buildNotification(true)

    val action = notification.actions[1]
    val intent = shadowOf(action.actionIntent).savedIntent
    assertThat(intent.getBooleanExtra(IS_TTS_PAUSE_OR_RESUME, true)).isFalse()
  }

  @Test
  fun `sets valid timestamp on notification`() {
    val before = System.currentTimeMillis()

    val notification = buildNotification(false)

    val after = System.currentTimeMillis()

    assertThat(notification.`when`).isBetween(before, after)
  }

  @Test
  fun `actions are ordered as stop then pause or resume`() {
    val notification = buildNotification(false)

    val actions = notification.actions

    assertThat(actions[0].title)
      .isEqualTo(context.getString(R.string.stop))
    assertThat(actions[1].title)
      .isEqualTo(context.getString(R.string.tts_pause))
  }
}
