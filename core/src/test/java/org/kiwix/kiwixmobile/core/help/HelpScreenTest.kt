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

package org.kiwix.kiwixmobile.core.help

import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.ui.components.TOOLBAR_TITLE_TESTING_TAG
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])
class HelpScreenTest {
  @get:Rule
  val composeTestRule = createComposeRule()

  private val context get() = RuntimeEnvironment.getApplication()

  private fun renderHelpScreen(
    data: MutableList<HelpScreenItemDataClass> = mutableListOf(),
    onSendReportButtonClick: () -> Unit = {}
  ) {
    composeTestRule.setContent {
      HelpScreen(
        data = data,
        onSendReportButtonClick = onSendReportButtonClick,
        navigationIcon = {}
      )
    }
  }

  @Test
  fun helpScreen_displaysCorrectTitle() {
    renderHelpScreen()
    composeTestRule
      .onNodeWithTag(TOOLBAR_TITLE_TESTING_TAG, useUnmergedTree = true)
      .assertTextEquals(context.getString(R.string.menu_help))
  }

  @Test
  fun helpScreen_displaysSendDiagnosticReport() {
    renderHelpScreen()
    composeTestRule
      .onNodeWithText(context.getString(R.string.send_report))
      .assertIsDisplayed()
  }

  @Test
  fun helpScreen_sendDiagnosticReportClick_triggersCallback() {
    var clicked = false
    renderHelpScreen(onSendReportButtonClick = { clicked = true })
    composeTestRule
      .onNodeWithTag(SEND_DIAGNOSTIC_REPORT_TESTING_TAG)
      .performClick()
    assertTrue("Send diagnostic report callback should be triggered", clicked)
  }

  @Test
  fun helpScreen_displaysHelpItems() {
    val helpItems = mutableListOf(
      HelpScreenItemDataClass("Title 1", "Description 1"),
      HelpScreenItemDataClass("Title 2", "Description 2")
    )
    renderHelpScreen(data = helpItems)
    composeTestRule
      .onNodeWithText("Title 1")
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithText("Title 2")
      .assertIsDisplayed()
  }

  @Test
  fun helpScreen_helpItemToggle_showsDescription() {
    val helpItems = mutableListOf(
      HelpScreenItemDataClass("Title 1", "Description 1")
    )
    renderHelpScreen(data = helpItems)

    // Description should be hidden initially (not in the tree or not displayed)
    composeTestRule
      .onNodeWithTag(HELP_SCREEN_ITEM_DESCRIPTION_TESTING_TAG)
      .assertDoesNotExist()

    // Click to expand
    composeTestRule
      .onNodeWithText("Title 1")
      .performClick()

    // Description should be displayed
    composeTestRule
      .onNodeWithTag(HELP_SCREEN_ITEM_DESCRIPTION_TESTING_TAG)
      .assertIsDisplayed()
  }

  @Test
  fun helpScreen_helpItemToggle_hidesDescription() {
    val helpItems = mutableListOf(
      HelpScreenItemDataClass("Title 1", "Description 1")
    )
    renderHelpScreen(data = helpItems)

    // Click to expand
    composeTestRule
      .onNodeWithText("Title 1")
      .performClick()

    // Verify it's displayed
    composeTestRule
      .onNodeWithTag(HELP_SCREEN_ITEM_DESCRIPTION_TESTING_TAG)
      .assertIsDisplayed()

    // Click to collapse
    composeTestRule
      .onNodeWithText("Title 1")
      .performClick()

    // Description should be hidden
    composeTestRule
      .onNodeWithTag(HELP_SCREEN_ITEM_DESCRIPTION_TESTING_TAG)
      .assertDoesNotExist()
  }
}
