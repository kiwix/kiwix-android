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

import androidx.core.net.toUri
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.kiwix.kiwixmobile.core.di.MainDispatcher
import org.kiwix.kiwixmobile.core.main.KiwixWebView
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderWebViewManager.WebViewNavigationHistoryResult.HistoryFound
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderWebViewManager.WebViewNavigationHistoryResult.NoHistoryFound
import org.kiwix.kiwixmobile.core.main.reader.helper.TabsManager.TabsState
import org.kiwix.kiwixmobile.core.page.history.models.NavigationHistoryListItem
import org.kiwix.kiwixmobile.core.page.history.models.WebViewHistoryItem
import org.kiwix.kiwixmobile.core.reader.ZimFileReader.Companion.CONTENT_PREFIX
import org.kiwix.kiwixmobile.core.utils.ZERO
import javax.inject.Inject

class ReaderWebViewManager @Inject constructor(
  private val tabsManager: TabsManager,
  private val readerSessionManager: ReaderSessionManager,
  private val webViewFactory: WebViewFactory,
  @param:MainDispatcher private val mainDispatcher: MainCoroutineDispatcher
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

  val tabsState: StateFlow<TabsManager.TabsState>
    get() = tabsManager.tabState

  private fun currentTabsState() = tabsState.value

  fun webViewList() = currentTabsState().webViews

  val currentWebViewIndex: Int get() = currentTabsState().selectedIndex

  suspend fun createNewTab(tabConfig: TabsManager.NewTabConfig): KiwixWebView {
    return withContext(mainDispatcher) {
      val webView = webViewFactory.create(tabConfig.callback, tabConfig.videoView)
      if (tabConfig.shouldLoadUrl) {
        loadUrl(tabConfig.url, webView)
      }
      setUpWithTextToSpeech(webView, tabConfig.readAloudManager)
      tabConfig.documentParser?.initInterface(webView)
      return@withContext webView
    }
  }

  suspend fun addNewTabInTabsManager(webView: KiwixWebView, tabConfig: TabsManager.NewTabConfig) {
    tabsManager.addWebView(webView, tabConfig.selectTab)
    if (tabConfig.selectTab) {
      tabConfig.selectTabCallback(currentWebViewIndex)
    }
  }

  suspend fun setUpWithTextToSpeech(
    kiwixWebView: KiwixWebView?,
    readAloudManager: ReadAloudManager
  ) {
    kiwixWebView?.let {
      withContext(mainDispatcher) {
        readAloudManager.initWebView(it)
      }
    }
  }

  suspend fun openPage(pageUrl: String?, kiwixWebView: KiwixWebView) {
    pageUrl?.let {
      loadUrlWithCurrentWebview(redirectOrOriginal(contentUrl(it)), kiwixWebView)
    }
  }

  suspend fun loadUrlWithCurrentWebview(url: String?, currentWebView: KiwixWebView) {
    loadUrl(url, currentWebView)
  }

  fun contentUrl(pageUrl: String?): String =
    "${CONTENT_PREFIX}$pageUrl".toUri().toString()

  private fun redirectOrOriginal(contentUrl: String): String {
    val zimReaderContainer = readerSessionManager.zimReaderContainer
    return if (zimReaderContainer.isRedirect(contentUrl)) {
      zimReaderContainer.getRedirect(contentUrl)
    } else {
      contentUrl
    }
  }

  suspend fun loadUrl(url: String?, webview: KiwixWebView) {
    if (url != null && !url.endsWith("null")) {
      withContext(mainDispatcher) {
        webview.loadUrl(url)
      }
    }
  }

  @Suppress("ReturnCount")
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

  fun closeTab(index: Int): KiwixWebView? = tabsManager.closeTab(index)

  fun closeAllTabs(): TabsManager.TabsState = tabsManager.closeAllTabs()

  fun restoreDeletedTab(kiwixWebView: KiwixWebView, index: Int) {
    tabsManager.restoreTab(kiwixWebView, index)
  }

  fun restoreDeletedTabs(tabsState: TabsState) {
    tabsManager.restoreTabs(tabsState)
  }

  suspend fun restoreTabs(
    webViewHistoryItemList: List<WebViewHistoryItem>,
    currentTab: Int,
    newTabConfig: TabsManager.NewTabConfig
  ): RestoreTabsResult =
    runCatching {
      withContext(mainDispatcher) {
        setCurrentWebViewIndex(ZERO)
        tabsManager.clearTabsState()
        webViewHistoryItemList.forEach { webViewHistoryItem ->
          val webView = createNewTab(newTabConfig)
          readerSessionManager.restoreTabState(webView, webViewHistoryItem)
          addNewTabInTabsManager(webView, newTabConfig)
        }
        setCurrentWebViewIndex(currentTab)
        RestoreTabsResult.TabsRestored
      }
    }.getOrElse { RestoreTabsResult.ErrorInRestoringTabs(it) }

  fun getCurrentWebView(): KiwixWebView? = tabsManager.getCurrentWebView()

  suspend fun newMainPageTab(newTabConfig: TabsManager.NewTabConfig): KiwixWebView {
    val mainPageUrl = redirectOrOriginal(contentUrl(readerSessionManager.zimReaderContainer.mainPage))
    val newConfig = newTabConfig.copy(url = mainPageUrl)
    return createNewTab(newConfig).also {
      addNewTabInTabsManager(it, newConfig)
    }
  }

  fun setCurrentWebViewIndex(index: Int) {
    tabsManager.setCurrentWebViewIndex(index)
  }

  suspend fun destroyAllTabs() {
    runCatching {
      withContext(mainDispatcher.immediate) {
        webViewList().apply {
          forEach { webView ->
            // Stop any ongoing loading of the WebView
            webView.stopLoading()
            // Clear the navigation history of the WebView
            webView.clearHistory()
            // Clear cached resources to prevent loading old content
            webView.clearCache(true)
            // Pause any ongoing activity in the WebView to prevent resource usage
            webView.onPause()
            // Break the reference chain from WebView → Callback
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
      }
    }.onFailure {
      it.printStackTrace()
      // Clear the WebView list in case of an error
      closeAllTabs()
    }
  }
}
