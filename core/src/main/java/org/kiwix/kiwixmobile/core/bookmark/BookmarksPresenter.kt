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
package org.kiwix.kiwixmobile.core.bookmark

import android.util.Log
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import org.kiwix.kiwixmobile.core.base.BasePresenter
import org.kiwix.kiwixmobile.core.data.DataSource
import org.kiwix.kiwixmobile.core.di.qualifiers.Computation
import org.kiwix.kiwixmobile.core.di.qualifiers.MainThread
import javax.inject.Inject

class BookmarksPresenter @Inject constructor(
  private val dataSource: DataSource,
  @param:MainThread private val mainThread: Scheduler,
  @param:Computation private val computation: Scheduler
) : BasePresenter<BookmarksContract.View>(),
  BookmarksContract.Presenter {
  private var disposable: Disposable? = null
  override fun loadBookmarks(showBookmarksCurrentBook: Boolean) {
    disposable?.takeIf { !it.isDisposed }?.dispose()
    dataSource.getBookmarks(showBookmarksCurrentBook)
      .subscribe(
        { view?.updateBookmarksList(it) },
        { Log.e("BookmarkPresenter", "Failed to load bookmarks", it) }
      ).let {
        compositeDisposable.add(it)
        disposable = it
      }
  }

  override fun filterBookmarks(bookmarksList: List<BookmarkItem>, newText: String) {
    compositeDisposable.add(Observable.fromCallable {
      bookmarksList.filter {
        it.bookmarkTitle.contains(newText, true)
      }
    }
      .subscribeOn(computation)
      .observeOn(mainThread)
      .subscribe(
        { view?.notifyBookmarksListFiltered(it) },
        { Log.e("BookmarkPresenter", "Failed to filter bookmark.", it) }
      )
    )
  }

  override fun deleteBookmarks(deleteList: List<BookmarkItem>) {
    dataSource.deleteBookmarks(deleteList)
      .subscribe({}, { Log.e("BookmarkPresenter", "Failed to delete bookmark", it) }
      )
  }
}
