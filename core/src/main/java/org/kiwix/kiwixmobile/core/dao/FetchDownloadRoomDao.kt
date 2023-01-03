/*
 * Kiwix Android
 * Copyright (c) 2023 Kiwix <android.kiwix.org>
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
import androidx.room.Transaction
import androidx.room.TypeConverter
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.Status
import io.reactivex.Flowable
import io.reactivex.Single
import org.kiwix.kiwixmobile.core.dao.entities.EnumConverter
import org.kiwix.kiwixmobile.core.dao.entities.FetchDownloadEntity
import org.kiwix.kiwixmobile.core.dao.entities.FetchDownloadRoomEntity
import org.kiwix.kiwixmobile.core.downloader.DownloadRequester
import org.kiwix.kiwixmobile.core.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.core.downloader.model.DownloadRequest
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem
import javax.inject.Inject

@Dao
abstract class FetchDownloadRoomDao {
  @Inject
  lateinit var newBookDao: NewBookDao

  @Query("SELECT * FROM FetchDownloadRoomEntity")
  abstract fun downloadsAsEntity(): Flowable<List<FetchDownloadRoomEntity>>

  @Query("SELECT * FROM FetchDownloadRoomEntity")
  abstract fun downloadsAsEntity2(): List<FetchDownloadRoomEntity>

  @Query("SELECT * FROM FetchDownloadRoomEntity WHERE bookId LIKE :id")
  abstract fun getBook(id: String): FetchDownloadRoomEntity?

  fun downloads(): Flowable<List<DownloadModel>> = downloadsAsEntity()
    .distinctUntilChanged()
    .doOnNext(::moveCompletedToBooksOnDiskDao)
    .map {
      it.map(::DownloadModel)
    }

  fun allDownloads() = Single.fromCallable {
    downloadsAsEntity2().map(::DownloadModel)
  }

  @Transaction
  open fun moveCompletedToBooksOnDiskDao(downloadEntities: List<FetchDownloadRoomEntity>) {
    downloadEntities.filter { it.status == Status.COMPLETED }.takeIf { it.isNotEmpty() }?.let {
      newBookDao.insert(it.map(BooksOnDiskListItem::BookOnDisk))
      it.forEach(::deleteAsEntity)

    }
  }

  @Delete
  abstract fun deleteAsEntity(downloadEntity: FetchDownloadRoomEntity)

  @Query("DELETE FROM FetchDownloadRoomEntity WHERE downloadId LIKE :id")
  abstract fun delete(id: Int)

  fun delete(download: Download) {
    delete(download.id)
  }

  @Query("SELECT * FROM FetchDownloadRoomEntity WHERE downloadId LIKE :id")
  abstract fun getEntityFor(id: Int): FetchDownloadRoomEntity?

  fun getEntityFor(download: Download) = getEntityFor(download.id)

  @Transaction
  open fun update(download: Download) {
    getEntityFor(download)?.let { dbEntity ->
      dbEntity.updateWith(download)
        .takeIf { updateEntity -> updateEntity != dbEntity }
        ?.let(::insertFetchDownloadEntity)
    }
  }

  @Transaction
  open fun addIfDoesNotExist(
    url: String,
    book: LibraryNetworkEntity.Book,
    downloadRequester: DownloadRequester
  ) {
    if (getBook(book.id) == null) {
      insert(downloadRequester.enqueue(DownloadRequest(url)), book)
    }
  }

  @Transaction
  open fun insert(downloadId: Long, book: LibraryNetworkEntity.Book) {
    val fetchDownloadRoomEntity = FetchDownloadRoomEntity(downloadId, book)
    insertFetchDownloadEntity(fetchDownloadRoomEntity)
  }

  @Insert
  abstract fun insertFetchDownloadEntity(fetchDownloadRoomEntity: FetchDownloadRoomEntity)
}

class StatusConverter : EnumConverter<Status>() {
  @TypeConverter
  override fun convertToEntityProperty(databaseValue: Int) = Status.valueOf(databaseValue)
}

class ErrorConverter : EnumConverter<Error>() {
  @TypeConverter
  override fun convertToEntityProperty(databaseValue: Int) = Error.valueOf(databaseValue)
}

abstract class BaseEnumConverter<E : Enum<E>> {
  @TypeConverter
  fun convertToEntityProperty(entityProperty: E): Int = entityProperty.ordinal
}
