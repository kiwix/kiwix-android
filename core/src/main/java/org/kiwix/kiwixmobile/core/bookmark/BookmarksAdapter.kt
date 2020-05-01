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

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_bookmark_history.favicon
import kotlinx.android.synthetic.main.item_bookmark_history.title
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.adapter.BaseViewHolder
import org.kiwix.kiwixmobile.core.extensions.ViewGroupExtensions.inflate
import org.kiwix.kiwixmobile.core.extensions.setBitmapFromString
import org.kiwix.kiwixmobile.core.extensions.setImageDrawableCompat

internal class BookmarksAdapter(
  private val bookmarkList: List<BookmarkItem>,
  private val deleteList: List<BookmarkItem>,
  private val itemClickListener: OnItemClickListener
) : RecyclerView.Adapter<BookmarksAdapter.BookmarkItemViewHolder>() {
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkItemViewHolder =
    BookmarkItemViewHolder(
      parent.inflate(R.layout.item_bookmark_history, false),
      deleteList,
      itemClickListener
    )

  override fun onBindViewHolder(holder: BookmarkItemViewHolder, position: Int) {
    holder.bind(bookmarkList[position])
  }

  override fun getItemCount(): Int = bookmarkList.size

  internal interface OnItemClickListener {
    fun onItemClick(favicon: ImageView, bookmark: BookmarkItem)
    fun onItemLongClick(favicon: ImageView, bookmark: BookmarkItem): Boolean
  }

  internal class BookmarkItemViewHolder(
    itemView: View,
    private val deleteList: List<BookmarkItem>,
    private val itemClickListener: OnItemClickListener
  ) : BaseViewHolder<BookmarkItem>(itemView) {
    override fun bind(item: BookmarkItem) {
      title.text = item.bookmarkTitle
      if (deleteList.contains(item)) {
        favicon.setImageDrawableCompat(R.drawable.ic_check_circle_blue_24dp)
      } else {
        favicon.setBitmapFromString(item.favicon)
      }
      itemView.setOnClickListener { itemClickListener.onItemClick(favicon, item) }
      itemView.setOnLongClickListener { itemClickListener.onItemLongClick(favicon, item) }
    }
  }
}
