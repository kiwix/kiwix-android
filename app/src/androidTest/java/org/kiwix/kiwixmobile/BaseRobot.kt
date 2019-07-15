package org.kiwix.kiwixmobile

import android.app.Instrumentation
import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.assertj.core.api.Assertions.assertThat

const val WAIT_TIMEOUT_MS = 5000L

abstract class BaseRobot(
  val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation(),
  val context: Context = instrumentation.targetContext,
  val uiDevice: UiDevice = UiDevice.getInstance(instrumentation)
) {

  protected fun isVisible(textId: Findable) {
    assertThat(waitFor(textId)).isNotNull()
  }

  protected fun waitFor(findable: Findable) = uiDevice.wait(Until.findObject(findable.selector(this)), WAIT_TIMEOUT_MS)
  protected fun UiObject2.swipeLeft() {
    customSwipe(LEFT)
  }

  protected fun UiObject2.swipeRight() {
    customSwipe(RIGHT)
  }

  private fun UiObject2.customSwipe(
    direction: Direction,
    fl: Float = 1.0f
  ) {
    this.swipe(direction, fl)
  }

}
