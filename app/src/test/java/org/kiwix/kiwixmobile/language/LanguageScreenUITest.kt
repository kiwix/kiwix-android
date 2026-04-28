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

package org.kiwix.kiwixmobile.language

import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.page.SEARCH_ICON_TESTING_TAG
import org.kiwix.kiwixmobile.core.search.SEARCH_FIELD_TESTING_TAG
import org.kiwix.kiwixmobile.core.ui.components.CONTENT_LOADING_PROGRESS_BAR_TESTING_TAG
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.zim_manager.Language
import org.kiwix.kiwixmobile.language.composables.LANGUAGE_HEADER_TESTING_TAG
import org.kiwix.kiwixmobile.language.composables.LanguageListItem
import org.kiwix.kiwixmobile.language.viewmodel.State
import org.kiwix.kiwixmobile.nav.destination.library.online.NO_CONTENT_VIEW_TEXT_TESTING_TAG
import org.kiwix.kiwixmobile.utils.TestApplication
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R], application = TestApplication::class)
class LanguageScreenUITest {
  @get:Rule
  val composeTestRule = createComposeRule()

  private val context get() = RuntimeEnvironment.getApplication()

  // ======== Helper ========
  private fun mockLanguage(
    languageCode: String = "en",
    active: Boolean = false,
    id: Long = 1L
  ) = Language(
    languageCode = languageCode,
    active = active,
    occurrencesOfLanguage = 10,
    id = id
  )

  private fun saveActionMenuItem(onClick: () -> Unit = {}) = ActionMenuItem(
    icon = IconItem.Vector(Icons.Default.Check),
    contentDescription = R.string.save_languages,
    onClick = onClick,
    testingTag = SAVE_ICON_TESTING_TAG
  )

  private fun searchActionMenuItem(onClick: () -> Unit = {}) = ActionMenuItem(
    icon = IconItem.Drawable(R.drawable.action_search),
    contentDescription = R.string.search_label,
    onClick = onClick,
    testingTag = SEARCH_ICON_TESTING_TAG
  )

  // ======== Language Screen Helper ========
  private fun mockLanguageScreen(
    searchText: String = "",
    isSearchActive: Boolean = false,
    state: State = State.Loading,
    actionMenuItemList: List<ActionMenuItem> = listOf(
      searchActionMenuItem(),
      saveActionMenuItem()
    ),
    onClearClick: () -> Unit = {},
    onAppBarValueChange: (String) -> Unit = {},
    selectLanguageItem: (LanguageListItem.LanguageItem) -> Unit = {},
    navigationIcon: @Composable () -> Unit = {}
  ) {
    composeTestRule.setContent {
      LanguageScreen(
        searchText = searchText,
        isSearchActive = isSearchActive,
        state = state,
        actionMenuItemList = actionMenuItemList,
        onClearClick = onClearClick,
        onAppBarValueChange = onAppBarValueChange,
        selectLanguageItem = selectLanguageItem,
        navigationIcon = navigationIcon
      )
    }
  }

  // ======== App Bar ========

  @Test
  fun languageScreen_whenScreenLaunched_titleIsDisplayed() {
    mockLanguageScreen()
    composeTestRule
      .onNodeWithText(context.getString(R.string.select_language))
      .assertIsDisplayed()
  }

  @Test
  fun languageScreen_whenScreenLaunched_saveIconIsDisplayed() {
    mockLanguageScreen()
    composeTestRule
      .onNodeWithTag(SAVE_ICON_TESTING_TAG)
      .assertIsDisplayed()
  }

  @Test
  fun languageScreen_whenSaveIconClicked_callbackIsTriggered() {
    var clicked = false
    mockLanguageScreen(
      actionMenuItemList = listOf(
        searchActionMenuItem(),
        saveActionMenuItem { clicked = true }
      )
    )
    composeTestRule
      .onNodeWithTag(SAVE_ICON_TESTING_TAG)
      .performClick()
    assertTrue("Save icon callback should be triggered", clicked)
  }

