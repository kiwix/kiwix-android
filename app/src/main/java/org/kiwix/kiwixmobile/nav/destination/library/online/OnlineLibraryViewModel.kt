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

import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.provider.Settings
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.Status
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.isWifi
import org.kiwix.kiwixmobile.core.dao.DownloadRoomDao
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookOnDisk
import org.kiwix.kiwixmobile.core.data.remote.KiwixService.Companion.ITEMS_PER_PAGE
import org.kiwix.kiwixmobile.core.di.IoDispatcher
import org.kiwix.kiwixmobile.core.downloader.Downloader
import org.kiwix.kiwixmobile.core.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.core.downloader.model.DownloadState
import org.kiwix.kiwixmobile.core.entity.LibkiwixBook
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.ui.components.ONE
import org.kiwix.kiwixmobile.core.utils.BookUtils
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
import org.kiwix.kiwixmobile.nav.destination.library.online.OnlineLibraryViewModel.OnlineLibraryState.Idle
import org.kiwix.kiwixmobile.nav.destination.library.online.OnlineLibraryViewModel.OnlineLibraryState.Loading
import org.kiwix.kiwixmobile.nav.destination.library.online.OnlineLibraryViewModel.OnlineLibraryState.Parsing
import org.kiwix.kiwixmobile.nav.destination.library.online.OnlineLibraryViewModel.OnlineLibraryState.Success
import org.kiwix.kiwixmobile.nav.destination.library.online.OnlineLibraryViewModel.UiEvent.NavigateToAppSettings
import org.kiwix.kiwixmobile.nav.destination.library.online.OnlineLibraryViewModel.UiEvent.NavigateToSettings
import org.kiwix.kiwixmobile.nav.destination.library.online.OnlineLibraryViewModel.UiEvent.RequestPermission
import org.kiwix.kiwixmobile.nav.destination.library.online.OnlineLibraryViewModel.UiEvent.ShowDialog
import org.kiwix.kiwixmobile.nav.destination.library.online.OnlineLibraryViewModel.UiEvent.ShowNoSpaceSnackbar
import org.kiwix.kiwixmobile.nav.destination.library.online.OnlineLibraryViewModel.UiEvent.ShowSnackbar
import org.kiwix.kiwixmobile.nav.destination.library.online.OnlineLibraryViewModel.UiEvent.ShowToast
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.HandleBookDownloadUseCase
import org.kiwix.kiwixmobile.nav.destination.library.online.repository.OnlineLibraryRepository
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.HandleBookDownloadUseCase.DownloadAction.RequestNotificationPermission
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.HandleBookDownloadUseCase.DownloadAction.RequestStoragePermission
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.HandleBookDownloadUseCase.DownloadAction.RequestManageExternalFilesPermission
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.HandleBookDownloadUseCase.DownloadAction.ShowWifiOnlyDialog
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.HandleBookDownloadUseCase.DownloadAction.ShowStorageSelection
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.HandleBookDownloadUseCase.DownloadAction.NotEnoughSpace
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.HandleBookDownloadUseCase.DownloadAction.StartDownload
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.HandleBookDownloadUseCase.DownloadAction.NoInternet
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.HandleBookDownloadUseCase.DownloadAction.DisableStorageSelection
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ObserveOnlineLibraryItems
import org.kiwix.kiwixmobile.storage.STORAGE_SELECT_STORAGE_TITLE_TEXTVIEW_SIZE
import org.kiwix.kiwixmobile.storage.StorageSelectDialog
import org.kiwix.kiwixmobile.zimManager.AppProgressListenerProvider
import org.kiwix.kiwixmobile.zimManager.Fat32Checker
import org.kiwix.kiwixmobile.zimManager.libraryView.AvailableSpaceCalculator
import org.kiwix.kiwixmobile.zimManager.libraryView.LibraryListItem
import org.kiwix.kiwixmobile.zimManager.libraryView.LibraryListItem.BookItem
import org.kiwix.kiwixmobile.zimManager.libraryView.LibraryListItem.LibraryDownloadItem
import org.kiwix.libkiwix.Book
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
  private val downloadDao: DownloadRoomDao,
  val availableSpaceCalculator: AvailableSpaceCalculator,
  private val connectivityManager: ConnectivityManager,
  private val fat32Checker: Fat32Checker,
  private val permissionChecker: KiwixPermissionChecker,
  val context: Application,
  private val connectivityBroadcastReceiver: ConnectivityBroadcastReceiver,
  private val repository: OnlineLibraryRepository,
  private val observeOnlineLibraryItems: ObserveOnlineLibraryItems,
  private val handleBookDownloadUseCase: HandleBookDownloadUseCase,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {
  data class OnlineLibraryRequest(
    val query: String? = null,
    val category: String? = null,
    val lang: String? = null,
    val isLoadMoreItem: Boolean,
    val page: Int
  )

  sealed class OnlineLibraryState {
    object Idle : OnlineLibraryState()

    data class Loading(val isLoadMore: Boolean) : OnlineLibraryState()

    data class Success(
      val request: OnlineLibraryRequest,
      val books: List<LibkiwixBook>,
      val totalPages: Int
    ) : OnlineLibraryState()

    data class Error(
      val request: OnlineLibraryRequest,
      val throwable: Throwable? = null
    ) : OnlineLibraryState()

    object Parsing : OnlineLibraryState()
  }

  data class OnlineLibraryUiState(
    val items: List<LibraryListItem> = emptyList(),
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val downloadingOnlineLibraryProgress: String = "",
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val scanningMessage: String = "",
    val showScanning: Boolean = false,
    val noContentMessage: String = "",
    val showNoContent: Boolean = false
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

  private val _uiState = MutableStateFlow(OnlineLibraryUiState())
  val uiState = _uiState.asStateFlow()
  private var appProgressListener: AppProgressListenerProvider? = null
  private val onlineLibraryRequest = MutableSharedFlow<OnlineLibraryRequest>(
    replay = 1,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
  )

  private var currentRequest = OnlineLibraryRequest(
    query = null,
    category = null,
    lang = null,
    isLoadMoreItem = false,
    page = 0
  )

  private val networkBooks = MutableStateFlow<List<LibkiwixBook>>(emptyList())

  private var onlineLibraryFetchingJob: Job? = null
  private var totalPages: Int = 0
  private val coroutineJobs: MutableList<Job> = mutableListOf()
  val isAndroid13OrAbove = permissionChecker.isAndroid13orAbove()

  var downloadBookItem: LibraryListItem.BookItem? = null
    private set

  init {
    appProgressListener = AppProgressListenerProvider(context) {
      _uiState.update { current -> current.copy(downloadingOnlineLibraryProgress = it) }
    }
    observeFlows()
  }

  private fun observeFlows() {
    val downloads = downloadDao.downloads()
    val booksFromDao = books()
    coroutineJobs.apply {
      add(updateLibraryItems(booksFromDao, downloads))
      add(updateNetworkStates())
      add(observeLibrary())
      add(observeCategory())
      add(observeSearch())
      add(observeLanguageChanges())
    }
  }

  private fun observeCategory() =
    kiwixDataStore.selectedOnlineContentCategory
      .onEach {
        _uiState.update { current -> current.copy(isRefreshing = true) }
        updateOnlineLibraryFilters(
          OnlineLibraryRequest(category = it, page = ZERO, isLoadMoreItem = false)
        )
      }
      .flowOn(ioDispatcher)
      .launchIn(viewModelScope)

  @OptIn(FlowPreview::class)
  private fun observeSearch() =
    uiState
      .map { it.searchQuery }
      .distinctUntilChanged()
      .debounce(500)
      .onEach {
        updateOnlineLibraryFilters(
          OnlineLibraryRequest(query = it, page = ZERO, isLoadMoreItem = false)
        )
      }
      .flowOn(ioDispatcher)
      .launchIn(viewModelScope)

  private fun observeLanguageChanges() =
    kiwixDataStore.selectedOnlineContentLanguage
      .onEach {
        updateOnlineLibraryFilters(
          OnlineLibraryRequest(lang = it, page = ZERO, isLoadMoreItem = false)
        )
      }
      .flowOn(ioDispatcher)
      .launchIn(viewModelScope)

  fun updateOnlineLibraryFilters(newRequest: OnlineLibraryRequest) {
    currentRequest = currentRequest.copy(
      query = newRequest.query ?: currentRequest.query,
      category = newRequest.category ?: currentRequest.category,
      lang = newRequest.lang ?: currentRequest.lang,
      page = newRequest.page,
      isLoadMoreItem = newRequest.isLoadMoreItem
    )
    viewModelScope.launch {
      onlineLibraryRequest.emit(currentRequest)
    }
  }

  private fun updateNetworkStates() = connectivityBroadcastReceiver.networkStates
    .onEach { state ->
      when (state) {
        NetworkState.CONNECTED -> {
          handleNetworkConnected()
        }

        NetworkState.NOT_CONNECTED -> {
          if (!uiState.value.items.isNotEmpty()) {
            emitNoInternetSnackbar(context)
          } else {
            _uiState.update {
              it.copy(
                noContentMessage = context.getString(string.no_network_connection),
                showNoContent = true
              )
            }
          }
          _uiState.update { it.copy(isRefreshing = false) }
        }
      }
    }
    .launchIn(viewModelScope)

  @OptIn(ExperimentalCoroutinesApi::class)
  private fun observeLibrary() =
    onlineLibraryRequest
      .flatMapLatest { request ->
        flow {
          connectivityBroadcastReceiver.networkStates
            .filter { it == CONNECTED }
            .first()

          if (!shouldProceedWithDownload()) return@flow

          emitAll(repository.fetchOnlineLibrary(request, appProgressListener))
        }
      }
      .onEach { state -> handleLibraryState(state) }
      .flowOn(ioDispatcher)
      .launchIn(viewModelScope)

  private suspend fun handleLibraryState(state: OnlineLibraryState) {
    val currentBooks = networkBooks.value
    when (state) {
      Idle -> updateDownloadProgressIfNeeded(R.string.empty_string)
      is Loading -> {
        updateDownloadState(!state.isLoadMore)
        updateDownloadProgressIfNeeded(R.string.starting_downloading_remote_library)
      }

      Parsing -> updateDownloadProgressIfNeeded(R.string.parsing_remote_library)

      is Success -> {
        totalPages = state.totalPages
        val request = state.request
        val newBooks = when {
          request.isLoadMoreItem -> currentBooks + state.books

          state.books.isEmpty() && currentBooks.isNotEmpty() -> currentBooks

          else -> state.books
        }
        networkBooks.emit(newBooks)
        if (!request.isLoadMoreItem && newBooks.isNotEmpty()) {
          sendUiEvent(UiEvent.ScrollToTop)
        }
        resetDownloadState()
      }

      is OnlineLibraryState.Error -> {
        if (currentBooks.isEmpty()) {
          networkBooks.emit(emptyList())
        }
        resetDownloadState()
      }
    }
  }

  private suspend fun shouldProceedWithDownload(): Boolean {
    if (!connectivityManager.isWifi()) {
      if (kiwixDataStore.wifiOnly.first()) {
        _uiState.update {
          it.copy(
            isRefreshing = false,
            isLoadingMore = false
          )
        }
        showWifiOnlyDialog()
        return false
      }
    }
    return true
  }

  private fun showWifiOnlyDialog() {
    emitDialog(
      KiwixDialog.YesNoDialog.WifiOnly, positiveAction = {
        viewModelScope.launch {
          _uiState.update { it.copy(noContentMessage = "", showNoContent = false) }
          kiwixDataStore.setWifiOnly(false)
          updateOnlineLibraryFilters(getOnlineLibraryRequest())
        }
      },
      negativeAction = {
        context.toast(
          context.getString(string.denied_internet_permission_message),
          Toast.LENGTH_SHORT
        )
        _uiState.update {
          it.copy(
            noContentMessage = context.getString(string.swipe_down_for_library),
            showNoContent = true
          )
        }
      })
  }

  private fun updateDownloadProgressIfNeeded(messageResId: Int) {
    _uiState.update { it.copy(downloadingOnlineLibraryProgress = context.getString(messageResId)) }
  }

  private fun updateDownloadState(isInitial: Boolean) {
    _uiState.update {
      it.copy(
        isRefreshing = isInitial,
        isLoadingMore = !isInitial,
        showScanning = isInitial
      )
    }
  }

  private fun resetDownloadState() {
    _uiState.update {
      it.copy(
        isRefreshing = false,
        isLoadingMore = false
      )
    }
  }

  @Suppress("UNCHECKED_CAST")
  @OptIn(FlowPreview::class)
  private fun updateLibraryItems(
    localBooksFromLibkiwix: Flow<List<Book>>,
    downloads: Flow<List<DownloadModel>>
  ) = viewModelScope.launch(ioDispatcher) {
    combine(
      localBooksFromLibkiwix,
      downloads,
      networkBooks,
      fat32Checker.fileSystemStates,
      kiwixDataStore.selectedOnlineContentLanguage
    ) { books, activeDownloads, remoteBooks, fsState, lang ->
      libraryItemsMapper.map(
        booksOnFileSystem = books,
        activeDownloads = activeDownloads,
        remoteBooks = remoteBooks,
        fileSystemState = fsState,
        selectedLanguage = lang,
        getString = { resId, argsStr -> context.getString(resId, *argsStr) },
        getSimpleString = { resId -> context.getString(resId) }
      )
    }
      .onEach { _uiState.update { it.copy(isRefreshing = false) } }
      .catch { throwable ->
        _uiState.update { it.copy(isRefreshing = false) }
        throwable.printStackTrace()
        Log.e("ZimManageViewModel", "Error----$throwable")
      }
      .collect {
        _uiState.update { current ->
          current.copy(
            items = it,
            isRefreshing = false,
            noContentMessage = noContentMessageWhenItemsComesFromOnlineSource(it),
            showNoContent = it.isEmpty()
          )
        }
      }
  }

  private fun noContentMessageWhenItemsComesFromOnlineSource(items: List<LibraryListItem>): String =
    when {
      items.isEmpty() -> if (!NetworkUtils.isNetworkAvailable(context)) {
        context.getString(string.no_network_connection)
      } else {
        context.getString(string.no_items_msg)
      }

      else -> ""
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

  fun onBookItemClick(item: BookItem, activity: KiwixMainActivity) {
    viewModelScope.launch {
      downloadBookItem = item
      val action = handleBookDownloadUseCase.invoke(
        item,
        activity.getStorageDeviceList().size
      )
      when (action) {
        RequestNotificationPermission -> if (isAndroid13OrAbove) {
          sendUiEvent(RequestPermission(POST_NOTIFICATIONS))
        }

        RequestStoragePermission -> sendUiEvent(RequestPermission(WRITE_EXTERNAL_STORAGE))
        RequestManageExternalFilesPermission -> emitDialog(KiwixDialog.ManageExternalFilesPermissionDialog) {
          sendUiEvent(NavigateToSettings)
        }

        NoInternet -> emitNoInternetSnackbar(context)

        ShowWifiOnlyDialog -> emitDialog(
          KiwixDialog.YesNoDialog.WifiOnly,
          positiveAction = {
            viewModelScope.launch {
              kiwixDataStore.setWifiOnly(false)
              onBookItemClick(item, activity)
            }
          }
        )

        ShowStorageSelection -> showStorageSelectDialog(activity)

        DisableStorageSelection -> {
          kiwixDataStore.setShowStorageOption(false)
          onBookItemClick(item, activity)
        }

        is NotEnoughSpace -> emitNoSpaceSnackbar(context, action.availableSpace) {
          showStorageSelectDialog(activity)
        }

        is StartDownload -> downloadFile()
      }
    }
  }

  fun onNavigateToAppSettingsClicked() {
    sendUiEvent(NavigateToAppSettings)
  }

  private fun showStorageSelectDialog(activity: KiwixMainActivity) {
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

  fun refreshScreen(isExplicitRefresh: Boolean) {
    viewModelScope.launch {
      if (!NetworkUtils.isNetworkAvailable(context)) {
        if (uiState.value.items.isNotEmpty()) {
          emitNoInternetSnackbar(context)
        } else {
          _uiState.update {
            it.copy(
              noContentMessage = context.getString(R.string.no_network_connection),
              showNoContent = true
            )
          }
        }
        _uiState.update {
          it.copy(
            isRefreshing = false,
            showScanning = false,
            scanningMessage = context.getString(R.string.reaching_remote_library)
          )
        }
      } else {
        updateOnlineLibraryFilters(getOnlineLibraryRequest())
        if (isExplicitRefresh) {
          _uiState.update {
            it.copy(
              noContentMessage = "",
              showNoContent = false
            )
          }
        }
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

  fun handleLoadMore(count: Int) {
    val currentPage = if (count > 0) (count - 1) / ITEMS_PER_PAGE else 0
    val nextPage = currentPage + 1
    if (uiState.value.isLoadingMore) return
    if (nextPage < totalPages) {
      updateOnlineLibraryFilters(
        currentRequest.copy(
          page = nextPage,
          isLoadMoreItem = true
        )
      )
    }
  }

  private fun handleNetworkConnected() {
    viewModelScope.launch {
      when {
        NetworkUtils.isWiFi(context) -> {
          refreshScreen(false)
        }

        kiwixDataStore.wifiOnly.first() && !NetworkUtils.isWiFi(context) -> {
          _uiState.update {
            it.copy(
              noContentMessage = context.getString(R.string.swipe_down_for_library),
              showNoContent = true
            )
          }
        }

        uiState.value.items.isEmpty() -> {
          updateOnlineLibraryFilters(getOnlineLibraryRequest())
          _uiState.update {
            it.copy(
              showScanning = true,
              scanningMessage = context.getString(R.string.reaching_remote_library),
              noContentMessage = "",
              showNoContent = false,
              isRefreshing = false
            )
          }
        }
      }
    }
  }

  fun onSearchQueryChanged(query: String) {
    _uiState.update {
      it.copy(searchQuery = query.trim())
    }
  }

  fun closeSearchView() {
    _uiState.update {
      it.copy(searchQuery = "", isSearchActive = false)
    }
  }

  fun openSearchView() {
    _uiState.update {
      it.copy(isSearchActive = true)
    }
  }

  fun clearSearch() {
    _uiState.update {
      it.copy(searchQuery = "")
    }
  }

  private fun deleteDownload(item: LibraryDownloadItem) {
    downloader.cancelDownload(item.downloadId)
  }

  fun onPauseResumeButtonClick(item: LibraryListItem.LibraryDownloadItem) {
    if (!NetworkUtils.isNetworkAvailable(context)) {
      emitNoInternetSnackbar(context)
      return
    }
    val isResumeAction = item.downloadState == DownloadState.Paused
    downloader.pauseResumeDownload(item.downloadId, isResumeAction)
  }

  fun onStopButtonClick(item: LibraryListItem.LibraryDownloadItem) {
    if (item.currentDownloadState == Status.FAILED) {
      when (item.downloadError) {
        Error.UNKNOWN_IO_ERROR,
        Error.CONNECTION_TIMED_OUT,
        Error.UNKNOWN -> {
          if (!NetworkUtils.isNetworkAvailable(context)) {
            emitNoInternetSnackbar(context)
          } else {
            downloader.retryDownload(item.downloadId)
          }
        }

        else -> {
          emitDialog(KiwixDialog.YesNoDialog.StopDownload) {
            deleteDownload(item)
          }
        }
      }
    } else {
      emitDialog(KiwixDialog.YesNoDialog.StopDownload) {
        deleteDownload(item)
      }
    }
  }

  private fun sendUiEvent(uiEvent: UiEvent) =
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

  fun onNotificationPermissionResult(isGranted: Boolean, activity: KiwixMainActivity) {
    if (isGranted) {
      downloadBookItem?.let { onBookItemClick(it, activity) }
      return
    }
    if (!permissionChecker.shouldShowRationale(activity, POST_NOTIFICATIONS)) {
      emitDialog(
        KiwixDialog.NotificationPermissionDialog,
        positiveAction = ::onNavigateToAppSettingsClicked
      )
    }
  }

  fun onStoragePermissionResult(isGranted: Boolean, activity: KiwixMainActivity) {
    if (isGranted) {
      downloadBookItem?.let { onBookItemClick(it, activity) }
      return
    }
    if (permissionChecker.shouldShowRationale(activity, WRITE_EXTERNAL_STORAGE)) {
      emitDialog(
        KiwixDialog.WriteStoragePermissionRationale,
        positiveAction = {
          sendUiEvent(RequestPermission(WRITE_EXTERNAL_STORAGE))
        }
      )
    } else {
      emitDialog(
        KiwixDialog.WriteStoragePermissionRationale,
        positiveAction = ::onNavigateToAppSettingsClicked
      )
    }
  }
}
