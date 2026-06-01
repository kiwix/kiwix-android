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

package org.kiwix.kiwixmobile.language

import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import applyWithViewHierarchyPrinting
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.page.SEARCH_ICON_TESTING_TAG
import org.kiwix.kiwixmobile.core.search.SEARCH_FIELD_TESTING_TAG
import org.kiwix.kiwixmobile.core.utils.files.Log
import org.kiwix.kiwixmobile.main.BOTTOM_NAV_DOWNLOADS_ITEM_TESTING_TAG
import org.kiwix.kiwixmobile.nav.destination.library.online.LANGUAGE_MENU_ICON_TESTING_TAG
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.testutils.TestUtils.waitUntilTimeout

fun language(func: LanguageRobot.() -> Unit) = LanguageRobot().applyWithViewHierarchyPrinting(func)

class LanguageRobot : BaseRobot() {
  fun clickDownloadOnBottomNav(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitUntilTimeout()
      onNodeWithTag(BOTTOM_NAV_DOWNLOADS_ITEM_TESTING_TAG).performClick()
    }
  }

  fun clickOnLanguageIcon(composeTestRule: ComposeContentTestRule) {
    // Wait for a few seconds to properly save selected language.
    composeTestRule.apply {
      waitUntilTimeout()
      onNodeWithTag(LANGUAGE_MENU_ICON_TESTING_TAG).performClick()
    }
  }

  fun clickOnSaveLanguageIcon(composeTestRule: ComposeContentTestRule) {
    composeTestRule.onNodeWithTag(SAVE_ICON_TESTING_TAG)
      .performClick()
  }

  fun clickOnLanguageSearchIcon(composeTestRule: ComposeContentTestRule) {
    composeTestRule.onNodeWithTag(SEARCH_ICON_TESTING_TAG).performClick()
  }

  fun searchLanguage(
    composeTestRule: ComposeContentTestRule,
    searchLanguage: String
  ) {
    val searchField = composeTestRule.onNodeWithTag(SEARCH_FIELD_TESTING_TAG)
    searchField.performTextInput(text = searchLanguage)
  }

  fun selectLanguage(
    composeTestRule: ComposeContentTestRule,
    matchLanguage: String
  ) {
    composeTestRule.onNodeWithText(matchLanguage)
      .performClick()
  }

  fun waitForLanguageToLoad(
    composeTestRule: ComposeContentTestRule,
    retryCount: Int = 20
  ) {
    repeat(retryCount) { attempt ->
      try {
        composeTestRule.waitUntil(TestUtils.TEST_PAUSE_MS_FOR_DOWNLOAD_TEST.toLong()) {
          composeTestRule
            .onAllNodesWithContentDescription(context.getString(string.select_language_content_description))
            .fetchSemanticsNodes()
            .isNotEmpty()
        }
        Log.d("LanguageTest", "Language list loaded")
        return
      } catch (_: ComposeTimeoutException) {
        Log.d(
          "LanguageTest",
          "Language list not loaded yet. Attempt ${attempt + 1}/$retryCount"
        )
      }
    }
    val nodeCount =
      composeTestRule
        .onAllNodesWithContentDescription(
          context.getString(string.select_language_content_description)
        )
        .fetchSemanticsNodes()
        .size
    // throw the exception when there is no more retry left.
    throw AssertionError(
      "Language list did not load after $retryCount attempts. " +
        "Found $nodeCount matching nodes."
    )
  }
}
