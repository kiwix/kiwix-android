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

import android.view.View
import org.kiwix.kiwixmobile.core.base.adapter.BaseViewHolder
import org.kiwix.kiwixmobile.core.databinding.HeaderLanguageBinding
import org.kiwix.kiwixmobile.core.databinding.ItemBookBinding
import org.kiwix.kiwixmobile.core.downloader.model.Base64String
import org.kiwix.kiwixmobile.core.extensions.setBitmap
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
    private val itemBookBinding: ItemBookBinding,
    private val clickAction: ((BookOnDisk) -> Unit)?,
    private val longClickAction: ((BookOnDisk) -> Unit)?,
    private val multiSelectAction: ((BookOnDisk) -> Unit)?
  ) : BookOnDiskViewHolder<BookOnDisk>(itemBookBinding.root) {

    override fun bind(item: BookOnDisk) {
    }

    fun bind(
      item: BookOnDisk,
      selectionMode: SelectionMode
    ) {
      val book = item.book
      itemBookBinding.itemBookTitle.text = book.title
      itemBookBinding.itemBookDate.text = book.date
      itemBookBinding.itemBookDescription.text = book.description
      itemBookBinding.itemBookSize.text = KiloByte(book.size).humanReadable
      book.articleCount?.let {
        itemBookBinding.itemBookArticleCount.text =
          ArticleCount(it).toHumanReadable(containerView.context)
      }

      itemBookBinding.itemBookIcon.setBitmap(Base64String(book.favicon))

      itemBookBinding.tags.visibility = if (item.tags.isEmpty()) View.GONE else View.VISIBLE
      itemBookBinding.tags.render(item.tags)

      itemBookBinding.itemBookCheckbox.isChecked = item.isSelected
      when (selectionMode) {
        MULTI -> {
          itemBookBinding.itemBookCheckbox.visibility = View.VISIBLE
          itemBookBinding.itemBookClickableArea.setOnClickListener {
            multiSelectAction?.invoke(item)
          }
          itemBookBinding.itemBookClickableArea.setOnLongClickListener(null)
        }
        NORMAL -> {
          itemBookBinding.itemBookCheckbox.visibility = View.GONE
          itemBookBinding.itemBookClickableArea.setOnClickListener { clickAction?.invoke(item) }
          itemBookBinding.itemBookClickableArea.setOnLongClickListener {
            longClickAction?.invoke(item)
            return@setOnLongClickListener true
          }
        }
      }
    }
  }
}

class LanguageItemViewHolder(private val headerLanguageBinding: HeaderLanguageBinding) :
  BookOnDiskViewHolder<LanguageItem>(headerLanguageBinding.root) {

  override fun bind(item: LanguageItem) {
    headerLanguageBinding.headerLanguage.text = item.text
  }
}
