/*
 * Kiwix Android
 * Copyright (c) 2025 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.main.reader

import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ReaderMenuStateTest {
  private val menuClickListener: ReaderMenuState.MenuClickListener = mockk(relaxed = true)
  private lateinit var readerMenuState: ReaderMenuState

  @BeforeEach
  fun setup() {
    readerMenuState = ReaderMenuState(
      menuClickListener = menuClickListener,
      isUrlValidInitially = true
    )
  }

  @Test
  internal fun `share menu item is present when url is valid`() {
    assertThat(
      readerMenuState.menuItems.any { it.testingTag == SHARE_ARTICLE_MENU_ITEM_TESTING_TAG }
    ).isTrue()
  }

  @Test
  internal fun `share menu item is in overflow menu`() {
    val shareItem = readerMenuState.menuItems.first {
      it.testingTag == SHARE_ARTICLE_MENU_ITEM_TESTING_TAG
    }
    assertThat(shareItem.isInOverflow).isTrue()
  }

  @Test
  internal fun `share menu item click invokes onShareMenuClicked`() {
    val shareItem = readerMenuState.menuItems.first {
      it.testingTag == SHARE_ARTICLE_MENU_ITEM_TESTING_TAG
    }
    shareItem.onClick()
    verify { menuClickListener.onShareMenuClicked() }
  }

  @Test
  internal fun `share menu item is hidden when url is not valid`() {
    val state = ReaderMenuState(
      menuClickListener = menuClickListener,
      isUrlValidInitially = false
    )
    assertThat(
      state.menuItems.any { it.testingTag == SHARE_ARTICLE_MENU_ITEM_TESTING_TAG }
    ).isFalse()
  }

  @Test
  internal fun `share menu item is hidden in tab switcher mode`() {
    readerMenuState.showTabSwitcherOptions()
    assertThat(
      readerMenuState.menuItems.any { it.testingTag == SHARE_ARTICLE_MENU_ITEM_TESTING_TAG }
    ).isFalse()
  }

  @Test
  internal fun `share menu item stays hidden after just hiding tab switcher`() {
    readerMenuState.showTabSwitcherOptions()
    readerMenuState.hideTabSwitcher()
    // hideTabSwitcher only resets the flag, it does not restore visibility
    assertThat(
      readerMenuState.menuItems.any { it.testingTag == SHARE_ARTICLE_MENU_ITEM_TESTING_TAG }
    ).isFalse()
  }

  @Test
  internal fun `share menu item reappears after showing web view options`() {
    readerMenuState.showTabSwitcherOptions()
    readerMenuState.showWebViewOptions(true)
    assertThat(
      readerMenuState.menuItems.any { it.testingTag == SHARE_ARTICLE_MENU_ITEM_TESTING_TAG }
    ).isTrue()
  }

  @Test
  internal fun `share menu item is hidden when book specific items are hidden`() {
    readerMenuState.hideBookSpecificMenuItems()
    assertThat(
      readerMenuState.menuItems.any { it.testingTag == SHARE_ARTICLE_MENU_ITEM_TESTING_TAG }
    ).isFalse()
  }

  @Test
  internal fun `share menu item reappears when book specific items are shown`() {
    readerMenuState.hideBookSpecificMenuItems()
    readerMenuState.showBookSpecificMenuItems()
    assertThat(
      readerMenuState.menuItems.any { it.testingTag == SHARE_ARTICLE_MENU_ITEM_TESTING_TAG }
    ).isTrue()
  }

  @Test
  internal fun `share menu item reappears after file opened with valid url`() {
    readerMenuState.hideBookSpecificMenuItems()
    readerMenuState.onFileOpened(urlIsValid = true)
    assertThat(
      readerMenuState.menuItems.any { it.testingTag == SHARE_ARTICLE_MENU_ITEM_TESTING_TAG }
    ).isTrue()
  }

  @Test
  internal fun `share menu item is hidden after file opened with invalid url`() {
    readerMenuState.onFileOpened(urlIsValid = false)
    assertThat(
      readerMenuState.menuItems.any { it.testingTag == SHARE_ARTICLE_MENU_ITEM_TESTING_TAG }
    ).isFalse()
  }
}
