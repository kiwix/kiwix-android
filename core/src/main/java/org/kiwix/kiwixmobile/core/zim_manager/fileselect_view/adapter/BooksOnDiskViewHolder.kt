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

package org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter

import android.graphics.ColorMatrixColorFilter
import android.view.View
import kotlinx.android.synthetic.main.header_language.header_language
import kotlinx.android.synthetic.main.item_book.itemBookCheckbox
import kotlinx.android.synthetic.main.item_book.item_book_article_count
import kotlinx.android.synthetic.main.item_book.item_book_clickable_area
import kotlinx.android.synthetic.main.item_book.item_book_date
import kotlinx.android.synthetic.main.item_book.item_book_description
import kotlinx.android.synthetic.main.item_book.item_book_icon
import kotlinx.android.synthetic.main.item_book.item_book_label_picture
import kotlinx.android.synthetic.main.item_book.item_book_label_video
import kotlinx.android.synthetic.main.item_book.item_book_size
import kotlinx.android.synthetic.main.item_book.item_book_title
import org.kiwix.kiwixmobile.core.base.adapter.BaseViewHolder
import org.kiwix.kiwixmobile.core.downloader.model.Base64String
import org.kiwix.kiwixmobile.core.extensions.setBitmap
import org.kiwix.kiwixmobile.core.main.KiwixWebView
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.zim_manager.KiloByte
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.ArticleCount
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.SelectionMode
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.SelectionMode.MULTI
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.SelectionMode.NORMAL
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.BookOnDisk
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.LanguageItem

sealed class BookOnDiskViewHolder<in T : BooksOnDiskListItem>(containerView: View) :
  BaseViewHolder<T>(containerView) {

  class BookViewHolder(
    containerView: View,
    private val sharedPreferenceUtil: SharedPreferenceUtil,
    private val clickAction: ((BookOnDisk) -> Unit)?,
    private val longClickAction: ((BookOnDisk) -> Unit)?,
    private val multiSelectAction: ((BookOnDisk) -> Unit)?
  ) : BookOnDiskViewHolder<BookOnDisk>(containerView) {

    override fun bind(item: BookOnDisk) {
    }

    fun bind(
      item: BookOnDisk,
      selectionMode: SelectionMode
    ) {
      val book = item.book
      item_book_title.text = book.getTitle()
      item_book_date.text = book.getDate()
      item_book_description.text = book.getDescription()
      item_book_size.text = (KiloByte(book.size).humanReadable)
      book.articleCount?.let {
        item_book_article_count.text =
          ArticleCount(it).toHumanReadable(containerView.context)
      }

      item_book_icon.setBitmap(Base64String(book.favicon))

      if (sharedPreferenceUtil.nightMode()) {
        item_book_icon.drawable
          ?.mutate()
          ?.colorFilter = ColorMatrixColorFilter(KiwixWebView.NIGHT_MODE_COLORS)
      }

      val path = item.file.path
      if (path.contains("nopic")) {
        item_book_label_picture.visibility = View.GONE
        item_book_label_video.visibility = View.GONE
      }
      if (path.contains("novid")) {
        item_book_label_video.visibility = View.GONE
      }

      itemBookCheckbox.isChecked = item.isSelected
      when (selectionMode) {
        MULTI -> {
          itemBookCheckbox.visibility = View.VISIBLE
          item_book_clickable_area.setOnClickListener { multiSelectAction?.invoke(item) }
          item_book_clickable_area.setOnLongClickListener(null)
        }
        NORMAL -> {
          itemBookCheckbox.visibility = View.GONE
          item_book_clickable_area.setOnClickListener { clickAction?.invoke(item) }
          item_book_clickable_area.setOnLongClickListener {
            longClickAction?.invoke(item)
            return@setOnLongClickListener true
          }
        }
      }
    }
  }
}

class LanguageItemViewHolder(containerView: View) :
  BookOnDiskViewHolder<LanguageItem>(containerView) {

  override fun bind(item: LanguageItem) {
    header_language.text = item.text
  }
}
