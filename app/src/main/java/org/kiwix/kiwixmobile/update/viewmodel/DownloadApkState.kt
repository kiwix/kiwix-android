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

import com.tonyodev.fetch2.Status
import org.kiwix.kiwixmobile.core.downloader.model.DownloadApkModel
import org.kiwix.kiwixmobile.core.downloader.model.Seconds

data class DownloadApkState(
  val downloadId: Long = 0,
  val name: String = "",
  val version: String = "",
  val url: String = "",
  val bytesDownloaded: Long = 0,
  val totalSizeBytes: Long = 0,
  val progress: Int = 0,
  val eta: Seconds = Seconds(0),
  val currentDownloadState: Status = Status.NONE,
  val downloadError: com.tonyodev.fetch2.Error = com.tonyodev.fetch2.Error.NONE
) {
  val readableEta: CharSequence = eta.takeIf { it.seconds > 0L }?.toHumanReadableTime().orEmpty()

  constructor(downloadModel: DownloadApkModel) : this(
    downloadModel.downloadId,
    downloadModel.name,
    downloadModel.version,
    downloadModel.url,
    downloadModel.bytesDownloaded,
    downloadModel.totalSizeOfDownload,
    downloadModel.progress,
    Seconds(downloadModel.etaInMilliSeconds),
    downloadModel.state,
    downloadModel.error
  )
}
