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

package org.kiwix.kiwixmobile.main

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavOptions
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.accessibility.AccessibilityChecks
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesCheck
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesViews
import com.google.android.apps.common.testing.accessibility.framework.checks.SpeakableTextPresentCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.TouchTargetSizeCheck
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.anyOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.ui.components.NAVIGATION_ICON_TESTING_TAG
import org.kiwix.kiwixmobile.core.utils.LanguageUtils
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.TestingUtils.COMPOSE_TEST_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.TestingUtils.RETRY_RULE_ORDER
import org.kiwix.kiwixmobile.nav.destination.reader.KiwixReaderFragment
import org.kiwix.kiwixmobile.settings.settingsRobo
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.testutils.TestUtils.TEST_PAUSE_MS_FOR_DOWNLOAD_TEST
import org.kiwix.kiwixmobile.testutils.TestUtils.waitUntilTimeout
import org.kiwix.kiwixmobile.ui.KiwixDestination
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class DarkModeViewPainterTest : BaseActivityTest() {
  @Rule(order = RETRY_RULE_ORDER)
  @JvmField
  val retryRule = RetryRule()

  @get:Rule(order = COMPOSE_TEST_RULE_ORDER)
  val composeTestRule = createComposeRule()
  private lateinit var kiwixMainActivity: KiwixMainActivity

  @Before
  override fun waitForIdle() {
    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).apply {
      if (TestUtils.isSystemUINotRespondingDialogVisible(this)) {
        TestUtils.closeSystemDialogs(context, this)
      }
      waitForIdle()
    }
    PreferenceManager.getDefaultSharedPreferences(
      InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    ).edit {
      putBoolean(SharedPreferenceUtil.PREF_SHOW_INTRO, false)
      putBoolean(SharedPreferenceUtil.PREF_WIFI_ONLY, false)
      putBoolean(SharedPreferenceUtil.PREF_IS_TEST, true)
      putBoolean(SharedPreferenceUtil.PREF_EXTERNAL_LINK_POPUP, true)
      putBoolean(SharedPreferenceUtil.PREF_SHOW_SHOWCASE, false)
      putString(SharedPreferenceUtil.PREF_LANG, "en")
    }
    activityScenario =
      ActivityScenario.launch(KiwixMainActivity::class.java).apply {
        moveToState(Lifecycle.State.RESUMED)
        onActivity {
          kiwixMainActivity = it
          LanguageUtils.handleLocaleChange(
            it,
            "en",
            SharedPreferenceUtil(context)
          )
        }
      }
  }

  init {
    AccessibilityChecks.enable().apply {
      setRunChecksFromRootView(true)
      setSuppressingResultMatcher(
        anyOf(
          allOf(
            matchesCheck(TouchTargetSizeCheck::class.java),
            matchesViews(withContentDescription("More options"))
          ),
          matchesCheck(SpeakableTextPresentCheck::class.java)
        )
      )
    }
  }

  @Test
  fun testDarkMode() {
    composeTestRule.waitForIdle()
    toggleDarkMode(true)
    openZimFileInReader()
    verifyDarkMode(true)
    toggleDarkMode(false)
    openZimFileInReader()
    verifyDarkMode(false)
  }

  private fun openZimFileInReader() {
    activityScenario.onActivity {
      kiwixMainActivity = it
    }
    composeTestRule.apply {
      waitUntilTimeout()
      onNodeWithTag(NAVIGATION_ICON_TESTING_TAG).performClick()
      waitUntilTimeout()
      onNodeWithTag(BOTTOM_NAV_LIBRARY_ITEM_TESTING_TAG).performClick()
      waitUntilTimeout()
    }
    loadZimFileInReader()
  }

  private fun toggleDarkMode(enable: Boolean) {
    darkModeViewPainter { openSettings(kiwixMainActivity as CoreMainActivity, composeTestRule) }
    settingsRobo { clickNightModePreference(composeTestRule) }
    darkModeViewPainter {
      if (enable) {
        enableTheDarkMode(composeTestRule)
      } else {
        enableTheLightMode(composeTestRule)
      }
    }
  }

  private fun verifyDarkMode(isEnabled: Boolean) {
    var kiwixReaderFragment: KiwixReaderFragment? = null
    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(TEST_PAUSE_MS_FOR_DOWNLOAD_TEST.toLong()) {
      kiwixReaderFragment =
        kiwixMainActivity.supportFragmentManager.fragments
          .filterIsInstance<KiwixReaderFragment>()
          .firstOrNull()
      kiwixReaderFragment?.getCurrentWebView() != null
    }
    val currentWebView = kiwixReaderFragment?.getCurrentWebView()
    currentWebView?.let {
      darkModeViewPainter {
        if (isEnabled) {
          assertNightModeEnabled(it)
        } else {
          assertLightModeEnabled(it)
        }
      }
    } ?: run {
      throw RuntimeException(
        "Could not check the dark mode enable or not because zim file is not loaded in the reader"
      )
    }
  }

  private fun loadZimFileInReader() {
    val loadFileStream =
      DarkModeViewPainterTest::class.java.classLoader.getResourceAsStream("testzim.zim")
    val zimFile =
      File(
        context.getExternalFilesDirs(null)[0],
        "testzim.zim"
      )
    if (zimFile.exists()) zimFile.delete()
    zimFile.createNewFile()
    loadFileStream.use { inputStream ->
      val outputStream: OutputStream = FileOutputStream(zimFile)
      outputStream.use { it ->
        val buffer = ByteArray(inputStream.available())
        var length: Int
        while (inputStream.read(buffer).also { length = it } > 0) {
          it.write(buffer, 0, length)
        }
      }
    }
    activityScenario.onActivity {
      kiwixMainActivity = it
    }
    composeTestRule.runOnIdle {
      val navOptions = NavOptions.Builder()
        .setPopUpTo(KiwixDestination.Reader.route, false)
        .build()
      kiwixMainActivity.navigate(
        KiwixDestination.Reader.createRoute(zimFileUri = zimFile.toUri().toString()),
        navOptions
      )
    }
  }

  @After
  fun finish() {
    TestUtils.deleteTemporaryFilesOfTestCases(context)
  }
}
