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
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.ConnectivityManager
import android.net.Uri
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
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import eu.mhutti1.utils.storage.StorageDevice
import eu.mhutti1.utils.storage.StorageSelectDialog
import kotlinx.android.synthetic.main.fragment_destination_download.allowInternetPermissionButton
import kotlinx.android.synthetic.main.fragment_destination_download.libraryErrorText
import kotlinx.android.synthetic.main.fragment_destination_download.libraryList
import kotlinx.android.synthetic.main.fragment_destination_download.librarySwipeRefresh
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.cachedComponent
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.base.BaseFragment
import org.kiwix.kiwixmobile.core.base.FragmentActivityExtensions
import org.kiwix.kiwixmobile.core.downloader.Downloader
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.navigate
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.viewModel
import org.kiwix.kiwixmobile.core.extensions.closeKeyboard
import org.kiwix.kiwixmobile.core.extensions.snack
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.utils.BookUtils
import org.kiwix.kiwixmobile.core.utils.EXTERNAL_SELECT_POSITION
import org.kiwix.kiwixmobile.core.utils.INTERNAL_SELECT_POSITION
import org.kiwix.kiwixmobile.core.utils.NetworkUtils
import org.kiwix.kiwixmobile.core.utils.REQUEST_SELECT_FOLDER_PERMISSION
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
import org.kiwix.kiwixmobile.zim_manager.NetworkState
import org.kiwix.kiwixmobile.zim_manager.ZimManageViewModel
import org.kiwix.kiwixmobile.zim_manager.library_view.AvailableSpaceCalculator
import org.kiwix.kiwixmobile.zim_manager.library_view.adapter.LibraryAdapter
import org.kiwix.kiwixmobile.zim_manager.library_view.adapter.LibraryDelegate
import org.kiwix.kiwixmobile.zim_manager.library_view.adapter.LibraryListItem
import javax.inject.Inject

class OnlineLibraryFragment : BaseFragment(), FragmentActivityExtensions {

  @Inject lateinit var conMan: ConnectivityManager
  @Inject lateinit var downloader: Downloader
  @Inject lateinit var dialogShower: DialogShower
  @Inject lateinit var alertDialogShower: AlertDialogShower
  @Inject lateinit var sharedPreferenceUtil: SharedPreferenceUtil
  @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
  @Inject lateinit var bookUtils: BookUtils
  @Inject lateinit var availableSpaceCalculator: AvailableSpaceCalculator
  private val zimManageViewModel by lazy {
    requireActivity().viewModel<ZimManageViewModel>(viewModelFactory)
  }

