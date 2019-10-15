/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.language.adapter

import android.view.View
import kotlinx.android.synthetic.main.header_date.header_date
import kotlinx.android.synthetic.main.item_language.item_language_books_count
import kotlinx.android.synthetic.main.item_language.item_language_checkbox
import kotlinx.android.synthetic.main.item_language.item_language_clickable_area
import kotlinx.android.synthetic.main.item_language.item_language_localized_name
import kotlinx.android.synthetic.main.item_language.item_language_name
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.adapter.BaseViewHolder
import org.kiwix.kiwixmobile.language.adapter.LanguageListItem.HeaderItem
import org.kiwix.kiwixmobile.language.adapter.LanguageListItem.LanguageItem

sealed class LanguageListViewHolder<in T : LanguageListItem>(override val containerView: View) :
  BaseViewHolder<T>(containerView) {
  class HeaderViewHolder(view: View) : LanguageListViewHolder<HeaderItem>(view) {
    override fun bind(item: HeaderItem) {
      header_date.setText(
        if (item.id == HeaderItem.SELECTED) R.string.your_languages
        else R.string.other_languages
      )
    }
  }

  class LanguageViewHolder(
    view: View,
    val clickAction: (LanguageItem) -> Unit
  ) : LanguageListViewHolder<LanguageItem>(view) {
    override fun bind(item: LanguageItem) {
      val language = item.language
      item_language_name.text = language.language
      item_language_localized_name.text = language.languageLocalized
      item_language_books_count.text = containerView.resources.getQuantityString(
        R.plurals.books_count, language.occurencesOfLanguage, language.occurencesOfLanguage
      )
      item_language_checkbox.isChecked = language.active
      item_language_clickable_area.setOnClickListener { clickAction(item) }
    }
  }
}
