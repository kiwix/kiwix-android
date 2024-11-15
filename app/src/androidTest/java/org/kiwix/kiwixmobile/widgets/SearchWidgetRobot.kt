/*
 * Kiwix Android
 * Copyright (c) 2024 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.widgets

import android.graphics.Point
import android.util.Log
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObjectNotFoundException
import applyWithViewHierarchyPrinting
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import org.hamcrest.core.AllOf.allOf
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.Findable.Text
import org.kiwix.kiwixmobile.SHORT_WAIT
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.testutils.TestUtils.TEST_PAUSE_MS_FOR_DOWNLOAD_TEST
import org.kiwix.kiwixmobile.testutils.TestUtils.testFlakyView

fun searchWidget(func: SearchWidgetRobot.() -> Unit) =
  SearchWidgetRobot().applyWithViewHierarchyPrinting(func)

class SearchWidgetRobot : BaseRobot() {

  fun removeWidgetIfAlreadyAdded(uiDevice: UiDevice) {
    try {
      val widget = uiDevice.findObject(By.text("Search Kiwix"))
      val removeTarget = Point(uiDevice.displayWidth / 2, uiDevice.displayHeight / 10)

      widget.drag(removeTarget)

      uiDevice.waitForIdle()
    } catch (ignore: Exception) {
      // nothing to do since widget is not added
      Log.e(
        "SEARCH_WIDGET_TEST",
        "Could not find the Search widget. It likely does not exist."
      )
    }
  }

  fun assertAddWidgetToHomeScreenVisible(): Boolean =
    try {
      isVisible(Text("Add automatically"))
      true
    } catch (ignore: Exception) {
      false
    }

  fun addWidgetToHomeScreenFromWidgetWindow() {
    testFlakyView({ clickOn(Text("Add automatically")) })
  }

  fun findSearchWidget(uiDevice: UiDevice) {
    try {
      assertSearchWidgetAddedToHomeScreen(2)
    } catch (ignore: RuntimeException) {
      // the search widget is not on the home screen, swipe right because
      // the widget has been added to next window
      swipeRightToOpenNextWindow(uiDevice)
    }
  }

  private fun swipeRightToOpenNextWindow(uiDevice: UiDevice) {
    val displayWidth = uiDevice.displayWidth
    val displayHeight = uiDevice.displayHeight

    val startX = (displayWidth * 0.9).toInt()
    val endX = (displayWidth * 0.1).toInt()
    val centerY = displayHeight / 2

    uiDevice.swipe(startX, centerY, endX, centerY, 20)
  }

  fun assertSearchWidgetAddedToHomeScreen(retryCount: Int = 5) {
    testFlakyView({ isVisible(Text("Search Kiwix"), SHORT_WAIT) }, retryCount)
  }

  fun clickOnMicIcon(
    uiDevice: UiDevice,
    kiwixMainActivity: KiwixMainActivity
  ) {
    clickOnElementById(uiDevice, kiwixMainActivity, "search_widget_mic")
  }

  fun closeIfGoogleSearchVisible() {
    try {
      pauseForBetterTestPerformance()
      testFlakyView({ isVisible(Text("Google")) })
      pressBack()
      Log.e("SEARCH_WIDGET_TEST", "Closed the Google speak dialog")
    } catch (ignore: Exception) {
      // do nothing since the Google speak is not recognized in this emulator.
      Log.e("SEARCH_WIDGET_TEST", "Could not close the Google speak dialog.")
    }
  }

  fun clickOnBookmarkIcon(
    uiDevice: UiDevice,
    kiwixMainActivity: KiwixMainActivity
  ) {
    clickOnElementById(uiDevice, kiwixMainActivity, "search_widget_star")
  }

  fun assertBookmarkScreenVisible() {
    testFlakyView({
      onView(allOf(withText(R.string.bookmarks), isDescendantOfA(withId(R.id.toolbar))))
        .check(matches(isDisplayed()))
    })
  }

  fun assertSearchScreenVisible() {
    testFlakyView({
      onView(withText(R.string.menu_search_in_text)).check(matches(isDisplayed()))
    })
  }

  fun clickOnSearchText(
    uiDevice: UiDevice,
    kiwixMainActivity: KiwixMainActivity
  ) {
    clickOnElementById(uiDevice, kiwixMainActivity, "search_widget_text")
  }

  private fun clickOnElementById(
    uiDevice: UiDevice,
    kiwixMainActivity: KiwixMainActivity,
    elementId: String,
    retryCount: Int = 20
  ) {
    var attempts = 0
    while (attempts < retryCount) {
      try {
        uiDevice.findObject(
          By.res("${kiwixMainActivity.packageName}:id/$elementId")
        ).click()
        return
      } catch (e: UiObjectNotFoundException) {
        attempts++
        Log.e("SEARCH_WIDGET_TEST", "Attempt $attempts: Failed to click on $elementId")
      }
    }
    throw RuntimeException("Could not find $elementId after $retryCount attempts")
  }

  private fun pauseForBetterTestPerformance() {
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS_FOR_DOWNLOAD_TEST.toLong())
  }
}
