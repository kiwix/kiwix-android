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

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import applyWithViewHierarchyPrinting
import attempt
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.Findable.StringId.TextId
import org.kiwix.kiwixmobile.Findable.ViewId
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.main.TopLevelDestinationRobot
import org.kiwix.kiwixmobile.main.topLevel
import org.kiwix.kiwixmobile.testutils.TestUtils.testFlakyView

fun intro(func: IntroRobot.() -> Unit) = IntroRobot().applyWithViewHierarchyPrinting(func)

class IntroRobot : BaseRobot() {

  fun swipeLeft() {
    isVisible(ViewId(R.id.get_started))
    isVisible(TextId(string.welcome_to_the_family))
    isVisible(TextId(string.humankind_knowledge))
    attempt(10) {
      isVisible(TextId(string.save_books_offline))
      isVisible(TextId(string.download_books_message))
    }
    attempt(10) {
      isVisible(TextId(string.save_books_in_desired_storage))
      isVisible(TextId(string.storage_location_hint))
    }
  }

  infix fun clickGetStarted(func: TopLevelDestinationRobot.() -> Unit): TopLevelDestinationRobot {
    testFlakyView({ onView(withId(R.id.get_started)).perform(click()) })
    return topLevel(func)
  }
}
