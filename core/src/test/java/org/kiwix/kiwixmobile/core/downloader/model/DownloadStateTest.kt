/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.downloader.model

import android.content.Context
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.Status
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.R

class DownloadStateTest {
  @Test
  fun `from returns Pending for NONE status`() {
    val state = DownloadState.from(Status.NONE, Error.NONE, null)
    assertThat(state).isEqualTo(DownloadState.Pending)
  }

  @Test
  fun `from returns Pending for ADDED status`() {
    val state = DownloadState.from(Status.ADDED, Error.NONE, null)
    assertThat(state).isEqualTo(DownloadState.Pending)
  }

  @Test
  fun `from returns Pending for QUEUED status`() {
    val state = DownloadState.from(Status.QUEUED, Error.NONE, null)
    assertThat(state).isEqualTo(DownloadState.Pending)
  }

  @Test
  fun `from returns Running for DOWNLOADING status`() {
    val state = DownloadState.from(Status.DOWNLOADING, Error.NONE, null)
    assertThat(state).isEqualTo(DownloadState.Running)
  }

  @Test
  fun `from returns Paused for PAUSED status`() {
    val state = DownloadState.from(Status.PAUSED, Error.NONE, null)
    assertThat(state).isEqualTo(DownloadState.Paused)
  }

  @Test
  fun `from returns Successful for COMPLETED status`() {
    val state = DownloadState.from(Status.COMPLETED, Error.NONE, null)
    assertThat(state).isEqualTo(DownloadState.Successful)
  }

  @Test
  fun `from returns Failed for CANCELLED status`() {
    val error = Error.UNKNOWN
    val zimUrl = "http://example.com/test.zim"
    val state = DownloadState.from(Status.CANCELLED, error, zimUrl)
    assertThat(state).isInstanceOf(DownloadState.Failed::class.java)
    val failed = state as DownloadState.Failed
    assertThat(failed.reason).isEqualTo(error)
    assertThat(failed.zimUrl).isEqualTo(zimUrl)
  }

  @Test
  fun `from returns Failed for FAILED status`() {
    val error = Error.CONNECTION_TIMED_OUT
    val state = DownloadState.from(Status.FAILED, error, "http://example.com/test.zim")
    assertThat(state).isInstanceOf(DownloadState.Failed::class.java)
    assertThat((state as DownloadState.Failed).reason).isEqualTo(error)
  }

  @Test
  fun `from returns Failed for REMOVED status`() {
    val state = DownloadState.from(Status.REMOVED, Error.NONE, null)
    assertThat(state).isInstanceOf(DownloadState.Failed::class.java)
  }

  @Test
  fun `from returns Failed for DELETED status`() {
    val state = DownloadState.from(Status.DELETED, Error.NONE, null)
    assertThat(state).isInstanceOf(DownloadState.Failed::class.java)
  }

  @Test
  fun `Failed state stores zimUrl`() {
    val zimUrl = "http://example.com/wiki.zim"
    val state = DownloadState.from(Status.FAILED, Error.UNKNOWN, zimUrl)
    assertThat(state.zimUrl).isEqualTo(zimUrl)
  }

  @Test
  fun `Failed state with null zimUrl`() {
    val state = DownloadState.from(Status.FAILED, Error.UNKNOWN, null)
    assertThat(state.zimUrl).isNull()
  }

  @Test
  fun `toString returns Pending for Pending state`() {
    assertThat(DownloadState.Pending.toString()).isEqualTo("Pending")
  }

  @Test
  fun `toString returns Running for Running state`() {
    assertThat(DownloadState.Running.toString()).isEqualTo("Running")
  }

  @Test
  fun `toString returns Successful for Successful state`() {
    assertThat(DownloadState.Successful.toString()).isEqualTo("Successful")
  }

  @Test
  fun `toString returns Paused for Paused state`() {
    assertThat(DownloadState.Paused.toString()).isEqualTo("Paused")
  }

  @Test
  fun `toString returns Failed for Failed state`() {
    val failed = DownloadState.Failed(Error.UNKNOWN, null)
    assertThat(failed.toString()).isEqualTo("Failed(reason=UNKNOWN, zimUrl=null)")
  }

  @Test
  fun `toReadableState returns correct string for Pending`() {
    val context = mockk<Context>()
    every { context.getString(R.string.pending_state) } returns "Pending"
    assertThat(DownloadState.Pending.toReadableState(context)).isEqualTo("Pending")
  }

  @Test
  fun `toReadableState returns correct string for Running`() {
    val context = mockk<Context>()
    every { context.getString(R.string.running_state) } returns "Downloading"
    assertThat(DownloadState.Running.toReadableState(context)).isEqualTo("Downloading")
  }

  @Test
  fun `toReadableState returns correct string for Paused`() {
    val context = mockk<Context>()
    every { context.getString(R.string.paused_state) } returns "Paused"
    assertThat(DownloadState.Paused.toReadableState(context)).isEqualTo("Paused")
  }

  @Test
  fun `toReadableState returns correct string for Successful`() {
    val context = mockk<Context>()
    every { context.getString(R.string.complete) } returns "Complete"
    assertThat(DownloadState.Successful.toReadableState(context)).isEqualTo("Complete")
  }

  @Test
  fun `toReadableState returns correct string for Failed`() {
    val context = mockk<Context>()
    val failed = DownloadState.Failed(Error.CONNECTION_TIMED_OUT, null)
    every { context.getString(R.string.failed_state, "CONNECTION_TIMED_OUT") } returns
      "Failed: CONNECTION_TIMED_OUT"
    assertThat(failed.toReadableState(context)).isEqualTo("Failed: CONNECTION_TIMED_OUT")
  }
}
