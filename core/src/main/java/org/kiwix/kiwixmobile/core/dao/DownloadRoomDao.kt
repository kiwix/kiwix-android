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

package org.kiwix.kiwixmobile.core.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Status
import com.tonyodev.fetch2.Status.COMPLETED
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import org.kiwix.kiwixmobile.core.dao.entities.DownloadRoomEntity
import org.kiwix.kiwixmobile.core.dao.entities.PauseReason
import org.kiwix.kiwixmobile.core.downloader.DownloadRequester
import org.kiwix.kiwixmobile.core.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.core.downloader.model.DownloadRequest
import org.kiwix.kiwixmobile.core.entity.LibkiwixBook
import org.kiwix.libkiwix.Book
import org.kiwix.libzim.Archive
import javax.inject.Inject

@Dao
abstract class DownloadRoomDao {
  @Inject
  lateinit var libkiwixBookOnDisk: LibkiwixBookOnDisk

  @Query("SELECT * FROM DownloadRoomEntity")
  abstract fun getAllDownloads(): Flow<List<DownloadRoomEntity>>

  fun downloads(): Flow<List<DownloadModel>> =
    getAllDownloads()
      .distinctUntilChanged()
      .onEach(::moveCompletedToBooksOnDiskDao)
      .map { it.map(::DownloadModel) }

  fun allDownloads() = getAllDownloads().map { it.map(::DownloadModel) }

  @Suppress("InjectDispatcher")
  private suspend fun moveCompletedToBooksOnDiskDao(downloadEntities: List<DownloadRoomEntity>) {
    downloadEntities.filter { it.status == COMPLETED }
      .takeIf(List<DownloadRoomEntity>::isNotEmpty)
      ?.let { completedDownloads ->
        deleteDownloadsList(completedDownloads)
        // We now use the OPDS stream instead of the custom library.xml handling.
        // In the OPDS stream, the favicon is a URL instead of a Base64 string.
        // So when a download is completed, we extract the illustration directly from the archive.
        val booksOnDisk = completedDownloads.map { download ->
          val archive = withContext(Dispatchers.IO) {
            Archive(download.file)
          }
          Book().apply { update(archive) }
        }
        libkiwixBookOnDisk.insert(booksOnDisk)
      }
  }

  fun update(download: Download) {
    getEntityForDownloadId(download.id.toLong())?.let { downloadRoomEntity ->
      downloadRoomEntity.updateWith(download)
        .takeIf { updatedEntity -> updatedEntity != downloadRoomEntity }
        ?.let(::updateDownloadItem)
    }
  }

  @Update
  abstract fun updateDownloadItem(downloadRoomEntity: DownloadRoomEntity)

  @Delete
  abstract fun deleteDownloadsList(downloadRoomEntityList: List<DownloadRoomEntity>)

  @Query("DELETE FROM DownloadRoomEntity WHERE downloadId=:downloadId")
  abstract fun deleteDownloadByDownloadId(downloadId: Long)

  @Query("SELECT * FROM DownloadRoomEntity WHERE downloadId=:downloadId")
  abstract fun getEntityForDownloadId(downloadId: Long): DownloadRoomEntity?

  @Query("SELECT COUNT() FROM DownloadRoomEntity WHERE bookId = :bookId")
  abstract fun count(bookId: String): Int

  @Query(
    "SELECT * FROM DownloadRoomEntity WHERE " +
      "file LIKE '%' || :fileName || '%' COLLATE NOCASE LIMIT 1"
  )
  abstract fun getEntityForFileName(fileName: String): DownloadRoomEntity?

  @Insert
  abstract fun saveDownload(downloadRoomEntity: DownloadRoomEntity)

  @Query("SELECT * FROM DownloadRoomEntity WHERE pauseReason = :reason")
  abstract fun getDownloadsPausedByService(
    reason: PauseReason = PauseReason.SERVICE
  ): List<DownloadRoomEntity>

  suspend fun getOngoingDownloads(): List<DownloadModel> = allDownloads().first()
    .filter {
      it.state == Status.QUEUED ||
        it.state == Status.DOWNLOADING ||
        it.state == Status.NONE ||
        it.state == Status.ADDED ||
        it.state == Status.PAUSED
    }

  fun delete(download: Download) {
    deleteDownloadByDownloadId(download.id.toLong())
  }

  suspend fun addIfDoesNotExist(
    url: String,
    book: LibkiwixBook,
    downloadRequester: DownloadRequester
  ) {
    if (doesNotAlreadyExist(book)) {
      saveDownload(
        DownloadRoomEntity(
          downloadRequester.enqueue(DownloadRequest(url)),
          book = book
        )
      )
    }
  }

  private fun doesNotAlreadyExist(book: LibkiwixBook) =
    count(book.id) == 0
}
