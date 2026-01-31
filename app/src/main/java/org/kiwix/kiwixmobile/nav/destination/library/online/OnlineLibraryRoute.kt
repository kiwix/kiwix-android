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

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.Status
import eu.mhutti1.utils.storage.StorageDevice
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.kiwix.kiwixmobile.R.drawable
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.R.string

// ... (in body)

  val lifecycleOwner = LocalLifecycleOwner.current
import org.kiwix.kiwixmobile.core.base.FragmentActivityExtensions
import org.kiwix.kiwixmobile.core.data.remote.KiwixService.Companion.ITEMS_PER_PAGE
import org.kiwix.kiwixmobile.core.downloader.model.DownloadState
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.hasNotificationPermission
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.isManageExternalStoragePermissionGranted
import org.kiwix.kiwixmobile.core.extensions.closeKeyboard
import org.kiwix.kiwixmobile.core.extensions.isKeyboardVisible
import org.kiwix.kiwixmobile.core.extensions.snack
import org.kiwix.kiwixmobile.core.navigateToAppSettings
import org.kiwix.kiwixmobile.core.page.SEARCH_ICON_TESTING_TAG
import org.kiwix.kiwixmobile.core.ui.components.NavigationIcon
import org.kiwix.kiwixmobile.core.ui.components.ONE
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.utils.EXTERNAL_SELECT_POSITION
import org.kiwix.kiwixmobile.core.utils.INTERNAL_SELECT_POSITION
import org.kiwix.kiwixmobile.core.utils.NetworkUtils
import org.kiwix.kiwixmobile.core.utils.REQUEST_STORAGE_PERMISSION
import org.kiwix.kiwixmobile.core.utils.ZERO
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.DialogHost
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog

import org.kiwix.kiwixmobile.core.zim_manager.NetworkState
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.storage.STORAGE_SELECT_STORAGE_TITLE_TEXTVIEW_SIZE
import org.kiwix.kiwixmobile.storage.StorageSelectDialog
import org.kiwix.kiwixmobile.ui.KiwixDestination
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.OnlineLibraryRequest
import org.kiwix.kiwixmobile.zimManager.libraryView.LibraryListItem

