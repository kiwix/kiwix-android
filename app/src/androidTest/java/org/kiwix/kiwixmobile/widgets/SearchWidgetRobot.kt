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
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import applyWithViewHierarchyPrinting
import org.hamcrest.core.AllOf.allOf
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.testutils.TestUtils.testFlakyView

fun searchWidget(func: SearchWidgetRobot.() -> Unit) =
  SearchWidgetRobot().applyWithViewHierarchyPrinting(func)

class SearchWidgetRobot : BaseRobot() {

  fun removeWidgetIfAlreadyAdded(uiDevice: UiDevice) {
    try {
      val widget = uiDevice.findObject(By.text("Search Kiwix"))
      widget.click(1000L)
      uiDevice.waitForIdle()
      val center = getScreenCenter(uiDevice)
      val widgetBounds = widget.visibleBounds

      uiDevice.swipe(
        widgetBounds.centerX(),
        widgetBounds.centerY(),
        center.x,
        100,
        150
      )

      uiDevice.waitForIdle()
    } catch (ignore: Exception) {
      // nothing to do since widget is not added
    }
  }

  fun addWidgetToHomeScreen(uiDevice: UiDevice) {
    val center = getScreenCenter(uiDevice)
    longPressInCenterOfScreen(uiDevice, center)
    clickOnWidgetsText(uiDevice)
    var widget = uiDevice.findObject(By.text("Kiwix"))
    var maxSwipes = 30
    while (widget == null && maxSwipes > 0) {
      uiDevice.swipe(center.x, center.y, center.x, 0, 200)
      uiDevice.waitForIdle()
      widget = uiDevice.findObject(By.text("Kiwix"))
      maxSwipes--
    }
    uiDevice.swipe(center.x, center.y, center.x, 0, 200)
    val b = widget.visibleBounds
    val c = Point(b.left + 150, b.bottom + 150)
    val dest = Point(c.x + 250, c.y + 250)
    uiDevice.swipe(arrayOf(c, c, dest), 150)
  }

  private fun clickOnWidgetsText(uiDevice: UiDevice) {
    try {
      // Different according to the devices
      uiDevice.findObject(By.text("Widgets")).click()
    } catch (ignore: Exception) {
      uiDevice.findObject(By.text("WIDGETS")).click()
    }
    uiDevice.waitForIdle()
  }

  fun assertSearchWidgetAddedToHomeScreen(uiDevice: UiDevice) {
    uiDevice.findObject(By.text("Search Kiwix"))
  }

  private fun longPressInCenterOfScreen(uiDevice: UiDevice, center: Point) {
    uiDevice.swipe(arrayOf(center, center), 150)
  }

  private fun getScreenCenter(device: UiDevice): Point {
    val size = device.displaySizeDp
    return Point(size.x / 2, size.y / 2)
  }

  fun clickOnBookmarkIcon(uiDevice: UiDevice, kiwixMainActivity: KiwixMainActivity) {
    uiDevice.findObject(
      By.res("${kiwixMainActivity.packageName}:id/search_widget_star")
    ).click()
  }

  fun assertBookmarkScreenVisible() {
    testFlakyView({
      onView(allOf(withText(R.string.bookmarks), isDescendantOfA(withId(R.id.toolbar))))
        .check(matches(isDisplayed()))
    })
  }

  fun clickOnMicIcon(uiDevice: UiDevice, kiwixMainActivity: KiwixMainActivity) {
    uiDevice.findObject(
      By.res("${kiwixMainActivity.packageName}:id/search_widget_mic")
    ).click()
  }

  fun assertSearchScreenVisible() {
    testFlakyView({
      onView(withText(R.string.menu_search_in_text)).check(matches(isDisplayed()))
    })
  }

  fun clickOnSearchText(uiDevice: UiDevice, kiwixMainActivity: KiwixMainActivity) {
    uiDevice.findObject(
      By.res("${kiwixMainActivity.packageName}:id/search_widget_text")
    ).click()
  }
}
