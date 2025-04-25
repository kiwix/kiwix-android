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
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import applyWithViewHierarchyPrinting
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.Findable.ViewId
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.page.DELETE_MENU_ICON_TESTING_TAG
import org.kiwix.kiwixmobile.core.page.NO_ITEMS_TEXT_TESTING_TAG
import org.kiwix.kiwixmobile.core.page.PAGE_LIST_TEST_TAG
import org.kiwix.kiwixmobile.core.page.SWITCH_TEXT_TESTING_TAG
import org.kiwix.kiwixmobile.core.page.bookmark.adapter.LibkiwixBookmarkItem
import org.kiwix.kiwixmobile.core.utils.dialog.ALERT_DIALOG_CONFIRM_BUTTON_TESTING_TAG
import org.kiwix.kiwixmobile.core.utils.dialog.ALERT_DIALOG_TITLE_TEXT_TESTING_TAG
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.testutils.TestUtils.testFlakyView
import org.kiwix.kiwixmobile.utils.StandardActions.openDrawer
import java.util.concurrent.TimeUnit

fun bookmarks(func: BookmarksRobot.() -> Unit) =
  BookmarksRobot().applyWithViewHierarchyPrinting(func)

class BookmarksRobot : BaseRobot() {
  private var retryCountForBookmarkAddedButton = 5

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
    pauseForBetterTestPerformance()
    testFlakyView({
      composeTestRule.apply {
        waitForIdle()
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

  fun clickOnSaveBookmarkImage() {
    pauseForBetterTestPerformance()
    clickOn(ViewId(R.id.bottom_toolbar_bookmark))
  }

  fun longClickOnSaveBookmarkImage() {
    // wait for disappearing the snack-bar after removing the bookmark
    BaristaSleepInteractions.sleep(5L, TimeUnit.SECONDS)
    testFlakyView({ onView(withId(R.id.bottom_toolbar_bookmark)).perform(longClick()) })
  }

  fun clickOnOpenSavedBookmarkButton() {
    try {
      onView(withText("OPEN")).perform(click())
    } catch (runtimeException: RuntimeException) {
      if (retryCountForBookmarkAddedButton > 0) {
        retryCountForBookmarkAddedButton--
        clickOnOpenSavedBookmarkButton()
      } else {
        throw RuntimeException(
          "Unable to save the bookmark, original exception is" +
            " ${runtimeException.localizedMessage}"
        )
      }
    }
  }

  fun assertBookmarkSaved(composeTestRule: ComposeContentTestRule) {
    pauseForBetterTestPerformance()
    composeTestRule.apply {
      waitForIdle()
      composeTestRule.onNodeWithText("Test Zim").assertExists()
    }
  }

  fun assertBookmarkRemoved(composeTestRule: ComposeTestRule) {
    pauseForBetterTestPerformance()
    composeTestRule.apply {
      waitForIdle()
      composeTestRule.onNodeWithText("Test Zim").assertDoesNotExist()
    }
  }

  private fun pauseForBetterTestPerformance() {
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS_FOR_SEARCH_TEST.toLong())
  }

  fun openBookmarkScreen() {
    testFlakyView({
      openDrawer()
      onView(withText(R.string.bookmarks)).perform(click())
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
}
