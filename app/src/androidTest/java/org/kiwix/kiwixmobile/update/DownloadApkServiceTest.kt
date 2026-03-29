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

package org.kiwix.kiwixmobile.update

import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.IdlingRegistry
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.DuplicateClickableBoundsCheck
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import kotlinx.coroutines.launch
import leakcanary.LeakAssertions
import org.hamcrest.Matchers.anyOf
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.Assertions
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.core.dao.DownloadApkDao
import org.kiwix.kiwixmobile.core.dao.entities.DownloadApkEntity
import org.kiwix.kiwixmobile.core.data.KiwixRoomDatabase
import org.kiwix.kiwixmobile.core.downloader.downloadManager.DownloadApkService
import org.kiwix.kiwixmobile.core.entity.ApkInfo
import org.kiwix.kiwixmobile.core.utils.TestingUtils.COMPOSE_TEST_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.TestingUtils.RETRY_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils.closeSystemDialogs
import org.kiwix.kiwixmobile.testutils.TestUtils.isSystemUINotRespondingDialogVisible
import org.kiwix.kiwixmobile.testutils.TestUtils.waitUntilTimeout
import org.kiwix.kiwixmobile.ui.KiwixDestination
import org.kiwix.kiwixmobile.utils.KiwixIdlingResource.Companion.getInstance
import java.util.concurrent.TimeUnit

@LargeTest
class DownloadApkServiceTest : BaseActivityTest() {
  @Rule(order = RETRY_RULE_ORDER)
  @JvmField
  val retryRule = RetryRule()

  @get:Rule(order = COMPOSE_TEST_RULE_ORDER)
  val composeTestRule = createComposeRule()

  private lateinit var kiwixMainActivity: KiwixMainActivity
  private lateinit var kiwixDataStore: KiwixDataStore
  private lateinit var kiwixRoomDatabase: KiwixRoomDatabase
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
        setPrefIsTest(true)
        setIsPlayStoreBuild(false)
      }
    }
    kiwixRoomDatabase = Room.databaseBuilder(
      context,
      KiwixRoomDatabase::class.java,
      "KiwixRoom.db"
    ).build()
    apkDao = kiwixRoomDatabase.downloadApkDao()
    // assuming this link will always work, perhaps we can use a different link.
    apkDao.addApkInfoItem(
      DownloadApkEntity(
        ApkInfo(
          "Kiwix Apk",
          MAX_APP_VERSION,
          DOWNLOAD_APK_LINK
        )
      )
    )
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
  fun downloadApkServiceTest() {
    activityScenario.onActivity {
      kiwixMainActivity = it
      kiwixMainActivity.navigate(KiwixDestination.Reader.route)
    }
    updateScreenRobot {
      clickOnYes(composeTestRule)
      waitForApkInfoToLoad(composeTestRule)
      clickUpdateApk(composeTestRule)
      assertDownloadApkStart(composeTestRule)
    }
    assertApkDownloadService(true)
    updateScreenRobot {
      assertDownloadStoppedAfterCancel(composeTestRule, kiwixMainActivity)
    }
    assertApkDownloadService(false)
    if (Build.VERSION.SDK_INT != Build.VERSION_CODES.TIRAMISU &&
      Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM
    ) {
      LeakAssertions.assertNoLeaks()
    }
  }

  private fun assertApkDownloadService(isRunning: Boolean) {
    composeTestRule.waitUntilTimeout(3000)
    Assertions.assertEquals(
      isRunning,
      DownloadApkService.isDownloadApkServiceRunning
    )
    composeTestRule.waitUntilTimeout(3000)
  }

  companion object {
    @BeforeClass
    fun beforeClass() {
      IdlingPolicies.setMasterPolicyTimeout(180, TimeUnit.SECONDS)
      IdlingPolicies.setIdlingResourceTimeout(180, TimeUnit.SECONDS)
      IdlingRegistry.getInstance().register(getInstance())
    }
  }
}
