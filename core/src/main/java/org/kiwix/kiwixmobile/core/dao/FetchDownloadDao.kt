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
package org.kiwix.kiwixmobile.core.dao

import io.objectbox.Box
import org.kiwix.kiwixmobile.core.dao.entities.FetchDownloadEntity
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity.Book
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import javax.inject.Inject

class FetchDownloadDao @Inject constructor(
  private val box: Box<FetchDownloadEntity>,
  private val newBookDao: NewBookDao,
  private val sharedPreferenceUtil: SharedPreferenceUtil
) {

  // fun downloads(): Flowable<List<DownloadModel>> =
  //   box.asFlowable()
  //     .distinctUntilChanged()
  //     .doOnNext(::moveCompletedToBooksOnDiskDao)
  //     .map { it.map(::DownloadModel) }

  // fun allDownloads() = Single.fromCallable { box.all.map(::DownloadModel) }

  // private fun moveCompletedToBooksOnDiskDao(downloadEntities: List<FetchDownloadEntity>) {
  //   downloadEntities.filter { it.status == COMPLETED }.takeIf { it.isNotEmpty() }?.let {
  //     box.store.callInTx {
  //       box.remove(it)
  //       newBookDao.insert(it.map(::BookOnDisk))
  //     }
  //   }
  // }

  // fun update(download: Download) {
  //   box.store.callInTx {
  //     getEntityFor(download.id)?.let { dbEntity ->
  //       dbEntity.updateWith(download)
  //         .takeIf { updatedEntity -> updatedEntity != dbEntity }
  //         ?.let(box::put)
  //     }
  //   }
  // }

  // fun getEntityFor(downloadId: Int) =
  //   box.query {
  //     equal(FetchDownloadEntity_.downloadId, downloadId)
  //   }.find().getOrNull(0)

  // fun getEntityForFileName(fileName: String) =
  //   box.query {
  //     endsWith(
  //       FetchDownloadEntity_.file, fileName,
  //       QueryBuilder.StringOrder.CASE_INSENSITIVE
  //     )
  //   }.findFirst()

  fun insert(downloadId: Long, book: Book, filePath: String?) {
    box.put(FetchDownloadEntity(downloadId, book, filePath))
  }

  // fun delete(downloadId: Long) {
  //   // remove the previous file from storage since we have cancelled the download.
  //   getEntityFor(downloadId.toInt())?.file?.let {
  //     File(it).deleteFile()
  //   }
  //   box.query {
  //     equal(FetchDownloadEntity_.downloadId, downloadId)
  //   }.remove()
  // }

  // fun addIfDoesNotExist(
  //   url: String,
  //   book: Book,
  //   downloadRequester: DownloadRequester
  // ) {
  //   box.store.callInTx {
  //     if (doesNotAlreadyExist(book)) {
  //       val downloadRequest = DownloadRequest(url, book.title)
  //       insert(
  //         downloadRequester.enqueue(downloadRequest),
  //         book = book,
  //         filePath = downloadRequest.getDestinationFile(sharedPreferenceUtil).path
  //       )
  //     }
  //   }
  // }

  // private fun doesNotAlreadyExist(book: Book) =
  //   box.query {
  //     equal(FetchDownloadEntity_.bookId, book.id, QueryBuilder.StringOrder.CASE_INSENSITIVE)
  //   }.count() == 0L
}
