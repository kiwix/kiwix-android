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

package org.kiwix.kiwixmobile.intro

import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import applyWithViewHierarchyPrinting
import attempt
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.DEFAULT_WAIT
import org.kiwix.kiwixmobile.Findable.StringId.TextId
import org.kiwix.kiwixmobile.Findable.ViewId
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.main.TopLevelDestinationRobot
import org.kiwix.kiwixmobile.main.topLevel

fun intro(func: IntroRobot.() -> Unit) = IntroRobot().applyWithViewHierarchyPrinting(func)

class IntroRobot : BaseRobot() {
  init {
    waitTillLoad()
  }

  private val getStarted = ViewId(R.id.get_started)
  private val viewPager = ViewId(R.id.view_pager)

  fun swipeLeft() {
    isVisible(getStarted)
    isVisible(TextId(R.string.welcome_to_the_family))
    isVisible(TextId(R.string.humankind_knowledge))
    attempt(10) {
      isVisible(viewPager).swipeLeft()
      isVisible(TextId(R.string.save_books_offline))
      isVisible(TextId(R.string.download_books_message))
    }
  }

  infix fun clickGetStarted(func: TopLevelDestinationRobot.() -> Unit): TopLevelDestinationRobot {
    clickOn(getStarted)
    return topLevel(func)
  }

  override fun waitTillLoad() {
    uiDevice.waitForIdle()
    uiDevice.wait(Until.findObjects(By.res(getStarted.toString())), DEFAULT_WAIT)
    uiDevice.wait(Until.findObjects(By.res(viewPager.toString())), DEFAULT_WAIT)
  }
}
