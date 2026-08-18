/*
 * Kiwix Android
 * Copyright (c) 2025 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.onlineCategory

import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.filters.LargeTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.core.utils.TestingUtils.COMPOSE_TEST_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.TestingUtils.RETRY_RULE_ORDER
import org.kiwix.kiwixmobile.download.downloadRobot
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.ui.KiwixDestination

@LargeTest
class OnlineCategoryTest : BaseActivityTest() {
  @Rule(order = RETRY_RULE_ORDER)
  @JvmField
  val retryRule = RetryRule()

  @Rule(order = COMPOSE_TEST_RULE_ORDER)
  @JvmField
  val composeTestRule = createComposeRule()

  lateinit var kiwixMainActivity: KiwixMainActivity

  @Before
  override fun waitForIdle() {
    super.waitForIdle()
    updateKiwixDataStore { setIntroShown(false) }
    launchMainActivity { kiwixMainActivity = it }
    composeTestRule.enableAccessibilityChecks(createAccessibilityValidator())
  }

  @Test
  fun testOnlineCategory() {
    composeTestRule.runOnUiThread {
      kiwixMainActivity.navigate(KiwixDestination.Downloads.route)
    }
    downloadRobot {
      waitForDataToLoad(composeTestRule = composeTestRule)
    }
    category {
      clickOnCategoryMenuIcon(composeTestRule)
      assertCategoryDialogDisplayed(composeTestRule)
      waitForCategoryToLoad(composeTestRule)
      // Select the "All language" category to freshly run the test case.
      selectCategory(composeTestRule, "")
      clickOnSaveCategoryIcon(composeTestRule)
      // Select "Gutenberg" and "Wikipedia" categories.
      clickOnCategoryMenuIcon(composeTestRule)
      assertCategoryDialogDisplayed(composeTestRule)
      selectCategory(composeTestRule, "gutenberg")
      selectCategory(composeTestRule, "wikipedia")
      clickOnSaveCategoryIcon(composeTestRule)
      // assert categories are selected.
      clickOnCategoryMenuIcon(composeTestRule)
      assertCategoryDialogDisplayed(composeTestRule)
      assertCategorySelected(composeTestRule, "gutenberg")
      assertCategorySelected(composeTestRule, "wikipedia")
    }
  }
}
