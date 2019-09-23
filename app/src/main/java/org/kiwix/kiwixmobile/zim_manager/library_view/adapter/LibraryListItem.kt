package org.kiwix.kiwixmobile.zim_manager.library_view.adapter

import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book

sealed class LibraryListItem {
  abstract val id: Long

  data class DividerItem constructor(
    override val id: Long,
    val text: String
  ) : LibraryListItem()

  data class BookItem(
    val book: Book,
    override val id: Long = book.id.hashCode().toLong()
  ) : LibraryListItem()
}
