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
package org.kiwix.kiwixmobile.splash

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.filters.LargeTest
import leakcanary.LeakAssertions
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.core.utils.TestingUtils.COMPOSE_TEST_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.TestingUtils.RETRY_RULE_ORDER
import org.kiwix.kiwixmobile.intro.composable.GET_STARTED_BUTTON_TESTING_TAG
import org.kiwix.kiwixmobile.main.BOTTOM_NAV_READER_ITEM_TESTING_TAG
import org.kiwix.kiwixmobile.testutils.RetryRule

@LargeTest
class KiwixSplashActivityTest : BaseActivityTest() {
  @Rule(order = RETRY_RULE_ORDER)
  @JvmField
  val retryRule = RetryRule()

  @get:Rule(order = COMPOSE_TEST_RULE_ORDER)
  val composeTestRule = createComposeRule()

  @Before
  override fun waitForIdle() {
    super.waitForIdle()
    composeTestRule.enableAccessibilityChecks(createAccessibilityValidator())
  }

  @Test
  fun testFirstRun() {
    shouldShowIntro(true)
    launchMainActivity()
    splash {
      swipeLeft(composeTestRule)
      clickGetStarted(composeTestRule) {}
    }
    LeakAssertions.assertNoLeaks()
  }

  @Test
  fun testNormalRun() {
    shouldShowIntro(false)
    launchMainActivity()
    composeTestRule.apply {
      // Intro screen should not be displayed.
      onAllNodesWithTag(GET_STARTED_BUTTON_TESTING_TAG)
        .assertCountEquals(0)
      // Main screen should be displayed instead.
      onNodeWithTag(BOTTOM_NAV_READER_ITEM_TESTING_TAG).assertIsDisplayed()
    }
    LeakAssertions.assertNoLeaks()
  }

  private fun shouldShowIntro(value: Boolean) {
    updateKiwixDataStore {
      setIntroShown(value)
      setPrefIsTest(true)
    }
  }
}
