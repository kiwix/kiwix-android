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
import android.widget.TextView
import androidx.core.view.get
import kotlinx.android.synthetic.main.list_item_search.list_item_search_new_tab_button
import kotlinx.android.synthetic.main.list_item_search.list_item_search_text
import kotlinx.android.synthetic.main.list_item_search.view.list_item_search_new_tab_button
import kotlinx.android.synthetic.main.list_item_search.view.list_item_search_text
import org.kiwix.kiwixmobile.core.base.adapter.BaseViewHolder
import org.kiwix.kiwixmobile.core.search.adapter.SearchListItem.RecentSearchListItem
import org.kiwix.kiwixmobile.core.search.adapter.SearchListItem.ZimSearchResultListItem

sealed class SearchViewHolder<in T : SearchListItem>(containerView: View) :
  BaseViewHolder<T>(containerView) {

  class RecentSearchViewHolder(
    override val containerView: View,
    private val onClickListener: (SearchListItem) -> Unit,
    private val onClickListenerNewTab: (SearchListItem) -> Unit,
    private val onLongClickListener: (SearchListItem) -> Unit
  ) : SearchViewHolder<RecentSearchListItem>(containerView) {
    override fun bind(item: RecentSearchListItem) {
      containerView.setOnClickListener { onClickListener(item) }
      containerView.list_item_search_new_tab_button
        .setOnClickListener { onClickListenerNewTab(item) }
      containerView.setOnLongClickListener {
        onLongClickListener(item)
        true
      }
      (containerView.list_item_search_text as TextView).text = item.value
    }
  }

  class ZimSearchResultViewHolder(
    override val containerView: View,
    private val onClickListener: (SearchListItem) -> Unit,
    private val onClickListenerNewTab: (SearchListItem) -> Unit
  ) : SearchViewHolder<ZimSearchResultListItem>(containerView) {
    override fun bind(item: ZimSearchResultListItem) {
      containerView.setOnClickListener { onClickListener(item) }
      list_item_search_new_tab_button.setOnClickListener { onClickListenerNewTab(item) }
      (list_item_search_text as TextView).text = item.value
    }
  }
}
