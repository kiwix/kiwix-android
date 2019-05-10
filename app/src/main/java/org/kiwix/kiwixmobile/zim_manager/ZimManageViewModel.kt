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

package org.kiwix.kiwixmobile.zim_manager

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function5
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.database.BookDao
import org.kiwix.kiwixmobile.database.DownloadDao
import org.kiwix.kiwixmobile.database.NetworkLanguageDao
import org.kiwix.kiwixmobile.downloader.Downloader
import org.kiwix.kiwixmobile.downloader.model.DownloadItem
import org.kiwix.kiwixmobile.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.downloader.model.DownloadState.Successful
import org.kiwix.kiwixmobile.downloader.model.DownloadStatus
import org.kiwix.kiwixmobile.extensions.calculateSearchMatches
import org.kiwix.kiwixmobile.extensions.registerReceiver
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book
import org.kiwix.kiwixmobile.network.KiwixService
import org.kiwix.kiwixmobile.utils.BookUtils
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.StorageObserver
import org.kiwix.kiwixmobile.zim_manager.library_view.adapter.Language
import org.kiwix.kiwixmobile.zim_manager.library_view.adapter.LibraryListItem
import org.kiwix.kiwixmobile.zim_manager.library_view.adapter.LibraryListItem.BookItem
import org.kiwix.kiwixmobile.zim_manager.library_view.adapter.LibraryListItem.DividerItem
import java.util.LinkedList
import java.util.Locale
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS
import javax.inject.Inject

