package org.kiwix.kiwixmobile

import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector

interface Findable {
  fun selector(baseRobot: BaseRobot): BySelector
  fun errorMessage(baseRobot: BaseRobot): String

  class ViewId(val viewId: Int) : Findable {
    override fun errorMessage(baseRobot: BaseRobot) =
      "No view found with Id ${resourceName(baseRobot)}"

    override fun selector(baseRobot: BaseRobot)=
      By.res(resourceName(baseRobot))

    private fun resourceName(baseRobot: BaseRobot) =
      baseRobot.context.resources.getResourceName(viewId)
  }
  class TextId(val textId: Int) : Findable {
    override fun errorMessage(baseRobot: BaseRobot) = "No view found with text ${text(baseRobot)}"

    override fun selector(baseRobot: BaseRobot) =
      By.text(text(baseRobot))

    private fun text(baseRobot: BaseRobot) = baseRobot.context.getString(textId)
  }

  class TextContains(val textId: Int) : Findable {
    override fun errorMessage(baseRobot: BaseRobot) = "No view found containing ${text(baseRobot)}"

    override fun selector(baseRobot: BaseRobot) =
      By.textContains(text(baseRobot))

    fun text(baseRobot: BaseRobot) = baseRobot.context.getString(textId)
  }

  class Text(val text: String) : Findable {
    override fun errorMessage(baseRobot: BaseRobot) = "No view found with text $text"

    override fun selector(baseRobot: BaseRobot) = By.text(text)
  }
}
