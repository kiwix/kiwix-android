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
import io.mockk.every
import io.mockk.mockk
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.navigation.compose.rememberNavController
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.FragmentActivityExtensions
import org.kiwix.kiwixmobile.core.main.KiwixWebView
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Behavior-driven UI tests for ReaderScreen.
 *
 * All tests render through the top-level [ReaderScreen] composable,
 * using [ReaderScreenState] to drive different UI states.
 * This ensures we test real user-visible behavior, not internal
 * composable implementation details.
 */
@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])
class ReaderScreenComposablesTest {
  @get:Rule
  val composeTestRule = createComposeRule()

  private val context get() = RuntimeEnvironment.getApplication()

  /**
   * Creates a minimal [ReaderScreenState] with sensible defaults for testing.
   * Test-specific values can be overridden via named parameters.
   */
  private fun createTestState(
    isNoBookOpenInReader: Boolean = false,
    showBackToTopButton: Boolean = false,
    showTtsControls: Boolean = false,
    pauseTtsButtonText: String = "Pause",
    shouldShowDonationPopup: Boolean = false,
    shouldShowBottomAppBar: Boolean = true,
    showTabSwitcher: Boolean = false,
    onOpenLibraryButtonClicked: () -> Unit = {},
    backToTopButtonClick: () -> Unit = {},
    onPauseTtsClick: () -> Unit = {},
    onStopTtsClick: () -> Unit = {},
    onHomeButtonClick: () -> Unit = {},
    onCloseAllTabs: () -> Unit = {},
    donateButtonClick: () -> Unit = {},
    laterButtonClick: () -> Unit = {},
    bookmarkOnClick: () -> Unit = {},
    bookmarkOnLongClick: () -> Unit = {},
    previousPageOnClick: () -> Unit = {},
    previousPageOnLongClick: () -> Unit = {},
    previousPageEnabled: Boolean = true,
    nextPageOnClick: () -> Unit = {},
    nextPageOnLongClick: () -> Unit = {},
    nextPageEnabled: Boolean = true,
    tocEnabled: Boolean = true,
    tocOnClick: () -> Unit = {},
    appName: String = "Kiwix",
    fullScreenItem: Pair<Boolean, FrameLayout?> = Pair(false, null)
  ): ReaderScreenState = ReaderScreenState(
    snackBarHostState = SnackbarHostState(),
    isNoBookOpenInReader = isNoBookOpenInReader,
    onOpenLibraryButtonClicked = onOpenLibraryButtonClicked,
    pageLoadingItem = Pair(false, 0),
    shouldShowDonationPopup = shouldShowDonationPopup,
    fullScreenItem = fullScreenItem,
    showBackToTopButton = showBackToTopButton,
    backToTopButtonClick = backToTopButtonClick,
    showTtsControls = showTtsControls,
    onPauseTtsClick = onPauseTtsClick,
    pauseTtsButtonText = pauseTtsButtonText,
    onStopTtsClick = onStopTtsClick,
    currentWebViewPosition = 0,
    kiwixWebViewList = emptyList(),
    showTabSwitcher = showTabSwitcher,
    selectedWebView = null,
    bookmarkButtonItem = Triple(
      bookmarkOnClick,
      bookmarkOnLongClick,
      IconItem.Drawable(R.drawable.ic_bookmark_border_24dp)
    ),
    previousPageButtonItem = Triple(
      previousPageOnClick,
      previousPageOnLongClick,
      previousPageEnabled
    ),
    onHomeButtonClick = onHomeButtonClick,
    nextPageButtonItem = Triple(nextPageOnClick, nextPageOnLongClick, nextPageEnabled),
    tocButtonItem = Pair(tocEnabled, tocOnClick),
    onCloseAllTabs = onCloseAllTabs,
    shouldShowBottomAppBar = shouldShowBottomAppBar,
    readerScreenTitle = "Test Reader",
    onTabClickListener = object : TabClickListener {
      override fun onSelectTab(position: Int) { /* no-op */ }
      override fun onCloseTab(position: Int) { /* no-op */ }
    },
    searchPlaceHolderItemForCustomApps = Pair(false) {},
    appName = appName,
    donateButtonClick = donateButtonClick,
    laterButtonClick = laterButtonClick,
    tableOfContentTitle = "Contents"
  )

