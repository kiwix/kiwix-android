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
package org.kiwix.kiwixmobile.main

import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.test.rule.ActivityTestRule
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil

class TopLevelDestinationTest : BaseActivityTest() {

  override var activityRule: ActivityTestRule<KiwixMainActivity> = activityTestRule {
    PreferenceManager.getDefaultSharedPreferences(context).edit {
      putBoolean(SharedPreferenceUtil.PREF_SHOW_INTRO, false)
      putBoolean(SharedPreferenceUtil.PREF_WIFI_ONLY, false)
    }
  }

  @Test
  fun testTopLevelDestination() {
    topLevel {
      clickReaderOnBottomNav {
      }
      clickLibraryOnBottomNav {
        clickFileTransferIcon {
        }
      }
      clickDownloadOnBottomNav {
        clickOnGlobeIcon {
        }
      }
      clickBookmarksOnNavDrawer {
        clickOnTrashIcon()
        assertDeleteBookmarksDialogDisplayed()
      }
      clickHistoryOnSideNav {
        clickOnTrashIcon()
        assertDeleteHistoryDialogDisplayed()
      }
      clickHostBooksOnSideNav {
      }
      clickSettingsOnSideNav {
      }
      clickHelpOnSideNav {
      }
      clickSupportKiwixOnSideNav()
      assertExternalLinkDialogDisplayed()
      pressBack()
    }
  }
}