  @Test
  fun languageScreen_whenSearchNotActive_searchIconIsDisplayed() {
    mockLanguageScreen(isSearchActive = false)
    composeTestRule
      .onNodeWithTag(SEARCH_ICON_TESTING_TAG)
      .assertIsDisplayed()
  }

  @Test
  fun languageScreen_whenSearchIconClicked_callbackIsTriggered() {
    var clicked = false
    mockLanguageScreen(
      actionMenuItemList = listOf(
        searchActionMenuItem { clicked = true },
        saveActionMenuItem()
      )
    )
    composeTestRule
      .onNodeWithTag(SEARCH_ICON_TESTING_TAG)
      .performClick()
    assertTrue("Search icon callback should be triggered", clicked)
  }

  // ======== Search Field ========

  @Test
  fun languageScreen_whenSearchIsActive_searchFieldIsDisplayed() {
    mockLanguageScreen(isSearchActive = true)
    composeTestRule
      .onNodeWithTag(SEARCH_FIELD_TESTING_TAG)
      .assertIsDisplayed()
  }

  @Test
  fun languageScreen_whenSearchIsNotActive_searchFieldDoesNotExist() {
    mockLanguageScreen(isSearchActive = false)
    composeTestRule
      .onNodeWithTag(SEARCH_FIELD_TESTING_TAG)
      .assertDoesNotExist()
  }

  @Test
  fun languageScreen_whenSearchIsActive_searchIconIsHidden() {
    mockLanguageScreen(
      isSearchActive = true,
      actionMenuItemList = listOf(saveActionMenuItem())
    )
    composeTestRule
      .onNodeWithTag(SEARCH_ICON_TESTING_TAG)
      .assertDoesNotExist()
  }

  // ======== Loading State ========

  @Test
  fun languageScreen_whenStateIsLoading_progressBarIsDisplayed() {
    mockLanguageScreen(state = State.Loading)
    composeTestRule
      .onNodeWithTag(CONTENT_LOADING_PROGRESS_BAR_TESTING_TAG)
      .assertIsDisplayed()
  }

  @Test
  fun languageScreen_whenStateIsLoading_languageListDoesNotExist() {
    mockLanguageScreen(state = State.Loading)
    composeTestRule
      .onNodeWithContentDescription(
        context.getString(R.string.select_language_content_description)
      )
      .assertDoesNotExist()
  }

  // ======== Saving State ========

  @Test
  fun languageScreen_whenStateIsSaving_progressBarIsDisplayed() {
    mockLanguageScreen(state = State.Saving)
    composeTestRule
      .onNodeWithTag(CONTENT_LOADING_PROGRESS_BAR_TESTING_TAG)
      .assertIsDisplayed()
  }

  @Test
  fun languageScreen_whenStateIsSaving_languageListDoesNotExist() {
    mockLanguageScreen(state = State.Saving)
    composeTestRule
      .onNodeWithContentDescription(
        context.getString(R.string.select_language_content_description)
      )
      .assertDoesNotExist()
  }

  // ======== Error State ========

  @Test
  fun languageScreen_whenStateIsError_errorMessageIsDisplayed() {
    mockLanguageScreen(state = State.Error("Error"))
    composeTestRule
      .onNodeWithText("Error")
      .assertIsDisplayed()
  }

  @Test
  fun languageScreen_whenStateIsError_errorTagIsPresent() {
    mockLanguageScreen(state = State.Error("Something went wrong"))
    composeTestRule
      .onNodeWithTag(NO_CONTENT_VIEW_TEXT_TESTING_TAG)
      .assertIsDisplayed()
  }

  @Test
  fun languageScreen_whenStateIsError_languageListDoesNotExist() {
    mockLanguageScreen(state = State.Error("Error"))
    composeTestRule
      .onNodeWithContentDescription(
        context.getString(R.string.select_language_content_description)
      )
      .assertDoesNotExist()
  }

  // ======== Content State ========

