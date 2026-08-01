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

import android.os.Build
import android.widget.FrameLayout
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.navigation.compose.rememberNavController
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.BackPressActivityExtensions
import org.kiwix.kiwixmobile.core.main.KiwixWebView
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderViewModel.ReaderAction
import org.kiwix.kiwixmobile.core.main.reader.helper.FindInPageManager
import org.kiwix.kiwixmobile.core.main.reader.helper.TabsManager
import org.kiwix.kiwixmobile.core.ui.components.CONTENT_LOADING_PROGRESS_BAR_TESTING_TAG
import org.kiwix.kiwixmobile.core.ui.components.FIND_IN_SEARCH_VIEW_TESTING_TAG
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Behavior-driven UI tests for ReaderScreen.
 *
 * All tests render through the top-level [ReaderScreen] composable,
 * using [CoreReaderViewModel.ReaderUiState] to drive different UI states.
 * This ensures we test real user-visible behavior, not internal
 * composable implementation details.
 */
@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])
class ReaderScreenComposablesTest {
  companion object {
    private const val TEST_TTS_SPEED = 1.5f
  }

  @Rule
  @JvmField
  val composeTestRule = createComposeRule()

  private val context get() = RuntimeEnvironment.getApplication()

  /**
   * Creates a minimal [CoreReaderViewModel.ReaderUiState] with sensible defaults
   * for testing. Test-specific values can be overridden via named parameters.
   */
  private fun createTestState(
    appName: String = "Kiwix",
    title: String = "Test Reader",
    loading: Boolean = false,
    progress: Int = 0,
    tabsState: TabsManager.TabsState = createTabsState(),
    videoView: FrameLayout? = null,
    shouldShowFullScreen: Boolean = false,
    showBackToTopButton: Boolean = false,
    showTtsControls: Boolean = false,
    showTabSwitcher: Boolean = false,
    showBottomBar: Boolean = true,
    bookmarkButtonItem: CoreReaderViewModel.BookmarkButtonItem =
      CoreReaderViewModel.BookmarkButtonItem(
        IconItem.Drawable(R.drawable.ic_bookmark_border_24dp),
        false
      ),
    showNoBookOpenInReader: Boolean = false,
    searchPlaceHolderItemForBrandedApps: Boolean = false,
    isPreviousPageButtonEnable: Boolean = true,
    isNextPageButtonEnable: Boolean = true,
    pauseTtsButtonText: String = "Pause",
    ttsSpeedText: String = "1.0X",
    isTocButtonEnable: Boolean = true,
    showTableOfContentDrawer: Boolean = false,
    tableOfContentTitle: String = "Contents",
    documentSections: List<DocumentSection> = emptyList(),
    showDonationPopup: Boolean = false,
    findInPageUiState: FindInPageManager.FindInPageUiState =
      FindInPageManager.FindInPageUiState()
  ): CoreReaderViewModel.ReaderUiState {
    return CoreReaderViewModel.ReaderUiState(
      appName = appName,
      title = title,
      loading = loading,
      progress = progress,
      tabsState = tabsState,
      videoView = videoView,
      shouldShowFullScreen = shouldShowFullScreen,
      showBackToTopButton = showBackToTopButton,
      showTtsControls = showTtsControls,
      showTabSwitcher = showTabSwitcher,
      showBottomBar = showBottomBar,
      bookmarkButtonItem = bookmarkButtonItem,
      showNoBookOpenInReader = showNoBookOpenInReader,
      searchPlaceHolderItemForBrandedApps = searchPlaceHolderItemForBrandedApps,
      isPreviousPageButtonEnable = isPreviousPageButtonEnable,
      isNextPageButtonEnable = isNextPageButtonEnable,
      pauseTtsButtonText = pauseTtsButtonText,
      ttsSpeedText = ttsSpeedText,
      isTocButtonEnable = isTocButtonEnable,
      showTableOfContentDrawer = showTableOfContentDrawer,
      tableOfContentTitle = tableOfContentTitle,
      documentSections = documentSections,
      showDonationPopup = showDonationPopup,
      findInPageUiState = findInPageUiState
    )
  }

