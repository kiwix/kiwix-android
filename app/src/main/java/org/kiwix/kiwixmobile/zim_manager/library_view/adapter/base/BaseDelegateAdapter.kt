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
package org.kiwix.kiwixmobile.zim_manager.library_view.adapter.base

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder

abstract class BaseDelegateAdapter<ITEM>(
  vararg delegates: AdapterDelegate<ITEM>,
  private val delegateManager: AdapterDelegateManager<ITEM> = AdapterDelegateManager()
) : Adapter<ViewHolder>() {
  init {
    delegates.forEach(delegateManager::addDelegate)
    @Suppress("LeakingThis")
    setHasStableIds(true)
  }

  var items: List<ITEM> = mutableListOf()
    set(value) {
      field = value
      notifyDataSetChanged()
    }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ) = delegateManager.createViewHolder(parent, viewType)

  override fun getItemCount() = items.size
  override fun onBindViewHolder(
    holder: ViewHolder,
    position: Int
  ) {
    delegateManager.onBindViewHolder(items[position], holder)
  }

  override fun getItemViewType(position: Int) =
    delegateManager.getViewTypeFor(items[position])

  override fun getItemId(position: Int): Long = getIdFor(items[position])

  abstract fun getIdFor(item: ITEM): Long
}
