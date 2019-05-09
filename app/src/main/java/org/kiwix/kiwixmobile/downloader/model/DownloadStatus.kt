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

import android.app.DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR
import android.app.DownloadManager.COLUMN_DESCRIPTION
import android.app.DownloadManager.COLUMN_ID
import android.app.DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP
import android.app.DownloadManager.COLUMN_LOCAL_URI
import android.app.DownloadManager.COLUMN_MEDIAPROVIDER_URI
import android.app.DownloadManager.COLUMN_MEDIA_TYPE
import android.app.DownloadManager.COLUMN_REASON
import android.app.DownloadManager.COLUMN_STATUS
import android.app.DownloadManager.COLUMN_TITLE
import android.app.DownloadManager.COLUMN_TOTAL_SIZE_BYTES
import android.app.DownloadManager.COLUMN_URI
import android.app.DownloadManager.STATUS_FAILED
import android.app.DownloadManager.STATUS_PAUSED
import android.app.DownloadManager.STATUS_PENDING
import android.app.DownloadManager.STATUS_RUNNING
import android.app.DownloadManager.STATUS_SUCCESSFUL
import android.database.Cursor
import android.net.Uri
import org.kiwix.kiwixmobile.extensions.get
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book
import java.io.File

class DownloadStatus(
  val downloadId: Long,
  val title: String,
  val description: String,
  val state: DownloadState,
  val reason: String,
  val bytesDownloadedSoFar: Long,
  val totalSizeBytes: Long,
  val lastModified: String,
  val localUri: String?,
  val mediaProviderUri: String?,
  val mediaType: String?,
  val uri: String?,
  val book: Book
) {
  fun toBook() = book.also {
    book.file = File(Uri.parse(localUri).path)
  }

  constructor(
    cursor: Cursor,
    downloadModel: DownloadModel
  ) : this(
      cursor[COLUMN_ID],
      cursor[COLUMN_TITLE],
      cursor[COLUMN_DESCRIPTION],
      DownloadState.from(cursor[COLUMN_STATUS]),
      cursor[COLUMN_REASON],
      cursor[COLUMN_BYTES_DOWNLOADED_SO_FAR],
      cursor[COLUMN_TOTAL_SIZE_BYTES],
      cursor[COLUMN_LAST_MODIFIED_TIMESTAMP],
      cursor[COLUMN_LOCAL_URI],
      cursor[COLUMN_MEDIAPROVIDER_URI],
      cursor[COLUMN_MEDIA_TYPE],
      cursor[COLUMN_URI],
      downloadModel.book
  )
}

sealed class DownloadState {
  companion object {
    fun from(status: Int) = when (status) {
      STATUS_FAILED -> Failed
      STATUS_PAUSED -> Paused
      STATUS_PENDING -> Pending
      STATUS_RUNNING -> Running
      STATUS_SUCCESSFUL -> Successful
      else -> throw RuntimeException("invalid status $status")
    }
  }

  object Paused : DownloadState()
  object Failed : DownloadState()
  object Pending : DownloadState()
  object Running : DownloadState()
  object Successful : DownloadState()

  override fun toString(): String {
    return javaClass.simpleName
  }
}
