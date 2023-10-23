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

import android.util.Log
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import applyWithViewHierarchyPrinting
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import junit.framework.AssertionFailedError
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.Findable.Text
import org.kiwix.kiwixmobile.Findable.ViewId
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.testutils.TestUtils

fun downloadRobot(func: DownloadRobot.() -> Unit) =
  DownloadRobot().applyWithViewHierarchyPrinting(func)

class DownloadRobot : BaseRobot() {

  private var retryCountForDataToLoad = 5
  private var retryCountForCheckDownloadStart = 5
  private val zimFileTitle = "Off the Grid"

  fun clickLibraryOnBottomNav() {
    clickOn(ViewId(R.id.libraryFragment))
  }

  fun clickDownloadOnBottomNav() {
    clickOn(ViewId(R.id.downloadsFragment))
  }

  private fun longClickOnZimFile() {
    longClickOn(Text(zimFileTitle))
  }

  private fun clickOnFileDeleteIcon() {
    clickOn(ViewId(R.id.zim_file_delete_item))
  }

  private fun assertDeleteDialogDisplayed() {
    pauseForBetterTestPerformance()
    onView(withText("DELETE")).check(matches(isDisplayed()))
  }

  private fun clickOnDeleteZimFile() {
    pauseForBetterTestPerformance()
    onView(withText("DELETE")).perform(click())
  }

  fun deleteZimIfExists(shouldDeleteZimFile: Boolean) {
    try {
      longClickOnZimFile()
      clickOnFileDeleteIcon()
      assertDeleteDialogDisplayed()
      clickOnDeleteZimFile()
      pauseForBetterTestPerformance()
    } catch (e: Exception) {
      if (shouldDeleteZimFile) {
        throw Exception(
          "$zimFileTitle downloaded successfully. " +
            "But failed to delete $zimFileTitle file " +
            "Actual exception ${e.localizedMessage}"
        )
      }
      Log.i(
        "TEST_DELETE_ZIM",
        "Failed to delete ZIM file with title [" + zimFileTitle + "]... " +
          "Probably because it doesn't exist"
      )
    }
  }

  fun waitForDataToLoad() {
    try {
      isVisible(Text(zimFileTitle))
    } catch (e: RuntimeException) {
      if (retryCountForDataToLoad > 0) {
        retryCountForDataToLoad--
        waitForDataToLoad()
      }
    }
  }

  fun checkIfZimFileDownloaded() {
    isVisible(Text(zimFileTitle))
  }

  fun downloadZimFile() {
    clickOn(Text(zimFileTitle))
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
    clickOn(ViewId(R.id.stop))
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
    isVisible(Text("Stop download?"))
  }

  private fun clickOnYesButton() {
    onView(withText("YES")).perform(click())
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
}
