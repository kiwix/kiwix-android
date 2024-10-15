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
import io.reactivex.Flowable
import io.reactivex.Single
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.dao.entities.DownloadRoomEntity
import org.kiwix.kiwixmobile.core.downloader.DownloadRequester
import org.kiwix.kiwixmobile.core.downloader.downloadManager.Status
import org.kiwix.kiwixmobile.core.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.core.downloader.model.DownloadRequest
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity
import org.kiwix.kiwixmobile.core.extensions.deleteFile
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem
import java.io.File
import javax.inject.Inject

@Dao
abstract class DownloadRoomDao {

  @Inject
  lateinit var newBookDao: NewBookDao

  @Query("SELECT * FROM DownloadRoomEntity")
  abstract fun downloadRoomEntity(): Flowable<List<DownloadRoomEntity>>

  @Query("SELECT * FROM DownloadRoomEntity")
  abstract fun getAllDownloads(): Single<List<DownloadRoomEntity>>

  fun downloads(): Flowable<List<DownloadModel>> =
    downloadRoomEntity()
      .distinctUntilChanged()
      .doOnNext(::moveCompletedToBooksOnDiskDao)
      .map { it.map(::DownloadModel) }

  fun allDownloads() = getAllDownloads().map { it.map(::DownloadModel) }

  private fun moveCompletedToBooksOnDiskDao(downloadEntities: List<DownloadRoomEntity>) {
    downloadEntities.filter { it.status == Status.COMPLETED }
      .takeIf(List<DownloadRoomEntity>::isNotEmpty)
      ?.let {
        deleteDownloadsList(it)
        newBookDao.insert(it.map(BooksOnDiskListItem::BookOnDisk))
      }
  }

  fun update(downloadModel: DownloadModel) {
    getEntityForDownloadId(downloadModel.downloadId)?.let { downloadRoomEntity ->
      downloadRoomEntity.updateWith(downloadModel)
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

  @Insert
  abstract fun saveDownload(downloadRoomEntity: DownloadRoomEntity)

  fun delete(downloadId: Long) {
    // remove the previous file from storage since we have cancelled the download.
    getEntityForDownloadId(downloadId)?.file?.let {
      CoroutineScope(Dispatchers.IO).launch {
        File(it).deleteFile()
      }
    }
    deleteDownloadByDownloadId(downloadId)
  }

  fun addIfDoesNotExist(
    url: String,
    book: LibraryNetworkEntity.Book,
    downloadRequester: DownloadRequester,
    sharedPreferenceUtil: SharedPreferenceUtil
  ) {
    if (doesNotAlreadyExist(book)) {
      val downloadRequest = DownloadRequest(url)
      saveDownload(
        DownloadRoomEntity(
          url,
          downloadRequester.enqueue(downloadRequest),
          book = book,
          file = downloadRequest.getDestinationFile(sharedPreferenceUtil).path
        )
      ).also {
        downloadRequester.onDownloadAdded()
      }
    }
  }

  private fun doesNotAlreadyExist(book: LibraryNetworkEntity.Book) =
    count(book.id) == 0
}
