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

package org.kiwix.kiwixmobile.core.main.reader

import android.webkit.WebView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.kiwix.kiwixmobile.core.ui.components.ONE
import org.kiwix.kiwixmobile.core.utils.ZERO
import javax.inject.Inject

class FindInPageManager @Inject constructor() {
  data class FindInPageUiState(
    val visible: Boolean = false,
    val query: String = "",
    val currentMatch: Int = ZERO,
    val totalMatches: Int = ZERO
  ) {
    val resultText: String
      get() = when {
        query.isEmpty() -> ""
        totalMatches == 0 -> "0/0"
        else -> "$currentMatch/$totalMatches"
      }
  }

  private var webView: WebView? = null
  private val _uiState = MutableStateFlow(FindInPageUiState())
  val uiState = _uiState.asStateFlow()

  private fun requireWebView(errorMessage: String) = requireNotNull(webView) {
    errorMessage
  }

  private var findListener: WebView.FindListener? =
    WebView.FindListener { activeMatchOrdinal, numberOfMatches, _ ->
      _uiState.update {
        it.copy(
          currentMatch = activeMatchOrdinal + ONE,
          totalMatches = numberOfMatches
        )
      }
    }

  fun setWebView(webView: WebView) {
    this.webView = webView
    requireWebView("WebView supplied to FindInPageManager cannot be null")
      .setFindListener(findListener)
  }

  fun search(text: String) {
    _uiState.update {
      it.copy(query = text)
    }

    if (text.isEmpty()) {
      webView?.clearMatches()
      return
    }

    requireWebView("No WebView for FindInPageManager::search")
      .findAllAsync(text)
  }

  fun findNext() {
    requireWebView("No WebView for FindInPageManager::findNext")
      .findNext(true)
  }

  fun findPrevious() {
    requireWebView("No WebView for FindInPageManager::findPrevious")
      .findNext(false)
  }

  fun stop() {
    webView?.clearMatches()
    findListener = null
  }
}
