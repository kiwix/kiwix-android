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
import android.content.Intent
import android.net.ConnectivityManager
import android.provider.Settings
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.mhutti1.utils.storage.StorageDevice
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
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.convertToLocal
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.isNetworkAvailable
import org.kiwix.kiwixmobile.core.dao.DownloadRoomDao
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookOnDisk
import org.kiwix.kiwixmobile.core.data.remote.KiwixService.Companion.ITEMS_PER_PAGE
import org.kiwix.kiwixmobile.core.di.IoDispatcher
import org.kiwix.kiwixmobile.core.downloader.Downloader
import org.kiwix.kiwixmobile.core.entity.LibkiwixBook
import org.kiwix.kiwixmobile.core.ui.components.ONE
import org.kiwix.kiwixmobile.core.utils.BookUtils
import org.kiwix.kiwixmobile.core.utils.EXTERNAL_SELECT_POSITION
import org.kiwix.kiwixmobile.core.utils.INTERNAL_SELECT_POSITION
import org.kiwix.kiwixmobile.core.utils.KiwixPermissionChecker
import org.kiwix.kiwixmobile.core.utils.LocaleHelper
import org.kiwix.kiwixmobile.core.utils.StorageDeviceProvider
import org.kiwix.kiwixmobile.core.utils.ZERO
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.core.utils.files.Log
import org.kiwix.kiwixmobile.core.zim_manager.Category
import org.kiwix.kiwixmobile.core.zim_manager.ConnectivityObserver
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.nav.destination.library.StorageSelectDialogConfig
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
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.UiEvent.SideEffects
import org.kiwix.kiwixmobile.storage.STORAGE_SELECT_STORAGE_TITLE_TEXTVIEW_SIZE
import org.kiwix.kiwixmobile.zimManager.libraryView.AvailableSpaceCalculator
import org.kiwix.kiwixmobile.zimManager.libraryView.LibraryListItem
import org.kiwix.kiwixmobile.zimManager.libraryView.LibraryListItem.BookItem
import org.kiwix.kiwixmobile.zimManager.libraryView.LibraryListItem.LibraryDownloadItem
import org.kiwix.libkiwix.Book
import javax.inject.Inject
import javax.inject.Provider
import kotlin.time.Duration.Companion.milliseconds

/**
 * ViewModel for the OnlineLibraryRoute composable.
 * Holds dependencies and business logic, emitting UI events for the composable to handle.
 */