  private fun createTabsState(
    webViews: List<KiwixWebView> = emptyList(),
    selectedIndex: Int = 0
  ) = TabsManager.TabsState(
    webViews = webViews,
    selectedIndex = selectedIndex
  )

  /**
   * Renders the full [ReaderScreen] with the given state,
   * providing minimal test doubles for required dependencies.
   */
  private fun renderReaderScreen(
    state: CoreReaderViewModel.ReaderUiState,
    onReaderAction: (ReaderAction) -> Unit = {}
  ) {
    composeTestRule.setContent {
      val navController = rememberNavController()
      val snackBarHostState = remember { SnackbarHostState() }
      ReaderScreen(
        state = state,
        snackBarHost = snackBarHostState,
        onReaderAction = onReaderAction,
        actionMenuItems = emptyList(),
        onUserBackPressed = { BackPressActivityExtensions.Super.ShouldCall },
        navHostController = navController,
        mainActivityBottomAppBarScrollBehaviour = null,
        navigationIcon = {}
      )
    }
  }

  @Test
  fun readerScreen_bottomAppBar_displaysAllButtons() {
    renderReaderScreen(createTestState(showBottomBar = true))
    composeTestRule
      .onNodeWithTag(READER_BOTTOM_BAR_BOOKMARK_BUTTON_TESTING_TAG)
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithTag(READER_BOTTOM_BAR_PREVIOUS_SCREEN_BUTTON_TESTING_TAG)
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithTag(READER_BOTTOM_BAR_HOME_BUTTON_TESTING_TAG)
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithTag(READER_BOTTOM_BAR_NEXT_SCREEN_BUTTON_TESTING_TAG)
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithTag(READER_BOTTOM_BAR_TABLE_CONTENT_BUTTON_TESTING_TAG)
      .assertIsDisplayed()
  }

  @Test
  fun readerScreen_bottomAppBar_bookmarkClick_triggersCallback() {
    var action: ReaderAction? = null
    renderReaderScreen(
      createTestState(showBottomBar = true),
      onReaderAction = { action = it }
    )
    composeTestRule
      .onNodeWithTag(READER_BOTTOM_BAR_BOOKMARK_BUTTON_TESTING_TAG)
      .performClick()
    assertEquals(ReaderAction.BookmarkClicked, action)
  }

  @Test
  fun readerScreen_bottomAppBar_bookmarkLongClick_triggersCallback() {
    var action: ReaderAction? = null
    renderReaderScreen(
      createTestState(showBottomBar = true),
      onReaderAction = { action = it }
    )
    composeTestRule
      .onNodeWithTag(READER_BOTTOM_BAR_BOOKMARK_BUTTON_TESTING_TAG)
      .performTouchInput { longClick() }
    assertEquals(ReaderAction.BookmarkLongClicked, action)
  }

  @Test
  fun readerScreen_bottomAppBar_homeClick_triggersCallback() {
    var action: ReaderAction? = null
    renderReaderScreen(
      createTestState(showBottomBar = true),
      onReaderAction = { action = it }
    )
    composeTestRule
      .onNodeWithTag(READER_BOTTOM_BAR_HOME_BUTTON_TESTING_TAG)
      .performClick()
    assertEquals(ReaderAction.HomeClicked, action)
  }

  @Test
  fun readerScreen_bottomAppBar_previousPageClick_triggersCallback() {
    var action: ReaderAction? = null
    renderReaderScreen(
      createTestState(showBottomBar = true),
      onReaderAction = { action = it }
    )
    composeTestRule
      .onNodeWithTag(READER_BOTTOM_BAR_PREVIOUS_SCREEN_BUTTON_TESTING_TAG)
      .performClick()
    assertEquals(ReaderAction.PreviousClicked, action)
  }

