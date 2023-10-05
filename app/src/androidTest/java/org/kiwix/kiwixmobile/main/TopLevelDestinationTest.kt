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
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import leakcanary.LeakAssertions
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.help.HelpRobot
import org.kiwix.kiwixmobile.localFileTransfer.LocalFileTransferRobot
import org.kiwix.kiwixmobile.nav.destination.library.OnlineLibraryRobot
import org.kiwix.kiwixmobile.settings.SettingsRobot
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils.closeSystemDialogs
import org.kiwix.kiwixmobile.testutils.TestUtils.isSystemUINotRespondingDialogVisible
import org.kiwix.kiwixmobile.webserver.ZimHostRobot

class TopLevelDestinationTest : BaseActivityTest() {

  @Rule
  @JvmField
  var retryRule = RetryRule()

  @Before
  override fun waitForIdle() {
    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).apply {
      if (isSystemUINotRespondingDialogVisible(this)) {
        closeSystemDialogs(context)
      }
      waitForIdle()
    }
    PreferenceManager.getDefaultSharedPreferences(
      InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    ).edit {
      putBoolean(SharedPreferenceUtil.PREF_SHOW_INTRO, false)
      putBoolean(SharedPreferenceUtil.PREF_WIFI_ONLY, false)
      putBoolean(SharedPreferenceUtil.PREF_IS_TEST, true)
      putBoolean(SharedPreferenceUtil.PREF_EXTERNAL_LINK_POPUP, true)
    }
    activityScenario = ActivityScenario.launch(KiwixMainActivity::class.java).apply {
      moveToState(Lifecycle.State.RESUMED)
    }
  }

  @Test
  fun testTopLevelDestination() {
    topLevel {
      clickReaderOnBottomNav {
      }
      clickLibraryOnBottomNav {
        assertGetZimNearbyDeviceDisplayed()
        clickFileTransferIcon(LocalFileTransferRobot::assertReceiveFileTitleVisible)
      }
      clickDownloadOnBottomNav(OnlineLibraryRobot::assertLibraryListDisplayed)
      clickBookmarksOnNavDrawer {
        assertBookMarksDisplayed()
        clickOnTrashIcon()
        assertDeleteBookmarksDialogDisplayed()
      }
      clickHistoryOnSideNav {
        assertHistoryDisplayed()
        clickOnTrashIcon()
        assertDeleteHistoryDialogDisplayed()
      }
      clickHostBooksOnSideNav(ZimHostRobot::assertMenuWifiHotspotDiplayed)
      clickSettingsOnSideNav(SettingsRobot::assertMenuSettingsDisplayed)
      clickHelpOnSideNav(HelpRobot::assertToolbarDisplayed)
      clickSupportKiwixOnSideNav()
      assertExternalLinkDialogDisplayed()
      pressBack()
    }
    LeakAssertions.assertNoLeaks()
  }

  @After
  fun setIsTestPreference() {
    PreferenceManager.getDefaultSharedPreferences(context).edit {
      putBoolean(SharedPreferenceUtil.PREF_IS_TEST, false)
    }
  }
}
