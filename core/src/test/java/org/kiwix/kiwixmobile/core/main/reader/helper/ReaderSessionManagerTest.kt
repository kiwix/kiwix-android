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

import android.os.Bundle
import android.webkit.WebBackForwardList
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.kiwix.kiwixmobile.core.main.KiwixWebView
import org.kiwix.kiwixmobile.core.main.MainRepositoryActions
import org.kiwix.kiwixmobile.core.page.history.models.WebViewHistoryItem
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.sharedFunctions.MainDispatcherRule

class ReaderSessionManagerTest {
  @JvmField
  @RegisterExtension
  val mainDispatcherRule = MainDispatcherRule()
  private val tabsManager = mockk<TabsManager>()
  private val mainRepositoryActions = mockk<MainRepositoryActions>()
  private val zimReaderContainer = mockk<ZimReaderContainer>()
  private val kiwixDataStore = mockk<KiwixDataStore>()
  private val zimFileManager = mockk<ZimFileManager>()
  private lateinit var readerSessionManager: ReaderSessionManager

  @BeforeEach
  fun setup() {
    clearAllMocks()
    readerSessionManager = ReaderSessionManager(
      tabsManager,
      zimFileManager,
      kiwixDataStore,
      mainRepositoryActions,
      zimReaderContainer,
      mainDispatcherRule.dispatcher,
      mainDispatcherRule.mainDispatcher
    )
  }

  @Nested
  inner class RestoreReaderSessionTest {
    @Test
    fun `restoreReaderSession returns Empty when no history exists`() = runTest {
      coEvery {
        mainRepositoryActions.loadWebViewPagesHistory()
      } returns emptyList()

      assertEquals(
        ReaderSessionManager.RestoreSessionResult.Empty,
        readerSessionManager.restoreReaderSession()
      )
    }

    @Test
    fun `restoreReaderSession returns Valid when history exists`() = runTest {
      val history = listOf(mockk<WebViewHistoryItem>())

      coEvery {
        mainRepositoryActions.loadWebViewPagesHistory()
      } returns history

      every { kiwixDataStore.currentTab } returns flowOf(2)
      every { kiwixDataStore.currentZimFile } returns flowOf("wikipedia.zim")

      assertEquals(
        ReaderSessionManager.RestoreSessionResult.Valid(
          currentTab = 2,
          currentZimFile = "wikipedia.zim",
          webViewHistoryList = history
        ),
        readerSessionManager.restoreReaderSession()
      )
    }

    @Test
    fun `restoreReaderSession returns zero when current tab is negative`() = runTest {
      val history = listOf(mockk<WebViewHistoryItem>())

      coEvery {
        mainRepositoryActions.loadWebViewPagesHistory()
      } returns history

      every { kiwixDataStore.currentTab } returns flowOf(-5)
      every { kiwixDataStore.currentZimFile } returns flowOf("zim")

      val result = readerSessionManager.restoreReaderSession()

      assertEquals(
        ReaderSessionManager.RestoreSessionResult.Valid(
          currentTab = 0,
          currentZimFile = "zim",
          webViewHistoryList = history
        ),
        result
      )
    }

    @Test
    fun `restoreReaderSession returns Invalid when loading history fails`() = runTest {
      coEvery {
        mainRepositoryActions.loadWebViewPagesHistory()
      } throws RuntimeException("failure")

      assertEquals(
        ReaderSessionManager.RestoreSessionResult.Invalid,
        readerSessionManager.restoreReaderSession()
      )
    }

    @Test
    fun `restoreReaderSession returns zero when current tab is null`() = runTest {
      val history = listOf(mockk<WebViewHistoryItem>())

      coEvery {
        mainRepositoryActions.loadWebViewPagesHistory()
      } returns history

      every { kiwixDataStore.currentTab } returns flowOf(null)
      every { kiwixDataStore.currentZimFile } returns flowOf("zim")

      val result = readerSessionManager.restoreReaderSession()

      assertEquals(
        ReaderSessionManager.RestoreSessionResult.Valid(
          currentTab = 0,
          currentZimFile = "zim",
          webViewHistoryList = history
        ),
        result
      )
    }
  }

