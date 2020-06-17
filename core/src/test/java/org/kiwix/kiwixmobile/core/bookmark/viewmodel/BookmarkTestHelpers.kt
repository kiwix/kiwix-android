package org.kiwix.kiwixmobile.core.bookmark.viewmodel

import org.kiwix.kiwixmobile.core.bookmark.adapter.BookmarkItem

fun bookmark(
  bookmarkTitle: String = "bookmarkTitle",
  isSelected: Boolean = false,
  id: Long = 2,
  zimId: String = "zimId",
  zimName: String = "zimName",
  zimFilePath: String = "zimFilePath",
  bookmarkUrl: String = "bookmarkUrl",
  favicon: String = "favicon"
): BookmarkItem {
  return BookmarkItem(
    id,
    zimId,
    zimName,
    zimFilePath,
    bookmarkUrl,
    bookmarkTitle,
    favicon,
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
