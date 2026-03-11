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

package org.kiwix.kiwixmobile.core.downloader

import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.NetworkType
import com.tonyodev.fetch2.Request
import com.tonyodev.fetch2core.Func
import io.mockk.Runs
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.dao.DownloadRoomDao
import org.kiwix.kiwixmobile.core.dao.entities.DownloadRoomEntity
import org.kiwix.kiwixmobile.core.downloader.downloadManager.DownloadManagerRequester
import org.kiwix.kiwixmobile.core.downloader.model.DownloadRequest
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.utils.AUTO_RETRY_MAX_ATTEMPTS
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore

class DownloadManagerRequesterTest {
  private lateinit var fetch: Fetch
  private lateinit var kiwixDataStore: KiwixDataStore
  private lateinit var context: CoreApp
  private lateinit var downloadRoomDao: DownloadRoomDao
  private lateinit var requester: DownloadManagerRequester
  private lateinit var mainActivity: CoreMainActivity
  private lateinit var dispatcher: TestDispatcher

  @BeforeEach
  fun setup() {
    fetch = mockk(relaxed = true)
    kiwixDataStore = mockk(relaxed = true)
    downloadRoomDao = mockk(relaxed = true)
    mainActivity = mockk(relaxed = true)
    context = mockk(relaxed = true)
    every { context.getMainActivity() } returns mainActivity
    every {
      mainActivity.startDownloadMonitorServiceIfOngoingDownloads()
    } just Runs
    every { kiwixDataStore.selectedStorage } returns flowOf("/storage/emulated/0")
    every { kiwixDataStore.wifiOnly } returns flowOf(false)
  }

  private fun createRequester(testScheduler: TestCoroutineScheduler) {
    dispatcher = StandardTestDispatcher(testScheduler)
    requester = DownloadManagerRequester(
      fetch,
      kiwixDataStore,
      context,
      downloadRoomDao,
      dispatcher
    )
  }

