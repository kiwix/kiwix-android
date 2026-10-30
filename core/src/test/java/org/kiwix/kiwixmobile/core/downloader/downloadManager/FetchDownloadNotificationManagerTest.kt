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

package org.kiwix.kiwixmobile.core.downloader.downloadManager

import android.app.Notification
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import com.tonyodev.fetch2.DownloadNotification
import com.tonyodev.fetch2.Status
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.dao.DownloadRoomDao
import org.kiwix.kiwixmobile.core.utils.ACTIVE_DOWNLOAD_GROUP_KEY
import org.kiwix.kiwixmobile.core.utils.DOWNLOAD_NOTIFICATION_CHANNEL_ID
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])
class FetchDownloadNotificationManagerTest {
  private lateinit var context: Context
  private lateinit var fetchDownloadNotificationManager: FetchDownloadNotificationManager

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    // registerBroadcastReceiver() (called from the super constructor) reads
    // CoreApp.instance.applicationContext, so it must be populated before construction.
    val coreApp = mockk<CoreApp>()
    every { coreApp.applicationContext } returns context
    CoreApp.instance = coreApp
    fetchDownloadNotificationManager = FetchDownloadNotificationManager(
      context,
      mockk<DownloadRoomDao>(relaxed = true),
      UnconfinedTestDispatcher()
    )
  }

  private fun downloadNotification(status: Status, title: String) =
    DownloadNotification().apply {
      this.status = status
      this.title = title
    }

  private fun newBuilder() = NotificationCompat.Builder(context, DOWNLOAD_NOTIFICATION_CHANNEL_ID)

  @Test
  fun `returns false and leaves the builder untouched when there are no active downloads`() {
    val builder = newBuilder()
    val downloadNotifications = listOf(
      downloadNotification(Status.COMPLETED, "Completed book"),
      downloadNotification(Status.FAILED, "Failed book")
    )

    val result = fetchDownloadNotificationManager.updateGroupSummaryNotification(
      ZERO_GROUP_ID,
      builder,
      downloadNotifications,
      context
    )

    assertFalse(result)
    assertNull(builder.build().group)
  }

  @Test
  fun `returns false for an empty download list`() {
    val builder = newBuilder()

    val result = fetchDownloadNotificationManager.updateGroupSummaryNotification(
      ZERO_GROUP_ID,
      builder,
      emptyList(),
      context
    )

    assertFalse(result)
  }

  @Test
  fun `returns true and builds a singular summary for exactly one active download`() {
    val builder = newBuilder()
    val downloadNotifications = listOf(downloadNotification(Status.DOWNLOADING, "Wikipedia"))

    val result = fetchDownloadNotificationManager.updateGroupSummaryNotification(
      ZERO_GROUP_ID,
      builder,
      downloadNotifications,
      context
    )

    assertTrue(result)
    val notification = builder.build()
    assertEquals(ACTIVE_DOWNLOAD_GROUP_KEY, notification.group)
    assertTrue(NotificationCompat.isGroupSummary(notification))
    assertEquals(
      context.resources.getQuantityString(R.plurals.downloading_files_count, 1, 1),
      notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString()
    )
    assertEquals(
      listOf("Wikipedia"),
      notification.extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.map { it.toString() }
    )
  }

  @Test
  fun `returns true and counts downloading, paused and queued downloads together`() {
    val builder = newBuilder()
    val downloadNotifications = listOf(
      downloadNotification(Status.DOWNLOADING, "Downloading book"),
      downloadNotification(Status.PAUSED, "Paused book"),
      downloadNotification(Status.QUEUED, "Queued book")
    )

    val result = fetchDownloadNotificationManager.updateGroupSummaryNotification(
      ZERO_GROUP_ID,
      builder,
      downloadNotifications,
      context
    )

    assertTrue(result)
    val notification = builder.build()
    assertEquals(
      context.resources.getQuantityString(R.plurals.downloading_files_count, 3, 3),
      notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString()
    )
    assertEquals(
      setOf("Downloading book", "Paused book", "Queued book"),
      notification.extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        ?.map { it.toString() }
        ?.toSet()
    )
  }

  @Test
  fun `excludes completed and failed downloads from the count and the line items`() {
    val builder = newBuilder()
    val downloadNotifications = listOf(
      downloadNotification(Status.DOWNLOADING, "Downloading book"),
      downloadNotification(Status.COMPLETED, "Completed book"),
      downloadNotification(Status.FAILED, "Failed book")
    )

    val result = fetchDownloadNotificationManager.updateGroupSummaryNotification(
      ZERO_GROUP_ID,
      builder,
      downloadNotifications,
      context
    )

    assertTrue(result)
    val notification = builder.build()
    assertEquals(
      context.resources.getQuantityString(R.plurals.downloading_files_count, 1, 1),
      notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString()
    )
    assertEquals(
      listOf("Downloading book"),
      notification.extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.map { it.toString() }
    )
  }

  companion object {
    private const val ZERO_GROUP_ID = 0
  }
}
