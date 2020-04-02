/*
 * Kiwix Android
 * Copyright (c) 2020 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.zim_manager.fileselect_view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.sheet_book_info.detail_article_count
import kotlinx.android.synthetic.main.sheet_book_info.detail_creator
import kotlinx.android.synthetic.main.sheet_book_info.detail_date
import kotlinx.android.synthetic.main.sheet_book_info.detail_file
import kotlinx.android.synthetic.main.sheet_book_info.detail_language
import kotlinx.android.synthetic.main.sheet_book_info.detail_media_count
import kotlinx.android.synthetic.main.sheet_book_info.detail_name
import kotlinx.android.synthetic.main.sheet_book_info.detail_publisher
import kotlinx.android.synthetic.main.sheet_book_info.detail_size
import kotlinx.android.synthetic.main.sheet_book_info.detail_tags
import kotlinx.android.synthetic.main.sheet_book_info.detail_url
import kotlinx.android.synthetic.main.sheet_book_info.info_sheet_description
import kotlinx.android.synthetic.main.sheet_book_info.info_sheet_favicon
import kotlinx.android.synthetic.main.sheet_book_info.info_sheet_title
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.downloader.model.Base64String
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity.Book
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.viewModel
import org.kiwix.kiwixmobile.core.extensions.setBitmap
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.BookOnDisk
import org.kiwix.kiwixmobile.zim_manager.ZimManageActivity
import org.kiwix.kiwixmobile.zim_manager.ZimManageViewModel
import javax.inject.Inject

class BookInfoBottomSheetDialog : BottomSheetDialogFragment() {
  @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

  private val zimManageViewModel by lazy {
    activity!!.viewModel<ZimManageViewModel>(viewModelFactory)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View = inflater.inflate(R.layout.sheet_book_info, container, false)

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    (activity as ZimManageActivity).cachedComponent.inject(this)
    zimManageViewModel.selectedInfoItem.observe(viewLifecycleOwner, Observer(::render))
  }

  private fun render(bookOnDisk: BookOnDisk?) {
    bookOnDisk?.let {
      render(it.book)
      detail_file.setDetail(it.file.absolutePath)
    }
  }

  private fun render(book: Book) {
    info_sheet_title.text = book.title
    info_sheet_favicon.setBitmap(Base64String(book.favicon))
    info_sheet_description.text = book.description
    detail_language.setDetail(book.language)
    detail_creator.setDetail(book.creator)
    detail_publisher.setDetail(book.publisher)
    detail_date.setDetail(book.date)
    detail_url.setDetail(book.url)
    detail_article_count.setDetail(book.articleCount)
    detail_media_count.setDetail(book.mediaCount)
    detail_size.setDetail(book.size)
    detail_name.setDetail(book.name)
    detail_tags.setDetail(book.tags)
  }
}
