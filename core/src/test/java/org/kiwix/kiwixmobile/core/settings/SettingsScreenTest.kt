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

package org.kiwix.kiwixmobile.core.settings

import android.os.Build
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.settings.viewmodel.CoreSettingsViewModel
import org.kiwix.kiwixmobile.core.settings.viewmodel.CoreSettingsViewModel.SettingsUiState
import org.kiwix.kiwixmobile.core.ui.components.NAVIGATION_ICON_TESTING_TAG
import org.kiwix.kiwixmobile.core.ui.components.NavigationIcon
import org.kiwix.kiwixmobile.core.ui.components.TOOLBAR_TITLE_TESTING_TAG
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore.Companion.DEFAULT_ZOOM
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Behavior-driven UI tests for SettingsScreen.
 *
 * All tests render through the [SettingsScreen] composable,
 * using a mocked [CoreSettingsViewModel] to drive different UI states.
 * This ensures we test real user-visible behavior, not internal
 * composable implementation details.
 */
@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])
class SettingsScreenTest {
  @get:Rule
  val composeTestRule = createComposeRule()

  private val context get() = RuntimeEnvironment.getApplication()

  /**
   * Creates a mocked [CoreSettingsViewModel] with sensible defaults.
   * Test-specific overrides can be applied via the [uiState] parameter.
   */
  private fun createMockViewModel(
    uiState: SettingsUiState = SettingsUiState(),
    themeLabel: String = "System default",
    backToTopEnabled: Boolean = false,
    textZoom: Int = DEFAULT_ZOOM,
    newTabInBackground: Boolean = false,
    externalLinkPopup: Boolean = true,
    wifiOnly: Boolean = true
  ): CoreSettingsViewModel {
    val viewModel = mockk<CoreSettingsViewModel>(relaxed = true)
    every { viewModel.uiState } returns MutableStateFlow(uiState)
    every { viewModel.themeLabel } returns MutableStateFlow(themeLabel)
    every { viewModel.backToTopEnabled } returns MutableStateFlow(backToTopEnabled)
    every { viewModel.textZoom } returns MutableStateFlow(textZoom)
    every { viewModel.newTabInBackground } returns MutableStateFlow(newTabInBackground)
    every { viewModel.externalLinkPopup } returns MutableStateFlow(externalLinkPopup)
    every { viewModel.wifiOnly } returns MutableStateFlow(wifiOnly)
    return viewModel
  }

  /**
   * Renders the [SettingsScreen] composable with the provided mocked ViewModel
   * and an optional navigation icon composable.
   */
  private fun renderSettingsScreen(
    viewModel: CoreSettingsViewModel,
    onNavigationClick: () -> Unit = {}
  ) {
    composeTestRule.setContent {
      SettingsScreen(
        coreSettingsViewModel = viewModel,
        navigationIcon = { NavigationIcon(onClick = onNavigationClick) }
      )
    }
  }

  /**
   * Scrolls the settings LazyColumn to bring the node matching [text] into view.
   */
  private fun scrollToText(text: String) {
    composeTestRule
      .onNodeWithTag(SETTINGS_LIST_TESTING_TAG)
      .performScrollToNode(hasText(text))
  }

  /**
   * Scrolls the settings LazyColumn to bring the node matching [testTag] into view.
   */
  private fun scrollToTag(testTag: String) {
    composeTestRule
      .onNodeWithTag(SETTINGS_LIST_TESTING_TAG)
      .performScrollToNode(hasTestTag(testTag))
  }

  /**
   * Scrolls the settings LazyColumn to bring the node
   * matching [contentDescription] into view.
   */
  private fun scrollToContentDescription(contentDescription: String) {
    composeTestRule
      .onNodeWithTag(SETTINGS_LIST_TESTING_TAG)
      .performScrollToNode(hasContentDescription(contentDescription))
  }

