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

package org.kiwix.kiwixmobile.core.page

import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.jakewharton.threetenabp.AndroidThreeTen
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.page.adapter.Page
import org.kiwix.kiwixmobile.core.page.adapter.PageRelated
import org.kiwix.kiwixmobile.core.page.history.models.HistoryListItem
import org.kiwix.kiwixmobile.core.page.viewmodel.PageState
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.ui.components.NAVIGATION_ICON_TESTING_TAG
import org.kiwix.kiwixmobile.core.ui.components.NavigationIcon
import org.kiwix.kiwixmobile.core.ui.components.TOOLBAR_TITLE_TESTING_TAG
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Behavior-driven UI tests for [PageScreen].
 *
 * All tests render through the [PageScreen] composable directly,
 * using a test-only [TestPageState] to drive different UI states.
 * This ensures we test real user-visible behavior, not internal
 * composable implementation details.
 *
 * Covers: empty state, populated list rendering, app bar title, selection mode,
 * switch row, search/delete action menu, navigation, item click/long-click,
 * selected item icon, and date item labels.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])
class PageScreenTest {
  @get:Rule
  val composeTestRule = createComposeRule()

  private val context get() = RuntimeEnvironment.getApplication()

  @Before
  fun setUp() {
    AndroidThreeTen.init(context)
  }

  /**
   * Minimal [Page] implementation used exclusively for testing.
   * Avoids needing real database entities or ZimFileReader instances.
   */
  private data class TestPage(
    override val zimReaderSource: ZimReaderSource? = null,
    override val zimId: String = "testZimId",
    override val url: String = "",
    override val title: String = "",
    override var isSelected: Boolean = false,
    override val favicon: String? = null,
    override val id: Long = 0L
  ) : Page

  /**
   * Concrete [PageState] for tests. Allows full control over
   * [pageItems], [showAll], [currentZimId], and [searchTerm].
   */
  private data class TestPageState(
    override val pageItems: List<TestPage> = emptyList(),
    override val showAll: Boolean = true,
    override val currentZimId: String? = "testZimId",
    override val searchTerm: String = ""
  ) : PageState<TestPage>() {
    override val visiblePageItems: List<PageRelated> = filteredPageItems
    override fun copyWithNewItems(newItems: List<TestPage>): PageState<TestPage> =
      copy(pageItems = newItems)
  }

  /**
   * Extended [TestPageState] that allows injecting arbitrary [visiblePageItems]
   * (including [HistoryListItem.DateItem]) for date-label tests.
   */
  private data class TestPageStateWithDateItems(
    override val pageItems: List<TestPage> = emptyList(),
    override val showAll: Boolean = true,
    override val currentZimId: String? = "testZimId",
    override val searchTerm: String = "",
    val customVisibleItems: List<PageRelated> = emptyList()
  ) : PageState<TestPage>() {
    override val visiblePageItems: List<PageRelated> = customVisibleItems
    override fun copyWithNewItems(newItems: List<TestPage>): PageState<TestPage> =
      TestPageState(pageItems = newItems)
  }

  /**
   * Renders the [PageScreen] with sensible defaults.
   * Callers can override any parameter to test specific behaviors.
   */
  private fun renderPageScreen(
    state: PageState<TestPage> = TestPageState(),
    searchText: String = "",
    screenTitle: String = "Test Screen",
    switchString: String = "Show all",
    noItemsString: String = "No items",
    searchQueryHint: String = "Search…",
    isSearchBarActive: Boolean = false,
    isInSelectionMode: Boolean = false,
    selectedCount: Int = 0,
    switchIsCheckedFlow: MutableStateFlow<Boolean> = MutableStateFlow(true),
    isCustomApp: Boolean = false,
    onItemClick: (Page) -> Unit = {},
    onItemLongClick: (Page) -> Unit = {},
    onSearchTextChange: (String) -> Unit = {},
    onSwitchCheckedChange: (Boolean) -> Unit = {},
    onClearSearch: () -> Unit = {},
    actionMenuItems: List<ActionMenuItem> = defaultActionMenuItems(),
    navigationIcon: @androidx.compose.runtime.Composable () -> Unit =
      { NavigationIcon(onClick = {}) }
  ) {
    composeTestRule.setContent {
      PageScreen<TestPage, PageState<TestPage>>(
        state = state,
        searchText = searchText,
        screenTitle = screenTitle,
        switchString = switchString,
        noItemsString = noItemsString,
        searchQueryHint = searchQueryHint,
        isSearchBarActive = isSearchBarActive,
        isInSelectionMode = isInSelectionMode,
        selectedCount = selectedCount,
        switchIsCheckedFlow = switchIsCheckedFlow,
        isCustomApp = isCustomApp,
        onItemClick = onItemClick,
        onItemLongClick = onItemLongClick,
        onSearchTextChange = onSearchTextChange,
        onSwitchCheckedChange = onSwitchCheckedChange,
        onClearSearch = onClearSearch,
        actionMenuItems = actionMenuItems,
        navigationIcon = navigationIcon
      )
    }
  }

