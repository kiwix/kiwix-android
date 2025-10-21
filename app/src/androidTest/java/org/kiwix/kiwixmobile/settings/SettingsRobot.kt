/*
 * Kiwix Android
 * Copyright (c) 2020 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.settings

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import applyWithViewHierarchyPrinting
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.settings.DIALOG_PREFERENCE_ITEM_TESTING_TAG
import org.kiwix.kiwixmobile.core.settings.PREFERENCE_ITEM_TESTING_TAG
import org.kiwix.kiwixmobile.core.settings.SEEKBAR_PREFERENCE_TESTING_TAG
import org.kiwix.kiwixmobile.core.settings.SETTINGS_LIST_TESTING_TAG
import org.kiwix.kiwixmobile.core.settings.SWITCH_PREFERENCE_TESTING_TAG
import org.kiwix.kiwixmobile.core.ui.components.STORAGE_DEVICE_ITEM_TESTING_TAG
import org.kiwix.kiwixmobile.core.ui.components.TOOLBAR_TITLE_TESTING_TAG
import org.kiwix.kiwixmobile.core.utils.dialog.ALERT_DIALOG_CONFIRM_BUTTON_TESTING_TAG
import org.kiwix.kiwixmobile.core.utils.dialog.ALERT_DIALOG_TITLE_TEXT_TESTING_TAG
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.testutils.TestUtils.testFlakyView
import org.kiwix.kiwixmobile.testutils.TestUtils.waitUntilTimeout

/**
 * Authored by Ayush Shrivastava on 25/8/20
 */

fun settingsRobo(func: SettingsRobot.() -> Unit) =
  SettingsRobot().applyWithViewHierarchyPrinting(func)

