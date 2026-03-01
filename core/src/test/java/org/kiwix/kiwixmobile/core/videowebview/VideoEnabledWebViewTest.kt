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

package org.kiwix.kiwixmobile.core.videowebview

import android.content.Context
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.kiwix.videowebview.VideoEnabledWebChromeClient
import org.kiwix.videowebview.VideoEnabledWebView

class VideoEnabledWebViewTest {
  private val context: Context = mockk(relaxed = true)

  @BeforeEach
  fun setUp() {
    clearAllMocks()
  }

  @Nested
  inner class SetWebChromeClientTests {
    @Test
    fun `should enable JavaScript when setWebChromeClient is called`() {
      val webView = spyk(VideoEnabledWebView(context))
      val settings: WebSettings = mockk(relaxed = true)
      every { webView.settings } returns settings

      val chromeClient = VideoEnabledWebChromeClient()
      webView.webChromeClient = chromeClient

      verify { settings.javaScriptEnabled = true }
    }

    @Test
    fun `should not store as video client when regular WebChromeClient is set`() {
      val webView = spyk(VideoEnabledWebView(context))
      val settings: WebSettings = mockk(relaxed = true)
      every { webView.settings } returns settings

      val regularClient: WebChromeClient = mockk(relaxed = true)
      webView.webChromeClient = regularClient

      assertThat(webView.isVideoFullscreen).isFalse()
    }
  }

  @Nested
  inner class IsVideoFullscreenTests {
    @Test
    fun `should return false when no chrome client is set`() {
      val webView = spyk(VideoEnabledWebView(context))

      assertThat(webView.isVideoFullscreen).isFalse()
    }
  }

  @Nested
  inner class LoadDataTests {
    @Test
    fun `should add JavaScript interface on first loadData call`() {
      val webView = spyk(VideoEnabledWebView(context))

      webView.loadData("<html></html>", "text/html", "utf-8")

      verify {
        webView.addJavascriptInterface(
          any<VideoEnabledWebView.JavascriptInterface>(),
          "_VideoEnabledWebView"
        )
      }
    }

    @Test
    fun `should add JavaScript interface only once across multiple loadData calls`() {
      val webView = spyk(VideoEnabledWebView(context))

      webView.loadData("<html>1</html>", "text/html", "utf-8")
      webView.loadData("<html>2</html>", "text/html", "utf-8")
      webView.loadData("<html>3</html>", "text/html", "utf-8")

      verify(exactly = 1) {
        webView.addJavascriptInterface(
          any<VideoEnabledWebView.JavascriptInterface>(),
          "_VideoEnabledWebView"
        )
      }
    }
  }

  @Nested
  inner class LoadUrlTests {
    @Test
    fun `should add JavaScript interface on first loadUrl call`() {
      val webView = spyk(VideoEnabledWebView(context))

      webView.loadUrl("https://example.com")

      verify {
        webView.addJavascriptInterface(
          any<VideoEnabledWebView.JavascriptInterface>(),
          "_VideoEnabledWebView"
        )
      }
    }

    @Test
    fun `should add JavaScript interface on first loadUrl with headers call`() {
      val webView = spyk(VideoEnabledWebView(context))

      webView.loadUrl("https://example.com", mapOf("Accept" to "text/html"))

      verify {
        webView.addJavascriptInterface(
          any<VideoEnabledWebView.JavascriptInterface>(),
          "_VideoEnabledWebView"
        )
      }
    }

    @Test
    fun `should add JavaScript interface only once across multiple loadUrl calls`() {
      val webView = spyk(VideoEnabledWebView(context))

      webView.loadUrl("https://example1.com")
      webView.loadUrl("https://example2.com")

      verify(exactly = 1) {
        webView.addJavascriptInterface(
          any<VideoEnabledWebView.JavascriptInterface>(),
          "_VideoEnabledWebView"
        )
      }
    }

    @Test
    fun `should add JavaScript interface on loadDataWithBaseURL call`() {
      val webView = spyk(VideoEnabledWebView(context))

      webView.loadDataWithBaseURL(
        "https://example.com",
        "<html></html>",
        "text/html",
        "utf-8",
        null
      )

      verify {
        webView.addJavascriptInterface(
          any<VideoEnabledWebView.JavascriptInterface>(),
          "_VideoEnabledWebView"
        )
      }
    }

    @Test
    fun `should add JavaScript interface only once across different load methods`() {
      val webView = spyk(VideoEnabledWebView(context))

      webView.loadUrl("https://example.com")
      webView.loadData("<html></html>", "text/html", "utf-8")
      webView.loadDataWithBaseURL(
        "https://example.com",
        "<html></html>",
        "text/html",
        "utf-8",
        null
      )

      verify(exactly = 1) {
        webView.addJavascriptInterface(
          any<VideoEnabledWebView.JavascriptInterface>(),
          "_VideoEnabledWebView"
        )
      }
    }
  }
}
