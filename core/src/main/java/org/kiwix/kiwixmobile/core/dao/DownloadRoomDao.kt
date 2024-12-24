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
import com.tonyodev.fetch2.Status.COMPLETED
import io.reactivex.Flowable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.kiwix.kiwixmobile.core.dao.entities.DownloadRoomEntity
import org.kiwix.kiwixmobile.core.downloader.DownloadRequester
import org.kiwix.kiwixmobile.core.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.core.downloader.model.DownloadRequest
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem
import javax.inject.Inject

@Dao
abstract class DownloadRoomDao {

  @Inject
  lateinit var newBookDao: NewBookDao

  @Query("SELECT * FROM DownloadRoomEntity")
  abstract fun downloadRoomEntity(): Flowable<List<DownloadRoomEntity>>

  @Query("SELECT * FROM DownloadRoomEntity")
  abstract fun getAllDownloads(): Flow<List<DownloadRoomEntity>>

  fun downloads(): Flowable<List<DownloadModel>> =
    downloadRoomEntity()
      .distinctUntilChanged()
      .doOnNext(::moveCompletedToBooksOnDiskDao)
      .map { it.map(::DownloadModel) }

  fun allDownloads() = getAllDownloads().map { it.map(::DownloadModel) }

  private fun moveCompletedToBooksOnDiskDao(downloadEntities: List<DownloadRoomEntity>) {
    downloadEntities.filter { it.status == COMPLETED }
      .takeIf(List<DownloadRoomEntity>::isNotEmpty)
      ?.let {
        deleteDownloadsList(it)
        newBookDao.insert(it.map(BooksOnDiskListItem::BookOnDisk))
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

  fun delete(download: Download) {
    deleteDownloadByDownloadId(download.id.toLong())
  }

  fun addIfDoesNotExist(
    url: String,
    book: LibraryNetworkEntity.Book,
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

  private fun doesNotAlreadyExist(book: LibraryNetworkEntity.Book) =
    count(book.id) == 0
}
