/*
 * Kiwix Android
 * Copyright (c) 2022 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.note

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.DriverAtoms.webClick
import androidx.test.espresso.web.webdriver.Locator
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.main.ADD_NOTE_DIALOG_CLOSE_IMAGE_BUTTON_TESTING_TAG
import org.kiwix.kiwixmobile.core.main.ADD_NOTE_TEXT_FILED_TESTING_TAG
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.main.DELETE_MENU_BUTTON_TESTING_TAG
import org.kiwix.kiwixmobile.core.main.LEFT_DRAWER_NOTES_ITEM_TESTING_TAG
import org.kiwix.kiwixmobile.core.main.SAVE_MENU_BUTTON_TESTING_TAG
import org.kiwix.kiwixmobile.core.main.reader.READER_BOTTOM_BAR_PREVIOUS_SCREEN_BUTTON_TESTING_TAG
import org.kiwix.kiwixmobile.core.main.reader.TAKE_NOTE_MENU_ITEM_TESTING_TAG
import org.kiwix.kiwixmobile.core.page.DELETE_MENU_ICON_TESTING_TAG
import org.kiwix.kiwixmobile.core.page.NO_ITEMS_TEXT_TESTING_TAG
import org.kiwix.kiwixmobile.core.page.PAGE_ITEM_TESTING_TAG
import org.kiwix.kiwixmobile.core.page.SWITCH_TEXT_TESTING_TAG
import org.kiwix.kiwixmobile.core.ui.components.OVERFLOW_MENU_BUTTON_TESTING_TAG
import org.kiwix.kiwixmobile.core.ui.components.TOOLBAR_TITLE_TESTING_TAG
import org.kiwix.kiwixmobile.core.utils.dialog.ALERT_DIALOG_CONFIRM_BUTTON_TESTING_TAG
import org.kiwix.kiwixmobile.core.utils.dialog.ALERT_DIALOG_DISMISS_BUTTON_TESTING_TAG
import org.kiwix.kiwixmobile.core.utils.dialog.ALERT_DIALOG_TITLE_TEXT_TESTING_TAG
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.testutils.TestUtils.TEST_PAUSE_MS_FOR_DOWNLOAD_TEST
import org.kiwix.kiwixmobile.testutils.TestUtils.testFlakyView
import org.kiwix.kiwixmobile.testutils.TestUtils.waitUntilTimeout
import org.kiwix.kiwixmobile.utils.StandardActions.openDrawer

fun note(func: NoteRobot.() -> Unit) = NoteRobot().apply(func)

class NoteRobot : BaseRobot() {
  private val noteText = "Test Note"

  fun assertToolbarExist(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithTag(TOOLBAR_TITLE_TESTING_TAG)
        .assertTextEquals(context.getString(R.string.pref_notes))
    })
  }

  fun assertSwitchWidgetExist(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitForIdle()
      onNodeWithTag(SWITCH_TEXT_TESTING_TAG)
        .assertTextEquals(context.getString(R.string.notes_from_all_books))
    }
  }

  fun clickOnNoteMenuItem(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitUntil(TEST_PAUSE_MS_FOR_DOWNLOAD_TEST.toLong()) {
        onNodeWithTag(OVERFLOW_MENU_BUTTON_TESTING_TAG).isDisplayed()
      }
      onNodeWithTag(OVERFLOW_MENU_BUTTON_TESTING_TAG).performClick()
      waitUntilTimeout()
      onNodeWithTag(TAKE_NOTE_MENU_ITEM_TESTING_TAG).performClick()
    }
  }

  fun assertNoteDialogDisplayed(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.waitForIdle()
      composeTestRule.waitUntil(
        TestUtils.TEST_PAUSE_MS.toLong()
      ) {
        composeTestRule.onAllNodesWithTag(TOOLBAR_TITLE_TESTING_TAG)
          .filter(hasText(context.getString(R.string.note)))
          .onFirst()
          .isDisplayed()
      }
    })
  }

  fun writeDemoNote(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.waitForIdle()
      // Click on the TextField to focus it
      composeTestRule.onNodeWithTag(ADD_NOTE_TEXT_FILED_TESTING_TAG)
        .assertExists("TextField not found in dialog")
        .performClick()
        .performTextReplacement(noteText)

      composeTestRule.waitForIdle()

      composeTestRule.onNodeWithTag(ADD_NOTE_TEXT_FILED_TESTING_TAG)
        .assertTextContains(noteText, substring = true)

      // Close the keyboard after typing
      closeSoftKeyboard()
    })
  }

  fun saveNote(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithTag(SAVE_MENU_BUTTON_TESTING_TAG)
        .performClick()
    })
  }

  fun openNoteFragment(
    coreMainActivity: CoreMainActivity,
    composeTestRule: ComposeContentTestRule
  ) {
    openDrawer(coreMainActivity)
    testFlakyView({
      composeTestRule.apply {
        waitForIdle()
        onNodeWithTag(LEFT_DRAWER_NOTES_ITEM_TESTING_TAG).performClick()
      }
    })
  }

  fun clickOnSavedNote(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitForIdle()
      onAllNodesWithTag(PAGE_ITEM_TESTING_TAG)[0].performClick()
    }
  }

  fun clickOnOpenNote(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.apply {
        waitUntil(
          TestUtils.TEST_PAUSE_MS.toLong()
        ) { onNodeWithTag(ALERT_DIALOG_DISMISS_BUTTON_TESTING_TAG).isDisplayed() }
        onNodeWithTag(ALERT_DIALOG_DISMISS_BUTTON_TESTING_TAG)
          .performClick()
      }
    })
  }

  fun assertNoteSaved(composeTestRule: ComposeContentTestRule) {
    // This is flaky since it is shown in a dialog and sometimes
    // UIDevice does not found the view immediately due to rendering process.
    testFlakyView({
      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithTag(ADD_NOTE_TEXT_FILED_TESTING_TAG)
        .assertTextEquals(noteText)
    })
  }

  fun assertNotDoesNotExist(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithTag(ADD_NOTE_TEXT_FILED_TESTING_TAG)
        .assertTextContains("", ignoreCase = true)
    })
  }

  fun closeAddNoteDialog(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.apply {
        waitForIdle()
        onNodeWithTag(ADD_NOTE_DIALOG_CLOSE_IMAGE_BUTTON_TESTING_TAG).performClick()
      }
    })
  }

  fun clickOnDeleteIcon(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithTag(DELETE_MENU_BUTTON_TESTING_TAG)
        .performClick()
    })
  }

  fun clickOnTrashIcon(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.apply {
        waitForIdle()
        onNodeWithTag(DELETE_MENU_ICON_TESTING_TAG)
          .performClick()
      }
    })
  }

  fun assertDeleteNoteDialogDisplayed(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.apply {
        waitForIdle()
        onNodeWithTag(ALERT_DIALOG_TITLE_TEXT_TESTING_TAG)
          .assertTextEquals(context.getString(R.string.delete_notes_confirmation_msg))
      }
    })
  }

  fun clickOnDeleteButton(composeTestRule: ComposeContentTestRule) {
    testFlakyView(
      {
        composeTestRule.apply {
          waitForIdle()
          onNodeWithTag(ALERT_DIALOG_CONFIRM_BUTTON_TESTING_TAG)
            .performClick()
        }
      }
    )
  }

  fun assertNoNotesTextDisplayed(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitForIdle()
      onNodeWithTag(NO_ITEMS_TEXT_TESTING_TAG)
        .assertTextEquals(context.getString(R.string.no_notes))
    }
  }

  fun assertHomePageIsLoadedOfTestZimFile() {
    pauseForBetterTestPerformance()
    testFlakyView({
      onWebView()
        .withElement(
          findElement(
            Locator.XPATH,
            "//*[contains(text(), 'Android_(operating_system)')]"
          )
        )
    })
  }

  fun clickOnAndroidArticle(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.apply {
        waitForIdle()
        onWebView()
          .withElement(
            findElement(
              Locator.XPATH,
              "//*[contains(text(), 'Android_(operating_system)')]"
            )
          )
          .perform(webClick())
      }
    })
  }

  fun clickOnBackwardButton(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitForIdle()
      waitUntil(TEST_PAUSE_MS_FOR_DOWNLOAD_TEST.toLong()) {
        onNodeWithTag(READER_BOTTOM_BAR_PREVIOUS_SCREEN_BUTTON_TESTING_TAG).isDisplayed()
      }
      onNodeWithTag(READER_BOTTOM_BAR_PREVIOUS_SCREEN_BUTTON_TESTING_TAG)
        .performClick()
    }
  }

  fun clickOnOpenArticle(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.apply {
        waitUntil(
          TestUtils.TEST_PAUSE_MS.toLong()
        ) { onNodeWithTag(ALERT_DIALOG_CONFIRM_BUTTON_TESTING_TAG).isDisplayed() }
        onNodeWithTag(ALERT_DIALOG_CONFIRM_BUTTON_TESTING_TAG)
          .performClick()
      }
    })
  }

  fun assertAndroidArticleLoadedInReader(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.apply {
        waitForIdle()
        waitUntilTimeout()
        onWebView()
          .withElement(
            findElement(
              Locator.XPATH,
              "//*[contains(text(), 'History')]"
            )
          )
          .perform(webClick())
      }
    })
  }

  private fun pauseForBetterTestPerformance() {
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS_FOR_SEARCH_TEST.toLong())
  }
}
