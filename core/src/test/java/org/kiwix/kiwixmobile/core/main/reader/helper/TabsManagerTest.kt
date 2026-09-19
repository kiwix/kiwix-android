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

import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.main.KiwixWebView

class TabsManagerTest {
  private lateinit var tabsManager: TabsManager

  @BeforeEach
  fun setup() {
    tabsManager = TabsManager()
  }

  private fun webView() = mockk<KiwixWebView>(relaxed = true)

  @Test
  fun `initial state should be empty`() {
    val state = tabsManager.currentState()

    assertTrue(state.webViews.isEmpty())
    assertEquals(0, state.selectedIndex)
    assertNull(state.currentWebView)
  }

  @Test
  fun `addWebView should add and select webview`() {
    val webView = webView()

    tabsManager.addWebView(webView)

    val state = tabsManager.currentState()

    assertEquals(listOf(webView), state.webViews)
    assertEquals(0, state.selectedIndex)
    assertEquals(webView, state.currentWebView)
  }

  @Test
  fun `addWebView with selectTab false should not change selection`() {
    val first = webView()
    val second = webView()

    tabsManager.addWebView(first)
    tabsManager.addWebView(second, selectTab = false)

    val state = tabsManager.currentState()

    assertEquals(2, state.webViews.size)
    assertEquals(0, state.selectedIndex)
    assertEquals(first, state.currentWebView)
  }

  @Test
  fun `setCurrentWebViewIndex should update selected index`() {
    val first = webView()
    val second = webView()

    tabsManager.addWebView(first)
    tabsManager.addWebView(second)

    tabsManager.setCurrentWebViewIndex(0)

    val state = tabsManager.currentState()

    assertEquals(0, state.selectedIndex)
    assertEquals(first, state.currentWebView)
  }

  @Test
  fun `setCurrentWebViewIndex with invalid index should not change state`() {
    val first = webView()

    tabsManager.addWebView(first)

    tabsManager.setCurrentWebViewIndex(10)

    val state = tabsManager.currentState()

    assertEquals(0, state.selectedIndex)
    assertEquals(first, state.currentWebView)
  }

  @Test
  fun `closeTab should remove selected tab`() {
    val first = webView()
    val second = webView()

    tabsManager.addWebView(first)
    tabsManager.addWebView(second)

    val removed = tabsManager.closeTab(1)

    val state = tabsManager.currentState()

    assertEquals(second, removed)
    assertEquals(listOf(first), state.webViews)
    assertEquals(0, state.selectedIndex)
  }

  @Test
  fun `closeTab before selected should shift selected index`() {
    val first = webView()
    val second = webView()
    val third = webView()

    tabsManager.addWebView(first)
    tabsManager.addWebView(second)
    tabsManager.addWebView(third)

    tabsManager.setCurrentWebViewIndex(2)

    tabsManager.closeTab(1)

    val state = tabsManager.currentState()

    assertEquals(1, state.selectedIndex)
    assertEquals(third, state.currentWebView)
  }

  @Test
  fun `closeTab with invalid index returns null`() {
    val removed = tabsManager.closeTab(5)

    assertNull(removed)
  }

  @Test
  fun `closeAllTabs should clear state and return previous state`() {
    val first = webView()
    val second = webView()

    tabsManager.addWebView(first)
    tabsManager.addWebView(second)

    val previous = tabsManager.closeAllTabs()

    assertEquals(2, previous.webViews.size)
    assertTrue(tabsManager.currentState().webViews.isEmpty())
    assertEquals(0, tabsManager.currentState().selectedIndex)
  }

  @Test
  fun `restoreTab should insert webview at index`() {
    val first = webView()
    val second = webView()
    val restored = webView()

    tabsManager.addWebView(first)
    tabsManager.addWebView(second)

    tabsManager.restoreTab(restored, 1)

    val state = tabsManager.currentState()

    assertEquals(
      listOf(first, restored, second),
      state.webViews
    )
  }

  @Test
  fun `restoreTabs should restore state`() {
    val first = webView()
    val second = webView()

    val state = TabsManager.TabsState(
      webViews = listOf(first, second),
      selectedIndex = 1
    )

    tabsManager.restoreTabs(state)

    assertEquals(state, tabsManager.currentState())
  }

  @Test
  fun `clearTabsState should reset state`() {
    tabsManager.addWebView(webView())

    tabsManager.clearTabsState()

    assertTrue(tabsManager.currentState().webViews.isEmpty())
    assertEquals(0, tabsManager.currentState().selectedIndex)
  }

  @Test
  fun `size should return number of tabs`() {
    assertEquals(0, tabsManager.size())

    tabsManager.addWebView(webView())
    tabsManager.addWebView(webView())

    assertEquals(2, tabsManager.size())
  }

  @Test
  fun `getCurrentWebView returns selected webview`() {
    val first = webView()
    val second = webView()

    tabsManager.addWebView(first)
    tabsManager.addWebView(second)

    tabsManager.setCurrentWebViewIndex(0)

    assertEquals(first, tabsManager.getCurrentWebView())
  }
}
