package org.kiwix.kiwixmobile.core.bookmark.viewmodel

import org.kiwix.kiwixmobile.core.bookmark.adapter.BookmarkItem

// dateFormat = d MMM yyyy
//             5 Jul 2020
fun createSimpleBookmarkItem(
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
