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

package org.kiwix.kiwixmobile.zimManager

import android.app.Application
import android.net.ConnectivityManager
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.BASIC
import okhttp3.logging.HttpLoggingInterceptor.Level.NONE
import org.kiwix.kiwixmobile.BuildConfig.DEBUG
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.StorageObserver
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.convertToLocal
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.isWifi
import org.kiwix.kiwixmobile.core.dao.DownloadRoomDao
import org.kiwix.kiwixmobile.core.dao.NewBookDao
import org.kiwix.kiwixmobile.core.dao.NewLanguagesDao
import org.kiwix.kiwixmobile.core.data.DataSource
import org.kiwix.kiwixmobile.core.data.remote.KiwixService
import org.kiwix.kiwixmobile.core.data.remote.KiwixService.Companion.OPDS_LIBRARY_NETWORK_PATH
import org.kiwix.kiwixmobile.core.data.remote.ProgressResponseBody
import org.kiwix.kiwixmobile.core.data.remote.UserAgentInterceptor
import org.kiwix.kiwixmobile.core.di.modules.CALL_TIMEOUT
import org.kiwix.kiwixmobile.core.di.modules.CONNECTION_TIMEOUT
import org.kiwix.kiwixmobile.core.di.modules.KIWIX_OPDS_LIBRARY_URL
import org.kiwix.kiwixmobile.core.di.modules.READ_TIMEOUT
import org.kiwix.kiwixmobile.core.di.modules.USER_AGENT
import org.kiwix.kiwixmobile.core.downloader.downloadManager.DEFAULT_INT_VALUE
import org.kiwix.kiwixmobile.core.downloader.downloadManager.FIVE
import org.kiwix.kiwixmobile.core.downloader.downloadManager.ZERO
import org.kiwix.kiwixmobile.core.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity
import org.kiwix.kiwixmobile.core.extensions.calculateSearchMatches
import org.kiwix.kiwixmobile.core.extensions.registerReceiver
import org.kiwix.kiwixmobile.core.ui.components.ONE
import org.kiwix.kiwixmobile.core.ui.components.TWO
import org.kiwix.kiwixmobile.core.utils.BookUtils
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.files.Log
import org.kiwix.kiwixmobile.core.utils.files.ScanningProgressListener
import org.kiwix.kiwixmobile.core.zim_manager.ConnectivityBroadcastReceiver
import org.kiwix.kiwixmobile.core.zim_manager.Language
import org.kiwix.kiwixmobile.core.zim_manager.NetworkState
import org.kiwix.kiwixmobile.core.zim_manager.NetworkState.CONNECTED
import org.kiwix.kiwixmobile.core.zim_manager.OnlineLibraryManager
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem.BookOnDisk
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.SelectionMode.MULTI
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.SelectionMode.NORMAL
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.MultiModeFinished
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.RequestDeleteMultiSelection
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.RequestMultiSelection
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.RequestNavigateTo
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.RequestSelect
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.RequestShareMultiSelection
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.RestartActionMode
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.UserClickedDownloadBooksButton
import org.kiwix.kiwixmobile.zimManager.fileselectView.FileSelectListState
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.DeleteFiles
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.NavigateToDownloads
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.None
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.OpenFileWithNavigation
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.ShareFiles
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.StartMultiSelection
import org.kiwix.kiwixmobile.zimManager.libraryView.adapter.LibraryListItem
import org.kiwix.kiwixmobile.zimManager.libraryView.adapter.LibraryListItem.BookItem
import org.kiwix.kiwixmobile.zimManager.libraryView.adapter.LibraryListItem.DividerItem
import org.kiwix.kiwixmobile.zimManager.libraryView.adapter.LibraryListItem.LibraryDownloadItem
import java.util.Locale
import java.util.concurrent.TimeUnit.SECONDS
import javax.inject.Inject

const val DEFAULT_PROGRESS = 0
const val MAX_PROGRESS = 100

