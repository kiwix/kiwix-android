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

import android.content.Intent
import android.net.ConnectivityManager
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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tonyodev.fetch2.Status
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.cachedComponent
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.base.BaseFragment
import org.kiwix.kiwixmobile.core.base.FragmentActivityExtensions
import org.kiwix.kiwixmobile.core.downloader.Downloader
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.navigate
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.viewModel
import org.kiwix.kiwixmobile.core.extensions.closeKeyboard
import org.kiwix.kiwixmobile.core.extensions.coreMainActivity
import org.kiwix.kiwixmobile.core.extensions.setBottomMarginToFragmentContainerView
import org.kiwix.kiwixmobile.core.extensions.snack
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.utils.BookUtils
import org.kiwix.kiwixmobile.core.utils.NetworkUtils
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.SimpleRecyclerViewScrollListener
import org.kiwix.kiwixmobile.core.utils.SimpleTextListener
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.DialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog.YesNoDialog.WifiOnly
import org.kiwix.kiwixmobile.databinding.FragmentDestinationDownloadBinding
import org.kiwix.kiwixmobile.zimManager.NetworkState
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel
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
  @Inject lateinit var alertDialogShower: AlertDialogShower
  private var fragmentDestinationDownloadBinding: FragmentDestinationDownloadBinding? = null

  private var downloadBookItem: LibraryListItem.BookItem? = null
  private val zimManageViewModel by lazy {
    requireActivity().viewModel<ZimManageViewModel>(viewModelFactory)
  }

  private val libraryAdapter: LibraryAdapter by lazy {
    LibraryAdapter(
      LibraryDelegate.BookDelegate(bookUtils, ::onBookItemClick),
      LibraryDelegate.DownloadDelegate {
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
  ): View? {
    setHasOptionsMenu(true)
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

    // hides keyboard when scrolled
    fragmentDestinationDownloadBinding?.libraryList?.addOnScrollListener(
      SimpleRecyclerViewScrollListener { _, newState ->
        if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
          fragmentDestinationDownloadBinding?.libraryList?.closeKeyboard()
        }
      }
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

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    super<BaseFragment>.onCreateOptionsMenu(menu, inflater)
    inflater.inflate(R.menu.menu_zim_manager, menu)
    val searchItem = menu.findItem(R.id.action_search)

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
    fragmentDestinationDownloadBinding?.libraryList?.adapter = null
    fragmentDestinationDownloadBinding = null
  }

  override fun onBackPressed(activity: AppCompatActivity): FragmentActivityExtensions.Super {
    getActivity()?.finish()
    return FragmentActivityExtensions.Super.ShouldNotCall
  }

  private fun onRefreshStateChange(isRefreshing: Boolean?) {
    fragmentDestinationDownloadBinding?.librarySwipeRefresh?.isRefreshing = isRefreshing!!
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
      else -> {}
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
      noInternetSnackbar()
    } else {
      zimManageViewModel.requestDownloadLibrary.onNext(Unit)
    }
    fragmentDestinationDownloadBinding?.libraryErrorText?.visibility = View.GONE
    fragmentDestinationDownloadBinding?.libraryList?.visibility = View.VISIBLE
  }

  private fun downloadFile() {
    downloadBookItem?.book?.let {
      downloader.download(it)
      downloadBookItem = null
    }
  }

  private fun onBookItemClick(item: LibraryListItem.BookItem) {
    downloadBookItem = item
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
      else -> downloadFile()
    }
  }

  private fun clickOnBookItem() {
    downloadBookItem?.let(::onBookItemClick)
  }
}
