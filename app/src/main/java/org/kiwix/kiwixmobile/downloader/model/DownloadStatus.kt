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
import android.app.DownloadManager.ERROR_CANNOT_RESUME
import android.app.DownloadManager.ERROR_DEVICE_NOT_FOUND
import android.app.DownloadManager.ERROR_FILE_ALREADY_EXISTS
import android.app.DownloadManager.ERROR_FILE_ERROR
import android.app.DownloadManager.ERROR_HTTP_DATA_ERROR
import android.app.DownloadManager.ERROR_INSUFFICIENT_SPACE
import android.app.DownloadManager.ERROR_TOO_MANY_REDIRECTS
import android.app.DownloadManager.ERROR_UNHANDLED_HTTP_CODE
import android.app.DownloadManager.ERROR_UNKNOWN
import android.app.DownloadManager.PAUSED_QUEUED_FOR_WIFI
import android.app.DownloadManager.PAUSED_UNKNOWN
import android.app.DownloadManager.PAUSED_WAITING_FOR_NETWORK
import android.app.DownloadManager.PAUSED_WAITING_TO_RETRY
import android.app.DownloadManager.STATUS_FAILED
import android.app.DownloadManager.STATUS_PAUSED
import android.app.DownloadManager.STATUS_PENDING
import android.app.DownloadManager.STATUS_RUNNING
import android.app.DownloadManager.STATUS_SUCCESSFUL
import android.database.Cursor
import android.net.Uri
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.extensions.get
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.BookOnDisk
import java.io.File

class DownloadStatus(
  val downloadId: Long,
  val title: String,
  val description: String,
  val state: DownloadState,
  val bytesDownloadedSoFar: Long,
  val totalSizeBytes: Long,
  val lastModified: String,
  private val localUri: String?,
  val mediaProviderUri: String?,
  val mediaType: String?,
  val uri: String?,
  val book: Book
) {

  fun toBookOnDisk(uriToFileConverter: UriToFileConverter) =
    BookOnDisk(book = book, file = uriToFileConverter.convert(localUri))

  constructor(
    cursor: Cursor,
    downloadModel: DownloadModel
  ) : this(
    cursor[COLUMN_ID],
    cursor[COLUMN_TITLE],
    cursor[COLUMN_DESCRIPTION],
    DownloadState.from(cursor[COLUMN_STATUS], cursor[COLUMN_REASON]),
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

interface UriToFileConverter {
  fun convert(uriString: String?) = File(Uri.parse(uriString).path)
  class Impl : UriToFileConverter
}

sealed class DownloadState(val stringId: Int) {
  companion object {
    fun from(
      status: Int,
      reason: Int
    ) = when (status) {
      STATUS_PAUSED -> Paused(PausedReason.from(reason))
      STATUS_FAILED -> Failed(FailureReason.from(reason))
      STATUS_PENDING -> Pending
      STATUS_RUNNING -> Running
      STATUS_SUCCESSFUL -> Successful
      else -> throw RuntimeException("invalid status $status")
    }
  }

  data class Paused(val reason: PausedReason) : DownloadState(R.string.paused_state)
  data class Failed(val reason: FailureReason) : DownloadState(R.string.failed_state)
  object Pending : DownloadState(R.string.pending_state)
  object Running : DownloadState(R.string.running_state)
  object Successful : DownloadState(R.string.successful_state)

  override fun toString(): String = javaClass.simpleName
}

sealed class FailureReason(val stringId: Int) {
  companion object {
    fun from(reason: Int) = when (reason) {
      in 100..505 -> Rfc2616HttpCode(reason)
      ERROR_CANNOT_RESUME -> CannotResume
      ERROR_DEVICE_NOT_FOUND -> StorageNotFound
      ERROR_FILE_ALREADY_EXISTS -> FileAlreadyExists
      ERROR_FILE_ERROR -> UnknownFileError
      ERROR_HTTP_DATA_ERROR -> HttpError
      ERROR_INSUFFICIENT_SPACE -> InsufficientSpace
      ERROR_TOO_MANY_REDIRECTS -> TooManyRedirects
      ERROR_UNHANDLED_HTTP_CODE -> UnhandledHttpCode
      ERROR_UNKNOWN -> Unknown
      else -> Unknown
    }
  }

  object CannotResume : FailureReason(R.string.failed_cannot_resume)
  object StorageNotFound : FailureReason(R.string.failed_storage_not_found)
  object FileAlreadyExists : FailureReason(R.string.failed_file_already_exists)
  object UnknownFileError : FailureReason(R.string.failed_unknown_file_error)
  object HttpError : FailureReason(R.string.failed_http_error)
  object InsufficientSpace : FailureReason(R.string.failed_insufficient_space)
  object TooManyRedirects : FailureReason(R.string.failed_too_many_redirects)
  object UnhandledHttpCode : FailureReason(R.string.failed_unhandled_http_code)
  object Unknown : FailureReason(R.string.failed_unknown)
  data class Rfc2616HttpCode(val code: Int) : FailureReason(R.string.failed_http_code)
}

sealed class PausedReason(val stringId: Int) {
  companion object {
    fun from(reason: Int) = when (reason) {
      PAUSED_QUEUED_FOR_WIFI -> WaitingForWifi
      PAUSED_WAITING_FOR_NETWORK -> WaitingForConnectivity
      PAUSED_WAITING_TO_RETRY -> WaitingForRetry
      PAUSED_UNKNOWN -> Unknown
      else -> Unknown
    }
  }

  object WaitingForWifi : PausedReason(R.string.paused_wifi)
  object WaitingForConnectivity : PausedReason(R.string.paused_connectivity)
  object WaitingForRetry : PausedReason(R.string.paused_retry)
  object Unknown : PausedReason(R.string.paused_unknown)
}
