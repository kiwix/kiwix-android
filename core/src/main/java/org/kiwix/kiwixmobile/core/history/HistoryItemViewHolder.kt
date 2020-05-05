package org.kiwix.kiwixmobile.core.history

import android.view.View
import kotlinx.android.synthetic.main.item_bookmark_history.favicon
import kotlinx.android.synthetic.main.item_bookmark_history.title
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.adapter.BaseViewHolder
import org.kiwix.kiwixmobile.core.downloader.model.Base64String
import org.kiwix.kiwixmobile.core.extensions.setBitmap
import org.kiwix.kiwixmobile.core.extensions.setImageDrawableCompat
import org.kiwix.kiwixmobile.core.history.HistoryAdapter.OnItemClickListener
import org.kiwix.kiwixmobile.core.history.adapter.HistoryListItem
import org.kiwix.kiwixmobile.core.history.adapter.HistoryListItem.HistoryItem

class HistoryItemViewHolder(
  itemView: View,
  val deleteList: List<HistoryListItem>,
  val itemClickListener: OnItemClickListener
) : BaseViewHolder<HistoryItem>(itemView) {
  override fun bind(item: HistoryItem) {
    title.text = item.historyTitle
    if (deleteList.contains(item)) {
      favicon.setImageDrawableCompat(R.drawable.ic_check_circle_blue_24dp)
    } else {
      favicon.setBitmap(Base64String(item.favicon))
    }
    itemView.setOnClickListener { itemClickListener.onItemClick(favicon, item) }
    itemView.setOnLongClickListener { itemClickListener.onItemLongClick(favicon, item) }
  }
}