  private val libraryAdapter: LibraryAdapter by lazy {
    LibraryAdapter(
      LibraryDelegate.BookDelegate(bookUtils, ::onBookItemClick),
      LibraryDelegate.DownloadDelegate {
        if (it.downloadState.toReadableState(requireActivity()).contains("Failed")) {
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
      LibraryDelegate.DividerDelegate
    )
  }

  private val noWifiWithWifiOnlyPreferenceSet
    get() = sharedPreferenceUtil.prefWifiOnly && !NetworkUtils.isWiFi(requireContext())

  private val isNotConnected get() = conMan.activeNetworkInfo?.isConnected == false

  override fun inject(baseActivity: BaseActivity) {
    baseActivity.cachedComponent.inject(this)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    setHasOptionsMenu(true)
    val root = inflater.inflate(R.layout.fragment_destination_download, container, false)
    val toolbar = root.findViewById<Toolbar>(R.id.toolbar)
    val activity = activity as CoreMainActivity
    activity.setSupportActionBar(toolbar)
    activity.supportActionBar?.apply {
      setDisplayHomeAsUpEnabled(true)
      setTitle(R.string.download)
    }
    activity.setupDrawerToggle(toolbar)
    return root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    librarySwipeRefresh.setOnRefreshListener(::refreshFragment)
    libraryList.run {
      adapter = libraryAdapter
      layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
      setHasFixedSize(true)
    }
    zimManageViewModel.libraryItems.observe(viewLifecycleOwner, Observer(::onLibraryItemsChange))
    zimManageViewModel.libraryListIsRefreshing.observe(
      viewLifecycleOwner, Observer(::onRefreshStateChange)
    )
    zimManageViewModel.networkStates.observe(viewLifecycleOwner, Observer(::onNetworkStateChange))
    zimManageViewModel.shouldShowWifiOnlyDialog.observe(
      viewLifecycleOwner
    ) {
      if (it) {
        showInternetPermissionDialog()
      }
    }

    // hides keyboard when scrolled
    libraryList.addOnScrollListener(
      SimpleRecyclerViewScrollListener { _, newState ->
        if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
          libraryList.closeKeyboard()
        }
      }
    )

    allowInternetPermissionButton.setOnClickListener {
      showInternetPermissionDialog()
    }
  }

  private fun showInternetPermissionDialog() {
    dialogShower.show(
      WifiOnly,
      {
        onRefreshStateChange(true)
        libraryErrorText.visibility = View.GONE
        allowInternetPermissionButton.visibility = View.GONE
        sharedPreferenceUtil.putPrefWifiOnly(false)
        zimManageViewModel.shouldShowWifiOnlyDialog.value = false
      },
      {
        onRefreshStateChange(false)
        context.toast(
          resources.getString(R.string.denied_internet_permission_message),
          Toast.LENGTH_SHORT
        )
        libraryErrorText.setText(R.string.allow_internet_permission_message)
        libraryErrorText.visibility = View.VISIBLE
        allowInternetPermissionButton.visibility = View.VISIBLE
      }
    )
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    super<BaseFragment>.onCreateOptionsMenu(menu, inflater)
    inflater.inflate(R.menu.menu_zim_manager, menu)
    val searchItem = menu.findItem(R.id.action_search)
    val getZimItem = menu.findItem(R.id.get_zim_nearby_device)
    getZimItem?.isVisible = false

    (searchItem?.actionView as? SearchView)?.setOnQueryTextListener(
      SimpleTextListener(zimManageViewModel.requestFiltering::onNext)
    )
    zimManageViewModel.requestFiltering.onNext("")
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.select_language -> {
        requireActivity().navigate(R.id.languageFragment)
        closeKeyboard()
      }
    }
    return super.onOptionsItemSelected(item)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    libraryList.adapter = null
  }

  override fun onBackPressed(activity: AppCompatActivity): FragmentActivityExtensions.Super {
    getActivity()?.finish()
    return FragmentActivityExtensions.Super.ShouldNotCall
  }

  private fun onRefreshStateChange(isRefreshing: Boolean?) {
    librarySwipeRefresh.isRefreshing = isRefreshing!!
  }

  private fun onNetworkStateChange(networkState: NetworkState?) {
    when (networkState) {
      NetworkState.CONNECTED -> {
      }
      NetworkState.NOT_CONNECTED -> {
        if (libraryAdapter.itemCount > 0) {
          noInternetSnackbar()
        } else {
          libraryErrorText.setText(R.string.no_network_connection)
          libraryErrorText.visibility = View.VISIBLE
        }
        allowInternetPermissionButton.visibility = View.GONE
        librarySwipeRefresh.isRefreshing = false
      }
    }
  }

  private fun noInternetSnackbar() {
    view?.snack(
      R.string.no_network_connection,
      R.string.menu_settings,
      ::openNetworkSettings
    )
  }

  private fun openNetworkSettings() {
    startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
  }

  private fun onLibraryItemsChange(it: List<LibraryListItem>?) {
    libraryAdapter.items = it!!
    if (it.isEmpty()) {
      libraryErrorText.setText(
        if (isNotConnected) R.string.no_network_connection
        else R.string.no_items_msg
      )
      libraryErrorText.visibility = View.VISIBLE
    } else {
      libraryErrorText.visibility = View.GONE
    }
    allowInternetPermissionButton.visibility = View.GONE
  }

