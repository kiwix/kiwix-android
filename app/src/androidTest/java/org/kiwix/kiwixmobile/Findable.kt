package org.kiwix.kiwixmobile

import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector

interface Findable {
  fun selector(baseRobot: BaseRobot): BySelector

  class ViewId(val viewId: Int) : Findable {
    override fun selector(baseRobot: BaseRobot)=
      By.res(baseRobot.context.resources.getResourceName(viewId))
  }

  class TextId(val textId: Int) : Findable {
    override fun selector(baseRobot: BaseRobot) =
      By.text(baseRobot.context.getString(textId))
  }

}