  @Test
  fun readerScreen_bottomAppBar_nextPageClick_triggersCallback() {
    var action: ReaderAction? = null
    renderReaderScreen(
      createTestState(showBottomBar = true),
      onReaderAction = { action = it }
    )
    composeTestRule
      .onNodeWithTag(READER_BOTTOM_BAR_NEXT_SCREEN_BUTTON_TESTING_TAG)
      .performClick()
    assertEquals(ReaderAction.NextClicked, action)
  }

  @Test
  fun readerScreen_bottomAppBar_disabledButton_doesNotTriggerCallback() {
    var action: ReaderAction? = null
    renderReaderScreen(
      createTestState(showBottomBar = true, isPreviousPageButtonEnable = false),
      onReaderAction = { action = it }
    )
    composeTestRule
      .onNodeWithTag(READER_BOTTOM_BAR_PREVIOUS_SCREEN_BUTTON_TESTING_TAG)
      .performClick()
    assertEquals(null, action)
  }

  @Test
  fun readerScreen_bottomAppBar_notShownWhenShouldShowIsFalse() {
    renderReaderScreen(
      createTestState(showBottomBar = false)
    )
    composeTestRule
      .onNodeWithTag(READER_BOTTOM_BAR_BOOKMARK_BUTTON_TESTING_TAG)
      .assertDoesNotExist()
  }

  @Test
  fun readerScreen_ttsControls_visibleWhenActive() {
    renderReaderScreen(
      createTestState(
        showTtsControls = true,
        pauseTtsButtonText = "Pause"
      )
    )
    composeTestRule
      .onNodeWithTag(TTS_CONTROL_PLAY_PAUSE_BUTTON_TESTING_TAG)
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithTag(TTS_CONTROL_STOP_BUTTON_TESTING_TAG)
      .assertIsDisplayed()
  }

  @Test
  fun readerScreen_ttsControls_hiddenWhenInactive() {
    renderReaderScreen(createTestState(showTtsControls = false))
    composeTestRule
      .onNodeWithTag(TTS_CONTROL_PLAY_PAUSE_BUTTON_TESTING_TAG)
      .assertDoesNotExist()
    composeTestRule
      .onNodeWithTag(TTS_CONTROL_STOP_BUTTON_TESTING_TAG)
      .assertDoesNotExist()
  }

  @Test
  fun readerScreen_ttsControls_pauseButton_triggersCallback() {
    var action: ReaderAction? = null
    renderReaderScreen(
      createTestState(
        showTtsControls = true,
        pauseTtsButtonText = "Pause"
      ),
      onReaderAction = { action = it }
    )
    composeTestRule
      .onNodeWithTag(TTS_CONTROL_PLAY_PAUSE_BUTTON_TESTING_TAG)
      .performClick()
    assertEquals(ReaderAction.PauseTts, action)
  }

  @Test
  fun readerScreen_ttsControls_stopButton_triggersCallback() {
    var action: ReaderAction? = null
    renderReaderScreen(
      createTestState(showTtsControls = true),
      onReaderAction = { action = it }
    )
    composeTestRule
      .onNodeWithTag(TTS_CONTROL_STOP_BUTTON_TESTING_TAG)
      .performClick()
    assertEquals(ReaderAction.StopTts, action)
  }

  @Test
  fun readerScreen_backToTopFab_visibleWhenShowIsTrue() {
    renderReaderScreen(createTestState(showBackToTopButton = true))
    composeTestRule
      .onNodeWithContentDescription(context.getString(R.string.pref_back_to_top))
      .assertIsDisplayed()
  }

  @Test
  fun readerScreen_backToTopFab_hiddenWhenShowIsFalse() {
    renderReaderScreen(createTestState(showBackToTopButton = false))
    composeTestRule
      .onNodeWithContentDescription(context.getString(R.string.pref_back_to_top))
      .assertDoesNotExist()
  }

