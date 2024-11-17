/*
 * Kiwix Android
 * Copyright (c) 2024 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.shortcuts

import android.app.Instrumentation
import android.content.Intent
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.accessibility.AccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.utils.LanguageUtils.Companion.handleLocaleChange
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.help.HelpRobot
import org.kiwix.kiwixmobile.localFileTransfer.LocalFileTransferRobot
import org.kiwix.kiwixmobile.main.ACTION_GET_CONTENT
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.main.topLevel
import org.kiwix.kiwixmobile.nav.destination.library.OnlineLibraryRobot
import org.kiwix.kiwixmobile.nav.destination.library.onlineLibrary
import org.kiwix.kiwixmobile.settings.SettingsRobot
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils.closeSystemDialogs
import org.kiwix.kiwixmobile.testutils.TestUtils.isSystemUINotRespondingDialogVisible
import org.kiwix.kiwixmobile.webserver.ZimHostRobot

@LargeTest
@RunWith(AndroidJUnit4::class)
class GetContentShortcutTest {

  @Rule
  @JvmField
  val retryRule = RetryRule()

  init {
    AccessibilityChecks.enable().setRunChecksFromRootView(true)
  }

  private val instrumentation: Instrumentation by lazy(InstrumentationRegistry::getInstrumentation)

  @Before
  fun setUp() {
    UiDevice.getInstance(instrumentation).apply {
      if (isSystemUINotRespondingDialogVisible(this)) {
        closeSystemDialogs(instrumentation.targetContext.applicationContext, this)
      }
      waitForIdle()
    }
    PreferenceManager.getDefaultSharedPreferences(
      instrumentation.targetContext.applicationContext
    ).edit {
      putBoolean(SharedPreferenceUtil.PREF_SHOW_INTRO, false)
      putBoolean(SharedPreferenceUtil.PREF_WIFI_ONLY, false)
      putBoolean(SharedPreferenceUtil.PREF_IS_TEST, true)
      putBoolean(SharedPreferenceUtil.PREF_EXTERNAL_LINK_POPUP, true)
      putBoolean(SharedPreferenceUtil.PREF_SHOW_SHOWCASE, false)
      putString(SharedPreferenceUtil.PREF_LANG, "en")
      putLong(
        SharedPreferenceUtil.PREF_LAST_DONATION_POPUP_SHOWN_IN_MILLISECONDS,
        System.currentTimeMillis()
      )
    }
    ActivityScenario.launch(KiwixMainActivity::class.java).apply {
      moveToState(Lifecycle.State.RESUMED)
      onActivity {
        handleLocaleChange(
          it,
          "en",
          SharedPreferenceUtil(instrumentation.targetContext.applicationContext)
        )
      }
    }
  }

  @Test
  fun testHandleGetContentShortcut() {
    val shortcutIntent = Intent(
      InstrumentationRegistry.getInstrumentation().targetContext,
      KiwixMainActivity::class.java
    ).apply {
      action = ACTION_GET_CONTENT
    }
    ActivityScenario.launch<KiwixMainActivity>(shortcutIntent)
    onlineLibrary(OnlineLibraryRobot::assertLibraryListDisplayed)
    topLevel {
      clickReaderOnBottomNav {
      }
      clickDownloadOnBottomNav(OnlineLibraryRobot::assertLibraryListDisplayed)
      clickLibraryOnBottomNav {
        assertGetZimNearbyDeviceDisplayed()
        clickFileTransferIcon(LocalFileTransferRobot::assertReceiveFileTitleVisible)
      }
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
  }
}
