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

package org.kiwix.kiwixmobile.download

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withSubstring
import androidx.test.espresso.matcher.ViewMatchers.withText
import applyWithViewHierarchyPrinting
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import com.adevinta.android.barista.interaction.BaristaSwipeRefreshInteractions.refresh
import junit.framework.AssertionFailedError
import org.hamcrest.Matcher
import org.junit.Assert
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.Findable.StringId.TextId
import org.kiwix.kiwixmobile.Findable.ViewId
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.utils.files.Log
import org.kiwix.kiwixmobile.download.DownloadTest.Companion.KIWIX_DOWNLOAD_TEST
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.testutils.TestUtils.testFlakyView
import org.kiwix.kiwixmobile.utils.RecyclerViewMatcher
import org.kiwix.kiwixmobile.zimManager.libraryView.adapter.LibraryListItem

fun downloadRobot(func: DownloadRobot.() -> Unit) =
  DownloadRobot().applyWithViewHierarchyPrinting(func)

class DownloadRobot : BaseRobot() {

  fun clickLibraryOnBottomNav() {
    clickOn(ViewId(R.id.libraryFragment))
  }

  fun clickDownloadOnBottomNav() {
    clickOn(ViewId(R.id.downloadsFragment))
  }

  fun waitForDataToLoad(retryCountForDataToLoad: Int = 10) {
    try {
      isVisible(TextId(string.your_languages))
    } catch (e: RuntimeException) {
      if (retryCountForDataToLoad > 0) {
        // refresh the data if there is "Swipe Down for Library" visible on the screen.
        refreshOnlineListIfSwipeDownForLibraryTextVisible()
        waitForDataToLoad(retryCountForDataToLoad - 1)
        return
      }
      // throw the exception when there is no more retry left.
      throw RuntimeException("Couldn't load the online library list.\n Original exception = $e")
    }
  }

  private fun refreshOnlineListIfSwipeDownForLibraryTextVisible() {
    try {
      onView(withText(string.swipe_down_for_library)).check(matches(isDisplayed()))
      refreshOnlineList()
    } catch (e: RuntimeException) {
      try {
        // do nothing as currently downloading the online library.
        onView(withId(R.id.onlineLibraryProgressLayout)).check(matches(isDisplayed()))
      } catch (e: RuntimeException) {
        // if not visible try to get the online library.
        refreshOnlineList()
      }
    }
  }

  fun checkIfZimFileDownloaded() {
    pauseForBetterTestPerformance()
    try {
      testFlakyView({
        onView(withId(R.id.file_management_no_files)).check(matches(isDisplayed()))
      })
      // if the "No files here" text found that means it failed to download the ZIM file.
      Assert.fail("Couldn't download the zim file. The [No files here] text is visible on screen")
    } catch (e: AssertionFailedError) {
      // check if "No files here" text is not visible on
      // screen that means zim file is downloaded successfully.
    }
  }

  private fun refreshOnlineList() {
    refresh(R.id.librarySwipeRefresh)
  }

  fun downloadZimFile(position: Int = 1) {
    pauseForBetterTestPerformance()
    testFlakyView({
      onView(
        RecyclerViewMatcher(R.id.libraryList).atPosition(
          position
        )
      ).perform(click())
    })
  }

  fun assertDownloadStart() {
    testFlakyView({ onView(withId(R.id.stop)).check(matches(isDisplayed())) })
  }

  private fun stopDownload() {
    testFlakyView({ onView(withId(R.id.stop)).perform(click()) })
  }

  fun pauseDownload() {
    clickOn(ViewId(R.id.pauseResume))
  }

  fun assertDownloadPaused() {
    testFlakyView({
      onView(withSubstring(context.getString(string.paused_state))).check(matches(isDisplayed()))
    })
  }

  fun resumeDownload() {
    pauseDownload()
  }

