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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.page.SEARCH_ICON_TESTING_TAG
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.ui.theme.Black
import org.kiwix.kiwixmobile.core.ui.theme.White
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.MATERIAL_MINIMUM_HEIGHT_AND_WIDTH
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.ONE_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SIX_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.TAB_SWITCHER_ICON_CORNER_RADIUS
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.TAB_SWITCHER_TEXT_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.TWELVE_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.TWENTY_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.TWO_DP

const val READ_ALOUD_MENU_ITEM_TESTING_TAG = "readAloudMenuItemTestingTag"
const val TAKE_NOTE_MENU_ITEM_TESTING_TAG = "takeNoteMenuItemTestingTag"
const val FULL_SCREEN_MENU_ITEM_TESTING_TAG = "fullScreenMenuItemTestingTag"
const val RANDOM_ARTICLE_MENU_ITEM_TESTING_TAG = "randomArticleMenuItemTestingTag"
const val TAB_MENU_ITEM_TESTING_TAG = "tabMenuItemTestingTag"

@Stable
class ReaderMenuState(
  private val menuClickListener: MenuClickListener,
  isUrlValidInitially: Boolean,
  private val disableReadAloud: Boolean = false,
  private val disableTabs: Boolean = false,
  private val disableSearch: Boolean = false
) {
  interface MenuClickListener {
    fun onTabMenuClicked()
    fun onHomeMenuClicked()
    fun onAddNoteMenuClicked()
    fun onRandomArticleMenuClicked()
    fun onReadAloudMenuClicked()
    fun onFullscreenMenuClicked()
    fun onSearchMenuClickedMenuClicked()
  }

  val menuItems = mutableStateListOf<ActionMenuItem>()

  private val menuItemVisibility = mutableMapOf<MenuItemType, Boolean>().apply {
    put(MenuItemType.Search, true)
    put(MenuItemType.TabSwitcher, true)
    put(MenuItemType.AddNote, true)
    put(MenuItemType.RandomArticle, true)
    put(MenuItemType.Fullscreen, true)
    put(MenuItemType.ReadAloud, true)
  }

  var isInTabSwitcher by mutableStateOf(false)
    private set

  private var isReadingAloud by mutableStateOf(false)

  private var webViewCount by mutableStateOf(0)
  private var urlIsValid by mutableStateOf(false)

  fun updateTabIcon(count: Int) {
    webViewCount = count
    updateMenuItems()
  }

  init {
    showWebViewOptions(isUrlValidInitially)
  }

  fun showWebViewOptions(valid: Boolean) {
    hideTabSwitcher()
    urlIsValid = valid
    setVisibility(
      urlIsValid,
      MenuItemType.RandomArticle,
      MenuItemType.Search,
      MenuItemType.ReadAloud,
      MenuItemType.Fullscreen,
      MenuItemType.AddNote,
      MenuItemType.TabSwitcher
    )
  }

  fun onFileOpened(urlIsValid: Boolean) {
    showWebViewOptions(urlIsValid)
  }

  fun onTextToSpeechStarted() {
    isReadingAloud = true
    updateMenuItems()
  }

  fun onTextToSpeechStopped() {
    isReadingAloud = false
    updateMenuItems()
  }

  fun hideBookSpecificMenuItems() {
    setVisibility(
      false,
      MenuItemType.Search,
      MenuItemType.TabSwitcher,
      MenuItemType.RandomArticle,
      MenuItemType.AddNote,
      MenuItemType.ReadAloud
    )
  }

  fun showBookSpecificMenuItems() {
    setVisibility(
      true,
      MenuItemType.Search,
      MenuItemType.TabSwitcher,
      MenuItemType.RandomArticle,
      MenuItemType.AddNote,
      MenuItemType.ReadAloud
    )
  }

  fun showTabSwitcherOptions() {
    isInTabSwitcher = true
    setVisibility(
      false,
      MenuItemType.RandomArticle,
      MenuItemType.ReadAloud,
      MenuItemType.AddNote,
      MenuItemType.Fullscreen
    )
  }

  fun hideTabSwitcher() {
    isInTabSwitcher = false
    updateMenuItems()
  }

  private fun updateMenuItems() {
    menuItems.clear()
    addSearchMenuItem()
    addTabMenuItem()
    addReaderMenuItems()
  }

  private fun addSearchMenuItem() {
    if (menuItemVisibility[MenuItemType.Search] == true && !disableSearch && urlIsValid) {
      menuItems += ActionMenuItem(
        icon = IconItem.Drawable(R.drawable.action_search),
        contentDescription = R.string.search_label,
        onClick = { menuClickListener.onSearchMenuClickedMenuClicked() },
        isInOverflow = false,
        testingTag = SEARCH_ICON_TESTING_TAG
      )
    }
  }

  private fun addTabMenuItem() {
    if (!disableTabs && urlIsValid && webViewCount > 0) {
      val tabLabel = if (webViewCount > 99) ":D" else "$webViewCount"
      menuItems += ActionMenuItem(
        icon = null,
        contentDescription = R.string.switch_tabs,
        onClick = { menuClickListener.onTabMenuClicked() },
        isInOverflow = false,
        iconButtonText = tabLabel,
        testingTag = TAB_MENU_ITEM_TESTING_TAG,
        customView = { TabSwitcherBadge(tabLabel = tabLabel) }
      )
    }
  }

  @Composable
  fun TabSwitcherBadge(tabLabel: String, modifier: Modifier = Modifier) {
    Box(
      modifier = modifier
        .size(MATERIAL_MINIMUM_HEIGHT_AND_WIDTH)
        .padding(TWELVE_DP),
      contentAlignment = Alignment.Center
    ) {
      Box(
        modifier = modifier
          .clip(RoundedCornerShape(TAB_SWITCHER_ICON_CORNER_RADIUS))
          .background(Black)
          .border(ONE_DP, White, RoundedCornerShape(TAB_SWITCHER_ICON_CORNER_RADIUS))
          .padding(horizontal = SIX_DP, vertical = TWO_DP)
          .defaultMinSize(minWidth = TWENTY_DP, minHeight = TWENTY_DP),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = tabLabel,
          color = White,
          fontWeight = FontWeight.Bold,
          fontSize = TAB_SWITCHER_TEXT_SIZE,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }

  private fun addReaderMenuItems() {
    if (menuItemVisibility[MenuItemType.AddNote] == true) {
      menuItems += ActionMenuItem(
        contentDescription = R.string.take_notes,
        onClick = { menuClickListener.onAddNoteMenuClicked() },
        testingTag = TAKE_NOTE_MENU_ITEM_TESTING_TAG,
        isInOverflow = true
      )
    }

    if (menuItemVisibility[MenuItemType.RandomArticle] == true) {
      menuItems += ActionMenuItem(
        contentDescription = R.string.menu_random_article,
        onClick = { menuClickListener.onRandomArticleMenuClicked() },
        testingTag = RANDOM_ARTICLE_MENU_ITEM_TESTING_TAG,
        isInOverflow = true
      )
    }

    if (menuItemVisibility[MenuItemType.Fullscreen] == true) {
      menuItems += ActionMenuItem(
        contentDescription = R.string.menu_full_screen,
        onClick = { menuClickListener.onFullscreenMenuClicked() },
        testingTag = FULL_SCREEN_MENU_ITEM_TESTING_TAG,
        isInOverflow = true
      )
    }

    if (menuItemVisibility[MenuItemType.ReadAloud] == true && !disableReadAloud) {
      menuItems += ActionMenuItem(
        contentDescription = if (isReadingAloud) R.string.menu_read_aloud_stop else R.string.menu_read_aloud,
        onClick = {
          isReadingAloud = !isReadingAloud
          menuClickListener.onReadAloudMenuClicked()
        },
        testingTag = READ_ALOUD_MENU_ITEM_TESTING_TAG,
        isInOverflow = true
      )
    }
  }

  private fun setVisibility(visible: Boolean, vararg types: MenuItemType) {
    types.forEach {
      if (it == MenuItemType.Search && disableSearch) {
        menuItemVisibility[it] = false
      } else {
        menuItemVisibility[it] = visible
      }
    }
    updateMenuItems()
  }
}

enum class MenuItemType {
  Search,
  TabSwitcher,
  AddNote,
  RandomArticle,
  Fullscreen,
  ReadAloud
}