  private fun defaultActionMenuItems(
    onSearchClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {}
  ): List<ActionMenuItem> = listOf(
    ActionMenuItem(
      icon = IconItem.Drawable(R.drawable.action_search),
      contentDescription = R.string.search_label,
      onClick = onSearchClick,
      testingTag = SEARCH_ICON_TESTING_TAG
    ),
    ActionMenuItem(
      icon = IconItem.Vector(Icons.Default.Delete),
      contentDescription = R.string.pref_clear_all_history_title,
      onClick = onDeleteClick,
      testingTag = DELETE_MENU_ICON_TESTING_TAG
    )
  )

  private fun selectionModeActionMenuItems(
    onSelectionDeleteClick: () -> Unit = {}
  ): List<ActionMenuItem> = listOf(
    ActionMenuItem(
      icon = IconItem.Vector(Icons.Default.Delete),
      contentDescription = R.string.delete,
      onClick = onSelectionDeleteClick,
      testingTag = DELETE_MENU_ICON_TESTING_TAG
    )
  )

  private fun createTestPages(count: Int): List<TestPage> =
    (0 until count).map { i ->
      TestPage(
        title = "Page Title $i",
        url = "https://example.com/page$i",
        id = i.toLong()
      )
    }

  @Test
  fun emptyState_displaysNoItemsText() {
    renderPageScreen(
      state = TestPageState(pageItems = emptyList()),
      noItemsString = "No bookmarks"
    )
    composeTestRule.onNodeWithTag(NO_ITEMS_TEXT_TESTING_TAG)
      .assertIsDisplayed()
      .assertTextEquals("No bookmarks")
  }

  @Test
  fun emptyState_doesNotShowPageList() {
    renderPageScreen(state = TestPageState(pageItems = emptyList()))
    composeTestRule.onNodeWithTag(PAGE_LIST_TEST_TAG)
      .assertDoesNotExist()
  }

  @Test
  fun populatedList_displaysPageItems() {
    val pages = createTestPages(ITEM_COUNT_THREE)
    renderPageScreen(state = TestPageState(pageItems = pages))
    pages.forEachIndexed { index, page ->
      composeTestRule
        .onNodeWithContentDescription("${page.title}$index")
        .assertIsDisplayed()
    }
  }

  @Test
  fun populatedList_doesNotShowNoItemsText() {
    renderPageScreen(state = TestPageState(pageItems = createTestPages(ITEM_COUNT_THREE)))
    composeTestRule.onNodeWithTag(NO_ITEMS_TEXT_TESTING_TAG)
      .assertDoesNotExist()
  }

  @Test
  fun populatedList_displaysPageList() {
    renderPageScreen(state = TestPageState(pageItems = createTestPages(ITEM_COUNT_THREE)))
    composeTestRule.onNodeWithTag(PAGE_LIST_TEST_TAG)
      .assertIsDisplayed()
  }

  @Test
  fun appBar_displaysCorrectScreenTitle() {
    renderPageScreen(screenTitle = "Bookmarks")
    composeTestRule.onNodeWithTag(TOOLBAR_TITLE_TESTING_TAG, useUnmergedTree = true)
      .assertIsDisplayed()
      .assertTextEquals("Bookmarks")
  }

  @Test
  fun appBar_displaysSelectionCountInSelectionMode() {
    val selectedCount = 2
    renderPageScreen(
      isInSelectionMode = true,
      selectedCount = selectedCount,
      actionMenuItems = selectionModeActionMenuItems()
    )
    val expectedTitle = context.getString(R.string.selected_items, selectedCount)
    composeTestRule.onNodeWithTag(TOOLBAR_TITLE_TESTING_TAG, useUnmergedTree = true)
      .assertIsDisplayed()
      .assertTextEquals(expectedTitle)
  }

  @Test
  fun switchRow_displaysWithCorrectLabel() {
    renderPageScreen(switchString = "Show all bookmarks")
    composeTestRule.onNodeWithTag(SWITCH_TEXT_TESTING_TAG)
      .assertIsDisplayed()
      .assertTextEquals("Show all bookmarks")
  }

