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

import org.kiwix.kiwixmobile.core.utils.files.Log
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.espresso.matcher.ViewMatchers.withText
import applyWithViewHierarchyPrinting
import com.adevinta.android.barista.interaction.BaristaSwipeRefreshInteractions.refresh
import junit.framework.AssertionFailedError
import org.hamcrest.CoreMatchers
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.Findable.StringId.TextId
import org.kiwix.kiwixmobile.Findable.Text
import org.kiwix.kiwixmobile.Findable.ViewId
import org.kiwix.kiwixmobile.R
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
    refresh(R.id.zim_swiperefresh)
  }

  fun assertZimFilesLoaded() {
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
    // stop the server if it is already running.
    stopServerIfAlreadyStarted()
    clickOn(ViewId(R.id.startServerButton))
    assetWifiDialogDisplayed()
    onView(withText("PROCEED")).perform(click())
  }

  private fun assetWifiDialogDisplayed() {
    isVisible(Text("WiFi connection detected"))
  }

  fun assertServerStarted() {
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
    try {
      onView(
        RecyclerViewMatcher(R.id.recyclerViewZimHost).atPositionOnView(
          position,
          R.id.itemBookCheckbox
        )
      ).check(matches(ViewMatchers.isChecked()))
    } catch (assertionError: AssertionFailedError) {
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
    isVisible(Text("START SERVER"))
  }
}