  @Test
  fun readerScreen_backToTopFab_click_triggersCallback() {
    var action: ReaderAction? = null
    renderReaderScreen(
      createTestState(showBackToTopButton = true),
      onReaderAction = { action = it }
    )
    composeTestRule
      .onNodeWithContentDescription(context.getString(R.string.pref_back_to_top))
      .performClick()
    assertEquals(ReaderAction.BackToTopButtonClick, action)
  }

  @Test
  fun readerScreen_noBookOpenView_displaysNoOpenBookText() {
    renderReaderScreen(createTestState(showNoBookOpenInReader = true))
    composeTestRule
      .onNodeWithText(context.getString(R.string.no_open_book))
      .assertIsDisplayed()
  }

  @Test
  fun readerScreen_noBookOpenView_displaysOpenLibraryButton() {
    renderReaderScreen(createTestState(showNoBookOpenInReader = true))
    composeTestRule
      .onNodeWithText(context.getString(R.string.open_library).uppercase())
      .assertIsDisplayed()
  }

  @Test
  fun readerScreen_noBookOpenView_openLibraryClick_triggersCallback() {
    var action: ReaderAction? = null
    renderReaderScreen(
      createTestState(showNoBookOpenInReader = true),
      onReaderAction = { action = it }
    )
    composeTestRule
      .onNodeWithText(context.getString(R.string.open_library).uppercase())
      .performClick()
    assertEquals(ReaderAction.OpenLibrary, action)
  }

  @Test
  fun readerScreen_donationLayout_visibleWhenShouldShow() {
    renderReaderScreen(
      createTestState(
        showDonationPopup = true,
        appName = "Kiwix"
      )
    )
    composeTestRule
      .onNodeWithTag(DONATION_LAYOUT_TESTING_TAG)
      .assertIsDisplayed()
  }

  @Test
  fun readerScreen_donationLayout_hiddenWhenShouldNotShow() {
    renderReaderScreen(createTestState(showDonationPopup = false))
    composeTestRule
      .onNodeWithTag(DONATION_LAYOUT_TESTING_TAG)
      .assertDoesNotExist()
  }

  @Test
  fun readerScreen_donationLayout_donateButton_triggersCallback() {
    var action: ReaderAction? = null
    renderReaderScreen(
      createTestState(showDonationPopup = true),
      onReaderAction = { action = it }
    )
    composeTestRule
      .onNodeWithText(context.getString(R.string.make_donation))
      .performClick()
    assertEquals(ReaderAction.DonateButtonClick, action)
  }

  @Test
  fun readerScreen_donationLayout_laterButton_triggersCallback() {
    var action: ReaderAction? = null
    renderReaderScreen(
      createTestState(showDonationPopup = true),
      onReaderAction = { action = it }
    )
    composeTestRule
      .onNodeWithText(context.getString(R.string.rate_dialog_neutral))
      .performClick()
    assertEquals(ReaderAction.DonateLaterButtonClick, action)
  }

  @Test
  fun readerScreen_tabSwitcher_closeAllTabButton_displaysWhenTabSwitcherShown() {
    renderReaderScreen(createTestState(showTabSwitcher = true))
    composeTestRule.waitForIdle()
    composeTestRule
      .onNodeWithTag(CLOSE_ALL_TABS_BUTTON_TESTING_TAG)
      .assertIsDisplayed()
  }

  @Test
  fun readerScreen_tabSwitcher_closeAllTabButton_triggersCallback() {
    var action: ReaderAction? = null
    renderReaderScreen(
      createTestState(showTabSwitcher = true),
      onReaderAction = { action = it }
    )
    composeTestRule.waitForIdle()
    composeTestRule
      .onNodeWithTag(CLOSE_ALL_TABS_BUTTON_TESTING_TAG)
      .performClick()
    assertEquals(ReaderAction.CloseAllTabs, action)
  }

