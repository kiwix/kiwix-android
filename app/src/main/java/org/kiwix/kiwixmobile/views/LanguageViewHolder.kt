package org.kiwix.kiwixmobile.views

import android.graphics.Typeface
import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.View
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.language_check_item.language_checkbox
import kotlinx.android.synthetic.main.language_check_item.language_entries_count
import kotlinx.android.synthetic.main.language_check_item.language_name
import kotlinx.android.synthetic.main.language_check_item.language_name_localized
import kotlinx.android.synthetic.main.language_check_item.language_row
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
    language_name.text = language.language
    language_name_localized.text = context.getString(
        R.string.language_localized,
        language.languageLocalized
    )
    language_name_localized.typeface = Typeface.createFromAsset(
        context.assets,
        LanguageUtils.getTypeface(language.languageCode)
    )
    language_entries_count.text =
      context.getString(R.string.language_count, language.occurencesOfLanguage)
    language_checkbox.setOnCheckedChangeListener(null)
    language_checkbox.isChecked = language.active
    language_checkbox.setOnCheckedChangeListener { _, _ ->
      onCheckboxChecked.invoke(position)
    }
    language_row.setOnClickListener {
      language_checkbox.toggle()
    }
  }
}