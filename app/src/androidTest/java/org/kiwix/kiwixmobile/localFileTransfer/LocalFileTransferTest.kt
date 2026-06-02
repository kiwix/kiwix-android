/*
 * Kiwix Android
 * Copyright (c) 2023 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.localFileTransfer

import android.os.Build
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import leakcanary.LeakAssertions
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.utils.TestingUtils.COMPOSE_TEST_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.TestingUtils.RETRY_RULE_ORDER
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.nav.destination.library.library
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.ui.KiwixDestination
import org.kiwix.kiwixmobile.utils.StandardActions

class LocalFileTransferTest : BaseActivityTest() {
  @Rule(order = RETRY_RULE_ORDER)
  @JvmField
  val retryRule = RetryRule()

  @get:Rule(order = COMPOSE_TEST_RULE_ORDER)
  val composeTestRule = createComposeRule()

  lateinit var kiwixMainActivity: KiwixMainActivity

  @Before
  override fun waitForIdle() {
    super.waitForIdle()
    composeTestRule.enableAccessibilityChecks(createAccessibilityValidator())
  }

  @Test
  fun localFileTransfer() {
    shouldShowShowCaseFeatureToUser(false)
    launchMainActivity { kiwixMainActivity = it }
    StandardActions.closeDrawer(kiwixMainActivity as CoreMainActivity)
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
      activityScenario.onActivity {
        it.navigate(KiwixDestination.Library.route)
      }
      library {
        assertGetZimNearbyDeviceDisplayed(composeTestRule)
        clickFileTransferIcon(composeTestRule) {
          assertReceiveFileTitleVisible(composeTestRule)
          assertSearchDeviceMenuItemVisible(composeTestRule)
          clickOnSearchDeviceMenuItem(composeTestRule)
          assertLocalFileTransferScreenVisible(composeTestRule)
          pressBack()
          assertLocalLibraryVisible(composeTestRule)
        }
      }
      LeakAssertions.assertNoLeaks()
    }
  }

  @Test
  fun showCaseFeature() {
    shouldShowShowCaseFeatureToUser(true)
    launchMainActivity {
      kiwixMainActivity = it
      it.navigate(KiwixDestination.Library.route)
    }
    StandardActions.closeDrawer(kiwixMainActivity as CoreMainActivity)
    library {
      assertGetZimNearbyDeviceDisplayed(composeTestRule)
      clickFileTransferIcon(composeTestRule) {
        assertClickNearbyDeviceMessageVisible(composeTestRule)
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
        clickOnNextButton(composeTestRule)
        assertDeviceNameMessageVisible(composeTestRule)
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
        clickOnNextButton(composeTestRule)
        assertNearbyDeviceListMessageVisible(composeTestRule)
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
        clickOnNextButton(composeTestRule)
        assertTransferZimFilesListMessageVisible(composeTestRule)
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
        clickOnNextButton(composeTestRule)
        pressBack()
        assertGetZimNearbyDeviceDisplayed(composeTestRule)
      }
    }
    LeakAssertions.assertNoLeaks()
  }

  @Test
  fun testShowCaseFeatureShowOnce() {
    shouldShowShowCaseFeatureToUser(false)
    launchMainActivity {
      kiwixMainActivity = it
      it.navigate(KiwixDestination.Library.route)
    }
    StandardActions.closeDrawer(kiwixMainActivity as CoreMainActivity)
    library {
      // test show case view show once.
      clickFileTransferIcon(composeTestRule) {
        assertClickNearbyDeviceMessageNotVisible(composeTestRule)
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
      }
    }
  }

  private fun shouldShowShowCaseFeatureToUser(shouldShowShowCase: Boolean) {
    updateKiwixDataStore {
      setWifiOnly(false)
      setIntroShown()
      setShowCaseViewForFileTransferShown(shouldShowShowCase)
      setPrefLanguage("en")
      setIsScanFileSystemDialogShown(true)
      setIsFirstRun(false)
      setPrefIsTest(true)
    }
  }
}