  @Test
  fun readerScreen_topBar_hiddenInFullScreenMode() {
    renderReaderScreen(
      createTestState(shouldShowFullScreen = true)
    )
    composeTestRule
      .onNodeWithText("Test Reader")
      .assertDoesNotExist()
  }

  @Test
  fun readerScreen_topBar_hiddenWhenTabSwitcherVisible() {
    renderReaderScreen(createTestState(showTabSwitcher = true))
    composeTestRule
      .onNodeWithText("Test Reader")
      .assertDoesNotExist()
  }

  @Test
  fun readerScreen_searchPlaceholder_visibleAndClickable() {
    var action: ReaderAction? = null
    renderReaderScreen(
      createTestState(searchPlaceHolderItemForBrandedApps = true),
      onReaderAction = { action = it }
    )
    composeTestRule
      .onNodeWithText(context.getString(R.string.search_label))
      .assertIsDisplayed()
      .performClick()
    assertEquals(ReaderAction.OpenSearch(), action)
  }

  @Test
  fun readerScreen_findInPageAppBar_visible() {
    renderReaderScreen(
      createTestState(findInPageUiState = FindInPageManager.FindInPageUiState(visible = true)),
    )
    composeTestRule
      .onNodeWithTag(FIND_IN_SEARCH_VIEW_TESTING_TAG)
      .assertIsDisplayed()
  }

  @Test
  fun readerScreen_progressBar_visibleWhenPageLoading() {
    renderReaderScreen(
      createTestState(loading = true, progress = 50)
    )
    composeTestRule
      .onNodeWithTag(CONTENT_LOADING_PROGRESS_BAR_TESTING_TAG)
      .assertIsDisplayed()
  }

  @Test
  fun readerScreen_tableOfContentDrawer_visibleWhenOpen() {
    val sections = mutableListOf(
      DocumentSection("Section 1", "section1", 1),
      DocumentSection("Section 2", "section2", 2)
    )
    renderReaderScreen(
      createTestState(showTableOfContentDrawer = true, documentSections = sections)
    )
    composeTestRule.waitForIdle()
    composeTestRule
      .onNodeWithText("Contents")
      .assertIsDisplayed()
  }

  @Test
  fun readerScreen_tabSwitcher_visibleWhenEnabled() {
    renderReaderScreen(createTestState(showTabSwitcher = true))
    composeTestRule.waitForIdle()
    composeTestRule
      .onNodeWithTag(TAB_SWITCHER_VIEW_TESTING_TAG)
      .assertIsDisplayed()
  }

  @Test
  fun readerScreen_tabSwitcher_hiddenWhenDisabled() {
    renderReaderScreen(createTestState(showTabSwitcher = false))
    composeTestRule.waitForIdle()
    composeTestRule
      .onNodeWithTag(TAB_SWITCHER_VIEW_TESTING_TAG)
      .assertDoesNotExist()
  }

  @Test
  fun readerScreen_bottomAppBar_tocDisabled_doesNotTriggerCallback() {
    var action: ReaderAction? = null
    renderReaderScreen(
      createTestState(
        showBottomBar = true,
        isTocButtonEnable = false,
      ),
      onReaderAction = { action = it }
    )
    composeTestRule
      .onNodeWithTag(READER_BOTTOM_BAR_TABLE_CONTENT_BUTTON_TESTING_TAG)
      .performClick()
    assertEquals(null, action)
  }

  @Test
  fun readerScreen_fullScreenItem_notActive_showsTopBar() {
    renderReaderScreen(createTestState(shouldShowFullScreen = false))
    composeTestRule
      .onNodeWithText("Test Reader")
      .assertIsDisplayed()
  }

