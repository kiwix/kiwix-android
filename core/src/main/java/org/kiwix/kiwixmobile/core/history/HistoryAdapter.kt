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

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import butterknife.BindView
import butterknife.ButterKnife
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.R.layout
import org.kiwix.kiwixmobile.core.R2.id
import org.kiwix.kiwixmobile.core.downloader.model.Base64String
import org.kiwix.kiwixmobile.core.extensions.ViewGroupExtensions.inflate
import org.kiwix.kiwixmobile.core.extensions.setBitmap
import org.kiwix.kiwixmobile.core.history.HistoryListItem.DateItem
import org.kiwix.kiwixmobile.core.history.HistoryListItem.HistoryItem
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter

internal class HistoryAdapter(
  val historyList: List<HistoryListItem>,
  val deleteList: List<HistoryListItem>,
  val itemClickListener: OnItemClickListener
) : Adapter<ViewHolder>() {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    return if (viewType == TYPE_ITEM) {
      Item(parent.inflate(layout.item_bookmark_history, false))
    } else {
      Category(parent.inflate(layout.header_date, false))
    }
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    if (holder is Item) {
      val history = historyList[position] as HistoryItem
      setItemDataWithHelpOfHistoryItem(holder, history)
    } else if (holder is Category) {
      val date = (historyList[position] as DateItem).dateString
      setCategoryDataWithHelpOfDate(date, holder)
    }
  }

  private fun setCategoryDataWithHelpOfDate(date: String, holder: Category) {
    val todaysDate = LocalDate.now()
    val yesterdayDate = LocalDate.now().minusDays(1)
    val formatter = DateTimeFormatter.ofPattern("d MMM yyyy")
    val givenDate = LocalDate.parse(date, formatter)

    if (todaysDate?.equals(givenDate) == true) {
      holder.date.setText(
        R.string.time_today
      )
    } else if (yesterdayDate?.equals(givenDate) == true) {
      holder.date.setText(
        R.string.time_yesterday
      )
    } else {
      holder.date.text = date
    }
  }

  fun ImageView.setImageDrawableCompat(context: Context, id: Int) {
    setImageDrawable(ContextCompat.getDrawable(context, id))
  }

  private fun setItemDataWithHelpOfHistoryItem(holder: Item, history: HistoryItem) {
    holder.title.text = history.historyTitle
    if (deleteList.contains(history)) {
      holder.favicon.setImageDrawableCompat(
          holder.favicon.context,
          R.drawable.ic_check_circle_blue_24dp
      )
    } else {
      holder.favicon.setBitmap(Base64String(history.favicon))
    }
    holder.itemView.setOnClickListener {
      itemClickListener.onItemClick(
        holder.favicon, history
      )
    }
    holder.itemView.setOnLongClickListener {
      itemClickListener.onItemLongClick(
        holder.favicon, history
      )
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

  internal inner class Item(itemView: View) : ViewHolder(itemView) {
    @BindView(id.favicon) lateinit var favicon: ImageView
    @BindView(id.title) lateinit var title: TextView
    init {
      ButterKnife.bind(this, itemView)
    }
  }

  internal inner class Category(itemView: View) : ViewHolder(itemView) {
    @BindView(id.header_date) lateinit var date: TextView
    init {
      ButterKnife.bind(this, itemView)
    }
  }

  companion object {
    private const val TYPE_ITEM = 1
  }
}
