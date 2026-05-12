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

package org.kiwix.kiwixmobile.nav.destination.library.online.helper

import android.net.ConnectivityManager
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.Status
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.isNetworkAvailable
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.isWifi
import org.kiwix.kiwixmobile.core.downloader.model.DownloadState
import org.kiwix.kiwixmobile.core.utils.KiwixPermissionChecker
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.zimManager.libraryView.AvailableSpaceCalculator.AvailableSpaceResult.HasAvailableSpaceForBook
import org.kiwix.kiwixmobile.zimManager.libraryView.AvailableSpaceCalculator.AvailableSpaceResult.NotEnoughSpaceForBook
import org.kiwix.kiwixmobile.zimManager.libraryView.LibraryListItem.BookItem
import org.kiwix.kiwixmobile.zimManager.libraryView.LibraryListItem.LibraryDownloadItem
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveBookClickAction.LibraryActionResult
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveBookClickAction.LibraryActionResult.CancelDownload
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveBookClickAction.LibraryActionResult.NoInternet
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveBookClickAction.LibraryActionResult.RetryDownload
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveBookClickAction.LibraryActionResult.PauseResume
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveBookClickAction.LibraryActionResult.RequestNotificationPermission
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveBookClickAction.LibraryActionResult.ShowWifiOnlyDialog
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveBookClickAction.LibraryActionResult.RequestStoragePermission
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveBookClickAction.LibraryActionResult.ShowStorageSelection
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveBookClickAction.LibraryActionResult.DisableStorageSelection
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveBookClickAction.LibraryActionResult.RequestManageExternalFilesPermission
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveBookClickAction.LibraryActionResult.StartDownload
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveBookClickAction.LibraryActionResult.NotEnoughSpace
import org.kiwix.kiwixmobile.zimManager.libraryView.AvailableSpaceCalculator

@OptIn(ExperimentalCoroutinesApi::class)
class ResolveBookClickActionTest {
  private val kiwixDataStore: KiwixDataStore = mockk()
  private val permissionChecker: KiwixPermissionChecker = mockk()
  private val availableSpaceCalculator: AvailableSpaceCalculator = mockk()
  private val connectivityManager: ConnectivityManager = mockk()
  private val bookItem = mockk<BookItem>(relaxed = true)

  private lateinit var resolver: ResolveBookClickAction

  @BeforeEach
  fun setup() {
    resolver = ResolveBookClickAction(
      kiwixDataStore,
      permissionChecker,
      availableSpaceCalculator,
      connectivityManager
    )
  }

  @Test
  fun `returns RequestNotificationPermission when notification permission not granted`() = runTest {
    coEvery { permissionChecker.hasNotificationPermission() } returns false

    val result = resolver.onBookItemClick(bookItem, 1)

    assertEquals(RequestNotificationPermission, result)
  }

  @Test
  fun `returns NoInternet when network unavailable`() = runTest {
    coEvery { permissionChecker.hasNotificationPermission() } returns true
    every { connectivityManager.isNetworkAvailable() } returns false

    val result = resolver.onBookItemClick(bookItem, 1)

    assertEquals(NoInternet, result)
  }

  @Test
  fun `returns ShowWifiOnlyDialog when wifiOnly enabled and not on wifi`() = runTest {
    coEvery { permissionChecker.hasNotificationPermission() } returns true
    every { connectivityManager.isNetworkAvailable() } returns true
    every { connectivityManager.isWifi() } returns false
    every { kiwixDataStore.wifiOnly } returns MutableStateFlow(true)

    val result = resolver.onBookItemClick(bookItem, 1)

    assertEquals(ShowWifiOnlyDialog, result)
  }

  @Test
  fun `returns RequestStoragePermission when storage permission not granted`() = runTest {
    coEvery { permissionChecker.hasNotificationPermission() } returns true
    every { connectivityManager.isNetworkAvailable() } returns true
    every { connectivityManager.isWifi() } returns true
    every { kiwixDataStore.wifiOnly } returns MutableStateFlow(false)
    coEvery { permissionChecker.hasWriteExternalStoragePermission() } returns false

    val result = resolver.onBookItemClick(bookItem, 1)

    assertEquals(RequestStoragePermission, result)
  }

  @Test
  fun `returns ShowStorageSelection when multiple devices and option enabled`() = runTest {
    coEvery { permissionChecker.hasNotificationPermission() } returns true
    every { connectivityManager.isNetworkAvailable() } returns true
    every { connectivityManager.isWifi() } returns true
    every { kiwixDataStore.wifiOnly } returns MutableStateFlow(false)
    coEvery { permissionChecker.hasWriteExternalStoragePermission() } returns true
    every { kiwixDataStore.showStorageOption } returns MutableStateFlow(true)

    val result = resolver.onBookItemClick(bookItem, 2)

    assertEquals(ShowStorageSelection, result)
  }

  @Test
  fun `returns DisableStorageSelection when single device and option enabled`() = runTest {
    coEvery { permissionChecker.hasNotificationPermission() } returns true
    every { connectivityManager.isNetworkAvailable() } returns true
    every { connectivityManager.isWifi() } returns true
    every { kiwixDataStore.wifiOnly } returns MutableStateFlow(false)
    coEvery { permissionChecker.hasWriteExternalStoragePermission() } returns true
    every { kiwixDataStore.showStorageOption } returns MutableStateFlow(true)

    val result = resolver.onBookItemClick(bookItem, 1)

    assertEquals(DisableStorageSelection, result)
  }

