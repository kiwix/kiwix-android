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

package org.kiwix.kiwixmobile.update.viewmodel

import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.Status
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.kiwix.kiwixmobile.core.dao.DownloadApkDao
import org.kiwix.kiwixmobile.core.downloader.Downloader
import org.kiwix.kiwixmobile.core.downloader.model.DownloadApkModel
import org.kiwix.sharedFunctions.InstantExecutorExtension

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(InstantExecutorExtension::class)
class UpdateViewModelTest {
  private val downloadApkDao: DownloadApkDao = mockk()
  private val downloader: Downloader = mockk()
  private val testDispatcher = StandardTestDispatcher()

  private lateinit var downloads: MutableSharedFlow<DownloadApkModel>
  private lateinit var viewModel: UpdateViewModel

  @BeforeEach
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    clearAllMocks()
    downloads = MutableSharedFlow(replay = 1)
    every { downloadApkDao.downloads() } returns downloads
    every { downloader.downloadApk(any()) } just Runs
    every { downloader.retryDownload(any()) } just Runs
    every { downloader.cancelApkDownload(any()) } just Runs
    coEvery { downloadApkDao.resetDownloadInfoState() } just Runs
  }

  @AfterEach
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `fetchDownloadInfo sets loading true while waiting for the first emission`() = runTest {
    createViewModel()
    runCurrent()
    assertThat(viewModel.state.value.loading).isTrue()
    assertThat(viewModel.state.value.downloadApkItem).isEqualTo(DownloadApkItem())
  }

  @Test
  fun `fetchDownloadInfo updates state from dao emissions`() = runTest {
    val download = downloadApkModel(
      name = "Kiwix Android 1.0.0",
      version = "1.0.0",
      url = APK_URL,
      downloadId = 12L,
      progress = 45,
      state = Status.DOWNLOADING
    )
    createViewModel()
    runCurrent()
    downloads.emit(download)
    advanceUntilIdle()
    assertThat(viewModel.state.value.loading).isFalse()
    assertThat(viewModel.state.value.downloadApkItem).isEqualTo(DownloadApkItem(download))
  }

  @Test
  fun `downloadApk uses the current apk url from state`() = runTest {
    val download = downloadApkModel(url = APK_URL)
    createViewModel()
    runCurrent()
    downloads.emit(download)
    advanceUntilIdle()
    viewModel.downloadApk()
    verify { downloader.downloadApk(APK_URL) }
  }

  @Test
  fun `retryDownload uses the current download id from state`() = runTest {
    val download = downloadApkModel(downloadId = 45L)
    createViewModel()
    runCurrent()
    downloads.emit(download)
    advanceUntilIdle()
    viewModel.retryDownload()
    verify { downloader.retryDownload(45L) }
  }

  @Test
  fun `cancelDownload cancels the current apk download and resets db state`() = runTest {
    val download = downloadApkModel(downloadId = 72L)
    createViewModel()
    runCurrent()
    downloads.emit(download)
    advanceUntilIdle()
    viewModel.cancelDownload()
    advanceUntilIdle()
    verify { downloader.cancelApkDownload(72L) }
    coVerify { downloadApkDao.resetDownloadInfoState() }
  }

  private fun createViewModel(downloadFlow: Flow<DownloadApkModel> = downloads) {
    every { downloadApkDao.downloads() } returns downloadFlow
    viewModel = UpdateViewModel(downloadApkDao, downloader)
  }

  private fun downloadApkModel(
    name: String = "Kiwix Android 2.0.0",
    version: String = "2.0.0",
    url: String = APK_URL,
    downloadId: Long = 1L,
    progress: Int = 0,
    state: Status = Status.NONE,
    error: Error = Error.NONE,
  ) = DownloadApkModel(
    databaseId = 1,
    name = name,
    version = version,
    url = url,
    downloadId = downloadId,
    file = "/storage/emulated/0/Download/kiwix.apk",
    etaInMilliSeconds = 0L,
    bytesDownloaded = 512L,
    totalSizeOfDownload = 1024L,
    progress = progress,
    state = state,
    error = error,
  )

  private companion object {
    const val APK_URL = "https://download.kiwix.org/kiwix.apk"
  }
}
