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
package org.kiwix.kiwixmobile.core.main

import android.util.Log
import org.kiwix.kiwixmobile.core.base.BasePresenter
import org.kiwix.kiwixmobile.core.bookmark.BookmarkItem
import org.kiwix.kiwixmobile.core.data.DataSource
import org.kiwix.kiwixmobile.core.di.ActivityScope
import org.kiwix.kiwixmobile.core.history.HistoryListItem.HistoryItem
import org.kiwix.kiwixmobile.core.main.MainContract.Presenter
import org.kiwix.kiwixmobile.core.main.MainContract.View
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.BookOnDisk
import javax.inject.Inject

private const val TAG = "MainPresenter"

@ActivityScope
internal class MainPresenter @Inject constructor(private val dataSource: DataSource) :
  BasePresenter<View?>(), Presenter {
  override fun loadBooks() {
    compositeDisposable.add(
      dataSource.languageCategorizedBooks.subscribe(
        { view?.addBooks(it) }
      ) { e -> Log.e(TAG, "Unable to load books", e) })
  }

  override fun saveBooks(book: List<BookOnDisk>) {
    dataSource.saveBooks(book)
      .subscribe(::loadBooks) { e -> Log.e(TAG, "Unable to save books", e) }
  }

  override fun saveHistory(history: HistoryItem) {
    dataSource.saveHistory(history)
      .subscribe({}, { e -> Log.e(TAG, "Unable to save history", e) })
  }

  override fun saveBookmark(bookmark: BookmarkItem) {
    dataSource.saveBookmark(bookmark)
      .subscribe({}, { e -> Log.e(TAG, "Unable to save bookmark", e) })
  }

  override fun deleteBookmark(bookmarkUrl: String) {
    dataSource.deleteBookmark(bookmarkUrl)
      .subscribe({}, { e -> Log.e(TAG, "Unable to delete bookmark", e) })
  }
}
