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

import android.Manifest
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.utils.StandardActions

class KiwixSettingsFragmentTest {
  @Rule @JvmField var activityTestRule = ActivityTestRule(
    KiwixMainActivity::class.java
  )

  @Rule @JvmField var readPermissionRule: GrantPermissionRule =
    GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE)

  @Rule @JvmField var writePermissionRule: GrantPermissionRule =
    GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)

  @Before fun setup() {
    StandardActions.openDrawer()
    StandardActions.enterSettings()
  }

  @Test
  fun testSettingsActivity() {
    settingsRobo {
      assertZoomTextViewPresent()
      assertVersionTextViewPresent()
      clickLanguagePreference()
      assertLanguagePrefDialogDisplayed()
      dismissDialog()
      toggleBackToTopPref()
      toggleOpenNewTabInBackground()
      toggleExternalLinkWarningPref()
      toggleWifiDownloadsOnlyPref()
      clickStoragePreference()
      assertStorageDialogDisplayed()
      dismissDialog()
      clickClearHistoryPreference()
      assertHistoryDialogDisplayed()
      dismissDialog()
      clickNightModePreference()
      assertNightModeDialogDisplayed()
      dismissDialog()
      clickCredits()
      assertContributorsDialogDisplayed()
      dismissDialog()
    }
  }
}
