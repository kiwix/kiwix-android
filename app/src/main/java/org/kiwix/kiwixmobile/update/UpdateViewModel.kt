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

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import org.kiwix.kiwixmobile.core.downloader.Downloader
import javax.inject.Inject

class UpdateViewModel @Inject constructor(
  // private val appUpdateDao: AppUpdateDao,
  private val downloader: Downloader
) : ViewModel() {
  private val _state = mutableStateOf(UpdateStates())
  val state: State<UpdateStates> = _state

  init {
    getLatestAppVersion()
  }

  private fun getLatestAppVersion() {
    /*val latestAppVersion: AppUpdateEntity = appUpdateDao.getLatestAppUpdate()
    _state.value = _state.value.copy(
      apkVersion = AppVersion(
        apkUrl = latestAppVersion.version,
        name = latestAppVersion.name,
        version = latestAppVersion.version
      )
    )*/
  }

  private fun downloadApp(url: String) {
    downloader.downloadApk(url)
  }

  fun event(event: UpdateEvents) {
    when (event) {
      is UpdateEvents.DownloadApp -> {
        downloadApp(event.url)
      }

      is UpdateEvents.CancelDownload -> {
        // downloader.cancelDownload()
      }

      is UpdateEvents.RetrieveLatestAppVersion -> {
        getLatestAppVersion()
      }
    }
  }
}
