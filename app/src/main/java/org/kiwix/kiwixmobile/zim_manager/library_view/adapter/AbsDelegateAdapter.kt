package org.kiwix.kiwixmobile.zim_manager.library_view.adapter

import android.support.v7.widget.RecyclerView
import android.view.ViewGroup

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
interface AbsDelegateAdapter<INSTANCE : SUPERTYPE, SUPERTYPE, VIEWHOLDER : RecyclerView.ViewHolder> : AdapterDelegate<SUPERTYPE> {

  override fun bind(
    viewHolder: RecyclerView.ViewHolder,
    itemToBind: SUPERTYPE
  ) {
    onBindViewHolder(itemToBind as INSTANCE, viewHolder as VIEWHOLDER)
  }

  override fun createViewHolder(parent: ViewGroup): VIEWHOLDER

  fun onBindViewHolder(
    item: INSTANCE,
    holder: VIEWHOLDER
  )
}
