/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
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
 */
package org.kiwix.kiwixmobile.zim_manager.library_view

import android.annotation.SuppressLint
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import eu.mhutti1.utils.storage.StorageDevice
import eu.mhutti1.utils.storage.StorageSelectDialog
import kotlinx.android.synthetic.main.activity_library.libraryErrorText
import kotlinx.android.synthetic.main.activity_library.libraryList
import kotlinx.android.synthetic.main.activity_library.librarySwipeRefresh
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.R.string
import org.kiwix.kiwixmobile.base.BaseFragment
import org.kiwix.kiwixmobile.di.components.ActivityComponent
import org.kiwix.kiwixmobile.downloader.Downloader
import org.kiwix.kiwixmobile.extensions.snack
import org.kiwix.kiwixmobile.extensions.toast
import org.kiwix.kiwixmobile.extensions.viewModel
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book
import org.kiwix.kiwixmobile.main.MainActivity
import org.kiwix.kiwixmobile.utils.BookUtils
import org.kiwix.kiwixmobile.utils.DialogShower
import org.kiwix.kiwixmobile.utils.KiwixDialog.YesNoDialog.WifiOnly
import org.kiwix.kiwixmobile.utils.NetworkUtils
import org.kiwix.kiwixmobile.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.utils.StyleUtils
import org.kiwix.kiwixmobile.utils.TestingUtils
import org.kiwix.kiwixmobile.zim_manager.NetworkState
import org.kiwix.kiwixmobile.zim_manager.NetworkState.CONNECTED
import org.kiwix.kiwixmobile.zim_manager.NetworkState.NOT_CONNECTED
import org.kiwix.kiwixmobile.zim_manager.ZimManageViewModel
import org.kiwix.kiwixmobile.zim_manager.library_view.adapter.LibraryAdapter
import org.kiwix.kiwixmobile.zim_manager.library_view.adapter.LibraryDelegate.BookDelegate
import org.kiwix.kiwixmobile.zim_manager.library_view.adapter.LibraryDelegate.DividerDelegate
import org.kiwix.kiwixmobile.zim_manager.library_view.adapter.LibraryListItem
import org.kiwix.kiwixmobile.zim_manager.library_view.adapter.LibraryListItem.BookItem
import java.io.File
import javax.inject.Inject

class LibraryFragment : BaseFragment() {

  @Inject lateinit var conMan: ConnectivityManager
  @Inject lateinit var downloader: Downloader
  @Inject lateinit var sharedPreferenceUtil: SharedPreferenceUtil
  @Inject lateinit var dialogShower: DialogShower
  @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
  @Inject lateinit var bookUtils: BookUtils

  private val zimManageViewModel by lazy {
    activity!!.viewModel<ZimManageViewModel>(viewModelFactory)
  }

  private val libraryAdapter: LibraryAdapter by lazy {
    LibraryAdapter(
      BookDelegate(bookUtils, ::onBookItemClick), DividerDelegate
    )
  }

  private val spaceAvailable: Long
    get() = File(sharedPreferenceUtil.prefStorage).freeSpace

  private val noWifiWithWifiOnlyPreferenceSet
    get() = sharedPreferenceUtil.prefWifiOnly && !NetworkUtils.isWiFi(context!!)

  private val isNotConnected get() = conMan.activeNetworkInfo?.isConnected == false

  override fun inject(activityComponent: ActivityComponent) {
    activityComponent.inject(this)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    TestingUtils.bindResource(LibraryFragment::class.java)
    return inflater.inflate(R.layout.activity_library, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    librarySwipeRefresh.setOnRefreshListener(::refreshFragment)
    libraryList.run {
      adapter = libraryAdapter
      layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
      setHasFixedSize(true)
    }
    zimManageViewModel.libraryItems.observe(this, Observer(::onLibraryItemsChange))
    zimManageViewModel.libraryListIsRefreshing.observe(
      this, Observer(::onRefreshStateChange)
    )
    zimManageViewModel.networkStates.observe(this, Observer(::onNetworkStateChange))
  }

  private fun onRefreshStateChange(isRefreshing: Boolean?) {
    librarySwipeRefresh.isRefreshing = isRefreshing!!
  }

  private fun onNetworkStateChange(networkState: NetworkState?) {
    when (networkState) {
      CONNECTED -> {
      }
      NOT_CONNECTED -> {
        if (libraryAdapter.itemCount > 0) {
          context.toast(string.no_network_connection)
        } else {
          libraryErrorText.setText(string.no_network_connection)
          libraryErrorText.visibility = VISIBLE
        }
      }
    }
  }

  private fun onLibraryItemsChange(it: List<LibraryListItem>?) {
    libraryAdapter.items = it!!
    if (it.isEmpty()) {
      libraryErrorText.setText(
        if (isNotConnected) string.no_network_connection
        else string.no_items_msg
      )
      libraryErrorText.visibility = VISIBLE
      TestingUtils.unbindResource(LibraryFragment::class.java)
    } else {
      libraryErrorText.visibility = GONE
    }
  }

  private fun refreshFragment() {
    if (isNotConnected) {
      context.toast(string.no_network_connection)
    } else {
      zimManageViewModel.requestDownloadLibrary.onNext(Unit)
    }
  }

  private fun downloadFile(book: Book) {
    downloader.download(book)
  }

  private fun storeDeviceInPreferences(storageDevice: StorageDevice) {
    sharedPreferenceUtil.putPrefStorage(storageDevice.name)
    sharedPreferenceUtil.putPrefStorageTitle(
      getString(
        if (storageDevice.isInternal) string.internal_storage
        else string.external_storage
      )
    )
  }

  private fun onBookItemClick(item: BookItem) {
    when {
      notEnoughSpaceAvailable(item) -> {
        context.toast(
          getString(string.download_no_space) +
            "\n" + getString(string.space_available) + " " +
            LibraryUtils.bytesToHuman(spaceAvailable)
        )
        libraryList.snack(
          string.download_change_storage,
          string.open,
          ::showStorageSelectDialog
        )
        return
      }
      isNotConnected -> {
        context.toast(string.no_network_connection)
        return
      }
      noWifiWithWifiOnlyPreferenceSet -> {
        dialogShower.show(WifiOnly, {
          sharedPreferenceUtil.putPrefWifiOnly(false)
          MainActivity.wifiOnly = false
          downloadFile(item.book)
        })
        return
      }
      else -> downloadFile(item.book)
    }
  }

  private fun notEnoughSpaceAvailable(item: BookItem) =
    spaceAvailable < item.book.size.toLong() * 1024f

  @SuppressLint("ImplicitSamInstance")
  private fun showStorageSelectDialog() {
    StorageSelectDialog()
      .apply {
        arguments = Bundle().apply {
          putString(
            StorageSelectDialog.STORAGE_DIALOG_INTERNAL,
            this@LibraryFragment.getString(string.internal_storage)
          )
          putString(
            StorageSelectDialog.STORAGE_DIALOG_EXTERNAL,
            this@LibraryFragment.getString(string.external_storage)
          )
          putInt(StorageSelectDialog.STORAGE_DIALOG_THEME, StyleUtils.dialogStyle())
        }
        setOnSelectListener(::storeDeviceInPreferences)
      }
      .show(fragmentManager, getString(string.pref_storage))
  }
}
