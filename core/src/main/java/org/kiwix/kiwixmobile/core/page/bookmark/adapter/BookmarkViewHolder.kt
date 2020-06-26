package org.kiwix.kiwixmobile.core.page.bookmark.adapter

import android.view.View
import kotlinx.android.synthetic.main.item_bookmark_history.favicon
import kotlinx.android.synthetic.main.item_bookmark_history.title
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.adapter.BaseViewHolder
import org.kiwix.kiwixmobile.core.downloader.model.Base64String
import org.kiwix.kiwixmobile.core.extensions.setBitmap
import org.kiwix.kiwixmobile.core.extensions.setImageDrawableCompat
import org.kiwix.kiwixmobile.core.page.adapter.OnItemClickListener
import org.kiwix.kiwixmobile.core.page.adapter.PageRelated

class BookmarkViewHolder(
  override val containerView: View,
  private val itemClickListener: OnItemClickListener
) : BaseViewHolder<PageRelated>(containerView) {
  override fun bind(item: PageRelated) {
    item as BookmarkItem
    title.text = item.bookmarkTitle
    if (item.isSelected) {
      favicon.setImageDrawableCompat(R.drawable.ic_check_circle_blue_24dp)
    } else {
      favicon.setBitmap(Base64String(item.favicon))
    }
    itemView.setOnClickListener { itemClickListener.onItemClick(item) }
    itemView.setOnLongClickListener { itemClickListener.onItemLongClick(item) }
  }
}