  @Test
  fun readerScreen_fullScreenItem_isDisplayed() {
    val videoView = FrameLayout(context).apply {
      contentDescription = "video_view"
    }
    renderReaderScreen(
      createTestState(shouldShowFullScreen = true, videoView = videoView)
    )
    // When fullScreenItem.first is true, the normal content should not be rendered.
    // Verify the no-book view is not shown (fullscreen path is taken instead).
    composeTestRule
      .onNodeWithText(context.getString(R.string.no_open_book))
      .assertDoesNotExist()
    // The top bar should also be hidden in fullscreen mode.
    composeTestRule
      .onNodeWithText("Test Reader")
      .assertDoesNotExist()
  }

  @Test
  fun readerScreen_tabSwitcher_onCloseTab_triggersCallback() {
    var action: ReaderAction? = null
    val webView = mockk<KiwixWebView>(relaxed = true)
    every { webView.parent } returns null
    every { webView.layoutParams } returns FrameLayout.LayoutParams(
      FrameLayout.LayoutParams.MATCH_PARENT,
      FrameLayout.LayoutParams.MATCH_PARENT
    )

    val state = createTestState(
      showTabSwitcher = true,
      tabsState = TabsManager.TabsState(listOf(webView))
    )
    renderReaderScreen(state, onReaderAction = { action = it })
    composeTestRule.waitForIdle()

    composeTestRule
      .onNodeWithContentDescription(
        context.getString(R.string.close_tab) + "0",
        useUnmergedTree = true
      )
      .performClick()
    assertEquals(ReaderAction.CloseTab(0), action)
  }

  @Test
  fun readerScreen_ttsSpeedButton_isDisplayed() {
    val state = createTestState(showTtsControls = true, ttsSpeedText = "1.0X")
    renderReaderScreen(state)
    composeTestRule
      .onNodeWithTag(TTS_CONTROL_SPEED_BUTTON_TESTING_TAG)
      .assertIsDisplayed()
  }

  @Test
  fun ttsSpeedBottomSheet_selectPreset_triggersCallback() {
    var action: ReaderAction? = null
    composeTestRule.setContent {
      TtsSpeedBottomSheetContent(
        currentSpeed = 1.0f,
        onSpeedChanged = { action = ReaderAction.ChangeTtsSpeed(it) }
      )
    }
    composeTestRule.waitForIdle()

    composeTestRule
      .onNodeWithText("1.5")
      .performClick()
    composeTestRule.waitForIdle()

    assertEquals(ReaderAction.ChangeTtsSpeed(TEST_TTS_SPEED), action)
  }

  @Test
  fun ttsSpeedBottomSheet_selectHalfSpeedPreset_triggersCallback() {
    var action: ReaderAction? = null
    composeTestRule.setContent {
      TtsSpeedBottomSheetContent(
        currentSpeed = 1.0f,
        onSpeedChanged = { action = ReaderAction.ChangeTtsSpeed(it) }
      )
    }
    composeTestRule.waitForIdle()

    composeTestRule
      .onNodeWithText("0.5")
      .performClick()
    composeTestRule.waitForIdle()

    assertEquals(ReaderAction.ChangeTtsSpeed(0.5f), action)
  }

  @Test
  fun ttsSpeedBottomSheet_selectQuarterSpeedPreset_triggersCallback() {
    var action: ReaderAction? = null
    composeTestRule.setContent {
      TtsSpeedBottomSheetContent(
        currentSpeed = 1.0f,
        onSpeedChanged = { action = ReaderAction.ChangeTtsSpeed(it) }
      )
    }
    composeTestRule.waitForIdle()

    composeTestRule
      .onNodeWithText("0.75")
      .performClick()
    composeTestRule.waitForIdle()

    assertEquals(ReaderAction.ChangeTtsSpeed(0.75f), action)
  }

