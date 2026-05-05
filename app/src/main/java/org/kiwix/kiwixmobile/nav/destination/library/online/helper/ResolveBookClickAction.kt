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
import kotlinx.coroutines.flow.first
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.isNetworkAvailable
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.isWifi
import org.kiwix.kiwixmobile.core.downloader.model.DownloadState
import org.kiwix.kiwixmobile.core.ui.components.ONE
import org.kiwix.kiwixmobile.core.utils.KiwixPermissionChecker
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveBookClickAction.LibraryActionResult.CancelDownload
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveBookClickAction.LibraryActionResult.DisableStorageSelection
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveBookClickAction.LibraryActionResult.NoInternet
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveBookClickAction.LibraryActionResult.NotEnoughSpace
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveBookClickAction.LibraryActionResult.PauseResume
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveBookClickAction.LibraryActionResult.RequestManageExternalFilesPermission
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveBookClickAction.LibraryActionResult.RequestNotificationPermission
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveBookClickAction.LibraryActionResult.RequestStoragePermission
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveBookClickAction.LibraryActionResult.RetryDownload
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveBookClickAction.LibraryActionResult.ShowStorageSelection
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveBookClickAction.LibraryActionResult.ShowWifiOnlyDialog
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveBookClickAction.LibraryActionResult.StartDownload
import org.kiwix.kiwixmobile.zimManager.libraryView.AvailableSpaceCalculator
import org.kiwix.kiwixmobile.zimManager.libraryView.AvailableSpaceCalculator.AvailableSpaceResult.HasAvailableSpaceForBook
import org.kiwix.kiwixmobile.zimManager.libraryView.AvailableSpaceCalculator.AvailableSpaceResult.NotEnoughSpaceForBook
import org.kiwix.kiwixmobile.zimManager.libraryView.LibraryListItem.LibraryDownloadItem
import org.kiwix.kiwixmobile.zimManager.libraryView.LibraryListItem.BookItem
import javax.inject.Inject

class ResolveBookClickAction @Inject constructor(
  private val kiwixDataStore: KiwixDataStore,
  private val permissionChecker: KiwixPermissionChecker,
  private val availableSpaceCalculator: AvailableSpaceCalculator,
  private val connectivityManager: ConnectivityManager
) {
  sealed class LibraryActionResult {
    object RequestNotificationPermission : LibraryActionResult()
    object RequestStoragePermission : LibraryActionResult()
    object RequestManageExternalFilesPermission : LibraryActionResult()
    object ShowWifiOnlyDialog : LibraryActionResult()
    object ShowStorageSelection : LibraryActionResult()
    data class NotEnoughSpace(val availableSpace: String) : LibraryActionResult()
    data class StartDownload(val item: BookItem) : LibraryActionResult()
    object NoInternet : LibraryActionResult()
    object DisableStorageSelection : LibraryActionResult()
    data class PauseResume(val downloadId: Long, val isPaused: Boolean) : LibraryActionResult()
    data class RetryDownload(val downloadId: Long) : LibraryActionResult()
    data class CancelDownload(val downloadId: Long) : LibraryActionResult()
  }

  suspend fun onBookItemClick(
    item: BookItem,
    storageDeviceCount: Int
  ): LibraryActionResult {
    return if (!permissionChecker.hasNotificationPermission()) {
      RequestNotificationPermission
    } else if (!connectivityManager.isNetworkAvailable()) {
      NoInternet
    } else if (kiwixDataStore.wifiOnly.first() && !connectivityManager.isWifi()) {
      ShowWifiOnlyDialog
    } else if (!permissionChecker.hasWriteExternalStoragePermission()) {
      return RequestStoragePermission
    } else if (kiwixDataStore.showStorageOption.first()) {
      if (storageDeviceCount > ONE) {
        ShowStorageSelection
      } else {
        DisableStorageSelection
      }
    } else if (!permissionChecker.isManageExternalStoragePermissionGranted()) {
      RequestManageExternalFilesPermission
    } else {
      when (val result = availableSpaceCalculator.hasAvailableSpaceFor(item)) {
        is HasAvailableSpaceForBook -> StartDownload(result.bookItem)
        is NotEnoughSpaceForBook -> NotEnoughSpace(result.availableSpace)
      }
    }
  }

  fun onPauseResumeButtonClick(item: LibraryDownloadItem): LibraryActionResult {
    return if (!connectivityManager.isNetworkAvailable()) {
      NoInternet
    } else {
      val isPaused = item.downloadState == DownloadState.Paused
      PauseResume(item.downloadId, isPaused)
    }
  }

  fun onStopButtonClick(item: LibraryDownloadItem): LibraryActionResult {
    return if (item.currentDownloadState == Status.FAILED) {
      when (item.downloadError) {
        Error.UNKNOWN_IO_ERROR,
        Error.CONNECTION_TIMED_OUT,
        Error.UNKNOWN -> {
          if (!connectivityManager.isNetworkAvailable()) {
            NoInternet
          } else {
            RetryDownload(item.downloadId)
          }
        }

        else -> CancelDownload(item.downloadId)
      }
    } else {
      CancelDownload(item.downloadId)
    }
  }
}
