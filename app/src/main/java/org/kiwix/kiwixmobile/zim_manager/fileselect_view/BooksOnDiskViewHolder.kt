package org.kiwix.kiwixmobile.zim_manager.fileselect_view

import android.view.View
import androidx.recyclerview.widget.RecyclerView.ViewHolder
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
import org.kiwix.kiwixmobile.downloader.model.BookOnDisk
import org.kiwix.kiwixmobile.extensions.setBitmap
import org.kiwix.kiwixmobile.extensions.setTextAndVisibility
import org.kiwix.kiwixmobile.utils.BookUtils
import org.kiwix.kiwixmobile.utils.NetworkUtils
import org.kiwix.kiwixmobile.zim_manager.library_view.adapter.KiloByte

class BooksOnDiskViewHolder(
  override val containerView: View,
  private val bookUtils: BookUtils
) : ViewHolder(containerView),
    LayoutContainer {
  fun bind(
    bookOnDisk: BookOnDisk,
    clickAction: (BookOnDisk) -> Unit,
    longClickAction: (BookOnDisk) -> Unit
  ) {
    val book = bookOnDisk.book
    title.setTextAndVisibility(book.title)
    description.setTextAndVisibility(book.description)
    creator.setTextAndVisibility(book.creator)
    publisher.setTextAndVisibility(book.publisher)
    date.setTextAndVisibility(book.date)
    size.setTextAndVisibility(KiloByte(book.size).humanReadable)
    language.text = bookUtils.getLanguage(book.getLanguage())
    fileName.text = NetworkUtils.parseURL(
        KiwixApplication.getInstance(), book.url ?: bookOnDisk.file.path
    )
    favicon.setBitmap(Base64String(book.favicon))

    containerView.setOnClickListener {
      clickAction.invoke(bookOnDisk)
    }
    containerView.setOnLongClickListener {
      longClickAction.invoke(bookOnDisk)
      return@setOnLongClickListener true
    }
  }
}


