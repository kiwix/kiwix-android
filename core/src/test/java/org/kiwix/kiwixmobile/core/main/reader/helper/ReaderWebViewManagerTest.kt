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

import android.webkit.WebBackForwardList
import android.webkit.WebHistoryItem
import android.widget.FrameLayout
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.kiwix.kiwixmobile.core.main.KiwixWebView
import org.kiwix.kiwixmobile.core.main.WebViewCallback
import org.kiwix.kiwixmobile.core.page.history.models.NavigationHistoryListItem
import org.kiwix.kiwixmobile.core.page.history.models.WebViewHistoryItem
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.sharedFunctions.MainDispatcherRule

class ReaderWebViewManagerTest {
  @RegisterExtension
  @JvmField
  val mainDispatcherRule = MainDispatcherRule()
  private lateinit var readerWebViewManager: ReaderWebViewManager

  private val tabsManager = mockk<TabsManager>(relaxed = true)
  private lateinit var tabsState: MutableStateFlow<TabsManager.TabsState>
  private val readerSessionManager = mockk<ReaderSessionManager>(relaxed = true)
  private val webViewFactory = mockk<WebViewFactory>(relaxed = true)
  private val callback = mockk<WebViewCallback>()
  private val frameLayout = mockk<FrameLayout>()
  private val historyList = mockk<WebBackForwardList>()

  @BeforeEach
  fun setup() {
    clearAllMocks()
    tabsState = MutableStateFlow(TabsManager.TabsState())
    every { tabsManager.tabState } returns tabsState
    readerWebViewManager = ReaderWebViewManager(
      tabsManager,
      readerSessionManager,
      webViewFactory,
      mainDispatcherRule.mainDispatcher
    )
  }

  @Nested
  inner class CreateNewTab {
    @Test
    fun `createNewTab creates webview`() = runTest {
      val webView = mockk<KiwixWebView>()
      every {
        webViewFactory.create(callback, frameLayout)
      } returns webView

      every { tabsManager.size() } returns 1
      val newTabConfig = TabsManager.NewTabConfig(
        url = "url",
        selectTab = false,
        callback = callback,
        videoView = frameLayout,
        readAloudManager = mockk(),
        documentParser = mockk(),
        selectTabCallback = {},
      )
      readerWebViewManager.createNewTab(tabConfig = newTabConfig).also { kiwixWebView ->
        readerWebViewManager.addNewTabInTabsManager(kiwixWebView, newTabConfig)
      }

      verify {
        tabsManager.addWebView(webView)
      }

      verify {
        webView.loadUrl("url")
      }
    }

    @Test
    fun `createNewTab does not load url when shouldLoadUrl is false`() = runTest {
      val webView = mockk<KiwixWebView>()
      every { webViewFactory.create(callback, frameLayout) } returns webView
      readerWebViewManager.createNewTab(
        tabConfig = TabsManager.NewTabConfig(
          url = "url",
          selectTab = false,
          callback = callback,
          videoView = frameLayout,
          shouldLoadUrl = false,
          readAloudManager = mockk(),
          documentParser = mockk(),
          selectTabCallback = {},
        )
      )

      verify(exactly = 0) {
        webView.loadUrl(any())
      }
    }

    @Test
    fun `createNewTab does not select tab when selectTab is false`() = runTest {
      val webView = mockk<KiwixWebView>()
      every { webViewFactory.create(callback, frameLayout) } returns webView
      val newTabConfig = TabsManager.NewTabConfig(
        url = "url",
        selectTab = false,
        callback = callback,
        videoView = frameLayout,
        readAloudManager = mockk(),
        documentParser = mockk(),
        selectTabCallback = {},
      )
      readerWebViewManager.createNewTab(tabConfig = newTabConfig).also { kiwixWebView ->
        readerWebViewManager.addNewTabInTabsManager(kiwixWebView, newTabConfig)
      }
      verify {
        tabsManager.addWebView(webView, false)
      }
    }
  }

  @Nested
  inner class LoadUrl {
    @Test
    fun `loadUrl loads valid url`() = runTest {
      val webView = mockk<KiwixWebView>()
      readerWebViewManager.loadUrl("testUrl", webView)

      verify {
        webView.loadUrl("testUrl")
      }
    }

    @Test
    fun `loadUrl ignores null url`() = runTest {
      val webView = mockk<KiwixWebView>()
      readerWebViewManager.loadUrl(null, webView)

      verify(exactly = 0) {
        webView.loadUrl(any())
      }
    }

    @Test
    fun `loadUrl ignores url ending with null`() = runTest {
      val webView = mockk<KiwixWebView>()
      readerWebViewManager.loadUrl("content://null", webView)

      verify(exactly = 0) {
        webView.loadUrl(any())
      }
    }

    @Test
    fun `loadUrlWithCurrentWebview delegates to webview`() = runTest {
      val webView = mockk<KiwixWebView>()
      readerWebViewManager.loadUrlWithCurrentWebview("url", webView)

      verify {
        webView.loadUrl("url")
      }
    }
  }

