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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withText
import applyWithViewHierarchyPrinting
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import junit.framework.AssertionFailedError
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.Findable.StringId.TextId
import org.kiwix.kiwixmobile.Findable.Text
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.utils.files.Log
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.testutils.TestUtils.refresh
import org.kiwix.kiwixmobile.testutils.TestUtils.testFlakyView
import org.kiwix.kiwixmobile.ui.BOOK_ITEM_CHECKBOX_TESTING_TAG
import org.kiwix.kiwixmobile.utils.StandardActions.openDrawer

fun zimHost(func: ZimHostRobot.() -> Unit) = ZimHostRobot().applyWithViewHierarchyPrinting(func)

class ZimHostRobot : BaseRobot() {
  fun assertMenuWifiHotspotDiplayed() {
    isVisible(TextId(R.string.menu_wifi_hotspot))
  }

  fun refreshLibraryList(composeTestRule: ComposeContentTestRule) {
    pauseForBetterTestPerformance()
    composeTestRule.runOnIdle {
      composeTestRule.refresh()
    }
  }

  fun assertZimFilesLoaded() {
    pauseForBetterTestPerformance()
    isVisible(Text("Test_Zim"))
  }

  fun openZimHostFragment() {
    openDrawer()
    clickOn(TextId(R.string.menu_wifi_hotspot))
  }

  fun clickOnTestZim(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.apply {
        waitForIdle()
        onNodeWithTag("${BOOK_ITEM_CHECKBOX_TESTING_TAG}2").performClick()
      }
    })
  }

  fun startServer(composeTestRule: ComposeContentTestRule) {
    // stop the server if it is already running.
    stopServerIfAlreadyStarted(composeTestRule)
    composeTestRule.onNodeWithTag(START_SERVER_BUTTON_TESTING_TAG)
      .performClick()
    assetWifiDialogDisplayed()
    testFlakyView({ onView(withText("PROCEED")).perform(click()) })
  }

  private fun assetWifiDialogDisplayed() {
    testFlakyView({ isVisible(Text("WiFi connection detected")) })
  }

  fun assertServerStarted(composeTestRule: ComposeContentTestRule) {
    pauseForBetterTestPerformance()
    // starting server takes a bit so sometimes it fails to find this view.
    // which makes this view flaky so we are testing this with FlakyView.
    testFlakyView({
      composeTestRule.apply {
        waitForIdle()
        onNodeWithTag(START_SERVER_BUTTON_TESTING_TAG)
          .assertTextEquals(context.getString(R.string.stop_server_label).uppercase())
      }
    })
  }

  fun stopServerIfAlreadyStarted(composeTestRule: ComposeContentTestRule) {
    try {
      // Check if the "START SERVER" button is visible because, in most scenarios,
      // this button will appear when the server is already stopped.
      // This will expedite our test case, as verifying the visibility of
      // non-visible views takes more time due to the try mechanism needed
      // to properly retrieve the view.
      assertServerStopped(composeTestRule)
    } catch (_: Exception) {
      // if "START SERVER" button is not visible it means server is started so close it.
      stopServer(composeTestRule)
      Log.i(
        "ZIM_HOST_FRAGMENT",
        "Stopped the server to perform our test case since it was already running"
      )
    }
  }

  fun selectZimFileIfNotAlreadySelected(composeTestRule: ComposeContentTestRule) {
    try {
      // check both files are selected.
      assertItemHostedOnServer(2, composeTestRule)
    } catch (_: AssertionFailedError) {
      try {
        selectZimFile(1, composeTestRule)
        selectZimFile(2, composeTestRule)
      } catch (_: AssertionFailedError) {
        Log.i("ZIM_HOST_FRAGMENT", "Failed to select the zim file, probably it is already selected")
      }
    }
  }

  private fun selectZimFile(position: Int, composeTestRule: ComposeContentTestRule) {
    try {
      composeTestRule.onNodeWithTag("$BOOK_ITEM_CHECKBOX_TESTING_TAG$position")
        .assertIsOn()
    } catch (_: AssertionFailedError) {
      composeTestRule.onNodeWithTag("$BOOK_ITEM_CHECKBOX_TESTING_TAG$position")
        .performClick()
    }
  }

  fun assertItemHostedOnServer(itemCount: Int, composeTestRule: ComposeContentTestRule) {
    for (i in 0 until itemCount) {
      composeTestRule.onNodeWithTag("$BOOK_ITEM_CHECKBOX_TESTING_TAG${i + 1}")
        .assertIsOn()
    }
  }

  fun stopServer(composeTestRule: ComposeContentTestRule) {
    testFlakyView(
      {
        composeTestRule.apply {
          waitForIdle()
          onNodeWithTag(START_SERVER_BUTTON_TESTING_TAG).performClick()
        }
      }
    )
  }

  fun assertServerStopped(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.apply {
        waitForIdle()
        onNodeWithTag(START_SERVER_BUTTON_TESTING_TAG)
          .assertTextEquals(context.getString(R.string.start_server_label).uppercase())
      }
    })
  }

  fun assertQrShown(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.apply {
        waitForIdle()
        onNodeWithTag(QR_IMAGE_TESTING_TAG)
          .assertIsDisplayed()
      }
    })
  }

  fun assertQrNotShown(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.apply {
        waitForIdle()
        onNodeWithTag(QR_IMAGE_TESTING_TAG)
          .assertIsNotDisplayed()
      }
    })
  }

  private fun pauseForBetterTestPerformance() {
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
  }
}
