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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import org.kiwix.kiwixmobile.core.main.KiwixWebView
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.ZERO
import javax.inject.Inject

class TabsManager @Inject constructor() {
  private val _webViewList = mutableStateListOf<KiwixWebView>()

  val webViewList: SnapshotStateList<KiwixWebView>
    get() = _webViewList

  var currentWebViewIndex by mutableIntStateOf(ZERO)
    private set

  /**
   * Add the webView in the current list.
   * @param webView: Webview for adding in the list.
   * @param selectTab: A boolean value, if false it will add the webView in background.
   */
  fun addWebView(webView: KiwixWebView, selectTab: Boolean = true) {
    webViewList.add(webView)
    if (selectTab) {
      currentWebViewIndex = webViewList.lastIndex
    }
  }

  fun getCurrentWebView(): KiwixWebView? = webViewList.getOrNull(currentWebViewIndex)

  fun setCurrentWebViewIndex(index: Int) {
    currentWebViewIndex = index
  }

  /**
   * Update the currentWebViewIndex.
   */
  fun selectTab(index: Int) {
    if (index !in webViewList.indices) return
    currentWebViewIndex = index
  }

  /**
   * Removed the tab for given index, and returns the KiwixWebView of that index.
   * So that caller can perform the restoreTab operation.
   */
  fun closeTab(index: Int): KiwixWebView? {
    if (index !in webViewList.indices) return null
    val removed = webViewList.removeAt(index)
    if (currentWebViewIndex >= webViewList.size) {
      currentWebViewIndex = maxOf(ZERO, webViewList.lastIndex)
    }
    return removed
  }

  /**
   * Removed the entire webView list, and return it list of removed webView.
   * So that caller can perform the restoreAllTabs operation.
   */
  fun closeAllTabs(): List<KiwixWebView> {
    val currentWebViewList = webViewList.toMutableList()
    clear()
    return currentWebViewList
  }

  fun restoreTab(kiwixWebView: KiwixWebView, index: Int) {
    _webViewList.add(index, kiwixWebView)
  }

  fun restoreTabs(webViewList: List<KiwixWebView>) {
    clear()
    _webViewList.addAll(webViewList)
  }

  fun clear() {
    _webViewList.clear()
  }

  fun isEmpty(): Boolean = webViewList.isEmpty()

  fun size(): Int = webViewList.size
}