  @Nested
  inner class OpenArticle {
    @Test
    fun `openArticle with null url does nothing`() = runTest {
      val webView = mockk<KiwixWebView>()
      val manager = spyk(readerWebViewManager)

      manager.openPage(null, webView)

      verify(exactly = 0) {
        webView.loadUrl(any())
      }
    }

    @Test
    fun `openArticle loads redirected url`() = runTest {
      val webView = mockk<KiwixWebView>()
      val manager = spyk(readerWebViewManager)

      val container = mockk<ZimReaderContainer>()

      every { manager.contentUrl("article") } returns "content://article"

      every { readerSessionManager.zimReaderContainer } returns container
      every { container.isRedirect("content://article") } returns true
      every { container.getRedirect("content://article") } returns "redirect"

      manager.openPage("article", webView)

      verify {
        webView.loadUrl("redirect")
      }
    }

    @Test
    fun `openArticle loads original url when not redirected`() = runTest {
      val webView = mockk<KiwixWebView>()
      val manager = spyk(readerWebViewManager)

      val container = mockk<ZimReaderContainer>()

      every { manager.contentUrl("article") } returns "content://article"

      every { readerSessionManager.zimReaderContainer } returns container
      every { container.isRedirect("content://article") } returns false

      manager.openPage("article", webView)

      verify {
        webView.loadUrl("content://article")
      }
    }
  }

  @Nested
  inner class TabsTest {
    @Test
    fun `restoreTabs restores all tabs`() = runTest {
      val history = listOf(
        mockk<WebViewHistoryItem>(),
        mockk<WebViewHistoryItem>()
      )

      coEvery {
        readerSessionManager.restoreTabState(any(), any())
      } just Runs

      val result =
        readerWebViewManager.restoreTabs(
          history,
          currentTab = 1,
          newTabConfig = TabsManager.NewTabConfig(
            callback = mockk(),
            videoView = mockk(),
            url = null,
            readAloudManager = mockk(),
            documentParser = mockk(),
            selectTabCallback = {},
          )
        )

      assertEquals(
        ReaderWebViewManager.RestoreTabsResult.TabsRestored,
        result
      )

      verify {
        tabsManager.clearTabsState()
      }

      coVerify {
        readerSessionManager.restoreTabState(any(), history[0])
        readerSessionManager.restoreTabState(any(), history[1])
      }
    }

    @Test
    fun `restoreTabs returns error when restore fails`() = runTest {
      val throwable = RuntimeException("failure")

      coEvery {
        readerSessionManager.restoreTabState(any(), any())
      } throws throwable

      val result =
        readerWebViewManager.restoreTabs(
          listOf(mockk()),
          currentTab = 0,
          newTabConfig = TabsManager.NewTabConfig(
            callback = mockk(),
            videoView = mockk(),
            url = null,
            readAloudManager = mockk(),
            documentParser = mockk(),
            selectTabCallback = {},
          )
        )

      assertThat(result).isInstanceOf(ReaderWebViewManager.RestoreTabsResult.ErrorInRestoringTabs::class.java)
      val resultt = result as ReaderWebViewManager.RestoreTabsResult.ErrorInRestoringTabs
      assertEquals(
        throwable.message,
        resultt.throwable.message
      )
    }
  }

  @Nested
  inner class DelegatesTest {
    @Test
    fun `tabsSize delegates to tabsManager`() {
      every { tabsManager.size() } returns 4

      assertEquals(4, readerWebViewManager.tabsSize())
    }

    @Test
    fun `closeTab delegates to tabsManager`() {
      val webView = mockk<KiwixWebView>()
      every { tabsManager.closeTab(1) } returns webView

      assertEquals(webView, readerWebViewManager.closeTab(1))
    }

    @Test
    fun `closeAllTabs delegates to tabsManager`() {
      val state = mockk<TabsManager.TabsState>()

      every { tabsManager.closeAllTabs() } returns state

      assertEquals(state, readerWebViewManager.closeAllTabs())
    }

    @Test
    fun `restoreDeletedTab delegates to tabsManager`() {
      val webView = mockk<KiwixWebView>()
      readerWebViewManager.restoreDeletedTab(webView, 2)

      verify {
        tabsManager.restoreTab(webView, 2)
      }
    }

    @Test
    fun `restoreDeletedTabs delegates to tabsManager`() {
      val state = mockk<TabsManager.TabsState>()

      readerWebViewManager.restoreDeletedTabs(state)

      verify {
        tabsManager.restoreTabs(state)
      }
    }

    @Test
    fun `getCurrentWebView delegates to tabsManager`() {
      val webView = mockk<KiwixWebView>()
      every { tabsManager.getCurrentWebView() } returns webView

      assertEquals(webView, readerWebViewManager.getCurrentWebView())
    }

    @Test
    fun `setCurrentWebViewIndex delegates to tabsManager`() {
      readerWebViewManager.setCurrentWebViewIndex(3)

      verify {
        tabsManager.setCurrentWebViewIndex(3)
      }
    }
  }

