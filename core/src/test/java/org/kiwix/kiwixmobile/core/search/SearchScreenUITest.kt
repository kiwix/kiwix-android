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

package org.kiwix.kiwixmobile.core.search

import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.search.SearchListItem.RecentSearchListItem
import org.kiwix.kiwixmobile.core.search.SearchListItem.ZimSearchResultListItem
import org.kiwix.kiwixmobile.core.search.viewmodel.SearchScreenUiState
import org.kiwix.kiwixmobile.core.search.viewmodel.SearchViewModel
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])
class SearchScreenUITest {
  @get:Rule
  val composeTestRule = createComposeRule()

  private val context get() = RuntimeEnvironment.getApplication()
  private lateinit var mockViewModel: SearchViewModel

  @Before
  fun setUp() {
    mockViewModel = mockk(relaxed = true)
  }

  // ======== Helper Mock ========

  private fun mockSearchScreenContent(
    state: SearchScreenUiState = SearchScreenUiState(),
    actionMenuItems: List<ActionMenuItem> = emptyList()
  ) {
    composeTestRule.setContent {
      SearchScreen(
        state = state,
        searchViewModel = mockViewModel,
        actionMenuItemList = actionMenuItems,
        navigationIcon = {}
      )
    }
  }

  private fun voiceSearchActionMenuItem(onClick: () -> Unit = {}) = ActionMenuItem(
    contentDescription = R.string.search_label,
    icon = IconItem.Drawable(R.drawable.ic_mic_black_24dp),
    testingTag = VOICE_SEARCH_TESTING_TAG,
    isEnabled = true,
    onClick = onClick
  )

  private fun findInPageActionMenuItem(
    isEnabled: Boolean = true,
    onClick: () -> Unit = {}
  ) = ActionMenuItem(
    contentDescription = R.string.menu_search_in_text,
    iconButtonText = context.getString(R.string.menu_search_in_text),
    testingTag = FIND_IN_PAGE_TESTING_TAG,
    isEnabled = isEnabled,
    onClick = onClick
  )

  // ======== Search Field ========

  @Test
  fun searchScreen_whenScreenLaunched_searchFieldIsDisplayed() {
    mockSearchScreenContent()
    composeTestRule
      .onNodeWithTag(SEARCH_FIELD_TESTING_TAG)
      .assertIsDisplayed()
  }

  // ======== Search Results ========

  @Test
  fun searchScreen_whenSearchListIsEmpty_noResultViewIsDisplayed() {
    mockSearchScreenContent(state = SearchScreenUiState(searchList = emptyList()))
    composeTestRule
      .onNodeWithTag(NO_SEARCH_RESULT_TESTING_TAG)
      .assertIsDisplayed()
  }

  @Test
  fun searchScreen_whenSearchListIsNotEmpty_noResultViewDoesNotExist() {
    mockSearchScreenContent(
      state = SearchScreenUiState(
        searchList = listOf(ZimSearchResultListItem("Wikipedia", "https://kiwix.org/wikipedia"))
      )
    )
    composeTestRule
      .onNodeWithTag(NO_SEARCH_RESULT_TESTING_TAG)
      .assertDoesNotExist()
  }

  @Test
  fun searchScreen_whenSearchListHasItem_searchItemIsDisplayed() {
    mockSearchScreenContent(
      state = SearchScreenUiState(
        searchList = listOf(ZimSearchResultListItem("Wikipedia", "https://kiwix.org/wikipedia"))
      )
    )
    composeTestRule
      .onNodeWithText("Wikipedia")
      .assertIsDisplayed()
  }

  @Test
  fun searchScreen_whenSearchListHasMultipleItems_allItemsAreDisplayed() {
    mockSearchScreenContent(
      state = SearchScreenUiState(
        searchList = listOf(
          ZimSearchResultListItem("Wikipedia", "https://kiwix.org/wikipedia"),
          ZimSearchResultListItem("Wiktionary", "https://kiwix.org/wikipedia")
        )
      )
    )
    composeTestRule.onNodeWithText("Wikipedia").assertIsDisplayed()
    composeTestRule.onNodeWithText("Wiktionary").assertIsDisplayed()
  }

  @Test
  fun searchScreen_whenSearchItemClicked_onItemClickIsTriggered() {
    mockSearchScreenContent(
      state = SearchScreenUiState(
        searchList = listOf(ZimSearchResultListItem("Wikipedia", "https://kiwix.org/wikipedia"))
      )
    )
    composeTestRule
      .onNodeWithTag(SEARCH_ITEM_TESTING_TAG)
      .performClick()
    verify { mockViewModel.onItemClick(any()) }
  }

  @Test
  fun searchScreen_whenSearchItemLongClicked_onItemLongClickIsTriggered() {
    mockSearchScreenContent(
      state = SearchScreenUiState(
        searchList = listOf(RecentSearchListItem("Wikipedia", "https://kiwix.org/wikipedia"))
      )
    )
    composeTestRule
      .onNodeWithTag(SEARCH_ITEM_TESTING_TAG)
      .performTouchInput { longClick() }
    verify { mockViewModel.onItemLongClick(any()) }
  }

  // ======== Open In New Tab ========

