/*
 * Kiwix Android
 * Copyright (c) 2025 Kiwix <android.kiwix.org>
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
package org.kiwix.kiwixmobile.nav.destination.library.online

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapConcat
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
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.convertToLocal
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.isWifi
import org.kiwix.kiwixmobile.core.dao.DownloadRoomDao
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookOnDisk
import org.kiwix.kiwixmobile.core.data.remote.KiwixService
import org.kiwix.kiwixmobile.core.data.remote.KiwixService.Companion.ITEMS_PER_PAGE
import org.kiwix.kiwixmobile.core.data.remote.ProgressResponseBody
import org.kiwix.kiwixmobile.core.data.remote.UserAgentInterceptor
import org.kiwix.kiwixmobile.core.di.IoDispatcher
import org.kiwix.kiwixmobile.core.di.OPDSKiwixService
import org.kiwix.kiwixmobile.core.di.modules.CALL_TIMEOUT
import org.kiwix.kiwixmobile.core.di.modules.CONNECTION_TIMEOUT
import org.kiwix.kiwixmobile.core.di.modules.KIWIX_OPDS_LIBRARY_URL
import org.kiwix.kiwixmobile.core.di.modules.READ_TIMEOUT
import org.kiwix.kiwixmobile.core.di.modules.USER_AGENT
import org.kiwix.kiwixmobile.core.downloader.Downloader
import org.kiwix.kiwixmobile.core.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.core.downloader.model.DownloadState
import org.kiwix.kiwixmobile.core.entity.LibkiwixBook
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.hasNotificationPermission
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.isManageExternalStoragePermissionGranted
import org.kiwix.kiwixmobile.core.ui.components.ONE
import org.kiwix.kiwixmobile.core.ui.components.TWO
import org.kiwix.kiwixmobile.core.utils.BookUtils
import org.kiwix.kiwixmobile.core.utils.DEFAULT_INT_VALUE
import org.kiwix.kiwixmobile.core.utils.EXTERNAL_SELECT_POSITION
import org.kiwix.kiwixmobile.core.utils.INTERNAL_SELECT_POSITION
import org.kiwix.kiwixmobile.core.utils.KiwixPermissionChecker
import org.kiwix.kiwixmobile.core.utils.NetworkUtils
import org.kiwix.kiwixmobile.core.utils.ZERO
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.core.utils.files.Log
import org.kiwix.kiwixmobile.core.zim_manager.ConnectivityBroadcastReceiver
import org.kiwix.kiwixmobile.core.zim_manager.NetworkState
import org.kiwix.kiwixmobile.core.zim_manager.NetworkState.CONNECTED
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.nav.destination.library.online.OnlineLibraryViewModel.UiEvent.NavigateToAppSettings
import org.kiwix.kiwixmobile.nav.destination.library.online.OnlineLibraryViewModel.UiEvent.NavigateToSettings
import org.kiwix.kiwixmobile.nav.destination.library.online.OnlineLibraryViewModel.UiEvent.ScrollToTop
import org.kiwix.kiwixmobile.nav.destination.library.online.OnlineLibraryViewModel.UiEvent.ShowDialog
import org.kiwix.kiwixmobile.nav.destination.library.online.OnlineLibraryViewModel.UiEvent.ShowNoSpaceSnackbar
import org.kiwix.kiwixmobile.nav.destination.library.online.OnlineLibraryViewModel.UiEvent.ShowSnackbar
import org.kiwix.kiwixmobile.nav.destination.library.online.OnlineLibraryViewModel.UiEvent.ShowToast
import org.kiwix.kiwixmobile.storage.STORAGE_SELECT_STORAGE_TITLE_TEXTVIEW_SIZE
import org.kiwix.kiwixmobile.storage.StorageSelectDialog
import org.kiwix.kiwixmobile.zimManager.AppProgressListenerProvider
import org.kiwix.kiwixmobile.zimManager.Fat32Checker
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState
import org.kiwix.kiwixmobile.zimManager.OnlineLibraryManager
import org.kiwix.kiwixmobile.zimManager.THREE
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.LibraryListItemWrapper
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.OnlineLibraryRequest
import org.kiwix.kiwixmobile.zimManager.libraryView.AvailableSpaceCalculator
import org.kiwix.kiwixmobile.zimManager.libraryView.LibraryListItem
import org.kiwix.kiwixmobile.zimManager.libraryView.LibraryListItem.BookItem
import org.kiwix.kiwixmobile.zimManager.libraryView.LibraryListItem.DividerItem
import org.kiwix.kiwixmobile.zimManager.libraryView.LibraryListItem.LibraryDownloadItem
import org.kiwix.libkiwix.Book
import retrofit2.Response
import java.util.concurrent.TimeUnit.SECONDS
import javax.inject.Inject

/**
 * ViewModel for the OnlineLibraryRoute composable.
 * Holds dependencies and business logic, emitting UI events for the composable to handle.
 */
