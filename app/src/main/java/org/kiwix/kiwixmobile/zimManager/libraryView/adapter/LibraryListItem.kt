/*
 * Kiwix Android
 * Copyright (c) 2021 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.zimManager.libraryView.adapter

import androidx.annotation.StringRes
import org.kiwix.kiwixmobile.core.downloader.model.Base64String
import org.kiwix.kiwixmobile.core.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.core.downloader.model.DownloadState
import org.kiwix.kiwixmobile.core.downloader.model.Seconds
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity.Book
import org.kiwix.kiwixmobile.core.zim_manager.KiwixTag
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.CanWrite4GbFile
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.CannotWrite4GbFile
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.NotEnoughSpaceFor4GbFile
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.Unknown

sealed class LibraryListItem {
  abstract val id: Long

  data class DividerItem constructor(
    override val id: Long,
    @StringRes val stringId: Int
  ) : LibraryListItem()

  data class BookItem constructor(
    val book: Book,
    val fileSystemState: FileSystemState,
    val tags: List<KiwixTag> = KiwixTag.from(book.tags),
    override val id: Long = book.id.hashCode().toLong()
  ) : LibraryListItem() {

    val canBeDownloaded: Boolean = when (fileSystemState) {
      Unknown, CannotWrite4GbFile -> book.isLessThan4GB()
      NotEnoughSpaceFor4GbFile, CanWrite4GbFile -> true
    }

    companion object {

      private fun Book.isLessThan4GB() =
        size.toLongOrNull() ?: 0L < Fat32Checker.FOUR_GIGABYTES_IN_KILOBYTES
    }
  }

  data class LibraryDownloadItem(
    val downloadId: Long,
    val favIcon: Base64String,
    val title: String,
    val description: String?,
    val bytesDownloaded: Long,
    val totalSizeBytes: Long,
    val progress: Int,
    val eta: Seconds,
    val downloadState: DownloadState,
    override val id: Long
  ) : LibraryListItem() {

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
      DownloadState.from(downloadModel.state, downloadModel.error),
      downloadModel.book.id.hashCode().toLong()
    )
  }
}