@Suppress("LongParameterList", "LargeClass")
@HiltViewModel
class OnlineLibraryViewModel @Inject constructor(
  private val downloaderProvider: Provider<Downloader>,
  val kiwixDataStore: KiwixDataStore,
  val bookUtils: BookUtils,
  private val libkiwixBookOnDisk: LibkiwixBookOnDisk,
  private val downloadDao: DownloadRoomDao,
  val availableSpaceCalculator: AvailableSpaceCalculator,
  private val permissionChecker: KiwixPermissionChecker,
  val context: Application,
  private val connectivityObserver: ConnectivityObserver,
  private val connectivityManager: ConnectivityManager,
  private val observeOnlineLibraryItems: ObserveOnlineLibraryItems,
  private val resolveBookClickAction: ResolveBookClickAction,
  private val observeOnlineLibrary: ObserveOnlineLibrary,
  private val refreshLibraryAction: ResolveRefreshLibraryAction,
  private val observeNetworkState: ObserveNetworkState,
  private val storageDeviceProvider: StorageDeviceProvider,
  @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
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

  data class LanguageTab(
    val languageCode: String? = null,
    val displayName: String = ""
  )

  data class TabData(
    val books: List<LibkiwixBook> = emptyList(),
    val totalPages: Int = ZERO,
    val currentPage: Int = ZERO,
    val isLoadingMore: Boolean = false,
    val isLoaded: Boolean = false
  )

  data class OnlineLibraryUiState(
    val items: List<LibraryListItem> = emptyList(),
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val scanningProgressBarMessage: String = "",
    val showScanningProgressBar: Boolean = false,
    val noContentMessage: String = "",
    val showNoContent: Boolean = false,
    val showStorageSelectDialog: Boolean = false,
    val showCategoryDialog: Boolean = false,
    val tabs: List<LanguageTab> = emptyList(),
    val selectedTabIndex: Int = 0,
    val categoryChips: List<String> = emptyList(),
    val selectedCategories: Set<String> = emptySet()
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

    data class SideEffects(val uISideEffects: UISideEffects) : UiEvent()

    data class ShowToast(val message: String) : UiEvent()

    data class RequestPermission(val permission: String) : UiEvent()

    object NavigateToSettings : UiEvent()

    object NavigateToAppSettings : UiEvent()
    object ScrollToTop : UiEvent()
  }

  sealed class UISideEffects {
    data class StorageSelectionDialog(val dialogConfig: StorageSelectDialogConfig) : UISideEffects()
  }

  private val _uiEvents = MutableSharedFlow<UiEvent>(
    extraBufferCapacity = 1,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
  )
  val uiEvents = _uiEvents.asSharedFlow()

  private val _uiState = MutableStateFlow(OnlineLibraryUiState())
  val uiState = _uiState.asStateFlow()
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

  var downloadBookItem: BookItem? = null
    internal set

  @VisibleForTesting
  internal fun setUiStateForTest(state: OnlineLibraryUiState) {
    _uiState.value = state
  }

  init {
    connectivityObserver.register()
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

  private fun getString(resId: Int, vararg args: Any): String =
    context.getString(resId, *args)

  private suspend fun getDisplayLanguage(languageCode: String): String {
    val mappedLocale = bookUtils.localeMap[languageCode] ?: languageCode.convertToLocal()
    return mappedLocale.getDisplayLanguage(LocaleHelper.getAppLocale(context, kiwixDataStore))
  }

  private fun observeLibraryItems() = observeOnlineLibraryItems(
    localBooks = books(),
    downloads = downloadDao.downloads(),
    networkBooks = networkBooks,
    getString = { resId, args -> context.getString(resId, *args) },
    getSimpleString = { resId -> context.getString(resId) },
    getDisplayLanguage = { langCode -> getDisplayLanguage(langCode) }
  ).onEach {
    updateLibraryItems(it)
  }.catch { throwable ->
    resetDownloadState()
    throwable.printStackTrace()
    Log.e("OnlineLibraryViewModel", "Error----$throwable")
  }.launchIn(viewModelScope)

  private fun updateLibraryItems(items: List<LibraryListItem>) {
    _uiState.update { current ->
      current.copy(
        items = items,
        noContentMessage = noContentMessageWhenItemsComesFromOnlineSource(items),
        showNoContent = items.isEmpty()
      )
    }
  }

  private fun noContentMessageWhenItemsComesFromOnlineSource(items: List<LibraryListItem>): String =
    when {
      items.isEmpty() -> if (connectivityManager.isNetworkAvailable()) {
        context.getString(R.string.no_items_msg)
      } else {
        context.getString(R.string.no_network_connection)
      }

      else -> ""
    }

  internal data class FiltersInput(
    val category: String,
    val language: String,
    val searchQuery: String,
    val cachedCategories: List<Category>?
  )

  internal val tabDataMap = mutableMapOf<String, TabData>()

  internal suspend fun getAppChosenLanguageCode(): String {
    val appLocale = LocaleHelper.getAppLocale(context, kiwixDataStore)
    return try {
      appLocale.isO3Language.ifEmpty { appLocale.language }
    } catch (_: Exception) {
      appLocale.language
    }
  }

  internal suspend fun createTabs(languageString: String): List<LanguageTab> {
    if (languageString.equals("all", ignoreCase = true)) {
      return listOf(
        LanguageTab(
          languageCode = null,
          displayName = context.getString(R.string.all_languages).uppercase()
        )
      )
    }
    val languageCodes = languageString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    return if (languageCodes.isEmpty()) {
      val appLang = getAppChosenLanguageCode()
      if (appLang.isNotEmpty()) {
        listOf(
          LanguageTab(
            languageCode = appLang,
            displayName = getDisplayLanguage(appLang).uppercase()
          )
        )
      } else {
        listOf(
          LanguageTab(
            languageCode = null,
            displayName = context.getString(R.string.all_languages).uppercase()
          )
        )
      }
    } else {
      languageCodes.map { code ->
        LanguageTab(
          languageCode = code,
          displayName = getDisplayLanguage(code).uppercase()
        )
      }
    }
  }

  @OptIn(FlowPreview::class)
  @Suppress("MagicNumber")
  private fun observeFilters() =
    combine(
      kiwixDataStore.selectedOnlineContentCategory,
      kiwixDataStore.selectedOnlineContentLanguage,
      uiState.map { it.searchQuery }.distinctUntilChanged().debounce(500.milliseconds),
      kiwixDataStore.cachedOnlineCategoryList,
      kiwixDataStore.prefLanguage
    ) { category, language, searchQuery, cachedCategories, _ ->
      FiltersInput(category, language, searchQuery, cachedCategories)
    }
      .onEach { handleFiltersChanged(it) }
      .flowOn(ioDispatcher)
      .launchIn(viewModelScope)

  internal suspend fun handleFiltersChanged(input: FiltersInput) {
    val categoryChips =
      input.category.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    val activeSelectedCategories = uiState.value.selectedCategories.filter { selected ->
      categoryChips.any { it.equals(selected, ignoreCase = true) }
    }.toSet()

    val newTabs = createTabs(input.language)
    val currentTabCode = uiState.value.tabs.getOrNull(uiState.value.selectedTabIndex)?.languageCode
    val newIndex = newTabs.indexOfFirst { it.languageCode == currentTabCode }.coerceAtLeast(0)

    _uiState.update { current ->
      current.copy(
        tabs = newTabs,
        selectedTabIndex = newIndex,
        categoryChips = categoryChips,
        selectedCategories = activeSelectedCategories
      )
    }

    val activeLanguage = newTabs.getOrNull(newIndex)?.languageCode.orEmpty()
    val activeCategory = if (activeSelectedCategories.isNotEmpty()) {
      activeSelectedCategories.joinToString(",")
    } else {
      input.category.takeUnless { it.isBlank() }.orEmpty()
    }

    val newRequest = OnlineLibraryRequest(
      input.searchQuery.takeIf { it.isNotBlank() }.orEmpty(),
      activeCategory,
      activeLanguage,
      false,
      ZERO
    )
    if (newRequest.query != currentRequest.query ||
      newRequest.category != currentRequest.category ||
      newRequest.lang != currentRequest.lang
    ) {
      tabDataMap.clear()
      updateOnlineLibraryFilters(newRequest)
    }
  }

  fun selectTab(index: Int) {
    if (index !in uiState.value.tabs.indices) return
    if (index == uiState.value.selectedTabIndex) return
    _uiState.update { it.copy(selectedTabIndex = index) }
    val tab = uiState.value.tabs[index]
    val tabKey = tab.languageCode.orEmpty()
    val existingData = tabDataMap[tabKey]
    if (existingData != null && existingData.isLoaded) {
      currentRequest = currentRequest.copy(
        lang = tab.languageCode,
        page = existingData.currentPage,
        isLoadMoreItem = false
      )
      totalPages = existingData.totalPages
      networkBooks.value = existingData.books
      _uiState.update {
        it.copy(
          isLoadingMore = existingData.isLoadingMore,
          showNoContent = existingData.books.isEmpty() && !it.showScanningProgressBar,
          noContentMessage = if (existingData.books.isEmpty()) {
            noContentMessageWhenItemsComesFromOnlineSource(emptyList())
          } else {
            ""
          }
        )
      }
      if (existingData.books.isEmpty()) {
        updateLibraryItems(emptyList())
      }
    } else {
      val category = if (uiState.value.selectedCategories.isNotEmpty()) {
        uiState.value.selectedCategories.joinToString(",")
      } else {
        currentRequest.category
      }
      val newRequest = currentRequest.copy(
        lang = tab.languageCode.orEmpty(),
        category = category,
        page = ZERO,
        isLoadMoreItem = false
      )
      updateOnlineLibraryFilters(newRequest)
    }
  }

  fun onCategoryChipClicked(category: String) {
    val currentSelected = uiState.value.selectedCategories
    val isAlreadySelected = currentSelected.any { it.equals(category, ignoreCase = true) }
    val newSelected = if (isAlreadySelected) {
      currentSelected.filterNot { it.equals(category, ignoreCase = true) }.toSet()
    } else {
      currentSelected + category
    }
    _uiState.update { it.copy(selectedCategories = newSelected) }
    tabDataMap.clear()
    viewModelScope.launch {
      updateOnlineLibraryFilters(getOnlineLibraryRequest())
    }
  }

  internal fun updateOnlineLibraryFilters(newRequest: OnlineLibraryRequest) {
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

  private fun updateNetworkStates() =
    observeNetworkState(connectivityObserver.networkStates)
      .onEach { handleNetworkState(it) }
      .flowOn(ioDispatcher)
      .launchIn(viewModelScope)

  internal suspend fun handleNetworkState(state: ObserveNetworkState.Result) {
    when (state) {
      ObserveNetworkState.Result.WifiAvailable -> refreshScreen(false)
      ObserveNetworkState.Result.ShowWifiOnlyMessage -> {
        _uiState.update {
          it.copy(
            noContentMessage = getString(R.string.swipe_down_for_library),
            showNoContent = true,
            showScanningProgressBar = false
          )
        }
      }

      ObserveNetworkState.Result.ShowNoInternetSnackBar -> {
        if (uiState.value.items.isEmpty()) {
          _uiState.update {
            it.copy(
              noContentMessage = getString(R.string.no_network_connection),
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
              scanningProgressBarMessage = getString(R.string.reaching_remote_library),
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
    observeOnlineLibrary(onlineLibraryRequest)
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

      is Success -> handleSuccessState(state)

      is OnlineLibraryState.Error -> {
        if (networkBooks.value.isEmpty()) {
          updateLibraryItems(emptyList())
        }
        resetDownloadState()
      }
    }
  }

  private suspend fun handleSuccessState(state: Success) {
    val request = state.request
    val tabKey = runCatching { request.lang.orEmpty() }.getOrDefault("")
    val page = runCatching { request.page }.getOrDefault(ZERO)
    val isLoadMore = runCatching { request.isLoadMoreItem }.getOrDefault(false)
    val existingData = tabDataMap[tabKey] ?: TabData(books = networkBooks.value)
    val updatedBooks = if (isLoadMore) {
      existingData.books.ifEmpty { networkBooks.value } + state.books
    } else {
      state.books
    }
    val updatedData = TabData(
      books = updatedBooks,
      totalPages = state.totalPages,
      currentPage = page,
      isLoadingMore = false,
      isLoaded = true
    )
    tabDataMap[tabKey] = updatedData

    val currentSelectedTab = uiState.value.tabs.getOrNull(uiState.value.selectedTabIndex)
    val currentTabKey = currentSelectedTab?.languageCode.orEmpty()

    if (tabKey == currentTabKey || currentSelectedTab == null) {
      totalPages = state.totalPages
      networkBooks.emit(updatedBooks)
      if (!isLoadMore && updatedBooks.isNotEmpty()) {
        sendUiEvent(UiEvent.ScrollToTop)
      }
      resetDownloadState()
      if (updatedBooks.isEmpty()) {
        updateLibraryItems(emptyList())
      }
    } else {
      resetDownloadState()
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
        emitToast(getString(R.string.denied_internet_permission_message))
        _uiState.update {
          it.copy(
            noContentMessage = getString(R.string.swipe_down_for_library),
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
        scanningProgressBarMessage = getString(messageResId),
        noContentMessage = ""
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
        message = getString(R.string.no_network_connection),
        actionLabel = getString(R.string.menu_settings),
        actionIntent = Intent(Settings.ACTION_WIFI_SETTINGS)
      )
    )
  }

  private fun emitNoSpaceSnackbar(
    availableSpace: String,
    onStorageSelect: () -> Unit
  ) {
    sendUiEvent(
      ShowNoSpaceSnackbar(
        message = """
            ${getString(R.string.download_no_space)}
            ${getString(R.string.space_available)} $availableSpace
        """.trimIndent(),
        actionLabel = getString(R.string.change_storage),
        onAction = onStorageSelect
      )
    )
  }

  private fun emitToast(message: String) {
    sendUiEvent(ShowToast(message))
  }

  private fun emitDialog(
    dialog: KiwixDialog,
    positiveAction: () -> Unit = {},
    negativeAction: () -> Unit = {},
  ) {
    sendUiEvent(ShowDialog(dialog, negativeAction, positiveAction))
  }

  private fun downloadFile() {
    downloadBookItem?.book?.let { book ->
      withDownloader { it.download(book) }
      downloadBookItem = null
    }
  }

  private fun withDownloader(block: suspend (Downloader) -> Unit) {
    viewModelScope.launch(ioDispatcher) {
      block(downloaderProvider.get())
    }
  }

  fun onBookItemClick(item: BookItem) {
    viewModelScope.launch {
      downloadBookItem = item
      val action = resolveBookClickAction.onBookItemClick(
        item,
        storageDeviceProvider.getWritableStorage().size
      )
      when (action) {
        ShowStorageSelection -> showStorageSelectDialog(false)
        is StartDownload -> downloadFile()
        NoInternet -> emitNoInternetSnackbar()
        RequestStoragePermission -> sendUiEvent(RequestPermission(WRITE_EXTERNAL_STORAGE))
        RequestNotificationPermission -> if (isAndroid13OrAbove) {
          sendUiEvent(RequestPermission(POST_NOTIFICATIONS))
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
              onBookItemClick(item)
            }
          }
        )

        DisableStorageSelection -> {
          kiwixDataStore.setShowStorageOption(false)
          onBookItemClick(item)
        }

        is NotEnoughSpace -> emitNoSpaceSnackbar(action.availableSpace) {
          showStorageSelectDialog(true)
        }

        else -> Unit
      }
    }
  }

  fun onPauseResumeButtonClick(item: LibraryDownloadItem) {
    when (val result = resolveBookClickAction.onPauseResumeButtonClick(item)) {
      NoInternet -> emitNoInternetSnackbar()
      is PauseResume -> withDownloader {
        it.pauseResumeDownload(result.downloadId, result.isPaused)
      }

      else -> Unit
    }
  }

  fun onStopButtonClick(item: LibraryDownloadItem) {
    when (val result = resolveBookClickAction.onStopButtonClick(item)) {
      NoInternet -> emitNoInternetSnackbar()
      is RetryDownload -> withDownloader {
        it.retryDownload(result.downloadId)
      }

      is CancelDownload -> emitDialog(
        KiwixDialog.YesNoDialog.StopDownload,
        positiveAction = {
          withDownloader {
            it.cancelDownload(result.downloadId)
          }
        }
      )

      else -> Unit
    }
  }

  fun onNavigateToAppSettingsClicked() {
    sendUiEvent(NavigateToAppSettings)
  }

  private fun showStorageSelectDialog(showCheckboxSelected: Boolean) {
    viewModelScope.launch {
      val dialogConfig = StorageSelectDialogConfig(
        title = context.getString(R.string.choose_storage_to_download_book),
        titleSize = STORAGE_SELECT_STORAGE_TITLE_TEXTVIEW_SIZE,
        storageDeviceList = storageDeviceProvider.getWritableStorage(),
        storageCalculator = availableSpaceCalculator.storageCalculator,
        kiwixDataStore = kiwixDataStore,
        shouldShowCheckboxSelected = showCheckboxSelected,
        onSelectAction = { onStorageDeviceClick(it) }
      )
      sendUiEvent(SideEffects(UISideEffects.StorageSelectionDialog(dialogConfig)))
    }
  }

  private fun onStorageDeviceClick(device: StorageDevice) {
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
        onBookItemClick(it)
      }
    }
  }

  fun setShowCategoryDialog(show: Boolean) {
    _uiState.update { it.copy(showCategoryDialog = show) }
  }

  fun refreshScreen(isExplicitRefresh: Boolean) {
    viewModelScope.launch {
      when (refreshLibraryAction(uiState.value.items.isNotEmpty())) {
        Proceed -> {
          tabDataMap.clear()
          updateOnlineLibraryFilters(getOnlineLibraryRequest())
          if (isExplicitRefresh) {
            _uiState.update {
              it.copy(
                noContentMessage = "",
                showNoContent = false,
                showScanningProgressBar = true,
                scanningProgressBarMessage = getString(R.string.reaching_remote_library)
              )
            }
          }
        }

        NoInternetWithContent -> emitNoInternetSnackbar()
        NoInternetWithEmptyContent -> {
          _uiState.update {
            it.copy(
              noContentMessage = getString(R.string.no_network_connection),
              showNoContent = true,
              isRefreshing = false,
              showScanningProgressBar = false,
              scanningProgressBarMessage = getString(R.string.reaching_remote_library)
            )
          }
        }

        WifiOnlyBlocked -> showWifiOnlyDialog()
      }
    }
  }

  private suspend fun getOnlineLibraryRequest(): OnlineLibraryRequest {
    val category = if (uiState.value.selectedCategories.isNotEmpty()) {
      uiState.value.selectedCategories.joinToString(",")
    } else {
      kiwixDataStore.selectedOnlineContentCategory.first().takeUnless { it.isBlank() }.orEmpty()
    }
    val currentTab = uiState.value.tabs.getOrNull(uiState.value.selectedTabIndex)
    val configuredLanguage = kiwixDataStore.selectedOnlineContentLanguage.first()
    val language = currentTab?.languageCode
      ?: if (configuredLanguage.equals("all", ignoreCase = true)) {
        ""
      } else {
        configuredLanguage.split(",")
          .map { it.trim() }.firstOrNull { it.isNotBlank() }
          ?: getAppChosenLanguageCode()
      }
    return OnlineLibraryRequest(
      uiState.value.searchQuery.takeIf { it.isNotBlank() }.orEmpty(),
      category,
      language.orEmpty(),
      false,
      ZERO
    )
  }

  fun handleLoadMore(count: Int) {
    val currentTab = uiState.value.tabs.getOrNull(uiState.value.selectedTabIndex)
    val tabKey = currentTab?.languageCode.orEmpty()
    val existingData = tabDataMap[tabKey]
    val totalPagesForTab = if (existingData != null && existingData.totalPages > 0) {
      existingData.totalPages
    } else {
      totalPages
    }
    val currentPage = if (count > ZERO) (count - ONE) / ITEMS_PER_PAGE else ZERO
    val nextPage = currentPage + ONE
    if (uiState.value.isLoadingMore || existingData?.isLoadingMore == true) return
    if (nextPage < totalPagesForTab) {
      existingData?.let { tabDataMap[tabKey] = it.copy(isLoadingMore = true) }
      _uiState.update { it.copy(isLoadingMore = true) }
      updateOnlineLibraryFilters(
        currentRequest.copy(
          lang = currentTab?.languageCode,
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
    connectivityObserver.unregister()
    observeOnlineLibraryItems.dispose()
    super.onCleared()
  }

  fun onNotificationPermissionResult(isGranted: Boolean) {
    if (isGranted) {
      downloadBookItem?.let { onBookItemClick(it) }
      return
    }
    viewModelScope.launch {
      kiwixDataStore.setHasSeenNotificationPermissionDeniedInfo()
      emitDialog(
        KiwixDialog.NotificationPermissionDeniedInfoDialog,
        positiveAction = { downloadBookItem?.let { onBookItemClick(it) } },
        negativeAction = ::onNavigateToAppSettingsClicked
      )
    }
  }

  fun onStoragePermissionResult(isGranted: Boolean, activity: KiwixMainActivity) {
    if (isGranted) {
      downloadBookItem?.let { onBookItemClick(it) }
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
