/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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
package org.kiwix.kiwixmobile.core.downloader.model

import android.content.Context
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.downloader.downloadManager.Error
import org.kiwix.kiwixmobile.core.downloader.downloadManager.Status
import org.kiwix.kiwixmobile.core.downloader.downloadManager.Status.ADDED
import org.kiwix.kiwixmobile.core.downloader.downloadManager.Status.CANCELLED
import org.kiwix.kiwixmobile.core.downloader.downloadManager.Status.COMPLETED
import org.kiwix.kiwixmobile.core.downloader.downloadManager.Status.DELETED
import org.kiwix.kiwixmobile.core.downloader.downloadManager.Status.DOWNLOADING
import org.kiwix.kiwixmobile.core.downloader.downloadManager.Status.FAILED
import org.kiwix.kiwixmobile.core.downloader.downloadManager.Status.NONE
import org.kiwix.kiwixmobile.core.downloader.downloadManager.Status.PAUSED
import org.kiwix.kiwixmobile.core.downloader.downloadManager.Status.QUEUED
import org.kiwix.kiwixmobile.core.downloader.downloadManager.Status.REMOVED

data class DownloadItem(
  val downloadId: Long,
  val favIcon: Base64String,
  val title: String,
  val description: String?,
  val bytesDownloaded: Long,
  val totalSizeBytes: Long,
  val progress: Int,
  val eta: Seconds,
  val downloadState: DownloadState
) {

  val readableEta: CharSequence = eta.takeIf { it.seconds > 0L }?.toHumanReadableTime() ?: ""

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
      downloadModel.error,
      downloadModel.book.url
    )
  )
}

sealed class DownloadState(
  private val stringId: Int,
  open val zimUrl: String? = null
) {

  companion object {
    fun from(state: Status, error: Error, zimUrl: String?): DownloadState =
      when (state) {
        NONE,
        ADDED,
        QUEUED -> Pending

        DOWNLOADING -> Running
        PAUSED -> Paused(error)
        COMPLETED -> Successful
        CANCELLED,
        FAILED,
        REMOVED,
        DELETED -> Failed(error, zimUrl)
      }
  }

  object Pending : DownloadState(R.string.pending_state)
  object Running : DownloadState(R.string.running_state)
  object Successful : DownloadState(R.string.complete)
  data class Paused(val reason: Error) : DownloadState(R.string.paused_state)
  data class Failed(val reason: Error, override val zimUrl: String?) :
    DownloadState(R.string.failed_state, zimUrl)

  override fun toString(): String = javaClass.simpleName

  fun toReadableState(context: Context): CharSequence = when (this) {
    is Failed -> context.getString(stringId, reason.name)
    is Paused -> getPauseReasonText(context, stringId, reason)
    Pending,
    Running,
    Successful -> context.getString(stringId)
  }

  private fun getPauseReasonText(
    context: Context,
    stringId: Int,
    pauseError: Error
  ): CharSequence {
    return when (pauseError) {
      Error.QUEUED_FOR_WIFI,
      Error.WAITING_FOR_NETWORK -> "${context.getString(stringId)}: ${pauseError.name}"

      else -> context.getString(stringId)
    }
  }
}
