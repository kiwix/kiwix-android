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
package org.kiwix.kiwixmobile.zim_manager.library_view.adapter

import android.view.ViewGroup
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.R.layout
import org.kiwix.kiwixmobile.extensions.inflate
import org.kiwix.kiwixmobile.utils.BookUtils
import org.kiwix.kiwixmobile.zim_manager.library_view.adapter.LibraryListItem.BookItem
import org.kiwix.kiwixmobile.zim_manager.library_view.adapter.LibraryListItem.DividerItem
import org.kiwix.kiwixmobile.zim_manager.library_view.adapter.LibraryViewHolder.LibraryBookViewHolder
import org.kiwix.kiwixmobile.zim_manager.library_view.adapter.LibraryViewHolder.LibraryDividerViewHolder

sealed class LibraryDelegate<I : LibraryListItem, VH : LibraryViewHolder<I>> :
    AbsDelegateAdapter<I, LibraryListItem, VH> {

  class BookDelegate(
    private val bookUtils: BookUtils,
    private val clickAction: (BookItem) -> Unit
  ) : LibraryDelegate<BookItem, LibraryBookViewHolder>() {

    override fun createViewHolder(parent: ViewGroup) =
      LibraryBookViewHolder(
          parent.inflate(R.layout.library_item, false),
          bookUtils,
          clickAction
      )

    override fun onBindViewHolder(
      item: BookItem,
      holder: LibraryBookViewHolder
    ) {
      holder.bind(item)
    }

    override fun isFor(item: LibraryListItem) = item is BookItem
  }

  object DividerDelegate : LibraryDelegate<DividerItem, LibraryDividerViewHolder>() {
    override fun createViewHolder(parent: ViewGroup) =
      LibraryDividerViewHolder(parent.inflate(layout.library_divider, false))

    override fun onBindViewHolder(
      item: DividerItem,
      holder: LibraryDividerViewHolder
    ) {
      holder.bind(item)
    }

    override fun isFor(item: LibraryListItem) = item is DividerItem
  }
}