  @Nested
  inner class SaveReaderSessionTest {
    @Test
    fun `saveReaderSession saves reader session`() = runTest {
      val webView = mockWebView("url", 50, 2)
      mockZimReader()
      mockCurrentTabs(listOf(webView))
      mockSaveReaderSessionDependencies()

      var callbackCalled = false

      readerSessionManager.saveReaderSession {
        callbackCalled = true
      }

      coVerifyOrder {
        mainRepositoryActions.clearWebViewPageHistory()
        mainRepositoryActions.saveWebViewPageHistory(any())
        kiwixDataStore.setCurrentZimFile("database-source")
        kiwixDataStore.setCurrentTab(0)
      }

      assertTrue(callbackCalled)
    }

    @Test
    fun `saveReaderSession saves empty zim source when source is null`() = runTest {
      mockCurrentTabs()
      every { zimFileManager.zimReaderSource } returns null

      mockSaveReaderSessionDependencies(false)

      readerSessionManager.saveReaderSession()

      coVerify {
        kiwixDataStore.setCurrentZimFile("")
      }
    }

    @Test
    fun `saveReaderSession skips webview with null url`() = runTest {
      val webView = mockk<KiwixWebView>()
      every { webView.url } returns null
      mockCurrentTabs(listOf(webView))
      mockSaveReaderSessionDependencies()

      readerSessionManager.saveReaderSession()

      coVerify {
        mainRepositoryActions.saveWebViewPageHistory(emptyList())
      }

      verify(exactly = 0) {
        webView.saveState(any())
      }
    }

    @Test
    fun `saveReaderSession skips webview when saveState returns null`() = runTest {
      val webView = mockk<KiwixWebView>(relaxed = true)

      every { webView.url } returns "url"
      every { webView.saveState(any()) } returns null

      val zimReader = mockk<ZimFileReader>()
      every { zimReader.id } returns "zim"

      every { zimReaderContainer.zimFileReader } returns zimReader
      mockCurrentTabs(listOf(webView))
      mockSaveReaderSessionDependencies()

      readerSessionManager.saveReaderSession()

      coVerify {
        mainRepositoryActions.saveWebViewPageHistory(emptyList())
      }
    }

    @Test
    fun `saveReaderSession skips webview when history is empty`() = runTest {
      mockSaveReaderSessionDependencies()
      mockZimReader()

      val webView = mockWebView(historySize = 0)

      mockCurrentTabs(listOf(webView))

      readerSessionManager.saveReaderSession()

      coVerify {
        mainRepositoryActions.saveWebViewPageHistory(emptyList())
      }
    }

    @Test
    fun `saveReaderSession skips webview when zim reader is null`() = runTest {
      mockSaveReaderSessionDependencies()

      every { zimReaderContainer.zimFileReader } returns null

      val webView = mockWebView()

      mockCurrentTabs(listOf(webView))

      readerSessionManager.saveReaderSession()

      coVerify {
        mainRepositoryActions.saveWebViewPageHistory(emptyList())
      }
    }

    @Test
    fun `saveReaderSession saves all valid webviews`() = runTest {
      mockSaveReaderSessionDependencies()
      mockZimReader()

      val first = mockWebView(url = "url1")
      val second = mockWebView(url = "url2")

      mockCurrentTabs(listOf(first, second))

      readerSessionManager.saveReaderSession()

      coVerify {
        mainRepositoryActions.saveWebViewPageHistory(
          match { it.size == 2 }
        )
      }
    }

    @Test
    fun `saveReaderSession saves selected tab index`() = runTest {
      mockSaveReaderSessionDependencies()

      mockCurrentTabs(selectedIndex = 3)

      every { zimFileManager.zimReaderSource } returns null

      readerSessionManager.saveReaderSession()

      coVerify {
        kiwixDataStore.setCurrentTab(3)
      }
    }

    private fun mockZimReader(id: String = "zim-id") {
      val zimReader = mockk<ZimFileReader>()

      every { zimReader.id } returns id
      every { zimReaderContainer.zimFileReader } returns zimReader
    }

    private fun mockCurrentTabs(
      webViews: List<KiwixWebView> = emptyList(),
      selectedIndex: Int = 0
    ) {
      every { tabsManager.currentState() } returns
        TabsManager.TabsState(
          webViews = webViews,
          selectedIndex = selectedIndex
        )
    }

    private fun mockSaveReaderSessionDependencies(shouldSetSource: Boolean = true) {
      if (shouldSetSource) {
        val source = mockk<ZimReaderSource>()
        every { source.toDatabase() } returns "database-source"
        every { zimFileManager.zimReaderSource } returns source
      }
      coEvery { mainRepositoryActions.clearWebViewPageHistory() } just Runs
      coEvery { mainRepositoryActions.saveWebViewPageHistory(any()) } just Runs
      coEvery { kiwixDataStore.setCurrentTab(any()) } just Runs
      coEvery { kiwixDataStore.setCurrentZimFile(any()) } just Runs
    }

    private fun mockWebView(
      url: String? = "url",
      scrollY: Int = 50,
      historySize: Int = 2
    ): KiwixWebView {
      val webView = mockk<KiwixWebView>(relaxed = true)
      val history = mockk<WebBackForwardList>()

      every { webView.url } returns url
      every { webView.scrollY } returns scrollY
      every { webView.saveState(any()) } returns history
      every { history.size } returns historySize

      return webView
    }
  }

