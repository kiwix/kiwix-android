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
package org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.kiwix.kiwixmobile.core.base.adapter.AbsDelegateAdapter
import org.kiwix.kiwixmobile.core.databinding.HeaderLanguageBinding
import org.kiwix.kiwixmobile.core.databinding.ItemBookBinding
import org.kiwix.kiwixmobile.core.extensions.ViewGroupExtensions.viewBinding
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.SelectionMode
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.SelectionMode.NORMAL
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BookOnDiskViewHolder.BookViewHolder
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.BookOnDisk
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.LanguageItem

sealed class BookOnDiskDelegate<I : BooksOnDiskListItem, out VH : BookOnDiskViewHolder<I>> :
  AbsDelegateAdapter<I, BooksOnDiskListItem, VH> {

  class BookDelegate(
    val sharedPreferenceUtil: SharedPreferenceUtil,
    private val clickAction: ((BookOnDisk) -> Unit)? = null,
    private val longClickAction: ((BookOnDisk) -> Unit)? = null,
    private val multiSelectAction: ((BookOnDisk) -> Unit)? = null
  ) : BookOnDiskDelegate<BookOnDisk, BookViewHolder>() {

    override val itemClass = BookOnDisk::class.java

    var selectionMode: SelectionMode = NORMAL

    override fun bind(
      viewHolder: ViewHolder,
      itemToBind: BooksOnDiskListItem
    ) {
      (viewHolder as BookViewHolder).bind(itemToBind as BookOnDisk, selectionMode)
    }

    override fun createViewHolder(parent: ViewGroup) =
      BookViewHolder(
        parent.viewBinding(ItemBookBinding::inflate, false),
        clickAction,
        longClickAction,
        multiSelectAction
      )
  }

  object LanguageDelegate : BookOnDiskDelegate<LanguageItem, LanguageItemViewHolder>() {

    override val itemClass = LanguageItem::class.java

    override fun createViewHolder(parent: ViewGroup) =
      LanguageItemViewHolder(
        parent.viewBinding(HeaderLanguageBinding::inflate, false)
      )
  }
}
