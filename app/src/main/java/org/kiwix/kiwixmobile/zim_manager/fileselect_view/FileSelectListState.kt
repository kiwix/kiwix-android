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
package org.kiwix.kiwixmobile.zim_manager.fileselect_view

import org.kiwix.kiwixmobile.zim_manager.fileselect_view.SelectionMode.NORMAL
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.adapter.BooksOnDiskListItem
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.BookOnDisk

data class FileSelectListState(
  val bookOnDiskListItems: List<BooksOnDiskListItem>,
  val selectionMode: SelectionMode = NORMAL
) {
  val selectedBooks by lazy {
    bookOnDiskListItems.filter(
      BooksOnDiskListItem::isSelected
    )
      .filterIsInstance(BookOnDisk::class.java)
  }
}

enum class SelectionMode {
  NORMAL,
  MULTI
}
