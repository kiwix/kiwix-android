package org.kiwix.kiwixmobile.language.adapter

import android.view.ViewGroup
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.extensions.inflate
import org.kiwix.kiwixmobile.language.adapter.LanguageListItem.HeaderItem
import org.kiwix.kiwixmobile.language.adapter.LanguageListItem.LanguageItem
import org.kiwix.kiwixmobile.language.adapter.LanguageListViewHolder.HeaderViewHolder
import org.kiwix.kiwixmobile.language.adapter.LanguageListViewHolder.LanguageViewHolder
import org.kiwix.kiwixmobile.zim_manager.library_view.adapter.base.AbsDelegateAdapter

sealed class LanguageDelegate<I : LanguageListItem, out VH : LanguageListViewHolder<I>> :
  AbsDelegateAdapter<I, LanguageListItem, VH> {

  class HeaderDelegate : LanguageDelegate<HeaderItem, HeaderViewHolder>() {
    override val itemClass = HeaderItem::class.java

    override fun createViewHolder(parent: ViewGroup) =
      HeaderViewHolder(parent.inflate(R.layout.header_date, false))
  }

  class LanguageItemDelegate(private val clickAction: (LanguageItem) -> Unit) :
    LanguageDelegate<LanguageItem, LanguageViewHolder>() {
    override val itemClass = LanguageItem::class.java

    override fun createViewHolder(parent: ViewGroup) =
      LanguageViewHolder(parent.inflate(R.layout.item_language, false), clickAction)
  }
}
