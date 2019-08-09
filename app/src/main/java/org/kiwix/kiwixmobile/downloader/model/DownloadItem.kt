/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
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
 */
package org.kiwix.kiwixmobile.downloader.model

data class DownloadItem(
  val downloadId: Long,
  val favIcon: Base64String,
  val title: String,
  val description: String,
  val bytesDownloaded: Long,
  val totalSizeBytes: Long,
  val downloadState: DownloadState
) {
  val progress get() = ((bytesDownloaded.toFloat() / totalSizeBytes) * 100).toInt()

  constructor(downloadStatus: DownloadStatus) : this(
    downloadStatus.downloadId,
    Base64String(downloadStatus.book.favicon),
    downloadStatus.title,
    downloadStatus.description,
    downloadStatus.bytesDownloadedSoFar,
    downloadStatus.totalSizeBytes,
    downloadStatus.state
  )
}
