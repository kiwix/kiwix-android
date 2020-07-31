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
import kotlinx.android.synthetic.main.fragment_destination_library.go_to_downloads_button_no_files
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.start
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.utils.LanguageUtils
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BookOnDiskDelegate
import org.kiwix.kiwixmobile.kiwixActivityComponent
import org.kiwix.kiwixmobile.local_file_transfer.LocalFileTransferActivity
import org.kiwix.kiwixmobile.zim_manager.ZimManageViewModel.FileSelectActions
import org.kiwix.kiwixmobile.zim_manager.ZimManageViewModel.FileSelectActions.RequestMultiSelection
import org.kiwix.kiwixmobile.zim_manager.ZimManageViewModel.FileSelectActions.RequestNavigateTo
import org.kiwix.kiwixmobile.zim_manager.ZimManageViewModel.FileSelectActions.RequestSelect
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.FileSelectListState
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.ZimFileSelectFragment

class LocalLibraryFragment : ZimFileSelectFragment() {

  override val bookDelegate: BookOnDiskDelegate.BookDelegate by lazy {
    BookOnDiskDelegate.BookDelegate(sharedPreferenceUtil,
      { offerAction(RequestNavigateTo(it)) },
      { offerAction(RequestMultiSelection(it)) },
      { offerAction(RequestSelect(it)) })
  }

  private fun offerAction(action: FileSelectActions) {
    zimManageViewModel.fileSelectActions.offer(action)
  }

  override fun inject(baseActivity: BaseActivity) {
    baseActivity.kiwixActivityComponent.inject(this)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    (activity as AppCompatActivity).setSupportActionBar(null)
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    inflater.inflate(R.menu.menu_zim_manager, menu)
    val searchItem = menu.findItem(R.id.action_search)
    val languageItem = menu.findItem(R.id.select_language)
    languageItem.isVisible = false
    searchItem.isVisible = false
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
    val activity = activity as CoreMainActivity
    activity.setSupportActionBar(toolbar)
    activity.supportActionBar?.apply {
      setDisplayHomeAsUpEnabled(true)
      setTitle(R.string.library)
    }
    activity.setupDrawerToggle(toolbar)
    return root
  }

  override fun render(state: FileSelectListState) {
    super.render(state)
    val items = state.bookOnDiskListItems
    go_to_downloads_button_no_files.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    go_to_downloads_button_no_files.setOnClickListener {
      offerAction(FileSelectActions.UserClickedDownloadBooksButton)
    }
  }
}
