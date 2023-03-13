/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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
package org.kiwix.kiwixmobile.zimManager.libraryView.adapter

import android.view.ViewGroup
import org.kiwix.kiwixmobile.core.base.adapter.AbsDelegateAdapter
import org.kiwix.kiwixmobile.core.extensions.ViewGroupExtensions.viewBinding
import org.kiwix.kiwixmobile.core.utils.BookUtils
import org.kiwix.kiwixmobile.databinding.ItemDownloadBinding
import org.kiwix.kiwixmobile.databinding.ItemLibraryBinding
import org.kiwix.kiwixmobile.databinding.LibraryDividerBinding
import org.kiwix.kiwixmobile.zimManager.libraryView.adapter.LibraryListItem.BookItem
import org.kiwix.kiwixmobile.zimManager.libraryView.adapter.LibraryListItem.DividerItem
import org.kiwix.kiwixmobile.zimManager.libraryView.adapter.LibraryListItem.LibraryDownloadItem
import org.kiwix.kiwixmobile.zimManager.libraryView.adapter.LibraryViewHolder.DownloadViewHolder
import org.kiwix.kiwixmobile.zimManager.libraryView.adapter.LibraryViewHolder.LibraryBookViewHolder
import org.kiwix.kiwixmobile.zimManager.libraryView.adapter.LibraryViewHolder.LibraryDividerViewHolder

sealed class LibraryDelegate<I : LibraryListItem, out VH : LibraryViewHolder<I>> :
  AbsDelegateAdapter<I, LibraryListItem, VH> {

  class BookDelegate(
    private val bookUtils: BookUtils,
    private val clickAction: (BookItem) -> Unit,
  ) : LibraryDelegate<BookItem, LibraryBookViewHolder>() {
    override val itemClass = BookItem::class.java

    override fun createViewHolder(parent: ViewGroup) =
      LibraryBookViewHolder(
        parent.viewBinding(ItemLibraryBinding::inflate, false),
        bookUtils,
        clickAction,
      )
  }

  class DownloadDelegate(private val clickAction: (LibraryDownloadItem) -> Unit) :
    LibraryDelegate<LibraryDownloadItem, DownloadViewHolder>() {
    override val itemClass = LibraryDownloadItem::class.java

    override fun createViewHolder(parent: ViewGroup) =
      DownloadViewHolder(
        parent.viewBinding(ItemDownloadBinding::inflate, false),
        clickAction
      )
  }

  object DividerDelegate : LibraryDelegate<DividerItem, LibraryDividerViewHolder>() {

    override val itemClass = DividerItem::class.java

    override fun createViewHolder(parent: ViewGroup) =
      LibraryDividerViewHolder(
        parent.viewBinding(LibraryDividerBinding::inflate, false)
      )
  }
}
