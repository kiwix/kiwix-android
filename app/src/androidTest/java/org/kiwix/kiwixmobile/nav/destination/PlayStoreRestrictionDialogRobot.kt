/*
 * Kiwix Android
 * Copyright (c) 2023 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.nav.destination

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import applyWithViewHierarchyPrinting
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.Findable.ViewId
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.testutils.TestUtils

fun playStoreRestriction(func: PlayStoreRestrictionDialogRobot.() -> Unit) =
  PlayStoreRestrictionDialogRobot().applyWithViewHierarchyPrinting(func)

class PlayStoreRestrictionDialogRobot : BaseRobot() {

  fun clickLibraryOnBottomNav() {
    pauseForBetterTestPerformance()
    clickOn(ViewId(R.id.libraryFragment))
  }

  fun assertPlayStoreRestrictionDialogDisplayed() {
    pauseForBetterTestPerformance()
    onView(withText("UNDERSTOOD"))
      .check(matches(isDisplayed()))
  }

  fun assetPlayStoreRestrictionDialogNotDisplayed() {
    pauseForBetterTestPerformance()
    onView(withText("UNDERSTOOD"))
      .check(doesNotExist())
  }

  fun clickOnUnderstood() {
    pauseForBetterTestPerformance()
    onView(withText("UNDERSTOOD"))
      .perform(click())
  }

  private fun pauseForBetterTestPerformance() {
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
  }
}
