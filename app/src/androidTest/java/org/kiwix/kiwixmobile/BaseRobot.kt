package org.kiwix.kiwixmobile

import android.app.Instrumentation
import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until

const val WAIT_TIMEOUT_MS = 5000L

abstract class BaseRobot(
  val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation(),
  val context: Context = instrumentation.targetContext,
  val uiDevice: UiDevice = UiDevice.getInstance(instrumentation)
) {

  protected fun isVisible(findable: Findable) =
    waitFor(findable) ?: throw RuntimeException(findable.errorMessage(this))

  protected fun waitFor(findable: Findable): UiObject2? =
    uiDevice.wait(Until.findObject(findable.selector(this)), WAIT_TIMEOUT_MS)

  protected fun UiObject2.swipeLeft() {
    customSwipe(Direction.LEFT)
  }

  protected fun UiObject2.swipeRight() {
    customSwipe(Direction.RIGHT)
  }

  private fun UiObject2.customSwipe(
    direction: Direction,
    fl: Float = 1.0f
  ) {
    this.swipe(direction, fl)
  }

  protected fun clickOn(findable: Findable) {
    isVisible(findable).click()
  }

}