  @Test
  fun languageScreen_whenStateIsContent_languageItemIsDisplayed() {
    val language = mockLanguage(languageCode = "en", active = false)
    mockLanguageScreen(state = State.Content(listOf(language)))
    composeTestRule
      .onNodeWithContentDescription(
        context.getString(R.string.select_language_content_description)
      )
      .assertIsDisplayed()
  }

  @Test
  fun languageScreen_whenStateIsContent_activeLanguageIsDisplayed() {
    val activeLanguage = mockLanguage(languageCode = "en", active = true, id = 1L)
    val otherLanguage = mockLanguage(languageCode = "fr", active = false, id = 2L)
    mockLanguageScreen(state = State.Content(listOf(activeLanguage, otherLanguage)))
    val nodes = composeTestRule
      .onAllNodesWithContentDescription(
        context.getString(R.string.select_language_content_description)
      )
    nodes[0].assertIsDisplayed()
    nodes[1].assertIsDisplayed()
  }

  @Test
  fun languageScreen_whenActiveLanguageExists_selectedHeaderIsDisplayed() {
    val language = mockLanguage(languageCode = "en", active = true)
    mockLanguageScreen(state = State.Content(listOf(language)))
    composeTestRule
      .onNodeWithTag("$LANGUAGE_HEADER_TESTING_TAG${LanguageListItem.HeaderItem.SELECTED}")
      .assertIsDisplayed()
  }

  @Test
  fun languageScreen_whenInactiveLanguageExists_otherHeaderIsDisplayed() {
    val language = mockLanguage(languageCode = "en", active = false)
    mockLanguageScreen(state = State.Content(listOf(language)))
    composeTestRule
      .onNodeWithTag("$LANGUAGE_HEADER_TESTING_TAG${LanguageListItem.HeaderItem.OTHER}")
      .assertIsDisplayed()
  }

  @Test
  fun languageScreen_whenBothActiveAndInactiveLanguagesExist_bothHeadersAreDisplayed() {
    val activeLanguage = mockLanguage(languageCode = "en", active = true, id = 1L)
    val otherLanguage = mockLanguage(languageCode = "fr", active = false, id = 2L)
    mockLanguageScreen(state = State.Content(listOf(activeLanguage, otherLanguage)))
    composeTestRule
      .onNodeWithTag("$LANGUAGE_HEADER_TESTING_TAG${LanguageListItem.HeaderItem.SELECTED}")
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithTag("$LANGUAGE_HEADER_TESTING_TAG${LanguageListItem.HeaderItem.OTHER}")
      .assertIsDisplayed()
  }

  @Test
  fun languageScreen_whenLanguageItemClicked_selectLanguageItemCallbackIsTriggered() {
    var selectedItem: LanguageListItem.LanguageItem? = null
    val language = mockLanguage(languageCode = "en", active = false)
    mockLanguageScreen(
      state = State.Content(listOf(language)),
      selectLanguageItem = { selectedItem = it }
    )
    composeTestRule
      .onNodeWithContentDescription(
        context.getString(R.string.select_language_content_description)
      )
      .performClick()
    assertTrue("selectLanguageItem callback should be triggered", selectedItem != null)
  }

  @Test
  fun languageScreen_whenStateIsContent_errorMessageDoesNotExist() {
    val language = mockLanguage(languageCode = "en", active = false)
    mockLanguageScreen(state = State.Content(listOf(language)))
    composeTestRule
      .onNodeWithTag(NO_CONTENT_VIEW_TEXT_TESTING_TAG)
      .assertDoesNotExist()
  }

  @Test
  fun languageScreen_whenFilterMatchesNoLanguage_languageListDoesNotExist() {
    val language = mockLanguage(languageCode = "en", active = false)
    mockLanguageScreen(state = State.Content(listOf(language), filter = "gibberish"))
    composeTestRule
      .onNodeWithContentDescription(
        context.getString(R.string.select_language_content_description)
      )
      .assertDoesNotExist()
  }
}
