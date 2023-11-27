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

package org.kiwix.kiwixmobile.webserver

import android.util.Log
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import applyWithViewHierarchyPrinting
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import com.adevinta.android.barista.interaction.BaristaSwipeRefreshInteractions.refresh
import junit.framework.AssertionFailedError
import org.hamcrest.CoreMatchers
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.Findable.StringId.TextId
import org.kiwix.kiwixmobile.Findable.Text
import org.kiwix.kiwixmobile.Findable.ViewId
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.utils.RecyclerViewItemCount
import org.kiwix.kiwixmobile.utils.RecyclerViewMatcher
import org.kiwix.kiwixmobile.utils.RecyclerViewSelectedCheckBoxCountAssertion
import org.kiwix.kiwixmobile.utils.StandardActions.openDrawer

fun zimHost(func: ZimHostRobot.() -> Unit) = ZimHostRobot().applyWithViewHierarchyPrinting(func)

class ZimHostRobot : BaseRobot() {

  fun assertMenuWifiHotspotDiplayed() {
    isVisible(TextId(R.string.menu_wifi_hotspot))
  }

  fun refreshLibraryList() {
    pauseForBetterTestPerformance()
    refresh(org.kiwix.kiwixmobile.R.id.zim_swiperefresh)
  }

  fun assertZimFilesLoaded() {
    pauseForBetterTestPerformance()
    isVisible(Text("Test_Zim"))
  }

  fun openZimHostFragment() {
    openDrawer()
    clickOn(TextId(R.string.menu_wifi_hotspot))
  }

  fun clickOnTestZim() {
    clickOn(Text("Test_Zim"))
  }

  fun startServer() {
    clickOn(ViewId(R.id.startServerButton))
    pauseForBetterTestPerformance()
    isVisible(TextId(R.string.wifi_dialog_title))
    clickOn(TextId(R.string.hotspot_dialog_neutral_button))
  }

  fun assertServerStarted() {
    pauseForBetterTestPerformance()
    isVisible(Text("STOP SERVER"))
  }

  fun stopServerIfAlreadyStarted() {
    try {
      assertServerStarted()
      stopServer()
    } catch (exception: Exception) {
      Log.i(
        "ZIM_HOST_FRAGMENT",
        "Failed to stop the server, Probably because server is not running"
      )
    }
  }

  fun selectZimFileIfNotAlreadySelected() {
    try {
      // check both files are selected.
      assertItemHostedOnServer(2)
    } catch (assertionFailedError: AssertionFailedError) {
      try {
        val recyclerViewItemsCount =
          RecyclerViewItemCount(R.id.recyclerViewZimHost).checkRecyclerViewCount()
        (0 until recyclerViewItemsCount)
          .asSequence()
          .filter { it != 0 }
          .forEach(::selectZimFile)
      } catch (assertionFailedError: AssertionFailedError) {
        Log.i("ZIM_HOST_FRAGMENT", "Failed to select the zim file, probably it is already selected")
      }
    }
  }

  private fun selectZimFile(position: Int) {
    pauseForBetterTestPerformance()
    try {
      onView(
        RecyclerViewMatcher(R.id.recyclerViewZimHost).atPositionOnView(
          position,
          R.id.itemBookCheckbox
        )
      ).check(matches(ViewMatchers.isChecked()))
    } catch (assertionError: AssertionFailedError) {
      pauseForBetterTestPerformance()
      onView(
        RecyclerViewMatcher(R.id.recyclerViewZimHost).atPositionOnView(
          position,
          R.id.itemBookCheckbox
        )
      ).perform(click())
    }
  }

  fun assertItemHostedOnServer(itemCount: Int) {
    val checkedCheckboxCount =
      RecyclerViewSelectedCheckBoxCountAssertion(
        R.id.recyclerViewZimHost,
        R.id.itemBookCheckbox
      ).countCheckedCheckboxes()
    assertThat(checkedCheckboxCount, CoreMatchers.`is`(itemCount))
  }

  fun stopServer() {
    clickOn(ViewId(R.id.startServerButton))
  }

  fun assertServerStopped() {
    pauseForBetterTestPerformance()
    isVisible(Text("START SERVER"))
  }

  private fun pauseForBetterTestPerformance() {
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
  }
}