  /**
   * Clicks a SwitchPreference identified by its title (contentDescription).
   * Uses the testTag to disambiguate the Column container from
   * the inner Switch, which share the same contentDescription.
   */
  private fun clickSwitchPreference(title: String) {
    scrollToTag(SWITCH_PREFERENCE_TESTING_TAG)
    // Scroll specifically to the switch with this content description
    scrollToContentDescription(title)
    composeTestRule
      .onNodeWithTag(SWITCH_PREFERENCE_TESTING_TAG)
      .performClick()
  }

  @Test
  fun settingsScreen_topBar_displaysSettingsTitle() {
    renderSettingsScreen(createMockViewModel())
    composeTestRule
      .onNodeWithTag(TOOLBAR_TITLE_TESTING_TAG, useUnmergedTree = true)
      .assertIsDisplayed()
      .assertTextEquals(context.getString(R.string.menu_settings))
  }

  @Test
  fun settingsScreen_topBar_navigationIconClick_triggersCallback() {
    var clicked = false
    renderSettingsScreen(
      viewModel = createMockViewModel(),
      onNavigationClick = { clicked = true }
    )
    composeTestRule
      .onNodeWithTag(NAVIGATION_ICON_TESTING_TAG)
      .performClick()
    assertTrue("Navigation icon click callback should be triggered", clicked)
  }

  @Test
  fun settingsScreen_settingsList_isDisplayed() {
    renderSettingsScreen(createMockViewModel())
    composeTestRule
      .onNodeWithTag(SETTINGS_LIST_TESTING_TAG)
      .assertIsDisplayed()
  }

  @Test
  fun settingsScreen_displayCategory_isDisplayed() {
    renderSettingsScreen(createMockViewModel())
    composeTestRule
      .onNodeWithText(context.getString(R.string.pref_display_title))
      .assertIsDisplayed()
  }

  @Test
  fun settingsScreen_displayCategory_themePreference_isDisplayed() {
    renderSettingsScreen(createMockViewModel(themeLabel = "System default"))
    composeTestRule
      .onNodeWithText(context.getString(R.string.pref_theme))
      .assertIsDisplayed()
  }

  @Test
  fun settingsScreen_displayCategory_backToTopSwitch_isDisplayed() {
    renderSettingsScreen(createMockViewModel())
    scrollToContentDescription(context.getString(R.string.pref_back_to_top))
    composeTestRule
      .onNodeWithText(context.getString(R.string.pref_back_to_top))
      .assertIsDisplayed()
  }

  @Test
  fun settingsScreen_displayCategory_textZoomSeekBar_isDisplayed() {
    renderSettingsScreen(createMockViewModel())
    scrollToContentDescription(context.getString(R.string.pref_text_zoom_title))
    composeTestRule
      .onNodeWithText(context.getString(R.string.pref_text_zoom_title))
      .assertIsDisplayed()
  }

  @Test
  fun settingsScreen_displayCategory_backToTopSwitch_toggleTriggersCallback() {
    val viewModel = createMockViewModel(backToTopEnabled = false)
    renderSettingsScreen(viewModel)
    // Click the back-to-top switch preference by text
    scrollToText(context.getString(R.string.pref_back_to_top))
    composeTestRule
      .onNodeWithText(context.getString(R.string.pref_back_to_top_summary))
      .performClick()
    verify { viewModel.setBackToTop(true) }
  }

  @Test
  fun settingsScreen_extrasCategory_isDisplayed() {
    renderSettingsScreen(createMockViewModel())
    scrollToText(context.getString(R.string.pref_extras))
    composeTestRule
      .onNodeWithText(context.getString(R.string.pref_extras))
      .assertIsDisplayed()
  }

  @Test
  fun settingsScreen_extrasCategory_newTabInBackgroundSwitch_isDisplayed() {
    renderSettingsScreen(createMockViewModel())
    scrollToContentDescription(context.getString(R.string.pref_newtab_background_title))
    composeTestRule
      .onNodeWithText(context.getString(R.string.pref_newtab_background_title))
      .assertIsDisplayed()
  }

