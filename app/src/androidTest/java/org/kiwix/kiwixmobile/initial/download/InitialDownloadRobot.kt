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

package org.kiwix.kiwixmobile.initial.download

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import applyWithViewHierarchyPrinting
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import com.adevinta.android.barista.interaction.BaristaSwipeRefreshInteractions.refresh
import junit.framework.AssertionFailedError
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.Findable.Text
import org.kiwix.kiwixmobile.Findable.ViewId
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.testutils.TestUtils

fun initialDownload(func: InitialDownloadRobot.() -> Unit) =
  InitialDownloadRobot().applyWithViewHierarchyPrinting(func)

class InitialDownloadRobot : BaseRobot() {

  private var retryCountForCheckDownloadStart = 5

  fun clickDownloadOnBottomNav() {
    clickOn(ViewId(R.id.downloadsFragment))
  }

  fun assertLibraryListDisplayed() {
    isVisible(ViewId(R.id.libraryList))
  }

  fun refreshList() {
    refresh(R.id.librarySwipeRefresh)
  }

  fun waitForDataToLoad() {
    try {
      isVisible(Text("Off the Grid"))
    } catch (e: RuntimeException) {
      BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS_FOR_DOWNLOAD_TEST.toLong())
      waitForDataToLoad()
    }
  }

  fun downloadZimFile() {
    clickOn(Text("Off the Grid"))
  }

  fun assertStorageConfigureDialogDisplayed() {
    isVisible(Text("Download book to internal storage?"))
  }

  fun assertStopDownloadDialogDisplayed() {
    isVisible(Text("Stop download?"))
  }

  fun clickOnYesToConfirm() {
    onView(withText("YES")).perform(click())
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

  fun stopDownload() {
    clickOn(ViewId(R.id.stop))
  }

  fun assertDownloadStop() {
    try {
      onView(withId(R.id.stop)).check(doesNotExist())
    } catch (e: AssertionFailedError) {
      BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS_FOR_DOWNLOAD_TEST.toLong())
      assertDownloadStop()
    }
  }
}