  @Test
  fun `cancel should call fetch delete`() = runTest {
    createRequester(testScheduler)
    val downloadId = 42L

    requester.cancel(downloadId)

    verify {
      fetch.delete(
        id = downloadId.toInt(),
        func = null,
        func2 = any<Func<Error>>()
      )
    }
    verify { mainActivity.startDownloadMonitorServiceIfOngoingDownloads() }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `cancel should remove from Room DB when Fetch returns error`() = runTest {
    createRequester(testScheduler)
    val downloadId = 42L
    val errorCallbackSlot = slot<Func<Error>>()
    every {
      fetch.delete(
        id = downloadId.toInt(),
        func = null,
        func2 = capture(errorCallbackSlot)
      )
    } answers {
      errorCallbackSlot.captured.call(Error.REQUEST_DOES_NOT_EXIST)
      fetch
    }

    requester.cancel(downloadId)
    advanceUntilIdle()
    verify { downloadRoomDao.deleteDownloadByDownloadId(downloadId) }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `retryDownload should call fetch retry`() = runTest {
    createRequester(testScheduler)
    val downloadId = 42L

    requester.retryDownload(downloadId)

    verify {
      fetch.retry(
        id = downloadId.toInt(),
        func = null,
        func2 = any<Func<Error>>()
      )
    }
    advanceUntilIdle()
    verify { mainActivity.startDownloadMonitorServiceIfOngoingDownloads() }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `retryDownload should re-enqueue when Fetch returns error`() = runTest {
    createRequester(testScheduler)
    val downloadId = 42L
    val errorCallbackSlot = slot<Func<Error>>()
    every {
      fetch.retry(
        id = downloadId.toInt(),
        func = null,
        func2 = capture(errorCallbackSlot)
      )
    } answers {
      errorCallbackSlot.captured.call(Error.REQUEST_DOES_NOT_EXIST)
      fetch
    }
    val staleEntity = mockk<DownloadRoomEntity>(relaxed = true)
    every { staleEntity.url } returns "https://example.com/test.zim"
    every { downloadRoomDao.getEntityForDownloadId(downloadId) } returns staleEntity

    requester.retryDownload(downloadId)
    advanceUntilIdle()
    verify { downloadRoomDao.getEntityForDownloadId(downloadId) }
    verify { downloadRoomDao.deleteDownloadByDownloadId(downloadId) }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `retryDownload should not re-enqueue when stale entity is null`() = runTest {
    createRequester(testScheduler)
    val downloadId = 42L
    val errorCallbackSlot = slot<Func<Error>>()
    every {
      fetch.retry(
        id = downloadId.toInt(),
        func = null,
        func2 = capture(errorCallbackSlot)
      )
    } answers {
      errorCallbackSlot.captured.call(Error.REQUEST_DOES_NOT_EXIST)
      fetch
    }
    every { downloadRoomDao.getEntityForDownloadId(downloadId) } returns null

    requester.retryDownload(downloadId)
    advanceUntilIdle()
    verify(exactly = 0) { downloadRoomDao.deleteDownloadByDownloadId(any<Long>()) }
  }

  @Test
  fun `pauseResumeDownload should call fetch resume when isPause is true`() = runTest {
    createRequester(testScheduler)
    val downloadId = 42L

    requester.pauseResumeDownload(downloadId, isPause = true)

    verify {
      fetch.resume(
        id = downloadId.toInt(),
        func = null,
        func2 = any<Func<Error>>()
      )
    }
    verify { mainActivity.startDownloadMonitorServiceIfOngoingDownloads() }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `pauseResumeDownload should re-enqueue when resuming a stale download`() = runTest {
    createRequester(testScheduler)
    val downloadId = 42L
    val url = "https://example.com/test.zim"
    val errorCallbackSlot = slot<Func<Error>>()
    every {
      fetch.resume(
        id = downloadId.toInt(),
        func = null,
        func2 = capture(errorCallbackSlot)
      )
    } answers {
      errorCallbackSlot.captured.call(Error.REQUEST_DOES_NOT_EXIST)
      fetch
    }
    val staleEntity = mockk<DownloadRoomEntity>(relaxed = true)
    every { staleEntity.url } returns url
    every { downloadRoomDao.getEntityForDownloadId(downloadId) } returns staleEntity

    requester.pauseResumeDownload(downloadId, isPause = true)

    advanceUntilIdle()

    verify { downloadRoomDao.getEntityForDownloadId(downloadId) }
    verify { downloadRoomDao.deleteDownloadByDownloadId(downloadId) }
    coVerify { downloadRoomDao.addIfDoesNotExist(eq(url), any(), requester) }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `pauseResumeDownload should call fetch pause when isPause is false`() = runTest {
    createRequester(testScheduler)
    val downloadId = 42L

    requester.pauseResumeDownload(downloadId, isPause = false)
    advanceUntilIdle()
    verify { fetch.pause(id = downloadId.toInt()) }
    verify { mainActivity.startDownloadMonitorServiceIfOngoingDownloads() }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `pauseResumeDownload should not re-enqueue when stale entity has null url`() = runTest {
    createRequester(testScheduler)
    val downloadId = 42L
    val errorCallbackSlot = slot<Func<Error>>()
    every {
      fetch.resume(
        id = downloadId.toInt(),
        func = null,
        func2 = capture(errorCallbackSlot)
      )
    } answers {
      errorCallbackSlot.captured.call(Error.REQUEST_DOES_NOT_EXIST)
      fetch
    }
    val staleEntity = mockk<DownloadRoomEntity>(relaxed = true)
    every { staleEntity.url } returns null
    every { downloadRoomDao.getEntityForDownloadId(downloadId) } returns staleEntity

    requester.pauseResumeDownload(downloadId, isPause = true)
    advanceUntilIdle()
    verify(exactly = 0) { downloadRoomDao.deleteDownloadByDownloadId(any<Long>()) }
  }

  @Test
  fun `enqueue should create fetch request with WiFi only config`() = runTest {
    createRequester(testScheduler)
    val downloadRequest = mockk<DownloadRequest>(relaxed = true)

    every { kiwixDataStore.wifiOnly } returns flowOf(true)

    val requestSlot = slot<Request>()
    every { fetch.enqueue(capture(requestSlot)) } returns fetch

    val id = requester.enqueue(downloadRequest)

    Assertions.assertEquals(requestSlot.captured.id.toLong(), id)
    Assertions.assertEquals(NetworkType.WIFI_ONLY, requestSlot.captured.networkType)
    Assertions.assertEquals(AUTO_RETRY_MAX_ATTEMPTS, requestSlot.captured.autoRetryMaxAttempts)
  }

  @Test
  fun `enqueue should create fetch request with all network config`() = runTest {
    createRequester(testScheduler)
    val downloadRequest = mockk<DownloadRequest>(relaxed = true)

    every { kiwixDataStore.wifiOnly } returns flowOf(false)

    val requestSlot = slot<Request>()
    every { fetch.enqueue(capture(requestSlot)) } returns fetch

    val id = requester.enqueue(downloadRequest)

    Assertions.assertEquals(requestSlot.captured.id.toLong(), id)
    Assertions.assertEquals(NetworkType.ALL, requestSlot.captured.networkType)
    Assertions.assertEquals(AUTO_RETRY_MAX_ATTEMPTS, requestSlot.captured.autoRetryMaxAttempts)
  }
}
