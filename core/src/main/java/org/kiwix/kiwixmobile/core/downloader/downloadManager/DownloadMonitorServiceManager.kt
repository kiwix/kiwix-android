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

package org.kiwix.kiwixmobile.core.downloader.downloadManager

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.dao.DownloadRoomDao
import org.kiwix.kiwixmobile.core.di.IoDispatcher
import org.kiwix.kiwixmobile.core.downloader.downloadManager.DownloadMonitorService.Companion.STOP_DOWNLOAD_SERVICE
import org.kiwix.kiwixmobile.core.downloader.downloadManager.DownloadMonitorService.Companion.isDownloadMonitorServiceRunning
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadMonitorServiceManager @Inject constructor(
  @param:ApplicationContext private val context: Context,
  private val downloadRoomDao: DownloadRoomDao,
  @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
  /**
   * Starts the [DownloadMonitorService] if there are any ongoing downloads.
   *
   * This method checks whether the download monitoring service is already running.
   * If not, it queries the database for ongoing downloads on a background thread.
   * When at least one active download is found, the service is started with the
   * required app metadata. If no ongoing downloads exist, the service is stopped
   * to avoid unnecessary background work.
   */
  fun startDownloadMonitorServiceIfOngoingDownloads(isAppStart: Boolean = false) {
    if (!isDownloadMonitorServiceRunning) {
      CoroutineScope(ioDispatcher).launch {
        runCatching {
          if (downloadRoomDao.getOngoingDownloads().isNotEmpty() || !isAppStart) {
            context.startService(
              Intent(
                context,
                DownloadMonitorService::class.java
              )
            )
          } else {
            stopDownloadServiceIfRunning()
          }
        }
      }
    }
  }

  /**
   * Stops the DownloadService if it is currently running,
   * as the application is now in the foreground and can handle downloads directly.
   */
  private fun stopDownloadServiceIfRunning() {
    if (isDownloadMonitorServiceRunning) {
      context.startService(
        Intent(
          context,
          DownloadMonitorService::class.java
        ).setAction(STOP_DOWNLOAD_SERVICE)
      )
    }
  }
}
