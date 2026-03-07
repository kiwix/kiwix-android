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
  fun `onReceive with matching action cancels notification`() {
    val intent: Intent = mockk()
    every { intent.action } returns BACKGROUND_DOWNLOAD_LIMIT_REACH_ACTION
    every {
      context.getSystemService(Context.NOTIFICATION_SERVICE)
    } returns notificationManager

    receiver.onReceive(context, intent)

    verify { notificationManager.cancel(DOWNLOAD_TIMEOUT_LIMIT_REACH_NOTIFICATION_ID) }
  }

  @Test
  fun `onReceive with wrong action does nothing`() {
    val intent: Intent = mockk()
    every { intent.action } returns "some_other_action"

    receiver.onReceive(context, intent)

    verify(exactly = 0) { context.getSystemService(any<String>()) }
  }

  @Test
  fun `onReceive with null intent does nothing`() {
    receiver.onReceive(context, null)

    verify(exactly = 0) { context.getSystemService(any<String>()) }
  }

  @Test
  fun `onIntentWithActionReceived handles service unavailable`() {
    val intent: Intent = mockk()
    every { context.getSystemService(Context.NOTIFICATION_SERVICE) } returns null
    receiver.onIntentWithActionReceived(context, intent)
  }
}