  @Test
  fun settingsScreen_extrasCategory_externalLinkPreference_visibleWhenEnabled() {
    renderSettingsScreen(
      createMockViewModel(
        uiState = SettingsUiState(shouldShowExternalLinkPreference = true)
      )
    )
    scrollToContentDescription(context.getString(R.string.pref_external_link_popup_title))
    composeTestRule
      .onNodeWithText(context.getString(R.string.pref_external_link_popup_title))
      .assertIsDisplayed()
  }

  @Test
  fun settingsScreen_extrasCategory_externalLinkPreference_hiddenWhenDisabled() {
    renderSettingsScreen(
      createMockViewModel(
        uiState = SettingsUiState(shouldShowExternalLinkPreference = false)
      )
    )
    composeTestRule
      .onNodeWithText(context.getString(R.string.pref_external_link_popup_title))
      .assertDoesNotExist()
  }

  @Test
  fun settingsScreen_extrasCategory_wifiOnlyPreference_visibleWhenEnabled() {
    renderSettingsScreen(
      createMockViewModel(
        uiState = SettingsUiState(shouldShowPrefWifiOnlyPreference = true)
      )
    )
    scrollToContentDescription(context.getString(R.string.pref_wifi_only))
    composeTestRule
      .onNodeWithText(context.getString(R.string.pref_wifi_only))
      .assertIsDisplayed()
  }

  @Test
  fun settingsScreen_extrasCategory_wifiOnlyPreference_hiddenWhenDisabled() {
    renderSettingsScreen(
      createMockViewModel(
        uiState = SettingsUiState(shouldShowPrefWifiOnlyPreference = false)
      )
    )
    composeTestRule
      .onNodeWithContentDescription(context.getString(R.string.pref_wifi_only))
      .assertDoesNotExist()
  }

  @Test
  fun settingsScreen_extrasCategory_newTabToggle_triggersCallback() {
    val viewModel = createMockViewModel(newTabInBackground = false)
    renderSettingsScreen(viewModel)
    scrollToText(context.getString(R.string.pref_newtab_background_summary))
    composeTestRule
      .onNodeWithText(context.getString(R.string.pref_newtab_background_summary))
      .performClick()
    verify { viewModel.setNewTabInBackground(true) }
  }

  @Test
  fun settingsScreen_extrasCategory_externalLinkToggle_triggersCallback() {
    val viewModel = createMockViewModel(
      uiState = SettingsUiState(shouldShowExternalLinkPreference = true),
      externalLinkPopup = true
    )
    renderSettingsScreen(viewModel)
    scrollToText(context.getString(R.string.pref_external_link_popup_summary))
    composeTestRule
      .onNodeWithText(context.getString(R.string.pref_external_link_popup_summary))
      .performClick()
    verify { viewModel.setExternalLinkPopup(false) }
  }

  @Test
  fun settingsScreen_extrasCategory_wifiOnlyToggle_triggersCallback() {
    val viewModel = createMockViewModel(
      uiState = SettingsUiState(shouldShowPrefWifiOnlyPreference = true),
      wifiOnly = true
    )
    renderSettingsScreen(viewModel)
    scrollToContentDescription(context.getString(R.string.pref_wifi_only))
    // pref_wifi_only is used for both title and summary so use onAllNodesWithText to avoid
    // ambiguous-node errors and pick the first match.
    composeTestRule
      .onAllNodesWithText(context.getString(R.string.pref_wifi_only))
      .onFirst()
      .performClick()
    verify { viewModel.setWifiOnly(false) }
  }

  @Test
  fun settingsScreen_historyCategory_isDisplayed() {
    renderSettingsScreen(createMockViewModel())
    scrollToText(context.getString(R.string.history))
    composeTestRule
      .onNodeWithText(context.getString(R.string.history))
      .assertIsDisplayed()
  }

  @Test
  fun settingsScreen_historyCategory_clearHistoryItem_isDisplayed() {
    val title = context.getString(R.string.pref_clear_all_history_title)
    renderSettingsScreen(createMockViewModel())
    scrollToTag(PREFERENCE_ITEM_TESTING_TAG + title)
    composeTestRule
      .onNodeWithText(title)
      .assertIsDisplayed()
  }

