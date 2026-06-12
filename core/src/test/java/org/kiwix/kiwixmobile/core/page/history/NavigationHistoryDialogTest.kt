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

package org.kiwix.kiwixmobile.core.page.history

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.page.DELETE_MENU_ICON_TESTING_TAG
import org.kiwix.kiwixmobile.core.page.NO_ITEMS_TEXT_TESTING_TAG
import org.kiwix.kiwixmobile.core.page.history.models.NavigationHistoryListItem
import org.kiwix.kiwixmobile.core.ui.components.NAVIGATION_ICON_TESTING_TAG
import org.kiwix.kiwixmobile.core.ui.components.TOOLBAR_TITLE_TESTING_TAG
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * UI tests for [NavigationHistoryDialog] Compose components.
 * Covers: empty state, populated list rendering, item details,
 * click interactions, app bar integration, and action menu state.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])
class NavigationHistoryDialogTest {
  @get:Rule
  val composeTestRule = createComposeRule()

  private val context get() = RuntimeEnvironment.getApplication()

  private fun setDialogScreen(
    @StringRes titleId: Int = R.string.backward_history,
    navigationHistoryList: MutableList<NavigationHistoryListItem> = mutableListOf(),
    onNavigationItemClick: ((NavigationHistoryListItem) -> Unit) = {},
    onClearNavigationHistoryClick: () -> Unit = {},
    onDialogDismissRequest: () -> Unit = {}
  ) {
    composeTestRule.setContent {
      NavigationHistoryDialog(
        titleId = titleId,
        navigationHistoryList = navigationHistoryList,
        onNavigationItemClick = onNavigationItemClick,
        onClearNavigationHistoryClick = onClearNavigationHistoryClick,
        onDialogDismissRequest = onDialogDismissRequest
      )
    }
  }

  @Test
  fun emptyState_displaysNoHistoryText() {
    setDialogScreen()
    composeTestRule.onNodeWithTag(NO_ITEMS_TEXT_TESTING_TAG)
      .assertIsDisplayed()
      .assertTextEquals(context.getString(R.string.no_history))
    composeTestRule
      .onNodeWithContentDescription("Page Title 00")
      .assertDoesNotExist()
  }

  @Test
  fun populatedList_rendersExpectedNumberOfItems() {
    val items = createTestHistoryItems(ITEM_COUNT_FIVE)
    setDialogScreen(navigationHistoryList = items)
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
    setDialogScreen(navigationHistoryList = items)
    composeTestRule.onNodeWithTag(NO_ITEMS_TEXT_TESTING_TAG).assertDoesNotExist()
  }

  @Test
  fun itemDetails_displaysPageTitleAndFavicon() {
    val items = mutableListOf(
      NavigationHistoryListItem("Kiwix Article", "https://example.com/kiwix")
    )
    setDialogScreen(navigationHistoryList = items)
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
    setDialogScreen(navigationHistoryList = items)
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
    setDialogScreen(navigationHistoryList = items, onNavigationItemClick = { clickedItem = it })
    composeTestRule.onNodeWithText("Clicked Page").performClick()
    assertThat(clickedItem).isEqualTo(items[0])
  }

  @Test
  fun clickItem_passesCorrectItemDataForSecondItem() {
    val items = mutableListOf(
      NavigationHistoryListItem("First", "https://example.com/1"),
      NavigationHistoryListItem("Second", "https://example.com/2")
    )
    var clickedItem: NavigationHistoryListItem? = null
    setDialogScreen(navigationHistoryList = items, onNavigationItemClick = { clickedItem = it })
    composeTestRule.onNodeWithText("Second").performClick()
    assertThat(clickedItem).isEqualTo(items[1])
    assertThat(clickedItem?.title).isEqualTo("Second")
    assertThat(clickedItem?.pageUrl).isEqualTo("https://example.com/2")
  }

  @Test
  fun appBar_displaysCorrectTitle() {
    setDialogScreen()
    composeTestRule.onNodeWithTag(TOOLBAR_TITLE_TESTING_TAG, useUnmergedTree = true)
      .assertIsDisplayed()
      .assertTextEquals(context.getString(R.string.backward_history))
  }

  @Test
  fun appBar_displaysForwardHistoryTitle() {
    setDialogScreen(titleId = R.string.forward_history)
    composeTestRule.onNodeWithTag(TOOLBAR_TITLE_TESTING_TAG, useUnmergedTree = true)
      .assertIsDisplayed()
      .assertTextEquals(context.getString(R.string.forward_history))
  }

  @Test
  fun navigationIconClick_triggersDismissCallback() {
    var dismissed = false
    setDialogScreen(onDialogDismissRequest = { dismissed = true })

    composeTestRule
      .onNodeWithTag(NAVIGATION_ICON_TESTING_TAG)
      .performClick()

    assertThat(dismissed).isTrue()
  }

  @Test
  fun actionMenuClearAll_isEnabledWhenListHasData() {
    val items = mutableListOf(
      NavigationHistoryListItem("Page 1", "https://example.com/1")
    )
    setDialogScreen(navigationHistoryList = items)
    composeTestRule.onNodeWithTag(DELETE_MENU_ICON_TESTING_TAG)
      .assertIsDisplayed()
      .assertIsEnabled()
  }

  @Test
  fun actionMenuClearAll_isDisabledWhenListIsEmpty() {
    val items = mutableListOf<NavigationHistoryListItem>()
    setDialogScreen(navigationHistoryList = items)
    composeTestRule.onNodeWithTag(DELETE_MENU_ICON_TESTING_TAG)
      .assertIsDisplayed()
      .assertIsNotEnabled()
  }

  @Test
  fun actionMenuClearAll_clickShowsConfirmationDialog() {
    setDialogScreen(
      navigationHistoryList = mutableListOf(
        NavigationHistoryListItem("Page", "https://example.com")
      )
    )

    composeTestRule
      .onNodeWithTag(DELETE_MENU_ICON_TESTING_TAG)
      .performClick()
    composeTestRule.waitForIdle()
    composeTestRule
      .onNodeWithText(context.getString(R.string.clear_all_navigation_history_message))
      .assertIsDisplayed()
  }

  private fun createTestHistoryItems(
    count: Int
  ): MutableList<NavigationHistoryListItem> =
    (0 until count).map { i ->
      NavigationHistoryListItem(
        "Page Title $i",
        "https://example.com/page$i"
      )
    }.toMutableList()

  companion object {
    private const val ITEM_COUNT_THREE = 3
    private const val ITEM_COUNT_FIVE = 5
  }
}
