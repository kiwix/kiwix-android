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
