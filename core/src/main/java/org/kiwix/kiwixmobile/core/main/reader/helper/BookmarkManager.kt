/*
 * Kiwix Android
 * Copyright (c) 2026 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.main.reader.helper

import androidx.annotation.StringRes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookmarks
import org.kiwix.kiwixmobile.core.main.MainRepositoryActions
import org.kiwix.kiwixmobile.core.page.bookmark.models.LibkiwixBookmarkItem
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.libkiwix.Book
import javax.inject.Inject

class BookmarkManager @Inject constructor(
  private val libkiwixBookmarks: LibkiwixBookmarks,
  private val zimReaderContainer: ZimReaderContainer,
  private val mainRepositoryActions: MainRepositoryActions
) {
  data class BookmarkState(val isBookmarked: Boolean = false)
  sealed interface BookmarkSaveResult {
    data object BookmarkAdded : BookmarkSaveResult
    data object BookmarkRemoved : BookmarkSaveResult

    // Can be improved with failure sealed class with proper error if the logic grow.
    data class Failure(@StringRes val messageId: Int) : BookmarkSaveResult
  }

  private var libkiwixBook: Book? = null
  private var bookmarkJob: Job? = null
  private val _bookmarkState = MutableStateFlow(BookmarkState())
  val bookmarkState: StateFlow<BookmarkState> = _bookmarkState.asStateFlow()

  fun observeBookmarks(
    scope: CoroutineScope,
    zimFileReaderId: String,
    webUrlsFlow: Flow<String?>
  ) {
    stopObserving()

    bookmarkJob = scope.launch {
      combine(
        flow = libkiwixBookmarks.bookmarkUrlsForCurrentBook(zimFileReaderId),
        flow2 = webUrlsFlow,
        transform = List<String?>::contains
      ).collect { isBookmarked ->
        _bookmarkState.value = BookmarkState(isBookmarked)
      }
    }
  }

  suspend fun addBookmark(
    pageTitle: String?,
    articleUrl: String?,
    isBookmarked: Boolean
  ): BookmarkSaveResult {
    val zimFileReader = zimReaderContainer.zimFileReader
    return runCatching {
      return when {
        pageTitle == null || articleUrl == null || zimFileReader == null -> {
          BookmarkSaveResult.Failure(string.unable_to_add_to_bookmarks)
        }

        isBookmarked -> {
          val libKiwixBook = getLibkiwixBook(zimFileReader)
          mainRepositoryActions.deleteBookmark(libKiwixBook.id, articleUrl)
          BookmarkSaveResult.BookmarkRemoved
        }

        else -> {
          val libKiwixBook = getLibkiwixBook(zimFileReader)
          mainRepositoryActions.saveBookmark(
            LibkiwixBookmarkItem(pageTitle, articleUrl, zimFileReader, libKiwixBook)
          )
          BookmarkSaveResult.BookmarkAdded
        }
      }
    }.getOrElse {
      // Catch the exception while saving the bookmarks for splitted zim files.
      // we have an issue with split zim files, see #3827
      BookmarkSaveResult.Failure(string.unable_to_add_to_bookmarks)
    }
  }

  /**
   * Returns the libkiwix book everytime when user saves or remove the bookmark.
   * the object will be created once to avoid creating it multiple times.
   */
  private fun getLibkiwixBook(zimFileReader: ZimFileReader): Book {
    libkiwixBook?.let { return it }
    val book = Book().apply {
      update(zimFileReader.jniKiwixReader)
    }
    libkiwixBook = book
    return book
  }

  fun stopObserving() {
    libkiwixBook = null
    bookmarkJob?.cancel()
  }
}
