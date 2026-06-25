/*
 * Kiwix Android
 * Copyright (c) 2025 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.error

import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.core.app.ActivityScenario
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.core.utils.TestingUtils.COMPOSE_TEST_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.TestingUtils.RETRY_RULE_ORDER
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.nav.destination.library.library
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils.getZimFileFromResourceFolder
import org.kiwix.kiwixmobile.ui.KiwixDestination

class ErrorActivityTest : BaseActivityTest() {
  @Rule(order = RETRY_RULE_ORDER)
  @JvmField
  val retryRule = RetryRule()

  @Rule(order = COMPOSE_TEST_RULE_ORDER)
  @JvmField
  val composeTestRule = createComposeRule()

  @Before
  override fun waitForIdle() {
    super.waitForIdle()
    launchMainActivity()
    composeTestRule.enableAccessibilityChecks(createAccessibilityValidator())
  }

  @Test
  fun verifyErrorActivity() {
    activityScenario.onActivity {
      it.navigate(KiwixDestination.Library.route)
    }
    library {
      refreshList(composeTestRule)
      waitUntilZimFilesRefreshing(composeTestRule)
      deleteZimIfExists(composeTestRule)
    }
    getZimFileFromResourceFolder(context, "testzim.zim")
    library {
      refreshList(composeTestRule)
      waitUntilZimFilesRefreshing(composeTestRule)
      assertLibraryListDisplayed(composeTestRule)
    }
    activityScenario.onActivity {
      it.navigate(KiwixDestination.Help.route)
    }
    errorActivity {
      assertSendDiagnosticReportDisplayed(composeTestRule)
      clickOnSendDiagnosticReport(composeTestRule)
      assertErrorActivityDisplayed(composeTestRule)
      // Click on "No, Thanks" button to see it's functionality working or not.
      clickOnNoThanksButton(composeTestRule)
      // Handle the app restart explicitly. Since test case does not handle the app restart.
      activityScenario = ActivityScenario.launch(KiwixMainActivity::class.java).onActivity {
        it.navigate(KiwixDestination.Help.route)
      }
      // Assert HelpFragment is visible or not after clicking on the "No, Thanks" button.
      assertSendDiagnosticReportDisplayed(composeTestRule)
      // Again click on "Send diagnostic report" button to open the ErrorActivity.
      clickOnSendDiagnosticReport(composeTestRule)
      assertErrorActivityDisplayed(composeTestRule)
      // Check diagnostic details are displayed or not.
      assertDetailsIncludedInErrorReportDisplayed(composeTestRule)
      // Click on "Send details" button.
      clickOnSendDetailsButton(composeTestRule)
      // Assert ZIM file validation dialog displayed.
      assertZimFileValidationDialogDisplayed(composeTestRule)
    }
    composeTestRule.onRoot().tryPerformAccessibilityChecks()
  }
}