  @Test
  fun settingsScreen_historyCategory_clearHistoryItem_isClickable() {
    val title = context.getString(R.string.pref_clear_all_history_title)
    val viewModel = createMockViewModel()
    renderSettingsScreen(viewModel)
    scrollToTag(PREFERENCE_ITEM_TESTING_TAG + title)
    composeTestRule
      .onNodeWithTag(PREFERENCE_ITEM_TESTING_TAG + title)
      .performClick()
    verify { viewModel.sendAction(any()) }
  }

  @Test
  fun settingsScreen_notesCategory_isDisplayed() {
    renderSettingsScreen(createMockViewModel())
    scrollToText(context.getString(R.string.pref_notes))
    composeTestRule
      .onNodeWithText(context.getString(R.string.pref_notes))
      .assertIsDisplayed()
  }

  @Test
  fun settingsScreen_notesCategory_clearNotesItem_isDisplayed() {
    val title = context.getString(R.string.pref_clear_all_notes_title)
    renderSettingsScreen(createMockViewModel())
    scrollToTag(PREFERENCE_ITEM_TESTING_TAG + title)
    composeTestRule
      .onNodeWithText(title)
      .assertIsDisplayed()
  }

  @Test
  fun settingsScreen_notesCategory_clearNotesItem_isClickable() {
    val title = context.getString(R.string.pref_clear_all_notes_title)
    val viewModel = createMockViewModel()
    renderSettingsScreen(viewModel)
    scrollToTag(PREFERENCE_ITEM_TESTING_TAG + title)
    composeTestRule
      .onNodeWithTag(PREFERENCE_ITEM_TESTING_TAG + title)
      .performClick()
    verify { viewModel.sendAction(any()) }
  }

  @Test
  fun settingsScreen_bookmarksCategory_isDisplayed() {
    renderSettingsScreen(createMockViewModel())
    scrollToText(context.getString(R.string.bookmarks))
    composeTestRule
      .onNodeWithText(context.getString(R.string.bookmarks))
      .assertIsDisplayed()
  }

  @Test
  fun settingsScreen_bookmarksCategory_importBookmarksItem_isDisplayed() {
    val title = context.getString(R.string.pref_import_bookmark_title)
    renderSettingsScreen(createMockViewModel())
    scrollToTag(PREFERENCE_ITEM_TESTING_TAG + title)
    composeTestRule
      .onNodeWithText(title)
      .assertIsDisplayed()
  }

  @Test
  fun settingsScreen_bookmarksCategory_exportBookmarksItem_isDisplayed() {
    val title = context.getString(R.string.pref_export_bookmark_title)
    renderSettingsScreen(createMockViewModel())
    scrollToTag(PREFERENCE_ITEM_TESTING_TAG + title)
    composeTestRule
      .onNodeWithText(title)
      .assertIsDisplayed()
  }

  @Test
  fun settingsScreen_bookmarksCategory_importBookmarksItem_isClickable() {
    val title = context.getString(R.string.pref_import_bookmark_title)
    val viewModel = createMockViewModel()
    renderSettingsScreen(viewModel)
    scrollToTag(PREFERENCE_ITEM_TESTING_TAG + title)
    composeTestRule
      .onNodeWithTag(PREFERENCE_ITEM_TESTING_TAG + title)
      .performClick()
    verify { viewModel.sendAction(any()) }
  }

  @Test
  fun settingsScreen_bookmarksCategory_exportBookmarksItem_isClickable() {
    val title = context.getString(R.string.pref_export_bookmark_title)
    val viewModel = createMockViewModel()
    renderSettingsScreen(viewModel)
    scrollToTag(PREFERENCE_ITEM_TESTING_TAG + title)
    composeTestRule
      .onNodeWithTag(PREFERENCE_ITEM_TESTING_TAG + title)
      .performClick()
    verify { viewModel.sendAction(any()) }
  }