  @Test
  fun `returns RequestManageExternalFilesPermission when not granted`() = runTest {
    coEvery { permissionChecker.hasNotificationPermission() } returns true
    every { connectivityManager.isNetworkAvailable() } returns true
    every { connectivityManager.isWifi() } returns true
    every { kiwixDataStore.wifiOnly } returns MutableStateFlow(false)
    coEvery { permissionChecker.hasWriteExternalStoragePermission() } returns true
    every { kiwixDataStore.showStorageOption } returns MutableStateFlow(false)
    coEvery { permissionChecker.isManageExternalStoragePermissionGranted() } returns false

    val result = resolver.onBookItemClick(bookItem, 1)

    assertEquals(RequestManageExternalFilesPermission, result)
  }

  @Test
  fun `returns StartDownload when enough space`() = runTest {
    coEvery { permissionChecker.hasNotificationPermission() } returns true
    every { connectivityManager.isNetworkAvailable() } returns true
    every { connectivityManager.isWifi() } returns true
    every { kiwixDataStore.wifiOnly } returns MutableStateFlow(false)
    coEvery { permissionChecker.hasWriteExternalStoragePermission() } returns true
    every { kiwixDataStore.showStorageOption } returns MutableStateFlow(false)
    coEvery { permissionChecker.isManageExternalStoragePermissionGranted() } returns true

    val availableSpace = HasAvailableSpaceForBook(bookItem)

    coEvery { availableSpaceCalculator.hasAvailableSpaceFor(bookItem) } returns availableSpace

    val result = resolver.onBookItemClick(bookItem, 1)

    assertEquals(StartDownload(bookItem), result)
  }

  @Test
  fun `returns NotEnoughSpace when insufficient storage`() = runTest {
    coEvery { permissionChecker.hasNotificationPermission() } returns true
    every { connectivityManager.isNetworkAvailable() } returns true
    every { connectivityManager.isWifi() } returns true
    every { kiwixDataStore.wifiOnly } returns MutableStateFlow(false)
    coEvery { permissionChecker.hasWriteExternalStoragePermission() } returns true
    every { kiwixDataStore.showStorageOption } returns MutableStateFlow(false)
    coEvery { permissionChecker.isManageExternalStoragePermissionGranted() } returns true

    val availableSpace = NotEnoughSpaceForBook("100MB")

    coEvery { availableSpaceCalculator.hasAvailableSpaceFor(bookItem) } returns availableSpace

    val result = resolver.onBookItemClick(bookItem, 1)

    assertEquals(NotEnoughSpace("100MB"), result)
  }

  @Test
  fun `pause resume returns NoInternet when offline`() {
    every { connectivityManager.isNetworkAvailable() } returns false

    val item = mockk<LibraryDownloadItem>()

    val result = resolver.onPauseResumeButtonClick(item)

    assertEquals(NoInternet, result)
  }

  @Test
  fun `pause resume returns correct state`() {
    every { connectivityManager.isNetworkAvailable() } returns true

    val item = mockk<LibraryDownloadItem> {
      every { downloadState } returns DownloadState.Paused
      every { downloadId } returns 1L
    }

    val result = resolver.onPauseResumeButtonClick(item)

    assertEquals(PauseResume(1L, true), result)

    val pausedItem = mockk<LibraryDownloadItem> {
      every { downloadState } returns DownloadState.Pending
      every { downloadId } returns 1L
    }

    val result1 = resolver.onPauseResumeButtonClick(pausedItem)

    assertEquals(PauseResume(1L, false), result1)
  }

  @Test
  fun `test onStopButtonClick`() {
    testRetryDownload(true, RetryDownload(1L), Status.FAILED, Error.UNKNOWN)
    testRetryDownload(true, RetryDownload(1L), Status.FAILED, Error.CONNECTION_TIMED_OUT)
    testRetryDownload(true, RetryDownload(1L), Status.FAILED, Error.UNKNOWN_IO_ERROR)
    testRetryDownload(false, NoInternet, Status.FAILED, Error.UNKNOWN)
    testRetryDownload(false, NoInternet, Status.FAILED, Error.CONNECTION_TIMED_OUT)
    testRetryDownload(false, NoInternet, Status.FAILED, Error.UNKNOWN_IO_ERROR)
    testRetryDownload(
      true,
      CancelDownload(downloadId = 1),
      Status.FAILED,
      Error.REQUEST_DOES_NOT_EXIST
    )
    testRetryDownload(true, CancelDownload(downloadId = 1), Status.DOWNLOADING, Error.NONE)
  }

  private fun testRetryDownload(
    isInternetAvailable: Boolean,
    libraryActionResult: LibraryActionResult,
    currentDownloadStatus: Status,
    error: Error
  ) {
    every { connectivityManager.isNetworkAvailable() } returns isInternetAvailable
    val item = mockk<LibraryDownloadItem> {
      every { currentDownloadState } returns currentDownloadStatus
      every { downloadError } returns error
      every { downloadId } returns 1L
    }

    val result = resolver.onStopButtonClick(item)
    assertEquals(libraryActionResult, result)
  }
}
