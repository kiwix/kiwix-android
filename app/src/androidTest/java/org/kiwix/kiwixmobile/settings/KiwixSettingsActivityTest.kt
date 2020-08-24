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
package org.kiwix.kiwixmobile.settings

import com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.core.R

class KiwixSettingsActivityTest : BaseActivityTest<KiwixSettingsActivity>() {

  override var activityRule = activityTestRule<KiwixSettingsActivity>()

  @Test fun testToggle() {
    settingsRobo {
      clickOn(R.string.pref_back_to_top)
      clickOn(R.string.pref_newtab_background_title)
      clickOn(R.string.pref_external_link_popup_title)
      clickOn(R.string.pref_wifi_only)
    }
  }

  @Test fun testLanguageDialog() {
    settingsRobo {
      clickOn(R.string.device_default)
      assertDisplayed(R.string.pref_language_title)
    }
  }

  @Test fun testStorageDialog() {
    settingsRobo {
      clickOn(R.string.internal_storage, R.string.external_storage)
      assertDisplayed(R.string.pref_storage)
    }
  }

  @Test fun testHistoryDialog() {
    settingsRobo {
      clickOn(R.string.pref_clear_all_history_title)
      assertDisplayed(R.string.clear_all_history_dialog_title)
    }
  }

  @Test fun testNightModeDialog() {
    settingsRobo {
      clickOn(R.string.pref_night_mode)
      for (nightModeString in nightModeStrings()) {
        assertDisplayed(nightModeString)
      }
    }
  }

  private fun nightModeStrings(): Array<String> {
    return activityRule.activity
      .resources
      .getStringArray(R.array.pref_night_modes_entries)
  }
}
