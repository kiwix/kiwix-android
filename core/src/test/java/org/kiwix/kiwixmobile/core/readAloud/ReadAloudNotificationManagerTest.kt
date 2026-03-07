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
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.R
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class ReadAloudNotificationManagerTest {
  private lateinit var context: Context
  private lateinit var notificationManager: NotificationManager
  private lateinit var readAloudNotificationManger: ReadAloudNotificationManager

  @Before
  fun setUp() {
    clearAllMocks()
    context = ApplicationProvider.getApplicationContext()
    notificationManager = mockk(relaxed = true)
    readAloudNotificationManger = ReadAloudNotificationManager(notificationManager, context)
  }

  private fun buildNotification(isPaused: Boolean): Notification =
    readAloudNotificationManger.buildForegroundNotification(isPaused)

  @Test
  fun `shows pause button when TTS is playing`() {
    val notification = buildNotification(false)

    val pauseOrResumeAction = notification.actions[1]
    assertThat(pauseOrResumeAction.title)
      .isEqualTo(context.getString(R.string.tts_pause))
    assertThat(pauseOrResumeAction.icon)
      .isEqualTo(R.drawable.ic_baseline_pause)
  }

  @Test
  fun `shows resume button when TTS is paused`() {
    val notification = buildNotification(true)

    val pauseOrResumeAction = notification.actions[1]
    assertThat(pauseOrResumeAction.title)
      .isEqualTo(context.getString(R.string.tts_resume))
    assertThat(pauseOrResumeAction.icon)
      .isEqualTo(R.drawable.ic_baseline_play)
  }

  @Test
  fun `includes stop action as first button`() {
    val notification = buildNotification(false)

    val stopAction = notification.actions[0]
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
  }

  @Test
  fun `sets small icon on notification`() {
    val notification = buildNotification(false)

    assertThat(notification.smallIcon).isNotNull()
  }

  @Test
  fun `cancels notification with READ_ALOUD_NOTIFICATION_ID when dismissed`() {
    readAloudNotificationManger.dismissNotification()

    verify {
      notificationManager.cancel(ReadAloudNotificationManager.Companion.READ_ALOUD_NOTIFICATION_ID)
    }
  }

  @Test
  fun `creates notification channel on Android O and above`() {
    buildNotification(false)

    verify { notificationManager.createNotificationChannel(any()) }
  }
}
