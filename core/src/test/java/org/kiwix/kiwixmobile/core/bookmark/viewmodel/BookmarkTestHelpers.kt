package org.kiwix.kiwixmobile.core.bookmark.viewmodel

import org.kiwix.kiwixmobile.core.bookmark.adapter.BookmarkItem

// dateFormat = d MMM yyyy
//             5 Jul 2020
fun bookmark(
  bookmarkTitle: String = "bookmarkTitle",
  isSelected: Boolean = false,
  id: Long = 2
): BookmarkItem {
  return BookmarkItem(
    id,
    "zimId",
    "zimName",
    "zimFilePath",
    "bookmarkUrl",
    bookmarkTitle,
    "favicon",
    isSelected
  )
}

fun bookmarkState(
  bookmarks: List<BookmarkItem> = listOf(),
  showAll: Boolean = true,
  zimId: String = "id",
  searchTerm: String = ""
): BookmarkState =
  BookmarkState(
    bookmarks,
    showAll,
    zimId,
    searchTerm
  )
