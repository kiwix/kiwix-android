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

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.kiwix.kiwixmobile.core.main.KiwixWebView
import org.kiwix.kiwixmobile.core.utils.ZERO
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TabsManager @Inject constructor() {
  data class TabsState(
    val webViews: List<KiwixWebView> = emptyList(),
    val selectedIndex: Int = ZERO
  ) {
    val currentWebView: KiwixWebView?
      get() = webViews.getOrNull(selectedIndex)
  }

  private val _tabState = MutableStateFlow(TabsState())
  val tabState = _tabState.asStateFlow()

  fun currentState() = _tabState.value

  /**
   * Add the webView in the current list.
   * @param webView: Webview for adding in the list.
   * @param selectTab: A boolean value, if false it will add the webView in background.
   */
  fun addWebView(webView: KiwixWebView, selectTab: Boolean = true) {
    _tabState.update { state ->
      val list = state.webViews + webView
      state.copy(
        webViews = list,
        selectedIndex = if (selectTab) list.lastIndex else state.selectedIndex
      )
    }
    Log.e("HISTORY", "addWebView: ${currentState().webViews}")
  }

  fun getCurrentWebView(): KiwixWebView? = tabState.value.currentWebView

  fun setCurrentWebViewIndex(index: Int) {
    _tabState.update { it.copy(selectedIndex = index) }
  }

  /**
   * Update the currentWebViewIndex.
   */
  fun selectTab(index: Int) {
    _tabState.update { state ->
      if (index !in state.webViews.indices) {
        state
      } else {
        state.copy(selectedIndex = index)
      }
    }
  }

  /**
   * Removed the tab for given index, and returns the KiwixWebView of that index.
   * So that caller can perform the restoreTab operation.
   */
  fun closeTab(index: Int): KiwixWebView? {
    val state = _tabState.value
    if (index !in state.webViews.indices) return null

    val removed = state.webViews[index]

    val list = state.webViews.toMutableList()
    list.removeAt(index)

    val selected =
      when {
        list.isEmpty() -> ZERO
        state.selectedIndex > list.lastIndex -> list.lastIndex
        state.selectedIndex > index -> state.selectedIndex - 1
        else -> state.selectedIndex
      }

    _tabState.value = state.copy(
      webViews = list,
      selectedIndex = selected
    )

    return removed
  }

  /**
   * Removed the entire webView list, and return it list of removed webView.
   * So that caller can perform the restoreAllTabs operation.
   */
  fun closeAllTabs(): TabsState {
    val currentWebViewList = tabState.value
    clearTabsState()
    return currentWebViewList
  }

  fun restoreTab(kiwixWebView: KiwixWebView, index: Int) {
    _tabState.update { state ->
      val list = state.webViews.toMutableList()

      val safeIndex = index.coerceIn(ZERO, list.size)
      list.add(safeIndex, kiwixWebView)

      state.copy(
        webViews = list,
        selectedIndex =
          if (safeIndex <= state.selectedIndex) {
            state.selectedIndex + 1
          } else {
            state.selectedIndex
          }
      )
    }
  }

  fun restoreTabs(tabsState: TabsState) {
    _tabState.update { tabsState }
  }

  fun clearTabsState() {
    _tabState.update { TabsState() }
  }

  fun size(): Int = tabState.value.webViews.size
}
