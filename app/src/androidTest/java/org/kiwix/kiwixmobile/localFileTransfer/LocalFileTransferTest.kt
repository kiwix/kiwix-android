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

import android.Manifest
import android.app.Instrumentation
import android.content.Context
import android.os.Build
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import leakcanary.LeakAssertions
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.utils.LanguageUtils.Companion.handleLocaleChange
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.nav.destination.library.library
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.utils.StandardActions

class LocalFileTransferTest {
  @Rule
  @JvmField
  var retryRule = RetryRule()

  private lateinit var context: Context
  private lateinit var activityScenario: ActivityScenario<KiwixMainActivity>

  private val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    arrayOf(
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

  private val instrumentation: Instrumentation by lazy {
    InstrumentationRegistry.getInstrumentation()
  }

  @Before
  fun setup() {
    context = instrumentation.targetContext.applicationContext
    UiDevice.getInstance(instrumentation).apply {
      if (TestUtils.isSystemUINotRespondingDialogVisible(this)) {
        TestUtils.closeSystemDialogs(context, this)
      }
      waitForIdle()
    }
  }

  @Test
  fun localFileTransfer() {
    shouldShowShowCaseFeatureToUser(false)
    activityScenario = ActivityScenario.launch(KiwixMainActivity::class.java).apply {
      moveToState(Lifecycle.State.RESUMED)
      onActivity {
        handleLocaleChange(
          it,
          "en",
          SharedPreferenceUtil(context)
        )
      }
    }
    StandardActions.closeDrawer()
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
      activityScenario.onActivity {
        it.navigate(R.id.libraryFragment)
      }
      library {
        assertGetZimNearbyDeviceDisplayed()
        clickFileTransferIcon {
          assertReceiveFileTitleVisible()
          assertSearchDeviceMenuItemVisible()
          clickOnSearchDeviceMenuItem()
          assertLocalFileTransferScreenVisible()
          pressBack()
          assertLocalLibraryVisible()
        }
      }
      LeakAssertions.assertNoLeaks()
    }
  }

  @Test
  fun showCaseFeature() {
    shouldShowShowCaseFeatureToUser(true, isResetShowCaseId = true)
    activityScenario = ActivityScenario.launch(KiwixMainActivity::class.java).apply {
      moveToState(Lifecycle.State.RESUMED)
      onActivity {
        handleLocaleChange(
          it,
          "en",
          SharedPreferenceUtil(context)
        )
        it.navigate(R.id.libraryFragment)
      }
    }
    StandardActions.closeDrawer()
    library {
      assertGetZimNearbyDeviceDisplayed()
      clickFileTransferIcon {
        assertClickNearbyDeviceMessageVisible()
        clickOnGotItButton()
        assertDeviceNameMessageVisible()
        clickOnGotItButton()
        assertNearbyDeviceListMessageVisible()
        clickOnGotItButton()
        assertTransferZimFilesListMessageVisible()
        clickOnGotItButton()
        pressBack()
        assertGetZimNearbyDeviceDisplayed()
      }
    }
    LeakAssertions.assertNoLeaks()
  }

  @Test
  fun testShowCaseFeatureShowOnce() {
    shouldShowShowCaseFeatureToUser(true)
    activityScenario = ActivityScenario.launch(KiwixMainActivity::class.java).apply {
      moveToState(Lifecycle.State.RESUMED)
      onActivity {
        it.navigate(R.id.libraryFragment)
      }
    }
    StandardActions.closeDrawer()
    library {
      // test show case view show once.
      clickFileTransferIcon(LocalFileTransferRobot::assertClickNearbyDeviceMessageNotVisible)
    }
  }

  private fun shouldShowShowCaseFeatureToUser(
    shouldShowShowCase: Boolean,
    isResetShowCaseId: Boolean = false
  ) {
    PreferenceManager.getDefaultSharedPreferences(context).edit {
      putBoolean(SharedPreferenceUtil.PREF_SHOW_INTRO, false)
      putBoolean(SharedPreferenceUtil.PREF_WIFI_ONLY, false)
      putBoolean(SharedPreferenceUtil.PREF_IS_TEST, true)
      putBoolean(SharedPreferenceUtil.PREF_SHOW_SHOWCASE, shouldShowShowCase)
      putBoolean(SharedPreferenceUtil.PREF_PLAY_STORE_RESTRICTION, false)
      putString(SharedPreferenceUtil.PREF_LANG, "en")
    }
    if (isResetShowCaseId) {
      // To clear showCaseID to ensure the showcase view will show.
      uk.co.deanwild.materialshowcaseview.PrefsManager.resetAll(context)
    } else {
      // set that Show Case is showed, because sometimes its change the
      // order of test case on API level 33 and our test case fails.
      val internal =
        context.getSharedPreferences("material_showcaseview_prefs", Context.MODE_PRIVATE)
      internal.edit().putInt("status_$SHOWCASE_ID", -1).apply()
    }
  }
}
