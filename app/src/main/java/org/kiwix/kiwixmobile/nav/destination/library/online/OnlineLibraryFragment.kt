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
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.tonyodev.fetch2.Status
import eu.mhutti1.utils.storage.StorageDevice
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.R.drawable
import org.kiwix.kiwixmobile.cachedComponent
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.base.BaseFragment
import org.kiwix.kiwixmobile.core.base.FragmentActivityExtensions
import org.kiwix.kiwixmobile.core.downloader.Downloader
import org.kiwix.kiwixmobile.core.downloader.downloadManager.ZERO
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.hasNotificationPermission
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.isManageExternalStoragePermissionGranted
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.navigate
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.requestNotificationPermission
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.viewModel
import org.kiwix.kiwixmobile.core.extensions.closeKeyboard
import org.kiwix.kiwixmobile.core.extensions.coreMainActivity
import org.kiwix.kiwixmobile.core.extensions.isKeyboardVisible
import org.kiwix.kiwixmobile.core.extensions.setBottomMarginToFragmentContainerView
import org.kiwix.kiwixmobile.core.extensions.snack
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.extensions.update
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.navigateToAppSettings
import org.kiwix.kiwixmobile.core.navigateToSettings
import org.kiwix.kiwixmobile.core.page.SEARCH_ICON_TESTING_TAG
import org.kiwix.kiwixmobile.core.ui.components.NavigationIcon
import org.kiwix.kiwixmobile.core.ui.components.rememberBottomNavigationVisibility
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.utils.BookUtils
import org.kiwix.kiwixmobile.core.utils.EXTERNAL_SELECT_POSITION
import org.kiwix.kiwixmobile.core.utils.INTERNAL_SELECT_POSITION
import org.kiwix.kiwixmobile.core.utils.NetworkUtils
import org.kiwix.kiwixmobile.core.utils.REQUEST_POST_NOTIFICATION_PERMISSION
import org.kiwix.kiwixmobile.core.utils.REQUEST_STORAGE_PERMISSION
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.DialogHost
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.core.zim_manager.NetworkState
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.storage.STORAGE_SELECT_STORAGE_TITLE_TEXTVIEW_SIZE
import org.kiwix.kiwixmobile.storage.StorageSelectDialog
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel
import org.kiwix.kiwixmobile.zimManager.libraryView.AvailableSpaceCalculator
import org.kiwix.kiwixmobile.zimManager.libraryView.adapter.LibraryListItem
import javax.inject.Inject

const val LANGUAGE_MENU_ICON_TESTING_TAG = "languageMenuIconTestingTag"

class OnlineLibraryFragment : BaseFragment(), FragmentActivityExtensions {
  @Inject lateinit var conMan: ConnectivityManager

  @Inject lateinit var downloader: Downloader

  @Inject lateinit var sharedPreferenceUtil: SharedPreferenceUtil

  @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

  @Inject lateinit var bookUtils: BookUtils

  @Inject lateinit var availableSpaceCalculator: AvailableSpaceCalculator

  @Inject lateinit var alertDialogShower: AlertDialogShower
  private val lock = Any()
  private var downloadBookItem: LibraryListItem.BookItem? = null
  private var composeView: ComposeView? = null
  private val zimManageViewModel by lazy {
    requireActivity().viewModel<ZimManageViewModel>(viewModelFactory)
  }
  private val onlineLibraryScreenState = lazy {
    mutableStateOf(
      OnlineLibraryScreenState(
        onlineLibraryList = null,
        snackBarHostState = SnackbarHostState(),
        swipeRefreshItem = Pair(false, true),
        scanningProgressItem = Pair(false, ""),
        noContentViewItem = Pair("", false),
        bottomNavigationHeight = ZERO,
        onBookItemClick = { onBookItemClick(it) },
        availableSpaceCalculator = availableSpaceCalculator,
        onRefresh = { refreshFragment() },
        bookUtils = bookUtils,
        onPauseResumeButtonClick = { onPauseResumeButtonClick(it) },
        onStopButtonClick = { onStopButtonClick(it) },
        isSearchActive = false,
        searchText = "",
        searchValueChangedListener = { onSearchValueChanged(it) },
        clearSearchButtonClickListener = { onSearchClear() }
      )
    )
  }

  private fun onSearchClear() {
    onlineLibraryScreenState.value.update {
      copy(searchText = "")
    }
    zimManageViewModel.onlineBooksSearchedQuery.value = null
    zimManageViewModel.requestFiltering.onNext("")
  }

