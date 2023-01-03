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
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.adapter.BaseViewHolder
import org.kiwix.kiwixmobile.databinding.HeaderDateBinding
import org.kiwix.kiwixmobile.databinding.ItemLanguageBinding
import org.kiwix.kiwixmobile.language.adapter.LanguageListItem.HeaderItem
import org.kiwix.kiwixmobile.language.adapter.LanguageListItem.LanguageItem

sealed class LanguageListViewHolder<in T : LanguageListItem>(override val containerView: View) :
  BaseViewHolder<T>(containerView) {
  class HeaderViewHolder(private val headerDateBinding: HeaderDateBinding) :
    LanguageListViewHolder<HeaderItem>(headerDateBinding.root) {
    override fun bind(item: HeaderItem) {
      headerDateBinding.headerDate.setText(
        if (item.id == HeaderItem.SELECTED) R.string.your_languages
        else R.string.other_languages
      )
    }
  }

  class LanguageViewHolder(
    private val itemLanguageBinding: ItemLanguageBinding,
    val clickAction: (LanguageItem) -> Unit
  ) : LanguageListViewHolder<LanguageItem>(itemLanguageBinding.root) {
    override fun bind(item: LanguageItem) {
      val language = item.language
      itemLanguageBinding.itemLanguageName.text = language.language
      itemLanguageBinding.itemLanguageLocalizedName.text = language.languageLocalized
      itemLanguageBinding.itemLanguageBooksCount.text = containerView.context
        .getString(R.string.books_count, language.occurencesOfLanguage)
      itemLanguageBinding.itemLanguageCheckbox.isChecked = language.active
      itemLanguageBinding.itemLanguageClickableArea.setOnClickListener { clickAction(item) }
    }
  }
}