  @Test
  fun settingsScreen_permissionCategory_visibleWhenEnabled() {
    renderSettingsScreen(
      createMockViewModel(
        uiState = SettingsUiState(permissionItem = true to "Granted")
      )
    )
    scrollToText(context.getString(R.string.pref_permission))
    composeTestRule
      .onNodeWithText(context.getString(R.string.pref_permission))
      .assertIsDisplayed()
  }

  @Test
  fun settingsScreen_permissionCategory_hiddenWhenDisabled() {
    renderSettingsScreen(
      createMockViewModel(
        uiState = SettingsUiState(permissionItem = false to "")
      )
    )
    composeTestRule
      .onNodeWithText(context.getString(R.string.pref_permission))
      .assertDoesNotExist()
  }

  @Test
  fun settingsScreen_permissionCategory_permissionItem_isClickable() {
    val title = context.getString(R.string.pref_allow_to_read_or_write_zim_files_on_sd_card)
    val viewModel = createMockViewModel(
      uiState = SettingsUiState(permissionItem = true to "Granted")
    )
    renderSettingsScreen(viewModel)
    scrollToTag(PREFERENCE_ITEM_TESTING_TAG + title)
    composeTestRule
      .onNodeWithTag(PREFERENCE_ITEM_TESTING_TAG + title)
      .performClick()
    verify { viewModel.sendAction(any()) }
  }

  @Test
  fun settingsScreen_storageCategory_hiddenWhenDisabled() {
    renderSettingsScreen(
      createMockViewModel(
        uiState = SettingsUiState(shouldShowStorageCategory = false)
      )
    )
    composeTestRule
      .onNodeWithText(context.getString(R.string.pref_storage))
      .assertDoesNotExist()
  }

  @Test
  fun settingsScreen_storageCategory_visibleWhenEnabled() {
    renderSettingsScreen(
      createMockViewModel(
        uiState = SettingsUiState(
          shouldShowStorageCategory = true,
          isLoadingStorageDetails = true
        )
      )
    )
    scrollToText(context.getString(R.string.pref_storage))
    composeTestRule
      .onNodeWithText(context.getString(R.string.pref_storage))
      .assertIsDisplayed()
  }

  @Test
  fun settingsScreen_storageCategory_showsLoadingWhenFetching() {
    renderSettingsScreen(
      createMockViewModel(
        uiState = SettingsUiState(
          shouldShowStorageCategory = true,
          isLoadingStorageDetails = true
        )
      )
    )
    scrollToText(context.getString(R.string.fetching_storage_info))
    composeTestRule
      .onNodeWithText(context.getString(R.string.fetching_storage_info))
      .assertIsDisplayed()
  }

  @Test
  fun settingsScreen_informationCategory_isDisplayed() {
    renderSettingsScreen(createMockViewModel())
    scrollToText(context.getString(R.string.pref_info_title))
    composeTestRule
      .onNodeWithText(context.getString(R.string.pref_info_title))
      .assertIsDisplayed()
  }

  @Test
  fun settingsScreen_informationCategory_versionItem_isDisplayed() {
    val title = context.getString(R.string.pref_info_version)
    renderSettingsScreen(createMockViewModel())
    scrollToTag(PREFERENCE_ITEM_TESTING_TAG + title)
    composeTestRule
      .onNodeWithText(title)
      .assertIsDisplayed()
  }

  @Test
  fun settingsScreen_informationCategory_creditsItem_isDisplayed() {
    val title = context.getString(R.string.pref_credits)
    renderSettingsScreen(createMockViewModel())
    scrollToTag(PREFERENCE_ITEM_TESTING_TAG + title)
    composeTestRule
      .onNodeWithText(title)
      .assertIsDisplayed()
  }

  @Test
  fun settingsScreen_informationCategory_creditsItem_isClickable() {
    val title = context.getString(R.string.pref_credits)
    val viewModel = createMockViewModel()
    renderSettingsScreen(viewModel)
    scrollToTag(PREFERENCE_ITEM_TESTING_TAG + title)
    composeTestRule
      .onNodeWithTag(PREFERENCE_ITEM_TESTING_TAG + title)
      .performClick()
    verify { viewModel.sendAction(any()) }
  }

