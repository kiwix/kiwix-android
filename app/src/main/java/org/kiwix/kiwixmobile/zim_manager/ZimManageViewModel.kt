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

package org.kiwix.kiwixmobile.zim_manager

import android.app.Application
import android.net.ConnectivityManager
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function6
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.StorageObserver
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.dao.FetchDownloadDao
import org.kiwix.kiwixmobile.core.dao.NewBookDao
import org.kiwix.kiwixmobile.core.dao.NewLanguagesDao
import org.kiwix.kiwixmobile.core.data.DataSource
import org.kiwix.kiwixmobile.core.data.remote.KiwixService
import org.kiwix.kiwixmobile.core.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity.Book
import org.kiwix.kiwixmobile.core.extensions.calculateSearchMatches
import org.kiwix.kiwixmobile.core.extensions.registerReceiver
import org.kiwix.kiwixmobile.core.utils.BookUtils
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.zim_manager.Language
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.SelectionMode.MULTI
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.SelectionMode.NORMAL
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.BookOnDisk
import org.kiwix.kiwixmobile.zim_manager.Fat32Checker.FileSystemState
import org.kiwix.kiwixmobile.zim_manager.NetworkState.CONNECTED
import org.kiwix.kiwixmobile.zim_manager.ZimManageViewModel.FileSelectActions.MultiModeFinished
import org.kiwix.kiwixmobile.zim_manager.ZimManageViewModel.FileSelectActions.RequestDeleteMultiSelection
import org.kiwix.kiwixmobile.zim_manager.ZimManageViewModel.FileSelectActions.RequestMultiSelection
import org.kiwix.kiwixmobile.zim_manager.ZimManageViewModel.FileSelectActions.RequestNavigateTo
import org.kiwix.kiwixmobile.zim_manager.ZimManageViewModel.FileSelectActions.RequestSelect
import org.kiwix.kiwixmobile.zim_manager.ZimManageViewModel.FileSelectActions.RequestShareMultiSelection
import org.kiwix.kiwixmobile.zim_manager.ZimManageViewModel.FileSelectActions.RestartActionMode
import org.kiwix.kiwixmobile.zim_manager.ZimManageViewModel.FileSelectActions.UserClickedDownloadBooksButton
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.FileSelectListState
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.effects.DeleteFiles
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.effects.NavigateToDownloads
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.effects.None
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.effects.OpenFileWithNavigation
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.effects.ShareFiles
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.effects.StartMultiSelection
import org.kiwix.kiwixmobile.zim_manager.library_view.adapter.LibraryListItem
import org.kiwix.kiwixmobile.zim_manager.library_view.adapter.LibraryListItem.BookItem
import org.kiwix.kiwixmobile.zim_manager.library_view.adapter.LibraryListItem.DividerItem
import org.kiwix.kiwixmobile.zim_manager.library_view.adapter.LibraryListItem.LibraryDownloadItem
import java.util.LinkedList
import java.util.Locale
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.inject.Inject

