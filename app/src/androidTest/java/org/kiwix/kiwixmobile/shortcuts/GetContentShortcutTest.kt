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

import android.content.Intent
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.utils.TestingUtils.COMPOSE_TEST_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.TestingUtils.RETRY_RULE_ORDER
import org.kiwix.kiwixmobile.help.HelpRobot
import org.kiwix.kiwixmobile.main.ACTION_GET_CONTENT
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.main.topLevel
import org.kiwix.kiwixmobile.nav.destination.library.onlineLibrary
import org.kiwix.kiwixmobile.testutils.RetryRule

@LargeTest
class GetContentShortcutTest : BaseActivityTest() {
  @Rule(order = RETRY_RULE_ORDER)
  @JvmField
  val retryRule = RetryRule()

  @get:Rule(order = COMPOSE_TEST_RULE_ORDER)
  val composeTestRule = createComposeRule()
  lateinit var kiwixMainActivity: KiwixMainActivity

  @Before
  override fun waitForIdle() {
    super.waitForIdle()
    updateKiwixDataStore {
      setShowCaseViewForFileTransferShown()
      setExternalLinkPopup(true)
    }
    launchMainActivity { kiwixMainActivity = it }
    composeTestRule.enableAccessibilityChecks(createAccessibilityValidator())
  }

  @Test
  fun testHandleGetContentShortcut() {
    val shortcutIntent =
      Intent(
        InstrumentationRegistry.getInstrumentation().targetContext,
        KiwixMainActivity::class.java
      ).apply {
        action = ACTION_GET_CONTENT
      }
    ActivityScenario.launch<KiwixMainActivity>(shortcutIntent)
    composeTestRule.waitForIdle()
    onlineLibrary { assertOnlineLibraryScreenDisplayed(composeTestRule) }
    topLevel {
      clickReaderOnBottomNav(composeTestRule) {
        assertReaderScreenDisplayed(composeTestRule)
      }
      clickDownloadOnBottomNav(composeTestRule) {
        onlineLibrary { assertOnlineLibraryScreenDisplayed(composeTestRule) }
      }
      clickLibraryOnBottomNav(composeTestRule) {
        assertGetZimNearbyDeviceDisplayed(composeTestRule)
        clickFileTransferIcon(composeTestRule) {
          assertReceiveFileTitleVisible(composeTestRule)
        }
      }
      clickBookmarksOnNavDrawer(kiwixMainActivity as CoreMainActivity, composeTestRule) {
        assertBookMarksDisplayed(composeTestRule)
        clickOnTrashIcon(composeTestRule)
        assertDeleteBookmarksDialogDisplayed(composeTestRule)
      }
      clickHistoryOnSideNav(kiwixMainActivity as CoreMainActivity, composeTestRule) {
        assertHistoryDisplayed(composeTestRule)
        clickOnTrashIcon(composeTestRule)
        assertDeleteHistoryDialogDisplayed(composeTestRule)
      }
      clickHostBooksOnSideNav(kiwixMainActivity as CoreMainActivity, composeTestRule) {
        assertMenuWifiHotspotDisplayed(composeTestRule)
      }
      clickSettingsOnSideNav(kiwixMainActivity as CoreMainActivity, composeTestRule) {
        assertMenuSettingsDisplayed(composeTestRule)
      }
      clickHelpOnSideNav(kiwixMainActivity as CoreMainActivity, composeTestRule) {
        HelpRobot().assertToolbarDisplayed(composeTestRule)
      }
    }
  }
}
