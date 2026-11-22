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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.snack
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.utils.dialog.ALERT_DIALOG_CONFIRM_BUTTON_TESTING_TAG
import org.kiwix.kiwixmobile.core.utils.dialog.ALERT_DIALOG_DISMISS_BUTTON_TESTING_TAG
import org.kiwix.kiwixmobile.core.utils.dialog.DialogConfirmButton
import org.kiwix.kiwixmobile.core.utils.dialog.DialogDismissButton
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val MENU_ITEM_TESTING_TAG = "menuItemTestingTag"

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R], qualifiers = "tr")
class UppercaseLabelsTest {
  @Rule
  @JvmField
  val composeTestRule = createComposeRule()

  @Test
  fun kiwixButton_uppercasesTextWithAppLocale() {
    composeTestRule.setContent {
      KiwixButton(buttonText = "İndir", clickListener = {})
    }
    composeTestRule.onNodeWithText("İNDİR").assertExists()
  }

  @Test
  fun kiwixAppBar_uppercasesMenuItemTextWithAppLocale() {
    composeTestRule.setContent {
      KiwixAppBar(
        title = "",
        navigationIcon = {},
        actionMenuItems = listOf(
          ActionMenuItem(
            contentDescription = R.string.delete,
            onClick = {},
            iconButtonText = "Sil",
            testingTag = MENU_ITEM_TESTING_TAG
          )
        )
      )
    }
    composeTestRule.onNodeWithTag(MENU_ITEM_TESTING_TAG)
      .assertTextEquals("SİL")
  }

  @Test
  fun dialogButtons_uppercaseTextWithAppLocale() {
    composeTestRule.setContent {
      DialogConfirmButton(
        confirmButtonText = "İndir",
        dialogConfirmButtonClick = {},
        alertDialogShower = null
      )
      DialogDismissButton(
        dismissButtonTextId = R.string.delete,
        dismissButtonClick = {},
        alertDialogShower = null
      )
    }
    composeTestRule.onNodeWithTag(ALERT_DIALOG_CONFIRM_BUTTON_TESTING_TAG)
      .assertTextEquals("İNDİR")
    composeTestRule.onNodeWithTag(ALERT_DIALOG_DISMISS_BUTTON_TESTING_TAG)
      .assertTextEquals("SİL")
  }

  @Test
  fun kiwixSnackbarHost_uppercasesActionLabelWithAppLocale() {
    composeTestRule.setContent {
      val snackbarHostState = remember { SnackbarHostState() }
      KiwixSnackbarHost(snackbarHostState = snackbarHostState)
      LaunchedEffect(Unit) {
        snackbarHostState.snack(
          message = "Not silindi",
          actionLabel = "Geri al",
          snackbarDuration = SnackbarDuration.Indefinite,
          lifecycleScope = this
        )
      }
    }
    composeTestRule.onNodeWithText("GERİ AL").assertExists()
  }
}