class ZimManageViewModel @Inject constructor(
  private val downloadDao: DownloadDao,
  private val bookDao: BookDao,
  private val languageDao: NetworkLanguageDao,
  private val downloader: Downloader,
  private val storageObserver: StorageObserver,
  private val kiwixService: KiwixService,
  private val context: Application,
  private val connectivityBroadcastReceiver: ConnectivityBroadcastReceiver,
  private val bookUtils: BookUtils
) : ViewModel() {

  val libraryItems: MutableLiveData<List<LibraryListItem>> = MutableLiveData()
  val downloadItems: MutableLiveData<List<DownloadItem>> = MutableLiveData()
  val bookItems: MutableLiveData<List<Book>> = MutableLiveData()
  val deviceListIsRefreshing = MutableLiveData<Boolean>()
  val libraryListIsRefreshing = MutableLiveData<Boolean>()
  val networkStates = MutableLiveData<NetworkState>()
  val languageItems = MutableLiveData<List<Language>>()

  val requestFileSystemCheck = PublishProcessor.create<Unit>()
  val requestDownloadLibrary = BehaviorProcessor.createDefault<Unit>(Unit)
  val requestFiltering = BehaviorProcessor.createDefault<String>("")
  val requestLanguagesDialog = PublishProcessor.create<Unit>()

  private val compositeDisposable = CompositeDisposable()

  init {
    compositeDisposable.addAll(*disposables())
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
        updateLanguagesInDao(library),
        updateNetworkStates(),
        updateLanguageItemsForDialog()
    )
  }

  private fun updateLanguageItemsForDialog() = requestLanguagesDialog
      .withLatestFrom(languageDao.allLanguages(),
          BiFunction<Unit, List<Language>, List<Language>> { _, languages -> languages })
      .subscribe(
          languageItems::postValue,
          Throwable::printStackTrace
      )

  private fun updateNetworkStates() =
    connectivityBroadcastReceiver.networkStates.subscribe(
        networkStates::postValue, Throwable::printStackTrace
    )

  private fun libraryFromNetwork() =
    Flowable.combineLatest(
        requestDownloadLibrary,
        connectivityBroadcastReceiver.networkStates.distinctUntilChanged().filter(
            NetworkState.CONNECTED::equals
        ),
        BiFunction<Unit, NetworkState, Unit> { _, _ -> Unit }
    )
        .subscribeOn(Schedulers.io())
        .doOnNext { libraryListIsRefreshing.postValue(true) }
        .switchMap { kiwixService.library }
        .doOnError(Throwable::printStackTrace)
        .onErrorResumeNext(Flowable.just(LibraryNetworkEntity().apply { book = LinkedList() }))

  private fun updateLibraryItems(
    booksFromDao: Flowable<List<Book>>,
    downloads: Flowable<MutableList<DownloadModel>>,
    library: Flowable<LibraryNetworkEntity>
  ) = Flowable.combineLatest(
      booksFromDao,
      downloads,
      languageDao.allLanguages()
          .debounce(100, MILLISECONDS)
          .filter { it.isNotEmpty() },
      library,
      requestFiltering
          .doOnNext { libraryListIsRefreshing.postValue(true) }
          .debounce(1, SECONDS)
          .observeOn(Schedulers.io()),
      Function5(this::combineLibrarySources)
  )
      .doOnNext { libraryListIsRefreshing.postValue(false) }
      .subscribeOn(Schedulers.io())
      .subscribe(
          libraryItems::postValue,
          Throwable::printStackTrace
      )

  private fun updateLanguagesInDao(
    library: Flowable<LibraryNetworkEntity>
  ) = library
      .subscribeOn(Schedulers.io())
      .map { it.books }
      .withLatestFrom(
          languageDao.allLanguages(),
          BiFunction(this::combineToLanguageList)
      )
      .subscribe(
          languageDao::saveFilteredLanguages,
          Throwable::printStackTrace
      )

  private fun combineToLanguageList(
    booksFromNetwork: List<Book>,
    allLanguages: List<Language>
  ): List<Language> {
    val networkLanguageCounts = booksFromNetwork.mapNotNull { it.language }
        .fold(
            mutableMapOf<String, Int>(),
            { acc, language -> acc.increment(language) }
        )
    return when {
      booksFromNetwork.isEmpty() && allLanguages.isEmpty() -> defaultLanguage()
      booksFromNetwork.isEmpty() && allLanguages.isNotEmpty() -> allLanguages
      booksFromNetwork.isNotEmpty() && allLanguages.isEmpty() ->
        fromLocalesWithNetworkMatchesSetActiveBy(networkLanguageCounts, defaultLanguage())
      booksFromNetwork.isNotEmpty() && allLanguages.isNotEmpty() ->
        fromLocalesWithNetworkMatchesSetActiveBy(networkLanguageCounts, allLanguages)
      else -> throw RuntimeException("Impossible state")
    }
  }

  private fun <K> MutableMap<K, Int>.increment(key: K) =
    apply { set(key, getOrElse(key, { 0 }) + 1) }

  private fun fromLocalesWithNetworkMatchesSetActiveBy(
    networkLanguageCounts: MutableMap<String, Int>,
    listToActivateBy: List<Language>
  ): List<Language> {
    return Locale.getISOLanguages()
        .map { Locale(it) }
        .filter { networkLanguageCounts.containsKey(it.isO3Language) }
        .map { locale ->
          Language(
              locale.isO3Language,
              languageIsActive(listToActivateBy, locale),
              networkLanguageCounts.getOrElse(locale.isO3Language, { 0 })
          )
        }
  }

  private fun defaultLanguage() =
    listOf(
        Language(
            context.resources.configuration.locale.isO3Language,
            true,
            1
        )
    )

  private fun languageIsActive(
    allLanguages: List<Language>,
    locale: Locale
  ) = allLanguages.firstOrNull { it.languageCode == locale.isO3Language }?.active == true

  private fun combineLibrarySources(
    booksOnFileSystem: List<Book>,
    activeDownloads: List<DownloadModel>,
    allLanguages: List<Language>,
    libraryNetworkEntity: LibraryNetworkEntity,
    filter: String
  ): List<LibraryListItem> {
    val downloadedBooksIds = booksOnFileSystem.map { it.id }
    val downloadingBookIds = activeDownloads.map { it.book.id }
    val activeLanguageCodes = allLanguages.filter(Language::active)
        .map { it.languageCode }
    val booksUnfilteredByLanguage =
      applyUserFilter(
          libraryNetworkEntity.books
              .filterNot { downloadedBooksIds.contains(it.id) }
              .filterNot { downloadingBookIds.contains(it.id) }
              .filterNot { it.url.contains("/stack_exchange/") },// Temp filter see #694, filter)
          filter
      )

    return listOf(
        *createLibrarySection(
            booksUnfilteredByLanguage.filter { activeLanguageCodes.contains(it.language) },
            R.string.your_languages,
            Long.MAX_VALUE
        ),
        *createLibrarySection(
            booksUnfilteredByLanguage.filterNot { activeLanguageCodes.contains(it.language) },
            R.string.other_languages,
            Long.MIN_VALUE
        )
    )
  }

  private fun createLibrarySection(
    books: List<Book>,
    sectionStringId: Int,
    sectionId: Long
  ) = if (books.isNotEmpty()) {
    arrayOf(
        DividerItem(sectionId, context.getString(sectionStringId)),
        *toBookItems(books)
    )
  } else {
    emptyArray()
  }

  private fun applyUserFilter(
    booksUnfilteredByLanguage: List<Book>,
    filter: String
  ) = if (filter.isEmpty()) {
    booksUnfilteredByLanguage
  } else {
    booksUnfilteredByLanguage.forEach { it.calculateSearchMatches(filter, bookUtils) }
    booksUnfilteredByLanguage.filter { it.searchMatches > 0 }
  }

  private fun toBookItems(books: List<Book>) =
    books.map { BookItem(it) }.toTypedArray()

  private fun checkFileSystemForBooksOnRequest(booksFromDao: Flowable<List<Book>>) =
    requestFileSystemCheck
        .subscribeOn(Schedulers.io())
        .observeOn(Schedulers.io())
        .onBackpressureDrop()
        .doOnNext { deviceListIsRefreshing.postValue(true) }
        .switchMap(
            {
              updateBookDaoFromFilesystem(booksFromDao)
            },
            1
        )
        .onBackpressureDrop()
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
  ): List<Book> {
    val idsInDao = booksFromDao.map { it.id }
    return booksFromFileSystem.filterNot { idsInDao.contains(it.id) }
  }

  private fun updateBookItems(booksFromDao: Flowable<List<Book>>) =
    booksFromDao
        .subscribe(
            bookItems::postValue,
            Throwable::printStackTrace
        )

  private fun removeCompletedDownloadsFromDb(downloadStatuses: Flowable<List<DownloadStatus>>) =
    downloadStatuses
        .observeOn(Schedulers.io())
        .subscribeOn(Schedulers.io())
        .map { it.filter { status -> status.state == Successful } }
        .subscribe(
            {
              bookDao.saveBooks(it.map { downloadStatus -> downloadStatus.toBook() })
              downloadDao.delete(
                  *it.map { status -> status.downloadId }.toTypedArray()
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