  @Test
  fun switchRow_switchIsDisplayed() {
    renderPageScreen(switchIsCheckedFlow = MutableStateFlow(true))
    composeTestRule.onNode(isToggleable())
      .assertIsDisplayed()
  }

  @Test
  fun switchRow_hiddenForCustomApp() {
    renderPageScreen(isCustomApp = true, switchString = "Show all bookmarks")
    composeTestRule.onNodeWithTag(SWITCH_TEXT_TESTING_TAG)
      .assertDoesNotExist()
  }

  @Test
  fun searchIcon_visibleWhenSearchNotActive() {
    renderPageScreen(
      isSearchBarActive = false,
      actionMenuItems = defaultActionMenuItems()
    )
    composeTestRule.onNodeWithTag(SEARCH_ICON_TESTING_TAG)
      .assertIsDisplayed()
  }

  @Test
  fun searchIcon_hiddenWhenSearchActive() {
    // When search is active, the search icon is not in the action menu
    renderPageScreen(
      isSearchBarActive = true,
      actionMenuItems = listOf(
        ActionMenuItem(
          icon = IconItem.Vector(Icons.Default.Delete),
          contentDescription = R.string.pref_clear_all_history_title,
          onClick = {},
          testingTag = DELETE_MENU_ICON_TESTING_TAG
        )
      )
    )
    composeTestRule.onNodeWithTag(SEARCH_ICON_TESTING_TAG)
      .assertDoesNotExist()
  }

  @Test
  fun deleteIcon_alwaysDisplayed() {
    renderPageScreen(actionMenuItems = defaultActionMenuItems())
    composeTestRule.onNodeWithTag(DELETE_MENU_ICON_TESTING_TAG)
      .assertIsDisplayed()
  }

  @Test
  fun navigationIcon_click_triggersCallback() {
    var navigationClicked = false
    renderPageScreen(
      navigationIcon = { NavigationIcon(onClick = { navigationClicked = true }) }
    )
    composeTestRule.onNodeWithTag(NAVIGATION_ICON_TESTING_TAG)
      .performClick()
    assertTrue("Navigation icon click callback should be triggered", navigationClicked)
  }

  @Test
  fun pageItem_click_triggersOnItemClickCallback() {
    val pages = createTestPages(1)
    var clickedPage: Page? = null
    renderPageScreen(
      state = TestPageState(pageItems = pages),
      onItemClick = { clickedPage = it }
    )
    composeTestRule
      .onNodeWithTag(PAGE_ITEM_TESTING_TAG)
      .performClick()
    assertEquals("Clicked page should match the expected page", pages[0], clickedPage)
  }

  @Test
  fun pageItem_longClick_triggersOnItemLongClickCallback() {
    val pages = createTestPages(1)
    var longClickedPage: Page? = null
    renderPageScreen(
      state = TestPageState(pageItems = pages),
      onItemLongClick = { longClickedPage = it }
    )
    composeTestRule
      .onNodeWithTag(PAGE_ITEM_TESTING_TAG)
      .performTouchInput { longClick() }
    assertEquals("Long clicked page should match the expected page", pages[0], longClickedPage)
  }

  @Test
  fun selectionMode_displaysDeleteIconInActionMenu() {
    renderPageScreen(
      isInSelectionMode = true,
      selectedCount = 1,
      actionMenuItems = selectionModeActionMenuItems()
    )
    composeTestRule.onNodeWithTag(DELETE_MENU_ICON_TESTING_TAG)
      .assertIsDisplayed()
  }

  @Test
  fun pageItem_displaysSelectedCheckIcon() {
    val selectedPage = TestPage(
      title = "Selected Page",
      url = "https://example.com/selected",
      isSelected = true,
      id = 1L
    )
    renderPageScreen(state = TestPageState(pageItems = listOf(selectedPage)))
    // When isSelected is true, the favicon is replaced with a check circle icon.
    // The contentDescription for the image is "Favicon" + index.
    val faviconDescription = context.getString(R.string.fav_icon) + "0"
    composeTestRule
      .onNodeWithContentDescription(faviconDescription)
      .assertIsDisplayed()
  }

  @Test
  fun dateItem_displaysFormattedDateLabel() {
    val dateItem = HistoryListItem.DateItem("01 Jan 2000")
    val page = TestPage(title = "History Page", id = 1L)
    val state = TestPageStateWithDateItems(
      pageItems = listOf(page),
      customVisibleItems = listOf(dateItem, page)
    )
    renderPageScreen(state = state)
    composeTestRule
      .onNodeWithText("01 Jan 2000")
      .assertIsDisplayed()
  }

  companion object {
    private const val ITEM_COUNT_THREE = 3
  }
}
