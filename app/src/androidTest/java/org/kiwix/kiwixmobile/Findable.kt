package org.kiwix.kiwixmobile

import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector

sealed class Findable {
  abstract fun selector(baseRobot: BaseRobot): BySelector
  abstract fun errorMessage(baseRobot: BaseRobot): String

  class ViewId(@IdRes private val resId: Int) : Findable() {
    override fun errorMessage(baseRobot: BaseRobot) =
      "No view found with Id ${resourceName(baseRobot)}"

    override fun selector(baseRobot: BaseRobot): BySelector =
      By.res(resourceName(baseRobot))

    private fun resourceName(baseRobot: BaseRobot) =
      baseRobot.context.resources.getResourceName(resId)
  }

  class Text(private val text: String) : Findable() {
    override fun errorMessage(baseRobot: BaseRobot) = "No view found with text $text"

    override fun selector(baseRobot: BaseRobot): BySelector = By.text(text)
  }

  sealed class StringId(@StringRes private val resId: Int) : Findable() {

    fun text(baseRobot: BaseRobot): String = baseRobot.context.getString(resId)

    class ContentDesc(@StringRes resId: Int) : StringId(resId) {
      override fun selector(baseRobot: BaseRobot): BySelector = By.desc(text(baseRobot))

      override fun errorMessage(baseRobot: BaseRobot) =
        "No view found with content description ${text(baseRobot)}"
    }

    class TextContains(@StringRes resId: Int) : StringId(resId) {
      override fun selector(baseRobot: BaseRobot): BySelector = By.textContains(text(baseRobot))

      override fun errorMessage(baseRobot: BaseRobot) =
        "No view found containing ${text(baseRobot)}"
    }

    class TextId(@StringRes resId: Int) : StringId(resId) {
      override fun selector(baseRobot: BaseRobot): BySelector = By.text(text(baseRobot))

      override fun errorMessage(baseRobot: BaseRobot) = "No view found with text ${text(baseRobot)}"
    }
  }
}
