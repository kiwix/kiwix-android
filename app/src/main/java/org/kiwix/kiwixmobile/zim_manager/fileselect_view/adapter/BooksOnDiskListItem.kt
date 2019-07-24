package org.kiwix.kiwixmobile.zim_manager.fileselect_view.adapter

import org.kiwix.kiwixmobile.database.newdb.entities.BookOnDiskEntity
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book
import java.io.File
import java.util.Locale

sealed class BooksOnDiskListItem {
  var isSelected: Boolean = false
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

  data class BookOnDisk(
    val databaseId: Long? = null,
    val book: Book,
    val file: File,
    override val id: Long = databaseId ?: 0L
  ) : BooksOnDiskListItem() {

    val locale: Locale by lazy {
      Locale(book.language)
    }

    constructor(bookOnDiskEntity: BookOnDiskEntity) : this(
      bookOnDiskEntity.id,
      bookOnDiskEntity.toBook(),
      bookOnDiskEntity.file
    )
  }
}
