/*
 * Kiwix Android
 * Copyright (c) 2022 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.initial.download

import android.os.Build
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.filters.LargeTest
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import leakcanary.LeakAssertions
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.core.utils.TestingUtils.COMPOSE_TEST_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.TestingUtils.RETRY_RULE_ORDER
import org.kiwix.kiwixmobile.download.downloadRobot
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.nav.destination.library.library
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.ui.KiwixDestination

@LargeTest
class InitialDownloadTest : BaseActivityTest() {
  @Rule(order = RETRY_RULE_ORDER)
  @JvmField
  val retryRule = RetryRule()

  @Rule(order = COMPOSE_TEST_RULE_ORDER)
  @JvmField
  val composeTestRule = createComposeRule()
  private lateinit var kiwixMainActivity: KiwixMainActivity

  @Before
  override fun waitForIdle() {
    super.waitForIdle()
    updateKiwixDataStore {
      setShowStorageOption(true)
      setSelectedOnlineContentCategory("")
    }
    launchMainActivity()
    composeTestRule.enableAccessibilityChecks(createAccessibilityValidator())
  }

  @Test
  fun initialDownloadTest() {
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS_FOR_SEARCH_TEST.toLong())
    activityScenario.onActivity {
      kiwixMainActivity = it
      it.navigate(KiwixDestination.Library.route)
    }
    // delete all the ZIM files showing in the LocalLibrary
    // screen to properly test the scenario.
    library {
      refreshList(composeTestRule)
      waitUntilZimFilesRefreshing(composeTestRule)
      deleteZimIfExists(composeTestRule)
    }
    downloadRobot {
      clickDownloadOnBottomNav(composeTestRule)
      waitForDataToLoad(composeTestRule = composeTestRule)
      stopDownloadIfAlreadyStarted(composeTestRule, kiwixMainActivity)
      searchD3JsDocsFile(composeTestRule)
      downloadZimFile(composeTestRule)
    }
    initialDownload {
      assertStorageConfigureDialogDisplayed(composeTestRule)
      clickOnInternalStorage(composeTestRule)
      downloadRobot {
        assertDownloadStart(composeTestRule)
        stopDownload(composeTestRule)
        assertStopDownloadDialogDisplayed(composeTestRule, kiwixMainActivity)
        clickOnYesButton(composeTestRule)
      }
      assertDownloadStop(composeTestRule)
    }
    if (Build.VERSION.SDK_INT != Build.VERSION_CODES.TIRAMISU &&
      Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM
    ) {
      LeakAssertions.assertNoLeaks()
    }
  }

  @After
  fun finish() {
    TestUtils.deleteTemporaryFilesOfTestCases(context)
  }
}
