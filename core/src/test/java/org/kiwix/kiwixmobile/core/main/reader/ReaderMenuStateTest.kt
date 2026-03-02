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
import org.kiwix.kiwixmobile.core.page.SEARCH_ICON_TESTING_TAG

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

  private fun hasMenuItem(tag: String) =
    readerMenuState.menuItems.any { it.testingTag == tag }

  private fun findMenuItem(tag: String) =
    readerMenuState.menuItems.first { it.testingTag == tag }

  // Search menu item tests

  @Test
  internal fun `search menu item is present when url is valid`() {
    assertThat(hasMenuItem(SEARCH_ICON_TESTING_TAG)).isTrue()
  }

  @Test
  internal fun `search menu item is not in overflow menu`() {
    assertThat(findMenuItem(SEARCH_ICON_TESTING_TAG).isInOverflow).isFalse()
  }

  @Test
  internal fun `search menu item click invokes onSearchMenuClicked`() {
    findMenuItem(SEARCH_ICON_TESTING_TAG).onClick()
    verify { menuClickListener.onSearchMenuClickedMenuClicked() }
  }

  @Test
  internal fun `search menu item is hidden when url is not valid`() {
    val state = ReaderMenuState(
      menuClickListener = menuClickListener,
      isUrlValidInitially = false
    )
    assertThat(state.menuItems.any { it.testingTag == SEARCH_ICON_TESTING_TAG }).isFalse()
  }

  @Test
  internal fun `search menu item is hidden when search is disabled`() {
    val state = ReaderMenuState(
      menuClickListener = menuClickListener,
      isUrlValidInitially = true,
      disableSearch = true
    )
    assertThat(state.menuItems.any { it.testingTag == SEARCH_ICON_TESTING_TAG }).isFalse()
  }

  @Test
  internal fun `search menu item remains visible in tab switcher mode`() {
    readerMenuState.showTabSwitcherOptions()
    assertThat(hasMenuItem(SEARCH_ICON_TESTING_TAG)).isTrue()
  }

  @Test
  internal fun `search menu item is hidden when book specific items are hidden`() {
    readerMenuState.hideBookSpecificMenuItems()
    assertThat(hasMenuItem(SEARCH_ICON_TESTING_TAG)).isFalse()
  }

  @Test
  internal fun `search menu item reappears when book specific items are shown`() {
    readerMenuState.hideBookSpecificMenuItems()
    readerMenuState.showBookSpecificMenuItems()
    assertThat(hasMenuItem(SEARCH_ICON_TESTING_TAG)).isTrue()
  }

  // Tab switcher menu item tests

  @Test
  internal fun `tab menu item is present when url is valid and webViewCount is greater than 0`() {
    readerMenuState.updateTabIcon(1)
    assertThat(hasMenuItem(TAB_MENU_ITEM_TESTING_TAG)).isTrue()
  }

  @Test
  internal fun `tab menu item is hidden when webViewCount is 0`() {
    readerMenuState.updateTabIcon(0)
    assertThat(hasMenuItem(TAB_MENU_ITEM_TESTING_TAG)).isFalse()
  }

  @Test
  internal fun `tab menu item is not in overflow menu`() {
    readerMenuState.updateTabIcon(1)
    assertThat(findMenuItem(TAB_MENU_ITEM_TESTING_TAG).isInOverflow).isFalse()
  }

  @Test
  internal fun `tab menu item click invokes onTabMenuClicked`() {
    readerMenuState.updateTabIcon(1)
    findMenuItem(TAB_MENU_ITEM_TESTING_TAG).onClick()
    verify { menuClickListener.onTabMenuClicked() }
  }

  @Test
  internal fun `tab menu item shows smiley when count exceeds 99`() {
    readerMenuState.updateTabIcon(100)
    assertThat(findMenuItem(TAB_MENU_ITEM_TESTING_TAG).iconButtonText).isEqualTo(":D")
  }

  @Test
  internal fun `tab menu item shows count as text when count is 99 or less`() {
    readerMenuState.updateTabIcon(5)
    assertThat(findMenuItem(TAB_MENU_ITEM_TESTING_TAG).iconButtonText).isEqualTo("5")
  }

  @Test
  internal fun `tab menu item is hidden when tabs are disabled`() {
    val state = ReaderMenuState(
      menuClickListener = menuClickListener,
      isUrlValidInitially = true,
      disableTabs = true
    )
    state.updateTabIcon(3)
    assertThat(state.menuItems.any { it.testingTag == TAB_MENU_ITEM_TESTING_TAG }).isFalse()
  }

  @Test
  internal fun `tab menu item is hidden when url is not valid`() {
    val state = ReaderMenuState(
      menuClickListener = menuClickListener,
      isUrlValidInitially = false
    )
    state.updateTabIcon(3)
    assertThat(state.menuItems.any { it.testingTag == TAB_MENU_ITEM_TESTING_TAG }).isFalse()
  }

  // Share menu item tests

  @Test
  internal fun `share menu item is present when url is valid`() {
    assertThat(hasMenuItem(SHARE_ARTICLE_MENU_ITEM_TESTING_TAG)).isTrue()
  }

  @Test
  internal fun `share menu item is in overflow menu`() {
    assertThat(findMenuItem(SHARE_ARTICLE_MENU_ITEM_TESTING_TAG).isInOverflow).isTrue()
  }

  @Test
  internal fun `share menu item click invokes onShareMenuClicked`() {
    findMenuItem(SHARE_ARTICLE_MENU_ITEM_TESTING_TAG).onClick()
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
    assertThat(hasMenuItem(SHARE_ARTICLE_MENU_ITEM_TESTING_TAG)).isFalse()
  }

  @Test
  internal fun `share menu item stays hidden after just hiding tab switcher`() {
    readerMenuState.showTabSwitcherOptions()
    readerMenuState.hideTabSwitcher()
    assertThat(hasMenuItem(SHARE_ARTICLE_MENU_ITEM_TESTING_TAG)).isFalse()
  }

  @Test
  internal fun `share menu item reappears after showing web view options`() {
    readerMenuState.showTabSwitcherOptions()
    readerMenuState.showWebViewOptions(true)
    assertThat(hasMenuItem(SHARE_ARTICLE_MENU_ITEM_TESTING_TAG)).isTrue()
  }

  @Test
  internal fun `share menu item is hidden when book specific items are hidden`() {
    readerMenuState.hideBookSpecificMenuItems()
    assertThat(hasMenuItem(SHARE_ARTICLE_MENU_ITEM_TESTING_TAG)).isFalse()
  }

  @Test
  internal fun `share menu item reappears when book specific items are shown`() {
    readerMenuState.hideBookSpecificMenuItems()
    readerMenuState.showBookSpecificMenuItems()
    assertThat(hasMenuItem(SHARE_ARTICLE_MENU_ITEM_TESTING_TAG)).isTrue()
  }

  @Test
  internal fun `share menu item reappears after file opened with valid url`() {
    readerMenuState.hideBookSpecificMenuItems()
    readerMenuState.onFileOpened(urlIsValid = true)
    assertThat(hasMenuItem(SHARE_ARTICLE_MENU_ITEM_TESTING_TAG)).isTrue()
  }

  @Test
  internal fun `share menu item is hidden after file opened with invalid url`() {
    readerMenuState.onFileOpened(urlIsValid = false)
    assertThat(hasMenuItem(SHARE_ARTICLE_MENU_ITEM_TESTING_TAG)).isFalse()
  }

  // Add note menu item tests

  @Test
  internal fun `add note menu item is present when url is valid`() {
    assertThat(hasMenuItem(TAKE_NOTE_MENU_ITEM_TESTING_TAG)).isTrue()
  }

  @Test
  internal fun `add note menu item is in overflow menu`() {
    assertThat(findMenuItem(TAKE_NOTE_MENU_ITEM_TESTING_TAG).isInOverflow).isTrue()
  }

  @Test
  internal fun `add note menu item click invokes onAddNoteMenuClicked`() {
    findMenuItem(TAKE_NOTE_MENU_ITEM_TESTING_TAG).onClick()
    verify { menuClickListener.onAddNoteMenuClicked() }
  }

  @Test
  internal fun `add note menu item is hidden when url is not valid`() {
    val state = ReaderMenuState(
      menuClickListener = menuClickListener,
      isUrlValidInitially = false
    )
    assertThat(
      state.menuItems.any { it.testingTag == TAKE_NOTE_MENU_ITEM_TESTING_TAG }
    ).isFalse()
  }

  @Test
  internal fun `add note menu item is hidden in tab switcher mode`() {
    readerMenuState.showTabSwitcherOptions()
    assertThat(hasMenuItem(TAKE_NOTE_MENU_ITEM_TESTING_TAG)).isFalse()
  }

  @Test
  internal fun `add note menu item is hidden when book specific items are hidden`() {
    readerMenuState.hideBookSpecificMenuItems()
    assertThat(hasMenuItem(TAKE_NOTE_MENU_ITEM_TESTING_TAG)).isFalse()
  }

  @Test
  internal fun `add note menu item reappears when book specific items are shown`() {
    readerMenuState.hideBookSpecificMenuItems()
    readerMenuState.showBookSpecificMenuItems()
    assertThat(hasMenuItem(TAKE_NOTE_MENU_ITEM_TESTING_TAG)).isTrue()
  }

  @Test
  internal fun `add note menu item reappears after file opened with valid url`() {
    readerMenuState.hideBookSpecificMenuItems()
    readerMenuState.onFileOpened(urlIsValid = true)
    assertThat(hasMenuItem(TAKE_NOTE_MENU_ITEM_TESTING_TAG)).isTrue()
  }

  @Test
  internal fun `add note menu item is hidden after file opened with invalid url`() {
    readerMenuState.onFileOpened(urlIsValid = false)
    assertThat(hasMenuItem(TAKE_NOTE_MENU_ITEM_TESTING_TAG)).isFalse()
  }

  // Random article menu item tests

  @Test
  internal fun `random article menu item is present when url is valid`() {
    assertThat(hasMenuItem(RANDOM_ARTICLE_MENU_ITEM_TESTING_TAG)).isTrue()
  }

  @Test
  internal fun `random article menu item is in overflow menu`() {
    assertThat(findMenuItem(RANDOM_ARTICLE_MENU_ITEM_TESTING_TAG).isInOverflow).isTrue()
  }

  @Test
  internal fun `random article menu item click invokes onRandomArticleMenuClicked`() {
    findMenuItem(RANDOM_ARTICLE_MENU_ITEM_TESTING_TAG).onClick()
    verify { menuClickListener.onRandomArticleMenuClicked() }
  }

  @Test
  internal fun `random article menu item is hidden when url is not valid`() {
    val state = ReaderMenuState(
      menuClickListener = menuClickListener,
      isUrlValidInitially = false
    )
    assertThat(
      state.menuItems.any { it.testingTag == RANDOM_ARTICLE_MENU_ITEM_TESTING_TAG }
    ).isFalse()
  }

  @Test
  internal fun `random article menu item is hidden in tab switcher mode`() {
    readerMenuState.showTabSwitcherOptions()
    assertThat(hasMenuItem(RANDOM_ARTICLE_MENU_ITEM_TESTING_TAG)).isFalse()
  }

  @Test
  internal fun `random article menu item is hidden when book specific items are hidden`() {
    readerMenuState.hideBookSpecificMenuItems()
    assertThat(hasMenuItem(RANDOM_ARTICLE_MENU_ITEM_TESTING_TAG)).isFalse()
  }

  @Test
  internal fun `random article menu item reappears when book specific items are shown`() {
    readerMenuState.hideBookSpecificMenuItems()
    readerMenuState.showBookSpecificMenuItems()
    assertThat(hasMenuItem(RANDOM_ARTICLE_MENU_ITEM_TESTING_TAG)).isTrue()
  }

  // Read aloud menu item tests

  @Test
  internal fun `read aloud menu item is present when url is valid`() {
    assertThat(hasMenuItem(READ_ALOUD_MENU_ITEM_TESTING_TAG)).isTrue()
  }

  @Test
  internal fun `read aloud menu item is in overflow menu`() {
    assertThat(findMenuItem(READ_ALOUD_MENU_ITEM_TESTING_TAG).isInOverflow).isTrue()
  }

  @Test
  internal fun `read aloud menu item click invokes onReadAloudMenuClicked`() {
    findMenuItem(READ_ALOUD_MENU_ITEM_TESTING_TAG).onClick()
    verify { menuClickListener.onReadAloudMenuClicked() }
  }

  @Test
  internal fun `read aloud menu item is hidden when url is not valid`() {
    val state = ReaderMenuState(
      menuClickListener = menuClickListener,
      isUrlValidInitially = false
    )
    assertThat(
      state.menuItems.any { it.testingTag == READ_ALOUD_MENU_ITEM_TESTING_TAG }
    ).isFalse()
  }

  @Test
  internal fun `read aloud menu item is hidden when read aloud is disabled`() {
    val state = ReaderMenuState(
      menuClickListener = menuClickListener,
      isUrlValidInitially = true,
      disableReadAloud = true
    )
    assertThat(
      state.menuItems.any { it.testingTag == READ_ALOUD_MENU_ITEM_TESTING_TAG }
    ).isFalse()
  }

  @Test
  internal fun `read aloud menu item is hidden in tab switcher mode`() {
    readerMenuState.showTabSwitcherOptions()
    assertThat(hasMenuItem(READ_ALOUD_MENU_ITEM_TESTING_TAG)).isFalse()
  }

  @Test
  internal fun `read aloud menu item is hidden when book specific items are hidden`() {
    readerMenuState.hideBookSpecificMenuItems()
    assertThat(hasMenuItem(READ_ALOUD_MENU_ITEM_TESTING_TAG)).isFalse()
  }

  @Test
  internal fun `read aloud menu item reappears when book specific items are shown`() {
    readerMenuState.hideBookSpecificMenuItems()
    readerMenuState.showBookSpecificMenuItems()
    assertThat(hasMenuItem(READ_ALOUD_MENU_ITEM_TESTING_TAG)).isTrue()
  }

  @Test
  internal fun `read aloud menu item remains after text to speech started`() {
    readerMenuState.onTextToSpeechStarted()
    assertThat(hasMenuItem(READ_ALOUD_MENU_ITEM_TESTING_TAG)).isTrue()
  }

  @Test
  internal fun `read aloud menu item remains after text to speech stopped`() {
    readerMenuState.onTextToSpeechStarted()
    readerMenuState.onTextToSpeechStopped()
    assertThat(hasMenuItem(READ_ALOUD_MENU_ITEM_TESTING_TAG)).isTrue()
  }

  // Aggregate visibility tests

  @Test
  internal fun `no menu items are present when url is not valid`() {
    val state = ReaderMenuState(
      menuClickListener = menuClickListener,
      isUrlValidInitially = false
    )
    assertThat(state.menuItems).isEmpty()
  }

  @Test
  internal fun `all menu items are hidden when book specific items are hidden`() {
    readerMenuState.hideBookSpecificMenuItems()
    assertThat(readerMenuState.menuItems).isEmpty()
  }

  @Test
  internal fun `all overflow menu items are hidden in tab switcher mode`() {
    readerMenuState.showTabSwitcherOptions()
    assertThat(hasMenuItem(SHARE_ARTICLE_MENU_ITEM_TESTING_TAG)).isFalse()
    assertThat(hasMenuItem(TAKE_NOTE_MENU_ITEM_TESTING_TAG)).isFalse()
    assertThat(hasMenuItem(RANDOM_ARTICLE_MENU_ITEM_TESTING_TAG)).isFalse()
    assertThat(hasMenuItem(READ_ALOUD_MENU_ITEM_TESTING_TAG)).isFalse()
  }
}
