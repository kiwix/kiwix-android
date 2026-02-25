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

import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import applyWithViewHierarchyPrinting
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.ui.components.TOOLBAR_TITLE_TESTING_TAG
import org.kiwix.kiwixmobile.nav.destination.library.online.CATEGORY_MENU_ICON_TESTING_TAG
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.testutils.TestUtils.testFlakyView

fun category(func: OnlineCategoryRobot.() -> Unit) =
  OnlineCategoryRobot().applyWithViewHierarchyPrinting(func)

class OnlineCategoryRobot : BaseRobot() {
  fun clickOnCategoryMenuIcon(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitForIdle()
      onNodeWithTag(CATEGORY_MENU_ICON_TESTING_TAG).performClick()
    }
  }

  fun assertCategoryDialogDisplayed(composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.waitForIdle()
      composeTestRule.waitUntil(
        TestUtils.TEST_PAUSE_MS.toLong()
      ) {
        composeTestRule.onAllNodesWithTag(TOOLBAR_TITLE_TESTING_TAG)
          .filter(hasText(context.getString(R.string.select_category)))
          .onFirst()
          .isDisplayed()
      }
    })
  }

  fun selectCategory(
    composeTestRule: ComposeContentTestRule,
    matchLanguage: String
  ) {
    composeTestRule.onNodeWithTag("categoryItemRadioButtonTestingTag$matchLanguage")
      .performClick()
  }

  fun waitForCategoryToLoad(
    composeTestRule: ComposeContentTestRule,
    retryCountForDataToLoad: Int = 20
  ) {
    try {
      composeTestRule.waitUntil(TestUtils.TEST_PAUSE_MS_FOR_DOWNLOAD_TEST.toLong()) {
        composeTestRule
          .onAllNodesWithContentDescription(
            context.getString(R.string.select_category_content_description)
          )[0].isDisplayed()
      }
    } catch (e: ComposeTimeoutException) {
      if (retryCountForDataToLoad > 0) {
        waitForCategoryToLoad(
          retryCountForDataToLoad = retryCountForDataToLoad - 1,
          composeTestRule = composeTestRule
        )
        return
      }
      // throw the exception when there is no more retry left.
      throw RuntimeException("Couldn't load the category list.\n Original exception = $e")
    }
  }

  fun assertCategorySelected(composeTestRule: ComposeContentTestRule, matchLanguage: String) {
    composeTestRule.apply {
      waitForIdle()
      onNodeWithTag("categoryItemRadioButtonTestingTag$matchLanguage")
        .assertIsOn()
    }
  }

  fun clickOnSaveCategoryIcon(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitForIdle()
      onNodeWithTag(org.kiwix.kiwixmobile.language.SAVE_ICON_TESTING_TAG).performClick()
    }
  }
}
