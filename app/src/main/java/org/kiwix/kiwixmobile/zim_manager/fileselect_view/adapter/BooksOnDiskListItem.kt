package org.kiwix.kiwixmobile.zim_manager.fileselect_view.adapter

import org.kiwix.kiwixmobile.downloader.model.BookOnDisk
import java.util.Locale

sealed class BooksOnDiskListItem {
  abstract val id: Long

  data class LanguageItem constructor(
    override val id: Long,
    val text: String
  ) : BooksOnDiskListItem() {
    constructor(locale: Locale) : this(
        locale.language.hashCode().toLong(),
        locale.getDisplayLanguage(locale)
    )
  }

  data class BookOnDiskItem(
    val bookOnDisk: BookOnDisk,
    override val id: Long = bookOnDisk.databaseId!!
  ) : BooksOnDiskListItem()
}
