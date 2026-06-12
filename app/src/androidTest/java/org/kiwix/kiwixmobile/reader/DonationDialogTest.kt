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

package org.kiwix.kiwixmobile.reader

import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.internal.runner.junit4.statement.UiThreadStatement
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.core.utils.THREE_MONTHS_IN_MILLISECONDS
import org.kiwix.kiwixmobile.core.utils.TestingUtils.COMPOSE_TEST_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.TestingUtils.RETRY_RULE_ORDER
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.nav.destination.library.library
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.testutils.TestUtils.getZimFileFromResourceFolder
import org.kiwix.kiwixmobile.ui.KiwixDestination

class DonationDialogTest : BaseActivityTest() {
  @Rule(order = RETRY_RULE_ORDER)
  @JvmField
  val retryRule = RetryRule()

  @get:Rule(order = COMPOSE_TEST_RULE_ORDER)
  val composeTestRule = createComposeRule()

  private lateinit var kiwixMainActivity: KiwixMainActivity

  @Before
  override fun waitForIdle() {
    super.waitForIdle()
    launchMainActivity()
    composeTestRule.enableAccessibilityChecks(createAccessibilityValidator())
  }

  @Test
  fun showDonationPopupWhenApplicationIsThreeMonthOldAndHaveAtleastOneZIMFile() {
    loadZIMFileInApplication()
    updateKiwixDataStore {
      setLastDonationPopupShownInMilliSeconds(0L)
      setLaterClickedMilliSeconds(0L)
    }
    openReaderFragment()
    donation { assertDonationDialogDisplayed(composeTestRule) }
  }

  @Test
  fun shouldNotShowDonationPopupWhenApplicationIsThreeMonthOldAndDoNotHaveAnyZIMFile() {
    updateKiwixDataStore { setLastDonationPopupShownInMilliSeconds(0L) }
    activityScenario.onActivity {
      kiwixMainActivity = it
      kiwixMainActivity.navigate(KiwixDestination.Library.route)
    }
    deleteAllZIMFilesFromApplication()
    openReaderFragment()
    donation { assertDonationDialogIsNotDisplayed(composeTestRule) }
  }

  @Test
  fun shouldNotShowPopupIfTimeSinceLastPopupIsLessThanThreeMonth() {
    updateKiwixDataStore {
      setLastDonationPopupShownInMilliSeconds(
        System.currentTimeMillis() - (THREE_MONTHS_IN_MILLISECONDS / 2)
      )
    }
    loadZIMFileInApplication()
    openReaderFragment()
    donation { assertDonationDialogIsNotDisplayed(composeTestRule) }
  }

  @Test
  fun shouldShowDonationPopupIfTimeSinceLastPopupExceedsThreeMonths() {
    updateKiwixDataStore {
      setLastDonationPopupShownInMilliSeconds(
        System.currentTimeMillis() - (THREE_MONTHS_IN_MILLISECONDS + 1000)
      )
    }
    loadZIMFileInApplication()
    openReaderFragment()
    donation { assertDonationDialogDisplayed(composeTestRule) }
  }

  @Test
  fun testShouldShowDonationPopupWhenLaterClickedTimeExceedsThreeMonths() {
    updateKiwixDataStore {
      setLastDonationPopupShownInMilliSeconds(0L)
      setLaterClickedMilliSeconds(System.currentTimeMillis() - (THREE_MONTHS_IN_MILLISECONDS + 1000))
    }
    loadZIMFileInApplication()
    openReaderFragment()
    donation { assertDonationDialogDisplayed(composeTestRule) }
  }

  @Test
  fun testShouldNotShowPopupIfLaterClickedTimeIsLessThanThreeMonths() {
    updateKiwixDataStore {
      setLastDonationPopupShownInMilliSeconds(0L)
      setLaterClickedMilliSeconds(System.currentTimeMillis() - 10000L)
    }
    loadZIMFileInApplication()
    openReaderFragment()
    donation { assertDonationDialogIsNotDisplayed(composeTestRule) }
  }

  private fun openReaderFragment() {
    UiThreadStatement.runOnUiThread {
      kiwixMainActivity.navigate(kiwixMainActivity.readerScreenRoute)
    }
  }

  private fun loadZIMFileInApplication() {
    openLocalLibraryScreen()
    deleteAllZIMFilesFromApplication()
    getZimFileFromResourceFolder(context, "testzim.zim")
    refreshZIMFilesList()
  }

  private fun openLocalLibraryScreen() {
    activityScenario.onActivity {
      kiwixMainActivity = it
      kiwixMainActivity.navigate(KiwixDestination.Library.route)
    }
  }

  private fun refreshZIMFilesList() {
    library {
      refreshList(composeTestRule)
      waitUntilZimFilesRefreshing(composeTestRule)
    }
  }

  private fun deleteAllZIMFilesFromApplication() {
    refreshZIMFilesList()
    library {
      // delete all the ZIM files showing in the LocalLibrary
      // screen to properly test the scenario.
      deleteZimIfExists(composeTestRule)
    }
  }

  @After
  fun finish() {
    TestUtils.deleteTemporaryFilesOfTestCases(context)
  }
}
