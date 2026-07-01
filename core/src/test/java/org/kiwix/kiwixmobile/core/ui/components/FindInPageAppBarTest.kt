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

package org.kiwix.kiwixmobile.core.ui.components

import android.os.Build
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])
class FindInPageAppBarTest {
  @Rule
  @JvmField
  val composeTestRule = createComposeRule()

  private fun renderFindInPageAppbar(
    query: String = "Android",
    resultText: String = "1/5",
    onQueryChange: (String) -> Unit = {},
    onNextClick: () -> Unit = {},
    onPreviousClick: () -> Unit = {},
    onCloseClick: () -> Unit = {},
  ) {
    composeTestRule.setContent {
      FindInPageAppBar(
        query = query,
        resultText = resultText,
        onQueryChange = onQueryChange,
        onNextClick = onNextClick,
        onPreviousClick = onPreviousClick,
        onCloseClick = onCloseClick
      )
    }
  }

  @Test
  fun appBar_displaysAllViews() {
    renderFindInPageAppbar()
    composeTestRule.onNodeWithTag(FIND_IN_SEARCH_VIEW_TESTING_TAG)
      .assertExists()

    composeTestRule.onNodeWithTag(FIND_IN_PAGE_RESULT_TEXT)
      .assertTextEquals("1/5")

    composeTestRule.onNodeWithTag(FIND_IN_PAGE_PREVIOUS_BUTTON)
      .assertExists()

    composeTestRule.onNodeWithTag(FIND_IN_PAGE_NEXT_BUTTON)
      .assertExists()

    composeTestRule.onNodeWithTag(NAVIGATION_ICON_TESTING_TAG)
      .assertExists()
  }

  @Test
  fun typing_callsOnQueryChanged() {
    var query = ""
    renderFindInPageAppbar(query, onQueryChange = { query = it })
    composeTestRule
      .onNodeWithTag(FIND_IN_SEARCH_VIEW_TESTING_TAG)
      .performTextInput("Hello")
    composeTestRule.waitForIdle()
    assertThat(query).isEqualTo("Hello")
  }

  @Test
  fun clearButton_clearsQuery() {
    var query = "Android"
    renderFindInPageAppbar(query, onQueryChange = { query = it })
    composeTestRule
      .onNodeWithTag(FIND_IN_PAGE_SEARCH_CLEAR_BUTTON_TESTING_TAG)
      .performClick()

    assertThat(query).isEmpty()
  }

  @Test
  fun previousButton_callsCallback() {
    var clicked = false
    renderFindInPageAppbar(onPreviousClick = { clicked = true })
    composeTestRule
      .onNodeWithTag(FIND_IN_PAGE_PREVIOUS_BUTTON)
      .performClick()

    assertThat(clicked).isTrue()
  }

  @Test
  fun nextButton_callsCallback() {
    var clicked = false
    renderFindInPageAppbar(onNextClick = { clicked = true })
    composeTestRule
      .onNodeWithTag(FIND_IN_PAGE_NEXT_BUTTON)
      .performClick()

    assertThat(clicked).isTrue()
  }

  @Test
  fun closeButton_callsCallback() {
    var clicked = false
    renderFindInPageAppbar(onCloseClick = { clicked = true })
    composeTestRule
      .onNodeWithTag(NAVIGATION_ICON_TESTING_TAG)
      .performClick()

    assertThat(clicked).isTrue()
  }

  @Test
  fun resultText_isDisplayed() {
    renderFindInPageAppbar(resultText = "12/25")
    composeTestRule
      .onNodeWithTag(FIND_IN_PAGE_RESULT_TEXT)
      .assertTextEquals("12/25")
  }
}
