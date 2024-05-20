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

package org.kiwix.kiwixmobile.nav.destination

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.accessibility.AccessibilityChecks
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import leakcanary.LeakAssertions
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils

class PlayStoreRestrictionDialogTest {
  @Rule
  @JvmField
  var retryRule = RetryRule()

  private lateinit var sharedPreferenceUtil: SharedPreferenceUtil

  private lateinit var activityScenario: ActivityScenario<KiwixMainActivity>

  private val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    arrayOf(
      Manifest.permission.READ_EXTERNAL_STORAGE,
      Manifest.permission.WRITE_EXTERNAL_STORAGE,
      Manifest.permission.NEARBY_WIFI_DEVICES
    )
  } else {
    arrayOf(
      Manifest.permission.READ_EXTERNAL_STORAGE,
      Manifest.permission.WRITE_EXTERNAL_STORAGE,
      Manifest.permission.ACCESS_FINE_LOCATION
    )
  }

  @Rule
  @JvmField
  var permissionRules: GrantPermissionRule =
    GrantPermissionRule.grant(*permissions)
  private var context: Context? = null

  init {
    AccessibilityChecks.enable().setRunChecksFromRootView(true)
  }

  @Before
  fun waitForIdle() {
    context = InstrumentationRegistry.getInstrumentation().targetContext
    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).apply {
      if (TestUtils.isSystemUINotRespondingDialogVisible(this)) {
        TestUtils.closeSystemDialogs(context, this)
      }
      waitForIdle()
    }
  }

  @Test
  fun showPlayStoreRestrictionDialog() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      setShowPlayStoreRestrictionDialog(true)
      activityScenario = ActivityScenario.launch(KiwixMainActivity::class.java).apply {
        moveToState(Lifecycle.State.RESUMED)
      }
      playStoreRestriction {
        clickLibraryOnBottomNav()
        assertPlayStoreRestrictionDialogDisplayed()
        clickOnUnderstood()
      }
      LeakAssertions.assertNoLeaks()
    }
  }

  @Test
  fun testPlayStoreDialogShowOnlyOnce() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      setShowPlayStoreRestrictionDialog(false)
      activityScenario = ActivityScenario.launch(KiwixMainActivity::class.java).apply {
        moveToState(Lifecycle.State.RESUMED)
      }
      playStoreRestriction {
        clickLibraryOnBottomNav()
        assetPlayStoreRestrictionDialogNotDisplayed()
      }
      LeakAssertions.assertNoLeaks()
    }
  }

  private fun setShowPlayStoreRestrictionDialog(showDialog: Boolean) {
    context?.let {
      sharedPreferenceUtil = SharedPreferenceUtil(it).apply {
        setIntroShown()
        putPrefWifiOnly(false)
        setIsPlayStoreBuildType(true)
        prefIsTest = true
        playStoreRestrictionPermissionDialog = showDialog
        putPrefLanguage("en")
      }
    }
  }
}
