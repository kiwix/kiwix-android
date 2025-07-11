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
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import applyWithViewHierarchyPrinting
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.Findable.ViewId
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.page.SEARCH_ICON_TESTING_TAG
import org.kiwix.kiwixmobile.core.search.SEARCH_FIELD_TESTING_TAG
import org.kiwix.kiwixmobile.nav.destination.library.online.LANGUAGE_MENU_ICON_TESTING_TAG
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.testutils.TestUtils.waitUntilTimeout

fun language(func: LanguageRobot.() -> Unit) = LanguageRobot().applyWithViewHierarchyPrinting(func)

class LanguageRobot : BaseRobot() {
  fun clickDownloadOnBottomNav() {
    clickOn(ViewId(R.id.downloadsFragment))
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
    retryCountForDataToLoad: Int = 20
  ) {
    try {
      composeTestRule.waitUntil(TestUtils.TEST_PAUSE_MS_FOR_DOWNLOAD_TEST.toLong()) {
        composeTestRule
          .onAllNodesWithContentDescription(
            context.getString(string.select_language_content_description)
          )[0].isDisplayed()
      }
    } catch (e: ComposeTimeoutException) {
      if (retryCountForDataToLoad > 0) {
        waitForLanguageToLoad(
          retryCountForDataToLoad = retryCountForDataToLoad - 1,
          composeTestRule = composeTestRule
        )
        return
      }
      // throw the exception when there is no more retry left.
      throw RuntimeException("Couldn't load the language list.\n Original exception = $e")
    }
  }
}
