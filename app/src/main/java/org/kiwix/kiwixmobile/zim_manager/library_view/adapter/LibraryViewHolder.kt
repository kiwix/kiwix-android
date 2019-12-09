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

package org.kiwix.kiwixmobile.zim_manager.library_view.adapter

import android.view.Gravity
import android.view.View
import android.view.View.MeasureSpec
import android.widget.Toast
import androidx.annotation.StringRes
import kotlinx.android.synthetic.main.item_library.creator
import kotlinx.android.synthetic.main.item_library.date
import kotlinx.android.synthetic.main.item_library.description
import kotlinx.android.synthetic.main.item_library.favicon
import kotlinx.android.synthetic.main.item_library.fileName
import kotlinx.android.synthetic.main.item_library.language
import kotlinx.android.synthetic.main.item_library.publisher
import kotlinx.android.synthetic.main.item_library.size
import kotlinx.android.synthetic.main.item_library.title
import kotlinx.android.synthetic.main.item_library.unableToDownload
import kotlinx.android.synthetic.main.library_divider.divider_text
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.base.adapter.BaseViewHolder
import org.kiwix.kiwixmobile.core.downloader.model.Base64String
import org.kiwix.kiwixmobile.core.extensions.setBitmap
import org.kiwix.kiwixmobile.core.extensions.setTextAndVisibility
import org.kiwix.kiwixmobile.core.utils.BookUtils
import org.kiwix.kiwixmobile.core.utils.NetworkUtils
import org.kiwix.kiwixmobile.core.zim_manager.KiloByte
import org.kiwix.kiwixmobile.zim_manager.Fat32Checker.FileSystemState.CannotWrite4GbFile
import org.kiwix.kiwixmobile.zim_manager.Fat32Checker.FileSystemState.Unknown
import org.kiwix.kiwixmobile.zim_manager.library_view.adapter.LibraryListItem.BookItem
import org.kiwix.kiwixmobile.zim_manager.library_view.adapter.LibraryListItem.DividerItem

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
      fileName.text = NetworkUtils.parseURL(CoreApp.getInstance(), item.book.url)
      favicon.setBitmap(Base64String(item.book.favicon))

      containerView.setOnClickListener { clickAction.invoke(item) }
      containerView.isClickable = item.canBeDownloaded

      unableToDownload.visibility = if (item.canBeDownloaded) View.GONE else View.VISIBLE
      unableToDownload.setOnLongClickListener {
        it.centreToast(
          when (item.fileSystemState) {
            CannotWrite4GbFile -> R.string.file_system_does_not_support_4gb
            Unknown -> R.string.detecting_file_system
            else -> throw RuntimeException("impossible invalid state: ${item.fileSystemState}")
          }
        )
        true
      }
    }
  }

  class LibraryDividerViewHolder(view: View) : LibraryViewHolder<DividerItem>(view) {
    override fun bind(item: DividerItem) {
      divider_text.text = item.text
    }
  }
}

private fun View.centreToast(@StringRes id: Int) {
  val locationXAndY = intArrayOf(0, 0)
  getLocationOnScreen(locationXAndY)
  val midX = locationXAndY[0] + width / 2
  val midY = locationXAndY[1] + height / 2
  Toast.makeText(context, id, Toast.LENGTH_LONG).apply {
    view.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
    setGravity(
      Gravity.TOP or Gravity.START,
      midX - view.measuredWidth / 2,
      midY - view.measuredHeight
    )
  }.show()
}
