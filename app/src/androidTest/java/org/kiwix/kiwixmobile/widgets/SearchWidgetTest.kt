/*
 * Kiwix Android
 * Copyright (c) 2024 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.accessibility.AccessibilityChecks
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesCheck
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesViews
import com.google.android.apps.common.testing.accessibility.framework.checks.TouchTargetSizeCheck
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.main.KiwixSearchWidget
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils

class SearchWidgetTest : BaseActivityTest() {

  @Rule
  @JvmField
  var retryRule = RetryRule()
  private lateinit var kiwixMainActivity: KiwixMainActivity
  private lateinit var uiDevice: UiDevice

  init {
    AccessibilityChecks.enable().apply {
      setRunChecksFromRootView(true)
      setSuppressingResultMatcher(
        allOf(
          matchesCheck(TouchTargetSizeCheck::class.java),
          matchesViews(withContentDescription("More options"))
        )
      )
    }
  }

  @Before
  override fun waitForIdle() {
    uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).apply {
      if (TestUtils.isSystemUINotRespondingDialogVisible(this)) {
        TestUtils.closeSystemDialogs(context, this)
      }
      waitForIdle()
    }
    context.let {
      SharedPreferenceUtil(it).apply {
        setIntroShown()
        putPrefWifiOnly(false)
        setIsPlayStoreBuildType(true)
        prefIsTest = true
        putPrefLanguage("en")
        lastDonationPopupShownInMilliSeconds = System.currentTimeMillis()
      }
    }
    activityScenario = ActivityScenario.launch(KiwixMainActivity::class.java).apply {
      moveToState(Lifecycle.State.RESUMED)
    }
  }

  @Test
  fun testSearchWidget() {
    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
      activityScenario.onActivity {
        kiwixMainActivity = it
      }
      searchWidget {
        pressHome()
        uiDevice.waitForIdle()
        if (addWidgetToHomeScreen()) {
          if (assertAddWidgetToHomeScreenVisible()) {
            addWidgetToHomeScreenFromWidgetWindow()
            assertSearchWidgetAddedToHomeScreen()
            clickOnBookmarkIcon(uiDevice, kiwixMainActivity)
            assertBookmarkScreenVisible()
            pressBack()
            pressHome()
            findSearchWidget(uiDevice)
            clickOnMicIcon(uiDevice, kiwixMainActivity)
            closeIfGoogleSearchVisible()
            assertSearchScreenVisible()
            pressBack()
            pressHome()
            findSearchWidget(uiDevice)
            clickOnSearchText(uiDevice, kiwixMainActivity)
            assertSearchScreenVisible()
            pressHome()
            removeWidgetIfAlreadyAdded(uiDevice)
          }
        }
      }
    }
  }

  private fun addWidgetToHomeScreen(): Boolean {
    val mAppWidgetManager: AppWidgetManager? =
      context.getSystemService(AppWidgetManager::class.java)
    val myProvider = ComponentName(context, KiwixSearchWidget::class.java)
    return if (mAppWidgetManager?.isRequestPinAppWidgetSupported == true) {
      val remoteViews =
        RemoteViews(context.packageName, R.layout.kiwix_search_widget)
      val bundle = Bundle()
      bundle.putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW, remoteViews)
      mAppWidgetManager.requestPinAppWidget(myProvider, bundle, null)
    } else {
      false
    }
  }
}
