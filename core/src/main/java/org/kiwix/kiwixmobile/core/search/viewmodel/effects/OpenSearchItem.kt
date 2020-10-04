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

import android.os.Parcelable
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.parcel.Parcelize
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.popNavigationBackstack
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.setNavigationResult
import org.kiwix.kiwixmobile.core.search.adapter.SearchListItem
import org.kiwix.kiwixmobile.core.utils.TAG_FILE_SEARCHED

data class OpenSearchItem(
  private val searchListItem: SearchListItem,
  private val openInNewTab: Boolean = false
) : SideEffect<Unit> {
  override fun invokeWith(activity: AppCompatActivity) {
    activity.setNavigationResult(
      SearchItemToOpen(searchListItem.value, openInNewTab),
      TAG_FILE_SEARCHED
    )
    activity.popNavigationBackstack()
  }
}

@Parcelize
data class SearchItemToOpen(
  val pageTitle: String,
  val shouldOpenInNewTab: Boolean
) : Parcelable