class SettingsRobot : BaseRobot() {
  fun assertMenuSettingsDisplayed(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitForIdle()
      onNodeWithTag(TOOLBAR_TITLE_TESTING_TAG)
        .assertTextEquals(context.getString(R.string.menu_settings))
    }
  }

  fun toggleBackToTopPref(composeTestRule: ComposeContentTestRule) {
    clickSwitchPreference(context.getString(R.string.pref_back_to_top), composeTestRule)
  }

  private fun clickSwitchPreference(title: String, composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitForIdle()
      composeTestRule.onNodeWithTag(SETTINGS_LIST_TESTING_TAG)
        .performScrollToNode(
          hasTestTag(SWITCH_PREFERENCE_TESTING_TAG) and hasContentDescription(title)
        )
      composeTestRule
        .onAllNodesWithTag(SWITCH_PREFERENCE_TESTING_TAG, true)
        .filter(hasContentDescription(title))
        .onFirst()
        .performScrollTo()
        .performClick()
    }
  }

  fun toggleOpenNewTabInBackground(composeTestRule: ComposeContentTestRule) {
    clickSwitchPreference(context.getString(R.string.pref_newtab_background_title), composeTestRule)
  }

  fun toggleExternalLinkWarningPref(composeTestRule: ComposeContentTestRule) {
    clickSwitchPreference(
      context.getString(R.string.pref_external_link_popup_title),
      composeTestRule
    )
  }

  fun toggleWifiDownloadsOnlyPref(composeTestRule: ComposeContentTestRule) {
    clickSwitchPreference(context.getString(R.string.pref_wifi_only), composeTestRule)
  }

  fun clickLanguagePreference(
    composeTestRule: ComposeContentTestRule,
    kiwixMainActivity: KiwixMainActivity
  ) {
    clickPreferenceItem(kiwixMainActivity.getString(R.string.pref_language_title), composeTestRule)
  }

  fun assertLanguagePrefDialogDisplayed(
    composeTestRule: ComposeContentTestRule,
    kiwixMainActivity: KiwixMainActivity
  ) {
    composeTestRule.apply {
      waitForIdle()
      onNodeWithTag(ALERT_DIALOG_TITLE_TEXT_TESTING_TAG)
        .assertTextEquals(kiwixMainActivity.getString(R.string.pref_language_title))
    }
  }

  fun clickInternalStoragePreference(composeTestRule: ComposeContentTestRule) {
    clickOnStorageItem(0, composeTestRule)
  }

  fun clickExternalStoragePreference(composeTestRule: ComposeContentTestRule) {
    clickOnStorageItem(1, composeTestRule)
  }

  private fun clickOnStorageItem(position: Int, composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitForIdle()
      onAllNodesWithTag(STORAGE_DEVICE_ITEM_TESTING_TAG, true)[position].performClick()
    }
  }

  fun clickClearHistoryPreference(composeTestRule: ComposeContentTestRule) {
    clickPreferenceItem(context.getString(R.string.pref_clear_all_history_title), composeTestRule)
  }

  fun assertHistoryDialogDisplayed(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitForIdle()
      onNodeWithTag(ALERT_DIALOG_TITLE_TEXT_TESTING_TAG)
        .assertTextEquals(context.getString(R.string.clear_all_history_dialog_title))
    }
  }

  fun clickClearNotesPreference(composeTestRule: ComposeContentTestRule) {
    clickPreferenceItem(context.getString(R.string.pref_clear_all_notes_title), composeTestRule)
  }

  fun assertNotesDialogDisplayed(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitForIdle()
      onNodeWithTag(ALERT_DIALOG_TITLE_TEXT_TESTING_TAG)
        .assertTextEquals(context.getString(R.string.delete_notes_confirmation_msg))
    }
  }

  fun clickExportBookmarkPreference(composeTestRule: ComposeContentTestRule) {
    clickPreferenceItem(context.getString(R.string.pref_export_bookmark_title), composeTestRule)
  }

  fun assertExportBookmarkDialogDisplayed(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitForIdle()
      onNodeWithTag(ALERT_DIALOG_TITLE_TEXT_TESTING_TAG)
        .assertTextEquals(context.getString(R.string.export_all_bookmarks_dialog_title))
    }
  }

  fun clickOnImportBookmarkPreference(composeTestRule: ComposeContentTestRule) {
    clickPreferenceItem(context.getString(R.string.pref_import_bookmark_title), composeTestRule)
  }

  fun assertImportBookmarkDialogDisplayed(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitForIdle()
      onNodeWithTag(ALERT_DIALOG_TITLE_TEXT_TESTING_TAG)
        .assertTextEquals(context.getString(R.string.import_bookmarks_dialog_title))
    }
  }

  fun clickNightModePreference(composeTestRule: ComposeContentTestRule) {
    clickPreferenceItem(context.getString(R.string.pref_theme), composeTestRule)
  }

  private fun clickPreferenceItem(title: String, composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitForIdle()
      composeTestRule.onNodeWithTag(SETTINGS_LIST_TESTING_TAG)
        .performScrollToNode(
          hasTestTag(PREFERENCE_ITEM_TESTING_TAG + title)
        )
      composeTestRule
        .onAllNodesWithTag(PREFERENCE_ITEM_TESTING_TAG + title, true)
        .onFirst()
        .performScrollTo()
        .performClick()
    }
  }

  fun assertNightModeDialogDisplayed(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitUntilTimeout()
      onNodeWithTag(ALERT_DIALOG_TITLE_TEXT_TESTING_TAG)
        .assertTextEquals(context.getString(R.string.pref_theme))
    }
  }

  fun clickCredits(composeTestRule: ComposeContentTestRule) {
    clickPreferenceItem(context.getString(R.string.pref_credits), composeTestRule)
  }

  fun assertContributorsDialogDisplayed(composeTestRule: ComposeContentTestRule) {
    // this is inside the dialog and dialog takes a bit to show on the screen.
    testFlakyView({
      composeTestRule.apply {
        waitForIdle()
        onNodeWithTag(ALERT_DIALOG_CONFIRM_BUTTON_TESTING_TAG)
          .assertTextEquals("OK")
      }
    })
  }

  fun assertZoomTextViewPresent(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitForIdle()
      composeTestRule.onNodeWithTag(SETTINGS_LIST_TESTING_TAG)
        .performScrollToNode(
          hasTestTag(SEEKBAR_PREFERENCE_TESTING_TAG) and hasContentDescription(context.getString(R.string.pref_text_zoom_title))
        )
      composeTestRule
        .onAllNodesWithTag(SEEKBAR_PREFERENCE_TESTING_TAG)
        .filter(hasContentDescription(context.getString(R.string.pref_text_zoom_title)))
        .onFirst()
        .performScrollTo()
        .performClick()
    }
  }

  fun assertVersionTextViewPresent(composeTestRule: ComposeContentTestRule) {
    clickPreferenceItem(context.getString(R.string.pref_info_version), composeTestRule)
  }

  fun selectAlbanianLanguage(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitForIdle()
      onAllNodesWithTag(DIALOG_PREFERENCE_ITEM_TESTING_TAG, true)[2].performClick()
    }
  }

  fun selectDeviceDefaultLanguage(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitForIdle()
      onAllNodesWithTag(DIALOG_PREFERENCE_ITEM_TESTING_TAG, true)[0].performClick()
    }
  }

  fun dismissDialog() {
    pressBack()
  }
}
