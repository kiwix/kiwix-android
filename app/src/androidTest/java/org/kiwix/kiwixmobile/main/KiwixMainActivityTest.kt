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

import androidx.test.rule.ActivityTestRule
import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.utils.StandardActions.openDrawer

class KiwixMainActivityTest {
  @Rule
  @JvmField
  var activityTestRule = ActivityTestRule(KiwixMainActivity::class.java)

  @Test
  fun testKiwixMainActivity() {
    kiwixMainRobo {
      clickReaderOnBottomNav()
      assertReaderScreenDisplayed()
      clickLibraryOnBottomNav()
      assertLibraryScreenDisplayed()
      clickFileTransferIcon()
      assertFileTransferScreenDisplayed()
      pressBack()
      clickDownloadOnBottomNav()
      assertDownloadScreenDisplayed()
      clickOnGlobeIcon()
      assertLanguagesScreenDisplayed()
      pressBack()
      clickLibraryOnBottomNav()
      openDrawer()
      clickBookmarksOnNavDrawer()
      // assertBookmarksScreenDisplayed()
      waitFor(200) // This is necessary unless we are making an assertion as clicking on sideNav
      // and pressBack occur in no time which most of the times does not lead the expected Frag to open
      pressBack()
      openDrawer()
      clickHistoryOnSideNav()
      // assertHistoryScreenDisplayed()
      waitFor(200)
      pressBack()
      openDrawer()
      clickHostBooksOnSideNav()
      assertHostBooksScreenDisplayed()
      pressBack()
      openDrawer()
      clickSettingsOnSideNav()
      assertSettingsScreenDisplayed()
      pressBack()
      openDrawer()
      clickHelpOnSideNav()
      assertHelpScreenDisplayed()
      pressBack()
      openDrawer()
      clickSupportKiwixOnSideNav()
      assertExternalLinkDialogDisplayed()
      pressBack()
      pressBack()
    }
  }
}
