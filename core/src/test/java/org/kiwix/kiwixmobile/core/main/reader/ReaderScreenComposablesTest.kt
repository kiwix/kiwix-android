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

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.ui.theme.KiwixTheme
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import android.os.Build

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
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
    appName: String = "Kiwix"
  ): ReaderScreenState = ReaderScreenState(
    snackBarHostState = SnackbarHostState(),
    isNoBookOpenInReader = isNoBookOpenInReader,
    onOpenLibraryButtonClicked = onOpenLibraryButtonClicked,
    pageLoadingItem = Pair(false, 0),
    shouldShowDonationPopup = shouldShowDonationPopup,
    fullScreenItem = Pair(false, null),
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
      override fun onSelectTab(position: Int) {}
      override fun onCloseTab(position: Int) {}
    },
    searchPlaceHolderItemForCustomApps = Pair(false) {},
    appName = appName,
    donateButtonClick = donateButtonClick,
    laterButtonClick = laterButtonClick,
    tableOfContentTitle = "Contents"
  )

  @Test
  fun closeAllTabButton_displaysCloseIcon() {
    composeTestRule.setContent {
      KiwixTheme {
        Box {
          CloseAllTabButton(onCloseAllTabs = {})
        }
      }
    }
    composeTestRule
      .onNodeWithTag(CLOSE_ALL_TABS_BUTTON_TESTING_TAG)
      .assertIsDisplayed()
  }

  @Test
  fun closeAllTabButton_click_triggersCallback() {
    var clicked = false
    composeTestRule.setContent {
      KiwixTheme {
        Box {
          CloseAllTabButton(onCloseAllTabs = { clicked = true })
        }
      }
    }
    composeTestRule
      .onNodeWithTag(CLOSE_ALL_TABS_BUTTON_TESTING_TAG)
      .performClick()
    assertTrue("onCloseAllTabs callback should be triggered", clicked)
  }

  @OptIn(ExperimentalMaterial3Api::class)
  @Test
  fun bottomAppBar_displaysAllButtons() {
    composeTestRule.setContent {
      KiwixTheme {
        BottomAppBarOfReaderScreen(
          bookmarkButtonItem = Triple({}, {}, IconItem.Drawable(R.drawable.ic_bookmark_border_24dp)),
          previousPageButtonItem = Triple({}, {}, true),
          onHomeButtonClick = {},
          nextPageButtonItem = Triple({}, {}, true),
          tocButtonItem = Pair(true) {},
          shouldShowBottomAppBar = true,
          bottomAppBarScrollBehavior = BottomAppBarDefaults.exitAlwaysScrollBehavior()
        )
      }
    }
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

  @OptIn(ExperimentalMaterial3Api::class)
  @Test
  fun bottomAppBar_bookmarkClick_triggersCallback() {
    var clicked = false
    composeTestRule.setContent {
      KiwixTheme {
        BottomAppBarOfReaderScreen(
          bookmarkButtonItem = Triple({ clicked = true }, {}, IconItem.Drawable(R.drawable.ic_bookmark_border_24dp)),
          previousPageButtonItem = Triple({}, {}, true),
          onHomeButtonClick = {},
          nextPageButtonItem = Triple({}, {}, true),
          tocButtonItem = Pair(true) {},
          shouldShowBottomAppBar = true,
          bottomAppBarScrollBehavior = BottomAppBarDefaults.exitAlwaysScrollBehavior()
        )
      }
    }
    composeTestRule
      .onNodeWithTag(READER_BOTTOM_BAR_BOOKMARK_BUTTON_TESTING_TAG)
      .performClick()
    assertTrue("Bookmark onClick callback should be triggered", clicked)
  }

  @OptIn(ExperimentalMaterial3Api::class)
  @Test
  fun bottomAppBar_bookmarkLongClick_triggersCallback() {
    var longClicked = false
    composeTestRule.setContent {
      KiwixTheme {
        BottomAppBarOfReaderScreen(
          bookmarkButtonItem = Triple({}, { longClicked = true }, IconItem.Drawable(R.drawable.ic_bookmark_border_24dp)),
          previousPageButtonItem = Triple({}, {}, true),
          onHomeButtonClick = {},
          nextPageButtonItem = Triple({}, {}, true),
          tocButtonItem = Pair(true) {},
          shouldShowBottomAppBar = true,
          bottomAppBarScrollBehavior = BottomAppBarDefaults.exitAlwaysScrollBehavior()
        )
      }
    }
    composeTestRule
      .onNodeWithTag(READER_BOTTOM_BAR_BOOKMARK_BUTTON_TESTING_TAG)
      .performTouchInput { longClick() }
    assertTrue("Bookmark onLongClick callback should be triggered", longClicked)
  }

  @OptIn(ExperimentalMaterial3Api::class)
  @Test
  fun bottomAppBar_homeClick_triggersCallback() {
    var clicked = false
    composeTestRule.setContent {
      KiwixTheme {
        BottomAppBarOfReaderScreen(
          bookmarkButtonItem = Triple({}, {}, IconItem.Drawable(R.drawable.ic_bookmark_border_24dp)),
          previousPageButtonItem = Triple({}, {}, true),
          onHomeButtonClick = { clicked = true },
          nextPageButtonItem = Triple({}, {}, true),
          tocButtonItem = Pair(true) {},
          shouldShowBottomAppBar = true,
          bottomAppBarScrollBehavior = BottomAppBarDefaults.exitAlwaysScrollBehavior()
        )
      }
    }
    composeTestRule
      .onNodeWithTag(READER_BOTTOM_BAR_HOME_BUTTON_TESTING_TAG)
      .performClick()
    assertTrue("Home onClick callback should be triggered", clicked)
  }

  @OptIn(ExperimentalMaterial3Api::class)
  @Test
  fun bottomAppBar_previousPageClick_triggersCallback() {
    var clicked = false
    composeTestRule.setContent {
      KiwixTheme {
        BottomAppBarOfReaderScreen(
          bookmarkButtonItem = Triple({}, {}, IconItem.Drawable(R.drawable.ic_bookmark_border_24dp)),
          previousPageButtonItem = Triple({ clicked = true }, {}, true),
          onHomeButtonClick = {},
          nextPageButtonItem = Triple({}, {}, true),
          tocButtonItem = Pair(true) {},
          shouldShowBottomAppBar = true,
          bottomAppBarScrollBehavior = BottomAppBarDefaults.exitAlwaysScrollBehavior()
        )
      }
    }
    composeTestRule
      .onNodeWithTag(READER_BOTTOM_BAR_PREVIOUS_SCREEN_BUTTON_TESTING_TAG)
      .performClick()
    assertTrue("Previous page onClick callback should be triggered", clicked)
  }

  @OptIn(ExperimentalMaterial3Api::class)
  @Test
  fun bottomAppBar_nextPageClick_triggersCallback() {
    var clicked = false
    composeTestRule.setContent {
      KiwixTheme {
        BottomAppBarOfReaderScreen(
          bookmarkButtonItem = Triple({}, {}, IconItem.Drawable(R.drawable.ic_bookmark_border_24dp)),
          previousPageButtonItem = Triple({}, {}, true),
          onHomeButtonClick = {},
          nextPageButtonItem = Triple({ clicked = true }, {}, true),
          tocButtonItem = Pair(true) {},
          shouldShowBottomAppBar = true,
          bottomAppBarScrollBehavior = BottomAppBarDefaults.exitAlwaysScrollBehavior()
        )
      }
    }
    composeTestRule
      .onNodeWithTag(READER_BOTTOM_BAR_NEXT_SCREEN_BUTTON_TESTING_TAG)
      .performClick()
    assertTrue("Next page onClick callback should be triggered", clicked)
  }

  @OptIn(ExperimentalMaterial3Api::class)
  @Test
  fun bottomAppBar_disabledButton_doesNotTriggerCallback() {
    var clicked = false
    composeTestRule.setContent {
      KiwixTheme {
        BottomAppBarOfReaderScreen(
          bookmarkButtonItem = Triple({}, {}, IconItem.Drawable(R.drawable.ic_bookmark_border_24dp)),
          previousPageButtonItem = Triple({ clicked = true }, {}, false),
          onHomeButtonClick = {},
          nextPageButtonItem = Triple({}, {}, true),
          tocButtonItem = Pair(true) {},
          shouldShowBottomAppBar = true,
          bottomAppBarScrollBehavior = BottomAppBarDefaults.exitAlwaysScrollBehavior()
        )
      }
    }
    composeTestRule
      .onNodeWithTag(READER_BOTTOM_BAR_PREVIOUS_SCREEN_BUTTON_TESTING_TAG)
      .performClick()
    assertTrue("Disabled button should not trigger callback", !clicked)
  }

  @OptIn(ExperimentalMaterial3Api::class)
  @Test
  fun bottomAppBar_notShownWhenShouldShowIsFalse() {
    composeTestRule.setContent {
      KiwixTheme {
        BottomAppBarOfReaderScreen(
          bookmarkButtonItem = Triple({}, {}, IconItem.Drawable(R.drawable.ic_bookmark_border_24dp)),
          previousPageButtonItem = Triple({}, {}, true),
          onHomeButtonClick = {},
          nextPageButtonItem = Triple({}, {}, true),
          tocButtonItem = Pair(true) {},
          shouldShowBottomAppBar = false,
          bottomAppBarScrollBehavior = BottomAppBarDefaults.exitAlwaysScrollBehavior()
        )
      }
    }
    composeTestRule
      .onNodeWithTag(READER_BOTTOM_BAR_BOOKMARK_BUTTON_TESTING_TAG)
      .assertDoesNotExist()
  }

  @Test
  fun ttsControls_visibleWhenActive() {
    val state = createTestState(
      showTtsControls = true,
      pauseTtsButtonText = "Pause"
    )
    composeTestRule.setContent {
      KiwixTheme {
        TtsControls(state)
      }
    }
    composeTestRule
      .onNodeWithText("PAUSE")
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithTag(TTS_CONTROL_STOP_BUTTON_TESTING_TAG)
      .assertIsDisplayed()
  }

  @Test
  fun ttsControls_hiddenWhenInactive() {
    val state = createTestState(showTtsControls = false)
    composeTestRule.setContent {
      KiwixTheme {
        TtsControls(state)
      }
    }
    composeTestRule
      .onNodeWithText("PAUSE")
      .assertDoesNotExist()
    composeTestRule
      .onNodeWithTag(TTS_CONTROL_STOP_BUTTON_TESTING_TAG)
      .assertDoesNotExist()
  }

  @Test
  fun ttsControls_pauseButton_triggersCallback() {
    var pauseClicked = false
    val state = createTestState(
      showTtsControls = true,
      pauseTtsButtonText = "Pause",
      onPauseTtsClick = { pauseClicked = true }
    )
    composeTestRule.setContent {
      KiwixTheme {
        TtsControls(state)
      }
    }
    composeTestRule
      .onNodeWithText("PAUSE")
      .performClick()
    assertTrue("Pause TTS callback should be triggered", pauseClicked)
  }

  @Test
  fun ttsControls_stopButton_triggersCallback() {
    var stopClicked = false
    val state = createTestState(
      showTtsControls = true,
      onStopTtsClick = { stopClicked = true }
    )
    composeTestRule.setContent {
      KiwixTheme {
        TtsControls(state)
      }
    }
    composeTestRule
      .onNodeWithTag(TTS_CONTROL_STOP_BUTTON_TESTING_TAG)
      .performClick()
    assertTrue("Stop TTS callback should be triggered", stopClicked)
  }

  @Test
  fun backToTopFab_visibleWhenShowIsTrue() {
    val state = createTestState(showBackToTopButton = true)
    composeTestRule.setContent {
      KiwixTheme {
        BackToTopFab(state)
      }
    }
    composeTestRule
      .onNodeWithContentDescription(context.getString(R.string.pref_back_to_top))
      .assertIsDisplayed()
  }

  @Test
  fun backToTopFab_hiddenWhenShowIsFalse() {
    val state = createTestState(showBackToTopButton = false)
    composeTestRule.setContent {
      KiwixTheme {
        BackToTopFab(state)
      }
    }
    composeTestRule
      .onNodeWithContentDescription(context.getString(R.string.pref_back_to_top))
      .assertDoesNotExist()
  }

  @Test
  fun backToTopFab_click_triggersCallback() {
    var clicked = false
    val state = createTestState(
      showBackToTopButton = true,
      backToTopButtonClick = { clicked = true }
    )
    composeTestRule.setContent {
      KiwixTheme {
        BackToTopFab(state)
      }
    }
    composeTestRule
      .onNodeWithContentDescription(context.getString(R.string.pref_back_to_top))
      .performClick()
    assertTrue("BackToTop click callback should be triggered", clicked)
  }

  @Test
  fun noBookOpenView_displaysNoOpenBookText() {
    composeTestRule.setContent {
      KiwixTheme {
        NoBookOpenView(onOpenLibraryButtonClicked = {})
      }
    }
    composeTestRule
      .onNodeWithText(context.getString(R.string.no_open_book))
      .assertIsDisplayed()
  }

  @Test
  fun noBookOpenView_displaysOpenLibraryButton() {
    composeTestRule.setContent {
      KiwixTheme {
        NoBookOpenView(onOpenLibraryButtonClicked = {})
      }
    }
    composeTestRule
      .onNodeWithText(context.getString(R.string.open_library).uppercase())
      .assertIsDisplayed()
  }

  @Test
  fun noBookOpenView_openLibraryClick_triggersCallback() {
    var clicked = false
    composeTestRule.setContent {
      KiwixTheme {
        NoBookOpenView(onOpenLibraryButtonClicked = { clicked = true })
      }
    }
    composeTestRule
      .onNodeWithText(context.getString(R.string.open_library).uppercase())
      .performClick()
    assertTrue("Open Library callback should be triggered", clicked)
  }

  @Test
  fun donationLayout_visibleWhenShouldShow() {
    val state = createTestState(
      shouldShowDonationPopup = true,
      appName = "Kiwix"
    )
    composeTestRule.setContent {
      KiwixTheme {
        ShowDonationLayout(state)
      }
    }
    composeTestRule
      .onNodeWithTag(DONATION_LAYOUT_TESTING_TAG)
      .assertIsDisplayed()
  }

  @Test
  fun donationLayout_hiddenWhenShouldNotShow() {
    val state = createTestState(shouldShowDonationPopup = false)
    composeTestRule.setContent {
      KiwixTheme {
        ShowDonationLayout(state)
      }
    }
    composeTestRule
      .onNodeWithTag(DONATION_LAYOUT_TESTING_TAG)
      .assertDoesNotExist()
  }

  @Test
  fun donationLayout_donateButton_triggersCallback() {
    var clicked = false
    val state = createTestState(
      shouldShowDonationPopup = true,
      donateButtonClick = { clicked = true }
    )
    composeTestRule.setContent {
      KiwixTheme {
        ShowDonationLayout(state)
      }
    }
    composeTestRule
      .onNodeWithText(context.getString(R.string.make_donation))
      .performClick()
    assertTrue("Donate callback should be triggered", clicked)
  }

  @Test
  fun donationLayout_laterButton_triggersCallback() {
    var clicked = false
    val state = createTestState(
      shouldShowDonationPopup = true,
      laterButtonClick = { clicked = true }
    )
    composeTestRule.setContent {
      KiwixTheme {
        ShowDonationLayout(state)
      }
    }
    composeTestRule
      .onNodeWithText(context.getString(R.string.rate_dialog_neutral))
      .performClick()
    assertTrue("Later callback should be triggered", clicked)
  }
}