  @Test
  fun searchScreen_whenSearchItemDisplayed_openInNewTabIconIsVisible() {
    mockSearchScreenContent(
      state = SearchScreenUiState(
        searchList = listOf(ZimSearchResultListItem("Wikipedia", "https://kiwix.org/wikipedia"))
      )
    )
    composeTestRule
      .onNodeWithTag(OPEN_ITEM_IN_NEW_TAB_ICON_TESTING_TAG)
      .assertIsDisplayed()
  }

  @Test
  fun searchScreen_whenOpenInNewTabIconClicked_onNewTabIconClickIsTriggered() {
    mockSearchScreenContent(
      state = SearchScreenUiState(
        searchList = listOf(ZimSearchResultListItem("Wikipedia", "https://kiwix.org/wikipedia"))
      )
    )
    composeTestRule
      .onNodeWithTag(OPEN_ITEM_IN_NEW_TAB_ICON_TESTING_TAG)
      .performClick()
    verify { mockViewModel.onNewTabIconClick(any()) }
  }

  // ======== Spelling Corrections ========

  @Test
  fun searchScreen_whenSpellingCorrectionsAvailable_doYouMeanHeaderIsDisplayed() {
    mockSearchScreenContent(
      state = SearchScreenUiState(
        spellingCorrectionSuggestions = listOf("Wikipedia"),
        searchList = emptyList()
      )
    )
    composeTestRule
      .onNodeWithText(context.getString(R.string.do_you_mean))
      .assertIsDisplayed()
  }

  @Test
  fun searchScreen_whenSpellingCorrectionsAvailable_suggestionItemIsDisplayed() {
    mockSearchScreenContent(
      state = SearchScreenUiState(
        spellingCorrectionSuggestions = listOf("Wikipedia"),
        searchList = emptyList()
      )
    )
    composeTestRule
      .onNodeWithText("Wikipedia")
      .assertIsDisplayed()
  }

  @Test
  fun searchScreen_whenSpellingCorrectionsAvailable_searchListIsHidden() {
    mockSearchScreenContent(
      state = SearchScreenUiState(
        spellingCorrectionSuggestions = listOf("Wikipedia"),
        searchList = listOf(ZimSearchResultListItem("Wiktionary", "https://kiwix.org/wikipedia"))
      )
    )
    composeTestRule
      .onNodeWithTag(SEARCH_ITEM_TESTING_TAG)
      .assertDoesNotExist()
  }

  @Test
  fun searchScreen_whenSpellingCorrectionItemClicked_onSuggestionItemClickIsTriggered() {
    mockSearchScreenContent(
      state = SearchScreenUiState(
        spellingCorrectionSuggestions = listOf("Wikipedia"),
        searchList = emptyList()
      )
    )
    composeTestRule
      .onNodeWithText("Wikipedia")
      .performClick()
    verify { mockViewModel.onSuggestionItemClick("Wikipedia") }
  }

  // ======== Loading ========

  @Test
  fun searchScreen_whenIsLoadingTrue_searchItemsAreHidden() {
    mockSearchScreenContent(
      state = SearchScreenUiState(
        isLoading = true,
        searchList = emptyList()
      )
    )
    composeTestRule
      .onNodeWithTag(SEARCH_ITEM_TESTING_TAG)
      .assertDoesNotExist()
  }

  // ======== Voice Search ========

  @Test
  fun searchScreen_whenVoiceSearchIconDisplayed_isVisible() {
    mockSearchScreenContent(actionMenuItems = listOf(voiceSearchActionMenuItem()))
    composeTestRule
      .onNodeWithTag(VOICE_SEARCH_TESTING_TAG)
      .assertIsDisplayed()
  }

  @Test
  fun searchScreen_whenVoiceSearchIconClicked_callbackIsTriggered() {
    var clicked = false
    mockSearchScreenContent(actionMenuItems = listOf(voiceSearchActionMenuItem { clicked = true }))
    composeTestRule
      .onNodeWithTag(VOICE_SEARCH_TESTING_TAG)
      .performClick()
    assertTrue("Voice search callback should be triggered", clicked)
  }

  // ======== Find In Page ========

  @Test
  fun searchScreen_whenFindInPageMenuItemPresent_isDisplayed() {
    mockSearchScreenContent(actionMenuItems = listOf(findInPageActionMenuItem()))
    composeTestRule
      .onNodeWithTag(FIND_IN_PAGE_TESTING_TAG)
      .assertIsDisplayed()
  }

  @Test
  fun searchScreen_whenFindInPageMenuItemNotInList_doesNotExist() {
    mockSearchScreenContent(actionMenuItems = emptyList())
    composeTestRule
      .onNodeWithTag(FIND_IN_PAGE_TESTING_TAG)
      .assertDoesNotExist()
  }

  @Test
  fun searchScreen_whenFindInPageMenuItemClicked_callbackIsTriggered() {
    var clicked = false
    mockSearchScreenContent(actionMenuItems = listOf(findInPageActionMenuItem { clicked = true }))
    composeTestRule
      .onNodeWithTag(FIND_IN_PAGE_TESTING_TAG)
      .performClick()
    assertTrue("Find in page callback should be triggered", clicked)
  }
}
