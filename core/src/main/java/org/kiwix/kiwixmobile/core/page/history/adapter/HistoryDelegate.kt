package org.kiwix.kiwixmobile.core.page.history.adapter

import android.view.ViewGroup
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.adapter.AbsDelegateAdapter
import org.kiwix.kiwixmobile.core.extensions.ViewGroupExtensions.inflate
import org.kiwix.kiwixmobile.core.page.adapter.OnItemClickListener
import org.kiwix.kiwixmobile.core.page.adapter.PageRelated
import org.kiwix.kiwixmobile.core.page.history.adapter.HistoryListItem.DateItem
import org.kiwix.kiwixmobile.core.page.history.adapter.HistoryListItem.HistoryItem
import org.kiwix.kiwixmobile.core.page.history.adapter.HistoryListItemViewHolder.DateItemViewHolder
import org.kiwix.kiwixmobile.core.page.history.adapter.HistoryListItemViewHolder.HistoryItemViewHolder

sealed class HistoryDelegate<I : PageRelated, out VH : HistoryListItemViewHolder<I>> :
  AbsDelegateAdapter<I, PageRelated, VH> {

  class HistoryItemDelegate(
    private val itemClickListener: OnItemClickListener
  ) : HistoryDelegate<HistoryItem, HistoryItemViewHolder>() {
    override val itemClass = HistoryItem::class.java

    override fun createViewHolder(parent: ViewGroup) =
      HistoryItemViewHolder(
        parent.inflate(R.layout.item_bookmark_history, false), itemClickListener
      )
  }

  class HistoryDateDelegate : HistoryDelegate<DateItem, DateItemViewHolder>() {
    override val itemClass = DateItem::class.java

    override fun createViewHolder(parent: ViewGroup): DateItemViewHolder =
      DateItemViewHolder(parent.inflate(R.layout.header_date, false))
  }
}