  @Test
  fun ttsSpeedBottomSheet_selectOneAndQuarterPreset_triggersCallback() {
    var action: ReaderAction? = null
    composeTestRule.setContent {
      TtsSpeedBottomSheetContent(
        currentSpeed = 1.0f,
        onSpeedChanged = { action = ReaderAction.ChangeTtsSpeed(it) }
      )
    }
    composeTestRule.waitForIdle()

    composeTestRule
      .onNodeWithText("1.25")
      .performClick()
    composeTestRule.waitForIdle()

    assertEquals(ReaderAction.ChangeTtsSpeed(1.25f), action)
  }

  @Test
  fun ttsSpeedBottomSheet_selectDoubleSpeedPreset_triggersCallback() {
    var action: ReaderAction? = null
    composeTestRule.setContent {
      TtsSpeedBottomSheetContent(
        currentSpeed = 1.0f,
        onSpeedChanged = { action = ReaderAction.ChangeTtsSpeed(it) }
      )
    }
    composeTestRule.waitForIdle()

    composeTestRule
      .onNodeWithText("2.0")
      .performScrollTo()
      .performClick()
    composeTestRule.waitForIdle()

    assertEquals(ReaderAction.ChangeTtsSpeed(2.0f), action)
  }

  @Test
  fun ttsSpeedBottomSheet_selectMaxSpeedPreset_triggersCallback() {
    var action: ReaderAction? = null
    composeTestRule.setContent {
      TtsSpeedBottomSheetContent(
        currentSpeed = 1.0f,
        onSpeedChanged = { action = ReaderAction.ChangeTtsSpeed(it) }
      )
    }
    composeTestRule.waitForIdle()

    composeTestRule
      .onNodeWithText("2.5")
      .performScrollTo()
      .performClick()
    composeTestRule.waitForIdle()

    assertEquals(ReaderAction.ChangeTtsSpeed(2.5f), action)
  }

  @Test
  fun ttsSpeedBottomSheet_decrementButton_triggersSpeedChange() {
    var action: ReaderAction? = null
    composeTestRule.setContent {
      TtsSpeedBottomSheetContent(
        currentSpeed = 1.0f,
        onSpeedChanged = { action = ReaderAction.ChangeTtsSpeed(it) }
      )
    }
    composeTestRule.waitForIdle()

    composeTestRule
      .onNodeWithTag(TTS_SPEED_DECREMENT_BUTTON_TESTING_TAG)
      .performClick()
    composeTestRule.waitForIdle()

    assertEquals(ReaderAction.ChangeTtsSpeed(0.95f), action)
  }

  @Test
  fun ttsSpeedBottomSheet_incrementButton_triggersSpeedChange() {
    var action: ReaderAction? = null
    composeTestRule.setContent {
      TtsSpeedBottomSheetContent(
        currentSpeed = 1.0f,
        onSpeedChanged = { action = ReaderAction.ChangeTtsSpeed(it) }
      )
    }
    composeTestRule.waitForIdle()

    composeTestRule
      .onNodeWithTag(TTS_SPEED_INCREMENT_BUTTON_TESTING_TAG)
      .performClick()
    composeTestRule.waitForIdle()

    assertEquals(ReaderAction.ChangeTtsSpeed(1.05f), action)
  }

  @Test
  fun ttsSpeedBottomSheet_decrementAtMinSpeed_clampedToMinimum() {
    var lastSpeed: Float? = null
    composeTestRule.setContent {
      TtsSpeedBottomSheetContent(
        currentSpeed = 0.5f,
        onSpeedChanged = { lastSpeed = it }
      )
    }
    composeTestRule.waitForIdle()

    composeTestRule
      .onNodeWithTag(TTS_SPEED_DECREMENT_BUTTON_TESTING_TAG)
      .performClick()
    composeTestRule.waitForIdle()

    assertEquals(0.5f, lastSpeed)
  }

