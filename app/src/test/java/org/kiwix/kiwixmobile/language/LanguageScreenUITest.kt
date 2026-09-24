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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.page.SEARCH_ICON_TESTING_TAG
import org.kiwix.kiwixmobile.core.search.SEARCH_FIELD_TESTING_TAG
import org.kiwix.kiwixmobile.core.ui.components.CONTENT_LOADING_PROGRESS_BAR_TESTING_TAG
import org.kiwix.kiwixmobile.core.ui.components.NAVIGATION_ICON_TESTING_TAG
import org.kiwix.kiwixmobile.core.ui.components.NavigationIcon
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.ui.models.IconItem.Vector
import org.kiwix.kiwixmobile.core.zim_manager.Language
import org.kiwix.kiwixmobile.language.composables.LANGUAGE_HEADER_TESTING_TAG
import org.kiwix.kiwixmobile.language.composables.LANGUAGE_ITEM_RADIO_BUTTON_TESTING_TAG
import org.kiwix.kiwixmobile.language.composables.LanguageListItem
import org.kiwix.kiwixmobile.language.viewmodel.State
import org.kiwix.kiwixmobile.nav.destination.library.online.NO_CONTENT_VIEW_TEXT_TESTING_TAG
import org.kiwix.sharedFunctions.TestApplication
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R], application = TestApplication::class)
class LanguageScreenUITest {
  @Rule
  @JvmField
  val composeTestRule = createComposeRule()

  private val context get() = RuntimeEnvironment.getApplication()
  private fun mockLanguage(
    languageCode: String = "en",
    active: Boolean = false,
    id: Long = 1L,
    occurrencesOfLanguage: Int = 10
  ) = Language(
    languageCode = languageCode,
    active = active,
    occurrencesOfLanguage = occurrencesOfLanguage,
    id = id
  )

  private fun customAccessibilityActions(
    node: SemanticsNodeInteraction
  ): List<CustomAccessibilityAction> =
    node.fetchSemanticsNode().config.getOrNull(SemanticsActions.CustomActions).orEmpty()

  private fun searchActionMenuItem(onClick: () -> Unit = {}) = ActionMenuItem(
    icon = IconItem.Drawable(R.drawable.action_search),
    contentDescription = R.string.search_label,
    onClick = onClick,
    testingTag = SEARCH_ICON_TESTING_TAG
  )

