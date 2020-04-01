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

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import kotlinx.android.synthetic.main.header_date.header_date
import kotlinx.android.synthetic.main.item_bookmark_history.favicon
import kotlinx.android.synthetic.main.item_bookmark_history.title
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.adapter.BaseViewHolder
import org.kiwix.kiwixmobile.core.downloader.model.Base64String
import org.kiwix.kiwixmobile.core.extensions.ViewGroupExtensions.inflate
import org.kiwix.kiwixmobile.core.extensions.setBitmap
import org.kiwix.kiwixmobile.core.extensions.setImageDrawableCompat
import org.kiwix.kiwixmobile.core.history.HistoryListItem.DateItem
import org.kiwix.kiwixmobile.core.history.HistoryListItem.HistoryItem
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter

internal class HistoryAdapter(
  val historyList: List<HistoryListItem>,
  val deleteList: List<HistoryListItem>,
  val itemClickListener: OnItemClickListener
) : Adapter<ViewHolder>() {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
    if (viewType == TYPE_ITEM)
      Item(parent.inflate(R.layout.item_bookmark_history, false))
    else
      Category(parent.inflate(R.layout.header_date, false))

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    if (holder is Item) {
      val historyItem = historyList[position] as HistoryItem
      holder.bind(historyItem)
    } else if (holder is Category) {
      val date = (historyList[position] as DateItem).dateString
      holder.bind(date)
    }
  }

  override fun getItemViewType(position: Int): Int =
    if (historyList[position] is DateItem) 0 else TYPE_ITEM

  override fun getItemCount(): Int = historyList.size

  internal interface OnItemClickListener {
    fun onItemClick(
      favicon: ImageView,
      history: HistoryItem
    )

    fun onItemLongClick(
      favicon: ImageView,
      history: HistoryItem
    ): Boolean
  }

  inner class Item(itemView: View) : BaseViewHolder<HistoryItem>(itemView) {
    override fun bind(item: HistoryItem) {
      title.text = item.historyTitle
      if (deleteList.contains(item)) {
        favicon.setImageDrawableCompat(
          R.drawable.ic_check_circle_blue_24dp
        )
      } else {
        favicon.setBitmap(Base64String(item.favicon))
      }
      itemView.setOnClickListener {
        itemClickListener.onItemClick(
          favicon, item
        )
      }
      itemView.setOnLongClickListener {
        itemClickListener.onItemLongClick(
          favicon, item
        )
      }
    }
  }

  inner class Category(itemView: View) : BaseViewHolder<String>(itemView) {
    override fun bind(item: String) {
      val todaysDate = LocalDate.now()
      val yesterdayDate = LocalDate.now().minusDays(1)
      val formatter = DateTimeFormatter.ofPattern("d MMM yyyy")
      val givenDate = LocalDate.parse(item, formatter)

      when {
        todaysDate == givenDate -> {
          header_date.setText(
            R.string.time_today
          )
        }
        yesterdayDate == givenDate -> {
          header_date.setText(
            R.string.time_yesterday
          )
        }
        else -> {
          header_date.text = item
        }
      }
    }
  }

  companion object {
    private const val TYPE_ITEM = 1
  }
}
