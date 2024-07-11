/*
 * Kiwix Android
 * Copyright (c) 2020 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.nav.destination.library

import android.Manifest
import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tonyodev.fetch2.Status
import eu.mhutti1.utils.storage.StorageDevice
import eu.mhutti1.utils.storage.StorageSelectDialog
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.cachedComponent
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.base.BaseFragment
import org.kiwix.kiwixmobile.core.base.FragmentActivityExtensions
import org.kiwix.kiwixmobile.core.downloader.Downloader
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.hasNotificationPermission
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.isManageExternalStoragePermissionGranted
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.navigate
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.requestNotificationPermission
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.viewModel
import org.kiwix.kiwixmobile.core.extensions.closeKeyboard
import org.kiwix.kiwixmobile.core.extensions.coreMainActivity
import org.kiwix.kiwixmobile.core.extensions.setBottomMarginToFragmentContainerView
import org.kiwix.kiwixmobile.core.extensions.setUpSearchView
import org.kiwix.kiwixmobile.core.extensions.snack
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.navigateToAppSettings
import org.kiwix.kiwixmobile.core.navigateToSettings
import org.kiwix.kiwixmobile.core.utils.BookUtils
import org.kiwix.kiwixmobile.core.utils.EXTERNAL_SELECT_POSITION
import org.kiwix.kiwixmobile.core.utils.INTERNAL_SELECT_POSITION
import org.kiwix.kiwixmobile.core.utils.NetworkUtils
import org.kiwix.kiwixmobile.core.utils.REQUEST_POST_NOTIFICATION_PERMISSION
import org.kiwix.kiwixmobile.core.utils.REQUEST_STORAGE_PERMISSION
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.SimpleRecyclerViewScrollListener
import org.kiwix.kiwixmobile.core.utils.SimpleTextListener
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.DialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog.SelectFolder
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog.YesNoDialog.WifiOnly
import org.kiwix.kiwixmobile.core.utils.files.FileUtils.getPathFromUri
import org.kiwix.kiwixmobile.databinding.FragmentDestinationDownloadBinding
import org.kiwix.kiwixmobile.zimManager.Fat32Checker
import org.kiwix.kiwixmobile.zimManager.NetworkState
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel
import org.kiwix.kiwixmobile.zimManager.libraryView.AvailableSpaceCalculator
import org.kiwix.kiwixmobile.zimManager.libraryView.adapter.LibraryAdapter
import org.kiwix.kiwixmobile.zimManager.libraryView.adapter.LibraryDelegate
import org.kiwix.kiwixmobile.zimManager.libraryView.adapter.LibraryListItem
import javax.inject.Inject

class OnlineLibraryFragment : BaseFragment(), FragmentActivityExtensions {

  @Inject lateinit var conMan: ConnectivityManager
  @Inject lateinit var downloader: Downloader
  @Inject lateinit var dialogShower: DialogShower
  @Inject lateinit var sharedPreferenceUtil: SharedPreferenceUtil
  @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
  @Inject lateinit var bookUtils: BookUtils
  @Inject lateinit var availableSpaceCalculator: AvailableSpaceCalculator
  @Inject lateinit var alertDialogShower: AlertDialogShower
  private var fragmentDestinationDownloadBinding: FragmentDestinationDownloadBinding? = null

  private var downloadBookItem: LibraryListItem.BookItem? = null
  private val zimManageViewModel by lazy {
    requireActivity().viewModel<ZimManageViewModel>(viewModelFactory)
  }

  private val libraryAdapter: LibraryAdapter by lazy {
    LibraryAdapter(
      LibraryDelegate.BookDelegate(bookUtils, ::onBookItemClick, availableSpaceCalculator),
      LibraryDelegate.DownloadDelegate(
        {
          if (it.currentDownloadState == Status.FAILED) {
            if (isNotConnected) {
              noInternetSnackbar()
            } else {
              downloader.retryDownload(it.downloadId)
            }
          } else {
            dialogShower.show(
              KiwixDialog.YesNoDialog.StopDownload,
              { downloader.cancelDownload(it.downloadId) }
            )
          }
        },
        {
          context?.let { context ->
            downloader.pauseResumeDownload(
              it.downloadId,
              it.downloadState.toReadableState(context) == getString(R.string.paused_state)
            )
          }
        }
      ),
      LibraryDelegate.DividerDelegate
    )
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
  ): View? {
    fragmentDestinationDownloadBinding =
      FragmentDestinationDownloadBinding.inflate(inflater, container, false)
    val toolbar = fragmentDestinationDownloadBinding?.root?.findViewById<Toolbar>(R.id.toolbar)
    val activity = activity as CoreMainActivity
    activity.setSupportActionBar(toolbar)
    activity.supportActionBar?.apply {
      setDisplayHomeAsUpEnabled(true)
      setTitle(R.string.download)
    }
    if (toolbar != null) {
      activity.setupDrawerToggle(toolbar)
    }
    return fragmentDestinationDownloadBinding?.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    fragmentDestinationDownloadBinding?.librarySwipeRefresh?.setOnRefreshListener(::refreshFragment)
    fragmentDestinationDownloadBinding?.libraryList?.run {
      adapter = libraryAdapter
      layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
      setHasFixedSize(true)
    }
    zimManageViewModel.libraryItems.observe(viewLifecycleOwner, Observer(::onLibraryItemsChange))
      .also {
        coreMainActivity.navHostContainer
          .setBottomMarginToFragmentContainerView(0)
      }
    zimManageViewModel.libraryListIsRefreshing.observe(
      viewLifecycleOwner, Observer(::onRefreshStateChange)
    )
    zimManageViewModel.networkStates.observe(viewLifecycleOwner, Observer(::onNetworkStateChange))
    zimManageViewModel.shouldShowWifiOnlyDialog.observe(
      viewLifecycleOwner
    ) {
      if (it && !NetworkUtils.isWiFi(requireContext())) {
        showInternetAccessViaMobileNetworkDialog()
      }
    }
    setupMenu()

    // hides keyboard when scrolled
    fragmentDestinationDownloadBinding?.libraryList?.addOnScrollListener(
      SimpleRecyclerViewScrollListener { _, newState ->
        if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
          fragmentDestinationDownloadBinding?.libraryList?.closeKeyboard()
        }
      }
    )
    // <book  />
    val libraryBookEntity = LibraryNetworkEntity.Book().apply {
      id = "6a5413cd-0d96-7288-9c1b-5beda035e472"
      size = "170628"
      url = "https://download.kiwix.org/zim/zimit/100r.co_en_all_2024-06.zim.meta4"
      mediaCount = "1433"
      articleCount = "320"
      favicon =
        "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"
      title = "100 Rabbits"
      description = "Research and test low-tech solutions, and document findings"
      language = "eng"
      creator = "-"
      publisher = "openZIM"
      bookName = "100r.co_en_all"
      tags = "_ftindex:yes;preppers;_category:other;_pictures:yes;_videos:yes;_details:yes"
      date = "2024-06-24"
      faviconMimeType = "image/png"
    }
    downloader.download(libraryBookEntity)
  }

  private fun setupMenu() {
    (requireActivity() as MenuHost).addMenuProvider(
      object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
          menuInflater.inflate(R.menu.menu_zim_manager, menu)
          val searchItem = menu.findItem(R.id.action_search)
          val getZimItem = menu.findItem(R.id.get_zim_nearby_device)
          getZimItem?.isVisible = false

          (searchItem?.actionView as? SearchView)?.apply {
            setUpSearchView(requireActivity())
            setOnQueryTextListener(
              SimpleTextListener { query, _ ->
                zimManageViewModel.requestFiltering.onNext(query)
              }
            )
          }
          zimManageViewModel.requestFiltering.onNext("")
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
          when (menuItem.itemId) {
            R.id.select_language -> {
              requireActivity().navigate(R.id.languageFragment)
              closeKeyboard()
              return true
            }
          }
          return false
        }
      },
      viewLifecycleOwner,
      Lifecycle.State.RESUMED
    )
  }

  private fun showInternetAccessViaMobileNetworkDialog() {
    dialogShower.show(
      WifiOnly,
      {
        onRefreshStateChange(true)
        showRecyclerviewAndHideSwipeDownForLibraryErrorText()
        sharedPreferenceUtil.putPrefWifiOnly(false)
        zimManageViewModel.shouldShowWifiOnlyDialog.value = false
      },
      {
        onRefreshStateChange(false)
        context.toast(
          resources.getString(R.string.denied_internet_permission_message),
          Toast.LENGTH_SHORT
        )
        hideRecyclerviewAndShowSwipeDownForLibraryErrorText()
      }
    )
  }

  private fun showRecyclerviewAndHideSwipeDownForLibraryErrorText() {
    fragmentDestinationDownloadBinding?.apply {
      libraryErrorText.visibility = View.GONE
      libraryList.visibility = View.VISIBLE
    }
  }

  private fun hideRecyclerviewAndShowSwipeDownForLibraryErrorText() {
    fragmentDestinationDownloadBinding?.apply {
      libraryErrorText.setText(
        R.string.swipe_down_for_library
      )
      libraryErrorText.visibility = View.VISIBLE
      libraryList.visibility = View.GONE
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    availableSpaceCalculator.dispose()
    fragmentDestinationDownloadBinding?.libraryList?.adapter = null
    fragmentDestinationDownloadBinding = null
  }

  override fun onBackPressed(activity: AppCompatActivity): FragmentActivityExtensions.Super {
    getActivity()?.finish()
    return FragmentActivityExtensions.Super.ShouldNotCall
  }

  private fun onRefreshStateChange(isRefreshing: Boolean?) {
    fragmentDestinationDownloadBinding?.librarySwipeRefresh?.isRefreshing = isRefreshing == true
  }

  private fun onNetworkStateChange(networkState: NetworkState?) {
    when (networkState) {
      NetworkState.CONNECTED -> {
        if (NetworkUtils.isWiFi(requireContext())) {
          onRefreshStateChange(true)
          refreshFragment()
        } else if (noWifiWithWifiOnlyPreferenceSet) {
          onRefreshStateChange(false)
          hideRecyclerviewAndShowSwipeDownForLibraryErrorText()
        }
      }

      NetworkState.NOT_CONNECTED -> {
        showNoInternetConnectionError()
      }

      else -> {}
    }
  }

  private fun showNoInternetConnectionError() {
    if (libraryAdapter.itemCount > 0) {
      noInternetSnackbar()
    } else {
      fragmentDestinationDownloadBinding?.libraryErrorText?.setText(
        R.string.no_network_connection
      )
      fragmentDestinationDownloadBinding?.libraryErrorText?.visibility = View.VISIBLE
    }
    fragmentDestinationDownloadBinding?.librarySwipeRefresh?.isRefreshing = false
  }

  private fun noInternetSnackbar() {
    fragmentDestinationDownloadBinding?.libraryList?.snack(
      R.string.no_network_connection,
      requireActivity().findViewById(R.id.bottom_nav_view),
      R.string.menu_settings,
      ::openNetworkSettings
    )
  }

  private fun openNetworkSettings() {
    startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
  }

  private fun onLibraryItemsChange(it: List<LibraryListItem>?) {
    if (it != null) {
      libraryAdapter.items = it
    }
    if (it?.isEmpty() == true) {
      fragmentDestinationDownloadBinding?.libraryErrorText?.setText(
        if (isNotConnected) R.string.no_network_connection
        else R.string.no_items_msg
      )
      fragmentDestinationDownloadBinding?.libraryErrorText?.visibility = View.VISIBLE
    } else {
      fragmentDestinationDownloadBinding?.libraryErrorText?.visibility = View.GONE
    }
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

  @SuppressLint("InflateParams")
  private fun storeDeviceInPreferences(
    storageDevice: StorageDevice
  ) {
    if (storageDevice.isInternal) {
      sharedPreferenceUtil.putPrefStorage(
        sharedPreferenceUtil.getPublicDirectoryPath(storageDevice.name)
      )
      sharedPreferenceUtil.putStoragePosition(INTERNAL_SELECT_POSITION)
      clickOnBookItem()
    } else {
      if (sharedPreferenceUtil.isPlayStoreBuild) {
        setExternalStoragePath(storageDevice)
      } else {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
          Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        ) {
          val view = LayoutInflater.from(activity).inflate(R.layout.select_folder_dialog, null)
          dialogShower.show(SelectFolder { view }, ::selectFolder)
        } else {
          setExternalStoragePath(storageDevice)
        }
      }
    }
  }

  private fun setExternalStoragePath(storageDevice: StorageDevice) {
    sharedPreferenceUtil.putPrefStorage(storageDevice.name)
    sharedPreferenceUtil.putStoragePosition(EXTERNAL_SELECT_POSITION)
    clickOnBookItem()
  }

  private fun selectFolder() {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
    intent.addFlags(
      Intent.FLAG_GRANT_READ_URI_PERMISSION
        or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
    )
    selectFolderLauncher.launch(intent)
  }

  private val selectFolderLauncher =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      if (result.resultCode == RESULT_OK) {
        result.data?.let { intent ->
          getPathFromUri(requireActivity(), intent)?.let(sharedPreferenceUtil::putPrefStorage)
          sharedPreferenceUtil.putStoragePosition(EXTERNAL_SELECT_POSITION)
          clickOnBookItem()
        } ?: run {
          activity.toast(
            resources
              .getString(R.string.system_unable_to_grant_permission_message),
            Toast.LENGTH_SHORT
          )
        }
      }
    }

  private fun requestNotificationPermission() {
    if (!shouldShowRationale(POST_NOTIFICATIONS)) {
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
        hasPermission(WRITE_EXTERNAL_STORAGE).also { permissionGranted ->
          if (!permissionGranted) {
            if (shouldShowRationale(WRITE_EXTERNAL_STORAGE)) {
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
      requireActivity(), arrayOf(permission),
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
      if (grantResults[0] != PERMISSION_GRANTED) {
        if (!sharedPreferenceUtil.isPlayStoreBuildWithAndroid11OrAbove())
          checkExternalStorageWritePermission()
      }
    } else if (requestCode == REQUEST_POST_NOTIFICATION_PERMISSION &&
      permissions.isNotEmpty() &&
      permissions[0] == POST_NOTIFICATIONS
    ) {
      if (grantResults[0] == PERMISSION_GRANTED) {
        downloadBookItem?.let(::onBookItemClick)
      }
    }
  }

  private fun hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(requireActivity(), permission) == PERMISSION_GRANTED

  @Suppress("NestedBlockDepth")
  private fun onBookItemClick(item: LibraryListItem.BookItem) {
    if (checkExternalStorageWritePermission()) {
      downloadBookItem = item
      if (requireActivity().hasNotificationPermission(sharedPreferenceUtil)) {
        when {
          isNotConnected -> {
            noInternetSnackbar()
            return
          }

          noWifiWithWifiOnlyPreferenceSet -> {
            dialogShower.show(WifiOnly, {
              sharedPreferenceUtil.putPrefWifiOnly(false)
              clickOnBookItem()
            })
            return
          }

          else -> if (sharedPreferenceUtil.showStorageOption) {
            showStorageConfigureDialog()
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
                fragmentDestinationDownloadBinding?.libraryList?.snack(
                  """ 
                ${getString(R.string.download_no_space)}
                ${getString(R.string.space_available)} $it
                  """.trimIndent(),
                  requireActivity().findViewById(R.id.bottom_nav_view),
                  R.string.download_change_storage,
                  ::showStorageSelectDialog
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

  private fun showStorageSelectDialog() = StorageSelectDialog()
    .apply {
      onSelectAction = ::storeDeviceInPreferences
    }
    .show(parentFragmentManager, getString(R.string.pref_storage))

  private fun showStorageConfigureDialog() {
    alertDialogShower.show(
      KiwixDialog.StorageConfigure,
      {
        showStorageSelectDialog()
        sharedPreferenceUtil.showStorageOption = false
      },
      {
        sharedPreferenceUtil.showStorageOption = false
        clickOnBookItem()
      }
    )
  }

  private fun clickOnBookItem() {
    if (!requireActivity().isManageExternalStoragePermissionGranted(sharedPreferenceUtil)) {
      showManageExternalStoragePermissionDialog()
    } else {
      downloadBookItem?.let(::onBookItemClick)
    }
  }

  private fun showManageExternalStoragePermissionDialog() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      dialogShower.show(
        KiwixDialog.ManageExternalFilesPermissionDialog,
        {
          this.activity?.let(FragmentActivity::navigateToSettings)
        }
      )
    }
  }
}