  fun assertDownloadResumed() {
    pauseForBetterTestPerformance()
    onView(withText(org.kiwix.kiwixmobile.core.R.string.paused_state)).check(doesNotExist())
  }

  // wait for 5 minutes for downloading the ZIM file
  fun waitUntilDownloadComplete(retryCountForDownloadingZimFile: Int = 30) {
    try {
      onView(withId(R.id.stop)).check(doesNotExist())
      Log.e(KIWIX_DOWNLOAD_TEST, "Download complete")
    } catch (e: AssertionFailedError) {
      if (retryCountForDownloadingZimFile > 0) {
        resumeDownloadIfPaused()
        BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS_FOR_DOWNLOAD_TEST.toLong())
        Log.e(KIWIX_DOWNLOAD_TEST, "Downloading in progress")
        waitUntilDownloadComplete(retryCountForDownloadingZimFile - 1)
        return
      }
      // throw the exception when there is no more retry left.
      throw RuntimeException("Couldn't download the ZIM file.\n Original exception = $e")
    }
  }

  private fun resumeDownloadIfPaused() {
    try {
      onView(withSubstring(context.getString(string.paused_state))).check(matches(isDisplayed()))
      resumeDownload()
    } catch (e: AssertionFailedError) {
      // do nothing since downloading is In Progress.
    } catch (e: RuntimeException) {
      // do nothing since downloading is In Progress.
    }
  }

  private fun pauseForBetterTestPerformance() {
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
  }

  private fun assertStopDownloadDialogDisplayed() {
    pauseForBetterTestPerformance()
    isVisible(TextId(string.confirm_stop_download_title))
  }

  private fun clickOnYesButton() {
    try {
      onView(withText("YES")).perform(click())
    } catch (ignore: Exception) {
      // stop the downloading for Albanian language
      onView(withText("PO")).perform(click())
    }
  }

  fun stopDownloadIfAlreadyStarted() {
    try {
      pauseForBetterTestPerformance()
      onView(withId(R.id.stop)).check(matches(isDisplayed()))
      stopDownload()
      assertStopDownloadDialogDisplayed()
      clickOnYesButton()
      pauseForBetterTestPerformance()
    } catch (e: Exception) {
      Log.e(
        KIWIX_DOWNLOAD_TEST,
        "Failed to stop downloading. Probably because it is not downloading the zim file"
      )
    }
  }

  fun getSmallestZimFileIndex(it: List<LibraryListItem>?): Int {
    var zimFileSizeWithIndex: Pair<Int, Long> = 0 to Long.MAX_VALUE
    it?.forEachIndexed { index, libraryItem ->
      if (libraryItem is LibraryListItem.BookItem) {
        val bookSize = libraryItem.book.size.toLong()
        if (bookSize < 20000L) {
          return@getSmallestZimFileIndex index
        } else if (bookSize < zimFileSizeWithIndex.second) {
          zimFileSizeWithIndex = index to bookSize
        }
      }
    }
    return zimFileSizeWithIndex.first
  }

  fun scrollToZimFileIndex(index: Int) {
    testFlakyView({
      onView(withId(R.id.libraryList))
        .perform(scrollToTop(index))
    })
  }

  private fun scrollToTop(position: Int): ViewAction {
    return object : ViewAction {
      override fun getDescription(): String =
        "scroll RecyclerView item at position $position to the top"

      override fun getConstraints(): Matcher<View> =
        androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom(RecyclerView::class.java)

      override fun perform(uiController: UiController, view: View) {
        val recyclerView = view as RecyclerView
        val viewHolder = recyclerView.findViewHolderForAdapterPosition(position)

        if (viewHolder?.itemView == null) {
          recyclerView.scrollToPosition(position)
          uiController.loopMainThreadUntilIdle()
        }

        val newViewHolder = recyclerView.findViewHolderForAdapterPosition(position)
        newViewHolder?.let {
          val top = newViewHolder.itemView.top
          recyclerView.scrollBy(0, top)
        }
      }
    }
  }
}
