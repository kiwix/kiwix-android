/*
 * Kiwix Android
 * Copyright (c) 2023 Kiwix <android.kiwix.org>
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
package org.kiwix.kiwixmobile.core.webserver

import android.util.Log
import io.reactivex.SingleObserver
import io.reactivex.disposables.Disposable
import org.kiwix.kiwixmobile.core.base.BasePresenter
import org.kiwix.kiwixmobile.core.data.DataSource
import org.kiwix.kiwixmobile.core.di.ActivityScope
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem
import org.kiwix.kiwixmobile.core.webserver.ZimHostContract.Presenter
import org.kiwix.kiwixmobile.core.webserver.ZimHostContract.View
import javax.inject.Inject

@ActivityScope
class ZimHostPresenter @Inject internal constructor(private val dataSource: DataSource) :
  BasePresenter<View>(),
  Presenter {

  override fun loadBooks(previouslyHostedBooks: Set<String>) {
    dataSource.getLanguageCategorizedBooks()
      .map { books ->
        books
          .filterIsInstance<BooksOnDiskListItem.BookOnDisk>()
          .forEach {
            it.isSelected =
              previouslyHostedBooks.contains(it.book.title) || previouslyHostedBooks.isEmpty()
          }
        books
      }.subscribe(object : SingleObserver<List<BooksOnDiskListItem>> {
        override fun onSubscribe(d: Disposable) {
          compositeDisposable.add(d)
        }

        override fun onSuccess(books: List<BooksOnDiskListItem>) {
          view?.addBooks(books)
        }

        override fun onError(e: Throwable) {
          Log.e(TAG, "Unable to load books", e)
        }
      })
  }

  companion object {
    private const val TAG = "ZimHostPresenter"
  }
}