const val THREE = 3
const val FOUR = 4

class ZimManageViewModel @Inject constructor(
  private val downloadDao: DownloadRoomDao,
  private val bookDao: NewBookDao,
  private val languageDao: NewLanguagesDao,
  private val storageObserver: StorageObserver,
  private var kiwixService: KiwixService,
  val context: Application,
  private val connectivityBroadcastReceiver: ConnectivityBroadcastReceiver,
  private val bookUtils: BookUtils,
  private val fat32Checker: Fat32Checker,
  private val defaultLanguageProvider: DefaultLanguageProvider,
  private val dataSource: DataSource,
  private val connectivityManager: ConnectivityManager,
  private val sharedPreferenceUtil: SharedPreferenceUtil,
  private val onlineLibraryManager: OnlineLibraryManager
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

  private var isUnitTestCase: Boolean = false
  val sideEffects: MutableSharedFlow<SideEffect<*>> = MutableSharedFlow()
  private val _libraryItems = MutableStateFlow<List<LibraryListItem>>(emptyList())
  val libraryItems: StateFlow<List<LibraryListItem>> = _libraryItems.asStateFlow()
  val fileSelectListStates: MutableLiveData<FileSelectListState> = MutableLiveData()
  val deviceListScanningProgress = MutableLiveData<Int>()
  val libraryListIsRefreshing = MutableLiveData<Boolean>()
  val onlineLibraryDownloading = MutableStateFlow(false)
  val shouldShowWifiOnlyDialog = MutableLiveData<Boolean>()
  val networkStates = MutableLiveData<NetworkState>()
  val networkLibrary = MutableSharedFlow<List<org.kiwix.libkiwix.Book>>(replay = 0)
  val requestFileSystemCheck = MutableSharedFlow<Unit>(replay = 0)
  val fileSelectActions = MutableSharedFlow<FileSelectActions>()
  private val requestDownloadLibrary = MutableSharedFlow<Unit>(
    replay = 0,
    extraBufferCapacity = 1,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
  )

  @Volatile
  var isOnlineLibraryDownloading = false
  val requestFiltering = MutableStateFlow("")
  val onlineBooksSearchedQuery = MutableLiveData<String>()
  private val coroutineJobs: MutableList<Job> = mutableListOf()
  val downloadProgress = MutableLiveData<String>()

  private lateinit var alertDialogShower: AlertDialogShower

  init {
    observeCoroutineFlows()
    context.registerReceiver(connectivityBroadcastReceiver)
  }

  fun requestOnlineLibraryIfNeeded(isExplicitRefresh: Boolean) {
    if (isOnlineLibraryDownloading && !isExplicitRefresh) return
    isOnlineLibraryDownloading = true
    viewModelScope.launch {
      requestDownloadLibrary.tryEmit(Unit)
    }
  }

  fun setIsUnitTestCase() {
    isUnitTestCase = true
  }

  fun setAlertDialogShower(alertDialogShower: AlertDialogShower) {
    this.alertDialogShower = alertDialogShower
  }

  private fun createKiwixServiceWithProgressListener(): KiwixService {
    if (isUnitTestCase) return kiwixService
    val contentLength = getContentLengthOfLibraryXmlFile()
    val customOkHttpClient =
      OkHttpClient().newBuilder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(CONNECTION_TIMEOUT, SECONDS)
        .readTimeout(READ_TIMEOUT, SECONDS)
        .callTimeout(CALL_TIMEOUT, SECONDS)
        .addNetworkInterceptor(
          HttpLoggingInterceptor().apply {
            level = if (DEBUG) BASIC else NONE
          }
        )
        .addNetworkInterceptor(UserAgentInterceptor(USER_AGENT))
        .addNetworkInterceptor { chain ->
          val originalResponse = chain.proceed(chain.request())
          originalResponse.body?.let { responseBody ->
            originalResponse.newBuilder()
              .body(
                ProgressResponseBody(
                  responseBody,
                  appProgressListener,
                  contentLength
                )
              )
              .build()
          } ?: originalResponse
        }
        .build()
    return KiwixService.ServiceCreator.newHackListService(
      customOkHttpClient,
      KIWIX_OPDS_LIBRARY_URL
    )
      .also {
        kiwixService = it
      }
  }

  private var appProgressListener: AppProgressListenerProvider? = AppProgressListenerProvider(this)

  private fun getContentLengthOfLibraryXmlFile(): Long {
    val headRequest =
      Request.Builder()
        .url("$KIWIX_OPDS_LIBRARY_URL$OPDS_LIBRARY_NETWORK_PATH")
        .head()
        .header("Accept-Encoding", "identity")
        .build()
    val client =
      OkHttpClient().newBuilder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(CONNECTION_TIMEOUT, SECONDS)
        .readTimeout(READ_TIMEOUT, SECONDS)
        .callTimeout(CALL_TIMEOUT, SECONDS)
        .addNetworkInterceptor(UserAgentInterceptor(USER_AGENT))
        .build()
    try {
      client.newCall(headRequest).execute().use { response ->
        if (response.isSuccessful) {
          return@getContentLengthOfLibraryXmlFile response.header("content-length")?.toLongOrNull()
            ?: DEFAULT_INT_VALUE.toLong()
        }
      }
    } catch (_: Exception) {
      // do nothing
    }
    return DEFAULT_INT_VALUE.toLong()
  }

  @VisibleForTesting
  fun onClearedExposed() {
    onCleared()
  }

  private fun observeCoroutineFlows(dispatcher: CoroutineDispatcher = Dispatchers.IO) {
    val downloads = downloadDao.downloads()
    val booksFromDao = books()
    val languages = languageDao.languages()
    coroutineJobs.apply {
      add(scanBooksFromStorage(dispatcher))
      add(updateBookItems())
      add(fileSelectActions())
      add(updateLibraryItems(booksFromDao, downloads, networkLibrary, languages))
      add(updateLanguagesInDao(networkLibrary, languages))
      add(updateNetworkStates())
      add(requestsAndConnectivityChangesToLibraryRequests(networkLibrary))
    }
  }

  override fun onCleared() {
    coroutineJobs.forEach {
      it.cancel()
    }
    coroutineJobs.clear()
    context.unregisterReceiver(connectivityBroadcastReceiver)
    appProgressListener = null
    super.onCleared()
  }

  private fun scanBooksFromStorage(dispatcher: CoroutineDispatcher = Dispatchers.IO) =
    checkFileSystemForBooksOnRequest(books())
      .catch { it.printStackTrace() }
      .onEach { books -> bookDao.insert(books) }
      .flowOn(dispatcher)
      .launchIn(viewModelScope)

  private fun fileSelectActions() =
    fileSelectActions
      .onEach { action ->
        runCatching {
          sideEffects.emit(
            when (action) {
              is RequestNavigateTo -> OpenFileWithNavigation(action.bookOnDisk)
              is RequestMultiSelection -> startMultiSelectionAndSelectBook(action.bookOnDisk)
              RequestDeleteMultiSelection -> DeleteFiles(selectionsFromState(), alertDialogShower)
              RequestShareMultiSelection -> ShareFiles(selectionsFromState())
              MultiModeFinished -> noSideEffectAndClearSelectionState()
              is RequestSelect -> noSideEffectSelectBook(action.bookOnDisk)
              RestartActionMode -> StartMultiSelection(fileSelectActions)
              UserClickedDownloadBooksButton -> NavigateToDownloads
            }
          )
        }.onFailure {
          it.printStackTrace()
        }
      }.launchIn(viewModelScope)

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
      if (listItem.id == bookOnDisk.id) {
        listItem.apply { isSelected = !isSelected }
      } else {
        listItem
      }
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

  private fun selectionsFromState() = fileSelectListStates.value?.selectedBooks.orEmpty()

  private fun noSideEffectAndClearSelectionState(): SideEffect<Unit> {
    fileSelectListStates.value?.let {
      fileSelectListStates.postValue(
        it.copy(
          bookOnDiskListItems =
            it.bookOnDiskListItems.map { booksOnDiskListItem ->
              booksOnDiskListItem.apply { isSelected = false }
            },
          selectionMode = NORMAL
        )
      )
    }
    return None
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  private fun requestsAndConnectivityChangesToLibraryRequests(
    library: MutableSharedFlow<List<org.kiwix.libkiwix.Book>>,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
  ) = requestDownloadLibrary.flatMapConcat {
    connectivityBroadcastReceiver.networkStates
      .filter { networkState -> networkState == CONNECTED }
      .take(1)
      .flatMapConcat {
        shouldProceedWithDownload()
          .flatMapConcat { kiwixService ->
            downloadLibraryFlow(kiwixService).also {
              onlineLibraryDownloading.tryEmit(true)
            }
          }
      }
  }
    .filterNotNull()
    .catch {
      it.printStackTrace().also {
        isOnlineLibraryDownloading = false
        onlineLibraryDownloading.tryEmit(false)
      }
    }
    .onEach {
      library.emit(it).also {
        // Setting this to true because once library downloaded we don't need to download again
        // until user wants to refresh the online library.
        isOnlineLibraryDownloading = true
        onlineLibraryDownloading.tryEmit(false)
      }
    }
    .flowOn(dispatcher)
    .launchIn(viewModelScope)

  private fun shouldProceedWithDownload(): Flow<KiwixService> {
    return if (connectivityManager.isWifi()) {
      flowOf(createKiwixServiceWithProgressListener())
    } else {
      flow {
        val wifiOnly = sharedPreferenceUtil.prefWifiOnlys.first()
        if (wifiOnly) {
          shouldShowWifiOnlyDialog.postValue(true)
          // Don't emit anything — just return
          return@flow
        }
        emit(createKiwixServiceWithProgressListener())
      }
    }
  }

  private fun downloadLibraryFlow(
    kiwixService: KiwixService
  ): Flow<List<org.kiwix.libkiwix.Book>> = flow {
    downloadProgress.postValue(context.getString(R.string.starting_downloading_remote_library))
    val response = kiwixService.getLibrary()
    downloadProgress.postValue(context.getString(R.string.parsing_remote_library))
    onlineLibraryManager.parseOPDSStream(response, KIWIX_OPDS_LIBRARY_URL)
    emit(onlineLibraryManager.getOnlineBooks())
  }
    .retry(5)
    .catch { e ->
      e.printStackTrace()
      emit(emptyList())
    }

  private fun updateNetworkStates() = connectivityBroadcastReceiver.networkStates
    .onEach { state -> networkStates.postValue(state) }
    .launchIn(viewModelScope)

  @Suppress("UNCHECKED_CAST")
  @OptIn(FlowPreview::class)
  private fun updateLibraryItems(
    booksFromDao: Flow<List<BookOnDisk>>,
    downloads: Flow<List<DownloadModel>>,
    library: MutableSharedFlow<List<org.kiwix.libkiwix.Book>>,
    languages: Flow<List<Language>>,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
  ) = viewModelScope.launch(dispatcher) {
    val requestFilteringFlow = merge(
      flowOf(""),
      requestFiltering
        .onEach { libraryListIsRefreshing.postValue(true) }
        .debounce(500)
        .flowOn(dispatcher)
    )

    combine(
      booksFromDao,
      downloads,
      languages.filter { it.isNotEmpty() },
      library,
      requestFilteringFlow,
      fat32Checker.fileSystemStates
    ) { args ->
      val books = args[ZERO] as List<BookOnDisk>
      val activeDownloads = args[ONE] as List<DownloadModel>
      val languageList = args[TWO] as List<Language>
      val libraryNetworkEntity = args[THREE] as List<org.kiwix.libkiwix.Book>
      val filter = args[FOUR] as String
      val fileSystemState = args[FIVE] as FileSystemState
      combineLibrarySources(
        booksOnFileSystem = books,
        activeDownloads = activeDownloads,
        allLanguages = languageList,
        libraryNetworkEntity = libraryNetworkEntity,
        filter = filter,
        fileSystemState = fileSystemState
      )
    }
      .onEach { libraryListIsRefreshing.postValue(false) }
      .catch { throwable ->
        libraryListIsRefreshing.postValue(false)
        throwable.printStackTrace()
        Log.e("ZimManageViewModel", "Error----$throwable")
      }
      .collect { _libraryItems.emit(it) }
  }

  private fun updateLanguagesInDao(
    library: MutableSharedFlow<List<org.kiwix.libkiwix.Book>>,
    languages: Flow<List<Language>>,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
  ) =
    combine(
      library,
      languages
    ) { books, existingLanguages ->
      combineToLanguageList(books, existingLanguages)
    }.map { it.sortedBy(Language::language) }
      .filter { it.isNotEmpty() }
      .distinctUntilChanged()
      .catch { it.printStackTrace() }
      .onEach { languageDao.insert(it) }
      .flowOn(dispatcher)
      .launchIn(viewModelScope)

  private fun combineToLanguageList(
    booksFromNetwork: List<org.kiwix.libkiwix.Book>,
    allLanguages: List<Language>
  ) = when {
    booksFromNetwork.isEmpty() -> {
      if (allLanguages.isEmpty()) {
        defaultLanguage()
      } else {
        emptyList()
      }
    }

    allLanguages.isEmpty() ->
      fromLocalesWithNetworkMatchesSetActiveBy(
        networkLanguageCounts(booksFromNetwork),
        defaultLanguage()
      )

    else ->
      fromLocalesWithNetworkMatchesSetActiveBy(
        networkLanguageCounts(booksFromNetwork),
        allLanguages
      )
  }

  private fun networkLanguageCounts(booksFromNetwork: List<org.kiwix.libkiwix.Book>) =
    booksFromNetwork.mapNotNull { it.language }
      .fold(
        mutableMapOf<String, Int>()
      ) { acc, language -> acc.increment(language) }

  private fun <K> MutableMap<K, Int>.increment(key: K) =
    apply { set(key, getOrElse(key) { 0 } + 1) }

  private fun fromLocalesWithNetworkMatchesSetActiveBy(
    networkLanguageCounts: MutableMap<String, Int>,
    listToActivateBy: List<Language>
  ) = Locale.getISOLanguages()
    .map { it.convertToLocal() }
    .filter { networkLanguageCounts.containsKey(it.isO3Language) }
    .map { locale ->
      Language(
        locale.isO3Language,
        languageIsActive(listToActivateBy, locale),
        networkLanguageCounts.getOrElse(locale.isO3Language) { 0 }
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

  @Suppress("UnsafeCallOnNullableType")
  private fun combineLibrarySources(
    booksOnFileSystem: List<BookOnDisk>,
    activeDownloads: List<DownloadModel>,
    allLanguages: List<Language>,
    onlineBooks: List<org.kiwix.libkiwix.Book>,
    filter: String,
    fileSystemState: FileSystemState
  ): List<LibraryListItem> {
    val activeLanguageCodes =
      allLanguages.filter(Language::active)
        .map(Language::languageCode)
    val allBooks = onlineBooks - booksOnFileSystem.map(BookOnDisk::book).toSet()
    val downloadingBooks =
      activeDownloads.mapNotNull { download ->
        allBooks.firstOrNull { it.id == download.book.id }
      }
    val booksUnfilteredByLanguage =
      applySearchFilter(
        allBooks - downloadingBooks.toSet(),
        filter
      )

    val booksWithActiveLanguages =
      booksUnfilteredByLanguage.filter { activeLanguageCodes.contains(it.language) }
    val booksWithoutActiveLanguages = booksUnfilteredByLanguage - booksWithActiveLanguages.toSet()
    return createLibrarySection(
      downloadingBooks,
      activeDownloads,
      fileSystemState,
      R.string.downloading,
      Long.MAX_VALUE
    ) +
      createLibrarySection(
        booksWithActiveLanguages,
        emptyList(),
        fileSystemState,
        R.string.your_languages,
        Long.MAX_VALUE - 1
      ) +
      createLibrarySection(
        booksWithoutActiveLanguages,
        emptyList(),
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
    if (books.isNotEmpty()) {
      listOf(DividerItem(sectionId, sectionStringId)) +
        books.asLibraryItems(activeDownloads, fileSystemState)
    } else {
      emptyList()
    }

  private fun applySearchFilter(
    unDownloadedBooks: List<Book>,
    filter: String
  ) = if (filter.isEmpty()) {
    unDownloadedBooks
  } else {
    unDownloadedBooks.iterator().forEach { it.calculateSearchMatches(filter, bookUtils) }
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

  @OptIn(ExperimentalCoroutinesApi::class)
  private fun checkFileSystemForBooksOnRequest(
    booksFromDao: Flow<List<BookOnDisk>>
  ): Flow<List<BookOnDisk>> = requestFileSystemCheck
    .flatMapLatest {
      // Initial progress
      deviceListScanningProgress.postValue(DEFAULT_PROGRESS)
      booksFromStorageNotIn(
        booksFromDao,
        object : ScanningProgressListener {
          override fun onProgressUpdate(scannedDirectory: Int, totalDirectory: Int) {
            val overallProgress =
              (scannedDirectory.toDouble() / totalDirectory.toDouble() * MAX_PROGRESS).toInt()
            if (overallProgress != MAX_PROGRESS) {
              deviceListScanningProgress.postValue(overallProgress)
            }
          }
        }
      )
    }
    .onEach {
      deviceListScanningProgress.postValue(MAX_PROGRESS)
    }
    .filter { it.isNotEmpty() }
    .map { books -> books.distinctBy { it.book.id } }

  private fun books() =
    bookDao.books()
      .map { it.sortedBy { book -> book.book.title } }

  private fun booksFromStorageNotIn(
    booksFromDao: Flow<List<BookOnDisk>>,
    scanningProgressListener: ScanningProgressListener
  ): Flow<List<BookOnDisk>> = flow {
    val scannedBooks = storageObserver.getBooksOnFileSystem(scanningProgressListener).first()
    val daoBookIds = booksFromDao.first().map { it.book.id }
    emit(removeBooksAlreadyInDao(scannedBooks, daoBookIds))
  }

  private fun removeBooksAlreadyInDao(
    booksFromFileSystem: Collection<BookOnDisk>,
    idsInDao: List<String>
  ) = booksFromFileSystem.filterNot { idsInDao.contains(it.book.id) }

  private fun updateBookItems() =
    dataSource.booksOnDiskAsListItems()
      .catch { it.printStackTrace() }
      .onEach { newList ->
        val currentState = fileSelectListStates.value
        val updatedState = currentState?.let {
          inheritSelections(it, newList.toMutableList())
        } ?: FileSelectListState(newList)

        fileSelectListStates.postValue(updatedState)
      }.launchIn(viewModelScope)

  private fun inheritSelections(
    oldState: FileSelectListState,
    newList: MutableList<BooksOnDiskListItem>
  ): FileSelectListState {
    return oldState.copy(
      bookOnDiskListItems =
        newList.map { newBookOnDisk ->
          val firstOrNull =
            oldState.bookOnDiskListItems.firstOrNull { oldBookOnDisk ->
              oldBookOnDisk.id == newBookOnDisk.id
            }
          newBookOnDisk.apply { isSelected = firstOrNull?.isSelected == true }
        }
    )
  }
}
