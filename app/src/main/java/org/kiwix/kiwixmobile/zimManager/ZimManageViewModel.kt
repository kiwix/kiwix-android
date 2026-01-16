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
import kotlinx.coroutines.flow.drop
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
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
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
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookOnDisk
import org.kiwix.kiwixmobile.core.data.DataSource
import org.kiwix.kiwixmobile.core.data.remote.KiwixService
import org.kiwix.kiwixmobile.core.data.remote.KiwixService.Companion.ITEMS_PER_PAGE
import org.kiwix.kiwixmobile.core.data.remote.ProgressResponseBody
import org.kiwix.kiwixmobile.core.data.remote.UserAgentInterceptor
import org.kiwix.kiwixmobile.core.di.modules.CALL_TIMEOUT
import org.kiwix.kiwixmobile.core.di.modules.CONNECTION_TIMEOUT
import org.kiwix.kiwixmobile.core.di.modules.KIWIX_OPDS_LIBRARY_URL
import org.kiwix.kiwixmobile.core.di.modules.READ_TIMEOUT
import org.kiwix.kiwixmobile.core.di.modules.USER_AGENT
import org.kiwix.kiwixmobile.core.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.core.entity.LibkiwixBook
import org.kiwix.kiwixmobile.core.extensions.registerReceiver
import org.kiwix.kiwixmobile.core.reader.integrity.ValidateZimViewModel
import org.kiwix.kiwixmobile.core.ui.components.ONE
import org.kiwix.kiwixmobile.core.ui.components.TWO
import org.kiwix.kiwixmobile.core.utils.DEFAULT_INT_VALUE
import org.kiwix.kiwixmobile.core.utils.ZERO
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.files.Log
import org.kiwix.kiwixmobile.core.utils.files.ScanningProgressListener
import org.kiwix.kiwixmobile.core.zim_manager.ConnectivityBroadcastReceiver
import org.kiwix.kiwixmobile.core.zim_manager.NetworkState
import org.kiwix.kiwixmobile.core.zim_manager.NetworkState.CONNECTED
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
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.RequestValidateZimFiles
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.RestartActionMode
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.UserClickedDownloadBooksButton
import org.kiwix.kiwixmobile.zimManager.fileselectView.FileSelectListState
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.DeleteFiles
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.NavigateToDownloads
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.None
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.OpenFileWithNavigation
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.ShareFiles
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.StartMultiSelection
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.ValidateZIMFiles
import org.kiwix.kiwixmobile.zimManager.libraryView.LibraryListItem
import org.kiwix.kiwixmobile.zimManager.libraryView.LibraryListItem.BookItem
import org.kiwix.kiwixmobile.zimManager.libraryView.LibraryListItem.DividerItem
import org.kiwix.kiwixmobile.zimManager.libraryView.LibraryListItem.LibraryDownloadItem
import org.kiwix.libkiwix.Book
import retrofit2.Response
import java.util.concurrent.TimeUnit.SECONDS
import javax.inject.Inject

const val DEFAULT_PROGRESS = 0
const val MAX_PROGRESS = 100

const val THREE = 3

