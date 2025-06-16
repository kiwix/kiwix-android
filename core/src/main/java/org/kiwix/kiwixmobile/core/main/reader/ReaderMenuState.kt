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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.page.SEARCH_ICON_TESTING_TAG
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.ui.models.IconItem

const val READ_ALOUD_MENU_ITEM_TESTING_TAG = "readAloudMenuItemTestingTag"
const val TAKE_NOTE_MENU_ITEM_TESTING_TAG = "takeNoteMenuItemTestingTag"
const val FULL_SCREEN_MENU_ITEM_TESTING_TAG = "fullScreenMenuItemTestingTag"
const val RANDOM_ARTICLE_MENU_ITEM_TESTING_TAG = "randomArticleMenuItemTestingTag"
const val TAB_MENU_ITEM_TESTING_TAG = "tabMenuItemTestingTag"

@Stable
class ReaderMenuState(
  private val menuClickListener: MenuClickListener,
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

  var isInTabSwitcher by mutableStateOf(false)
    private set

  var isReadingAloud by mutableStateOf(false)
    private set

  var webViewCount by mutableStateOf(0)
  var urlIsValid by mutableStateOf(false)
  var zimFileReaderAvailable by mutableStateOf(false)

  fun onTabsChanged(count: Int) {
    webViewCount = count
  }

  fun onUrlValidityChanged(valid: Boolean) {
    urlIsValid = valid
  }

  fun onZimFileReaderAvailable(available: Boolean) {
    zimFileReaderAvailable = available
  }

  fun onTextToSpeechStarted() {
    isReadingAloud = true
  }

  fun onTextToSpeechStopped() {
    isReadingAloud = false
  }

  fun exitTabSwitcher() {
    isInTabSwitcher = false
  }

  @Suppress("LongMethod", "MagicNumber")
  fun getActionMenuItems(): List<ActionMenuItem> {
    if (isInTabSwitcher) {
      return emptyList()
    }

    val list = mutableListOf<ActionMenuItem>()

    if (!disableSearch && urlIsValid) {
      list += ActionMenuItem(
        icon = IconItem.Drawable(R.drawable.action_search),
        contentDescription = R.string.search_label,
        onClick = { menuClickListener.onSearchMenuClickedMenuClicked() },
        isInOverflow = false,
        testingTag = SEARCH_ICON_TESTING_TAG
      )
    }

    if (!disableTabs) {
      val tabLabel = if (webViewCount > 99) ":D" else "$webViewCount"
      list += ActionMenuItem(
        icon = IconItem.Vector(Icons.Default.Add),
        contentDescription = R.string.switch_tabs,
        onClick = {
          isInTabSwitcher = true
          menuClickListener.onTabMenuClicked()
        },
        isInOverflow = false,
        iconButtonText = tabLabel,
        testingTag = TAB_MENU_ITEM_TESTING_TAG
      )
    }

    if (urlIsValid) {
      list += listOf(
        ActionMenuItem(
          icon = IconItem.Drawable(R.drawable.ic_add_note),
          contentDescription = R.string.take_notes,
          onClick = { menuClickListener.onAddNoteMenuClicked() },
          testingTag = TAKE_NOTE_MENU_ITEM_TESTING_TAG
        ),
        ActionMenuItem(
          contentDescription = R.string.menu_random_article,
          onClick = { menuClickListener.onRandomArticleMenuClicked() },
          testingTag = RANDOM_ARTICLE_MENU_ITEM_TESTING_TAG
        ),
        ActionMenuItem(
          contentDescription = R.string.menu_full_screen,
          onClick = { menuClickListener.onFullscreenMenuClicked() },
          testingTag = FULL_SCREEN_MENU_ITEM_TESTING_TAG
        )
      )

      if (!disableReadAloud) {
        list += ActionMenuItem(
          contentDescription = if (isReadingAloud) R.string.menu_read_aloud_stop else R.string.menu_read_aloud,
          onClick = {
            isReadingAloud = !isReadingAloud
            menuClickListener.onReadAloudMenuClicked()
          },
          testingTag = READ_ALOUD_MENU_ITEM_TESTING_TAG
        )
      }
    }

    return list
  }
}