class OnlineLibraryViewModel @Inject constructor(
  val downloader: Downloader,
  val kiwixDataStore: KiwixDataStore,
  val bookUtils: BookUtils,
  private val libkiwixBookOnDisk: LibkiwixBookOnDisk,
  @OPDSKiwixService private var kiwixService: KiwixService,
  private val downloadDao: DownloadRoomDao,
  val availableSpaceCalculator: AvailableSpaceCalculator,
  val onlineLibraryManager: OnlineLibraryManager,
  private val connectivityManager: ConnectivityManager,
  private val fat32Checker: Fat32Checker,
  private val permissionChecker: KiwixPermissionChecker,
  val context: Application,
  private val connectivityBroadcastReceiver: ConnectivityBroadcastReceiver,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {
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

  /**
   * Sealed class representing UI events that the composable should handle.
   * The ViewModel emits these events, and the composable collects and responds to them.
   */
  sealed class UiEvent {
    data class ShowSnackbar(
      val message: String,
      val actionLabel: String? = null,
      val actionIntent: Intent? = null,
      val onAction: (() -> Unit)? = null
    ) : UiEvent()

    data class ShowNoSpaceSnackbar(
      val message: String,
      val actionLabel: String,
      val onAction: () -> Unit
    ) : UiEvent()

    data class ShowDialog(
      val dialog: KiwixDialog,
      val negativeAction: () -> Unit = {},
      val positiveAction: () -> Unit = {}
    ) : UiEvent()

    data class ShowToast(val message: String) : UiEvent()

    data class RequestPermission(val permission: String) : UiEvent()

    object NavigateToSettings : UiEvent()

    object NavigateToAppSettings : UiEvent()
    object ScrollToTop : UiEvent()
  }

  private val _uiEvents = MutableSharedFlow<UiEvent>(
    replay = 1,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
  )
  val uiEvents = _uiEvents.asSharedFlow()

  private val _scanningProgress = MutableStateFlow(false to "")
  val scanningProgress = _scanningProgress.asStateFlow()

  private val _noContentState = MutableStateFlow("" to false)
  val noContentState = _noContentState.asStateFlow()
  private var appProgressListener: AppProgressListenerProvider? = AppProgressListenerProvider(this)
  val downloadProgress = MutableLiveData<String>()
  val networkStates = MutableLiveData<NetworkState>()
  val shouldShowWifiOnlyDialog = MutableLiveData<Boolean>()
  private val _libraryItems =
    MutableStateFlow<LibraryListItemWrapper>(LibraryListItemWrapper(emptyList()))
  val libraryItems: StateFlow<LibraryListItemWrapper> = _libraryItems.asStateFlow()
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
  val libraryListIsRefreshing = MutableLiveData<Boolean>()

  /**
   * Manages the showing of downloading online library progress,
   * and showing the progressBar at the end of content when loading more items.
   *
   * A [Pair] containing:
   *  - [Boolean]: When initial content is downloading.
   *  - [Boolean]: When loading more item.
   */
  val onlineLibraryDownloading = MutableStateFlow(false to false)
  private val requestDownloadLibrary = MutableSharedFlow<OnlineLibraryRequest>(
    replay = 0,
    extraBufferCapacity = 1,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
  )

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

  private var onlineLibraryFetchingJob: Job? = null
  private var isUnitTestCase: Boolean = false
  private val coroutineJobs: MutableList<Job> = mutableListOf()

  var downloadBookItem: LibraryListItem.BookItem? = null
    private set

  init {
    observeFlows()
  }

  fun setIsUnitTestCase() {
    isUnitTestCase = true
  }

  private fun observeFlows() {
    val downloads = downloadDao.downloads()
    val booksFromDao = books()
    coroutineJobs.apply {
      add(updateLibraryItems(booksFromDao, downloads, networkLibrary))
      add(updateNetworkStates())
      add(requestsAndConnectivityChangesToLibraryRequests(networkLibrary))
      add(observeCategory())
      add(observeSearch())
      add(observeLanguageChanges())
      add(onlineLibraryRequest())
    }
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

  private fun updateNetworkStates() = connectivityBroadcastReceiver.networkStates
    .onEach { state -> networkStates.postValue(state) }
    .launchIn(viewModelScope)

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
          if (!result.onlineLibraryRequest.isLoadMoreItem) {
            sendUiEvent(ScrollToTop)
          }
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

  private fun updateDownloadState(isInitial: Boolean) {
    onlineLibraryDownloading.tryEmit(isInitial to !isInitial)
  }

  private fun resetDownloadState() {
    onlineLibraryDownloading.tryEmit(false to false)
  }

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

  private fun books(): Flow<List<Book>> =
    libkiwixBookOnDisk.books().map { bookOnDiskList ->
      bookOnDiskList
        .sortedBy { it.book.title }
        .mapNotNull { it.book.nativeBook }
    }

  fun setDownloadBookItem(item: LibraryListItem.BookItem?) {
    downloadBookItem = item
  }

  fun emitNoInternetSnackbar(context: Context) {
    sendUiEvent(
      ShowSnackbar(
        message = context.getString(R.string.no_network_connection),
        actionLabel = context.getString(R.string.menu_settings),
        actionIntent = Intent(Settings.ACTION_WIFI_SETTINGS)
      )
    )
  }

  fun emitNoSpaceSnackbar(
    context: android.content.Context,
    availableSpace: String,
    onStorageSelect: () -> Unit
  ) {
    sendUiEvent(
      ShowNoSpaceSnackbar(
        message = """
            ${context.getString(R.string.download_no_space)}
            ${context.getString(R.string.space_available)} $availableSpace
          """.trimIndent(),
        actionLabel = context.getString(R.string.change_storage),
        onAction = onStorageSelect
      )
    )
  }

  fun emitToast(message: String) {
    sendUiEvent(ShowToast(message))
  }

  fun emitDialog(
    dialog: KiwixDialog,
    negativeAction: () -> Unit = {},
    positiveAction: () -> Unit = {}
  ) {
    sendUiEvent(ShowDialog(dialog, negativeAction, positiveAction))
  }

  fun downloadFile() {
    downloadBookItem?.book?.let {
      downloader.download(it)
      downloadBookItem = null
    }
  }

  fun checkSpaceAndDownload(
    context: android.content.Context,
    activity: KiwixMainActivity,
    onStorageSelect: () -> Unit
  ) {
    viewModelScope.launch {
      if (!activity.isManageExternalStoragePermissionGranted(kiwixDataStore)) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          emitDialog(KiwixDialog.ManageExternalFilesPermissionDialog) {
            onNavigateToSettingsClicked()
          }
        }
      } else {
        downloadBookItem?.let { item ->
          availableSpaceCalculator.hasAvailableSpaceFor(
            item,
            { downloadFile() },
            { availableSpace -> emitNoSpaceSnackbar(context, availableSpace, onStorageSelect) }
          )
        }
      }
    }
  }

  @Suppress("NestedBlockDepth")
  fun onBookItemClick(
    activity: KiwixMainActivity,
    item: LibraryListItem.BookItem,
    context: android.content.Context,
    onRequestStoragePermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onShowStorageSelectDialog: () -> Unit
  ) {
    viewModelScope.launch {
      downloadBookItem = item
      if (activity.hasNotificationPermission(kiwixDataStore)) {
        when {
          !NetworkUtils.isNetworkAvailable(activity) -> {
            emitNoInternetSnackbar(context)
            return@launch
          }

          kiwixDataStore.wifiOnly.first() &&
            !NetworkUtils.isWiFi(context) -> {
            emitDialog(
              KiwixDialog.YesNoDialog.WifiOnly,
              positiveAction = {
                viewModelScope.launch {
                  kiwixDataStore.setWifiOnly(false)
                  checkExternalStorageWritePermission(
                    activity,
                    onRequestStoragePermission
                  )
                }
              }
            )
            return@launch
          }

          else -> {
            if (kiwixDataStore.showStorageOption.first()) {
              if (activity.getStorageDeviceList().size > 1) {
                onShowStorageSelectDialog()
              } else {
                kiwixDataStore.setShowStorageOption(false)
                onBookItemClick(
                  activity,
                  item,
                  context,
                  onRequestStoragePermission,
                  onRequestNotificationPermission,
                  onShowStorageSelectDialog
                )
              }
            } else {
              checkExternalStorageWritePermission(
                activity,
                onRequestStoragePermission
              )
            }
          }
        }
      } else {
        onRequestNotificationPermission()
      }
    }
  }

  private suspend fun checkExternalStorageWritePermission(
    activity: KiwixMainActivity,
    onRequestStoragePermission: () -> Unit
  ) {
    if (permissionChecker.hasWriteExternalStoragePermission()) {
      checkSpaceAndDownload(
        activity,
        activity
      ) { showStorageSelectDialog(activity) }
    } else {
      onRequestStoragePermission()
    }
  }

  fun handleStoragePermissionRationale(
    activity: KiwixMainActivity,
    onRequestPermission: () -> Unit
  ) {
    viewModelScope.launch {
      val showRationale = permissionChecker.shouldShowRationale(
        activity,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
      )
      if (showRationale) {
        emitDialog(
          KiwixDialog.WriteStoragePermissionRationale,
          positiveAction = onRequestPermission
        )
      } else {
        emitDialog(
          KiwixDialog.WriteStoragePermissionRationale,
          positiveAction = ::onNavigateToAppSettingsClicked
        )
      }
    }
  }

  fun onNavigateToSettingsClicked() {
    sendUiEvent(NavigateToSettings)
  }

  fun onNavigateToAppSettingsClicked() {
    sendUiEvent(NavigateToAppSettings)
  }

  fun showStorageSelectDialog(activity: KiwixMainActivity) {
    viewModelScope.launch {
      val dialog = StorageSelectDialog().apply {
        onSelectAction = { device ->
          viewModelScope.launch {
            kiwixDataStore.setShowStorageOption(false)
            kiwixDataStore.setSelectedStorage(
              kiwixDataStore.getPublicDirectoryPath(device.name)
            )
            kiwixDataStore.setSelectedStoragePosition(
              if (device.isInternal) {
                INTERNAL_SELECT_POSITION
              } else {
                EXTERNAL_SELECT_POSITION
              }
            )
            downloadFile()
          }
        }
        titleSize = STORAGE_SELECT_STORAGE_TITLE_TEXTVIEW_SIZE
      }
      dialog.setStorageDeviceList(activity.getStorageDeviceList())
      dialog.show(
        activity.supportFragmentManager,
        activity.getString(R.string.choose_storage_to_download_book)
      )
    }
  }

  suspend fun refreshFragment(
    activity: KiwixMainActivity,
    isExplicitRefresh: Boolean,
    context: android.content.Context,
    isListEmpty: Boolean,
    onRefreshingChanged: (Boolean) -> Unit
  ) {
    if (!NetworkUtils.isNetworkAvailable(activity)) {
      if (!isListEmpty) {
        emitNoInternetSnackbar(context)
      } else {
        _noContentState.value = context.getString(R.string.no_network_connection) to true
      }
      onRefreshingChanged(false)
      _scanningProgress.value = false to context.getString(R.string.reaching_remote_library)
    } else {
      updateOnlineLibraryFilters(getOnlineLibraryRequest())
      if (isExplicitRefresh) {
        _noContentState.value = "" to false
      }
    }
  }

  suspend fun getOnlineLibraryRequest(): OnlineLibraryRequest {
    val category =
      kiwixDataStore.selectedOnlineContentCategory.first().takeUnless { it.isBlank() }
    val language =
      kiwixDataStore.selectedOnlineContentLanguage.first().takeUnless { it.isBlank() }
    return OnlineLibraryRequest(
      null,
      category,
      language,
      false,
      0
    )
  }

  fun handleLoadMore(
    zimManageViewModel: ZimManageViewModel,
    count: Int
  ) {
    viewModelScope.launch {
      val totalResults = zimManageViewModel.onlineLibraryManager.totalResult
      val totalPages = zimManageViewModel.onlineLibraryManager.calculateTotalPages(
        totalResults,
        ITEMS_PER_PAGE
      )
      val currentPage = if (count > 0) (count - 1) / ITEMS_PER_PAGE else 0
      val nextPage = currentPage + 1
      if (nextPage < totalPages) {
        updateOnlineLibraryFilters(
          onlineLibraryRequest.value.copy(
            page = nextPage,
            isLoadMoreItem = true
          )
        )
      }
    }
  }

  fun handleNetworkConnected(
    context: Context,
    activity: KiwixMainActivity,
    isListEmpty: Boolean,
    onRefreshingChanged: (Boolean) -> Unit
  ) {
    viewModelScope.launch {
      when {
        NetworkUtils.isWiFi(context) -> {
          refreshFragment(
            activity,
            false,
            context,
            isListEmpty,
            onRefreshingChanged
          )
        }

        kiwixDataStore.wifiOnly.first() && !NetworkUtils.isWiFi(context) -> {
          _noContentState.value =
            context.getString(R.string.swipe_down_for_library) to true
        }

        isListEmpty -> {
          updateOnlineLibraryFilters(getOnlineLibraryRequest())
          _noContentState.value = "" to false
          onRefreshingChanged(false)
          _scanningProgress.value =
            true to context.getString(R.string.reaching_remote_library)
        }
      }
    }
  }

  fun pauseResumeDownload(item: LibraryDownloadItem) {
    val isResumeAction = item.downloadState == DownloadState.Paused
    downloader.pauseResumeDownload(item.downloadId, isResumeAction)
  }

  fun deleteDownload(item: LibraryDownloadItem) {
    downloader.cancelDownload(item.downloadId)
  }

  fun sendUiEvent(uiEvent: UiEvent) =
    viewModelScope.launch {
      _uiEvents.emit(uiEvent)
    }

  override fun onCleared() {
    coroutineJobs.forEach {
      it.cancel()
    }
    coroutineJobs.clear()
    context.unregisterReceiver(connectivityBroadcastReceiver)
    appProgressListener = null
    onlineLibraryFetchingJob = null
    super.onCleared()
  }
}