  /**
   * Renders the full [ReaderScreen] with the given state,
   * providing minimal test doubles for required dependencies.
   */
  private fun renderReaderScreen(
    state: ReaderScreenState,
    showTocDrawer: MutableState<Boolean> = mutableStateOf(false),
    documentSections: MutableList<DocumentSection>? = null
  ) {
    composeTestRule.setContent {
      val navController = rememberNavController()
      ReaderScreen(
        state = state,
        actionMenuItems = emptyList(),
        showTableOfContentDrawer = showTocDrawer,
        documentSections = documentSections,
        onUserBackPressed = { FragmentActivityExtensions.Super.ShouldCall },
        navHostController = navController,
        mainActivityBottomAppBarScrollBehaviour = null,
        navigationIcon = {}
      )
    }
  }

  @Test
  fun readerScreen_bottomAppBar_displaysAllButtons() {
    renderReaderScreen(createTestState(shouldShowBottomAppBar = true))
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
    var clicked = false
    renderReaderScreen(
      createTestState(
        shouldShowBottomAppBar = true,
        bookmarkOnClick = { clicked = true }
      )
    )
    composeTestRule
      .onNodeWithTag(READER_BOTTOM_BAR_BOOKMARK_BUTTON_TESTING_TAG)
      .performClick()
    assertTrue("Bookmark onClick callback should be triggered", clicked)
  }

  @Test
  fun readerScreen_bottomAppBar_bookmarkLongClick_triggersCallback() {
    var longClicked = false
    renderReaderScreen(
      createTestState(
        shouldShowBottomAppBar = true,
        bookmarkOnLongClick = { longClicked = true }
      )
    )
    composeTestRule
      .onNodeWithTag(READER_BOTTOM_BAR_BOOKMARK_BUTTON_TESTING_TAG)
      .performTouchInput { longClick() }
    assertTrue("Bookmark onLongClick callback should be triggered", longClicked)
  }

  @Test
  fun readerScreen_bottomAppBar_homeClick_triggersCallback() {
    var clicked = false
    renderReaderScreen(
      createTestState(
        shouldShowBottomAppBar = true,
        onHomeButtonClick = { clicked = true }
      )
    )
    composeTestRule
      .onNodeWithTag(READER_BOTTOM_BAR_HOME_BUTTON_TESTING_TAG)
      .performClick()
    assertTrue("Home onClick callback should be triggered", clicked)
  }

  @Test
  fun readerScreen_bottomAppBar_previousPageClick_triggersCallback() {
    var clicked = false
    renderReaderScreen(
      createTestState(
        shouldShowBottomAppBar = true,
        previousPageOnClick = { clicked = true }
      )
    )
    composeTestRule
      .onNodeWithTag(READER_BOTTOM_BAR_PREVIOUS_SCREEN_BUTTON_TESTING_TAG)
      .performClick()
    assertTrue("Previous page onClick callback should be triggered", clicked)
  }

  @Test
  fun readerScreen_bottomAppBar_nextPageClick_triggersCallback() {
    var clicked = false
    renderReaderScreen(
      createTestState(
        shouldShowBottomAppBar = true,
        nextPageOnClick = { clicked = true }
      )
    )
    composeTestRule
      .onNodeWithTag(READER_BOTTOM_BAR_NEXT_SCREEN_BUTTON_TESTING_TAG)
      .performClick()
    assertTrue("Next page onClick callback should be triggered", clicked)
  }

  @Test
  fun readerScreen_bottomAppBar_disabledButton_doesNotTriggerCallback() {
    var clicked = false
    renderReaderScreen(
      createTestState(
        shouldShowBottomAppBar = true,
        previousPageOnClick = { clicked = true },
        previousPageEnabled = false
      )
    )
    composeTestRule
      .onNodeWithTag(READER_BOTTOM_BAR_PREVIOUS_SCREEN_BUTTON_TESTING_TAG)
      .performClick()
    assertTrue("Disabled button should not trigger callback", !clicked)
  }

  @Test
  fun readerScreen_bottomAppBar_notShownWhenShouldShowIsFalse() {
    renderReaderScreen(createTestState(shouldShowBottomAppBar = false))
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
      .onNodeWithText("PAUSE")
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithTag(TTS_CONTROL_STOP_BUTTON_TESTING_TAG)
      .assertIsDisplayed()
  }

  @Test
  fun readerScreen_ttsControls_hiddenWhenInactive() {
    renderReaderScreen(createTestState(showTtsControls = false))
    composeTestRule
      .onNodeWithText("PAUSE")
      .assertDoesNotExist()
    composeTestRule
      .onNodeWithTag(TTS_CONTROL_STOP_BUTTON_TESTING_TAG)
      .assertDoesNotExist()
  }

