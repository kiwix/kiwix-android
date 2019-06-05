package org.kiwix.kiwixmobile.views

import android.graphics.Typeface
import android.view.View
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_language.item_language_books_count
import kotlinx.android.synthetic.main.item_language.item_language_checkbox
import kotlinx.android.synthetic.main.item_language.item_language_localized_name
import kotlinx.android.synthetic.main.item_language.item_language_name
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.utils.LanguageUtils
import org.kiwix.kiwixmobile.zim_manager.library_view.adapter.Language

class LanguageViewHolder(
  override val containerView: View,
  private val onCheckboxChecked: (Int) -> Unit
) : ViewHolder(containerView),
    LayoutContainer {
  fun bind(
    language: Language,
    position: Int
  ) {
    val context = containerView.context
    item_language_name.text = language.language
    item_language_localized_name.text = context.getString(
        R.string.language_localized,
        language.languageLocalized
    )
    item_language_localized_name.typeface = Typeface.createFromAsset(
        context.assets,
        LanguageUtils.getTypeface(language.languageCode)
    )
    item_language_books_count.text =
      context.getString(R.string.language_count, language.occurencesOfLanguage)
    item_language_checkbox.setOnCheckedChangeListener(null)
    item_language_checkbox.isChecked = language.active
    item_language_checkbox.setOnCheckedChangeListener { _, _ ->
      onCheckboxChecked.invoke(position)
    }
    containerView.setOnClickListener {
      item_language_checkbox.toggle()
    }
  }
}
