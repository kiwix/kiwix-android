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
package org.kiwix.kiwixmobile.core.history

import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.ViewGroupExtensions.inflate
import org.kiwix.kiwixmobile.core.history.HistoryListItem.DateItem
import org.kiwix.kiwixmobile.core.history.HistoryListItem.HistoryItem

class HistoryAdapter(
  val historyList: List<HistoryListItem>,
  val deleteList: List<HistoryListItem>,
  val itemClickListener: OnItemClickListener
) : Adapter<ViewHolder>() {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
    if (viewType == DateItem.TYPE_ITEM)
      HistoryItemViewHolder(
        parent.inflate(R.layout.item_bookmark_history, false),
        deleteList,
        itemClickListener
      )
    else
      HistoryCategoryItemViewHolder(parent.inflate(R.layout.header_date, false))

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    if (holder is HistoryItemViewHolder) {
      holder.bind(historyList[position] as HistoryItem)
    } else if (holder is HistoryCategoryItemViewHolder) {
      holder.bind(historyList[position] as DateItem)
    }
  }

  override fun getItemViewType(position: Int): Int =
    if (historyList[position] is DateItem) DateItem.TYPE_CATEGORY else DateItem.TYPE_ITEM

  override fun getItemCount(): Int = historyList.size

  interface OnItemClickListener {
    fun onItemClick(favicon: ImageView, history: HistoryItem)

    fun onItemLongClick(favicon: ImageView, history: HistoryItem): Boolean
  }

}
