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

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.dao.DownloadApkDao
import org.kiwix.kiwixmobile.core.downloader.Downloader
import org.kiwix.kiwixmobile.core.entity.ApkInfo
import javax.inject.Inject

class UpdateViewModel @Inject constructor(
  private val downloadApkDao: DownloadApkDao,
  private val downloader: Downloader
) : ViewModel() {
  private val _state = mutableStateOf(UpdateStates())
  val state: State<UpdateStates> = _state

  init {
    fetchApkInfo()
  }

  private fun fetchApkInfo() = viewModelScope.launch {
    downloadApkDao.downloads().collect { download ->
      _state.value = UpdateStates(
        downloadApkState = DownloadApkState(download)
      )
    }
  }

  private fun downloadApk() {
    val apkUrl = _state.value.downloadApkState.url
    downloader.downloadApk(
      apkInfo = ApkInfo(
        name = _state.value.downloadApkState.name,
        version = _state.value.downloadApkState.version,
        apkUrl = apkUrl
      )
    )
  }

  private fun cancelDownload() {
    downloader.cancelDownload(_state.value.downloadApkState.downloadId)
  }

  fun event(event: UpdateEvents) {
    when (event) {
      is UpdateEvents.DownloadApk -> {
        downloadApk()
      }

      is UpdateEvents.CancelDownload -> {
        cancelDownload()
      }
    }
  }
}
