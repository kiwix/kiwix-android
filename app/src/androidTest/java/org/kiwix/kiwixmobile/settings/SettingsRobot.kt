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

import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItem
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withResourceName
import androidx.test.espresso.matcher.ViewMatchers.withSubstring
import androidx.test.espresso.matcher.ViewMatchers.withText
import applyWithViewHierarchyPrinting
import org.hamcrest.Matchers
import org.hamcrest.Matchers.anything
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.Findable.StringId.TextId
import org.kiwix.kiwixmobile.Findable.Text
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.testutils.TestUtils.getResourceString
import org.kiwix.kiwixmobile.testutils.TestUtils.testFlakyView

/**
 * Authored by Ayush Shrivastava on 25/8/20
 */

fun settingsRobo(func: SettingsRobot.() -> Unit) =
  SettingsRobot().applyWithViewHierarchyPrinting(func)

class SettingsRobot : BaseRobot() {

  fun assertMenuSettingsDisplayed() {
    isVisible(TextId(R.string.menu_settings))
  }

  private fun clickRecyclerViewItems(@StringRes vararg stringIds: Int) {
    onView(
      withResourceName("recycler_view")
    ).perform(
      actionOnItem<RecyclerView.ViewHolder>(
        hasDescendant(Matchers.anyOf(*stringIds.matchers())), ViewActions.click()
      )
    )
  }

  private fun clickRecyclerViewItemsContainingText(@StringRes vararg stringIds: Int) {
    onView(
      withResourceName("recycler_view")
    ).perform(
      actionOnItem<RecyclerView.ViewHolder>(
        hasDescendant(Matchers.anyOf(*stringIds.subStringMatchers())), ViewActions.click()
      )
    )
  }

  fun toggleBackToTopPref() {
    clickRecyclerViewItems(R.string.pref_back_to_top)
  }

  fun toggleOpenNewTabInBackground() {
    clickRecyclerViewItems(R.string.pref_newtab_background_title)
  }

  fun toggleExternalLinkWarningPref() {
    clickRecyclerViewItems(R.string.pref_external_link_popup_title)
  }

  fun toggleWifiDownloadsOnlyPref() {
    clickRecyclerViewItems(R.string.pref_wifi_only)
  }

  fun clickLanguagePreference() {
    testFlakyView({
      onView(
        withResourceName("recycler_view")
      ).perform(
        actionOnItem<RecyclerView.ViewHolder>(
          hasDescendant(
            Matchers.anyOf(
              withText("shqip"),
              withText("English"),
              withText(R.string.device_default)
            )
          ),
          ViewActions.click()
        )
      )
    })
  }

  fun assertLanguagePrefDialogDisplayed() {
    testFlakyView({ onView(withText(R.string.pref_language_title)).check(matches(isDisplayed())) })
  }

  fun clickInternalStoragePreference() {
    clickRecyclerViewItemsContainingText(R.string.internal_storage)
  }

  fun clickExternalStoragePreference() {
    clickRecyclerViewItemsContainingText(R.string.external_storage)
  }

  fun clickClearHistoryPreference() {
    clickRecyclerViewItems(R.string.pref_clear_all_history_title)
  }

  fun assertHistoryDialogDisplayed() {
    isVisible(TextId(R.string.clear_all_history_dialog_title))
  }

  fun clickExportBookmarkPreference() {
    clickRecyclerViewItems(R.string.pref_export_bookmark_title)
  }

  fun assertExportBookmarkDialogDisplayed() {
    isVisible(TextId(R.string.export_all_bookmarks_dialog_title))
  }

  fun clickOnImportBookmarkPreference() {
    clickRecyclerViewItems(R.string.pref_import_bookmark_title)
  }

  fun assertImportBookmarkDialogDisplayed() {
    isVisible(TextId(R.string.import_bookmarks_dialog_title))
  }

  fun clickNightModePreference() {
    clickRecyclerViewItems(R.string.pref_dark_mode)
  }

  fun assertNightModeDialogDisplayed() {
    for (nightModeString in nightModeStrings()) {
      isVisible(Text(nightModeString))
    }
  }

  fun clickCredits() {
    clickRecyclerViewItems(R.string.pref_credits_title)
  }

  fun assertContributorsDialogDisplayed() {
    // this is inside the dialog and dialog takes a bit to show on the screen.
    testFlakyView({ isVisible(Text("OK")) })
  }

  fun assertZoomTextViewPresent() {
    clickRecyclerViewItems(R.string.pref_text_zoom_title)
  }

  fun assertVersionTextViewPresent() {
    clickRecyclerViewItems(R.string.pref_info_version)
  }

  fun selectAlbanianLanguage() {
    testFlakyView({
      onData(anything()).inAdapterView(withId(androidx.appcompat.R.id.select_dialog_listview))
        .atPosition(2)
        .perform(click())
    })
  }

  fun selectDeviceDefaultLanguage() {
    testFlakyView({
      onData(anything()).inAdapterView(withId(androidx.appcompat.R.id.select_dialog_listview))
        .atPosition(0)
        .perform(click())
    })
  }

  fun dismissDialog() {
    pressBack()
  }

  private fun nightModeStrings(): Array<String> =
    context.resources.getStringArray(R.array.pref_dark_modes_entries)

  private fun IntArray.matchers() = map(::withText).toTypedArray()
  private fun IntArray.subStringMatchers() = map {
    withSubstring(getResourceString(it))
  }.toTypedArray()
}
