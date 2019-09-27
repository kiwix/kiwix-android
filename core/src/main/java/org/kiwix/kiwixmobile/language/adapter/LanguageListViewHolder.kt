package org.kiwix.kiwixmobile.language.adapter

import android.view.View
import kotlinx.android.synthetic.main.header_date.header_date
import kotlinx.android.synthetic.main.item_language.item_language_books_count
import kotlinx.android.synthetic.main.item_language.item_language_checkbox
import kotlinx.android.synthetic.main.item_language.item_language_clickable_area
import kotlinx.android.synthetic.main.item_language.item_language_localized_name
import kotlinx.android.synthetic.main.item_language.item_language_name
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.language.adapter.LanguageListItem.HeaderItem
import org.kiwix.kiwixmobile.language.adapter.LanguageListItem.LanguageItem
import org.kiwix.kiwixmobile.zim_manager.library_view.adapter.base.BaseViewHolder

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
