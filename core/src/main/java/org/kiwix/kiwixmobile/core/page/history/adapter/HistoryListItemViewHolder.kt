package org.kiwix.kiwixmobile.core.page.history.adapter

import android.view.View
import kotlinx.android.synthetic.main.header_date.header_date
import kotlinx.android.synthetic.main.item_bookmark_history.favicon
import kotlinx.android.synthetic.main.item_bookmark_history.title
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.adapter.BaseViewHolder
import org.kiwix.kiwixmobile.core.downloader.model.Base64String
import org.kiwix.kiwixmobile.core.extensions.setBitmap
import org.kiwix.kiwixmobile.core.extensions.setImageDrawableCompat
import org.kiwix.kiwixmobile.core.page.history.adapter.HistoryAdapter.OnItemClickListener
import org.kiwix.kiwixmobile.core.page.history.adapter.HistoryListItem.DateItem
import org.kiwix.kiwixmobile.core.page.history.adapter.HistoryListItem.HistoryItem
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.DateTimeParseException

sealed class HistoryListItemViewHolder<in T : HistoryListItem>(containerView: View) :
  BaseViewHolder<T>(containerView) {

  class HistoryItemViewHolder(
    override val containerView: View,
    private val itemClickListener: OnItemClickListener
  ) : HistoryListItemViewHolder<HistoryItem>(containerView) {
    override fun bind(item: HistoryItem) {
      title.text = item.historyTitle
      if (item.isSelected) {
        favicon.setImageDrawableCompat(R.drawable.ic_check_circle_blue_24dp)
      } else {
        favicon.setBitmap(Base64String(item.favicon))
      }
      itemView.setOnClickListener { itemClickListener.onItemClick(favicon, item) }
      itemView.setOnLongClickListener { itemClickListener.onItemLongClick(favicon, item) }
    }
  }

  class DateItemViewHolder(override val containerView: View) :
    HistoryListItemViewHolder<DateItem>(containerView) {

    override fun bind(item: DateItem) {
      val todaysDate = LocalDate.now()
      val yesterdayDate = todaysDate.minusDays(1)
      val givenDate = try {
        LocalDate.parse(item.dateString, DateTimeFormatter.ofPattern("d MMM yyyy"))
      } catch (ignore: DateTimeParseException) {
        null
      }

      when (givenDate) {
        todaysDate -> header_date.setText(R.string.time_today)
        yesterdayDate -> header_date.setText(R.string.time_yesterday)
        else -> header_date.text = item.dateString
      }
    }
  }
}
