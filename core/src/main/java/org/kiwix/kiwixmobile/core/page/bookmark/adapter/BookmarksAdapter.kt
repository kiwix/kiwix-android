package org.kiwix.kiwixmobile.core.page.bookmark.adapter

import org.kiwix.kiwixmobile.core.base.adapter.AdapterDelegate
import org.kiwix.kiwixmobile.core.base.adapter.BaseDelegateAdapter

class BookmarksAdapter(
  vararg delegates: AdapterDelegate<BookmarkItem>
) : BaseDelegateAdapter<BookmarkItem>(*delegates) {
  override fun getIdFor(item: BookmarkItem): Long = item.databaseId
  interface OnItemClickListener {
    fun onItemClick(bookmark: BookmarkItem)
    fun onItemLongClick(bookmark: BookmarkItem): Boolean
  }
}
