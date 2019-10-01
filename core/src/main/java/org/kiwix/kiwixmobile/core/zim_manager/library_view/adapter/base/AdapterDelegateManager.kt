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

package org.kiwix.kiwixmobile.core.zim_manager.library_view.adapter.base

import android.view.ViewGroup
import androidx.collection.SparseArrayCompat
import androidx.recyclerview.widget.RecyclerView

class AdapterDelegateManager<T> {
  fun addDelegate(delegate: AdapterDelegate<T>) {
    delegates.put(delegates.size(), delegate)
  }

  fun createViewHolder(
    parent: ViewGroup,
    viewType: Int
  ) = delegates[viewType]!!.createViewHolder(parent)

  fun onBindViewHolder(
    libraryListItem: T,
    holder: RecyclerView.ViewHolder
  ) {
    delegates[holder.itemViewType]!!.bind(holder, libraryListItem)
  }

  fun getViewTypeFor(item: T) = delegates.keyAt(getDelegateIndexFor(item))

  private fun getDelegateIndexFor(item: T): Int {
    for (index in 0..delegates.size()) {
      val valueAt = delegates.valueAt(index)
      if (valueAt?.isFor(item) == true) {
        return index
      }
    }
    throw RuntimeException("No delegate registered for $item")
  }

  private var delegates: SparseArrayCompat<AdapterDelegate<T>> = SparseArrayCompat()
}