  private fun mockLanguageScreen(
    searchText: String = "",
    isSearchActive: Boolean = false,
    state: State = State.Loading,
    actionMenuItemList: List<ActionMenuItem> = listOf(
      searchActionMenuItem()
    ),
    onClearClick: () -> Unit = {},
    onAppBarValueChange: (String) -> Unit = {},
    selectLanguageItem: (LanguageListItem.LanguageItem) -> Unit = {},
    onMoveUp: (LanguageListItem.LanguageItem) -> Unit = {},
    onMoveDown: (LanguageListItem.LanguageItem) -> Unit = {},
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
        onMoveUp = onMoveUp,
        onMoveDown = onMoveDown,
        navigationIcon = navigationIcon
      )
    }
  }

  @Test
  fun languageScreen_whenScreenLaunched_titleIsDisplayed() {
    mockLanguageScreen()
    composeTestRule
      .onNodeWithText(context.getString(R.string.select_language))
      .assertIsDisplayed()
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
        searchActionMenuItem { clicked = true }
      )
    )
    composeTestRule
      .onNodeWithTag(SEARCH_ICON_TESTING_TAG)
      .performClick()
    assertTrue("Search icon callback should be triggered", clicked)
  }

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
      actionMenuItemList = emptyList()
    )
    composeTestRule
      .onNodeWithTag(SEARCH_ICON_TESTING_TAG)
      .assertDoesNotExist()
  }

  @Test
  fun languageScreen_whenUserTypesInSearchField_queryIsDisplayed() {
    var query = ""
    mockLanguageScreen(
      isSearchActive = true,
      onAppBarValueChange = { query = it }
    )
    composeTestRule
      .onNodeWithTag(SEARCH_FIELD_TESTING_TAG)
      .performTextInput("eng")
    composeTestRule.waitForIdle()
    assertTrue(query == "eng")
  }

  @Test
  fun languageScreen_whenClearSearchClicked_callbackIsTriggered() {
    var cleared = false
    mockLanguageScreen(
      isSearchActive = true,
      searchText = "English",
      onClearClick = { cleared = true }
    )
    composeTestRule
      .onNodeWithContentDescription(
        context.getString(R.string.searchview_description_clear)
      ).performClick()
    assertTrue("onClearClick callback should be triggered", cleared)
  }

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
      ).assertDoesNotExist()
  }

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
      ).assertDoesNotExist()
  }

  @Test
  fun languageScreen_whenBackPressedWhileSearchActive_navigationIconCallbackIsTriggered() {
    var backPressHandled = false
    mockLanguageScreen(
      isSearchActive = true,
      actionMenuItemList = emptyList(),
      navigationIcon = {
        NavigationIcon(
          iconItem = Vector(Icons.AutoMirrored.Filled.ArrowBack),
          onClick = { backPressHandled = true }
        )
      }
    )
    composeTestRule
      .onNodeWithTag(NAVIGATION_ICON_TESTING_TAG)
      .performClick()
    assertTrue(
      "Navigation icon callback should be triggered when search is active",
      backPressHandled
    )
  }

  @Test
  fun languageScreen_whenBackPressedWhileSearchNotActive_navigationIconCallbackIsTriggered() {
    var navigatedBack = false
    mockLanguageScreen(
      isSearchActive = false,
      navigationIcon = {
        NavigationIcon(
          iconItem = IconItem.Drawable(R.drawable.ic_close_white_24dp),
          onClick = { navigatedBack = true }
        )
      }
    )
    composeTestRule
      .onNodeWithTag(NAVIGATION_ICON_TESTING_TAG)
      .performClick()
    assertTrue(
      "navigateBack callback should be triggered when search is not active",
      navigatedBack
    )
  }

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
      ).assertDoesNotExist()
  }

  @Test
  fun languageScreen_whenStateIsContent_languageItemIsDisplayed() {
    val language = mockLanguage(languageCode = "en", active = false)
    mockLanguageScreen(state = State.Content(listOf(language)))
    composeTestRule
      .onNodeWithContentDescription(
        context.getString(R.string.select_language_content_description)
      ).assertIsDisplayed()
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
      ).performClick()
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
      ).assertDoesNotExist()
  }

  @Test
  fun languageScreen_whenSelectedItemDraggedDown_moveDownCallbackIsTriggered() {
    var movedDownItem: LanguageListItem.LanguageItem? = null
    val lang1 = mockLanguage(languageCode = "en", active = true, id = 1L)
    val lang2 = mockLanguage(languageCode = "de", active = true, id = 2L)
    mockLanguageScreen(
      state = State.Content(listOf(lang1, lang2)),
      onMoveDown = { movedDownItem = it }
    )
    composeTestRule
      .onNodeWithTag("$LANGUAGE_ITEM_RADIO_BUTTON_TESTING_TAG${lang1.language}")
      .performTouchInput {
        down(center)
        advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
        moveBy(Offset(0f, 200f))
        up()
      }
    assertTrue("onMoveDown should be triggered on long press drag", movedDownItem != null)
  }

  @Test
  fun languageScreen_whenSelectedItemDraggedUp_moveUpCallbackIsTriggered() {
    var movedUpItem: LanguageListItem.LanguageItem? = null
    val lang1 = mockLanguage(languageCode = "en", active = true, id = 1L)
    val lang2 = mockLanguage(languageCode = "de", active = true, id = 2L)
    mockLanguageScreen(
      state = State.Content(listOf(lang1, lang2)),
      onMoveUp = { movedUpItem = it }
    )
    composeTestRule
      .onNodeWithTag("$LANGUAGE_ITEM_RADIO_BUTTON_TESTING_TAG${lang2.language}")
      .performTouchInput {
        down(center)
        advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
        moveBy(Offset(0f, -200f))
        up()
      }
    assertTrue("onMoveUp should be triggered on long press drag", movedUpItem != null)
  }

  @Test
  fun languageScreen_whenSelectedItemsDisplayed_rankTextsAreDisplayedInOrder() {
    val lang1 = mockLanguage(languageCode = "en", active = true, id = 1L)
    val lang2 = mockLanguage(languageCode = "de", active = true, id = 2L)
    mockLanguageScreen(state = State.Content(listOf(lang1, lang2)))
    composeTestRule.onNodeWithText("1.").assertIsDisplayed()
    composeTestRule.onNodeWithText("2.").assertIsDisplayed()
  }

  @Test
  fun languageScreen_whenFirstSelectedItemDraggedUp_moveUpCallbackIsNotTriggered() {
    var movedUpItem: LanguageListItem.LanguageItem? = null
    val lang1 = mockLanguage(languageCode = "en", active = true, id = 1L)
    val lang2 = mockLanguage(languageCode = "de", active = true, id = 2L)
    mockLanguageScreen(
      state = State.Content(listOf(lang1, lang2)),
      onMoveUp = { movedUpItem = it }
    )
    composeTestRule
      .onNodeWithTag("$LANGUAGE_ITEM_RADIO_BUTTON_TESTING_TAG${lang1.language}")
      .performTouchInput {
        down(center)
        advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
        moveBy(Offset(0f, -200f))
        up()
      }
    assertTrue(
      "onMoveUp should not be triggered for the first selected item",
      movedUpItem == null
    )
  }

  @Test
  fun languageScreen_whenLastSelectedItemDraggedDown_moveDownCallbackIsNotTriggered() {
    var movedDownItem: LanguageListItem.LanguageItem? = null
    val lang1 = mockLanguage(languageCode = "en", active = true, id = 1L)
    val lang2 = mockLanguage(languageCode = "de", active = true, id = 2L)
    mockLanguageScreen(
      state = State.Content(listOf(lang1, lang2)),
      onMoveDown = { movedDownItem = it }
    )
    composeTestRule
      .onNodeWithTag("$LANGUAGE_ITEM_RADIO_BUTTON_TESTING_TAG${lang2.language}")
      .performTouchInput {
        down(center)
        advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
        moveBy(Offset(0f, 200f))
        up()
      }
    assertTrue(
      "onMoveDown should not be triggered for the last selected item",
      movedDownItem == null
    )
  }

  @Test
  fun languageScreen_whenOtherSectionItemLongPressDragged_moveCallbacksAreNotTriggered() {
    var movedUpItem: LanguageListItem.LanguageItem? = null
    var movedDownItem: LanguageListItem.LanguageItem? = null
    val otherLanguage = mockLanguage(languageCode = "en", active = false)
    mockLanguageScreen(
      state = State.Content(listOf(otherLanguage)),
      onMoveUp = { movedUpItem = it },
      onMoveDown = { movedDownItem = it }
    )
    composeTestRule
      .onNodeWithTag("$LANGUAGE_ITEM_RADIO_BUTTON_TESTING_TAG${otherLanguage.language}")
      .performTouchInput {
        down(center)
        advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
        moveBy(Offset(0f, 200f))
        up()
      }
    assertTrue(
      "Other-section items should not support reorder drag",
      movedUpItem == null && movedDownItem == null
    )
  }

  @Test
  fun languageScreen_whenOtherSectionItem_reorderIconAndRankAreNotDisplayed() {
    val otherLanguage = mockLanguage(languageCode = "en", active = false)
    mockLanguageScreen(state = State.Content(listOf(otherLanguage)))
    composeTestRule
      .onNodeWithContentDescription(context.getString(R.string.reorder_language))
      .assertDoesNotExist()
    composeTestRule.onNodeWithText("1.").assertDoesNotExist()
  }

  @Test
  fun languageScreen_whenFirstSelectedItem_moveUpActionUnavailableAndMoveDownActionWorks() {
    var movedDownItem: LanguageListItem.LanguageItem? = null
    val lang1 = mockLanguage(languageCode = "en", active = true, id = 1L)
    val lang2 = mockLanguage(languageCode = "de", active = true, id = 2L)
    mockLanguageScreen(
      state = State.Content(listOf(lang1, lang2)),
      onMoveDown = { movedDownItem = it }
    )
    val moveUpLabel = context.getString(R.string.move_up)
    val moveDownLabel = context.getString(R.string.move_down)
    val actions = customAccessibilityActions(
      composeTestRule.onAllNodesWithContentDescription(
        context.getString(R.string.reorder_language)
      )[0]
    )
    assertTrue(
      "First item should not offer a move up accessibility action",
      actions.none { it.label == moveUpLabel }
    )
    val moveDownAction = actions.first { it.label == moveDownLabel }
    composeTestRule.runOnIdle { moveDownAction.action() }
    composeTestRule.waitForIdle()
    assertTrue(
      "Invoking the move down accessibility action should trigger onMoveDown",
      movedDownItem != null
    )
  }

  @Test
  fun languageScreen_whenLastSelectedItem_moveDownActionUnavailableAndMoveUpActionWorks() {
    var movedUpItem: LanguageListItem.LanguageItem? = null
    val lang1 = mockLanguage(languageCode = "en", active = true, id = 1L)
    val lang2 = mockLanguage(languageCode = "de", active = true, id = 2L)
    mockLanguageScreen(
      state = State.Content(listOf(lang1, lang2)),
      onMoveUp = { movedUpItem = it }
    )
    val moveUpLabel = context.getString(R.string.move_up)
    val moveDownLabel = context.getString(R.string.move_down)
    val actions = customAccessibilityActions(
      composeTestRule.onAllNodesWithContentDescription(
        context.getString(R.string.reorder_language)
      )[1]
    )
    assertTrue(
      "Last item should not offer a move down accessibility action",
      actions.none { it.label == moveDownLabel }
    )
    val moveUpAction = actions.first { it.label == moveUpLabel }
    composeTestRule.runOnIdle { moveUpAction.action() }
    composeTestRule.waitForIdle()
    assertTrue(
      "Invoking the move up accessibility action should trigger onMoveUp",
      movedUpItem != null
    )
  }

  @Test
  fun languageScreen_whenSearchFilterActive_reorderActionsAreUnavailable() {
    val lang1 = Language(
      id = 1L,
      active = true,
      occurencesOfLanguage = 5,
      language = "English",
      languageLocalized = "English",
      languageCode = "en",
      languageCodeISO2 = "eng"
    )
    val lang2 = Language(
      id = 2L,
      active = true,
      occurencesOfLanguage = 5,
      language = "German",
      languageLocalized = "Deutsch",
      languageCode = "de",
      languageCodeISO2 = "deu"
    )
    mockLanguageScreen(state = State.Content(listOf(lang1, lang2), filter = "e"))
    val actions = customAccessibilityActions(
      composeTestRule.onAllNodesWithContentDescription(
        context.getString(R.string.reorder_language)
      )[1]
    )
    assertTrue(
      "Reorder actions should be unavailable while a search filter is active",
      actions.isEmpty()
    )
  }

  @Test
  fun languageScreen_whenBookCountIsOne_singularBookCountIsDisplayed() {
    val language = mockLanguage(languageCode = "en", active = false, occurrencesOfLanguage = 1)
    mockLanguageScreen(state = State.Content(listOf(language)))
    val expected = context.resources.getQuantityString(R.plurals.book_count, 1, 1)
    composeTestRule.onNodeWithText(expected).assertIsDisplayed()
  }

  @Test
  fun languageScreen_whenBookCountIsMultiple_pluralBookCountIsDisplayed() {
    val language = mockLanguage(languageCode = "en", active = false, occurrencesOfLanguage = 5)
    mockLanguageScreen(state = State.Content(listOf(language)))
    val expected = context.resources.getQuantityString(R.plurals.book_count, 5, 5)
    composeTestRule.onNodeWithText(expected).assertIsDisplayed()
  }

  @Test
  fun languageScreen_whenLocalizedNameDiffersFromLanguageName_localizedNameIsDisplayed() {
    val language = Language(
      id = 1L,
      active = false,
      occurencesOfLanguage = 5,
      language = "German",
      languageLocalized = "Deutsch",
      languageCode = "de",
      languageCodeISO2 = "deu"
    )
    mockLanguageScreen(state = State.Content(listOf(language)))
    composeTestRule.onNodeWithText("German").assertIsDisplayed()
    composeTestRule.onNodeWithText("Deutsch").assertIsDisplayed()
  }

  @Test
  fun languageScreen_whenLocalizedNameEqualsLanguageName_localizedNameIsNotDuplicated() {
    val language = Language(
      id = 1L,
      active = false,
      occurencesOfLanguage = 5,
      language = "English",
      languageLocalized = "English",
      languageCode = "en",
      languageCodeISO2 = "eng"
    )
    mockLanguageScreen(state = State.Content(listOf(language)))
    composeTestRule
      .onAllNodesWithText("English")
      .assertCountEquals(1)
  }

  @Test
  fun languageScreen_whenFilterMatchesLanguage_matchingItemDisplayedAndNonMatchingHidden() {
    val english = Language(
      id = 1L,
      active = false,
      occurencesOfLanguage = 5,
      language = "English",
      languageLocalized = "English",
      languageCode = "en",
      languageCodeISO2 = "eng"
    )
    val german = Language(
      id = 2L,
      active = false,
      occurencesOfLanguage = 5,
      language = "German",
      languageLocalized = "Deutsch",
      languageCode = "de",
      languageCodeISO2 = "deu"
    )
    mockLanguageScreen(state = State.Content(listOf(english, german), filter = "Ger"))
    composeTestRule.onNodeWithText("German").assertIsDisplayed()
    composeTestRule.onNodeWithText("English").assertDoesNotExist()
  }
}
