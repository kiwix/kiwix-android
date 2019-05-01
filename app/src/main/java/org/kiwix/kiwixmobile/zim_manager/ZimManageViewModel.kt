package org.kiwix.kiwixmobile.zim_manager

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import org.kiwix.kiwixmobile.database.BookDao
import org.kiwix.kiwixmobile.database.DownloadDao
import org.kiwix.kiwixmobile.downloader.Downloader
import org.kiwix.kiwixmobile.downloader.model.DownloadItem
import org.kiwix.kiwixmobile.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.downloader.model.DownloadState.Successful
import org.kiwix.kiwixmobile.downloader.model.DownloadStatus
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.StorageObserver
import java.util.concurrent.TimeUnit.SECONDS
import javax.inject.Inject

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
class ZimManageViewModel @Inject constructor(
  private val downloadDao: DownloadDao,
  private val bookDao: BookDao,
  private val downloader: Downloader,
  private val storageObserver: StorageObserver
) : ViewModel() {

  val downloadItems: MutableLiveData<List<DownloadItem>> = MutableLiveData()
  val bookItems: MutableLiveData<List<Book>> = MutableLiveData()
  val checkFileSystem = PublishProcessor.create<Unit>()
  val deviceListIsRefreshing = MutableLiveData<Boolean>()

  private val compositeDisposable = CompositeDisposable()

  init {
    val downloadStatuses = downloadStatuses()
    val booksFromDao = books()
    compositeDisposable.addAll(
        updateDownloadItems(downloadStatuses),
        removeCompletedDownloadsFromDb(downloadStatuses),
        updateBookItems(booksFromDao),
        checkFileSystemForBooksOnRequest(booksFromDao)
    )
  }

  private fun checkFileSystemForBooksOnRequest(booksFromDao: Flowable<List<Book>>): Disposable? {
    return checkFileSystem
        .doOnNext { deviceListIsRefreshing.postValue(true) }
        .switchMap {
          updateBookDaoFromFilesystem(booksFromDao)
        }
        .doOnNext { deviceListIsRefreshing.postValue(false) }
        .subscribe(
            bookDao::saveBooks,
            Throwable::printStackTrace
        )
  }

  private fun books() = bookDao.books()
      .subscribeOn(Schedulers.io())
      .map { it.sortedBy { book -> book.title } }

  private fun updateBookDaoFromFilesystem(booksFromDao: Flowable<List<Book>>) =
    storageObserver.booksOnFileSystem.withLatestFrom(
        booksFromDao,
        BiFunction<Collection<Book>, List<Book>, List<Book>> { booksFileSystem, booksDao ->
          booksFileSystem.minus(
              booksDao
          )
        })

  private fun updateBookItems(booksFromDao: Flowable<List<Book>>) =
    booksFromDao
        .subscribe(
            bookItems::postValue,
            Throwable::printStackTrace
        )

  private fun removeCompletedDownloadsFromDb(downloadStatuses: Flowable<List<DownloadStatus>>) =
    downloadStatuses.subscribe(
        {
          downloadDao.delete(
              *it.filter { status -> status.state == Successful }.map { status -> status.downloadId }.toTypedArray()
          )
        },
        Throwable::printStackTrace
    )

  private fun updateDownloadItems(downloadStatuses: Flowable<List<DownloadStatus>>) =
    downloadStatuses
        .map { statuses -> statuses.map { DownloadItem(it) } }
        .subscribe(
            downloadItems::postValue,
            Throwable::printStackTrace
        )

  private fun downloadStatuses() = Flowable.combineLatest(
      downloadDao.downloads(),
      Flowable.interval(1, SECONDS),
      BiFunction { downloadModels: List<DownloadModel>, _: Long -> downloadModels }
  )
      .subscribeOn(Schedulers.io())
      .map(downloader::queryStatus)
      .distinctUntilChanged()

  override fun onCleared() {
    compositeDisposable.clear()
    super.onCleared()
  }
}