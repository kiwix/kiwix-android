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

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.internal.runner.junit4.statement.UiThreadStatement
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.DuplicateClickableBoundsCheck
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.anyOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.core.dao.DownloadApkDao
import org.kiwix.kiwixmobile.core.data.KiwixRoomDatabase
import org.kiwix.kiwixmobile.core.main.THREE_DAYS_IN_MILLISECONDS
import org.kiwix.kiwixmobile.core.utils.TestingUtils.COMPOSE_TEST_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.TestingUtils.RETRY_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils.closeSystemDialogs
import org.kiwix.kiwixmobile.testutils.TestUtils.isSystemUINotRespondingDialogVisible
import org.kiwix.kiwixmobile.ui.KiwixDestination

class UpdateDialogTest : BaseActivityTest() {
  @Rule(order = RETRY_RULE_ORDER)
  @JvmField
  val retryRule = RetryRule()

  @get:Rule(order = COMPOSE_TEST_RULE_ORDER)
  val composeTestRule = createComposeRule()
  private lateinit var kiwixRoomDatabase: KiwixRoomDatabase
  private lateinit var downloadApkDao: DownloadApkDao
  private lateinit var kiwixMainActivity: KiwixMainActivity
  private lateinit var kiwixDataStore: KiwixDataStore

  @Before
  override fun waitForIdle() {
    kiwixRoomDatabase = Room.inMemoryDatabaseBuilder(context, KiwixRoomDatabase::class.java).build()
    downloadApkDao = kiwixRoomDatabase.downloadApkDao()
    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).apply {
      if (isSystemUINotRespondingDialogVisible(this)) {
        closeSystemDialogs(context, this)
      }
      waitForIdle()
    }
    kiwixDataStore = KiwixDataStore(context).apply {
      lifeCycleScope.launch {
        setWifiOnly(false)
        setIntroShown()
        setPrefLanguage("en")
        setIsScanFileSystemDialogShown(true)
        setIsFirstRun(false)
        setIsPlayStoreBuild(false)
        setPrefIsTest(true)
      }
    }
    activityScenario =
      ActivityScenario.launch(KiwixMainActivity::class.java).apply {
        moveToState(Lifecycle.State.RESUMED)
        onActivity {
          AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
        }
      }
    val accessibilityValidator = AccessibilityValidator().setRunChecksFromRootView(true).apply {
      setSuppressingResultMatcher(
        anyOf(
          matchesCheck(DuplicateClickableBoundsCheck::class.java)
        )
      )
    }
    composeTestRule.enableAccessibilityChecks(accessibilityValidator)
  }

  @Test
  fun showUpdatePopupWhenApplicationIsThreeDaysOld() {
    runBlocking {
      downloadApkDao.addLastDialogShownInfo(0L)
      downloadApkDao.addLaterClickedInfo(0L)
    }
    openLocalLibraryScreen()
    openReaderFragment()
    update { assertUpdateDialogDisplayed(composeTestRule) }
  }

  @Test
  fun shouldNotShowUpdatePopupIfTimeSinceLastPopupIsLessThanThreeDays() {
    runBlocking {
      downloadApkDao.addLastDialogShownInfo(
        System.currentTimeMillis() - (THREE_DAYS_IN_MILLISECONDS / 2)
      )
    }
    openLocalLibraryScreen()
    openReaderFragment()
    update { assertUpdateDialogIsNotDisplayed(composeTestRule) }
  }

  private fun openLocalLibraryScreen() {
    activityScenario.onActivity {
      kiwixMainActivity = it
      kiwixMainActivity.navigate(KiwixDestination.Library.route)
    }
  }

  private fun openReaderFragment() {
    UiThreadStatement.runOnUiThread {
      kiwixMainActivity.navigate(kiwixMainActivity.readerFragmentRoute)
    }
  }
}
