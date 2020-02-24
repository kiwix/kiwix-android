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

package org.kiwix.kiwixmobile.core.search.adapter

import android.view.ViewGroup
import org.kiwix.kiwixmobile.core.base.adapter.AbsDelegateAdapter
import org.kiwix.kiwixmobile.core.extensions.ViewGroupExtensions.inflate
import org.kiwix.kiwixmobile.core.search.adapter.SearchListItem.RecentSearchListItem
import org.kiwix.kiwixmobile.core.search.adapter.SearchListItem.ZimSearchResultListItem
import org.kiwix.kiwixmobile.core.search.adapter.SearchViewHolder.RecentSearchViewHolder
import org.kiwix.kiwixmobile.core.search.adapter.SearchViewHolder.ZimSearchResultViewHolder

sealed class SearchDelegate<I : SearchListItem, out VH : SearchViewHolder<I>> :
  AbsDelegateAdapter<I, SearchListItem, VH> {

  class RecentSearchDelegate(
    private val onClickListener: (SearchListItem) -> Unit,
    private val onLongClickListener: (SearchListItem) -> Unit
  ) : SearchDelegate<RecentSearchListItem, RecentSearchViewHolder>() {
    override val itemClass = RecentSearchListItem::class.java

    override fun createViewHolder(parent: ViewGroup) =
      RecentSearchViewHolder(
        parent.inflate(android.R.layout.simple_list_item_1, false),
        onClickListener,
        onLongClickListener
      )
  }

  class ZimSearchResultDelegate(
    private val onClickListener: (SearchListItem) -> Unit
  ) : SearchDelegate<ZimSearchResultListItem, ZimSearchResultViewHolder>() {
    override val itemClass = ZimSearchResultListItem::class.java

    override fun createViewHolder(parent: ViewGroup) =
      ZimSearchResultViewHolder(
        parent.inflate(android.R.layout.simple_list_item_1, false),
        onClickListener
      )
  }
}