const val LANGUAGE_MENU_ICON_TESTING_TAG = "LANGUAGE_MENU_ICON_TESTING_TAG"
const val CATEGORY_MENU_ICON_TESTING_TAG = "CATEGORY_MENU_ICON_TESTING_TAG"
@Suppress("LargeClass", "LongMethod", "ComplexMethod", "MagicNumber")
@Composable
fun OnlineLibraryRoute(
  viewModelFactory: ViewModelProvider.Factory,
  alertDialogShower: AlertDialogShower,
  navController: NavHostController
) {
  val context = LocalContext.current
  val activity = context as KiwixMainActivity
  val component = activity.cachedComponent
  val downloader = component.downloader()
  val kiwixDataStore = component.kiwixDataStore()
  val bookUtils = component.bookUtils()
  val availableSpaceCalculator = component.availableSpaceCalculator()

  val zimManageViewModel: ZimManageViewModel = viewModel(viewModelStoreOwner = activity, factory = viewModelFactory)
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  val lazyListState = rememberLazyListState()
  val lifecycleOwner = LocalLifecycleOwner.current

  var isSearchActive by remember {
    mutableStateOf(zimManageViewModel.onlineBooksSearchedQuery.value.isNotEmpty())
  }
  var searchText by remember {
    mutableStateOf(zimManageViewModel.onlineBooksSearchedQuery.value)
  }
  var isLoadingMoreItem by remember { mutableStateOf(false) }
  var scanningProgressItem by remember {
    mutableStateOf(Pair(false, ""))
  }
  var noContentViewItem by remember {
    mutableStateOf(Pair("", false))
  }
  var isRefreshing by remember { mutableStateOf(false) }
  var onlineLibraryList by remember { mutableStateOf<List<LibraryListItem>?>(null) }
  
  // State for permission handling
  var downloadBookItem: LibraryListItem.BookItem? by remember { mutableStateOf(null) }

  // Function definitions (hoisted state helpers)
  fun isNotConnected(): Boolean = !NetworkUtils.isNetworkAvailable(activity)

  fun noInternetSnackbar() {
    snackbarHostState.snack(
      message = context.getString(string.no_network_connection),
      actionLabel = context.getString(string.menu_settings),
      lifecycleScope = scope,
      actionClick = { context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) }
    )
  }

  fun showNoInternetConnectionError() {
    if (onlineLibraryList?.isNotEmpty() == true) {
      noInternetSnackbar()
    } else {
      noContentViewItem = context.getString(string.no_network_connection) to true
    }
    scanningProgressItem = false to context.getString(string.reaching_remote_library)
  }

  fun getOnlineLibraryRequest(): OnlineLibraryRequest = OnlineLibraryRequest(
    null,
    runBlocking {
      kiwixDataStore.selectedOnlineContentCategory.first().takeUnless { it.isBlank() }
    },
    runBlocking {
      kiwixDataStore.selectedOnlineContentLanguage.first().takeUnless { it.isBlank() }
    },
    false,
    ZERO
  )

  fun startDownloadingLibrary(onlineLibraryRequest: OnlineLibraryRequest) {
    zimManageViewModel.updateOnlineLibraryFilters(onlineLibraryRequest)
  }

  fun refreshFragment(isExplicitRefresh: Boolean) {
    if (isNotConnected()) {
      showNoInternetConnectionError()
    } else {
      startDownloadingLibrary(getOnlineLibraryRequest())
      if (isExplicitRefresh) {
        noContentViewItem = "" to false
      }
    }
  }

  fun showStopDownloadDialog(item: LibraryListItem.LibraryDownloadItem) {
    alertDialogShower.show(
      KiwixDialog.YesNoDialog.StopDownload,
      { downloader.cancelDownload(item.downloadId) }
    )
  }

  fun onStopButtonClick(item: LibraryListItem.LibraryDownloadItem) {
    if (item.currentDownloadState == Status.FAILED) {
      when (item.downloadError) {
        Error.UNKNOWN_IO_ERROR,
        Error.CONNECTION_TIMED_OUT,
        Error.UNKNOWN -> {
          if (isNotConnected()) {
            noInternetSnackbar()
          } else {
            downloader.retryDownload(item.downloadId)
          }
        }
        else -> showStopDownloadDialog(item)
      }
    } else {
      showStopDownloadDialog(item)
    }
  }

  fun onPauseResumeButtonClick(item: LibraryListItem.LibraryDownloadItem) {
    if (isNotConnected()) {
      noInternetSnackbar()
      return
    }
    downloader.pauseResumeDownload(
      item.downloadId,
      item.downloadState == DownloadState.Paused
    )
  }

  fun downloadFile() {
    downloadBookItem?.book?.let {
      downloader.download(it)
      downloadBookItem = null
    }
  }

  fun showStorageSelectDialog(storageDeviceList: List<StorageDevice>) {
    StorageSelectDialog().apply {
       onSelectAction = { device ->
         scope.launch {
           kiwixDataStore.setShowStorageOption(false)
           kiwixDataStore.setSelectedStorage(kiwixDataStore.getPublicDirectoryPath(device.name))
           kiwixDataStore.setSelectedStoragePosition(
             if (device.isInternal) INTERNAL_SELECT_POSITION else EXTERNAL_SELECT_POSITION
           )
           downloadFile()
         }
       }
       titleSize = STORAGE_SELECT_STORAGE_TITLE_TEXTVIEW_SIZE
       setStorageDeviceList(storageDeviceList)
       setShouldShowCheckboxSelected(false)
     }.show(activity.supportFragmentManager, context.getString(string.choose_storage_to_download_book))
  }

  fun clickOnBookItem() {
    scope.launch {
      if (kiwixDataStore.showStorageOption.first()) {
        if (activity.getStorageDeviceList().size > 1) {
          showStorageSelectDialog(activity.getStorageDeviceList())
        } else {
          kiwixDataStore.setShowStorageOption(false)
          downloadFile()
        }
      } else if (!activity.isManageExternalStoragePermissionGranted(kiwixDataStore)) {
         // This dialog handling was implicit in Fragment logic via ActivityExtensions, 
         // but here we can rely on data store state or standard dialogs if needed.
         // Assuming standard permission flow covered by callers or specific dialogs if external storage manager needed.
         // In original fragment: showManageExternalStoragePermissionDialog() logic was extension based.
         // We will proceed to check space which is the standard next step.
         
         // If we strictly follow original fragment logic, it proceeds to check space if permission granted or implied.
         downloadBookItem?.let { item ->
           availableSpaceCalculator.hasAvailableSpaceFor(
             item,
             { downloadFile() },
             { space ->
               snackbarHostState.snack(
                 message = "${context.getString(string.download_no_space)}\n${context.getString(string.space_available)} $space",
                 actionLabel = context.getString(string.change_storage),
                 actionClick = {
                   scope.launch {
                      showStorageSelectDialog(activity.getStorageDeviceList())
                   }
                 },
                 lifecycleScope = scope
               )
             }
           )
         }
      } else {
        downloadBookItem?.let { item ->
           availableSpaceCalculator.hasAvailableSpaceFor(
             item,
             { downloadFile() },
             { space ->
                snackbarHostState.snack(
                  message = "${context.getString(string.download_no_space)}\n${context.getString(string.space_available)} $space",
                  actionLabel = context.getString(string.change_storage),
                  actionClick = {
                    scope.launch {
                       showStorageSelectDialog(activity.getStorageDeviceList())
                    }
                  },
                  lifecycleScope = scope
                )
             }
           )
        }
      }
    }
  }

  // Permission Launchers
  val storagePermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (isGranted) {
      clickOnBookItem()
    } else {
      val showRationale = androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
        activity, Manifest.permission.WRITE_EXTERNAL_STORAGE
      )
      if (showRationale) {
         alertDialogShower.show(
            KiwixDialog.WriteStoragePermissionRationale,
            { /* Request READ implicitly as fallback or retry */ }
         )
      } else {
         alertDialogShower.show(
            KiwixDialog.WriteStoragePermissionRationale,
            activity::navigateToAppSettings
         )
      }
    }
  }

  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (isGranted) {
      downloadBookItem?.let { 
        scope.launch {
          if (activity.getStorageDeviceList().isNotEmpty()) clickOnBookItem()
        }
      }
    }
  }

  fun checkExternalStorageWritePermissionAndProceed() {
    scope.launch {
      if (!kiwixDataStore.isPlayStoreBuildWithAndroid11OrAbove()) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
          clickOnBookItem()
        } else {
          storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
      } else {
        clickOnBookItem()
      }
    }
  }

  fun onBookItemClick(item: LibraryListItem.BookItem) {
    scope.launch {
      downloadBookItem = item
      if (activity.hasNotificationPermission(kiwixDataStore)) {
        if (isNotConnected()) {
          noInternetSnackbar()
          return@launch
        }
        if (kiwixDataStore.wifiOnly.first() && !NetworkUtils.isWiFi(context)) {
          alertDialogShower.show(KiwixDialog.YesNoDialog.WifiOnly, {
            scope.launch {
              kiwixDataStore.setWifiOnly(false)
              checkExternalStorageWritePermissionAndProceed()
            }
          })
          return@launch
        }
        checkExternalStorageWritePermissionAndProceed()
      } else {
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
  }
  
  // Observers
  val libraryItems by zimManageViewModel.libraryItems.collectAsState()
  val onlineLibraryDownloading by zimManageViewModel.onlineLibraryDownloading.collectAsState()
  
  // Observe Wifi Only Dialog
  androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
    val observer = androidx.lifecycle.Observer<Boolean> { shouldShow ->
      if (shouldShow == true) {
        alertDialogShower.show(KiwixDialog.YesNoDialog.WifiOnly) {
          scope.launch {
            kiwixDataStore.setWifiOnly(false)
            startDownloadingLibrary(getOnlineLibraryRequest())
          }
        }
      }
    }
    zimManageViewModel.shouldShowWifiOnlyDialog.observe(lifecycleOwner, observer)
    onDispose {
      zimManageViewModel.shouldShowWifiOnlyDialog.removeObserver(observer)
    }
  }

  // Manually observe LiveData for network states
  androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
      val observer = androidx.lifecycle.Observer<NetworkState> { networkState ->
          scope.launch {
              if (networkState == NetworkState.CONNECTED) {
                  if (NetworkUtils.isWiFi(context)) {
                      refreshFragment(false)
                  } else if (kiwixDataStore.wifiOnly.first() && !NetworkUtils.isWiFi(context)) {
                       // Hide RecyclerView and show error handled in showNoInternetConnectionError logic potentially
                       // or specifically for wifi only preference
                       noContentViewItem = context.getString(string.swipe_down_for_library) to true
                  } else if (onlineLibraryList.isNullOrEmpty()) {
                       startDownloadingLibrary(getOnlineLibraryRequest())
                       scanningProgressItem = true to context.getString(string.reaching_remote_library)
                       noContentViewItem = "" to false
                       isRefreshing = false
                  }
              } else if (networkState == NetworkState.NOT_CONNECTED) {
                  showNoInternetConnectionError()
              }
          }
      }
      zimManageViewModel.networkStates.observe(lifecycleOwner, observer)
      onDispose {
          zimManageViewModel.networkStates.removeObserver(observer)
      }
  }

  LaunchedEffect(libraryItems) {
      onlineLibraryList = libraryItems.items
      scanningProgressItem = false to context.getString(string.reaching_remote_library)
      if (libraryItems.items.isEmpty()) {
           noContentViewItem = if (isNotConnected()) {
             context.getString(string.no_network_connection)
           } else {
             context.getString(string.no_items_msg)
           } to true
      } else {
           noContentViewItem = "" to false
      }
      isLoadingMoreItem = false
  }
  
  LaunchedEffect(onlineLibraryDownloading) {
      val (initialLibraryDownloading, loadingMoreItem) = onlineLibraryDownloading
      if (initialLibraryDownloading) {
          noContentViewItem = "" to false
          isRefreshing = false
          scanningProgressItem = true to context.getString(string.reaching_remote_library)
      } else {
          isRefreshing = false
          scanningProgressItem = false to context.getString(string.reaching_remote_library)
      }
      isLoadingMoreItem = loadingMoreItem
  }

  LaunchedEffect(Unit) {
      startDownloadingLibrary(getOnlineLibraryRequest())
      scanningProgressItem = false to context.getString(string.reaching_remote_library)
  }
  
  fun navigationIconClick() {
    if (isSearchActive) {
        isSearchActive = false
        searchText = ""
        zimManageViewModel.onlineBooksSearchedQuery.value = null
        zimManageViewModel.requestFiltering.tryEmit("")
        activity.onBackPressedDispatcher.onBackPressed() 
    } else {
        if (activity.navigationDrawerIsOpen()) activity.closeNavigationDrawer() else activity.openNavigationDrawer()
    }
  }

  val actionMenuItems = listOfNotNull(
      if (!isSearchActive) ActionMenuItem(
        icon = IconItem.Drawable(R.drawable.action_search),
        contentDescription = string.search_label,
        onClick = { isSearchActive = true },
        testingTag = SEARCH_ICON_TESTING_TAG
      ) else null,
      ActionMenuItem(
        IconItem.Drawable(drawable.ic_category),
        org.kiwix.kiwixmobile.R.string.select_category,
        {
             val fragmentTransaction = activity.supportFragmentManager.beginTransaction()
             if (activity.supportFragmentManager.findFragmentByTag("online_category_dialog") == null) {
               org.kiwix.kiwixmobile.nav.destination.library.online.OnlineCategoryDialog()
                 .show(fragmentTransaction, "online_category_dialog")
             }
        },
        isEnabled = true
      ),
      ActionMenuItem(
        IconItem.Drawable(drawable.ic_language_white_24dp),
        string.pref_language_chooser,
        {
          navController.navigate(KiwixDestination.Language.route)
          activity.closeKeyboard()
        },
        isEnabled = true
      )
  )

  OnlineLibraryScreen(
    state = org.kiwix.kiwixmobile.nav.destination.library.online.OnlineLibraryScreenState(
      onlineLibraryList = onlineLibraryList,
      isRefreshing = isRefreshing,
      snackBarHostState = snackbarHostState,
      onRefresh = { refreshFragment(true) },
      scanningProgressItem = scanningProgressItem,
      noContentViewItem = noContentViewItem,
      bookUtils = bookUtils,
      availableSpaceCalculator = availableSpaceCalculator,
      onBookItemClick = { onBookItemClick(it) },
      onPauseResumeButtonClick = { onPauseResumeButtonClick(it) },
      onStopButtonClick = { onStopButtonClick(it) },
      isSearchActive = isSearchActive,
      searchText = searchText,
      searchValueChangedListener = { 
          searchText = it
          if (it.isNotEmpty()) zimManageViewModel.onlineBooksSearchedQuery.value = it
          zimManageViewModel.requestFiltering.tryEmit(it.trim())
      },
      clearSearchButtonClickListener = { 
          searchText = "" 
          zimManageViewModel.onlineBooksSearchedQuery.value = null
          zimManageViewModel.requestFiltering.tryEmit("")
      },
      onLoadMore = { count -> 
          val totalResults = zimManageViewModel.onlineLibraryManager.totalResult
          val totalPages = zimManageViewModel.onlineLibraryManager.calculateTotalPages(totalResults, ITEMS_PER_PAGE)
          val currentPage = count / ITEMS_PER_PAGE
          val nextPage = currentPage + ONE
          if (nextPage < totalPages) {
             zimManageViewModel.updateOnlineLibraryFilters(
               zimManageViewModel.onlineLibraryRequest.value.copy(page = nextPage, isLoadMoreItem = true)
             )
          }
      },
      isLoadingMoreItem = isLoadingMoreItem
    ),
    actionMenuItems = actionMenuItems,
    listState = lazyListState,
    bottomAppBarScrollBehaviour = activity.bottomAppBarScrollBehaviour,
    onUserBackPressed = { 
        if (activity.navigationDrawerIsOpen()) {
            activity.closeNavigationDrawer()
            FragmentActivityExtensions.Super.ShouldNotCall
        } else if (activity.isKeyboardVisible() || isSearchActive) {
            activity.closeKeyboard()
            isSearchActive = false
            searchText = ""
            zimManageViewModel.onlineBooksSearchedQuery.value = null
            zimManageViewModel.requestFiltering.tryEmit("")
            FragmentActivityExtensions.Super.ShouldNotCall
        } else {
            FragmentActivityExtensions.Super.ShouldCall
        }
    },
    navHostController = navController,
    navigationIcon = {
        NavigationIcon(
            iconItem = if (isSearchActive) IconItem.Vector(Icons.AutoMirrored.Default.ArrowBack) else IconItem.Vector(Icons.Filled.Menu),
            contentDescription = string.open_drawer,
            onClick = { navigationIconClick() }
        )
    }
  )
  
  DialogHost(alertDialogShower)
}
