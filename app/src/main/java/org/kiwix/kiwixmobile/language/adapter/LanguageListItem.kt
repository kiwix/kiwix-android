package org.kiwix.kiwixmobile.language.adapter

import org.kiwix.kiwixmobile.zim_manager.Language

sealed class LanguageListItem {
  abstract val id: Long

  data class HeaderItem constructor(
    override val id: Long
  ) : LanguageListItem() {
    companion object {
      const val SELECTED = Long.MAX_VALUE
      const val OTHER = Long.MIN_VALUE
    }
  }

  data class LanguageItem(
    val language: Language,
    override val id: Long = language.id
  ) : LanguageListItem()
}
