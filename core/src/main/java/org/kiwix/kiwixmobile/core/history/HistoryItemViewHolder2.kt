package org.kiwix.kiwixmobile.core.history

import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import kotlinx.android.synthetic.main.item_bookmark_history.favicon
import kotlinx.android.synthetic.main.item_bookmark_history.title
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.adapter.BaseViewHolder
import org.kiwix.kiwixmobile.core.downloader.model.Base64String
import org.kiwix.kiwixmobile.core.extensions.setBitmap
import org.kiwix.kiwixmobile.core.extensions.setImageDrawableCompat
import org.kiwix.kiwixmobile.core.history.HistoryAdapter.OnItemClickListener
import org.kiwix.kiwixmobile.core.history.HistoryListItem.HistoryItem
import org.kiwix.kiwixmobile.core.search.adapter.SearchListItem

sealed class HistoryItemViewHolder2 <in T : HistoryListItem>(containerView: View):
  BaseViewHolder<T>(containerView){

  class HistoryItemViewHolder(
    override val containerView: View,
    private val deleteList: List<HistoryListItem>,
    private val onClickListener: OnClickListener,
    private val onLongClickListener: OnLongClickListener
  ) : HistoryItemViewHolder2<HistoryItem>(containerView){
    override fun bind(item: HistoryItem) {
      title.text = item.historyTitle
      if (deleteList.contains(item)) {
        favicon.setImageDrawableCompat(R.drawable.ic_check_circle_blue_24dp)
      } else {
        favicon.setBitmap(Base64String(item.favicon))
      }
      itemView.setOnClickListener(onClickListener)
      itemView.setOnLongClickListener(onLongClickListener)
    }

  }


}
