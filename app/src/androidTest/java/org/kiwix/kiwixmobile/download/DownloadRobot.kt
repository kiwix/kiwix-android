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

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import applyWithViewHierarchyPrinting
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import com.adevinta.android.barista.interaction.BaristaSwipeRefreshInteractions.refresh
import junit.framework.AssertionFailedError
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.Findable.StringId.TextId
import org.kiwix.kiwixmobile.Findable.Text
import org.kiwix.kiwixmobile.Findable.ViewId
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.utils.files.Log
import org.kiwix.kiwixmobile.download.DownloadTest.Companion.KIWIX_DOWNLOAD_TEST
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.testutils.TestUtils.testFlakyView
import org.kiwix.kiwixmobile.utils.RecyclerViewMatcher

fun downloadRobot(func: DownloadRobot.() -> Unit) =
  DownloadRobot().applyWithViewHierarchyPrinting(func)

class DownloadRobot : BaseRobot() {

  private var retryCountForDataToLoad = 10
  private var retryCountForCheckDownloadStart = 10
  private val zimFileTitle = "Off the Grid"

  fun clickLibraryOnBottomNav() {
    clickOn(ViewId(R.id.libraryFragment))
  }

  fun clickDownloadOnBottomNav() {
    clickOn(ViewId(R.id.downloadsFragment))
  }

  fun waitForDataToLoad() {
    try {
      isVisible(TextId(R.string.your_languages))
    } catch (e: RuntimeException) {
      if (retryCountForDataToLoad > 0) {
        retryCountForDataToLoad--
        waitForDataToLoad()
        return
      }
      // throw the exception when there is no more retry left.
      throw RuntimeException("Couldn't load the online library list.\n Original exception = $e")
    }
  }

  fun checkIfZimFileDownloaded() {
    isVisible(Text(zimFileTitle))
  }

  fun refreshOnlineList() {
    refresh(R.id.librarySwipeRefresh)
  }

  fun downloadZimFile() {
    pauseForBetterTestPerformance()
    testFlakyView({
      onView(
        RecyclerViewMatcher(R.id.libraryList).atPosition(
          1
        )
      ).perform(click())
    })
  }

  fun assertDownloadStart() {
    try {
      isVisible(ViewId(R.id.stop))
    } catch (e: RuntimeException) {
      if (retryCountForCheckDownloadStart > 0) {
        retryCountForCheckDownloadStart--
        assertDownloadStart()
      }
    }
  }

  private fun stopDownload() {
    testFlakyView({ onView(withId(R.id.stop)).perform(click()) })
  }

  fun pauseDownload() {
    clickOn(ViewId(R.id.pauseResume))
  }

  fun assertDownloadPaused() {
    pauseForBetterTestPerformance()
    onView(withText(org.kiwix.kiwixmobile.core.R.string.paused_state)).check(matches(isDisplayed()))
  }

  fun resumeDownload() {
    pauseDownload()
  }

  fun assertDownloadResumed() {
    pauseForBetterTestPerformance()
    onView(withText(org.kiwix.kiwixmobile.core.R.string.paused_state)).check(doesNotExist())
  }

  fun waitUntilDownloadComplete() {
    try {
      onView(withId(R.id.stop)).check(doesNotExist())
      Log.i("kiwixDownloadTest", "Download complete")
    } catch (e: AssertionFailedError) {
      BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS_FOR_DOWNLOAD_TEST.toLong())
      Log.i("kiwixDownloadTest", "Downloading in progress")
      waitUntilDownloadComplete()
    }
  }

  private fun pauseForBetterTestPerformance() {
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
  }

  private fun assertStopDownloadDialogDisplayed() {
    pauseForBetterTestPerformance()
    isVisible(TextId(R.string.confirm_stop_download_title))
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
      Log.i(
        "DOWNLOAD_TEST",
        "Failed to stop download with title [" + zimFileTitle + "]... " +
          "Probably because it doesn't download the zim file"
      )
    }
  }

  fun refreshLocalLibraryData() {
    try {
      refresh(R.id.zim_swiperefresh)
      pauseForBetterTestPerformance()
    } catch (e: RuntimeException) {
      Log.w(KIWIX_DOWNLOAD_TEST, "Failed to refresh ZIM list: " + e.localizedMessage)
    }
  }
}
