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

  @Test
  fun testToggling() {
    settingsRobo {
      toggleButtons()
    }
  }

  @Test
  fun testLanguageDialog() {
    settingsRobo {
      invokeLanguageDialog()
      assertDisplayed(R.string.pref_language_title)
    }
  }

  @Test
  fun testStorageDialog() {
    settingsRobo {
      invokeStorageDialog()
      assertDisplayed(R.string.pref_storage)
    }
  }

  @Test
  fun testHistoryDialog() {
    settingsRobo {
      invokeHistoryDeletionDialog()
      assertDisplayed(R.string.clear_all_history_dialog_title)
    }
  }

  @Test
  fun testNightModeDialog() {
    settingsRobo {
      invokeNightModeDialog()
      for (nightModeString in nightModeStrings()) {
        assertDisplayed(nightModeString)
      }
    }
  }

  private fun nightModeStrings(): Array<String> =
    activityRule.activity.resources.getStringArray(R.array.pref_night_modes_entries)
}
