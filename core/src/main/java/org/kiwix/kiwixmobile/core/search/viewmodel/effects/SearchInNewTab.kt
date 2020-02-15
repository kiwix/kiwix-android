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
package org.kiwix.kiwixmobile.core.search.viewmodel.effects

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.utils.Constants

data class SearchInNewTab(private val searchString: String) : SideEffect<Unit> {
  override fun invokeWith(activity: AppCompatActivity) {
    Log.d("kiwix", "SearchInNewTab invoked")
    activity.setResult(
      Activity.RESULT_OK,
      Intent().apply {
        putExtra(SearchInNewTab.EXTRA_SEARCH_IN_NEW_TAB, true)
        putExtra(Constants.TAG_FILE_SEARCHED, searchString)
      }
    )
    activity.finish()
  }

  companion object {
    const val EXTRA_SEARCH_IN_NEW_TAB = "bool_search_in_new_window"
  }
}
