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

import android.app.Application
import kotlinx.coroutines.flow.first
import org.kiwix.kiwixmobile.core.ui.components.ONE
import org.kiwix.kiwixmobile.core.utils.KiwixPermissionChecker
import org.kiwix.kiwixmobile.core.utils.NetworkUtils
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.zimManager.libraryView.AvailableSpaceCalculator
import org.kiwix.kiwixmobile.zimManager.libraryView.LibraryListItem
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.HandleBookDownloadUseCase.DownloadAction.RequestNotificationPermission
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.HandleBookDownloadUseCase.DownloadAction.NoInternet
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.HandleBookDownloadUseCase.DownloadAction.ShowWifiOnlyDialog
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.HandleBookDownloadUseCase.DownloadAction.RequestStoragePermission
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.HandleBookDownloadUseCase.DownloadAction.DisableStorageSelection
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.HandleBookDownloadUseCase.DownloadAction.ShowStorageSelection
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.HandleBookDownloadUseCase.DownloadAction.RequestManageExternalFilesPermission
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.HandleBookDownloadUseCase.DownloadAction.StartDownload
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.HandleBookDownloadUseCase.DownloadAction.NotEnoughSpace
import org.kiwix.kiwixmobile.zimManager.libraryView.AvailableSpaceCalculator.AvailableSpaceResult.NotEnoughSpaceForBook
import org.kiwix.kiwixmobile.zimManager.libraryView.AvailableSpaceCalculator.AvailableSpaceResult.HasAvailableSpaceForBook
import javax.inject.Inject

class HandleBookDownloadUseCase @Inject constructor(
  private val context: Application,
  private val kiwixDataStore: KiwixDataStore,
  private val permissionChecker: KiwixPermissionChecker,
  private val availableSpaceCalculator: AvailableSpaceCalculator
) {
  sealed class DownloadAction {
    object RequestNotificationPermission : DownloadAction()
    object RequestStoragePermission : DownloadAction()
    object RequestManageExternalFilesPermission : DownloadAction()
    object ShowWifiOnlyDialog : DownloadAction()
    object ShowStorageSelection : DownloadAction()
    data class NotEnoughSpace(val availableSpace: String) : DownloadAction()
    data class StartDownload(val item: LibraryListItem.BookItem) : DownloadAction()
    object NoInternet : DownloadAction()
    object DisableStorageSelection : DownloadAction()
  }

  suspend operator fun invoke(
    item: LibraryListItem.BookItem,
    storageDeviceCount: Int
  ): DownloadAction {
    if (!permissionChecker.hasNotificationPermission()) {
      return RequestNotificationPermission
    }
    if (!NetworkUtils.isNetworkAvailable(context)) {
      return NoInternet
    }
    if (kiwixDataStore.wifiOnly.first() && !NetworkUtils.isWiFi(context)) {
      return ShowWifiOnlyDialog
    }
    if (!permissionChecker.hasWriteExternalStoragePermission()) {
      return RequestStoragePermission
    }
    if (kiwixDataStore.showStorageOption.first()) {
      return if (storageDeviceCount > ONE) {
        ShowStorageSelection
      } else {
        DisableStorageSelection
      }
    }

    if (!permissionChecker.isManageExternalStoragePermissionGranted()) {
      return RequestManageExternalFilesPermission
    }
    return when (val result = availableSpaceCalculator.hasAvailableSpaceFor(item)) {
      is HasAvailableSpaceForBook -> StartDownload(result.bookItem)
      is NotEnoughSpaceForBook -> NotEnoughSpace(result.availableSpace)
    }
  }
}
