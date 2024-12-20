/*
 * Kiwix Android
 * Copyright (c) 2024 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.downloader.downloadManager

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import org.kiwix.kiwixmobile.core.downloader.DownloadMonitor
import org.kiwix.kiwixmobile.core.extensions.isServiceRunning
import javax.inject.Inject

const val ZERO = 0
const val FIVE = 5
const val HUNDERED = 100
const val DEFAULT_INT_VALUE = -1

@SuppressLint("CheckResult")
class DownloadManagerMonitor @Inject constructor(
  val context: Context
) : DownloadMonitor {

  init {
    startMonitoringDownloads()
  }

  /**
   * Starts monitoring the downloads by ensuring that the `DownloadMonitorService` is running.
   * This service keeps the Fetch instance alive when the application is in the background
   *  or has been killed by the user or system, allowing downloads to continue in the background.
   */
  fun startMonitoringDownloads() {
    if (!context.isServiceRunning(DownloadMonitorService::class.java)) {
      context.startService(Intent(context, DownloadMonitorService::class.java)).also {
        Log.e("DOWNLOAD_MANAGER_MONITOR", "Starting DownloadMonitorService")
      }
    }
  }

  override fun init() {
    // empty method to so class does not get reported unused
  }
}
