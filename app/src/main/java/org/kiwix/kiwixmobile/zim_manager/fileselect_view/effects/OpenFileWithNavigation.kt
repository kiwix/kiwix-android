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

package org.kiwix.kiwixmobile.zim_manager.fileselect_view.effects

import androidx.appcompat.app.AppCompatActivity
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.navigate
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem
import org.kiwix.kiwixmobile.nav.destination.library.LocalLibraryFragmentDirections.actionNavigationLibraryToNavigationReader

data class OpenFileWithNavigation(private val bookOnDisk: BooksOnDiskListItem.BookOnDisk) :
  SideEffect<Unit> {

  override fun invokeWith(activity: AppCompatActivity) {
    val zimSource = bookOnDisk.zimSource
    if (zimSource.exists()) {
      activity.navigate(
        actionNavigationLibraryToNavigationReader().apply { zimFileUri = zimSource.toDatabase() }
      )
    } else {
      activity.toast(R.string.error_file_not_found)
    }
  }
}