  private fun onSearchValueChanged(searchText: String) {
    if (searchText.isNotEmpty()) {
      // Store only when query is not empty because when device going to sleep,
      // then `viewLifecycleOwner` tries to clear the written text in searchView
      // and due to that, this listener fired with empty query which resets the search.
      zimManageViewModel.onlineBooksSearchedQuery.value = searchText
    }
    onlineLibraryScreenState.value.update {
      copy(searchText = searchText)
    }
    zimManageViewModel.requestFiltering.onNext(searchText)
  }

  private val noWifiWithWifiOnlyPreferenceSet
    get() = sharedPreferenceUtil.prefWifiOnly && !NetworkUtils.isWiFi(requireContext())

  private val isNotConnected: Boolean
    get() = !NetworkUtils.isNetworkAvailable(requireActivity())

  override fun inject(baseActivity: BaseActivity) {
    baseActivity.cachedComponent.inject(this)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? = ComposeView(requireContext()).also {
    composeView = it
  }

  private fun getBottomNavigationView() =
    requireActivity().findViewById<BottomNavigationView>(org.kiwix.kiwixmobile.R.id.bottom_nav_view)

  private fun getBottomNavigationHeight() = getBottomNavigationView().measuredHeight

  private fun onPauseResumeButtonClick(item: LibraryListItem.LibraryDownloadItem) {
    context?.let { context ->
      if (isNotConnected) {
        noInternetSnackbar()
        return@let
      }
      downloader.pauseResumeDownload(
        item.downloadId,
        item.downloadState.toReadableState(context) == getString(string.paused_state)
      )
    }
  }

  private fun onStopButtonClick(item: LibraryListItem.LibraryDownloadItem) {
    if (item.currentDownloadState == Status.FAILED) {
      if (isNotConnected) {
        noInternetSnackbar()
      } else {
        downloader.retryDownload(item.downloadId)
      }
    } else {
      alertDialogShower.show(
        KiwixDialog.YesNoDialog.StopDownload,
        { downloader.cancelDownload(item.downloadId) }
      )
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    composeView?.setContent {
      val lazyListState = rememberLazyListState()
      val isBottomNavVisible = rememberBottomNavigationVisibility(lazyListState)
      LaunchedEffect(isBottomNavVisible) {
        (requireActivity() as KiwixMainActivity).toggleBottomNavigation(isBottomNavVisible)
      }
      LaunchedEffect(Unit) {
        onlineLibraryScreenState.value.update {
          copy(
            bottomNavigationHeight = getBottomNavigationHeight(),
            isSearchActive = isSearchActive,
            scanningProgressItem = false to getString(R.string.reaching_remote_library)
          )
        }
      }
      OnlineLibraryScreen(
        state = onlineLibraryScreenState.value.value,
        listState = lazyListState,
        actionMenuItems = actionMenuItems {
          onlineLibraryScreenState.value.update { copy(isSearchActive = true) }
        },
        navigationIcon = {
          NavigationIcon(
            iconItem = navigationIconItem(onlineLibraryScreenState.value.value.isSearchActive),
            contentDescription = string.open_drawer,
            onClick = { navigationIconClick(onlineLibraryScreenState.value.value.isSearchActive) }
          )
        }
      )
      DialogHost(alertDialogShower)
    }
    zimManageViewModel.libraryItems.observe(viewLifecycleOwner, Observer(::onLibraryItemsChange))
      .also {
        coreMainActivity.navHostContainer
          .setBottomMarginToFragmentContainerView(0)
      }
    zimManageViewModel.libraryListIsRefreshing.observe(
      viewLifecycleOwner,
      Observer { onRefreshStateChange(it, true) }
    )
    zimManageViewModel.networkStates.observe(viewLifecycleOwner, Observer(::onNetworkStateChange))
    zimManageViewModel.shouldShowWifiOnlyDialog.observe(
      viewLifecycleOwner
    ) {
      if (it && !NetworkUtils.isWiFi(requireContext())) {
        showInternetAccessViaMobileNetworkDialog()
        hideProgressBarOfFetchingOnlineLibrary()
      }
    }
    zimManageViewModel.downloadProgress.observe(viewLifecycleOwner, ::onLibraryStatusChanged)
    showPreviouslySearchedTextInSearchView()
  }

  private fun showPreviouslySearchedTextInSearchView() {
    zimManageViewModel.onlineBooksSearchedQuery.value.takeIf { it?.isNotEmpty() == true }
      ?.let {
        // Set the query in searchView which was previously set.
        onlineLibraryScreenState.value.update {
          copy(isSearchActive = true, searchText = it)
        }
        zimManageViewModel.requestFiltering.onNext(it)
      } ?: run {
      // If no previously saved query found then normally initiate the search.
      zimManageViewModel.onlineBooksSearchedQuery.value = ""
      zimManageViewModel.requestFiltering.onNext("")
    }
  }

  private fun navigationIconItem(isSearchActive: Boolean) =
    if (isSearchActive) {
      IconItem.Vector(Icons.AutoMirrored.Default.ArrowBack)
    } else {
      IconItem.Vector(Icons.Filled.Menu)
    }

  private fun navigationIconClick(isSearchActive: Boolean) {
    if (isSearchActive) {
      closeSearch()
      requireActivity().onBackPressedDispatcher.onBackPressed()
    } else {
      // Manually handle the navigation open/close.
      // Since currently we are using the view based navigation drawer in other screens.
      // Once we fully migrate to jetpack compose we will refactor this code to use the
      // compose navigation.
      // TODO Replace with compose based navigation when migration is done.
      val activity = activity as CoreMainActivity
      if (activity.navigationDrawerIsOpen()) {
        activity.closeNavigationDrawer()
      } else {
        activity.openNavigationDrawer()
      }
    }
  }

  private fun actionMenuItems(onSearchClick: () -> Unit) = listOfNotNull(
    when {
      !onlineLibraryScreenState.value.value.isSearchActive -> ActionMenuItem(
        icon = IconItem.Drawable(R.drawable.action_search),
        contentDescription = string.search_label,
        onClick = onSearchClick,
        testingTag = SEARCH_ICON_TESTING_TAG
      )

      else -> null // Handle the case when both conditions are false
    },
    ActionMenuItem(
      IconItem.Drawable(drawable.ic_language_white_24dp),
      string.pref_language_chooser,
      { onLanguageMenuIconClick() },
      isEnabled = true,
      testingTag = LANGUAGE_MENU_ICON_TESTING_TAG
    )
  )

  private fun onLanguageMenuIconClick() {
    requireActivity().navigate(org.kiwix.kiwixmobile.R.id.languageFragment)
    closeKeyboard()
  }

  private fun showInternetAccessViaMobileNetworkDialog() {
    alertDialogShower.show(
      KiwixDialog.YesNoDialog.WifiOnly,
      {
        showRecyclerviewAndHideSwipeDownForLibraryErrorText()
        sharedPreferenceUtil.putPrefWifiOnly(false)
        zimManageViewModel.shouldShowWifiOnlyDialog.value = false
      },
      {
        context.toast(
          resources.getString(string.denied_internet_permission_message),
          Toast.LENGTH_SHORT
        )
        hideRecyclerviewAndShowSwipeDownForLibraryErrorText()
      }
    )
  }

  private fun showRecyclerviewAndHideSwipeDownForLibraryErrorText() {
    onlineLibraryScreenState.value.update {
      copy(noContentViewItem = "" to false)
    }
    showProgressBarOfFetchingOnlineLibrary()
  }

  private fun hideRecyclerviewAndShowSwipeDownForLibraryErrorText() {
    onlineLibraryScreenState.value.update {
      copy(noContentViewItem = getString(string.swipe_down_for_library) to true)
    }
    hideProgressBarOfFetchingOnlineLibrary()
  }

  private fun showProgressBarOfFetchingOnlineLibrary() {
    onRefreshStateChange(isRefreshing = false, shouldShowScanningProgressItem = false)
    onlineLibraryScreenState.value.update {
      copy(
        noContentViewItem = "" to false,
        swipeRefreshItem = onlineLibraryScreenState.value.value.swipeRefreshItem.first to false,
        scanningProgressItem = true to getString(string.reaching_remote_library)
      )
    }
  }

  private fun hideProgressBarOfFetchingOnlineLibrary() {
    onRefreshStateChange(isRefreshing = false, false)
    onlineLibraryScreenState.value.update {
      copy(
        swipeRefreshItem = onlineLibraryScreenState.value.value.swipeRefreshItem.first to true,
        scanningProgressItem = false to getString(string.reaching_remote_library)
      )
    }
  }

  private fun onLibraryStatusChanged(libraryStatus: String) {
    synchronized(lock) {
      onlineLibraryScreenState.value.update {
        copy(
          scanningProgressItem = onlineLibraryScreenState.value.value.scanningProgressItem.first to libraryStatus
        )
      }
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    composeView?.disposeComposition()
    composeView = null
  }

  override fun onBackPressed(activity: AppCompatActivity): FragmentActivityExtensions.Super {
    if (isKeyboardVisible() || onlineLibraryScreenState.value.value.isSearchActive) {
      closeKeyboard()
      closeSearch()
      return FragmentActivityExtensions.Super.ShouldNotCall
    }
    return FragmentActivityExtensions.Super.ShouldCall
  }

  private fun closeSearch() {
    onlineLibraryScreenState.value.update {
      copy(isSearchActive = false, searchText = "")
    }
    onSearchClear()
  }

  private fun onRefreshStateChange(
    isRefreshing: Boolean?,
    shouldShowScanningProgressItem: Boolean
  ) {
    var refreshing = isRefreshing == true
    val onlineLibraryState = onlineLibraryScreenState.value.value
    // do not show the refreshing when the online library is downloading
    if (onlineLibraryState.scanningProgressItem.first ||
      onlineLibraryState.noContentViewItem.second
    ) {
      refreshing = false
    }
    onlineLibraryScreenState.value.update {
      copy(
        swipeRefreshItem = refreshing to onlineLibraryState.swipeRefreshItem.second,
        scanningProgressItem = shouldShowScanningProgressItem to onlineLibraryState.scanningProgressItem.second
      )
    }
  }

  private fun onNetworkStateChange(networkState: NetworkState?) {
    when (networkState) {
      NetworkState.CONNECTED -> {
        if (NetworkUtils.isWiFi(requireContext())) {
          refreshFragment()
        } else if (noWifiWithWifiOnlyPreferenceSet) {
          hideRecyclerviewAndShowSwipeDownForLibraryErrorText()
        } else if (!noWifiWithWifiOnlyPreferenceSet) {
          if (onlineLibraryScreenState.value.value.onlineLibraryList?.isEmpty() == true) {
            showProgressBarOfFetchingOnlineLibrary()
          }
        }
      }

      NetworkState.NOT_CONNECTED -> {
        showNoInternetConnectionError()
      }

      else -> {}
    }
  }

  private fun showNoInternetConnectionError() {
    if (onlineLibraryScreenState.value.value.onlineLibraryList?.isNotEmpty() == true) {
      noInternetSnackbar()
    } else {
      onlineLibraryScreenState.value.update {
        copy(noContentViewItem = getString(string.no_network_connection) to true)
      }
    }
    hideProgressBarOfFetchingOnlineLibrary()
  }

  private fun noInternetSnackbar() {
    onlineLibraryScreenState.value.value.snackBarHostState.snack(
      message = getString(string.no_network_connection),
      actionLabel = getString(string.menu_settings),
      lifecycleScope = lifecycleScope,
      actionClick = { openNetworkSettings() }
    )
  }

  private fun openNetworkSettings() {
    startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
  }

  private fun onLibraryItemsChange(it: List<LibraryListItem>?) {
    if (it != null) {
      onlineLibraryScreenState.value.update {
        copy(
          onlineLibraryList = it,
          noContentViewItem = if (isNotConnected) {
            getString(string.no_network_connection)
          } else {
            getString(string.no_items_msg)
          } to it.isEmpty()
        )
      }
    }
    hideProgressBarOfFetchingOnlineLibrary()
  }

  private fun refreshFragment() {
    if (isNotConnected) {
      showNoInternetConnectionError()
    } else {
      zimManageViewModel.requestDownloadLibrary.onNext(Unit)
      showRecyclerviewAndHideSwipeDownForLibraryErrorText()
    }
  }

  private fun downloadFile() {
    downloadBookItem?.book?.let {
      downloader.download(it)
      downloadBookItem = null
    }
  }

  private fun storeDeviceInPreferences(
    storageDevice: StorageDevice
  ) {
    sharedPreferenceUtil.showStorageOption = false
    sharedPreferenceUtil.putPrefStorage(
      sharedPreferenceUtil.getPublicDirectoryPath(storageDevice.name)
    )
    sharedPreferenceUtil.putStoragePosition(
      if (storageDevice.isInternal) {
        INTERNAL_SELECT_POSITION
      } else {
        EXTERNAL_SELECT_POSITION
      }
    )
    clickOnBookItem()
  }

  private fun requestNotificationPermission() {
    if (!shouldShowRationale(Manifest.permission.POST_NOTIFICATIONS)) {
      requireActivity().requestNotificationPermission()
    } else {
      alertDialogShower.show(
        KiwixDialog.NotificationPermissionDialog,
        requireActivity()::navigateToAppSettings
      )
    }
  }

  private fun checkExternalStorageWritePermission(): Boolean {
    if (!sharedPreferenceUtil.isPlayStoreBuildWithAndroid11OrAbove()) {
      return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        true
      } else {
        hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE).also { permissionGranted ->
          if (!permissionGranted) {
            if (shouldShowRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
              alertDialogShower.show(
                KiwixDialog.WriteStoragePermissionRationale,
                {
                  requestPermission(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    REQUEST_STORAGE_PERMISSION
                  )
                }
              )
            } else {
              alertDialogShower.show(
                KiwixDialog.WriteStoragePermissionRationale,
                requireActivity()::navigateToAppSettings
              )
            }
          }
        }
      }
    }
    return true
  }

  private fun requestPermission(permission: String, requestCode: Int) {
    ActivityCompat.requestPermissions(
      requireActivity(),
      arrayOf(permission),
      requestCode
    )
  }

  private fun shouldShowRationale(permission: String) =
    ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), permission)

  @Suppress("DEPRECATION")
  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode == REQUEST_STORAGE_PERMISSION &&
      permissions.isNotEmpty() &&
      permissions[0] == Manifest.permission.WRITE_EXTERNAL_STORAGE
    ) {
      if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
        if (!sharedPreferenceUtil.isPlayStoreBuildWithAndroid11OrAbove()) {
          checkExternalStorageWritePermission()
        }
      }
    } else if (requestCode == REQUEST_POST_NOTIFICATION_PERMISSION &&
      permissions.isNotEmpty() &&
      permissions[0] == Manifest.permission.POST_NOTIFICATIONS
    ) {
      if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        downloadBookItem?.let(::onBookItemClick)
      }
    }
  }

  private fun hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(
      requireActivity(),
      permission
    ) == PackageManager.PERMISSION_GRANTED

  @Suppress("NestedBlockDepth")
  private fun onBookItemClick(item: LibraryListItem.BookItem) {
    lifecycleScope.launch {
      if (checkExternalStorageWritePermission()) {
        downloadBookItem = item
        if (requireActivity().hasNotificationPermission(sharedPreferenceUtil)) {
          when {
            isNotConnected -> {
              noInternetSnackbar()
              return@launch
            }

            noWifiWithWifiOnlyPreferenceSet -> {
              alertDialogShower.show(KiwixDialog.YesNoDialog.WifiOnly, {
                sharedPreferenceUtil.putPrefWifiOnly(false)
                clickOnBookItem()
              })
              return@launch
            }

            else ->
              if (sharedPreferenceUtil.showStorageOption) {
                // Show the storage selection dialog for configuration if there is an SD card available.
                if (getStorageDeviceList().size > 1) {
                  showStorageSelectDialog(getStorageDeviceList())
                } else {
                  // If only internal storage is available, proceed with the ZIM file download directly.
                  // Displaying a configuration dialog is unnecessary in this case.
                  sharedPreferenceUtil.showStorageOption = false
                  onBookItemClick(item)
                }
              } else if (!requireActivity().isManageExternalStoragePermissionGranted(
                  sharedPreferenceUtil
                )
              ) {
                showManageExternalStoragePermissionDialog()
              } else {
                availableSpaceCalculator.hasAvailableSpaceFor(
                  item,
                  { downloadFile() },
                  {
                    onlineLibraryScreenState.value.value.snackBarHostState.snack(
                      message = """
                      ${getString(string.download_no_space)}
                      ${getString(string.space_available)} $it
                      """.trimIndent(),
                      actionLabel = getString(string.change_storage),
                      actionClick = {
                        lifecycleScope.launch {
                          showStorageSelectDialog(getStorageDeviceList())
                        }
                      },
                      lifecycleScope = lifecycleScope
                    )
                  }
                )
              }
          }
        } else {
          requestNotificationPermission()
        }
      }
    }
  }

  private fun showStorageSelectDialog(storageDeviceList: List<StorageDevice>) =
    StorageSelectDialog()
      .apply {
        onSelectAction = ::storeDeviceInPreferences
        titleSize = STORAGE_SELECT_STORAGE_TITLE_TEXTVIEW_SIZE
        setStorageDeviceList(storageDeviceList)
        setShouldShowCheckboxSelected(false)
      }
      .show(parentFragmentManager, getString(string.choose_storage_to_download_book))

  private fun clickOnBookItem() {
    if (!requireActivity().isManageExternalStoragePermissionGranted(sharedPreferenceUtil)) {
      showManageExternalStoragePermissionDialog()
    } else {
      downloadBookItem?.let(::onBookItemClick)
    }
  }

  private fun showManageExternalStoragePermissionDialog() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      alertDialogShower.show(
        KiwixDialog.ManageExternalFilesPermissionDialog,
        {
          this.activity?.let(FragmentActivity::navigateToSettings)
        }
      )
    }
  }

  private suspend fun getStorageDeviceList() =
    (activity as KiwixMainActivity).getStorageDeviceList()
}
