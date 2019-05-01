package org.kiwix.kiwixmobile.zim_manager.fileselect_view

import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.TextView
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.library_item.creator
import kotlinx.android.synthetic.main.library_item.date
import kotlinx.android.synthetic.main.library_item.description
import kotlinx.android.synthetic.main.library_item.favicon
import kotlinx.android.synthetic.main.library_item.fileName
import kotlinx.android.synthetic.main.library_item.language
import kotlinx.android.synthetic.main.library_item.publisher
import kotlinx.android.synthetic.main.library_item.size
import kotlinx.android.synthetic.main.library_item.title
import org.kiwix.kiwixmobile.KiwixApplication
import org.kiwix.kiwixmobile.downloader.model.Base64String
import org.kiwix.kiwixmobile.extensions.setBitmap
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book
import org.kiwix.kiwixmobile.utils.BookUtils
import org.kiwix.kiwixmobile.utils.NetworkUtils

class RescanViewHolder(
  override val containerView: View,
  val bookUtils: BookUtils
) : ViewHolder(containerView),
    LayoutContainer {
  fun bind(
    book: Book,
    clickAction: (Book) -> Unit,
    longClickAction: (Book) -> Unit
  ) {
    title.setTextAndVisibility(book.title)
    description.setTextAndVisibility(book.description)
    creator.setTextAndVisibility(book.creator)
    publisher.setTextAndVisibility(book.publisher)
    date.setTextAndVisibility(book.date)
    size.setTextAndVisibility(book.size)
    language.text = bookUtils.getLanguage(book.getLanguage())
    fileName.text = NetworkUtils.parseURL(
        KiwixApplication.getInstance(), book.file.path
    )
    favicon.setBitmap(Base64String(book.favicon))

    containerView.setOnClickListener {
      clickAction.invoke(book)
    }
    containerView.setOnLongClickListener {
      longClickAction.invoke(book)
      return@setOnLongClickListener true
    }
  }

  private fun TextView.setTextAndVisibility(title: String?) =
    if (title != null && title.isNotEmpty()) {
      text = title
      visibility = VISIBLE
    } else {
      visibility = GONE
    }

}
