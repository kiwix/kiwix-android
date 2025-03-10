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

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import applyWithViewHierarchyPrinting
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.Findable.StringId.ContentDesc
import org.kiwix.kiwixmobile.Findable.StringId.TextId
import org.kiwix.kiwixmobile.Findable.Text
import org.kiwix.kiwixmobile.Findable.ViewId
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.page.bookmark.adapter.LibkiwixBookmarkItem
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.testutils.TestUtils.testFlakyView
import org.kiwix.kiwixmobile.utils.StandardActions.openDrawer
import java.util.concurrent.TimeUnit

fun bookmarks(func: BookmarksRobot.() -> Unit) =
  BookmarksRobot().applyWithViewHierarchyPrinting(func)

class BookmarksRobot : BaseRobot() {
  private var retryCountForBookmarkAddedButton = 5

  fun assertBookMarksDisplayed() {
    assertDisplayed(R.string.bookmarks_from_current_book)
  }

  fun clickOnTrashIcon() {
    clickOn(ContentDesc(R.string.pref_clear_all_bookmarks_title))
  }

  fun assertDeleteBookmarksDialogDisplayed() {
    testFlakyView({ isVisible(TextId(R.string.delete_bookmarks)) })
  }

  fun clickOnDeleteButton() {
    pauseForBetterTestPerformance()
    testFlakyView({ onView(withText("DELETE")).perform(click()) })
  }

  fun assertNoBookMarkTextDisplayed() {
    testFlakyView({ isVisible(TextId(R.string.no_bookmarks)) })
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

  fun assertBookmarkSaved() {
    pauseForBetterTestPerformance()
    isVisible(Text("Test Zim"))
  }

  fun assertBookmarkRemoved() {
    pauseForBetterTestPerformance()
    onView(withText("Test Zim")).check(ViewAssertions.doesNotExist())
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

  fun testAllBookmarkShowing(bookmarkList: ArrayList<LibkiwixBookmarkItem>) {
    bookmarkList.forEachIndexed { index, libkiwixBookmarkItem ->
      testFlakyView({
        onView(withId(R.id.recycler_view))
          .perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(index))
          .check(matches(hasDescendant(withText(libkiwixBookmarkItem.title))))
      })
    }
  }
}
