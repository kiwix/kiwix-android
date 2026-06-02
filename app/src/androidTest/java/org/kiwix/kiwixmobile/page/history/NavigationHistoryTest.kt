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

package org.kiwix.kiwixmobile.page.history

import android.os.Build
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.net.toUri
import androidx.navigation.NavOptions
import leakcanary.LeakAssertions
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.setNavigationResultOnCurrent
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.main.ZIM_FILE_URI_KEY
import org.kiwix.kiwixmobile.core.utils.TestingUtils.COMPOSE_TEST_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.TestingUtils.RETRY_RULE_ORDER
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.testutils.TestUtils.getZimFileFromResourceFolder
import org.kiwix.kiwixmobile.ui.KiwixDestination
import org.kiwix.kiwixmobile.utils.StandardActions

class NavigationHistoryTest : BaseActivityTest() {
  @Rule(order = RETRY_RULE_ORDER)
  @JvmField
  val retryRule = RetryRule()

  @get:Rule(order = COMPOSE_TEST_RULE_ORDER)
  val composeTestRule = createComposeRule()

  private lateinit var kiwixMainActivity: KiwixMainActivity

  @Before
  override fun waitForIdle() {
    super.waitForIdle()
    launchMainActivity { kiwixMainActivity = it }
    composeTestRule.enableAccessibilityChecks(createAccessibilityValidator())
  }

  @Test
  fun navigationHistoryDialogTest() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
      activityScenario.onActivity {
        kiwixMainActivity = it
      }
      composeTestRule.apply {
        waitForIdle()
        runOnUiThread {
          kiwixMainActivity.navigate(KiwixDestination.Library.route)
        }
      }
      val zimFile = getZimFileFromResourceFolder(context, "testzim.zim")
      composeTestRule.apply {
        waitForIdle()
        runOnUiThread {
          val navOptions = NavOptions.Builder()
            .setPopUpTo(KiwixDestination.Reader.route, false)
            .build()
          kiwixMainActivity.apply {
            kiwixMainActivity.navigate(KiwixDestination.Reader.route, navOptions)
            setNavigationResultOnCurrent(zimFile.toUri().toString(), ZIM_FILE_URI_KEY)
          }
        }
        waitForIdle()
      }
      StandardActions.closeDrawer(kiwixMainActivity as CoreMainActivity) // close the drawer if open before running the test cases.
      navigationHistory {
        closeTabSwitcherIfVisible(composeTestRule)
        checkZimFileLoadedSuccessful(composeTestRule)
        clickOnAndroidArticle(composeTestRule)
        longClickOnBackwardButton(composeTestRule)
        assertBackwardNavigationHistoryDialogDisplayed(composeTestRule)
        pressBack()
        clickOnBackwardButton(composeTestRule)
        longClickOnForwardButton(composeTestRule)
        assertForwardNavigationHistoryDialogDisplayed(composeTestRule)
        clickOnDeleteHistory(composeTestRule)
        assertDeleteDialogDisplayed(composeTestRule)
        clickOnCancelButton(composeTestRule)
      }
      if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
        // temporary disabled on Android 25
        LeakAssertions.assertNoLeaks()
      }
    }
  }

  @After
  fun finish() {
    TestUtils.deleteTemporaryFilesOfTestCases(context)
  }
}