  @Nested
  inner class RestoreTabStateTest {
    @Test
    fun `restoreTabState restores webview state and scroll position`() {
      val webView = mockk<KiwixWebView>(relaxed = true)
      val bundle = Bundle()

      val historyItem = mockk<WebViewHistoryItem>()

      every { historyItem.webViewBackForwardListBundle } returns bundle
      every { historyItem.webViewCurrentPosition } returns 250

      readerSessionManager.restoreTabState(
        webView,
        historyItem
      )

      verify {
        webView.restoreState(bundle)
      }

      verify {
        webView.scrollY = 250
      }

      verify(exactly = 0) {
        webView.loadUrl(any())
      }
    }

    @Test
    fun `restoreTabState does nothing when bundle is null and zim reader is null`() {
      val webView = mockk<KiwixWebView>(relaxed = true)
      val historyItem = mockk<WebViewHistoryItem>()

      every { historyItem.webViewBackForwardListBundle } returns null
      every { zimReaderContainer.zimFileReader } returns null

      readerSessionManager.restoreTabState(
        webView,
        historyItem
      )

      verify(exactly = 0) {
        webView.restoreState(any())
      }

      verify(exactly = 0) {
        webView.loadUrl(any())
      }
    }

    @Test
    fun `restoreTabState does nothing when history item is null and zim reader is null`() {
      val webView = mockk<KiwixWebView>(relaxed = true)

      every { zimReaderContainer.zimFileReader } returns null

      readerSessionManager.restoreTabState(
        webView,
        null
      )

      verify(exactly = 0) {
        webView.restoreState(any())
      }

      verify(exactly = 0) {
        webView.loadUrl(any())
      }
    }

    @Test
    fun `restoreTabState percent-encodes the main page path before loading it`() {
      val webView = mockk<KiwixWebView>(relaxed = true)
      val zimFileReader = mockk<ZimFileReader>()
      val url = "https://kiwix.app/A/4%2B4_Photocycloaddition"

      every { zimReaderContainer.zimFileReader } returns zimFileReader
      every { zimFileReader.mainPage } returns "A/4+4_Photocycloaddition"
      every { zimReaderContainer.isRedirect(url) } returns false

      readerSessionManager.restoreTabState(webView, null)

      verify {
        webView.loadUrl(url)
      }
    }
  }

  @Nested
  inner class ClearHistoryTest {
    @Test
    fun `clearWebViewHistory clears current webview history and repository`() = runTest {
      val webView = mockk<KiwixWebView>(relaxed = true)

      every { tabsManager.getCurrentWebView() } returns webView
      coEvery { mainRepositoryActions.clearWebViewPageHistory() } just Runs

      readerSessionManager.clearWebViewHistory()

      verify {
        webView.clearHistory()
      }

      coVerify {
        mainRepositoryActions.clearWebViewPageHistory()
      }
    }

    @Test
    fun `clearWebViewHistory clears repository when current webview is null`() = runTest {
      every { tabsManager.getCurrentWebView() } returns null
      coEvery { mainRepositoryActions.clearWebViewPageHistory() } just Runs

      readerSessionManager.clearWebViewHistory()

      coVerify {
        mainRepositoryActions.clearWebViewPageHistory()
      }
    }

    @Test
    fun `clearWebViewHistory handles repository exception`() = runTest {
      val webView = mockk<KiwixWebView>(relaxed = true)

      every { tabsManager.getCurrentWebView() } returns webView

      coEvery {
        mainRepositoryActions.clearWebViewPageHistory()
      } throws RuntimeException("failure")

      readerSessionManager.clearWebViewHistory()

      verify {
        webView.clearHistory()
      }

      coVerify {
        mainRepositoryActions.clearWebViewPageHistory()
      }
    }
  }
}