  private fun refreshFragment() {
    if (isNotConnected) {
      noInternetSnackbar()
    } else {
      zimManageViewModel.requestDownloadLibrary.onNext(Unit)
    }
  }

  private fun downloadFile(book: LibraryNetworkEntity.Book) {
    downloader.download(book)
  }

  @SuppressLint("InflateParams")
  private fun storeDeviceInPreferences(storageDevice: StorageDevice) {
    if (storageDevice.isInternal) {
      sharedPreferenceUtil.putPrefStorage(
        sharedPreferenceUtil.getPublicDirectoryPath(storageDevice.name)
      )
      sharedPreferenceUtil.putStoragePosition(INTERNAL_SELECT_POSITION)
    } else {
      if (sharedPreferenceUtil.isPlayStoreBuild) {
        setExternalStoragePath(storageDevice)
      } else {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
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
  }

  private fun selectFolder() {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
    intent.addFlags(
      Intent.FLAG_GRANT_READ_URI_PERMISSION
        or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
    )
    startActivityForResult(intent, REQUEST_SELECT_FOLDER_PERMISSION)
  }

  @SuppressLint("WrongConstant") override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  ) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == REQUEST_SELECT_FOLDER_PERMISSION && resultCode == Activity.RESULT_OK) {
      data?.let {
        getPathFromUri(requireActivity(), data)?.let(sharedPreferenceUtil::putPrefStorage)
        sharedPreferenceUtil.putStoragePosition(EXTERNAL_SELECT_POSITION)
      } ?: run {
        activity.toast(
          resources
            .getString(R.string.system_unable_to_grant_permission_message),
          Toast.LENGTH_SHORT
        )
      }
    }
  }

  private fun checkExternalStorageWritePermission(): Boolean {
    if (!sharedPreferenceUtil.isPlayStoreBuildWithAndroid11OrAbove()) {
      return hasPermission(WRITE_EXTERNAL_STORAGE).also { permissionGranted ->
        if (!permissionGranted) {
          if (shouldShowRationale(WRITE_EXTERNAL_STORAGE)) {
            alertDialogShower.show(
              KiwixDialog.WriteStoragePermissionRationale,
              ::requestExternalStoragePermission
            )
          } else {
            alertDialogShower.show(
              KiwixDialog.WriteStoragePermissionRationale,
              ::openAppSettings
            )
          }
        }
      }
    }
    return true
  }

  private fun openAppSettings() {
    val uri: Uri = Uri.fromParts("package", requireActivity().packageName, null)
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK
      data = uri
    }
    startActivity(intent)
  }

  private fun requestExternalStoragePermission() {
    ActivityCompat.requestPermissions(
      requireActivity(), arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
      REQUEST_STORAGE_PERMISSION
    )
  }

  private fun shouldShowRationale(writeExternalStorage: String) =
    ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), writeExternalStorage)

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
    }
  }

  private fun hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(requireActivity(), permission) == PERMISSION_GRANTED

  private fun onBookItemClick(item: LibraryListItem.BookItem) {
    if (checkExternalStorageWritePermission()) {
      when {
        isNotConnected -> {
          noInternetSnackbar()
          return
        }
        noWifiWithWifiOnlyPreferenceSet -> {
          dialogShower.show(WifiOnly, {
            sharedPreferenceUtil.putPrefWifiOnly(false)
            downloadFile(item.book)
          })
          return
        }
        else -> availableSpaceCalculator.hasAvailableSpaceFor(
          item,
          { downloadFile(item.book) },
          {
            libraryList.snack(
              getString(R.string.download_no_space) +
                "\n" + getString(R.string.space_available) + " " +
                it,
              R.string.download_change_storage,
              ::showStorageSelectDialog
            )
          }
        )
      }
    }
  }

  private fun showStorageSelectDialog() = StorageSelectDialog()
    .apply {
      onSelectAction = ::storeDeviceInPreferences
    }
    .show(requireFragmentManager(), getString(R.string.pref_storage))
}
