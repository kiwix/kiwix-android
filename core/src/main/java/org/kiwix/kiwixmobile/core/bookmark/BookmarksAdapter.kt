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
package org.kiwix.kiwixmobile.core.bookmark

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.R2
import org.kiwix.kiwixmobile.core.extensions.setBitmapFromString
import org.kiwix.kiwixmobile.core.extensions.setImageDrawableCompat

internal class BookmarksAdapter(
  private val bookmarkList: List<BookmarkItem>,
  private val deleteList: List<BookmarkItem>,
  private val itemClickListener: OnItemClickListener
) : RecyclerView.Adapter<BookmarksAdapter.Item>() {
  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): Item = Item(
    LayoutInflater.from(parent.context)
      .inflate(R.layout.item_bookmark_history, parent, false)
  )

  override fun onBindViewHolder(
    holder: Item,
    position: Int
  ) {
    val bookmark = bookmarkList[position]
    holder.title!!.text = bookmark.bookmarkTitle
    if (deleteList.contains(bookmark)) {
      holder.favicon!!.setImageDrawableCompat(
        R.drawable.ic_check_circle_blue_24dp
      )
    } else {
      holder.favicon!!.setBitmapFromString(bookmark.favicon)
    }
    holder.itemView.setOnClickListener {
      itemClickListener.onItemClick(
        holder.favicon,
        bookmark
      )
    }
    holder.itemView.setOnLongClickListener {
      itemClickListener.onItemLongClick(
        holder.favicon,
        bookmark
      )
    }
  }

  override fun getItemCount(): Int = bookmarkList.size

  internal interface OnItemClickListener {
    fun onItemClick(favicon: ImageView?, bookmark: BookmarkItem)
    fun onItemLongClick(
      favicon: ImageView?,
      bookmark: BookmarkItem
    ): Boolean
  }

  internal class Item(itemView: View?) :
    RecyclerView.ViewHolder(itemView!!) {
    @JvmField @BindView(R2.id.favicon)
    var favicon: ImageView? = null

    @JvmField @BindView(R2.id.title)
    var title: TextView? = null

    init {
      ButterKnife.bind(this, itemView!!)
    }
  }
}
