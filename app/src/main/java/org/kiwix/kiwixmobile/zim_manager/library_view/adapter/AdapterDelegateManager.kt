package org.kiwix.kiwixmobile.zim_manager.library_view.adapter

import android.support.v4.util.SparseArrayCompat
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup

class AdapterDelegateManager<T>() {
  fun addDelegate(delegate: AdapterDelegate<T>) {
    delegates.put(delegates.size(), delegate)
  }

  fun createViewHolder(
    parent: ViewGroup,
    viewType: Int
  ) = delegates[viewType].createViewHolder(parent)

  fun onBindViewHolder(
    libraryListItem: T,
    holder: RecyclerView.ViewHolder
  ) {
    delegates[holder.itemViewType].bind(holder, libraryListItem)
  }

  fun getViewTypeFor(item: T) = delegates.keyAt(getDelegateIndexFor(item))

  private fun getDelegateIndexFor(item: T): Int {
    for (index in 0..delegates.size()) {
      val valueAt = delegates.valueAt(index)
      if (valueAt.isFor(item)) {
        return index;
      }
    }
    throw RuntimeException("No delegate registered for $item")
  }

  var delegates: SparseArrayCompat<AdapterDelegate<T>> = SparseArrayCompat()

}
