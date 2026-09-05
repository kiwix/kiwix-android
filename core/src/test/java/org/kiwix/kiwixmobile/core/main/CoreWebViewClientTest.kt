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

package org.kiwix.kiwixmobile.core.main

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer

class CoreWebViewClientTest {
  private val callback: WebViewCallback = mockk(relaxed = true)
  private val zimReaderContainer: ZimReaderContainer = mockk()
  private val view: WebView = mockk()
  private lateinit var client: CoreWebViewClient

  @BeforeEach
  fun setup() {
    mockkStatic(Uri::class)
    every { Uri.parse(any()) } returns mockk(relaxed = true)
    mockkStatic(Log::class)
    every { Log.w(any(), any<String>(), any()) } returns 0
    mockkConstructor(Intent::class)
    val coreApp = mockk<CoreApp>()
    CoreApp.instance = coreApp
    every { coreApp.packageName } returns "mock_package"
    client = CoreWebViewClient(callback, zimReaderContainer)
    every { zimReaderContainer.isRedirect(any()) } returns false
  }

  @AfterEach
  fun tearDown() {
    unmockkAll()
    clearAllMocks()
  }

  private fun request(url: String, scheme: String): WebResourceRequest {
    val uri = mockk<Uri> {
      every { this@mockk.toString() } returns url
      every { this@mockk.scheme } returns scheme
    }
    every { Uri.parse(url) } returns uri
    return mockk<WebResourceRequest> {
      every { this@mockk.url } returns uri
    }
  }

  @Test
  fun `external navigation to an allowed scheme is handed off`() {
    val result = client.shouldOverrideUrlLoading(view, request("https://example.com/", "https"))
    assertTrue(result)
    verify { callback.openExternalUrl(any()) }
  }

  @Test
  fun `external navigation to a file scheme is blocked`() {
    val result = client.shouldOverrideUrlLoading(view, request("file:///etc/passwd", "file"))
    assertBlocked(result)
  }

  @Test
  fun `external navigation to a content scheme is blocked`() {
    val result =
      client.shouldOverrideUrlLoading(view, request("content://com.attacker/x.zim", "content"))
    assertBlocked(result)
  }

  @Test
  fun `external navigation to an intent scheme is blocked`() {
    val result =
      client.shouldOverrideUrlLoading(view, request("intent://attacker#Intent;end", "intent"))
    assertBlocked(result)
  }

  private fun assertBlocked(result: Boolean) {
    assertTrue(result)
    verify(exactly = 0) { callback.openExternalUrl(any()) }
    verify { Log.w(any(), any<String>(), any()) }
  }
}