  @Nested
  inner class DestroyTabs {
    @Test
    fun `destroyAllTabs destroys every webview`() = runTest {
      val first = mockk<KiwixWebView>(relaxed = true)
      val second = mockk<KiwixWebView>(relaxed = true)
      tabsState.value = TabsManager.TabsState(listOf(first, second), 0)

      readerWebViewManager.destroyAllTabs()

      verify {
        first.stopLoading()
        first.clearHistory()
        first.clearCache(true)
        first.onPause()
        first.dispose()
        first.destroy()

        second.stopLoading()
        second.clearHistory()
        second.clearCache(true)
        second.onPause()
        second.dispose()
        second.destroy()

        tabsManager.closeAllTabs()
      }
    }

    @Test
    fun `destroyAllTabs clears tabs when destroy throws`() = runTest {
      val webView = mockk<KiwixWebView>()
      every { webView.stopLoading() } just Runs
      every { webView.clearHistory() } just Runs
      every { webView.clearCache(true) } just Runs
      every { webView.onPause() } just Runs
      every { webView.dispose() } just Runs
      every { webView.destroy() } throws RuntimeException()
      tabsState.value = TabsManager.TabsState(listOf(webView), 0)

      readerWebViewManager.destroyAllTabs()

      verify {
        tabsManager.closeAllTabs()
      }
    }
  }

  @Nested
  inner class WebViewNavigationHistory {
    @Test
    fun `getWebViewNavigationHistory returns NoHistoryFound when current webview is null`() {
      every { tabsManager.getCurrentWebView() } returns null

      assertEquals(
        ReaderWebViewManager.WebViewNavigationHistoryResult.NoHistoryFound,
        readerWebViewManager.getWebViewNavigationHistory(false)
      )
    }

    @Test
    fun `getWebViewNavigationHistory returns NoHistoryFound when no forward history exists`() {
      mockk<KiwixWebView>().mockCurrentWebView()

      assertEquals(
        ReaderWebViewManager.WebViewNavigationHistoryResult.NoHistoryFound,
        readerWebViewManager.getWebViewNavigationHistory(true)
      )
    }

    @Test
    fun `getWebViewNavigationHistory returns NoHistoryFound when no back history exists`() {
      mockk<KiwixWebView>().mockCurrentWebView()

      assertEquals(
        ReaderWebViewManager.WebViewNavigationHistoryResult.NoHistoryFound,
        readerWebViewManager.getWebViewNavigationHistory(false)
      )
    }

    @Test
    fun `getWebViewNavigationHistory returns back history`() {
      val item0 = webViewHistoryItem("A", "urlA")
      val item1 = webViewHistoryItem("B", "urlB")
      val current = webViewHistoryItem("Current", "current")
      mockk<KiwixWebView>().mockCurrentWebView(canGoBack = true)

      every { historyList.currentIndex } returns 2
      every { historyList.size } returns 3

      every { historyList.getItemAtIndex(0) } returns item0
      every { historyList.getItemAtIndex(1) } returns item1
      every { historyList.getItemAtIndex(2) } returns current

      val result = readerWebViewManager.getWebViewNavigationHistory(false)

      assertEquals(
        ReaderWebViewManager.WebViewNavigationHistoryResult.HistoryFound(
          false,
          listOf(
            NavigationHistoryListItem("B", "urlB"),
            NavigationHistoryListItem("A", "urlA")
          )
        ),
        result
      )
    }

    @Test
    fun `getWebViewNavigationHistory returns forward history`() {
      val current = webViewHistoryItem("Current", "current")
      val item1 = webViewHistoryItem("B", "urlB")
      val item2 = webViewHistoryItem("C", "urlC")
      mockk<KiwixWebView>().mockCurrentWebView(canGoForward = true)

      every { historyList.currentIndex } returns 0
      every { historyList.size } returns 3

      every { historyList.getItemAtIndex(0) } returns current
      every { historyList.getItemAtIndex(1) } returns item1
      every { historyList.getItemAtIndex(2) } returns item2

      val result = readerWebViewManager.getWebViewNavigationHistory(true)

      assertEquals(
        ReaderWebViewManager.WebViewNavigationHistoryResult.HistoryFound(
          true,
          listOf(
            NavigationHistoryListItem("B", "urlB"),
            NavigationHistoryListItem("C", "urlC")
          )
        ),
        result
      )
    }

    @Test
    fun `getWebViewNavigationHistory returns NoHistoryFound when history contains only current page`() {
      mockk<KiwixWebView>().mockCurrentWebView(true)
      every { historyList.currentIndex } returns 0
      every { historyList.size } returns 1

      assertEquals(
        ReaderWebViewManager.WebViewNavigationHistoryResult.NoHistoryFound,
        readerWebViewManager.getWebViewNavigationHistory(true)
      )
    }

    private fun KiwixWebView.mockCurrentWebView(
      canGoBack: Boolean = false,
      canGoForward: Boolean = false
    ) {
      every { tabsManager.getCurrentWebView() } returns this
      every { canGoBack() } returns canGoBack
      every { canGoForward() } returns canGoForward
      every { copyBackForwardList() } returns historyList
    }

    private fun webViewHistoryItem(
      title: String,
      url: String
    ): WebHistoryItem {
      val item = mockk<WebHistoryItem>()
      every { item.title } returns title
      every { item.url } returns url
      return item
    }
  }
}
