/*
 * Kiwix Android
 * Copyright (c) 2026 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel

import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.provider.Settings
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.isNetworkAvailable
import org.kiwix.kiwixmobile.core.dao.DownloadRoomDao
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookOnDisk
import org.kiwix.kiwixmobile.core.data.remote.KiwixService.Companion.ITEMS_PER_PAGE
import org.kiwix.kiwixmobile.core.di.IoDispatcher
import org.kiwix.kiwixmobile.core.downloader.Downloader
import org.kiwix.kiwixmobile.core.entity.LibkiwixBook
import org.kiwix.kiwixmobile.core.extensions.registerReceiver
import org.kiwix.kiwixmobile.core.ui.components.ONE
import org.kiwix.kiwixmobile.core.utils.BookUtils
import org.kiwix.kiwixmobile.core.utils.EXTERNAL_SELECT_POSITION
import org.kiwix.kiwixmobile.core.utils.INTERNAL_SELECT_POSITION
import org.kiwix.kiwixmobile.core.utils.KiwixPermissionChecker
import org.kiwix.kiwixmobile.core.utils.ZERO
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.core.utils.files.Log
import org.kiwix.kiwixmobile.core.zim_manager.ConnectivityBroadcastReceiver
import org.kiwix.kiwixmobile.data.remote.AppProgressListenerProvider
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ObserveNetworkState
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ObserveOnlineLibrary
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ObserveOnlineLibraryItems
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveBookClickAction
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveBookClickAction.LibraryActionResult.CancelDownload
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveBookClickAction.LibraryActionResult.DisableStorageSelection
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveBookClickAction.LibraryActionResult.NoInternet
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveBookClickAction.LibraryActionResult.NotEnoughSpace
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveBookClickAction.LibraryActionResult.PauseResume
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveBookClickAction.LibraryActionResult.RequestManageExternalFilesPermission
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveBookClickAction.LibraryActionResult.RequestNotificationPermission
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveBookClickAction.LibraryActionResult.RequestStoragePermission
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveBookClickAction.LibraryActionResult.RetryDownload
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveBookClickAction.LibraryActionResult.ShowStorageSelection
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveBookClickAction.LibraryActionResult.ShowWifiOnlyDialog
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveBookClickAction.LibraryActionResult.StartDownload
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveRefreshLibraryAction
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveRefreshLibraryAction.Result.NoInternetWithContent
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveRefreshLibraryAction.Result.NoInternetWithEmptyContent
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveRefreshLibraryAction.Result.Proceed
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveRefreshLibraryAction.Result.WifiOnlyBlocked
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.OnlineLibraryState.Idle
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.OnlineLibraryState.Loading
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.OnlineLibraryState.NoInternetConnection
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.OnlineLibraryState.Parsing
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.OnlineLibraryState.Success
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.OnlineLibraryState.WifiOnlyException
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.UiEvent.NavigateToAppSettings
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.UiEvent.NavigateToSettings
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.UiEvent.RequestPermission
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.UiEvent.ShowDialog
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.UiEvent.ShowNoSpaceSnackbar
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.UiEvent.ShowToast
import org.kiwix.kiwixmobile.storage.STORAGE_SELECT_STORAGE_TITLE_TEXTVIEW_SIZE
import org.kiwix.kiwixmobile.storage.StorageSelectDialog
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
@Suppress("LongParameterList")
class OnlineLibraryViewModel @Inject constructor(
  val downloader: Downloader,
  val kiwixDataStore: KiwixDataStore,
  val bookUtils: BookUtils,
  private val libkiwixBookOnDisk: LibkiwixBookOnDisk,
  private val downloadDao: DownloadRoomDao,
  val availableSpaceCalculator: AvailableSpaceCalculator,
  private val permissionChecker: KiwixPermissionChecker,
  val context: Application,
  private val connectivityBroadcastReceiver: ConnectivityBroadcastReceiver,
  private val connectivityManager: ConnectivityManager,
  private val observeOnlineLibraryItems: ObserveOnlineLibraryItems,
  private val resolveBookClickAction: ResolveBookClickAction,
  private val observeOnlineLibrary: ObserveOnlineLibrary,
  private val refreshLibraryAction: ResolveRefreshLibraryAction,
  private val observeNetworkState: ObserveNetworkState,
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
    data class Idle(val isLoadMore: Boolean) : OnlineLibraryState()

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

    object WifiOnlyException : OnlineLibraryState()
    object NoInternetConnection : OnlineLibraryState()
    data class Parsing(val isLoadMore: Boolean) : OnlineLibraryState()
  }

  data class OnlineLibraryUiState(
    val items: List<LibraryListItem> = emptyList(),
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val scanningProgressBarMessage: String = "",
    val showScanningProgressBar: Boolean = false,
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
    extraBufferCapacity = 1,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
  )
  val uiEvents = _uiEvents.asSharedFlow()

  private val _uiState = MutableStateFlow(OnlineLibraryUiState())
  val uiState = _uiState.asStateFlow()
  private var appProgressListener: AppProgressListenerProvider? = null
  internal val onlineLibraryRequest = MutableSharedFlow<OnlineLibraryRequest>(
    replay = 1,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
  )

  internal var currentRequest = OnlineLibraryRequest(
    query = null,
    category = null,
    lang = null,
    isLoadMoreItem = false,
    page = ZERO
  )

  internal val networkBooks = MutableStateFlow<List<LibkiwixBook>>(emptyList())
  internal var totalPages: Int = 0
  private val coroutineJobs: MutableList<Job> = mutableListOf()
  val isAndroid13OrAbove = permissionChecker.isAndroid13orAbove()

  private var downloadBookItem: BookItem? = null

  @VisibleForTesting
  internal fun setUiStateForTest(state: OnlineLibraryUiState) {
    _uiState.value = state
  }

  init {
    context.registerReceiver(connectivityBroadcastReceiver)
    appProgressListener = AppProgressListenerProvider(context) {
      _uiState.update { current -> current.copy(scanningProgressBarMessage = it) }
    }
    observeFlows()
  }

  private fun observeFlows() {
    coroutineJobs.apply {
      add(observeLibraryItems())
      add(updateNetworkStates())
      add(observeLibrary())
      add(observeFilters())
    }
  }

  fun loadInitialLibrary() {
    viewModelScope.launch {
      if (uiState.value.items.isEmpty()) {
        // Load the library initially, and avoid loading the library on every screen recomposition.
        updateOnlineLibraryFilters(getOnlineLibraryRequest())
      }
    }
  }

  private fun observeLibraryItems() = observeOnlineLibraryItems(
    localBooks = books(),
    downloads = downloadDao.downloads(),
    networkBooks = networkBooks,
    getString = { resId, args -> context.getString(resId, *args) },
    getSimpleString = { resId -> context.getString(resId) }
  ).onEach {
    _uiState.update { current ->
      current.copy(
        items = it,
        noContentMessage = noContentMessageWhenItemsComesFromOnlineSource(it),
        showNoContent = it.isEmpty()
      )
    }
  }.catch { throwable ->
    resetDownloadState()
    throwable.printStackTrace()
    Log.e("OnlineLibraryViewModel", "Error----$throwable")
  }.launchIn(viewModelScope)

  private fun noContentMessageWhenItemsComesFromOnlineSource(items: List<LibraryListItem>): String =
    when {
      items.isEmpty() -> if (connectivityManager.isNetworkAvailable()) {
        context.getString(R.string.no_items_msg)
      } else {
        context.getString(R.string.no_network_connection)
      }

      else -> ""
    }

  @OptIn(FlowPreview::class)
  @Suppress("MagicNumber")
  private fun observeFilters() =
    combine(
      kiwixDataStore.selectedOnlineContentCategory,
      kiwixDataStore.selectedOnlineContentLanguage,
      uiState.map { it.searchQuery }.distinctUntilChanged().debounce(500)
    ) { category, language, searchQuery ->
      OnlineLibraryRequest(searchQuery, category, language, false, ZERO)
    }
      .onEach { updateOnlineLibraryFilters(it) }
      .flowOn(ioDispatcher)
      .launchIn(viewModelScope)

  internal fun updateOnlineLibraryFilters(newRequest: OnlineLibraryRequest) {
    val updatedRequest = currentRequest.copy(
      query = newRequest.query ?: currentRequest.query,
      category = newRequest.category ?: currentRequest.category,
      lang = newRequest.lang ?: currentRequest.lang,
      page = newRequest.page,
      isLoadMoreItem = newRequest.isLoadMoreItem
    )
    if (updatedRequest == currentRequest) return
    currentRequest = updatedRequest
    viewModelScope.launch {
      onlineLibraryRequest.emit(currentRequest)
    }
  }

  private fun updateNetworkStates() =
    observeNetworkState(connectivityBroadcastReceiver.networkStates)
      .onEach { handleNetworkState(it) }
      .flowOn(ioDispatcher)
      .launchIn(viewModelScope)

  internal suspend fun handleNetworkState(state: ObserveNetworkState.Result) {
    when (state) {
      ObserveNetworkState.Result.WifiAvailable -> refreshScreen(false)
      ObserveNetworkState.Result.ShowWifiOnlyMessage -> {
        _uiState.update {
          it.copy(
            noContentMessage = context.getString(R.string.swipe_down_for_library),
            showNoContent = true,
            showScanningProgressBar = false
          )
        }
      }

      ObserveNetworkState.Result.ShowNoInternetSnackBar -> {
        if (uiState.value.items.isEmpty()) {
          _uiState.update {
            it.copy(
              noContentMessage = context.getString(R.string.no_network_connection),
              showNoContent = true,
              isRefreshing = false,
              showScanningProgressBar = false
            )
          }
        } else {
          emitNoInternetSnackbar()
          _uiState.update {
            it.copy(isRefreshing = false, showScanningProgressBar = false)
          }
        }
      }

      ObserveNetworkState.Result.MobileInternet -> {
        if (uiState.value.items.isEmpty()) {
          updateOnlineLibraryFilters(getOnlineLibraryRequest())
          _uiState.update {
            it.copy(
              showScanningProgressBar = true,
              scanningProgressBarMessage = context.getString(R.string.reaching_remote_library),
              noContentMessage = "",
              showNoContent = false,
              isRefreshing = false
            )
          }
        }
      }
    }
  }

  private fun observeLibrary() =
    observeOnlineLibrary(onlineLibraryRequest, appProgressListener)
      .onEach { state -> handleLibraryState(state) }
      .flowOn(ioDispatcher)
      .launchIn(viewModelScope)

  @Suppress("CyclomaticComplexMethod")
  internal suspend fun handleLibraryState(state: OnlineLibraryState) {
    when (state) {
      is Idle -> updateDownloadProgressIfNeeded(
        state.isLoadMore,
        R.string.reaching_remote_library
      )

      WifiOnlyException -> {
        _uiState.update {
          it.copy(
            showScanningProgressBar = false,
            isLoadingMore = false
          )
        }
        showWifiOnlyDialog()
      }

      NoInternetConnection -> {
        _uiState.update {
          it.copy(
            showScanningProgressBar = false,
            isLoadingMore = false
          )
        }
      }

      is Loading -> {
        updateDownloadProgressIfNeeded(
          state.isLoadMore,
          R.string.starting_downloading_remote_library
        )
      }

      is Parsing -> updateDownloadProgressIfNeeded(
        state.isLoadMore,
        R.string.parsing_remote_library
      )

      is Success -> {
        val currentBooks = networkBooks.value
        totalPages = state.totalPages
        val request = state.request
        val newBooks = when {
          request.isLoadMoreItem -> currentBooks + state.books
          else -> state.books
        }
        networkBooks.emit(newBooks)
        if (!request.isLoadMoreItem && newBooks.isNotEmpty()) {
          sendUiEvent(UiEvent.ScrollToTop)
        }
        resetDownloadState()
      }

      is OnlineLibraryState.Error -> {
        if (networkBooks.value.isEmpty()) {
          networkBooks.emit(emptyList())
        }
        resetDownloadState()
      }
    }
  }

  private fun showWifiOnlyDialog() {
    emitDialog(
      KiwixDialog.YesNoDialog.WifiOnly,
      positiveAction = {
        viewModelScope.launch {
          _uiState.update { it.copy(noContentMessage = "", showNoContent = false) }
          kiwixDataStore.setWifiOnly(false)
          updateOnlineLibraryFilters(getOnlineLibraryRequest())
        }
      },
      negativeAction = {
        emitToast(context.getString(R.string.denied_internet_permission_message))
        _uiState.update {
          it.copy(
            noContentMessage = context.getString(R.string.swipe_down_for_library),
            showNoContent = true
          )
        }
      }
    )
  }

  private fun updateDownloadProgressIfNeeded(isLoadMore: Boolean, messageResId: Int) {
    _uiState.update {
      it.copy(
        showScanningProgressBar = !isLoadMore,
        isLoadingMore = isLoadMore,
        scanningProgressBarMessage = context.getString(messageResId)
      )
    }
  }

  private fun resetDownloadState() {
    _uiState.update {
      it.copy(
        isRefreshing = false,
        isLoadingMore = false,
        showScanningProgressBar = false
      )
    }
  }

  private fun books(): Flow<List<Book>> =
    libkiwixBookOnDisk.books().map { bookOnDiskList ->
      bookOnDiskList
        .sortedBy { it.book.title }
        .mapNotNull { it.book.nativeBook }
    }

  private fun emitNoInternetSnackbar() {
    sendUiEvent(
      UiEvent.ShowSnackbar(
        message = context.getString(R.string.no_network_connection),
        actionLabel = context.getString(R.string.menu_settings),
        actionIntent = Intent(Settings.ACTION_WIFI_SETTINGS)
      )
    )
  }

  private fun emitNoSpaceSnackbar(
    context: Context,
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

  private fun emitToast(message: String) {
    sendUiEvent(ShowToast(message))
  }

  private fun emitDialog(
    dialog: KiwixDialog,
    negativeAction: () -> Unit = {},
    positiveAction: () -> Unit = {}
  ) {
    sendUiEvent(ShowDialog(dialog, negativeAction, positiveAction))
  }

  private fun downloadFile() {
    downloadBookItem?.book?.let {
      downloader.download(it)
      downloadBookItem = null
    }
  }

  fun setDownloadBookItem(item: BookItem) {
    downloadBookItem = item
  }

  fun onBookItemClick(item: BookItem, activity: KiwixMainActivity) {
    viewModelScope.launch {
      setDownloadBookItem(item)
      val action = resolveBookClickAction.onBookItemClick(
        item,
        activity.getStorageDeviceList().size
      )
      when (action) {
        ShowStorageSelection -> showStorageSelectDialog(activity)
        is StartDownload -> downloadFile()
        NoInternet -> emitNoInternetSnackbar()
        RequestStoragePermission -> sendUiEvent(UiEvent.RequestPermission(WRITE_EXTERNAL_STORAGE))
        RequestNotificationPermission -> if (isAndroid13OrAbove) {
          sendUiEvent(UiEvent.RequestPermission(POST_NOTIFICATIONS))
        }

        RequestManageExternalFilesPermission -> emitDialog(
          KiwixDialog.ManageExternalFilesPermissionDialog,
          positiveAction = {
            sendUiEvent(NavigateToSettings)
          }
        )

        ShowWifiOnlyDialog -> emitDialog(
          KiwixDialog.YesNoDialog.WifiOnly,
          positiveAction = {
            viewModelScope.launch {
              kiwixDataStore.setWifiOnly(false)
              onBookItemClick(item, activity)
            }
          }
        )

        DisableStorageSelection -> {
          kiwixDataStore.setShowStorageOption(false)
          onBookItemClick(item, activity)
        }

        is NotEnoughSpace -> emitNoSpaceSnackbar(context, action.availableSpace) {
          showStorageSelectDialog(activity)
        }

        else -> Unit
      }
    }
  }

  fun onPauseResumeButtonClick(item: LibraryDownloadItem) {
    when (val result = resolveBookClickAction.onPauseResumeButtonClick(item)) {
      NoInternet -> emitNoInternetSnackbar()
      is PauseResume -> downloader.pauseResumeDownload(result.downloadId, result.isPaused)
      else -> Unit
    }
  }

  fun onStopButtonClick(item: LibraryDownloadItem) {
    when (val result = resolveBookClickAction.onStopButtonClick(item)) {
      NoInternet -> emitNoInternetSnackbar()
      is RetryDownload -> downloader.retryDownload(result.downloadId)
      is CancelDownload -> emitDialog(
        KiwixDialog.YesNoDialog.StopDownload,
        positiveAction = {
          downloader.cancelDownload(result.downloadId)
        }
      )

      else -> Unit
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
            downloadBookItem?.let {
              onBookItemClick(it, activity)
            }
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
      when (refreshLibraryAction(uiState.value.items.isNotEmpty())) {
        Proceed -> {
          updateOnlineLibraryFilters(getOnlineLibraryRequest())
          if (isExplicitRefresh) {
            _uiState.update {
              it.copy(
                noContentMessage = "",
                showNoContent = false,
                showScanningProgressBar = true,
                scanningProgressBarMessage = context.getString(R.string.reaching_remote_library)
              )
            }
          }
        }

        NoInternetWithContent -> emitNoInternetSnackbar()
        NoInternetWithEmptyContent -> {
          _uiState.update {
            it.copy(
              noContentMessage = context.getString(R.string.no_network_connection),
              showNoContent = true,
              isRefreshing = false,
              showScanningProgressBar = false,
              scanningProgressBarMessage = context.getString(R.string.reaching_remote_library)
            )
          }
        }

        WifiOnlyBlocked -> showWifiOnlyDialog()
      }
    }
  }

  private suspend fun getOnlineLibraryRequest(): OnlineLibraryRequest {
    val category =
      kiwixDataStore.selectedOnlineContentCategory.first().takeUnless { it.isBlank() }
    val language =
      kiwixDataStore.selectedOnlineContentLanguage.first().takeUnless { it.isBlank() }
    return OnlineLibraryRequest(
      null,
      category,
      language,
      false,
      ZERO
    )
  }

  fun handleLoadMore(count: Int) {
    val currentPage = if (count > ZERO) (count - ONE) / ITEMS_PER_PAGE else ZERO
    val nextPage = currentPage + ONE
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

  private fun sendUiEvent(uiEvent: UiEvent) =
    viewModelScope.launch {
      _uiEvents.emit(uiEvent)
    }

  @VisibleForTesting
  fun onClearedExposed() {
    onCleared()
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
