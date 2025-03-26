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

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.Findable.ViewId
import org.kiwix.kiwixmobile.core.R.id
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.help.HELP_SCREEN_ITEM_DESCRIPTION_TESTING_TAG
import org.kiwix.kiwixmobile.core.help.HELP_SCREEN_ITEM_TITLE_TESTING_TAG
import org.kiwix.kiwixmobile.testutils.TestUtils.testFlakyView

fun help(func: HelpRobot.() -> Unit) = HelpRobot().apply(func)

class HelpRobot : BaseRobot() {
  fun assertToolbarDisplayed() {
    isVisible(ViewId(id.toolbar))
  }

  fun clickOnWhatDoesKiwixDo(composeTestRule: ComposeContentTestRule) {
    clickOnHelpScreenItemTitle(0, composeTestRule)
  }

  fun assertWhatDoesKiwixDoIsExpanded(composeTestRule: ComposeContentTestRule) {
    assertHelpScreenDescriptionDisplayed(
      helpTextFormat(string.help_3, string.help_4),
      composeTestRule
    )
  }

  fun clickOnWhereIsContent(composeTestRule: ComposeContentTestRule) {
    clickOnHelpScreenItemTitle(1, composeTestRule)
  }

  fun assertWhereIsContentIsExpanded(composeTestRule: ComposeContentTestRule) {
    assertHelpScreenDescriptionDisplayed(
      helpTextFormat(
        string.help_6,
        string.help_7,
        string.help_8,
        string.help_9,
        string.help_10,
        string.help_11
      ),
      composeTestRule
    )
  }

  fun clickOnHowToUpdateContent(composeTestRule: ComposeContentTestRule) {
    clickOnHelpScreenItemTitle(2, composeTestRule)
  }

  fun assertHowToUpdateContentIsExpanded(composeTestRule: ComposeContentTestRule) {
    assertHelpScreenDescriptionDisplayed(
      context.getString(string.update_content_description),
      composeTestRule
    )
  }

  fun clickWhyCopyMoveFilesToAppPublicDirectory(composeTestRule: ComposeContentTestRule) {
    clickOnHelpScreenItemTitle(3, composeTestRule)
  }

  fun assertWhyCopyMoveFilesToAppPublicDirectoryIsExpanded(composeTestRule: ComposeContentTestRule) {
    assertHelpScreenDescriptionDisplayed(
      context.getString(string.copy_move_files_to_app_directory_description),
      composeTestRule
    )
  }

  fun assertWhyCopyMoveFilesToAppPublicDirectoryIsNotVisible(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitForIdle()
      val itemTitleList = onAllNodesWithTag(HELP_SCREEN_ITEM_TITLE_TESTING_TAG)
      val itemCount = itemTitleList.fetchSemanticsNodes().size
      repeat(itemCount) { index ->
        try {
          itemTitleList[index]
            .assertTextEquals(context.getString(string.why_copy_move_files_to_app_directory))
          // If "Why copy/move files to app public directory?" item is visible throw the error.
          throw RuntimeException("\"Why copy/move files to app public directory?\" help item is visible in non-playStore variant")
        } catch (_: AssertionError) {
          // If not found then nothing will do.
        }
      }
    }
    onView(withText(string.why_copy_move_files_to_app_directory))
      .check(doesNotExist())
  }

  private fun clickOnHelpScreenItemTitle(index: Int, composeTestRule: ComposeContentTestRule) {
    testFlakyView({
      composeTestRule.apply {
        waitForIdle()
        val itemTitleList = onAllNodesWithTag(HELP_SCREEN_ITEM_TITLE_TESTING_TAG)
        itemTitleList[index].performClick()
      }
    })
  }

  private fun assertHelpScreenDescriptionDisplayed(
    description: String,
    composeTestRule: ComposeContentTestRule
  ) {
    testFlakyView({
      composeTestRule.apply {
        waitForIdle()
        onNodeWithTag(HELP_SCREEN_ITEM_DESCRIPTION_TESTING_TAG)
          .assertContentDescriptionEquals(description)
      }
    })
  }

  private fun helpTextFormat(vararg stringIds: Int) =
    stringIds.joinToString(separator = "\n", transform = context::getString)
}
