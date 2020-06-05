package org.kiwix.kiwixmobile.core.bookmark.adapter

import android.view.ViewGroup
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.adapter.AbsDelegateAdapter
import org.kiwix.kiwixmobile.core.bookmark.adapter.BookmarksAdapter.OnItemClickListener
import org.kiwix.kiwixmobile.core.extensions.ViewGroupExtensions.inflate

sealed class BookmarkDelegate :
  AbsDelegateAdapter<BookmarkItem, BookmarkItem, BookmarkViewHolder> {

  class BookmarkItemDelegate(
    private val itemClickListener: OnItemClickListener
  ) : BookmarkDelegate() {
    override val itemClass = BookmarkItem::class.java

    override fun createViewHolder(parent: ViewGroup) =
      BookmarkViewHolder(
        parent.inflate(R.layout.item_bookmark_history, false), itemClickListener)
  }
}
