/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
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
 */
package org.kiwix.kiwixmobile.downloader

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.downloader.model.DownloadItem
import org.kiwix.kiwixmobile.extensions.inflate

class DownloadAdapter(private val itemClickListener: (DownloadItem) -> Unit) :
  RecyclerView.Adapter<DownloadViewHolder>() {

  init {
    setHasStableIds(true)
  }

  var itemList: List<DownloadItem> = mutableListOf()
    set(value) {
      field = value
      notifyDataSetChanged()
    }

  override fun getItemId(position: Int) = itemList[position].downloadId

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ) = DownloadViewHolder(parent.inflate(R.layout.download_item, false))

  override fun getItemCount() = itemList.size

  override fun onBindViewHolder(
    holder: DownloadViewHolder,
    position: Int
  ) {
    holder.bind(itemList[position], itemClickListener)
  }
}
