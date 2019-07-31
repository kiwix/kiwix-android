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
package org.kiwix.kiwixmobile.downloader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.layout_download_management.download_management_no_downloads
import kotlinx.android.synthetic.main.layout_download_management.zim_downloader_list
import org.kiwix.kiwixmobile.base.BaseFragment
import org.kiwix.kiwixmobile.di.components.ActivityComponent
import org.kiwix.kiwixmobile.downloader.model.DownloadItem
import org.kiwix.kiwixmobile.extensions.viewModel
import org.kiwix.kiwixmobile.utils.DialogShower
import org.kiwix.kiwixmobile.utils.KiwixDialog.YesNoDialog.StopDownload
import org.kiwix.kiwixmobile.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.zim_manager.ZimManageViewModel
import javax.inject.Inject

class DownloadFragment : BaseFragment() {

  @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
  @Inject lateinit var dialogShower: DialogShower
  @Inject lateinit var sharedPreferenceUtil: SharedPreferenceUtil
  @Inject lateinit var downloader: Downloader

  private val zimManageViewModel by lazy {
    activity!!.viewModel<ZimManageViewModel>(viewModelFactory)
  }

  private val downloadAdapter = DownloadAdapter {
    dialogShower.show(StopDownload, { downloader.cancelDownload(it) })
  }

  override fun inject(activityComponent: ActivityComponent) {
    activityComponent.inject(this)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View =
    inflater.inflate(org.kiwix.kiwixmobile.R.layout.layout_download_management, container, false)

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    zim_downloader_list.run {
      adapter = downloadAdapter
      layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
      setHasFixedSize(true)
    }
    zimManageViewModel.downloadItems.observe(this, Observer {
      onDownloadItemsUpdate(it!!)
    })
  }

  private fun onDownloadItemsUpdate(items: List<DownloadItem>) {
    downloadAdapter.itemList = items
    updateNoDownloads(items)
  }

  private fun updateNoDownloads(downloadItems: List<DownloadItem>) {
    download_management_no_downloads.visibility =
      if (downloadItems.isEmpty()) View.VISIBLE
      else View.GONE
  }
}
