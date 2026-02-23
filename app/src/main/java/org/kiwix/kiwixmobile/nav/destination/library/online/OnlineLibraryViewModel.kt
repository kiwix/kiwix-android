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

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.data.remote.KiwixService.Companion.ITEMS_PER_PAGE
import org.kiwix.kiwixmobile.core.downloader.Downloader
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.hasNotificationPermission
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.isManageExternalStoragePermissionGranted
import org.kiwix.kiwixmobile.core.utils.BookUtils
import org.kiwix.kiwixmobile.core.utils.EXTERNAL_SELECT_POSITION
import org.kiwix.kiwixmobile.core.utils.INTERNAL_SELECT_POSITION
import org.kiwix.kiwixmobile.core.utils.KiwixPermissionChecker
import org.kiwix.kiwixmobile.core.utils.NetworkUtils
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.storage.StorageSelectDialog
import org.kiwix.kiwixmobile.storage.STORAGE_SELECT_STORAGE_TITLE_TEXTVIEW_SIZE
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel
import org.kiwix.kiwixmobile.zimManager.libraryView.AvailableSpaceCalculator
import org.kiwix.kiwixmobile.zimManager.libraryView.LibraryListItem
import javax.inject.Inject

/**
 * ViewModel for the OnlineLibraryRoute composable.
 * Holds dependencies and business logic, emitting UI events for the composable to handle.
 */
class OnlineLibraryViewModel @Inject constructor(
  val downloader: Downloader,
  val kiwixDataStore: KiwixDataStore,
  val bookUtils: BookUtils,
  val availableSpaceCalculator: AvailableSpaceCalculator,
  val permissionChecker: KiwixPermissionChecker
) : ViewModel() {
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
      val positiveAction: () -> Unit = {},
      val negativeAction: () -> Unit = {}
    ) : UiEvent()

    data class ShowToast(val message: String) : UiEvent()

    data class RequestPermission(val permission: String) : UiEvent()

    object NavigateToSettings : UiEvent()

    object NavigateToAppSettings : UiEvent()
  }

  private val _uiEvents = MutableSharedFlow<UiEvent>(extraBufferCapacity = 10)
  val uiEvents = _uiEvents.asSharedFlow()

  private val _scanningProgress = MutableStateFlow<Pair<Boolean, String>>(false to "")
  val scanningProgress = _scanningProgress.asStateFlow()

  private val _noContentState = MutableStateFlow<Pair<String, Boolean>>("" to false)
  val noContentState = _noContentState.asStateFlow()

  var downloadBookItem: LibraryListItem.BookItem? = null
    private set

  fun setDownloadBookItem(item: LibraryListItem.BookItem?) {
    downloadBookItem = item
  }

  fun emitNoInternetSnackbar(context: android.content.Context) {
    viewModelScope.launch {
      _uiEvents.emit(
        UiEvent.ShowSnackbar(
          message = context.getString(R.string.no_network_connection),
          actionLabel = context.getString(R.string.menu_settings),
          actionIntent = Intent(Settings.ACTION_WIFI_SETTINGS)
        )
      )
    }
  }

  fun emitNoSpaceSnackbar(
    context: android.content.Context,
    availableSpace: String,
    onStorageSelect: () -> Unit
  ) {
    viewModelScope.launch {
      _uiEvents.emit(
        UiEvent.ShowNoSpaceSnackbar(
          message = """
            ${context.getString(R.string.download_no_space)}
            ${context.getString(R.string.space_available)} $availableSpace
          """.trimIndent(),
          actionLabel = context.getString(R.string.change_storage),
          onAction = onStorageSelect
        )
      )
    }
  }

  fun emitToast(message: String) {
    viewModelScope.launch {
      _uiEvents.emit(UiEvent.ShowToast(message))
    }
  }

  fun emitDialog(
    dialog: KiwixDialog,
    positiveAction: () -> Unit = {},
    negativeAction: () -> Unit = {}
  ) {
    viewModelScope.launch {
      _uiEvents.emit(UiEvent.ShowDialog(dialog, positiveAction, negativeAction))
    }
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
            _uiEvents.tryEmit(UiEvent.NavigateToSettings)
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
        setShouldShowCheckboxSelected(false)
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
    zimManageViewModel: ZimManageViewModel,
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
      zimManageViewModel.updateOnlineLibraryFilters(
        getOnlineLibraryRequest()
      )
      if (isExplicitRefresh) {
        _noContentState.value = "" to false
      }
    }
  }

  suspend fun getOnlineLibraryRequest(): ZimManageViewModel.OnlineLibraryRequest {
    val category =
      kiwixDataStore.selectedOnlineContentCategory.first().takeUnless { it.isBlank() }
    val language =
      kiwixDataStore.selectedOnlineContentLanguage.first().takeUnless { it.isBlank() }
    return ZimManageViewModel.OnlineLibraryRequest(
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
      val currentPage = count / ITEMS_PER_PAGE
      val nextPage = currentPage + 1
      if (nextPage < totalPages) {
        zimManageViewModel.updateOnlineLibraryFilters(
          zimManageViewModel.onlineLibraryRequest.value.copy(
            page = nextPage,
            isLoadMoreItem = true
          )
        )
      }
    }
  }

  fun handleNetworkConnected(
    context: android.content.Context,
    activity: KiwixMainActivity,
    zimManageViewModel: ZimManageViewModel,
    isListEmpty: Boolean,
    onRefreshingChanged: (Boolean) -> Unit
  ) {
    viewModelScope.launch {
      when {
        NetworkUtils.isWiFi(context) -> {
          refreshFragment(
            activity,
            zimManageViewModel,
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
          zimManageViewModel.updateOnlineLibraryFilters(getOnlineLibraryRequest())
          _noContentState.value = "" to false
          onRefreshingChanged(false)
          _scanningProgress.value =
            true to context.getString(R.string.reaching_remote_library)
        }
      }
    }
  }
}