class ZimManageViewModel @Inject constructor(
  private val downloadDao: FetchDownloadDao,
  private val bookDao: NewBookDao,
  private val languageDao: NewLanguagesDao,
  private val storageObserver: StorageObserver,
  private val kiwixService: KiwixService,
  private val context: Application,
  private val connectivityBroadcastReceiver: ConnectivityBroadcastReceiver,
  private val bookUtils: BookUtils,
  private val fat32Checker: Fat32Checker,
  private val defaultLanguageProvider: DefaultLanguageProvider,
  private val dataSource: DataSource,
  private val connectivityManager: ConnectivityManager,
  private val sharedPreferenceUtil: SharedPreferenceUtil
) : ViewModel() {
  sealed class FileSelectActions {
    data class RequestNavigateTo(val bookOnDisk: BookOnDisk) : FileSelectActions()
    data class RequestSelect(val bookOnDisk: BookOnDisk) : FileSelectActions()
    data class RequestMultiSelection(val bookOnDisk: BookOnDisk) : FileSelectActions()
    object RequestDeleteMultiSelection : FileSelectActions()
    object RequestShareMultiSelection : FileSelectActions()
    object MultiModeFinished : FileSelectActions()
    object RestartActionMode : FileSelectActions()
    object UserClickedDownloadBooksButton : FileSelectActions()
  }

  val sideEffects = PublishProcessor.create<SideEffect<Any?>>()
  val libraryItems: MutableLiveData<List<LibraryListItem>> = MutableLiveData()
  val fileSelectListStates: MutableLiveData<FileSelectListState> = MutableLiveData()
  val deviceListIsRefreshing = MutableLiveData<Boolean>()
  val libraryListIsRefreshing = MutableLiveData<Boolean>()
  val shouldShowWifiOnlyDialog = MutableLiveData<Boolean>()
  val networkStates = MutableLiveData<NetworkState>()

  val requestFileSystemCheck = PublishProcessor.create<Unit>()
  val fileSelectActions = PublishProcessor.create<FileSelectActions>()
  val requestDownloadLibrary = BehaviorProcessor.createDefault(Unit)
  val requestFiltering = BehaviorProcessor.createDefault("")

  private var compositeDisposable: CompositeDisposable? = CompositeDisposable()

  init {
    compositeDisposable?.addAll(*disposables())
    context.registerReceiver(connectivityBroadcastReceiver)
  }

  @VisibleForTesting
  fun onClearedExposed() {
    onCleared()
  }

  override fun onCleared() {
    compositeDisposable?.clear()
    context.unregisterReceiver(connectivityBroadcastReceiver)
    connectivityBroadcastReceiver.stopNetworkState()
    requestFileSystemCheck.onComplete()
    fileSelectActions.onComplete()
    requestDownloadLibrary.onComplete()
    compositeDisposable = null
    super.onCleared()
  }

  private fun disposables(): Array<Disposable> {
    val downloads = downloadDao.downloads()
    val booksFromDao = books()
    val networkLibrary = PublishProcessor.create<LibraryNetworkEntity>()
    val languages = languageDao.languages()
    return arrayOf(
      updateBookItems(),
      checkFileSystemForBooksOnRequest(booksFromDao),
      updateLibraryItems(booksFromDao, downloads, networkLibrary, languages),
      updateLanguagesInDao(networkLibrary, languages),
      updateNetworkStates(),
      requestsAndConnectivtyChangesToLibraryRequests(networkLibrary),
      fileSelectActions()
    )
  }

  private fun fileSelectActions() = fileSelectActions.subscribe({
    sideEffects.offer(
      when (it) {
        is RequestNavigateTo -> OpenFileWithNavigation(it.bookOnDisk)
        is RequestMultiSelection -> startMultiSelectionAndSelectBook(it.bookOnDisk)
        RequestDeleteMultiSelection -> DeleteFiles(selectionsFromState())
        RequestShareMultiSelection -> ShareFiles(selectionsFromState())
        MultiModeFinished -> noSideEffectAndClearSelectionState()
        is RequestSelect -> noSideEffectSelectBook(it.bookOnDisk)
        RestartActionMode -> StartMultiSelection(fileSelectActions)
        UserClickedDownloadBooksButton -> NavigateToDownloads
      }
    )
  }, Throwable::printStackTrace)

  private fun startMultiSelectionAndSelectBook(
    bookOnDisk: BookOnDisk
  ): StartMultiSelection {
    fileSelectListStates.value?.let {
      fileSelectListStates.postValue(
        it.copy(
          bookOnDiskListItems = selectBook(it, bookOnDisk),
          selectionMode = MULTI
        )
      )
    }
    return StartMultiSelection(fileSelectActions)
  }

  private fun selectBook(
    it: FileSelectListState,
    bookOnDisk: BookOnDisk
  ): List<BooksOnDiskListItem> {
    return it.bookOnDiskListItems.map { listItem ->
      if (listItem.id == bookOnDisk.id) listItem.apply { isSelected = !isSelected }
      else listItem
    }
  }

  private fun noSideEffectSelectBook(bookOnDisk: BookOnDisk): SideEffect<Unit> {
    fileSelectListStates.value?.let {
      fileSelectListStates.postValue(
        it.copy(bookOnDiskListItems = selectBook(it, bookOnDisk))
      )
    }
    return None
  }

  private fun selectionsFromState() = fileSelectListStates.value?.selectedBooks ?: emptyList()

  private fun noSideEffectAndClearSelectionState(): SideEffect<Unit> {
    fileSelectListStates.value?.let {
      fileSelectListStates.postValue(
        it.copy(
          bookOnDiskListItems = it.bookOnDiskListItems.map { booksOnDiskListItem ->
            booksOnDiskListItem.apply { isSelected = false }
          },
          selectionMode = NORMAL
        )
      )
    }
    return None
  }

  private fun requestsAndConnectivtyChangesToLibraryRequests(
    library: PublishProcessor<LibraryNetworkEntity>
  ) =
    Flowable.combineLatest(
      requestDownloadLibrary,
      connectivityBroadcastReceiver.networkStates.distinctUntilChanged().filter(
        CONNECTED::equals
      ),
      BiFunction<Unit, NetworkState, Unit> { _, _ -> }
    )
      .switchMap {
        if (connectivityManager.activeNetworkInfo?.type == ConnectivityManager.TYPE_WIFI) {
          Flowable.just(Unit)
        } else {
          sharedPreferenceUtil.prefWifiOnlys
            .doOnNext {
              if (it) {
                shouldShowWifiOnlyDialog.postValue(true)
              }
            }
            .filter { !it }
            .map { }
        }
      }
      .subscribeOn(Schedulers.io())
      .observeOn(Schedulers.io())
      .subscribe(
        {
          kiwixService.library
            .retry(5)
            .subscribe(library::onNext) {
              it.printStackTrace()
              library.onNext(LibraryNetworkEntity().apply { book = LinkedList() })
            }
        },
        Throwable::printStackTrace
      )

  private fun updateNetworkStates() =
    connectivityBroadcastReceiver.networkStates.subscribe(
      networkStates::postValue, Throwable::printStackTrace
    )

  private fun updateLibraryItems(
    booksFromDao: Flowable<List<BookOnDisk>>,
    downloads: Flowable<List<DownloadModel>>,
    library: Flowable<LibraryNetworkEntity>,
    languages: Flowable<List<Language>>
  ) = Flowable.combineLatest(
    booksFromDao,
    downloads,
    languages.filter { it.isNotEmpty() },
    library,
    Flowable.merge(
      Flowable.just(""),
      requestFiltering
        .doOnNext { libraryListIsRefreshing.postValue(true) }
        .debounce(500, MILLISECONDS)
        .observeOn(Schedulers.io())
    ),
    fat32Checker.fileSystemStates,
    Function6(::combineLibrarySources)
  )
    .doOnNext { libraryListIsRefreshing.postValue(false) }
    .subscribeOn(Schedulers.io())
    .subscribe(
      libraryItems::postValue,
      Throwable::printStackTrace
    )

  private fun updateLanguagesInDao(
    library: Flowable<LibraryNetworkEntity>,
    languages: Flowable<List<Language>>
  ) = library
    .subscribeOn(Schedulers.io())
    .map { it.books }
    .withLatestFrom(
      languages,
      BiFunction(::combineToLanguageList)
    )
    .map { it.sortedBy(Language::language) }
    .filter { it.isNotEmpty() }
    .subscribe(
      languageDao::insert,
      Throwable::printStackTrace
    )

  private fun combineToLanguageList(
    booksFromNetwork: List<Book>,
    allLanguages: List<Language>
  ) = when {
    booksFromNetwork.isEmpty() && allLanguages.isEmpty() -> defaultLanguage()
    booksFromNetwork.isEmpty() && allLanguages.isNotEmpty() -> emptyList()
    booksFromNetwork.isNotEmpty() && allLanguages.isEmpty() ->
      fromLocalesWithNetworkMatchesSetActiveBy(
        networkLanguageCounts(booksFromNetwork), defaultLanguage()
      )
    booksFromNetwork.isNotEmpty() && allLanguages.isNotEmpty() ->
      fromLocalesWithNetworkMatchesSetActiveBy(
        networkLanguageCounts(booksFromNetwork), allLanguages
      )
    else -> throw RuntimeException("Impossible state")
  }

  private fun networkLanguageCounts(booksFromNetwork: List<Book>) =
    booksFromNetwork.mapNotNull(Book::language)
      .fold(
        mutableMapOf<String, Int>(),
        { acc, language -> acc.increment(language) }
      )

  private fun <K> MutableMap<K, Int>.increment(key: K) =
    apply { set(key, getOrElse(key, { 0 }) + 1) }

  private fun fromLocalesWithNetworkMatchesSetActiveBy(
    networkLanguageCounts: MutableMap<String, Int>,
    listToActivateBy: List<Language>
  ) = Locale.getISOLanguages()
    .map(::Locale)
    .filter { networkLanguageCounts.containsKey(it.isO3Language) }
    .map { locale ->
      Language(
        locale.isO3Language,
        languageIsActive(listToActivateBy, locale),
        networkLanguageCounts.getOrElse(locale.isO3Language, { 0 })
      )
    }

  private fun defaultLanguage() =
    listOf(
      defaultLanguageProvider.provide()
    )

  private fun languageIsActive(
    allLanguages: List<Language>,
    locale: Locale
  ) = allLanguages.firstOrNull { it.languageCode == locale.isO3Language }?.active == true

  private fun combineLibrarySources(
    booksOnFileSystem: List<BookOnDisk>,
    activeDownloads: List<DownloadModel>,
    allLanguages: List<Language>,
    libraryNetworkEntity: LibraryNetworkEntity,
    filter: String,
    fileSystemState: FileSystemState
  ): List<LibraryListItem> {
    val activeLanguageCodes = allLanguages.filter(Language::active)
      .map(Language::languageCode)
    val booksUnfilteredByLanguage =
      applySearchFilter(
        libraryNetworkEntity.books - booksOnFileSystem.map(BookOnDisk::book),
        filter
      )

    val booksWithActiveLanguages =
      booksUnfilteredByLanguage.filter { activeLanguageCodes.contains(it.language) }
    return createLibrarySection(
      booksWithActiveLanguages,
      activeDownloads,
      fileSystemState,
      R.string.your_languages,
      Long.MAX_VALUE
    ) +
      createLibrarySection(
        booksUnfilteredByLanguage - booksWithActiveLanguages,
        activeDownloads,
        fileSystemState,
        R.string.other_languages,
        Long.MIN_VALUE
      )
  }

  private fun createLibrarySection(
    books: List<Book>,
    activeDownloads: List<DownloadModel>,
    fileSystemState: FileSystemState,
    sectionStringId: Int,
    sectionId: Long
  ) =
    if (books.isNotEmpty())
      listOf(DividerItem(sectionId, sectionStringId)) +
        books.asLibraryItems(activeDownloads, fileSystemState)
    else emptyList()

  private fun applySearchFilter(
    unDownloadedBooks: List<Book>,
    filter: String
  ) = if (filter.isEmpty()) {
    unDownloadedBooks
  } else {
    unDownloadedBooks.forEach { it.calculateSearchMatches(filter, bookUtils) }
    unDownloadedBooks.filter { it.searchMatches > 0 }
  }

  private fun List<Book>.asLibraryItems(
    activeDownloads: List<DownloadModel>,
    fileSystemState: FileSystemState
  ) = map { book ->
    activeDownloads.firstOrNull { download -> download.book == book }
      ?.let(::LibraryDownloadItem)
      ?: BookItem(book, fileSystemState)
  }

  private fun checkFileSystemForBooksOnRequest(booksFromDao: Flowable<List<BookOnDisk>>):
    Disposable =
    requestFileSystemCheck
      .subscribeOn(Schedulers.io())
      .observeOn(Schedulers.io())
      .onBackpressureDrop()
      .doOnNext { deviceListIsRefreshing.postValue(true) }
      .switchMap(
        {
          booksFromStorageNotIn(booksFromDao)
        },
        1
      )
      .onBackpressureDrop()
      .doOnNext { deviceListIsRefreshing.postValue(false) }
      .filter { it.isNotEmpty() }
      .map { it.distinctBy { bookOnDisk -> bookOnDisk.book.id } }
      .subscribe(
        bookDao::insert,
        Throwable::printStackTrace
      )

  private fun books() = bookDao.books()
    .subscribeOn(Schedulers.io())
    .map { it.sortedBy { book -> book.book.title } }

  private fun booksFromStorageNotIn(booksFromDao: Flowable<List<BookOnDisk>>) =
    storageObserver.booksOnFileSystem
      .withLatestFrom(
        booksFromDao.map { it.map { bookOnDisk -> bookOnDisk.book.id } },
        BiFunction(::removeBooksAlreadyInDao)
      )

  private fun removeBooksAlreadyInDao(
    booksFromFileSystem: Collection<BookOnDisk>,
    idsInDao: List<String>
  ) = booksFromFileSystem.filterNot { idsInDao.contains(it.book.id) }

  private fun updateBookItems() =
    dataSource.booksOnDiskAsListItems()
      .subscribe(
        { newList ->
          fileSelectListStates.postValue(
            fileSelectListStates.value?.let { inheritSelections(it, newList) }
              ?: FileSelectListState(newList)
          )
        },
        Throwable::printStackTrace
      )

  private fun inheritSelections(
    oldState: FileSelectListState,
    newList: MutableList<BooksOnDiskListItem>
  ): FileSelectListState {
    return oldState.copy(
      bookOnDiskListItems = newList.map { newBookOnDisk ->
        val firstOrNull =
          oldState.bookOnDiskListItems.firstOrNull { oldBookOnDisk ->
            oldBookOnDisk.id == newBookOnDisk.id
          }
        newBookOnDisk.apply { isSelected = firstOrNull?.isSelected ?: false }
      })
  }
}
