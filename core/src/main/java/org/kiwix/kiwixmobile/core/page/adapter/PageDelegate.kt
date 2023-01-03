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

package org.kiwix.kiwixmobile.core.page.adapter

import android.view.ViewGroup
import org.kiwix.kiwixmobile.core.base.adapter.AbsDelegateAdapter
import org.kiwix.kiwixmobile.core.databinding.HeaderDateBinding
import org.kiwix.kiwixmobile.core.databinding.ItemBookmarkHistoryBinding
import org.kiwix.kiwixmobile.core.extensions.ViewGroupExtensions.viewBinding
import org.kiwix.kiwixmobile.core.page.adapter.PageRelatedListItemViewHolder.DateItemViewHolder
import org.kiwix.kiwixmobile.core.page.adapter.PageRelatedListItemViewHolder.PageListItemViewHolder
import org.kiwix.kiwixmobile.core.page.history.adapter.HistoryListItem.DateItem

sealed class PageDelegate<I : PageRelated, out VH : PageRelatedListItemViewHolder<I>> :
  AbsDelegateAdapter<I, PageRelated, VH> {

  class PageItemDelegate(
    private val itemClickListener: OnItemClickListener
  ) : PageDelegate<Page, PageListItemViewHolder>() {
    override val itemClass = Page::class.java

    override fun createViewHolder(parent: ViewGroup) =
      PageListItemViewHolder(
        parent.viewBinding(ItemBookmarkHistoryBinding::inflate, false),
        itemClickListener
      )
  }

  class HistoryDateDelegate : PageDelegate<DateItem, DateItemViewHolder>() {
    override val itemClass = DateItem::class.java

    override fun createViewHolder(parent: ViewGroup): DateItemViewHolder =
      DateItemViewHolder(
        parent.viewBinding(HeaderDateBinding::inflate, false)
      )
  }
}
