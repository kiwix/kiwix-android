package org.kiwix.kiwixmobile.core.page.history.adapter

import android.widget.ImageView
import org.kiwix.kiwixmobile.core.base.adapter.AdapterDelegate
import org.kiwix.kiwixmobile.core.base.adapter.BaseDelegateAdapter
import org.kiwix.kiwixmobile.core.page.history.adapter.HistoryListItem.HistoryItem

class HistoryAdapter(
  vararg delegates: AdapterDelegate<HistoryListItem>
) : BaseDelegateAdapter<HistoryListItem>(*delegates) {
  override fun getIdFor(item: HistoryListItem): Long = item.id
  interface OnItemClickListener {
    fun onItemClick(favicon: ImageView, history: HistoryItem)

    fun onItemLongClick(favicon: ImageView, history: HistoryItem): Boolean
  }
}
