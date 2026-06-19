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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookmarks
import javax.inject.Inject

class BookmarkManager @Inject constructor(private val libkiwixBookmarks: LibkiwixBookmarks) {
  data class BookmarkState(val isBookmarked: Boolean = false)

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

  fun stopObserving() {
    bookmarkJob?.cancel()
  }
}
