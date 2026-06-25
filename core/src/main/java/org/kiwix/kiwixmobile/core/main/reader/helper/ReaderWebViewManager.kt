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

import android.widget.FrameLayout
import androidx.compose.runtime.snapshots.SnapshotStateList
import org.kiwix.kiwixmobile.core.main.KiwixWebView
import org.kiwix.kiwixmobile.core.main.WebViewCallback
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderWebViewManager.WebViewNavigationHistoryResult.NoHistoryFound
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderWebViewManager.WebViewNavigationHistoryResult.HistoryFound
import org.kiwix.kiwixmobile.core.page.history.models.NavigationHistoryListItem
import org.kiwix.kiwixmobile.core.page.history.models.WebViewHistoryItem
import org.kiwix.kiwixmobile.core.utils.ZERO
import javax.inject.Inject

class ReaderWebViewManager @Inject constructor(
  private val tabsManager: TabsManager,
  private val readerSessionManager: ReaderSessionManager,
  private val webViewFactory: WebViewFactory
) {
  sealed interface WebViewNavigationHistoryResult {
    data class HistoryFound(
      val isForwardHistory: Boolean,
      val list: List<NavigationHistoryListItem>
    ) : WebViewNavigationHistoryResult

    data object NoHistoryFound : WebViewNavigationHistoryResult
  }

  sealed interface RestoreTabsResult {
    data object TabsRestored : RestoreTabsResult
    data class ErrorInRestoringTabs(val throwable: Throwable) : RestoreTabsResult
  }

  val webViewList: SnapshotStateList<KiwixWebView>
    get() = tabsManager.webViewList

  val currentWebViewIndex: Int = tabsManager.currentWebViewIndex

  /**
   * Initializes a new instance of `KiwixWebView` with the specified URL.
   *
   * @param url The URL to load in the web view. This is ignored if [shouldLoadUrl] is false.
   * @param selectTab A boolean value, when set to false it will load the webView in background.
   * @param callback A callback that attached to webView and provides the webView callbacks.
   * @param videoView A frameLayout, in which videos will play.
   * @param shouldLoadUrl A flag indicating whether to load the specified URL in the web view.
   *                      When restoring tabs, this should be set to false to avoid loading
   *                      an extra page, as the previous web view history will be restored directly.
   * @return The initialized [org.kiwix.kiwixmobile.core.main.KiwixWebView] instance.
   */
  fun createNewTab(
    url: String?,
    selectTab: Boolean = true,
    callback: WebViewCallback,
    videoView: FrameLayout,
    shouldLoadUrl: Boolean = true
  ): KiwixWebView {
    val webView = webViewFactory.create(callback, videoView)
    tabsManager.addWebView(webView)
    if (selectTab) {
      tabsManager.selectTab(tabsManager.size() - 1)
    }

    if (shouldLoadUrl) {
      loadUrl(url, webView)
    }
    return webView
  }

  private fun loadUrl(url: String?, webview: KiwixWebView) {
    if (url != null && !url.endsWith("null")) {
      webview.loadUrl(url)
    }
  }

  fun backToTop() {
    getCurrentWebView()?.pageUp(true)
  }

  fun goBack() {
    if (getCurrentWebView()?.canGoBack() == true) {
      getCurrentWebView()?.goBack()
    }
  }

  fun goForward() {
    if (getCurrentWebView()?.canGoForward() == true) {
      getCurrentWebView()?.goForward()
    }
  }

  fun getWebViewNavigationHistory(isForwardHistory: Boolean): WebViewNavigationHistoryResult {
    val webView = getCurrentWebView() ?: return NoHistoryFound

    if (isForwardHistory && !webView.canGoForward()) return NoHistoryFound
    if (!isForwardHistory && !webView.canGoBack()) return NoHistoryFound
    val historyList = webView.copyBackForwardList()

    val navigationItems = buildList {
      val indices = if (isForwardHistory) {
        historyList.currentIndex until historyList.size
      } else {
        historyList.currentIndex downTo ZERO
      }

      indices.forEach { index ->
        if (index != historyList.currentIndex) {
          historyList.getItemAtIndex(index)?.let {
            add(NavigationHistoryListItem(title = it.title, pageUrl = it.url))
          }
        }
      }
    }
    return if (navigationItems.isEmpty()) {
      NoHistoryFound
    } else {
      HistoryFound(isForwardHistory = isForwardHistory, list = navigationItems)
    }
  }

  /**
   * Returns the size of active tabs.
   */
  fun tabsSize(): Int = tabsManager.size()

  fun selectTab(index: Int) {
    tabsManager.selectTab(index)
  }

  fun closeTab(index: Int): KiwixWebView? = tabsManager.closeTab(index)

  fun closeAllTabs(): List<KiwixWebView> = tabsManager.closeAllTabs()

  fun restoreDeletedTab(kiwixWebView: KiwixWebView, index: Int) {
    tabsManager.restoreTab(kiwixWebView, index)
  }

  fun restoreDeletedTabs(webViewList: List<KiwixWebView>) {
    tabsManager.restoreTabs(webViewList)
  }

  suspend fun restoreTabs(
    webViewHistoryItemList: List<WebViewHistoryItem>,
    currentTab: Int,
    createWebView: () -> KiwixWebView
  ): RestoreTabsResult =
    runCatching {
      setCurrentWebViewIndex(ZERO)
      webViewList.removeFirstOrNull()
      webViewHistoryItemList.forEach { webViewHistoryItem ->
        readerSessionManager.restoreTabState(createWebView(), webViewHistoryItem)
      }
      selectTab(currentTab)
      RestoreTabsResult.TabsRestored
    }.getOrElse { RestoreTabsResult.ErrorInRestoringTabs(it) }

  fun getCurrentWebView(): KiwixWebView? = tabsManager.getCurrentWebView()

  fun setCurrentWebViewIndex(index: Int) {
    tabsManager.setCurrentWebViewIndex(index)
  }

  fun safelyGetWebView(position: Int, newMainPageTab: () -> KiwixWebView?): KiwixWebView? =
    if (webViewList.isEmpty()) newMainPageTab() else webViewList[safePosition(position)]

  private fun safePosition(position: Int): Int =
    when {
      position < 0 -> 0
      position >= webViewList.size -> webViewList.size - 1
      else -> position
    }

  fun destroyAllTabs() {
    runCatching {
      webViewList.apply {
        forEach { webView ->
          // Stop any ongoing loading of the WebView
          webView.stopLoading()
          // Clear the navigation history of the WebView
          webView.clearHistory()
          // Clear cached resources to prevent loading old content
          webView.clearCache(true)
          // Pause any ongoing activity in the WebView to prevent resource usage
          webView.onPause()
          // Break the reference chain from WebView → Fragment (via callback)
          // to prevent memory leaks through InputMethodManager/DecorView retention.
          webView.dispose()
          // Forcefully destroy the WebView before setting the new ZIM file
          // to ensure that it does not continue attempting to load internal links
          // from the previous ZIM file, which could cause errors.
          webView.destroy()
        }
        // Clear the WebView list after destroying the WebViews
        closeAllTabs()
      }
    }.onFailure {
      it.printStackTrace()
      // Clear the WebView list in case of an error
      closeAllTabs()
    }
  }
}
