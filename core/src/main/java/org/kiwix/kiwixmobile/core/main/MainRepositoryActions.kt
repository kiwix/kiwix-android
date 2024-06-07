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

import org.kiwix.kiwixmobile.core.utils.files.Log
import io.reactivex.disposables.Disposable
import org.kiwix.kiwixmobile.core.data.DataSource
import org.kiwix.kiwixmobile.core.di.ActivityScope
import org.kiwix.kiwixmobile.core.page.bookmark.adapter.LibkiwixBookmarkItem
import org.kiwix.kiwixmobile.core.page.history.adapter.HistoryListItem.HistoryItem
import org.kiwix.kiwixmobile.core.page.notes.adapter.NoteListItem
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.BookOnDisk
import javax.inject.Inject

private const val TAG = "MainPresenter"

@ActivityScope
class MainRepositoryActions @Inject constructor(private val dataSource: DataSource) {
  private var saveHistoryDisposable: Disposable? = null
  private var saveBookmarkDisposable: Disposable? = null
  private var saveNoteDisposable: Disposable? = null
  private var saveBookDisposable: Disposable? = null
  private var deleteNoteDisposable: Disposable? = null

  fun saveHistory(history: HistoryItem) {
    saveHistoryDisposable = dataSource.saveHistory(history)
      .subscribe({}, { e -> Log.e(TAG, "Unable to save history", e) })
  }

  fun saveBookmark(bookmark: LibkiwixBookmarkItem) {
    saveBookmarkDisposable = dataSource.saveBookmark(bookmark)
      .subscribe({}, { e -> Log.e(TAG, "Unable to save bookmark", e) })
  }

  fun deleteBookmark(bookId: String, bookmarkUrl: String) {
    dataSource.deleteBookmark(bookId, bookmarkUrl)
      ?.subscribe({}, { e -> Log.e(TAG, "Unable to delete bookmark", e) })
      ?: Log.e(TAG, "Unable to delete bookmark")
  }

  fun saveNote(note: NoteListItem) {
    saveNoteDisposable = dataSource.saveNote(note)
      .subscribe({}, { e -> Log.e(TAG, "Unable to save note", e) })
  }

  fun deleteNote(noteTitle: String) {
    deleteNoteDisposable = dataSource.deleteNote(noteTitle)
      .subscribe({}, { e -> Log.e(TAG, "Unable to delete note", e) })
  }

  fun saveBook(book: BookOnDisk) {
    saveBookDisposable = dataSource.saveBook(book)
      .subscribe({}, { e -> Log.e(TAG, "Unable to save book", e) })
  }

  fun dispose() {
    saveHistoryDisposable?.dispose()
    saveBookmarkDisposable?.dispose()
    saveNoteDisposable?.dispose()
    deleteNoteDisposable?.dispose()
    saveBookDisposable?.dispose()
  }
}
