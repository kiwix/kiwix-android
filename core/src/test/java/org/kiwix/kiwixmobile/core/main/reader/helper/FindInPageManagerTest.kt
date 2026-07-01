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

import android.os.Build
import android.webkit.WebView
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])
class FindInPageManagerTest {
  private lateinit var manager: FindInPageManager

  private val webView: WebView = mockk(relaxed = true)

  @Before
  fun setUp() {
    manager = FindInPageManager()
  }

  @Test
  fun `default ui state is correct`() {
    with(manager.uiState.value) {
      assertThat(visible).isFalse()
      assertThat(query).isEmpty()
      assertThat(currentMatch).isZero()
      assertThat(totalMatches).isZero()
      assertThat(resultText).isEmpty()
    }
  }

  @Test(expected = IllegalArgumentException::class)
  fun `search throws when webView is not set`() {
    manager.search("Android")
  }

  @Test(expected = IllegalArgumentException::class)
  fun `findNext throws when webView is not set`() {
    manager.findNext()
  }

  @Test(expected = IllegalArgumentException::class)
  fun `findPrevious throws when webView is not set`() {
    manager.findPrevious()
  }

  @Test
  fun `setWebView registers find listener`() {
    manager.setWebView(webView)

    verify {
      webView.setFindListener(any())
    }
  }

  @Test
  fun `search updates query and delegates to webView`() {
    manager.setWebView(webView)

    manager.search("Kiwix")

    assertThat(manager.uiState.value.query).isEqualTo("Kiwix")

    verify {
      webView.findAllAsync("Kiwix")
    }
  }

  @Test
  fun `empty search clears matches`() {
    manager.setWebView(webView)

    manager.search("")

    verify {
      webView.clearMatches()
    }

    verify(exactly = 0) {
      webView.findAllAsync(any())
    }
  }

  @Test
  fun `findNext delegates to webView`() {
    manager.setWebView(webView)

    manager.findNext()

    verify {
      webView.findNext(true)
    }
  }

  @Test
  fun `findPrevious delegates to webView`() {
    manager.setWebView(webView)

    manager.findPrevious()

    verify {
      webView.findNext(false)
    }
  }

  @Test
  fun `stop clears matches`() {
    manager.setWebView(webView)

    manager.stop()

    verify {
      webView.clearMatches()
    }
  }

  @Test
  fun `find listener updates ui state`() {
    val listenerSlot = slot<WebView.FindListener>()

    every {
      webView.setFindListener(capture(listenerSlot))
    } just Runs

    manager.setWebView(webView)

    manager.search("Android")

    listenerSlot.captured.onFindResultReceived(
      2,
      5,
      true
    )

    with(manager.uiState.value) {
      assertThat(query).isEqualTo("Android")
      assertThat(currentMatch).isEqualTo(3)
      assertThat(totalMatches).isEqualTo(5)
      assertThat(resultText).isEqualTo("3/5")
    }
  }

  @Test
  fun `find listener shows zero results`() {
    val listenerSlot = slot<WebView.FindListener>()

    every {
      webView.setFindListener(capture(listenerSlot))
    } just Runs

    manager.setWebView(webView)

    manager.search("Android")

    listenerSlot.captured.onFindResultReceived(
      0,
      0,
      true
    )

    assertThat(manager.uiState.value.resultText).isEqualTo("0/0")
  }

  @Test
  fun `result text is empty when query is empty`() {
    val state = FindInPageManager.FindInPageUiState()

    assertThat(state.resultText).isEmpty()
  }

  @Test
  fun `result text is zero when there are no matches`() {
    val state = FindInPageManager.FindInPageUiState(
      query = "Android",
      totalMatches = 0
    )

    assertThat(state.resultText).isEqualTo("0/0")
  }

  @Test
  fun `result text shows current and total matches`() {
    val state = FindInPageManager.FindInPageUiState(
      query = "Android",
      currentMatch = 4,
      totalMatches = 9
    )

    assertThat(state.resultText).isEqualTo("4/9")
  }

  @Test
  fun `stop resets ui state`() {
    manager.setWebView(webView)
    manager.search("Android")

    manager.stop()

    assertThat(manager.uiState.value).isEqualTo(FindInPageManager.FindInPageUiState())
  }
}
