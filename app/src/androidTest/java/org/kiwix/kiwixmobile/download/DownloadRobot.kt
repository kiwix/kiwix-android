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
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.contrib.RecyclerViewActions.scrollToPosition
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
  private val zimFileTitle = "A little question a day"

  fun clickLibraryOnBottomNav() {
    clickOn(ViewId(R.id.libraryFragment))
  }

  fun clickDownloadOnBottomNav() {
    clickOn(ViewId(R.id.downloadsFragment))
  }

  fun deleteZimIfExists() {
    try {
      longClickOn(Text(zimFileTitle))
      clickOn(ViewId(R.id.zim_file_delete_item))
      onView(withText("DELETE")).perform(click())
    } catch (e: RuntimeException) {
      Log.i(
        "TEST_DELETE_ZIM",
        "Failed to delete ZIM file with title [" + zimFileTitle + "]... " +
          "Probably because it doesn't exist"
      )
    }
  }

  fun waitForDataToLoad() {
    try {
      isVisible(Text("Off the Grid"))
    } catch (e: RuntimeException) {
      if (retryCountForDataToLoad > 0) {
        retryCountForDataToLoad--
        waitForDataToLoad()
      }
    }
  }

  fun scrollToAlpineWikiZim() {
    onView(withId(R.id.libraryList)).perform(scrollToPosition<ViewHolder>(15))
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
}