  @Test
  fun readerScreen_ttsControls_pauseButton_triggersCallback() {
    var pauseClicked = false
    renderReaderScreen(
      createTestState(
        showTtsControls = true,
        pauseTtsButtonText = "Pause",
        onPauseTtsClick = { pauseClicked = true }
      )
    )
    composeTestRule
      .onNodeWithText("PAUSE")
      .performClick()
    assertTrue("Pause TTS callback should be triggered", pauseClicked)
  }

  @Test
  fun readerScreen_ttsControls_stopButton_triggersCallback() {
    var stopClicked = false
    renderReaderScreen(
      createTestState(
        showTtsControls = true,
        onStopTtsClick = { stopClicked = true }
      )
    )
    composeTestRule
      .onNodeWithTag(TTS_CONTROL_STOP_BUTTON_TESTING_TAG)
      .performClick()
    assertTrue("Stop TTS callback should be triggered", stopClicked)
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
    var clicked = false
    renderReaderScreen(
      createTestState(
        showBackToTopButton = true,
        backToTopButtonClick = { clicked = true }
      )
    )
    composeTestRule
      .onNodeWithContentDescription(context.getString(R.string.pref_back_to_top))
      .performClick()
    assertTrue("BackToTop click callback should be triggered", clicked)
  }

  @Test
  fun readerScreen_noBookOpenView_displaysNoOpenBookText() {
    renderReaderScreen(createTestState(isNoBookOpenInReader = true))
    composeTestRule
      .onNodeWithText(context.getString(R.string.no_open_book))
      .assertIsDisplayed()
  }

  @Test
  fun readerScreen_noBookOpenView_displaysOpenLibraryButton() {
    renderReaderScreen(createTestState(isNoBookOpenInReader = true))
    composeTestRule
      .onNodeWithText(context.getString(R.string.open_library).uppercase())
      .assertIsDisplayed()
  }

  @Test
  fun readerScreen_noBookOpenView_openLibraryClick_triggersCallback() {
    var clicked = false
    renderReaderScreen(
      createTestState(
        isNoBookOpenInReader = true,
        onOpenLibraryButtonClicked = { clicked = true }
      )
    )
    composeTestRule
      .onNodeWithText(context.getString(R.string.open_library).uppercase())
      .performClick()
    assertTrue("Open Library callback should be triggered", clicked)
  }

  @Test
  fun readerScreen_donationLayout_visibleWhenShouldShow() {
    renderReaderScreen(
      createTestState(
        shouldShowDonationPopup = true,
        appName = "Kiwix"
      )
    )
    composeTestRule
      .onNodeWithTag(DONATION_LAYOUT_TESTING_TAG)
      .assertIsDisplayed()
  }

  @Test
  fun readerScreen_donationLayout_hiddenWhenShouldNotShow() {
    renderReaderScreen(createTestState(shouldShowDonationPopup = false))
    composeTestRule
      .onNodeWithTag(DONATION_LAYOUT_TESTING_TAG)
      .assertDoesNotExist()
  }

  @Test
  fun readerScreen_donationLayout_donateButton_triggersCallback() {
    var clicked = false
    renderReaderScreen(
      createTestState(
        shouldShowDonationPopup = true,
        donateButtonClick = { clicked = true }
      )
    )
    composeTestRule
      .onNodeWithText(context.getString(R.string.make_donation))
      .performClick()
    assertTrue("Donate callback should be triggered", clicked)
  }

  @Test
  fun readerScreen_donationLayout_laterButton_triggersCallback() {
    var clicked = false
    renderReaderScreen(
      createTestState(
        shouldShowDonationPopup = true,
        laterButtonClick = { clicked = true }
      )
    )
    composeTestRule
      .onNodeWithText(context.getString(R.string.rate_dialog_neutral))
      .performClick()
    assertTrue("Later callback should be triggered", clicked)
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
    var clicked = false
    renderReaderScreen(
      createTestState(
        showTabSwitcher = true,
        onCloseAllTabs = { clicked = true }
      )
    )
    composeTestRule.waitForIdle()
    composeTestRule
      .onNodeWithTag(CLOSE_ALL_TABS_BUTTON_TESTING_TAG)
      .performClick()
    assertTrue("onCloseAllTabs callback should be triggered", clicked)
  }

