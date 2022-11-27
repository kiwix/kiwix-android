/*
 * Kiwix Android
 * Copyright (c) 2022 Kiwix <android.kiwix.org>
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
import androidx.room.Insert
import androidx.room.Query
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Status
import io.reactivex.Flowable
import io.reactivex.Single
import org.kiwix.kiwixmobile.core.dao.entities.FetchDownloadRoomEntity
import org.kiwix.kiwixmobile.core.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity

@Dao
abstract class FetchDownloadRoomDao {
  @Query("SELECT * FROM fetchdownloadroomentity")
  abstract fun getFetchDownloadRoomEntity(): Flowable<List<FetchDownloadRoomEntity>>

  @Query("SELECT * FROM fetchdownloadroomentity WHERE downloadId LIKE :downloadId")
  abstract fun getEntityFor(downloadId: Int): FetchDownloadRoomEntity

  @Insert
  abstract fun insertDownload(fetchDownloadRoomEntity: FetchDownloadRoomEntity)

  fun update(download: Download) {
    getEntityFor(download.id)?.let { dbEntity ->
      dbEntity.updateWith(download)
        .takeIf { updatedEntity -> updatedEntity != dbEntity }
      // todo insert into table
    }
    insertDownload(getEntityFor(download.id))
  }

  fun insert(downloadId: Long, book: LibraryNetworkEntity.Book) {
    insertDownload(FetchDownloadRoomEntity(downloadId, book))
  }

  fun downloads(): Flowable<List<DownloadModel>> =
    getFetchDownloadRoomEntity()
      .distinctUntilChanged()
      .doOnNext(::moveCompletedToBooksOnDiskDao)
      .map { it.map(::DownloadModel) }

  fun allDownloads() {
    Single.fromCallable {
      getFetchDownloadRoomEntity()
        .flatMap { list -> Flowable.fromIterable(list) }
        .map(::DownloadModel)
    }
  }

  private fun moveCompletedToBooksOnDiskDao(downloadEntities: List<FetchDownloadRoomEntity>) {
    downloadEntities.filter { it.status == Status.COMPLETED }.takeIf { it.isNotEmpty() }?.let {
      // TODO: Add NewBookDao and remove the downloaded from the database
    }
    //   box.store.callInTx {
    //     box.remove(it)
    //     newBookDao.insert(it.map(BooksOnDiskListItem::BookOnDisk))
    //   }
    // }
  }
  // Todo
  // fun addIfDoesNotExist(
  //   url: String,
  //   book: LibraryNetworkEntity.Book,
  //   downloadRequester: DownloadRequester
  // ) {
  //   box.store.callInTx {
  //     if (doesNotAlreadyExist(book)) {
  //       insert(
  //         downloadRequester.enqueue(DownloadRequest(url)),
  //         book = book
  //       )
  //     }
  //   }
  // }
  //
  // private fun doesNotAlreadyExist(book: LibraryNetworkEntity.Book) =
  //   box.query {
  //     equal(FetchDownloadEntity_.bookId, book.id)
  //   }.count() == 0L
}


