package org.kiwix.kiwixmobile.zim_manager

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function4
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import org.kiwix.kiwixmobile.R.string
import org.kiwix.kiwixmobile.database.BookDao
import org.kiwix.kiwixmobile.database.DownloadDao
import org.kiwix.kiwixmobile.database.NetworkLanguageDao
import org.kiwix.kiwixmobile.downloader.Downloader
import org.kiwix.kiwixmobile.downloader.model.DownloadItem
import org.kiwix.kiwixmobile.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.downloader.model.DownloadState.Successful
import org.kiwix.kiwixmobile.downloader.model.DownloadStatus
import org.kiwix.kiwixmobile.extensions.registerReceiver
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book
import org.kiwix.kiwixmobile.network.KiwixService
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.StorageObserver
import org.kiwix.kiwixmobile.zim_manager.library_view.adapter.Language
import org.kiwix.kiwixmobile.zim_manager.library_view.adapter.LibraryListItem
import org.kiwix.kiwixmobile.zim_manager.library_view.adapter.LibraryListItem.BookItem
import org.kiwix.kiwixmobile.zim_manager.library_view.adapter.LibraryListItem.DividerItem
import java.util.LinkedList
import java.util.Locale
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
  private val languageDao: NetworkLanguageDao,
  private val downloader: Downloader,
  private val storageObserver: StorageObserver,
  private val kiwixService: KiwixService,
  private val context: Application,
  private val connectivityBroadcastReceiver: ConnectivityBroadcastReceiver
) : ViewModel() {

  val libraryItems: MutableLiveData<List<LibraryListItem>> = MutableLiveData()
  val downloadItems: MutableLiveData<List<DownloadItem>> = MutableLiveData()
  val bookItems: MutableLiveData<List<Book>> = MutableLiveData()
  val deviceListIsRefreshing = MutableLiveData<Boolean>()
  val libraryListIsRefreshing = MutableLiveData<Boolean>()
  val networkStates = MutableLiveData<NetworkState>()

  val requestFileSystemCheck = PublishProcessor.create<Unit>()
  val requestDownloadLibrary = PublishProcessor.create<Unit>()

  private val compositeDisposable = CompositeDisposable()

  init {
    compositeDisposable.addAll(*disposables())
    requestDownloadLibrary.onNext(Unit)
    context.registerReceiver(connectivityBroadcastReceiver)
  }

  override fun onCleared() {
    compositeDisposable.clear()
    context.unregisterReceiver(connectivityBroadcastReceiver)
    super.onCleared()
  }

  private fun disposables(): Array<Disposable> {
    val downloads: Flowable<MutableList<DownloadModel>> = downloadDao.downloads()
    val downloadStatuses = downloadStatuses(downloads)
    val booksFromDao: Flowable<List<Book>> = books()
    val library = libraryFromNetwork()
    return arrayOf(
        updateDownloadItems(downloadStatuses),
        removeCompletedDownloadsFromDb(downloadStatuses),
        updateBookItems(booksFromDao),
        checkFileSystemForBooksOnRequest(booksFromDao),
        updateLibraryItems(booksFromDao, downloads, library),
        updateActiveLanguages(library),
        updateNetworkStates()
    )
  }

  private fun updateNetworkStates() =
    connectivityBroadcastReceiver.networkStates.subscribe(
        networkStates::postValue, Throwable::printStackTrace
    )

  private fun libraryFromNetwork() =
    Flowable.combineLatest(
        requestDownloadLibrary,
        connectivityBroadcastReceiver.networkStates.filter(
            NetworkState.CONNECTED::equals
        ),
        BiFunction<Unit, NetworkState, Unit> { _, _ -> Unit }
    )
        .subscribeOn(Schedulers.io())
        .doOnNext { libraryListIsRefreshing.postValue(true) }
        .switchMap { kiwixService.library }
        .doOnError(Throwable::printStackTrace)
        .onErrorResumeNext(Flowable.just(LibraryNetworkEntity().apply { book = LinkedList() }))
        .doOnNext { libraryListIsRefreshing.postValue(false) }

  private fun updateLibraryItems(
    booksFromDao: Flowable<List<Book>>,
    downloads: Flowable<MutableList<DownloadModel>>,
    library: Flowable<LibraryNetworkEntity>?
  ) = Flowable.combineLatest(
      booksFromDao,
      downloads,
      languageDao.activeLanguages().filter { it.isNotEmpty() },
      library,
      Function4(this::combineLibrarySources)
  )
      .subscribeOn(Schedulers.io())
      .subscribe(
          libraryItems::postValue,
          Throwable::printStackTrace
      )

  private fun updateActiveLanguages(library: Flowable<LibraryNetworkEntity>) = library
      .subscribeOn(Schedulers.io())
      .map { it.books }
      .withLatestFrom(
          languageDao.activeLanguages(),
          BiFunction(this::combineToLanguageList)
      )
      .subscribe(
          languageDao::saveFilteredLanguages,
          Throwable::printStackTrace
      )

  private fun combineToLanguageList(
    booksFromNetwork: List<Book>,
    activeLanguages: List<Language>
  ): List<Language> {
    val languagesFromNetwork = booksFromNetwork.distinctBy { it.language }
        .map { it.language }
    return Locale.getISOLanguages()
        .map { Locale(it) }
        .filter { languagesFromNetwork.contains(it.isO3Language) }
        .map { locale ->
          Language(
              locale.isO3Language,
              languageWasPreviouslyActiveOrIsPrimaryLanguage(activeLanguages, locale)
          )
        }
        .ifEmpty { listOf(Language(context.resources.configuration.locale.isO3Language, true)) }
  }

  private fun languageWasPreviouslyActiveOrIsPrimaryLanguage(
    activeLanguages: List<Language>,
    locale: Locale
  ) = activeLanguages.firstOrNull { it.languageCode == locale.isO3Language }?.let { true }
      ?: isPrimaryLocale(locale)

  private fun isPrimaryLocale(locale: Locale) =
    context.resources.configuration.locale.isO3Language == locale.isO3Language

  private fun combineLibrarySources(
    booksOnFileSystem: List<Book>,
    activeDownloads: List<DownloadModel>,
    activeLanguages: List<Language>,
    libraryNetworkEntity: LibraryNetworkEntity
  ): List<LibraryListItem> {
    val downloadedBooksIds = booksOnFileSystem.map { it.id }
    val downloadingBookIds = activeDownloads.map { it.bookId }
    val activeLanguageCodes = activeLanguages.map { it.languageCode }
    val booksUnfilteredByLanguage = libraryNetworkEntity.books
        .filterNot { downloadedBooksIds.contains(it.id) }
        .filterNot { downloadingBookIds.contains(it.id) }
        .filterNot { it.url.contains("/stack_exchange/") }// Temp filter see #694
    return listOf(
        DividerItem(Long.MAX_VALUE, context.getString(string.your_languages)),
        *toBookItems(
            booksUnfilteredByLanguage.filter { activeLanguageCodes.contains(it.language) }
        ),
        DividerItem(Long.MIN_VALUE, context.getString(string.other_languages)),
        *toBookItems(
            booksUnfilteredByLanguage.filterNot { activeLanguageCodes.contains(it.language) }
        )
    )
  }

  private fun toBookItems(books: List<Book>) =
    books.map { BookItem(it) }.toTypedArray()

  private fun checkFileSystemForBooksOnRequest(booksFromDao: Flowable<List<Book>>) =
    requestFileSystemCheck
        .doOnNext { deviceListIsRefreshing.postValue(true) }
        .switchMap {
          updateBookDaoFromFilesystem(booksFromDao)
        }
        .doOnNext { deviceListIsRefreshing.postValue(false) }
        .subscribe(
            bookDao::saveBooks,
            Throwable::printStackTrace
        )

  private fun books() = bookDao.books()
      .subscribeOn(Schedulers.io())
      .map { it.sortedBy { book -> book.title } }

  private fun updateBookDaoFromFilesystem(booksFromDao: Flowable<List<Book>>) =
    storageObserver.booksOnFileSystem
        .withLatestFrom(
            booksFromDao,
            BiFunction(this::removeBooksAlreadyInDao)
        )

  private fun removeBooksAlreadyInDao(
    booksFromFileSystem: Collection<Book>,
    booksFromDao: List<Book>
  ) = booksFromFileSystem.minus(
      booksFromDao
  )

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

  private fun downloadStatuses(downloads: Flowable<MutableList<DownloadModel>>) =
    Flowable.combineLatest(
        downloads,
        Flowable.interval(1, SECONDS),
        BiFunction { downloadModels: List<DownloadModel>, _: Long -> downloadModels }
    )
        .subscribeOn(Schedulers.io())
        .map(downloader::queryStatus)
        .distinctUntilChanged()
}