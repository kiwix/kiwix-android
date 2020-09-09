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
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItem
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withText
import applyWithViewHierarchyPrinting
import com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import org.hamcrest.Matchers
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.Findable.Text
import org.kiwix.kiwixmobile.Findable.ViewId
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.R.array
import org.kiwix.kiwixmobile.core.R.string

/**
 * Authored by Ayush Shrivastava on 25/8/20
 */

fun settingsRobo(func: SettingsRobot.() -> Unit) =
  SettingsRobot().applyWithViewHierarchyPrinting(func)

class SettingsRobot : BaseRobot() {

  init {
    isVisible(ViewId(R.id.toolbar))
  }

  private fun clickRecyclerViewItems(@StringRes vararg stringIds: Int) {
    onView(
      withClassName(Matchers.`is`(RecyclerView::class.java.name))
    ).perform(
      actionOnItem<RecyclerView.ViewHolder>(
        hasDescendant(Matchers.anyOf(*stringIds.matchers())), ViewActions.click()
      )
    )
  }

  fun toggleBackToTopPref() {
    clickRecyclerViewItems(string.pref_back_to_top)
  }

  fun toggleOpenNewTabInBackground() {
    clickRecyclerViewItems(string.pref_newtab_background_title)
  }

  fun toggleExternalLinkWarningPref() {
    clickRecyclerViewItems(string.pref_external_link_popup_title)
  }

  fun toggleWifiDownloadsOnlyPref() {
    clickRecyclerViewItems(string.pref_wifi_only)
  }

  fun clickLanguagePreference() {
    clickRecyclerViewItems(string.device_default)
  }

  fun assertLanguagePrefDialogDisplayed() {
    assertDisplayed(string.pref_language_title)
  }

  fun clickStoragePreference() {
    clickRecyclerViewItems(string.internal_storage, string.external_storage)
  }

  fun assertStorageDialogDisplayed() {
    assertDisplayed(string.pref_storage)
  }

  fun clickClearHistoryPreference() {
    clickRecyclerViewItems(string.pref_clear_all_history_title)
  }

  fun assertHistoryDialogDisplayed() {
    assertDisplayed(string.clear_all_history_dialog_title)
  }

  fun clickNightModePreference() {
    clickRecyclerViewItems(string.pref_night_mode)
  }

  fun assertNightModeDialogDisplayed() {
    for (nightModeString in nightModeStrings()) {
      assertDisplayed(nightModeString)
    }
  }

  fun clickCredits() {
    clickRecyclerViewItems(string.pref_credits_title)
  }

  fun assertContributorsDialogDisplayed() {
    isVisible(Text("OK"))
  }

  fun assertZoomTextViewPresent() {
    clickRecyclerViewItems(string.pref_text_zoom_title)
  }

  fun assertVersionTextViewPresent() {
    clickRecyclerViewItems(string.pref_info_version)
  }

  fun dismissDialog() {
    pressBack()
  }

  private fun nightModeStrings(): Array<String> =
    context.resources.getStringArray(array.pref_night_modes_entries)

  private fun IntArray.matchers() = map(::withText).toTypedArray()
}
