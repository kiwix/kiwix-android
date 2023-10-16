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

import androidx.test.espresso.matcher.ViewMatchers.assertThat
import applyWithViewHierarchyPrinting
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import com.adevinta.android.barista.interaction.BaristaSwipeRefreshInteractions.refresh
import org.hamcrest.CoreMatchers
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.Findable.StringId.TextId
import org.kiwix.kiwixmobile.Findable.Text
import org.kiwix.kiwixmobile.Findable.ViewId
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.utils.RecyclerViewSelectedCheckBoxCountAssertion
import org.kiwix.kiwixmobile.utils.StandardActions.openDrawer

fun zimHost(func: ZimHostRobot.() -> Unit) = ZimHostRobot().applyWithViewHierarchyPrinting(func)

class ZimHostRobot : BaseRobot() {

  fun assertMenuWifiHotspotDiplayed() {
    isVisible(TextId(R.string.menu_wifi_hotspot))
  }

  fun refreshLibraryList() {
    pauseForBetterTestPerformance()
    refresh(R.id.zim_swiperefresh)
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
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS_FOR_SEARCH_TEST.toLong())
  }
}