  @Test
  fun ttsSpeedBottomSheet_incrementAtMaxSpeed_clampedToMaximum() {
    var lastSpeed: Float? = null
    composeTestRule.setContent {
      TtsSpeedBottomSheetContent(
        currentSpeed = 2.5f,
        onSpeedChanged = { lastSpeed = it }
      )
    }
    composeTestRule.waitForIdle()

    composeTestRule
      .onNodeWithTag(TTS_SPEED_INCREMENT_BUTTON_TESTING_TAG)
      .performClick()
    composeTestRule.waitForIdle()

    assertEquals(2.5f, lastSpeed)
  }

  @Test
  fun ttsSpeedBottomSheet_headerDisplaysFormattedSpeed() {
    composeTestRule.setContent {
      TtsSpeedBottomSheetContent(
        currentSpeed = 1.25f,
        onSpeedChanged = {}
      )
    }
    composeTestRule.waitForIdle()

    composeTestRule
      .onNodeWithText("1.25x")
      .assertIsDisplayed()
  }

  @Test
  fun ttsSpeedBottomSheet_sliderIsDisplayed() {
    composeTestRule.setContent {
      TtsSpeedBottomSheetContent(
        currentSpeed = 1.0f,
        onSpeedChanged = {}
      )
    }
    composeTestRule.waitForIdle()

    composeTestRule
      .onNodeWithTag(TTS_SPEED_SLIDER_TESTING_TAG)
      .assertIsDisplayed()
  }

  @Test
  fun ttsSpeedBottomSheet_normalLabelShownUnderDefaultPreset() {
    composeTestRule.setContent {
      TtsSpeedBottomSheetContent(
        currentSpeed = 1.0f,
        onSpeedChanged = {}
      )
    }
    composeTestRule.waitForIdle()

    composeTestRule
      .onNodeWithText("Normal")
      .assertIsDisplayed()
  }

  @Test
  fun ttsSpeedBottomSheet_allSevenPresetsDisplayed() {
    composeTestRule.setContent {
      TtsSpeedBottomSheetContent(
        currentSpeed = 1.0f,
        onSpeedChanged = {}
      )
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("0.5").assertExists()
    composeTestRule.onNodeWithText("0.75").assertExists()
    composeTestRule.onNodeWithText("1.0").assertExists()
    composeTestRule.onNodeWithText("1.25").assertExists()
    composeTestRule.onNodeWithText("1.5").assertExists()
    composeTestRule.onNodeWithText("2.0").assertExists()
    composeTestRule.onNodeWithText("2.5").assertExists()
  }

  @Test
  fun readerScreen_ttsSpeedButton_displaysFormattedSpeed() {
    val state = createTestState(
      showTtsControls = true,
      ttsSpeedText = "1.50X"
    )
    renderReaderScreen(state)
    composeTestRule.waitForIdle()

    composeTestRule
      .onNodeWithText("1.50x")
      .assertIsDisplayed()
  }

  @Test
  fun ttsSpeedBottomSheet_decrementAndIncrementButtons_displayed() {
    composeTestRule.setContent {
      TtsSpeedBottomSheetContent(
        currentSpeed = 1.0f,
        onSpeedChanged = {}
      )
    }
    composeTestRule.waitForIdle()

    composeTestRule
      .onNodeWithTag(TTS_SPEED_DECREMENT_BUTTON_TESTING_TAG)
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithTag(TTS_SPEED_INCREMENT_BUTTON_TESTING_TAG)
      .assertIsDisplayed()
  }

  @Test
  fun ttsSpeedBottomSheet_selectNormalPreset_triggersCallback() {
    var action: ReaderAction? = null
    composeTestRule.setContent {
      TtsSpeedBottomSheetContent(
        currentSpeed = 1.5f,
        onSpeedChanged = { action = ReaderAction.ChangeTtsSpeed(it) }
      )
    }
    composeTestRule.waitForIdle()

    composeTestRule
      .onNodeWithText("1.0")
      .performClick()
    composeTestRule.waitForIdle()

    assertEquals(ReaderAction.ChangeTtsSpeed(1.0f), action)
  }
}
