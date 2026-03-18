/*
 * Kiwix Android
 * Copyright (c) 2025 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.downloader.downloadManager

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class DownloadTimeoutDismissReceiverTest {
  private lateinit var receiver: DownloadTimeoutDismissReceiver
  private lateinit var context: Context
  private lateinit var notificationManager: NotificationManager

  @BeforeEach
  fun setUp() {
    receiver = DownloadTimeoutDismissReceiver()
    context = mockk(relaxed = true)
    notificationManager = mockk(relaxed = true)
  }

  @Test
  fun onReceiveWithMatchingActionCancelsNotification() {
    val intent = mockk<Intent>()
    every { intent.action } returns BACKGROUND_DOWNLOAD_LIMIT_REACH_ACTION
    every { context.getSystemService(Context.NOTIFICATION_SERVICE) } returns notificationManager
    receiver.onReceive(context, intent)
    verify { notificationManager.cancel(DOWNLOAD_TIMEOUT_LIMIT_REACH_NOTIFICATION_ID) }
  }

  @Test
  fun onReceiveWithWrongActionDoesNothing() {
    val intent = mockk<Intent>()
    every { intent.action } returns "wrong_action"
    every {
      context.getSystemService(Context.NOTIFICATION_SERVICE)
    } returns notificationManager
    receiver.onReceive(context, intent)
    verify(exactly = 0) {
      notificationManager.cancel(DOWNLOAD_TIMEOUT_LIMIT_REACH_NOTIFICATION_ID)
    }
  }

  @Test
  fun onReceiveWithNullIntentDoesNothing() {
    every {
      context.getSystemService(Context.NOTIFICATION_SERVICE)
    } returns notificationManager
    receiver.onReceive(context, null)
    verify(exactly = 0) {
      notificationManager.cancel(DOWNLOAD_TIMEOUT_LIMIT_REACH_NOTIFICATION_ID)
    }
  }

  @Test
  fun onIntentWithActionReceivedHandlesNullNotificationManager() {
    val intent = mockk<Intent>()
    every { intent.action } returns BACKGROUND_DOWNLOAD_LIMIT_REACH_ACTION
    every { context.getSystemService(Context.NOTIFICATION_SERVICE) } returns null
    assertDoesNotThrow {
      receiver.onIntentWithActionReceived(context, intent)
    }
    verify { context.getSystemService(Context.NOTIFICATION_SERVICE) }
    verify(exactly = 0) {
      notificationManager.cancel(DOWNLOAD_TIMEOUT_LIMIT_REACH_NOTIFICATION_ID)
    }
  }

  @Test
  fun onIntentWithActionReceivedHandlesWrongServiceType() {
    val intent = mockk<Intent>()
    every { context.getSystemService(Context.NOTIFICATION_SERVICE) } returns mockk<Any>()
    assertDoesNotThrow {
      receiver.onIntentWithActionReceived(context, intent)
    }
    verify { context.getSystemService(Context.NOTIFICATION_SERVICE) }
    verify(exactly = 0) {
      notificationManager.cancel(DOWNLOAD_TIMEOUT_LIMIT_REACH_NOTIFICATION_ID)
    }
  }
}
