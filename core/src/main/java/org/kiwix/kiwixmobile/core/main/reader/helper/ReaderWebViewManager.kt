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
import org.kiwix.kiwixmobile.core.page.history.models.WebViewHistoryItem
import org.kiwix.kiwixmobile.core.utils.ZERO
import javax.inject.Inject

class ReaderWebViewManager @Inject constructor(
  private val tabsManager: TabsManager,
  private val webViewFactory: WebViewFactory
) {
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
    // TODO: Improve according to compose lifeCycle.
    // saveTabs()
    return webView
  }

  private fun loadUrl(url: String?, webview: KiwixWebView) {
    if (url != null && !url.endsWith("null")) {
      webview.loadUrl(url)
    }
  }

  /**
   * Returns the size of active tabs.
   */
  fun tabsSize(): Int = tabsManager.size()

  fun selectTab(index: Int) {
    tabsManager.selectTab(index)
  }

  fun clearAndGetWebViewList(): List<KiwixWebView> = tabsManager.clearAndGetWebViewList()

  fun restoreDeletedTabs(webViewList: List<KiwixWebView>) {
    tabsManager.webViewList.addAll(webViewList)
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
        tabsManager.restoreTabState(createWebView(), webViewHistoryItem)
      }
      selectTab(currentTab)
      RestoreTabsResult.TabsRestored
    }.getOrElse { RestoreTabsResult.ErrorInRestoringTabs(it) }

  fun getCurrentWebView(): KiwixWebView? = tabsManager.getCurrentWebView()

  fun setCurrentWebViewIndex(index: Int) {
    tabsManager.setCurrentWebViewIndex(index)
  }
}
