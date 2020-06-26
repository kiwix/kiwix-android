package org.kiwix.kiwixmobile.core.page.history.adapter

import org.kiwix.kiwixmobile.core.base.adapter.AdapterDelegate
import org.kiwix.kiwixmobile.core.base.adapter.BaseDelegateAdapter

class HistoryAdapter(
  vararg delegates: AdapterDelegate<HistoryListItem>
) : BaseDelegateAdapter<HistoryListItem>(*delegates) {
  override fun getIdFor(item: HistoryListItem): Long = item.id
}
