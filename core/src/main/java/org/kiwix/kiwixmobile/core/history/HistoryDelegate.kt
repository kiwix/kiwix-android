package org.kiwix.kiwixmobile.core.history

import android.content.DialogInterface.OnClickListener
import android.view.ViewGroup
import org.kiwix.kiwixmobile.core.base.adapter.AbsDelegateAdapter
import org.kiwix.kiwixmobile.core.extensions.ViewGroupExtensions.inflate
import org.kiwix.kiwixmobile.core.history.HistoryAdapter.OnItemClickListener
import org.kiwix.kiwixmobile.core.history.HistoryListItem.HistoryItem

sealed class HistoryDelegate<I : HistoryListItem, out VH : HistoryItemViewHolder2<I>> :
  AbsDelegateAdapter<I, HistoryListItem, VH> {

  class HistoryItemDelegate(
    private val deleteList: List<HistoryListItem>,
    private val itemClickListener: OnItemClickListener
  ) : HistoryDelegate<HistoryItem, HistoryItemViewHolder2.HistoryItemViewHolder>(){
    override val itemClass = HistoryItem::class.java

    override fun createViewHolder(parent: ViewGroup) = HistoryItemViewHolder2.HistoryItemViewHolder(parent.inflate(android.R.layout.simple_selectable_list_item, false),
    deleteList, itemClickListener)
  }

  }
