/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile

import android.R.id
import android.app.Instrumentation
import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.kiwix.kiwixmobile.Findable.StringId.ContentDesc
import org.kiwix.kiwixmobile.Findable.Text
import org.kiwix.kiwixmobile.Findable.ViewId

const val WAIT_TIMEOUT_MS = 10_000L

abstract class BaseRobot(
  private val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation(),
  val context: Context = instrumentation.targetContext,
  val uiDevice: UiDevice = UiDevice.getInstance(instrumentation)
) {

  internal fun clickNegativeDialogButton() {
    clickOn(ViewId(id.button2))
  }

  internal fun clickPositiveDialogButton() {
    clickOn(ViewId(id.button1))
  }

  internal fun pressBack() {
    uiDevice.pressBack()
  }

  protected fun isVisible(findable: Findable, timeout: Long = WAIT_TIMEOUT_MS) =
    waitFor(findable, timeout) ?: throw RuntimeException(findable.errorMessage(this))

  protected fun UiObject2.swipeLeft() {
    customSwipe(Direction.LEFT)
  }

  protected fun UiObject2.swipeRight() {
    customSwipe(Direction.RIGHT)
  }

  protected fun clickOn(findable: Findable, timeout: Long = WAIT_TIMEOUT_MS) {
    isVisible(findable, timeout).click()
  }

  protected fun longClickOn(findable: Findable) {
    isVisible(findable).click(1000L)
  }

  protected fun clickOnTab(textId: Int) {
    clickOn(ContentDesc(textId), 20_000L)
  }

  protected fun waitFor(milliseconds: Long) {
    waitFor(Text("WILL_NEVER_EXIST"), milliseconds)
  }

  private fun waitFor(
    findable: Findable,
    timeout: Long = WAIT_TIMEOUT_MS
  ): UiObject2? =
    uiDevice.wait(Until.findObject(findable.selector(this)), timeout)

  private fun UiObject2.customSwipe(
    direction: Direction,
    percent: Float = 0.8f
  ) {
    swipe(direction, percent)
  }
}
