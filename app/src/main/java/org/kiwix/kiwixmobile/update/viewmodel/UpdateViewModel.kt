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
import javax.inject.Inject

@Suppress("all")
class UpdateViewModel @Inject constructor(
  private val downloadApkDao: DownloadApkDao,
  private val downloader: Downloader
) : ViewModel() {
  private val _state = mutableStateOf(UpdateStates())
  val state: State<UpdateStates> = _state

  init {
    fetchDownloadInfo()
  }

  private fun fetchDownloadInfo() = viewModelScope.launch {
    _state.value = _state.value.copy(
      loading = true
    )
    try {
      downloadApkDao.downloads().collect { download ->
        _state.value = UpdateStates(
          downloadApkState = DownloadApkState(download),
        )
      }
    } catch (e: Exception) {
      _state.value = _state.value.copy(
        loading = false,
        error = e.message ?: "Unknown error"
      )
    }
  }

  private fun downloadApk() {
    _state.value = _state.value.copy(
      loading = true
    )
    try {
      _state.value = _state.value.copy(
        loading = false
      )
      downloader.downloadApk(
        url = _state.value.downloadApkState.url
      )
    } catch (e: Exception) {
      _state.value = _state.value.copy(
        loading = false,
        error = e.message ?: "Unknown error"
      )
    }
  }

  private fun cancelDownload() {
    _state.value = _state.value.copy(
      loading = true
    )
    try {
      _state.value = _state.value.copy(
        loading = false
      )
      downloader.cancelDownload(_state.value.downloadApkState.downloadId)
    } catch (e: Exception) {
      _state.value = _state.value.copy(
        loading = false,
        error = e.message ?: "Unknown error"
      )
    }
  }

  /*  fun onDownloadComplete() {
      viewModelScope.launch {

        _state.value = _state.value.copy(
          installerIntent = installerIntent
        )
      }
    }*/

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
