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

package org.kiwix.kiwixmobile.main

import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers.withText
import applyWithViewHierarchyPrinting
import org.junit.Assert.assertEquals
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.main.KiwixWebView
import org.kiwix.kiwixmobile.testutils.TestUtils.testFlakyView
import org.kiwix.kiwixmobile.utils.StandardActions.enterSettings
import org.kiwix.kiwixmobile.utils.StandardActions.openDrawer

fun darkModeViewPainter(func: DarkModeViewPainterRobot.() -> Unit) =
  DarkModeViewPainterRobot().applyWithViewHierarchyPrinting(func)

class DarkModeViewPainterRobot : BaseRobot() {

  fun openSettings() {
    openDrawer()
    enterSettings()
  }

  fun enableTheDarkMode() {
    testFlakyView({
      onView(withText(R.string.on)).perform(ViewActions.click())
    })
  }

  fun enableTheLightMode() {
    testFlakyView({
      onView(withText(R.string.off)).perform(ViewActions.click())
    })
  }

  fun assertNightModeEnabled(kiwixWebView: KiwixWebView) {
    assertEquals(kiwixWebView.layerType, View.LAYER_TYPE_HARDWARE)
  }

  fun assertLightModeEnabled(kiwixWebView: KiwixWebView) {
    assertEquals(kiwixWebView.layerType, View.LAYER_TYPE_NONE)
  }
}
