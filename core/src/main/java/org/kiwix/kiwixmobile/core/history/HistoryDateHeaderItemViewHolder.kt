package org.kiwix.kiwixmobile.core.history

import android.view.View
import kotlinx.android.synthetic.main.header_date.header_date
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.adapter.BaseViewHolder
import org.kiwix.kiwixmobile.core.history.HistoryListItem.DateItem
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.DateTimeParseException

class HistoryDateHeaderItemViewHolder(itemView: View) : BaseViewHolder<DateItem>(itemView) {
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