class ZimManageViewModel @Inject constructor(
  private val downloadDao: DownloadRoomDao,
  private val libkiwixBookOnDisk: LibkiwixBookOnDisk,
  private val storageObserver: StorageObserver,
  private var kiwixService: KiwixService,
  val context: Application,
  private val connectivityBroadcastReceiver: ConnectivityBroadcastReceiver,
  private val fat32Checker: Fat32Checker,
  private val dataSource: DataSource,
  private val connectivityManager: ConnectivityManager,
  val onlineLibraryManager: OnlineLibraryManager,
  private val kiwixDataStore: KiwixDataStore
) : ViewModel() {
  sealed class FileSelectActions {
    data class RequestNavigateTo(val bookOnDisk: BookOnDisk) : FileSelectActions()
    data class RequestSelect(val bookOnDisk: BookOnDisk) : FileSelectActions()
    data class RequestMultiSelection(val bookOnDisk: BookOnDisk) : FileSelectActions()
    object RequestValidateZimFiles : FileSelectActions()
    object RequestDeleteMultiSelection : FileSelectActions()
    object RequestShareMultiSelection : FileSelectActions()
    object MultiModeFinished : FileSelectActions()
    object RestartActionMode : FileSelectActions()
    object UserClickedDownloadBooksButton : FileSelectActions()
  }

  data class OnlineLibraryRequest(
    val query: String? = null,
    val category: String? = null,
    val lang: String? = null,
    val isLoadMoreItem: Boolean,
    val page: Int,
    // Bug Fix #4381
    val version: Long = System.nanoTime()
  )

  data class OnlineLibraryResult(
    val onlineLibraryRequest: OnlineLibraryRequest,
    val books: List<LibkiwixBook>
  )

  data class LibraryListItemWrapper(
    val items: List<LibraryListItem>,
    val version: Long = System.nanoTime()
  )

  private lateinit var validateZimViewModel: ValidateZimViewModel

  @Suppress("InjectDispatcher")
  private var ioDispatcher: CoroutineDispatcher = Dispatchers.IO
  private var isUnitTestCase: Boolean = false
  val sideEffects: MutableSharedFlow<SideEffect<*>> = MutableSharedFlow()
  private val _libraryItems =
    MutableStateFlow<LibraryListItemWrapper>(LibraryListItemWrapper(emptyList()))
  val libraryItems: StateFlow<LibraryListItemWrapper> = _libraryItems.asStateFlow()
  val fileSelectListStates: MutableLiveData<FileSelectListState> = MutableLiveData()
  val deviceListScanningProgress = MutableLiveData<Int>()
  val libraryListIsRefreshing = MutableLiveData<Boolean>()

  private var onlineLibraryFetchingJob: Job? = null

  /**
   * Manages the showing of downloading online library progress,
   * and showing the progressBar at the end of content when loading more items.
   *
   * A [Pair] containing:
   *  - [Boolean]: When initial content is downloading.
   *  - [Boolean]: When loading more item.
   */
  val onlineLibraryDownloading = MutableStateFlow(false to false)
  val shouldShowWifiOnlyDialog = MutableLiveData<Boolean>()
  val networkStates = MutableLiveData<NetworkState>()
  val networkLibrary = MutableStateFlow<OnlineLibraryResult>(
    OnlineLibraryResult(
      OnlineLibraryRequest(
        query = null,
        category = null,
        lang = null,
        isLoadMoreItem = false,
        page = ZERO
      ),
      emptyList()
    )
  )
  val requestFileSystemCheck = MutableSharedFlow<Unit>(replay = 0)
  val fileSelectActions = MutableSharedFlow<FileSelectActions>()
  private val requestDownloadLibrary = MutableSharedFlow<OnlineLibraryRequest>(
    replay = 0,
    extraBufferCapacity = 1,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
  )
  val onlineLibraryRequest: MutableStateFlow<OnlineLibraryRequest> =
    MutableStateFlow<OnlineLibraryRequest>(
      OnlineLibraryRequest(
        query = null,
        category = null,
        lang = null,
        isLoadMoreItem = false,
        page = ZERO
      )
    )
  val requestFiltering = MutableStateFlow("")
  val onlineBooksSearchedQuery = MutableLiveData<String>()
  private val coroutineJobs: MutableList<Job> = mutableListOf()
  val downloadProgress = MutableLiveData<String>()

  private lateinit var alertDialogShower: AlertDialogShower

  init {
    observeCoroutineFlows()
    context.registerReceiver(connectivityBroadcastReceiver)
  }

  fun setValidateZimViewModel(validateZimViewModel: ValidateZimViewModel) {
    this.validateZimViewModel = validateZimViewModel
  }

  fun setIsUnitTestCase() {
    isUnitTestCase = true
  }

  fun setAlertDialogShower(alertDialogShower: AlertDialogShower) {
    this.alertDialogShower = alertDialogShower
  }

  private fun createKiwixServiceWithProgressListener(
    baseUrl: String,
    start: Int = ZERO,
    count: Int = ITEMS_PER_PAGE,
    query: String? = null,
    lang: String? = null,
    category: String? = null,
    shouldTrackProgress: Boolean
  ): KiwixService {
    if (isUnitTestCase) return kiwixService
    val contentLength =
      getContentLengthOfLibraryXmlFile(baseUrl, start, count, query, lang, category)
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
          val body = originalResponse.body
          if (shouldTrackProgress && body != null) {
            originalResponse.newBuilder()
              .body(ProgressResponseBody(body, appProgressListener, contentLength))
              .build()
          } else {
            originalResponse
          }
        }
        .build()
    return KiwixService.ServiceCreator.newHackListService(
      customOkHttpClient,
      baseUrl
    )
      .also {
        kiwixService = it
      }
  }

  private var appProgressListener: AppProgressListenerProvider? = AppProgressListenerProvider(this)

  private fun getContentLengthOfLibraryXmlFile(
    baseUrl: String,
    start: Int = ZERO,
    count: Int = ITEMS_PER_PAGE,
    query: String? = null,
    lang: String? = null,
    category: String? = null
  ): Long {
    val requestUrl =
      onlineLibraryManager.buildLibraryUrl(baseUrl, start, count, query, lang, category)
    val headRequest =
      Request.Builder()
        .url(requestUrl)
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

  private fun observeCoroutineFlows() {
    val downloads = downloadDao.downloads()
    val booksFromDao = books()
    coroutineJobs.apply {
      add(scanBooksFromStorage())
      add(updateBookItems())
      add(fileSelectActions())
      add(updateLibraryItems(booksFromDao, downloads, networkLibrary))
      add(updateNetworkStates())
      add(requestsAndConnectivityChangesToLibraryRequests(networkLibrary))
      add(onlineLibraryRequest())
      add(observeLanguageChanges())
      add(observeSearch())
      add(observeCategory())
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

  private fun observeCategory() =
    kiwixDataStore.selectedOnlineContentCategory
      .onEach {
        libraryListIsRefreshing.postValue(true)
        updateOnlineLibraryFilters(
          OnlineLibraryRequest(category = it, page = ONE, isLoadMoreItem = false)
        )
      }
      .flowOn(ioDispatcher)
      .launchIn(viewModelScope)

  @OptIn(FlowPreview::class)
  private fun observeSearch() =
    requestFiltering
      .onEach {
        updateOnlineLibraryFilters(
          OnlineLibraryRequest(query = it, page = ZERO, isLoadMoreItem = false)
        )
      }
      .debounce(500)
      .flowOn(ioDispatcher)
      .launchIn(viewModelScope)

  private fun observeLanguageChanges() =
    kiwixDataStore.selectedOnlineContentLanguage
      .onEach {
        updateOnlineLibraryFilters(
          OnlineLibraryRequest(lang = it, page = ONE, isLoadMoreItem = false)
        )
      }
      .flowOn(ioDispatcher)
      .launchIn(viewModelScope)

  fun updateOnlineLibraryFilters(newRequest: OnlineLibraryRequest) {
    onlineLibraryRequest.update { current ->
      current.copy(
        query = newRequest.query ?: current.query,
        category = newRequest.category ?: current.category,
        lang = newRequest.lang ?: current.lang,
        page = newRequest.page,
        isLoadMoreItem = newRequest.isLoadMoreItem,
        version = if (isUnitTestCase) {
          // In unit tests, we want predictable and testable values,
          // so use the provided version instead of a dynamic timestamp.
          newRequest.version
        } else {
          // Bug Fix #4381:
          // Force StateFlow to emit even if all other fields are unchanged.
          // Without this, identical requests may not trigger observers,
          // causing the UI not to refresh.
          // Using System.nanoTime() ensures a unique value each time,
          // guaranteeing that collectors receive an update.
          System.nanoTime()
        }
      )
    }
  }

  private fun onlineLibraryRequest() = onlineLibraryRequest
    .drop(1)
    .onEach { request ->
      requestDownloadLibrary.tryEmit(request)
    }
    .launchIn(viewModelScope)

  private fun scanBooksFromStorage() =
    checkFileSystemForBooksOnRequest(books())
      .catch { it.printStackTrace() }
      .onEach { books -> libkiwixBookOnDisk.insert(books) }
      .flowOn(ioDispatcher)
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
              RequestValidateZimFiles ->
                ValidateZIMFiles(selectionsFromState(), alertDialogShower, validateZimViewModel)

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

  private fun updateDownloadState(isInitial: Boolean) {
    onlineLibraryDownloading.tryEmit(isInitial to !isInitial)
  }

  private fun resetDownloadState() {
    onlineLibraryDownloading.tryEmit(false to false)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  private fun requestsAndConnectivityChangesToLibraryRequests(
    onlineLibraryResult: MutableStateFlow<OnlineLibraryResult>
  ) = requestDownloadLibrary.onEach { onlineLibraryRequest ->
    onlineLibraryFetchingJob?.cancel()

    onlineLibraryFetchingJob = viewModelScope.launch(ioDispatcher) {
      connectivityBroadcastReceiver.networkStates
        .filter { it == CONNECTED }
        .take(1)
        .flatMapConcat {
          updateDownloadState(!onlineLibraryRequest.isLoadMoreItem)
          shouldProceedWithDownload(onlineLibraryRequest)
            .flatMapConcat { kiwixService ->
              downloadLibraryFlow(kiwixService, onlineLibraryRequest)
            }
        }
        .filterNotNull()
        .catch {
          it.printStackTrace()
          resetDownloadState()
          if (onlineLibraryResult.value.books.isEmpty()) {
            onlineLibraryResult.emit(
              OnlineLibraryResult(onlineLibraryResult.value.onlineLibraryRequest, emptyList())
            )
          }
        }
        .collect { result ->
          resetDownloadState()
          val newResult = OnlineLibraryResult(
            result.onlineLibraryRequest,
            if (result.onlineLibraryRequest.isLoadMoreItem) {
              onlineLibraryResult.value.books + result.books
            } else {
              result.books
            }
          )
          onlineLibraryResult.emit(newResult)
        }
    }
  }.flowOn(ioDispatcher)
    .launchIn(viewModelScope)

  private fun shouldProceedWithDownload(onlineLibraryRequest: OnlineLibraryRequest): Flow<KiwixService> {
    val baseUrl = KIWIX_OPDS_LIBRARY_URL
    val start =
      onlineLibraryManager.getStartOffset(onlineLibraryRequest.page, ITEMS_PER_PAGE)
    val shouldTrackProgress = !onlineLibraryRequest.isLoadMoreItem
    return if (connectivityManager.isWifi()) {
      flowOf(
        createKiwixServiceWithProgressListener(
          baseUrl,
          start,
          ITEMS_PER_PAGE,
          onlineLibraryRequest.query,
          onlineLibraryRequest.lang,
          onlineLibraryRequest.category,
          shouldTrackProgress
        )
      )
    } else {
      flow {
        val wifiOnly = kiwixDataStore.wifiOnly.first()
        if (wifiOnly) {
          onlineLibraryDownloading.emit(false to false)
          shouldShowWifiOnlyDialog.postValue(true)
          // Don't emit anything — just return
          return@flow
        }
        emit(
          createKiwixServiceWithProgressListener(
            baseUrl,
            start,
            ITEMS_PER_PAGE,
            onlineLibraryRequest.query,
            onlineLibraryRequest.lang,
            onlineLibraryRequest.category,
            shouldTrackProgress
          )
        )
      }
    }
  }

  private fun downloadLibraryFlow(
    kiwixService: KiwixService,
    request: OnlineLibraryRequest
  ): Flow<OnlineLibraryResult> = flow {
    updateDownloadProgressIfNeeded(
      request,
      R.string.starting_downloading_remote_library
    )
    val start =
      onlineLibraryManager.getStartOffset(request.page, ITEMS_PER_PAGE)
    val buildUrl = onlineLibraryManager.buildLibraryUrl(
      KIWIX_OPDS_LIBRARY_URL,
      start,
      ITEMS_PER_PAGE,
      request.query,
      request.lang,
      request.category,
    )
    val response = kiwixService.getLibraryPage(buildUrl)
    val urlHost = response.getResolvedBaseUrl()
    updateDownloadProgressIfNeeded(
      request,
      R.string.parsing_remote_library
    )
    val libraryXml = response.body()
    val onlineBooks =
      onlineLibraryManager.parseOPDSStreamAndGetBooks(libraryXml, urlHost).orEmpty()
    emit(OnlineLibraryResult(request, onlineBooks))
  }
    .retry(5)
    .catch { e ->
      e.printStackTrace()
      emit(OnlineLibraryResult(request, emptyList()))
    }

  private fun updateDownloadProgressIfNeeded(request: OnlineLibraryRequest, messageResId: Int) {
    if (!request.isLoadMoreItem) {
      downloadProgress.postValue(context.getString(messageResId))
    }
  }

  private fun Response<String>.getResolvedBaseUrl(): String {
    val url = raw().networkResponse?.request?.url ?: raw().request.url
    return "${url.scheme}://${url.host}"
  }

  private fun updateNetworkStates() = connectivityBroadcastReceiver.networkStates
    .onEach { state -> networkStates.postValue(state) }
    .launchIn(viewModelScope)

  @Suppress("UNCHECKED_CAST")
  @OptIn(FlowPreview::class)
  private fun updateLibraryItems(
    localBooksFromLibkiwix: Flow<List<Book>>,
    downloads: Flow<List<DownloadModel>>,
    library: MutableStateFlow<OnlineLibraryResult>
  ) = viewModelScope.launch(ioDispatcher) {
    combine(
      localBooksFromLibkiwix,
      downloads,
      library,
      fat32Checker.fileSystemStates
    ) { args ->
      val books = args[ZERO] as List<Book>
      val activeDownloads = args[ONE] as List<DownloadModel>
      val onlineLibraryResult = args[TWO] as OnlineLibraryResult
      val fileSystemState = args[THREE] as FileSystemState
      combineLibrarySources(
        booksOnFileSystem = books,
        activeDownloads = activeDownloads,
        onlineLibraryResult = onlineLibraryResult,
        fileSystemState = fileSystemState
      )
    }
      .onEach { libraryListIsRefreshing.postValue(false) }
      .catch { throwable ->
        libraryListIsRefreshing.postValue(false)
        throwable.printStackTrace()
        Log.e("ZimManageViewModel", "Error----$throwable")
      }
      .collect { _libraryItems.emit(LibraryListItemWrapper(it)) }
  }

  @Suppress("UnsafeCallOnNullableType")
  private suspend fun combineLibrarySources(
    booksOnFileSystem: List<Book>,
    activeDownloads: List<DownloadModel>,
    onlineLibraryResult: OnlineLibraryResult,
    fileSystemState: FileSystemState
  ): List<LibraryListItem> {
    val allBooks = onlineLibraryResult.books - booksOnFileSystem.map { LibkiwixBook(it) }.toSet()
    val downloadingBooks =
      activeDownloads.map { download ->
        allBooks.firstOrNull { it.id == download.book.id } ?: download.book
      }
    val filteredBooks = allBooks - downloadingBooks.toSet()
    val selectedLanguage = kiwixDataStore.selectedOnlineContentLanguage.first()
    val onlineLibrarySectionTitle =
      if (selectedLanguage.isBlank()) {
        context.getString(R.string.all_languages)
      } else {
        context.getString(
          R.string.your_language,
          selectedLanguage.convertToLocal().displayLanguage
        )
      }
    return createLibrarySection(
      downloadingBooks,
      activeDownloads,
      fileSystemState,
      context.getString(R.string.downloading),
      Long.MAX_VALUE
    ) +
      createLibrarySection(
        filteredBooks,
        emptyList(),
        fileSystemState,
        onlineLibrarySectionTitle,
        Long.MIN_VALUE
      )
  }

  private fun createLibrarySection(
    books: List<LibkiwixBook>,
    activeDownloads: List<DownloadModel>,
    fileSystemState: FileSystemState,
    sectionTitle: String,
    sectionId: Long
  ) =
    if (books.isNotEmpty()) {
      listOf(DividerItem(sectionId, sectionTitle)) +
        books.asLibraryItems(activeDownloads, fileSystemState)
    } else {
      emptyList()
    }

  private fun List<LibkiwixBook>.asLibraryItems(
    activeDownloads: List<DownloadModel>,
    fileSystemState: FileSystemState
  ) = map { book ->
    activeDownloads.firstOrNull { download -> download.book == book }
      ?.let(::LibraryDownloadItem)
      ?: BookItem(book, fileSystemState)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  private fun checkFileSystemForBooksOnRequest(
    booksFromDao: Flow<List<Book>>
  ): Flow<List<Book>> = requestFileSystemCheck
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
    .map { books -> books.distinctBy { it.id } }

  private fun books(): Flow<List<Book>> =
    libkiwixBookOnDisk.books().map { bookOnDiskList ->
      bookOnDiskList
        .sortedBy { it.book.title }
        .mapNotNull { it.book.nativeBook }
    }

  private fun booksFromStorageNotIn(
    localBooksFromLibkiwix: Flow<List<Book>>,
    scanningProgressListener: ScanningProgressListener
  ): Flow<List<Book>> = flow {
    val scannedBooks = storageObserver.getBooksOnFileSystem(scanningProgressListener).first()
    val daoBookIds = localBooksFromLibkiwix.first().map { it.id }
    emit(removeBooksAlreadyInDao(scannedBooks, daoBookIds))
  }

  private fun removeBooksAlreadyInDao(
    booksFromFileSystem: Collection<Book>,
    idsInDao: List<String>
  ) = booksFromFileSystem.filterNot { idsInDao.contains(it.id) }

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
