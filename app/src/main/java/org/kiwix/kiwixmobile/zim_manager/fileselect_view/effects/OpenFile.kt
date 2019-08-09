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
package org.kiwix.kiwixmobile.zim_manager.fileselect_view.effects

import android.app.Activity
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.data.ZimContentProvider
import org.kiwix.kiwixmobile.extensions.toast
import org.kiwix.kiwixmobile.zim_manager.ZimManageActivity
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.BookOnDisk

class OpenFile(val bookOnDisk: BookOnDisk) : SideEffect<Unit> {

  override fun invokeWith(activity: Activity) {
    val file = bookOnDisk.file
    ZimContentProvider.canIterate = false
    if (!file.canRead()) {
      activity.toast(R.string.error_filenotfound)
    } else {
      (activity as ZimManageActivity).finishResult(file.path)
    }
  }
}
