/*
 * Copyright 2013  Rashiq Ahmad <rashiq.z@gmail.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU  General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */

package org.kiwix.kiwixmobile.zim_manager.library_view.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class LibraryAdapter(
  private val delegateManager: AdapterDelegateManager<LibraryListItem> = AdapterDelegateManager(),
  vararg delegates: AdapterDelegate<LibraryListItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

  init {
    delegates.forEach(delegateManager::addDelegate)
    setHasStableIds(true)
  }

  var itemList: List<LibraryListItem> = mutableListOf()
    set(value) {
      field = value
      notifyDataSetChanged()
    }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ) = delegateManager.createViewHolder(parent, viewType)

  override fun getItemCount() = itemList.size

  override fun onBindViewHolder(
    holder: RecyclerView.ViewHolder,
    position: Int
  ) {
    delegateManager.onBindViewHolder(itemList[position], holder)
  }

  override fun getItemId(position: Int) = itemList[position].id

  override fun getItemViewType(position: Int) =
    delegateManager.getViewTypeFor(itemList[position])

}
