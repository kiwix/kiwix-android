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

package org.kiwix.kiwixmobile.core.main.note

import android.os.Build
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.input.TextFieldValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.sharedFunctions.TestApplication
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R], application = TestApplication::class)
class AddNoteDialogScreenTest {
  @get:Rule
  val composeRule = createComposeRule()

  private val snackbarHostState = SnackbarHostState()

  private fun setDialogScreen(
    articleTitle: String = "Android",
    noteTextFieldValue: TextFieldValue = TextFieldValue(""),
    actionMenuItems: List<ActionMenuItem> = emptyList(),
    onTextChange: (TextFieldValue) -> Unit = {},
    navigationIcon: @Composable () -> Unit = {}
  ) {
    composeRule.setContent {
      AddNoteDialogScreen(
        articleTitle = articleTitle,
        noteText = noteTextFieldValue,
        actionMenuItems = actionMenuItems,
        onTextChange = onTextChange,
        snackBarHostState = snackbarHostState,
        navigationIcon = navigationIcon
      )
    }
  }

  @Test
  fun articleTitleIsDisplayed() {
    setDialogScreen()
    composeRule
      .onNodeWithText("Android")
      .assertIsDisplayed()
  }

  @Test
  fun existingNoteTextIsDisplayed() {
    val text = "My Note"
    setDialogScreen(noteTextFieldValue = TextFieldValue(text))
    composeRule
      .onNodeWithTag(ADD_NOTE_TEXT_FILED_TESTING_TAG)
      .assertTextContains(text)
  }

  @Test
  fun placeholderIsDisplayedForEmptyNote() {
    setDialogScreen()
    composeRule
      .onNodeWithTag(NOTE_TEXT_PLACEHOLDER_TESTING_TAG, useUnmergedTree = true)
      .assertIsDisplayed()
  }

  @Test
  fun onTextChangeIsCalledWhenUserTypes() {
    val text = "Hello"
    var updatedValue: TextFieldValue? = null
    setDialogScreen(onTextChange = { updatedValue = it })
    composeRule
      .onNodeWithTag(ADD_NOTE_TEXT_FILED_TESTING_TAG)
      .performTextInput(text)

    assertThat(updatedValue?.text).isEqualTo(text)
  }

  @Test
  fun navigationIconIsDisplayed() {
    val text = "Back"
    setDialogScreen(navigationIcon = { Text(text) })
    composeRule
      .onNodeWithText(text)
      .assertIsDisplayed()
  }

  @Test
  fun saveMenuItemIsDisplayedAndClickable() {
    var clicked = false
    setDialogScreen(
      actionMenuItems = listOf(
        ActionMenuItem(
          contentDescription = R.string.save,
          testingTag = SAVE_MENU_BUTTON_TESTING_TAG,
          onClick = { clicked = true }
        )
      )
    )

    composeRule
      .onNodeWithTag(SAVE_MENU_BUTTON_TESTING_TAG).apply {
        assertExists()
        assertIsDisplayed()
        performClick()
      }
    assertThat(clicked).isTrue()
  }

  @Test
  fun allMenuItemsAreDisplayed() {
    val actionMenuItems = listOf(
      ActionMenuItem(
        contentDescription = R.string.save,
        testingTag = SAVE_MENU_BUTTON_TESTING_TAG,
        onClick = {}
      ),
      ActionMenuItem(
        contentDescription = R.string.share,
        testingTag = SHARE_MENU_BUTTON_TESTING_TAG,
        onClick = {}
      ),
      ActionMenuItem(
        contentDescription = R.string.delete,
        testingTag = DELETE_MENU_BUTTON_TESTING_TAG,
        onClick = {}
      )
    )
    setDialogScreen(actionMenuItems = actionMenuItems)

    composeRule.onNodeWithTag(SAVE_MENU_BUTTON_TESTING_TAG).assertExists()
    composeRule.onNodeWithTag(SHARE_MENU_BUTTON_TESTING_TAG).assertExists()
    composeRule.onNodeWithTag(DELETE_MENU_BUTTON_TESTING_TAG).assertExists()
  }

  @Test
  fun disabledMenuItemIsNotEnabled() {
    setDialogScreen(
      actionMenuItems = listOf(
        ActionMenuItem(
          contentDescription = R.string.save,
          testingTag = SAVE_MENU_BUTTON_TESTING_TAG,
          isEnabled = false,
          onClick = {}
        )
      )
    )
    composeRule
      .onNodeWithTag(SAVE_MENU_BUTTON_TESTING_TAG)
      .assertIsNotEnabled()
  }
}
