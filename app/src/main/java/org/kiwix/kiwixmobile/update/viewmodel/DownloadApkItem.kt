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

import android.content.Context
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
import org.kiwix.kiwixmobile.core.downloader.model.DownloadApkModel
import org.kiwix.kiwixmobile.core.downloader.model.Seconds

@Suppress("all")
data class DownloadApkItem(
  val downloadId: Long? = 0,
  val name: String = " ",
  val version: String = " ",
  val url: String = " ",
  val file: String? = " ",
  val bytesDownloaded: Long = 0,
  val totalSizeBytes: Long = 0,
  val progress: Int = 0,
  private val eta: Seconds = Seconds(0),
  val downloadApkState: DownloadApkState = DownloadApkState.from(
    state = Status.NONE,
    error = Error.NONE
  ),
  val currentDownloadState: Status? = Status.NONE,
  val downloadError: Error? = Error.NONE
) {
  val readableEta: CharSequence = eta.takeIf { it.seconds > 0L }?.toHumanReadableTime().orEmpty()

  constructor(downloadApkModel: DownloadApkModel) : this(
    downloadApkModel.downloadId,
    downloadApkModel.name,
    downloadApkModel.version,
    downloadApkModel.url,
    downloadApkModel.file,
    downloadApkModel.bytesDownloaded,
    downloadApkModel.totalSizeOfDownload,
    downloadApkModel.progress,
    Seconds(downloadApkModel.etaInMilliSeconds / 1000L),
    DownloadApkState.from(
      state = downloadApkModel.state,
      error = downloadApkModel.error
    ),
    downloadApkModel.state,
    downloadApkModel.error
  )
}

sealed class DownloadApkState(
  private val stringId: Int
) {
  companion object {
    fun from(state: Status?, error: Error?): DownloadApkState =
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

        else -> {
          Pending
        }
      }
  }

  object Pending : DownloadApkState(R.string.pending_state)
  object Running : DownloadApkState(R.string.running_state)
  object Successful : DownloadApkState(R.string.complete)
  object Paused : DownloadApkState(R.string.paused_state)
  data class Failed(val reason: Error?) :
    DownloadApkState(R.string.failed_state)

  override fun toString(): String = javaClass.simpleName

  fun toReadableState(context: Context): CharSequence =
    when (this) {
      is Failed -> context.getString(stringId, reason?.name)
      Pending,
      Running,
      Paused,
      Successful -> context.getString(stringId)
    }
}
