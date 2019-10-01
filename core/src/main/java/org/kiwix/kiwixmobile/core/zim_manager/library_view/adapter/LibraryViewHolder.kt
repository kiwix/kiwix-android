/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.kiwix.kiwixmobile.core.zim_manager.library_view.adapter

import android.view.View
import kotlinx.android.synthetic.main.library_divider.divider_text
import kotlinx.android.synthetic.main.library_item.creator
import kotlinx.android.synthetic.main.library_item.date
import kotlinx.android.synthetic.main.library_item.description
import kotlinx.android.synthetic.main.library_item.favicon
import kotlinx.android.synthetic.main.library_item.fileName
import kotlinx.android.synthetic.main.library_item.language
import kotlinx.android.synthetic.main.library_item.publisher
import kotlinx.android.synthetic.main.library_item.size
import kotlinx.android.synthetic.main.library_item.title
import org.kiwix.kiwixmobile.core.KiwixApplication
import org.kiwix.kiwixmobile.core.downloader.model.Base64String
import org.kiwix.kiwixmobile.core.extensions.setBitmap
import org.kiwix.kiwixmobile.core.extensions.setTextAndVisibility
import org.kiwix.kiwixmobile.core.utils.BookUtils
import org.kiwix.kiwixmobile.core.utils.NetworkUtils
import org.kiwix.kiwixmobile.core.zim_manager.KiloByte
import org.kiwix.kiwixmobile.core.zim_manager.library_view.adapter.LibraryListItem.BookItem
import org.kiwix.kiwixmobile.core.zim_manager.library_view.adapter.LibraryListItem.DividerItem
import org.kiwix.kiwixmobile.core.zim_manager.library_view.adapter.base.BaseViewHolder

sealed class LibraryViewHolder<in T : LibraryListItem>(containerView: View) :
  BaseViewHolder<T>(containerView) {

  class LibraryBookViewHolder(
    view: View,
    private val bookUtils: BookUtils,
    private val clickAction: (BookItem) -> Unit
  ) : LibraryViewHolder<BookItem>(view) {
    override fun bind(item: BookItem) {
      title.setTextAndVisibility(item.book.title)
      description.setTextAndVisibility(item.book.description)
      creator.setTextAndVisibility(item.book.creator)
      publisher.setTextAndVisibility(item.book.publisher)
      date.setTextAndVisibility(item.book.date)
      size.setTextAndVisibility(KiloByte(item.book.size).humanReadable)
      language.text = bookUtils.getLanguage(item.book.getLanguage())
      fileName.text = NetworkUtils.parseURL(
        KiwixApplication.getInstance(), item.book.url
      )
      favicon.setBitmap(Base64String(item.book.favicon))

      containerView.setOnClickListener {
        clickAction.invoke(item)
      }
    }
  }

  class LibraryDividerViewHolder(view: View) : LibraryViewHolder<DividerItem>(view) {
    override fun bind(item: DividerItem) {
      divider_text.text = item.text
    }
  }
}
