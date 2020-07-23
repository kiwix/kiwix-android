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
import androidx.appcompat.widget.SearchView
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.base.BaseFragmentActivityExtensions
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.start
import org.kiwix.kiwixmobile.core.utils.SimpleTextListener
import org.kiwix.kiwixmobile.kiwixActivityComponent
import org.kiwix.kiwixmobile.language.LanguageActivity
import org.kiwix.kiwixmobile.zim_manager.library_view.LibraryFragment

class OnlineLibraryFragment : LibraryFragment(), BaseFragmentActivityExtensions {

  override fun inject(baseActivity: BaseActivity) {
    baseActivity.kiwixActivityComponent.inject(this)
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    super<LibraryFragment>.onCreateOptionsMenu(menu, inflater)
    inflater.inflate(R.menu.menu_zim_manager, menu)
    val searchItem = menu.findItem(R.id.action_search)
    val getZimItem = menu.findItem(R.id.get_zim_nearby_device)
    getZimItem?.isVisible = false

    (searchItem?.actionView as? SearchView)?.setOnQueryTextListener(
      SimpleTextListener(zimManageViewModel.requestFiltering::onNext)
    )
    zimManageViewModel.requestFiltering.onNext("")
  }

  override fun onBackPressed(activity: AppCompatActivity): BaseFragmentActivityExtensions.Super {
    getActivity()?.finish()
    return BaseFragmentActivityExtensions.Super.ShouldNotCall
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.select_language -> activity?.start<LanguageActivity>()
    }
    return super.onOptionsItemSelected(item)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    setHasOptionsMenu(true)
    return inflater.inflate(R.layout.fragment_destination_download, container, false)
  }
}
