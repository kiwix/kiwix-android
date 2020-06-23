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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.start
import org.kiwix.kiwixmobile.core.utils.LanguageUtils
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BookOnDiskDelegate
import org.kiwix.kiwixmobile.kiwixActivityComponent
import org.kiwix.kiwixmobile.local_file_transfer.LocalFileTransferActivity
import org.kiwix.kiwixmobile.zim_manager.ZimManageViewModel
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.ZimFileSelectFragment

class LocalLibraryFragment : ZimFileSelectFragment() {

  private var searchItem: MenuItem? = null
  private var languageItem: MenuItem? = null
  private var getZimItem: MenuItem? = null

  override val bookDelegate: BookOnDiskDelegate.BookDelegate by lazy {
    BookOnDiskDelegate.BookDelegate(sharedPreferenceUtil,
      { offerAction(ZimManageViewModel.FileSelectActions.RequestNavigateTo(it)) },
      { offerAction(ZimManageViewModel.FileSelectActions.RequestMultiSelection(it)) },
      { offerAction(ZimManageViewModel.FileSelectActions.RequestSelect(it)) })
  }

  private fun offerAction(action: ZimManageViewModel.FileSelectActions) {
    zimManageViewModel.fileSelectActions.offer(action)
  }

  override fun inject(baseActivity: BaseActivity) {
    baseActivity.kiwixActivityComponent.inject(this)
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    inflater.inflate(R.menu.menu_zim_manager, menu)
    searchItem = menu.findItem(R.id.action_search)
    languageItem = menu.findItem(R.id.select_language)
    getZimItem = menu.findItem(R.id.get_zim_nearby_device)
    languageItem?.isVisible = false
    searchItem?.isVisible = false
    super.onCreateOptionsMenu(menu, inflater)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.get_zim_nearby_device -> activity?.start<LocalFileTransferActivity>()
    }
    return super.onOptionsItemSelected(item)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    LanguageUtils(requireActivity())
      .changeFont(requireActivity().layoutInflater, sharedPreferenceUtil)
    setHasOptionsMenu(true)
    val root = inflater.inflate(R.layout.fragment_destination_library, container, false)
    val toolbar = root.findViewById<Toolbar>(R.id.toolbar)
    val activity = activity as AppCompatActivity
    activity.setSupportActionBar(toolbar)
    activity.supportActionBar!!.setTitle(R.string.library)
    return root
  }
}
