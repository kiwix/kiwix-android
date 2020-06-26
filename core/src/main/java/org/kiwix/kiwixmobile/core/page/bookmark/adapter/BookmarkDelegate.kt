package org.kiwix.kiwixmobile.core.page.bookmark.adapter

import android.view.ViewGroup
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.adapter.AbsDelegateAdapter
import org.kiwix.kiwixmobile.core.extensions.ViewGroupExtensions.inflate
import org.kiwix.kiwixmobile.core.page.adapter.OnItemClickListener
import org.kiwix.kiwixmobile.core.page.adapter.PageRelated

sealed class BookmarkDelegate :
  AbsDelegateAdapter<PageRelated, PageRelated, BookmarkViewHolder> {

  class BookmarkItemDelegate(
    private val itemClickListener: OnItemClickListener
  ) : BookmarkDelegate() {
    override val itemClass = PageRelated::class.java

    override fun createViewHolder(parent: ViewGroup) =
      BookmarkViewHolder(
        parent.inflate(R.layout.item_bookmark_history, false), itemClickListener
      )
  }
}
