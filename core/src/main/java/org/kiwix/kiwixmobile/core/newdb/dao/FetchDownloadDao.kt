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
package org.kiwix.kiwixmobile.core.newdb.dao

import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Status.COMPLETED
import io.objectbox.Box
import io.objectbox.kotlin.equal
import io.objectbox.kotlin.query
import io.reactivex.Flowable
import org.kiwix.kiwixmobile.core.downloader.DownloadRequester
import org.kiwix.kiwixmobile.core.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.core.downloader.model.DownloadRequest
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity.Book
import org.kiwix.kiwixmobile.core.entity.MetaLinkNetworkEntity
import org.kiwix.kiwixmobile.core.newdb.entities.FetchDownloadEntity
import org.kiwix.kiwixmobile.core.newdb.entities.FetchDownloadEntity_
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.BookOnDisk
import javax.inject.Inject

class FetchDownloadDao @Inject constructor(
  private val box: Box<FetchDownloadEntity>,
  private val newBookDao: NewBookDao
) {

  fun downloads(): Flowable<List<DownloadModel>> =
    box.asFlowable()
      .distinctUntilChanged()
      .doOnNext(::moveCompletedToBooksOnDiskDao)
      .map { it.map(::DownloadModel) }

  private fun moveCompletedToBooksOnDiskDao(downloadEntities: List<FetchDownloadEntity>) {
    downloadEntities.filter { it.status == COMPLETED }.takeIf { it.isNotEmpty() }?.let {
      box.store.callInTx {
        box.remove(it)
        newBookDao.insert(it.map(::BookOnDisk))
      }
    }
  }

  fun update(download: Download) {
    box.store.callInTx {
      getEntityFor(download)?.let { dbEntity ->
        dbEntity.updateWith(download)
          .takeIf { updatedEntity -> updatedEntity != dbEntity }
          ?.let(box::put)
      }
    }
  }

  private fun getEntityFor(download: Download) =
    box.query {
      equal(FetchDownloadEntity_.downloadId, download.id)
    }.find().getOrNull(0)

  fun insert(downloadId: Long, book: Book) {
    box.put(FetchDownloadEntity(downloadId, book))
  }

  fun delete(download: Download) {
    box.query {
      equal(FetchDownloadEntity_.downloadId, download.id)
    }.remove()
  }

  fun addIfDoesNotExist(
    metaLinkNetworkEntity: MetaLinkNetworkEntity,
    book: Book,
    downloadRequester: DownloadRequester
  ) {
    box.store.callInTx {
      if (doesNotAlreadyExist(book)) {
        insert(
          downloadRequester.enqueue(
            DownloadRequest(
              metaLinkNetworkEntity,
              book
            )
          ),
          book = book
        )
      }
    }
  }

  private fun doesNotAlreadyExist(book: Book) =
    box.query {
      equal(FetchDownloadEntity_.bookId, book.id)
    }.count() == 0L
}
