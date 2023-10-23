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

package org.kiwix.kiwixmobile.nav.destination.library

import android.util.Log
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import applyWithViewHierarchyPrinting
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.Findable.Text
import org.kiwix.kiwixmobile.Findable.ViewId
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.localFileTransfer.LocalFileTransferRobot
import org.kiwix.kiwixmobile.localFileTransfer.localFileTransfer
import org.kiwix.kiwixmobile.testutils.TestUtils

fun library(func: LibraryRobot.() -> Unit) = LibraryRobot().applyWithViewHierarchyPrinting(func)

class LibraryRobot : BaseRobot() {

  private val zimFileTitle = "Test_Zim"

  fun assertGetZimNearbyDeviceDisplayed() {
    isVisible(ViewId(R.id.get_zim_nearby_device))
  }

  fun clickFileTransferIcon(func: LocalFileTransferRobot.() -> Unit) {
    clickOn(ViewId(R.id.get_zim_nearby_device))
    localFileTransfer(func)
  }

  fun assertLibraryListDisplayed() {
    isVisible(ViewId(R.id.zimfilelist))
  }

  fun assertNoFilesTextDisplayed() {
    pauseForBetterTestPerformance()
    isVisible(ViewId(R.id.file_management_no_files))
  }

  fun deleteZimIfExists() {
    try {
      longClickOnZimFile()
      clickOnFileDeleteIcon()
      assertDeleteDialogDisplayed()
      clickOnDeleteZimFile()
      pauseForBetterTestPerformance()
    } catch (e: Exception) {
      Log.i(
        "TEST_DELETE_ZIM",
        "Failed to delete ZIM file with title [" + zimFileTitle + "]... " +
          "Probably because it doesn't exist"
      )
    }
  }

  private fun clickOnFileDeleteIcon() {
    clickOn(ViewId(R.id.zim_file_delete_item))
  }

  private fun assertDeleteDialogDisplayed() {
    pauseForBetterTestPerformance()
    onView(withText("DELETE"))
      .check(ViewAssertions.matches(isDisplayed()))
  }

  private fun longClickOnZimFile() {
    longClickOn(Text(zimFileTitle))
  }

  private fun clickOnDeleteZimFile() {
    pauseForBetterTestPerformance()
    onView(withText("DELETE")).perform(click())
  }

  private fun pauseForBetterTestPerformance() {
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
  }
}
