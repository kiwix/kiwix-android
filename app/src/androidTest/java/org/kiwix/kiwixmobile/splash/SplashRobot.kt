/*
 * Kiwix Android
 * Copyright (c) 2026 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.splash

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import applyWithViewHierarchyPrinting
import attempt
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.ui.components.ONE
import org.kiwix.kiwixmobile.core.ui.components.TWO
import org.kiwix.kiwixmobile.core.utils.ZERO
import org.kiwix.kiwixmobile.intro.HORIZONTAL_PAGER_TESTING_TAG
import org.kiwix.kiwixmobile.intro.composable.GET_STARTED_BUTTON_TESTING_TAG
import org.kiwix.kiwixmobile.intro.composable.INTRO_HEADING_TEXT_TESTING_TAG
import org.kiwix.kiwixmobile.intro.composable.INTRO_SUB_HEADING_TEXT_TESTING_TAG
import org.kiwix.kiwixmobile.main.TopLevelDestinationRobot
import org.kiwix.kiwixmobile.main.topLevel
import org.kiwix.kiwixmobile.nav.destination.library.local.THREE
import org.kiwix.kiwixmobile.testutils.TestUtils.testFlakyView

fun splash(func: SplashRobot.() -> Unit) = SplashRobot().applyWithViewHierarchyPrinting(func)

class SplashRobot : BaseRobot() {
  fun swipeLeft(composeTestRule: ComposeTestRule, isPlayStoreBuild: Boolean) {
    composeTestRule.apply {
      waitForIdle()
      assertIntroPage(
        ZERO,
        context.getString(string.welcome_to_the_family),
        context.getString(string.humankind_knowledge)
      )

      assertIntroPage(
        ONE,
        context.getString(string.save_books_offline),
        context.getString(string.download_books_message)
      )

      assertIntroPage(
        TWO,
        context.getString(string.save_books_in_desired_storage),
        context.getString(string.storage_location_hint)
      )

      if (!isPlayStoreBuild) {
        assertIntroPage(
          THREE,
          context.getString(R.string.auto_detect_books),
          context.getString(R.string.auto_detect_books_description)
        )
      }
      onNodeWithTag(GET_STARTED_BUTTON_TESTING_TAG)
        .assertTextEquals(context.getString(string.get_started).uppercase())
    }
  }

  private fun ComposeTestRule.assertIntroPage(
    page: Int,
    heading: String,
    subHeading: String
  ) {
    attempt(10) {
      scrollToPage(page, this)
      onNodeWithTag(INTRO_HEADING_TEXT_TESTING_TAG)
        .assertTextEquals(heading)

      onNodeWithTag(INTRO_SUB_HEADING_TEXT_TESTING_TAG)
        .assertTextEquals(subHeading)
    }

    onRoot().tryPerformAccessibilityChecks()
  }

  private fun scrollToPage(index: Int, composeTestRule: ComposeTestRule) {
    composeTestRule.apply {
      waitForIdle()
      onNodeWithTag(HORIZONTAL_PAGER_TESTING_TAG)
        .performScrollToIndex(index)
      waitForIdle()
    }
  }

  fun clickGetStarted(
    composeTestRule: ComposeTestRule,
    func: TopLevelDestinationRobot.() -> Unit
  ): TopLevelDestinationRobot {
    testFlakyView({
      composeTestRule.apply {
        waitForIdle()
        onNodeWithTag(GET_STARTED_BUTTON_TESTING_TAG)
          .performClick()
      }
    })
    return topLevel(func)
  }
}
