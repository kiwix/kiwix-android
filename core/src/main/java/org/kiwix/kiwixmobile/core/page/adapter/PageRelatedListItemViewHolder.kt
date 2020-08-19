/*
 * Kiwix Android
 * Copyright (c) 2020 Kiwix <android.kiwix.org>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.kiwix.kiwixmobile.core.page.adapter

import android.view.View
import kotlinx.android.synthetic.main.header_date.header_date
import kotlinx.android.synthetic.main.item_bookmark_history.favicon
import kotlinx.android.synthetic.main.item_bookmark_history.title
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.adapter.BaseViewHolder
import org.kiwix.kiwixmobile.core.downloader.model.Base64String
import org.kiwix.kiwixmobile.core.extensions.setBitmap
import org.kiwix.kiwixmobile.core.extensions.setImageDrawableCompat
import org.kiwix.kiwixmobile.core.page.history.adapter.HistoryListItem.DateItem
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.DateTimeParseException

sealed class PageRelatedListItemViewHolder<in T : PageRelated>(containerView: View) :
  BaseViewHolder<T>(containerView) {

  class PageListItemViewHolder(
    override val containerView: View,
    private val itemClickListener: OnItemClickListener
  ) : PageRelatedListItemViewHolder<Page>(containerView) {
    override fun bind(item: Page) {
      title.text = item.title
      if (item.isSelected) {
        favicon.setImageDrawableCompat(R.drawable.ic_check_circle_blue_24dp)
      } else {
        favicon.setBitmap(Base64String(item.favicon))
      }
      itemView.setOnClickListener { itemClickListener.onItemClick(item) }
      itemView.setOnLongClickListener { itemClickListener.onItemLongClick(item) }
    }
  }

  class DateItemViewHolder(override val containerView: View) :
    PageRelatedListItemViewHolder<DateItem>(containerView) {

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
