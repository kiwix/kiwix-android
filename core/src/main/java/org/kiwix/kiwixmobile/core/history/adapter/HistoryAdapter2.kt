package org.kiwix.kiwixmobile.core.history.adapter

import org.kiwix.kiwixmobile.core.base.adapter.AdapterDelegate
import org.kiwix.kiwixmobile.core.base.adapter.BaseDelegateAdapter

class HistoryAdapter2(
  vararg delegates: AdapterDelegate<HistoryListItem>
) : BaseDelegateAdapter<HistoryListItem>(*delegates){
  override fun getIdFor(item: HistoryListItem): Long = item.id
}
