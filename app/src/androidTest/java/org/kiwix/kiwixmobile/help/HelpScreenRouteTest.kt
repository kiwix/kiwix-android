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
package org.kiwix.kiwixmobile.help

import android.os.Build
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.espresso.IdlingRegistry
import leakcanary.LeakAssertions
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.core.utils.TestingUtils.COMPOSE_TEST_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.TestingUtils.RETRY_RULE_ORDER
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.ui.KiwixDestination
import org.kiwix.kiwixmobile.utils.KiwixIdlingResource

class HelpScreenRouteTest : BaseActivityTest() {
  @Rule(order = RETRY_RULE_ORDER)
  @JvmField
  val retryRule = RetryRule()

  @Rule(order = COMPOSE_TEST_RULE_ORDER)
  @JvmField
  val composeTestRule = createComposeRule()

  @Before
  override fun waitForIdle() {
    super.waitForIdle()
    launchMainActivity()
    composeTestRule.enableAccessibilityChecks(createAccessibilityValidator())
  }

  @Test
  fun verifyHelpActivity() {
    setShowCopyMoveToPublicDirectory(false)
    activityScenario.onActivity {
      it.navigate(KiwixDestination.Help.route)
    }
    help {
      clickOnWhatDoesKiwixDo(composeTestRule)
      assertWhatDoesKiwixDoIsExpanded(composeTestRule)
      clickOnWhatDoesKiwixDo(composeTestRule)
      clickOnWhereIsContent(composeTestRule)
      assertWhereIsContentIsExpanded(composeTestRule)
      clickOnWhereIsContent(composeTestRule)
      clickOnHowToUpdateContent(composeTestRule)
      assertHowToUpdateContentIsExpanded(composeTestRule)
      clickOnHowToUpdateContent(composeTestRule)
      assertWhyCopyMoveFilesToAppPublicDirectoryIsNotVisible(composeTestRule)
    }
    composeTestRule.onRoot().tryPerformAccessibilityChecks()
    if (Build.VERSION.SDK_INT != Build.VERSION_CODES.TIRAMISU &&
      Build.VERSION.SDK_INT <= Build.VERSION_CODES.VANILLA_ICE_CREAM
    ) {
      LeakAssertions.assertNoLeaks()
    }
  }

  @Test
  fun verifyHelpActivityWithPlayStoreRestriction() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      setShowCopyMoveToPublicDirectory(true)
      activityScenario.onActivity {
        it.navigate(KiwixDestination.Help.route)
      }
      help {
        clickOnWhatDoesKiwixDo(composeTestRule)
        assertWhatDoesKiwixDoIsExpanded(composeTestRule)
        clickOnWhatDoesKiwixDo(composeTestRule)
        clickOnWhereIsContent(composeTestRule)
        assertWhereIsContentIsExpanded(composeTestRule)
        clickOnWhereIsContent(composeTestRule)
        clickOnHowToUpdateContent(composeTestRule)
        assertHowToUpdateContentIsExpanded(composeTestRule)
        clickOnHowToUpdateContent(composeTestRule)
        clickWhyCopyMoveFilesToAppPublicDirectory(composeTestRule)
        assertWhyCopyMoveFilesToAppPublicDirectoryIsExpanded(composeTestRule)
        clickWhyCopyMoveFilesToAppPublicDirectory(composeTestRule)
      }
      composeTestRule.onRoot().tryPerformAccessibilityChecks()
      if (Build.VERSION.SDK_INT != Build.VERSION_CODES.TIRAMISU &&
        Build.VERSION.SDK_INT <= Build.VERSION_CODES.VANILLA_ICE_CREAM
      ) {
        LeakAssertions.assertNoLeaks()
      }
    }
  }

  private fun setShowCopyMoveToPublicDirectory(showRestriction: Boolean) {
    updateKiwixDataStore {
      setWifiOnly(false)
      setIntroShown()
      setPrefLanguage("en")
      setIsPlayStoreBuild(showRestriction)
      setPrefIsTest(true)
    }
  }

  @After
  fun finish() {
    IdlingRegistry.getInstance().unregister(KiwixIdlingResource.getInstance())
  }
}
