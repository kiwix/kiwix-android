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
 */

package org.kiwix.kiwixmobile.core.main

import android.os.Build
import android.webkit.WebView
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class CoreWebViewClientTest {
  private lateinit var callback: WebViewCallback
  private lateinit var zimReaderContainer: ZimReaderContainer
  private lateinit var coreWebViewClient: CoreWebViewClient

  @Before
  fun setUp() {
    clearAllMocks()
    callback = mockk(relaxed = true)
    zimReaderContainer = mockk(relaxed = true)
    coreWebViewClient = CoreWebViewClient(callback, zimReaderContainer)
  }

  @Test
  fun `onPageFinished dispatches callback for the specific webview after visual state callback`() {
    val webView = mockk<WebView>(relaxed = true)
    val visualStateCallback = slot<WebView.VisualStateCallback>()
    every {
      webView.postVisualStateCallback(any(), capture(visualStateCallback))
    } answers {
      visualStateCallback.captured.onComplete(firstArg())
      Unit
    }

    coreWebViewClient.onPageFinished(webView, "zim://content/A/Article")

    verify(exactly = 1) { callback.webViewUrlFinishedLoading(webView) }
  }

  @Test
  fun `onPageFinished ignores invalid null article url`() {
    val webView = mockk<WebView>(relaxed = true)

    coreWebViewClient.onPageFinished(webView, ZimFileReader.CONTENT_PREFIX + "null")

    verify(exactly = 0) { callback.webViewUrlFinishedLoading(any()) }
    verify(exactly = 0) { webView.postVisualStateCallback(any(), any()) }
  }

  @Test
  fun `onPageCommitVisible refreshes accessibility for kiwix webview only`() {
    val kiwixWebView = mockk<KiwixWebView>(relaxed = true)
    val plainWebView = mockk<WebView>(relaxed = true)

    coreWebViewClient.onPageCommitVisible(kiwixWebView, "zim://content/A/Article")
    coreWebViewClient.onPageCommitVisible(plainWebView, "zim://content/A/Article")

    verify(exactly = 1) { kiwixWebView.refreshVisibleContentForAccessibility() }
  }
}
