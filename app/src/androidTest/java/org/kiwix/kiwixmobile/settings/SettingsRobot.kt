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

import android.view.View
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItem
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.Findable.Text
import org.kiwix.kiwixmobile.core.R

/**
 * Authored by Ayush Shrivastava on 25/8/20
 */

fun settingsRobo(func: SettingsRobot.() -> Unit) =
  SettingsRobot().apply(func)

class SettingsRobot : BaseRobot() {

  private fun clickRecyclerViewItems(@StringRes vararg stringIds: Int) {
    val matchers: Array<Matcher<View>?> = arrayOfNulls(stringIds.size)
    for (i in stringIds.indices) {
      matchers[i] = withText(stringIds[i])
    }
    onView(
      withClassName(Matchers.`is`(RecyclerView::class.java.name))
    ).perform(
      actionOnItem<RecyclerView.ViewHolder>(
        hasDescendant(Matchers.anyOf(*matchers)), ViewActions.click()
      )
    )
  }

  fun toggleButtons() {
    clickRecyclerViewItems(R.string.pref_back_to_top)
    clickRecyclerViewItems(R.string.pref_newtab_background_title)
    clickRecyclerViewItems(R.string.pref_external_link_popup_title)
    clickRecyclerViewItems(R.string.pref_wifi_only)
  }

  fun invokeLanguageDialog() {
    clickRecyclerViewItems(R.string.device_default)
    assertDisplayed(R.string.pref_language_title)
  }

  fun invokeStorageDialog() {
    clickRecyclerViewItems(R.string.internal_storage, R.string.external_storage)
    assertDisplayed(R.string.pref_storage)
  }

  fun invokeHistoryDeletionDialog() {
    clickRecyclerViewItems(R.string.pref_clear_all_history_title)
    assertDisplayed(R.string.clear_all_history_dialog_title)
  }

  fun invokeNightModeDialog() {
    clickRecyclerViewItems(R.string.pref_night_mode)
    for (nightModeString in nightModeStrings()) {
      assertDisplayed(nightModeString)
    }
  }

  fun invokeContributorsDialog() {
    clickRecyclerViewItems(R.string.pref_credits_title)
    isVisible(Text("Contributors"))
  }

  fun checkRemainingTextViews() {
    clickRecyclerViewItems(R.string.pref_info_version)
    clickRecyclerViewItems(R.string.pref_text_zoom_title)
  }

  private fun nightModeStrings(): Array<String> =
    context.resources.getStringArray(R.array.pref_night_modes_entries)
}
