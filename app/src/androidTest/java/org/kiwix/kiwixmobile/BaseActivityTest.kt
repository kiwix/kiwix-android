/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.NEARBY_WIFI_DEVICES
import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.DuplicateClickableBoundsCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.SpeakableTextPresentCheck
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.anyOf
import org.junit.After
import org.junit.Rule
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.di.components.DaggerTestComponent
import org.kiwix.kiwixmobile.core.di.components.TestComponent
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.testutils.TestUtils

@RunWith(AndroidJUnit4::class)
abstract class BaseActivityTest {
  protected lateinit var activityScenario: ActivityScenario<KiwixMainActivity>
  protected val kiwixDataStore by lazy {
    KiwixDataStore(context)
  }

  open fun permissions(): Array<String> =
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
      arrayOf(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE, ACCESS_FINE_LOCATION)
    } else {
      arrayOf(POST_NOTIFICATIONS, NEARBY_WIFI_DEVICES)
    }

  @Rule
  @JvmField
  var permissionRules: GrantPermissionRule = GrantPermissionRule.grant(*permissions())

  val context: Context by lazy {
    getInstrumentation().targetContext.applicationContext
  }

  protected fun testComponent(): TestComponent = DaggerTestComponent.builder()
    .context(context)
    .build()

  /**
   * Contains common test setup logic that can be invoked from a subclass
   * `@Before` method.
   */
  open fun waitForIdle() {
    prepareDevice()
    setupCommonDataStore()
  }

  private fun prepareDevice() {
    UiDevice.getInstance(getInstrumentation()).apply {
      if (TestUtils.isSystemUINotRespondingDialogVisible(this)) {
        TestUtils.closeSystemDialogs(context, this)
      }
      waitForIdle()
    }
  }

  /**
   * Initializes the datastore with default values used by most instrumentation
   * tests. This ensures tests start from a consistent application state and do
   * not depend on values persisted from previous test runs.
   */
  private fun setupCommonDataStore() {
    kiwixDataStore.apply {
      runBlocking {
        setWifiOnly(false)
        setIntroShown()
        setPrefLanguage("en")
        setLastDonationPopupShownInMilliSeconds(System.currentTimeMillis())
        setShowStorageSelectionDialogOnCopyMove(false)
        setIsScanFileSystemDialogShown(true)
        setIsPlayStoreBuild(true)
        setPrefIsTest(true)
        setIsFirstRun(false)
      }
    }
  }

  /**
   * Updates the test [KiwixDataStore] by executing the provided block and waits
   * for all datastore operations to complete before returning.
   *
   * This allows tests to configure only the datastore values they need while
   * reusing the shared datastore instance.
   */
  protected fun updateKiwixDataStore(block: suspend KiwixDataStore.() -> Unit) {
    runBlocking { kiwixDataStore.block() }
  }

  /**
   * Launches [KiwixMainActivity] and sets the application's locale using the
   * provided language tag. Defaults to English ("en").
   */
  protected fun launchMainActivity(
    languageTag: String = "en",
    onActivityLaunched: ((KiwixMainActivity) -> Unit)? = null
  ) {
    activityScenario =
      ActivityScenario.launch(KiwixMainActivity::class.java).apply {
        moveToState(Lifecycle.State.RESUMED)
        onActivity {
          AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
          onActivityLaunched?.invoke(it)
        }
      }
  }

  /**
   * Creates and configures an [AccessibilityValidator] for Compose UI tests.
   *
   * Accessibility checks are run from the root view to validate the UI against
   * accessibility guidelines. Certain framework accessibility checks are
   * suppressed because they can produce false positives in our test environment:
   *
   * - [DuplicateClickableBoundsCheck]
   * - [SpeakableTextPresentCheck] (Android 14+ only)
   *
   * Sub classes can override this to provide the their own implementation.
   */
  protected open fun createAccessibilityValidator(): AccessibilityValidator {
    return AccessibilityValidator()
      .setRunChecksFromRootView(true)
      .apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
          setSuppressingResultMatcher(
            anyOf(
              matchesCheck(DuplicateClickableBoundsCheck::class.java),
              matchesCheck(SpeakableTextPresentCheck::class.java)
            )
          )
        } else {
          setSuppressingResultMatcher(anyOf(matchesCheck(DuplicateClickableBoundsCheck::class.java)))
        }
      }
  }

  @After
  fun tearDown() {
    if (::activityScenario.isInitialized) {
      runCatching {
        activityScenario.close()
      }
    }
  }
}