  @Test
  fun readerScreen_topBar_hiddenInFullScreenMode() {
    renderReaderScreen(
      createTestState(fullScreenItem = Pair(true, null))
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
    var clicked = false
    renderReaderScreen(
      createTestState().copy(
        searchPlaceHolderItemForCustomApps = Pair(true) {
          clicked = true
        }
      )
    )
    composeTestRule
      .onNodeWithText(context.getString(R.string.search_label))
      .assertIsDisplayed()
      .performClick()
    assertTrue(clicked)
  }

  @Test
  fun readerScreen_progressBar_visibleWhenPageLoading() {
    renderReaderScreen(
      createTestState().copy(
        pageLoadingItem = Pair(true, 50)
      )
    )
    composeTestRule
      .onNodeWithTag(CONTENT_LOADING_PROGRESSBAR_TESTING_TAG)
      .assertIsDisplayed()
  }

  @Test
  fun readerScreen_progressBar_hiddenWhenNotLoading() {
    renderReaderScreen(
      createTestState().copy(
        pageLoadingItem = Pair(false, 0)
      )
    )
    composeTestRule
      .onNodeWithTag(CONTENT_LOADING_PROGRESSBAR_TESTING_TAG)
      .assertDoesNotExist()
  }

  @Test
  fun readerScreen_tableOfContentDrawer_visibleWhenOpen() {
    val showTocDrawer = mutableStateOf(true)
    val sections = mutableListOf(
      DocumentSection("Section 1", "section1", 1),
      DocumentSection("Section 2", "section2", 2)
    )
    renderReaderScreen(
      createTestState(),
      showTocDrawer = showTocDrawer,
      documentSections = sections
    )
    composeTestRule.waitForIdle()
    composeTestRule
      .onNodeWithText("Contents")
      .assertIsDisplayed()
  }

  @Test
  fun readerScreen_tableOfContentDrawer_dismissedOnOverlayClick() {
    val showTocDrawer = mutableStateOf(true)
    renderReaderScreen(
      createTestState(),
      showTocDrawer = showTocDrawer
    )
    composeTestRule.waitForIdle()
    composeTestRule
      .onNodeWithContentDescription(context.getString(android.R.string.untitled))
      .performClick()
    composeTestRule.waitForIdle()
    assertTrue("Table of content drawer should be dismissed", !showTocDrawer.value)
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
    var clicked = false
    renderReaderScreen(
      createTestState(
        shouldShowBottomAppBar = true,
        tocEnabled = false,
        tocOnClick = { clicked = true }
      )
    )
    composeTestRule
      .onNodeWithTag(READER_BOTTOM_BAR_TABLE_CONTENT_BUTTON_TESTING_TAG)
      .performClick()
    assertTrue(!clicked)
  }

  @Test
  fun readerScreen_fullScreenItem_notActive_showsTopBar() {
    renderReaderScreen(createTestState(fullScreenItem = Pair(false, null)))
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
      createTestState(fullScreenItem = Pair(true, videoView))
    )
    composeTestRule
      .onNodeWithContentDescription("video_view")
      .assertIsDisplayed()
  }

  @Test
  fun readerScreen_tabSwitcher_onSelectTab_triggersCallback() {
    var selectedIndex = -1
    val webView = mockk<KiwixWebView>(relaxed = true)
    every { webView.contentDescription } returns "tab_webview"

    val state = createTestState(
      showTabSwitcher = true
    ).copy(
      kiwixWebViewList = listOf(webView),
      onTabClickListener = object : TabClickListener {
        override fun onSelectTab(position: Int) {
          selectedIndex = position
        }
        override fun onCloseTab(position: Int) { /* no-op */ }
      }
    )
    renderReaderScreen(state)
    composeTestRule.waitForIdle()

    // The contentDescription is composed as "${webView.contentDescription}${webView.hashCode()}"
    composeTestRule
      .onNodeWithContentDescription("tab_webview${webView.hashCode()}", substring = true)
      .performClick()

    assertTrue("onSelectTab callback should be triggered with index 0", selectedIndex == 0)
  }

  @Test
  fun readerScreen_tabSwitcher_onCloseTab_triggersCallback() {
    var closedIndex = -1
    val webView = mockk<KiwixWebView>(relaxed = true)

    val state = createTestState(
      showTabSwitcher = true
    ).copy(
      kiwixWebViewList = listOf(webView),
      onTabClickListener = object : TabClickListener {
        override fun onSelectTab(position: Int) { /* no-op */ }
        override fun onCloseTab(position: Int) {
          closedIndex = position
        }
      }
    )
    renderReaderScreen(state)
    composeTestRule.waitForIdle()

    composeTestRule
      .onNodeWithContentDescription(context.getString(R.string.close_tab) + "0")
      .performClick()

    assertTrue("onCloseTab callback should be triggered with index 0", closedIndex == 0)
  }
}
