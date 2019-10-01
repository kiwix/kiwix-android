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
package org.kiwix.kiwixmobile.core.downloader.model

import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.Status
import com.tonyodev.fetch2.Status.ADDED
import com.tonyodev.fetch2.Status.CANCELLED
import com.tonyodev.fetch2.Status.COMPLETED
import com.tonyodev.fetch2.Status.DELETED
import com.tonyodev.fetch2.Status.DOWNLOADING
import com.tonyodev.fetch2.Status.FAILED
import com.tonyodev.fetch2.Status.NONE
import com.tonyodev.fetch2.Status.PAUSED
import com.tonyodev.fetch2.Status.QUEUED
import com.tonyodev.fetch2.Status.REMOVED
import org.kiwix.kiwixmobile.core.R

data class DownloadItem(
  val downloadId: Long,
  val favIcon: Base64String,
  val title: String,
  val description: String,
  val bytesDownloaded: Long,
  val totalSizeBytes: Long,
  val progress: Int,
  val eta: Seconds,
  val downloadState: DownloadState
) {

  constructor(downloadModel: DownloadModel) : this(
    downloadModel.downloadId,
    Base64String(downloadModel.book.favicon),
    downloadModel.book.title,
    downloadModel.book.description,
    downloadModel.bytesDownloaded,
    downloadModel.totalSizeOfDownload,
    downloadModel.progress,
    Seconds(downloadModel.etaInMilliSeconds / 1000L),
    DownloadState.from(
      downloadModel.state,
      downloadModel.error
    )
  )
}

sealed class DownloadState(val stringId: Int) {

  companion object {
    fun from(state: Status, error: Error): DownloadState =
      when (state) {
        NONE,
        ADDED,
        QUEUED -> Pending
        DOWNLOADING -> Running
        PAUSED -> Paused
        COMPLETED -> Successful
        CANCELLED,
        FAILED,
        REMOVED,
        DELETED -> Failed(error)
      }
  }

  object Pending : DownloadState(R.string.pending_state)
  object Running : DownloadState(R.string.running_state)
  object Successful : DownloadState(R.string.successful_state)
  object Paused : DownloadState(R.string.paused_state)
  data class Failed(val reason: Error) : DownloadState(R.string.failed_state)

  override fun toString(): String = javaClass.simpleName
}
