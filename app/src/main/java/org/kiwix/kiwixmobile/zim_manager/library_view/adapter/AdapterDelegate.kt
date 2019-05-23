package org.kiwix.kiwixmobile.zim_manager.library_view.adapter

import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.ViewGroup

interface AdapterDelegate<T> {
  fun createViewHolder(parent: ViewGroup): ViewHolder

  fun bind(
    viewHolder: ViewHolder,
    itemToBind: T
  )

  fun isFor(item: T): Boolean

}
