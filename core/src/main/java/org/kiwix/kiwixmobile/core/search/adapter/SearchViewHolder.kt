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

import android.view.View
import org.kiwix.kiwixmobile.core.base.adapter.BaseViewHolder
import org.kiwix.kiwixmobile.core.databinding.ListItemSearchBinding
import org.kiwix.kiwixmobile.core.search.adapter.SearchListItem.RecentSearchListItem
import org.kiwix.kiwixmobile.core.search.adapter.SearchListItem.ZimSearchResultListItem

sealed class SearchViewHolder<in T : SearchListItem>(containerView: View) :
  BaseViewHolder<T>(containerView) {

  class RecentSearchViewHolder(
    private val listItemSearchBinding: ListItemSearchBinding,
    private val onClickListener: (SearchListItem) -> Unit,
    private val onClickListenerNewTab: (SearchListItem) -> Unit,
    private val onLongClickListener: (SearchListItem) -> Unit
  ) : SearchViewHolder<RecentSearchListItem>(listItemSearchBinding.root) {
    override fun bind(item: RecentSearchListItem) {
      containerView.setOnClickListener { onClickListener(item) }
      containerView.setOnLongClickListener {
        onLongClickListener(item)
        true
      }
      listItemSearchBinding.listItemSearchNewTabButton.setOnClickListener {
        onClickListenerNewTab(
          item
        )
      }
      listItemSearchBinding.listItemSearchText.text = item.value
    }
  }

  class ZimSearchResultViewHolder(
    private val listItemSearchBinding: ListItemSearchBinding,
    private val onClickListener: (SearchListItem) -> Unit,
    private val onClickListenerNewTab: (SearchListItem) -> Unit
  ) : SearchViewHolder<ZimSearchResultListItem>(listItemSearchBinding.root) {
    override fun bind(item: ZimSearchResultListItem) {
      containerView.setOnClickListener { onClickListener(item) }
      listItemSearchBinding.listItemSearchNewTabButton.setOnClickListener {
        onClickListenerNewTab(
          item
        )
      }
      listItemSearchBinding.listItemSearchText.text = item.value
    }
  }
}
