package org.kiwix.kiwixmobile.core.history

import android.view.View
import kotlinx.android.synthetic.main.header_date.header_date
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.adapter.BaseViewHolder
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.LocalDate

class HistoryCategoryItemViewHolder(itemView: View) : BaseViewHolder<String>(itemView) {
  override fun bind(item: String) {
    val todaysDate = LocalDate.now()
    val yesterdayDate = LocalDate.now().minusDays(1)
    val formatter = DateTimeFormatter.ofPattern("d MMM yyyy")
    val givenDate = LocalDate.parse(item, formatter)

    when {
      todaysDate == givenDate -> { header_date.setText(R.string.time_today) }
      yesterdayDate == givenDate -> { header_date.setText(R.string.time_yesterday) }
      else -> { header_date.text = item }
    }
  }
}
