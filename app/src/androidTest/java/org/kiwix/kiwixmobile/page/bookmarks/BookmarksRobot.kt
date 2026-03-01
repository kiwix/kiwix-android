/*
 * Kiwix Android
 * Copyright (c) 2020 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.page.bookmarks

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.test.espresso.web.sugar.Web
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.DriverAtoms.webClick
import androidx.test.espresso.web.webdriver.Locator
import applyWithViewHierarchyPrinting
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.main.LEFT_DRAWER_BOOKMARK_ITEM_TESTING_TAG
import org.kiwix.kiwixmobile.core.main.reader.READER_BOTTOM_BAR_BOOKMARK_BUTTON_TESTING_TAG
import org.kiwix.kiwixmobile.core.main.reader.READER_BOTTOM_BAR_HOME_BUTTON_TESTING_TAG
import org.kiwix.kiwixmobile.core.main.reader.READER_BOTTOM_BAR_PREVIOUS_SCREEN_BUTTON_TESTING_TAG
import org.kiwix.kiwixmobile.core.page.DELETE_MENU_ICON_TESTING_TAG
import org.kiwix.kiwixmobile.core.page.NO_ITEMS_TEXT_TESTING_TAG
import org.kiwix.kiwixmobile.core.page.PAGE_ITEM_TESTING_TAG
import org.kiwix.kiwixmobile.core.page.PAGE_LIST_TEST_TAG
import org.kiwix.kiwixmobile.core.page.SWITCH_TEXT_TESTING_TAG
import org.kiwix.kiwixmobile.core.page.bookmark.models.LibkiwixBookmarkItem
import org.kiwix.kiwixmobile.core.utils.dialog.ALERT_DIALOG_CONFIRM_BUTTON_TESTING_TAG
import org.kiwix.kiwixmobile.core.utils.dialog.ALERT_DIALOG_TITLE_TEXT_TESTING_TAG
import org.kiwix.kiwixmobile.main.BOTTOM_NAV_READER_ITEM_TESTING_TAG
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.testutils.TestUtils.TEST_PAUSE_MS
import org.kiwix.kiwixmobile.testutils.TestUtils.TEST_PAUSE_MS_FOR_DOWNLOAD_TEST
import org.kiwix.kiwixmobile.testutils.TestUtils.testFlakyView
import org.kiwix.kiwixmobile.testutils.TestUtils.waitUntilTimeout
import org.kiwix.kiwixmobile.utils.StandardActions.openDrawer

fun bookmarks(func: BookmarksRobot.() -> Unit) =
  BookmarksRobot().applyWithViewHierarchyPrinting(func)

class BookmarksRobot : BaseRobot() {
  fun assertBookMarksDisplayed(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitForIdle()
      onNodeWithTag(SWITCH_TEXT_TESTING_TAG)
        .assertTextEquals(context.getString(R.string.bookmarks_from_current_book))
    }
  }

  fun clickOnTrashIcon(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitForIdle()
      onNodeWithTag(DELETE_MENU_ICON_TESTING_TAG)
        .performClick()
    }
  }

  fun assertDeleteBookmarksDialogDisplayed(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.apply {
        waitForIdle()
        onNodeWithTag(ALERT_DIALOG_TITLE_TEXT_TESTING_TAG)
          .assertTextEquals(context.getString(R.string.delete_bookmarks))
      }
    })
  }

  fun clickOnDeleteButton(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.apply {
        waitUntil(TestUtils.TEST_PAUSE_MS.toLong()) {
          onNodeWithTag(ALERT_DIALOG_CONFIRM_BUTTON_TESTING_TAG).isDisplayed()
        }
        onNodeWithTag(ALERT_DIALOG_CONFIRM_BUTTON_TESTING_TAG)
          .assertTextEquals(context.getString(R.string.delete).uppercase())
          .performClick()
      }
    })
  }

  fun assertNoBookMarkTextDisplayed(composeTestRule: ComposeTestRule) {
    composeTestRule.apply {
      waitForIdle()
      onNodeWithTag(NO_ITEMS_TEXT_TESTING_TAG)
        .assertTextEquals(context.getString(R.string.no_bookmarks))
    }
  }

  fun clickOnSaveBookmarkImage(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitForIdle()
      waitUntilTimeout()
      waitUntil(TEST_PAUSE_MS_FOR_DOWNLOAD_TEST.toLong()) {
        onNodeWithTag(READER_BOTTOM_BAR_BOOKMARK_BUTTON_TESTING_TAG).isDisplayed()
      }
      onNodeWithTag(READER_BOTTOM_BAR_BOOKMARK_BUTTON_TESTING_TAG)
        .performClick()
    }
  }

  fun longClickOnSaveBookmarkImage(
    composeTestRule: ComposeContentTestRule,
    timeout: Long = TEST_PAUSE_MS.toLong()
  ) {
    composeTestRule.apply {
      waitForIdle()
      // wait for disappearing the snack-bar after removing the bookmark
      waitUntilTimeout(timeout)
      waitUntil(TEST_PAUSE_MS_FOR_DOWNLOAD_TEST.toLong()) {
        onNodeWithTag(READER_BOTTOM_BAR_BOOKMARK_BUTTON_TESTING_TAG).isDisplayed()
      }
      onNodeWithTag(READER_BOTTOM_BAR_BOOKMARK_BUTTON_TESTING_TAG)
        .performTouchInput {
          longClick()
        }
    }
  }

  fun assertBookmarkSaved(composeTestRule: ComposeContentTestRule) {
    pauseForBetterTestPerformance()
    testFlakyView({
      composeTestRule.apply {
        waitForIdle()
        onNodeWithText("Test Zim").assertExists()
      }
    })
  }

  private fun pauseForBetterTestPerformance() {
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS_FOR_SEARCH_TEST.toLong())
  }

  fun openBookmarkScreen(
    coreMainActivity: CoreMainActivity,
    composeTestRule: ComposeContentTestRule
  ) {
    testFlakyView({
      composeTestRule.waitForIdle()
      openDrawer(coreMainActivity)
      composeTestRule.apply {
        waitUntilTimeout()
        onNodeWithTag(LEFT_DRAWER_BOOKMARK_ITEM_TESTING_TAG).performClick()
      }
    })
  }

  fun testAllBookmarkShowing(
    bookmarkList: ArrayList<LibkiwixBookmarkItem>,
    composeTestRule: ComposeTestRule
  ) {
    composeTestRule.apply {
      waitForIdle()
      bookmarkList.forEachIndexed { index, libkiwixBookmarkItem ->
        testFlakyView({
          composeTestRule.onNodeWithTag(PAGE_LIST_TEST_TAG)
            .performScrollToNode(hasText(libkiwixBookmarkItem.title))
          composeTestRule.onNodeWithText(libkiwixBookmarkItem.title).assertExists()
        })
      }
    }
  }

  fun assertZimFileLoadedIntoTheReader(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      composeTestRule.apply {
        waitUntilTimeout()
        onNodeWithTag(BOTTOM_NAV_READER_ITEM_TESTING_TAG).performClick()
      }
    }
    testFlakyView({
      Web.onWebView()
        .withElement(
          DriverAtoms.findElement(
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

  fun assertAndroidArticleLoadedInReader(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.apply {
        waitForIdle()
        onWebView()
          .withElement(
            findElement(
              Locator.XPATH,
              "//*[contains(text(), 'History')]"
            )
          )
      }
    })
  }

  fun clickOnBackwardButton(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitForIdle()
      // wait for disappearing the snack-bar after removing the bookmark
      waitUntilTimeout(TEST_PAUSE_MS_FOR_DOWNLOAD_TEST.toLong())
      waitUntil(TEST_PAUSE_MS_FOR_DOWNLOAD_TEST.toLong()) {
        onNodeWithTag(READER_BOTTOM_BAR_PREVIOUS_SCREEN_BUTTON_TESTING_TAG).isDisplayed()
      }
      onNodeWithTag(READER_BOTTOM_BAR_PREVIOUS_SCREEN_BUTTON_TESTING_TAG)
        .performClick()
    }
  }

  fun clickOnHomeButton(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitForIdle()
      // wait for disappearing the snack-bar after removing the bookmark
      waitUntilTimeout(TEST_PAUSE_MS_FOR_DOWNLOAD_TEST.toLong())
      waitUntil(TEST_PAUSE_MS_FOR_DOWNLOAD_TEST.toLong()) {
        onNodeWithTag(READER_BOTTOM_BAR_HOME_BUTTON_TESTING_TAG).isDisplayed()
      }
      onNodeWithTag(READER_BOTTOM_BAR_HOME_BUTTON_TESTING_TAG)
        .performClick()
    }
  }

  fun openBookmarkInReader(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.apply {
        waitForIdle()
        onAllNodesWithTag(PAGE_ITEM_TESTING_TAG)[0].performClick()
      }
    })
  }
}
