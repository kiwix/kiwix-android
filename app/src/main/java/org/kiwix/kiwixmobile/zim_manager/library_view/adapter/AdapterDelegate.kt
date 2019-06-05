package org.kiwix.kiwixmobile.zim_manager.library_view.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder

interface AdapterDelegate<T> {
  fun createViewHolder(parent: ViewGroup): ViewHolder

  fun bind(
    viewHolder: ViewHolder,
    itemToBind: T
  )

  fun isFor(item: T): Boolean

}
