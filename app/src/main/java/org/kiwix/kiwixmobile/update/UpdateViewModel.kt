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

package org.kiwix.kiwixmobile.update

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.dao.AppUpdateDao
import org.kiwix.kiwixmobile.core.dao.DownloadRoomDao
import org.kiwix.kiwixmobile.core.downloader.Downloader
import javax.inject.Inject

class UpdateViewModel @Inject constructor(
  private val appUpdateDao: AppUpdateDao,
  private val downloadRoomDao: DownloadRoomDao,
  private val downloader: Downloader
) : ViewModel() {
  private val _state = mutableStateOf(UpdateStates())
  val state: State<UpdateStates> = _state

  init {
    getLatestAppVersion()
  }

  private fun getLatestAppVersion() = viewModelScope.launch {
    appUpdateDao.getLatestAppUpdate().collect { latestAppVersion ->
      _state.value = _state.value.copy(
        apkVersion = AppVersion(
          apkUrl = latestAppVersion.url,
          name = latestAppVersion.name,
          version = latestAppVersion.version
        )
      )
    }
  }

  private fun downloadApp() {
    downloader.downloadApk("")
  }

  private fun cancelDownloadApp() = viewModelScope.launch {
    downloadRoomDao.getOngoingDownloads().forEach { downloads ->
      Log.d("TAG", "cancelDownloadApp: cancel download clicked")
      downloader.cancelDownload(downloads.downloadId)
    }
  }

  fun event(event: UpdateEvents) {
    when (event) {
      is UpdateEvents.DownloadApp -> {
        downloadApp()
      }

      is UpdateEvents.CancelDownload -> {
        cancelDownloadApp()
      }

      is UpdateEvents.RetrieveLatestAppVersion -> {
        getLatestAppVersion()
      }
    }
  }
}
