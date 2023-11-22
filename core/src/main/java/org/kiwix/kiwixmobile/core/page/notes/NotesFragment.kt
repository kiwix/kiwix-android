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

package org.kiwix.kiwixmobile.core.page.notes

import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.cachedComponent
import org.kiwix.kiwixmobile.core.extensions.viewModel
import org.kiwix.kiwixmobile.core.page.PageFragment
import org.kiwix.kiwixmobile.core.page.adapter.PageAdapter
import org.kiwix.kiwixmobile.core.page.adapter.PageDelegate
import org.kiwix.kiwixmobile.core.page.notes.viewmodel.NotesViewModel

class NotesFragment : PageFragment() {
  override val pageViewModel by lazy { viewModel<NotesViewModel>(viewModelFactory) }

  override val screenTitle: String
    get() = getString(R.string.pref_notes)

  override val pageAdapter: PageAdapter by lazy {
    PageAdapter(PageDelegate.PageItemDelegate(this))
  }

  override val noItemsString: String by lazy { getString(R.string.no_notes) }
  override val switchString: String by lazy { getString(R.string.notes_from_all_books) }
  override val switchIsChecked: Boolean by lazy { sharedPreferenceUtil.showNotesAllBooks }

  override fun inject(baseActivity: BaseActivity) {
    baseActivity.cachedComponent.inject(this)
  }

  override val searchQueryHint: String by lazy { getString(R.string.search_notes) }
}
