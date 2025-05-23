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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.kiwix.kiwixmobile.core.dao.entities.WebViewHistoryEntity
import org.kiwix.kiwixmobile.core.data.DataSource
import org.kiwix.kiwixmobile.core.di.ActivityScope
import org.kiwix.kiwixmobile.core.page.bookmark.adapter.LibkiwixBookmarkItem
import org.kiwix.kiwixmobile.core.page.history.adapter.HistoryListItem.HistoryItem
import org.kiwix.kiwixmobile.core.page.history.adapter.WebViewHistoryItem
import org.kiwix.kiwixmobile.core.page.notes.adapter.NoteListItem
import org.kiwix.kiwixmobile.core.utils.files.Log
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem.BookOnDisk
import javax.inject.Inject

private const val TAG = "MainPresenter"

@ActivityScope
class MainRepositoryActions @Inject constructor(private val dataSource: DataSource) {
  @Suppress("InjectDispatcher")
  suspend fun saveHistory(history: HistoryItem) {
    runCatching {
      withContext(Dispatchers.IO) {
        dataSource.saveHistory(history)
      }
    }.onFailure {
      Log.e(TAG, "Unable to save history", it)
    }
  }

  @Suppress("InjectDispatcher")
  suspend fun saveBookmark(bookmark: LibkiwixBookmarkItem) {
    runCatching {
      withContext(Dispatchers.IO) {
        dataSource.saveBookmark(bookmark)
      }
    }.onFailure {
      Log.e(TAG, "Unable to save bookmark", it)
    }
  }

  @Suppress("InjectDispatcher")
  suspend fun deleteBookmark(bookId: String, bookmarkUrl: String) {
    runCatching {
      withContext(Dispatchers.IO) {
        dataSource.deleteBookmark(bookId, bookmarkUrl)
      }
    }.onFailure {
      Log.e(TAG, "Unable to delete bookmark", it)
    }
  }

  suspend fun saveNote(note: NoteListItem) {
    runCatching {
      dataSource.saveNote(note)
    }.onFailure {
      Log.e(TAG, "Unable to save note", it)
    }
  }

  suspend fun deleteNote(noteTitle: String) {
    runCatching {
      dataSource.deleteNote(noteTitle)
    }.onFailure {
      Log.e(TAG, "Unable to delete note", it)
    }
  }

  suspend fun saveBook(book: BookOnDisk) {
    runCatching {
      dataSource.saveBook(book)
    }.onFailure {
      Log.e(TAG, "Unable to save book", it)
    }
  }

  suspend fun saveWebViewPageHistory(webViewHistoryEntityList: List<WebViewHistoryEntity>) {
    dataSource.insertWebViewPageHistoryItems(webViewHistoryEntityList)
  }

  suspend fun clearWebViewPageHistory() {
    dataSource.clearWebViewPagesHistory()
  }

  suspend fun loadWebViewPagesHistory(): List<WebViewHistoryItem> =
    dataSource.getAllWebViewPagesHistory()
      .first()
      .map(::WebViewHistoryItem)
}
