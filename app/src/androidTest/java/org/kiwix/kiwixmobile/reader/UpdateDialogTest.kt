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
import org.kiwix.kiwixmobile.BuildConfig
import org.kiwix.kiwixmobile.core.dao.DownloadApkDao
import org.kiwix.kiwixmobile.core.dao.entities.DownloadApkEntity
import org.kiwix.kiwixmobile.core.data.KiwixRoomDatabase
import org.kiwix.kiwixmobile.core.entity.ApkInfo
import org.kiwix.kiwixmobile.core.main.THREE_DAYS_IN_MILLISECONDS
import org.kiwix.kiwixmobile.core.utils.TestingUtils.COMPOSE_TEST_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.TestingUtils.RETRY_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils.closeSystemDialogs
import org.kiwix.kiwixmobile.testutils.TestUtils.isSystemUINotRespondingDialogVisible

class UpdateDialogTest : BaseActivityTest() {
  @Rule(order = RETRY_RULE_ORDER)
  @JvmField
  val retryRule = RetryRule()

  @get:Rule(order = COMPOSE_TEST_RULE_ORDER)
  val composeTestRule = createComposeRule()

  private lateinit var kiwixMainActivity: KiwixMainActivity
  private lateinit var kiwixDataStore: KiwixDataStore

  private lateinit var apkDao: DownloadApkDao

  @Before
  override fun waitForIdle() {
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
    val db = Room.databaseBuilder(
      context,
      KiwixRoomDatabase::class.java,
      "KiwixRoom.db"
    ).build()
    apkDao = db.downloadApkDao()
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
  fun showUpdatePopupWhenUpdateIsAvailableAndApplicationIsOlderThanThreeDays() {
    openReaderFragment()

    insertApkVersion("10.10.10")

    val olderThanThreeDays = System.currentTimeMillis() - (THREE_DAYS_IN_MILLISECONDS + 1000)

    runBlocking {
      setDialogState(
        lastDialog = olderThanThreeDays,
        laterClick = 0L
      )
    }

    update { assertUpdateDialogDisplayed(composeTestRule) }
  }

  @Test
  fun showUpdatePopupWhenUpdateIsAvailableAndLaterWasClickedMoreThanThreeDaysAgo() {
    openReaderFragment()

    insertApkVersion("10.10.10")

    val olderThanThreeDays = System.currentTimeMillis() - (THREE_DAYS_IN_MILLISECONDS + 1000)

    runBlocking {
      setDialogState(
        lastDialog = 0L,
        laterClick = olderThanThreeDays
      )
    }

    update { assertUpdateDialogDisplayed(composeTestRule) }
  }

  @Test
  fun doNotShowUpdatePopupWhenLaterClickedLessThanThreeDaysAgo() {
    openReaderFragment()

    insertApkVersion("10.10.10")

    val lessThanThreeDays = System.currentTimeMillis() - (THREE_DAYS_IN_MILLISECONDS + 1000)

    runBlocking {
      setDialogState(
        lastDialog = 0L,
        laterClick = lessThanThreeDays
      )
    }

    update { assertUpdateDialogIsNotDisplayed(composeTestRule) }
  }

  @Test
  fun doNotShowUpdatePopupWhenUpdateIsNotAvailable() {
    openReaderFragment()

    runBlocking {
      setDialogState(
        lastDialog = 0L,
        laterClick = 0L
      )
    }

    update { assertUpdateDialogIsNotDisplayed(composeTestRule) }
  }

  @Test
  fun doNotShowUpdatePopupWhenApkVersionIsLessThanApplicationVersion() {
    openReaderFragment()

    insertApkVersion("1.0.0")

    runBlocking {
      setDialogState(
        lastDialog = 0L,
        laterClick = 0L
      )
    }

    update { assertUpdateDialogIsNotDisplayed(composeTestRule) }
  }

  @Test
  fun doNotShowUpdatePopupWhenApkVersionIsEqualToApplicationVersion() {
    openReaderFragment()

    insertApkVersion(BuildConfig.VERSION_NAME)

    runBlocking {
      setDialogState(
        lastDialog = 0L,
        laterClick = 0L
      )
    }

    update { assertUpdateDialogIsNotDisplayed(composeTestRule) }
  }

  private fun insertApkVersion(version: String) {
    apkDao.addApkInfoItem(DownloadApkEntity(ApkInfo("", version, "")))
  }

  private suspend fun setDialogState(lastDialog: Long, laterClick: Long) {
    apkDao.addLaterClickedInfo(laterClick)
    apkDao.addLastDialogShownInfo(lastDialog)
  }

  private fun openReaderFragment() {
    activityScenario.onActivity {
      kiwixMainActivity = it
      kiwixMainActivity.navigate(kiwixMainActivity.readerFragmentRoute)
    }
  }
}