  @Test
  fun settingsScreen_informationCategory_displaysVersionInfo() {
    val versionInfo = "1.0.0 Build: 100"
    renderSettingsScreen(
      createMockViewModel(
        uiState = SettingsUiState(versionInformation = versionInfo)
      )
    )
    scrollToText(versionInfo)
    composeTestRule
      .onNodeWithText(versionInfo)
      .assertIsDisplayed()
  }

  @Test
  fun settingsScreen_languageCategory_hiddenWhenDisabled() {
    renderSettingsScreen(
      createMockViewModel(
        uiState = SettingsUiState(shouldShowLanguageCategory = false)
      )
    )
    composeTestRule
      .onNodeWithText(context.getString(R.string.pref_language_title))
      .assertDoesNotExist()
  }

  @Test
  fun listPreference_opensDialogOnClick() {
    composeTestRule.setContent {
      ListPreference(
        titleId = R.string.pref_theme,
        summary = "System default",
        options = listOf("Light", "Dark", "System default"),
        selectedOption = "System default",
        onOptionSelected = {}
      )
    }
    composeTestRule
      .onNodeWithText(context.getString(R.string.pref_theme))
      .performClick()
    composeTestRule
      .onNodeWithText("Light")
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithText("Dark")
      .assertIsDisplayed()
  }

  @Test
  fun listPreference_selectingOption_triggersCallback() {
    var selectedOption = ""
    composeTestRule.setContent {
      ListPreference(
        titleId = R.string.pref_theme,
        summary = "System default",
        options = listOf("Light", "Dark", "System default"),
        selectedOption = "System default",
        onOptionSelected = { selectedOption = it }
      )
    }
    composeTestRule
      .onNodeWithText(context.getString(R.string.pref_theme))
      .performClick()
    composeTestRule
      .onNodeWithContentDescription("Dark")
      .performClick()
    assertTrue(
      "Expected selected option to be 'Dark' but was '$selectedOption'",
      selectedOption == "Dark"
    )
  }

  @Test
  fun listPreference_cancelButton_dismissesDialog() {
    composeTestRule.setContent {
      ListPreference(
        titleId = R.string.pref_theme,
        summary = "System default",
        options = listOf("Light", "Dark", "System default"),
        selectedOption = "System default",
        onOptionSelected = {}
      )
    }
    composeTestRule
      .onNodeWithText(context.getString(R.string.pref_theme))
      .performClick()
    composeTestRule
      .onNodeWithText("Light")
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithText(context.getString(R.string.cancel).uppercase())
      .performClick()
    composeTestRule
      .onNodeWithText("Light")
      .assertDoesNotExist()
  }

  @Test
  fun listPreference_displaysCurrentSummary() {
    composeTestRule.setContent {
      ListPreference(
        titleId = R.string.pref_theme,
        summary = "Dark",
        options = listOf("Light", "Dark", "System default"),
        selectedOption = "Dark",
        onOptionSelected = {}
      )
    }
    composeTestRule
      .onNodeWithText("Dark")
      .assertIsDisplayed()
  }

  @Test
  fun appThemePreference_displaysThemeLabel() {
    val viewModel = createMockViewModel(themeLabel = "Dark")
    composeTestRule.setContent {
      AppThemePreference(
        context = context,
        themeLabel = "Dark",
        coreSettingsViewModel = viewModel
      )
    }
    composeTestRule
      .onNodeWithText("Dark")
      .assertIsDisplayed()
  }

  @Test
  fun appThemePreference_displaysThemeTitle() {
    val viewModel = createMockViewModel()
    composeTestRule.setContent {
      AppThemePreference(
        context = context,
        themeLabel = "System default",
        coreSettingsViewModel = viewModel
      )
    }
    composeTestRule
      .onNodeWithText(context.getString(R.string.pref_theme))
      .assertIsDisplayed()
  }
}
