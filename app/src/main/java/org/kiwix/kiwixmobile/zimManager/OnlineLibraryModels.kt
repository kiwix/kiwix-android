package org.kiwix.kiwixmobile.zimManager

import org.kiwix.kiwixmobile.core.entity.LibkiwixBook
import org.kiwix.kiwixmobile.zimManager.libraryView.LibraryListItem

data class OnlineLibraryRequest(
  val query: String? = null,
  val category: String? = null,
  val lang: String? = null,
  val isLoadMoreItem: Boolean,
  val page: Int,
  // Bug Fix #4381
  val version: Long = System.nanoTime()
)

data class OnlineLibraryResult(
  val onlineLibraryRequest: OnlineLibraryRequest,
  val books: List<LibkiwixBook>
)

data class LibraryListItemWrapper(
  val items: List<LibraryListItem>,
  val version: Long = System.nanoTime()
)
