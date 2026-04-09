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

package org.kiwix.kiwixmobile.page.history

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.page.DELETE_MENU_ICON_TESTING_TAG
import org.kiwix.kiwixmobile.core.page.NO_ITEMS_TEXT_TESTING_TAG
import org.kiwix.kiwixmobile.core.page.history.NavigationHistoryDialogScreen
import org.kiwix.kiwixmobile.core.page.history.models.NavigationHistoryListItem
import org.kiwix.kiwixmobile.core.ui.components.NAVIGATION_ICON_TESTING_TAG
import org.kiwix.kiwixmobile.core.ui.components.NavigationIcon
import org.kiwix.kiwixmobile.core.ui.components.TOOLBAR_TITLE_TESTING_TAG
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.ui.models.IconItem

/**
 * UI tests for [NavigationHistoryDialogScreen] Compose components.
 * Covers: empty state, populated list rendering, item details,
 * click interactions, app bar integration, and action menu state.
 */
@RunWith(AndroidJUnit4::class)
class NavigationHistoryDialogScreenTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  private val context =
    InstrumentationRegistry.getInstrumentation().targetContext

  @Test
  fun emptyState_displaysNoHistoryText() {
    composeTestRule.setContent {
      NavigationHistoryDialogScreen(
        titleId = R.string.backward_history,
        navigationHistoryList = mutableListOf(),
        actionMenuItems = createDeleteMenuItem(isEnabled = false),
        onNavigationItemClick = {},
        navigationIcon = { NavigationIcon(onClick = {}) }
      )
    }
    composeTestRule.onNodeWithTag(NO_ITEMS_TEXT_TESTING_TAG)
      .assertIsDisplayed()
      .assertTextEquals(context.getString(R.string.no_history))
  }

  @Test
  fun emptyState_doesNotShowListItems() {
    composeTestRule.setContent {
      NavigationHistoryDialogScreen(
        titleId = R.string.backward_history,
        navigationHistoryList = mutableListOf(),
        actionMenuItems = createDeleteMenuItem(isEnabled = false),
        onNavigationItemClick = {},
        navigationIcon = { NavigationIcon(onClick = {}) }
      )
    }
    // "No History" text should be visible; no list item content should exist.
    composeTestRule.onNodeWithTag(NO_ITEMS_TEXT_TESTING_TAG).assertIsDisplayed()
    composeTestRule
      .onNodeWithContentDescription("Page Title 00")
      .assertDoesNotExist()
  }

  @Test
  fun populatedList_rendersExpectedNumberOfItems() {
    val items = createTestHistoryItems(ITEM_COUNT_FIVE)
    composeTestRule.setContent {
      NavigationHistoryDialogScreen(
        titleId = R.string.backward_history,
        navigationHistoryList = items.toMutableList(),
        actionMenuItems = createDeleteMenuItem(isEnabled = true),
        onNavigationItemClick = {},
        navigationIcon = { NavigationIcon(onClick = {}) }
      )
    }
    // Each NavigationHistoryItem has Text with
    // contentDescription = "${item.title}$index"
    items.forEachIndexed { index, item ->
      composeTestRule
        .onNodeWithContentDescription("${item.title}$index")
        .assertIsDisplayed()
    }
  }

  @Test
  fun populatedList_doesNotShowNoHistoryText() {
    val items = createTestHistoryItems(ITEM_COUNT_THREE)
    composeTestRule.setContent {
      NavigationHistoryDialogScreen(
        titleId = R.string.backward_history,
        navigationHistoryList = items.toMutableList(),
        actionMenuItems = createDeleteMenuItem(isEnabled = true),
        onNavigationItemClick = {},
        navigationIcon = { NavigationIcon(onClick = {}) }
      )
    }
    composeTestRule.onNodeWithTag(NO_ITEMS_TEXT_TESTING_TAG).assertDoesNotExist()
  }

  @Test
  fun itemDetails_displaysPageTitleAndFavicon() {
    val items = mutableListOf(
      NavigationHistoryListItem("Kiwix Article", "https://example.com/kiwix")
    )
    composeTestRule.setContent {
      NavigationHistoryDialogScreen(
        titleId = R.string.backward_history,
        navigationHistoryList = items,
        actionMenuItems = createDeleteMenuItem(isEnabled = true),
        onNavigationItemClick = {},
        navigationIcon = { NavigationIcon(onClick = {}) }
      )
    }
    // Verify the page title text is displayed
    composeTestRule.onNodeWithText("Kiwix Article").assertIsDisplayed()

    // Verify the favicon image is displayed.
    // contentDescription is "Favicon" + index (from stringResource(R.string.fav_icon) + index)
    val faviconDescription = context.getString(R.string.fav_icon) + "0"
    composeTestRule.onNodeWithContentDescription(faviconDescription).assertIsDisplayed()
  }

  @Test
  fun itemDetails_displaysCorrectTitlesForMultipleItems() {
    val items = mutableListOf(
      NavigationHistoryListItem("First Page", "https://example.com/1"),
      NavigationHistoryListItem("Second Page", "https://example.com/2"),
      NavigationHistoryListItem("Third Page", "https://example.com/3")
    )
    composeTestRule.setContent {
      NavigationHistoryDialogScreen(
        titleId = R.string.backward_history,
        navigationHistoryList = items,
        actionMenuItems = createDeleteMenuItem(isEnabled = true),
        onNavigationItemClick = {},
        navigationIcon = { NavigationIcon(onClick = {}) }
      )
    }
    composeTestRule.onNodeWithText("First Page").assertIsDisplayed()
    composeTestRule.onNodeWithText("Second Page").assertIsDisplayed()
    composeTestRule.onNodeWithText("Third Page").assertIsDisplayed()
  }

  @Test
  fun clickItem_triggersOnNavigationItemClickWithCorrectData() {
    val items = mutableListOf(
      NavigationHistoryListItem("Clicked Page", "https://example.com/clicked")
    )
    var clickedItem: NavigationHistoryListItem? = null
    composeTestRule.setContent {
      NavigationHistoryDialogScreen(
        titleId = R.string.backward_history,
        navigationHistoryList = items,
        actionMenuItems = createDeleteMenuItem(isEnabled = true),
        onNavigationItemClick = { clickedItem = it },
        navigationIcon = { NavigationIcon(onClick = {}) }
      )
    }
    composeTestRule.onNodeWithText("Clicked Page").performClick()
    assertEquals(items[0], clickedItem)
  }

  @Test
  fun clickItem_passesCorrectItemDataForSecondItem() {
    val items = mutableListOf(
      NavigationHistoryListItem("First", "https://example.com/1"),
      NavigationHistoryListItem("Second", "https://example.com/2")
    )
    var clickedItem: NavigationHistoryListItem? = null
    composeTestRule.setContent {
      NavigationHistoryDialogScreen(
        titleId = R.string.backward_history,
        navigationHistoryList = items,
        actionMenuItems = createDeleteMenuItem(isEnabled = true),
        onNavigationItemClick = { clickedItem = it },
        navigationIcon = { NavigationIcon(onClick = {}) }
      )
    }
    composeTestRule.onNodeWithText("Second").performClick()
    assertEquals(items[1], clickedItem)
    assertEquals("Second", clickedItem?.title)
    assertEquals("https://example.com/2", clickedItem?.pageUrl)
  }

  @Test
  fun appBar_displaysCorrectTitle() {
    composeTestRule.setContent {
      NavigationHistoryDialogScreen(
        titleId = R.string.backward_history,
        navigationHistoryList = mutableListOf(),
        actionMenuItems = emptyList(),
        onNavigationItemClick = {},
        navigationIcon = { NavigationIcon(onClick = {}) }
      )
    }
    composeTestRule.onNodeWithTag(TOOLBAR_TITLE_TESTING_TAG, useUnmergedTree = true)
      .assertIsDisplayed()
      .assertTextEquals(context.getString(R.string.backward_history))
  }

  @Test
  fun appBar_displaysForwardHistoryTitle() {
    composeTestRule.setContent {
      NavigationHistoryDialogScreen(
        titleId = R.string.forward_history,
        navigationHistoryList = mutableListOf(),
        actionMenuItems = emptyList(),
        onNavigationItemClick = {},
        navigationIcon = { NavigationIcon(onClick = {}) }
      )
    }
    composeTestRule.onNodeWithTag(TOOLBAR_TITLE_TESTING_TAG, useUnmergedTree = true)
      .assertIsDisplayed()
      .assertTextEquals(context.getString(R.string.forward_history))
  }

  @Test
  fun appBar_navigationIconClick_triggersCallback() {
    var navigationIconClicked = false
    composeTestRule.setContent {
      NavigationHistoryDialogScreen(
        titleId = R.string.backward_history,
        navigationHistoryList = mutableListOf(),
        actionMenuItems = emptyList(),
        onNavigationItemClick = {},
        navigationIcon = {
          NavigationIcon(onClick = { navigationIconClicked = true })
        }
      )
    }
    composeTestRule.onNodeWithTag(NAVIGATION_ICON_TESTING_TAG).performClick()
    assertTrue(navigationIconClicked)
  }

  @Test
  fun actionMenuClearAll_isEnabledWhenListHasData() {
    val items = mutableListOf(
      NavigationHistoryListItem("Page 1", "https://example.com/1")
    )
    composeTestRule.setContent {
      NavigationHistoryDialogScreen(
        titleId = R.string.backward_history,
        navigationHistoryList = items,
        actionMenuItems = createDeleteMenuItem(isEnabled = items.isNotEmpty()),
        onNavigationItemClick = {},
        navigationIcon = { NavigationIcon(onClick = {}) }
      )
    }
    composeTestRule.onNodeWithTag(DELETE_MENU_ICON_TESTING_TAG)
      .assertIsDisplayed()
      .assertIsEnabled()
  }

  @Test
  fun actionMenuClearAll_isDisabledWhenListIsEmpty() {
    val items = mutableListOf<NavigationHistoryListItem>()
    composeTestRule.setContent {
      NavigationHistoryDialogScreen(
        titleId = R.string.backward_history,
        navigationHistoryList = items,
        actionMenuItems = createDeleteMenuItem(isEnabled = items.isNotEmpty()),
        onNavigationItemClick = {},
        navigationIcon = { NavigationIcon(onClick = {}) }
      )
    }
    composeTestRule.onNodeWithTag(DELETE_MENU_ICON_TESTING_TAG)
      .assertIsDisplayed()
      .assertIsNotEnabled()
  }

  @Test
  fun actionMenuClearAll_clickTriggersCallback() {
    var clearAllClicked = false
    composeTestRule.setContent {
      NavigationHistoryDialogScreen(
        titleId = R.string.backward_history,
        navigationHistoryList = mutableListOf(
          NavigationHistoryListItem("Page", "https://example.com")
        ),
        actionMenuItems = listOf(
          ActionMenuItem(
            icon = IconItem.Drawable(R.drawable.ic_delete_white_24dp),
            contentDescription = R.string.pref_clear_all_history_title,
            onClick = { clearAllClicked = true },
            isEnabled = true,
            testingTag = DELETE_MENU_ICON_TESTING_TAG
          )
        ),
        onNavigationItemClick = {},
        navigationIcon = { NavigationIcon(onClick = {}) }
      )
    }
    composeTestRule.onNodeWithTag(DELETE_MENU_ICON_TESTING_TAG).performClick()
    assertTrue(clearAllClicked)
  }

  private fun createTestHistoryItems(
    count: Int
  ): List<NavigationHistoryListItem> =
    (0 until count).map { i ->
      NavigationHistoryListItem(
        "Page Title $i",
        "https://example.com/page$i"
      )
    }

  private fun createDeleteMenuItem(
    isEnabled: Boolean,
    onClick: () -> Unit = {}
  ): List<ActionMenuItem> = listOf(
    ActionMenuItem(
      icon = IconItem.Drawable(R.drawable.ic_delete_white_24dp),
      contentDescription = R.string.pref_clear_all_history_title,
      onClick = onClick,
      isEnabled = isEnabled,
      testingTag = DELETE_MENU_ICON_TESTING_TAG
    )
  )

  companion object {
    private const val ITEM_COUNT_THREE = 3
    private const val ITEM_COUNT_FIVE = 5
  }
}
