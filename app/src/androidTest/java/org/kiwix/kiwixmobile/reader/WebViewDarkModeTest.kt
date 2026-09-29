/*
 * Kiwix Android
 * Copyright (c) 2026 Kiwix <android.kiwix.org>
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

import android.view.View
import android.webkit.WebView
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.ThemeConfig
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.settings.DIALOG_PREFERENCE_ITEM_TESTING_TAG
import org.kiwix.kiwixmobile.core.utils.TestingUtils.COMPOSE_TEST_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.TestingUtils.HILT_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.TestingUtils.RETRY_RULE_ORDER
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.settings.settingsRobo
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils.getZimFileFromResourceFolder
import org.kiwix.kiwixmobile.testutils.TestUtils.waitUntilTimeout
import org.kiwix.kiwixmobile.ui.KiwixDestination
import org.kiwix.kiwixmobile.utils.StandardActions
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@HiltAndroidTest
class WebViewDarkModeTest : BaseActivityTest() {
  @Rule(order = HILT_RULE_ORDER)
  @JvmField
  val hiltRule = HiltAndroidRule(this)

  @Rule(order = RETRY_RULE_ORDER)
  @JvmField
  val retryRule = RetryRule()

  @Rule(order = COMPOSE_TEST_RULE_ORDER)
  @JvmField
  val composeTestRule = createComposeRule()

  private lateinit var kiwixMainActivity: KiwixMainActivity

  @Before
  override fun waitForIdle() {
    hiltRule.injectOnce()
    super.waitForIdle()
    launchMainActivity { kiwixMainActivity = it }
    composeTestRule.enableAccessibilityChecks(createAccessibilityValidator())
    composeTestRule.waitForIdle()
  }

  @After
  fun resetTheme() {
    runBlocking { kiwixDataStore.updateAppTheme(ThemeConfig.Theme.SYSTEM.value.toString()) }
  }

  @Test
  fun webViewReflectsThemeSelectedViaSettingsScreen() {
    activityScenario.onActivity {
      kiwixMainActivity = it
      kiwixMainActivity.navigate(KiwixDestination.Library.route)
    }
    composeTestRule.waitForIdle()
    val zimFile = getZimFileFromResourceFolder(context, "testzim.zim")
    composeTestRule.runOnUiThread {
      kiwixMainActivity.openZimFromFilePath(zimFile.absolutePath)
    }
    composeTestRule.waitForIdle()
    reader {
      checkZimFileLoadedSuccessful(composeTestRule, "Android_(operating_system)")
    }

    selectAppThemeViaSettingsScreen(context.getString(R.string.theme_light))
    returnToReaderScreen()
    reader {
      checkZimFileLoadedSuccessful(composeTestRule, "Android_(operating_system)")
    }
    assertWebViewPrefersColorSchemeDark(expectDark = false)

    selectAppThemeViaSettingsScreen(context.getString(R.string.theme_dark))
    returnToReaderScreen()
    reader {
      checkZimFileLoadedSuccessful(composeTestRule, "Android_(operating_system)")
    }
    assertWebViewPrefersColorSchemeDark(expectDark = true)
  }

  private fun selectAppThemeViaSettingsScreen(themeLabel: String) {
    StandardActions.openDrawer(kiwixMainActivity as CoreMainActivity)
    StandardActions.enterSettings(composeTestRule)
    composeTestRule.waitForIdle()
    settingsRobo {
      clickNightModePreference(composeTestRule)
      assertNightModeDialogDisplayed(composeTestRule)
    }
    composeTestRule
      .onAllNodesWithTag(DIALOG_PREFERENCE_ITEM_TESTING_TAG, useUnmergedTree = true)
      .filter(hasContentDescription(themeLabel))
      .onFirst()
      .performClick()
    composeTestRule.waitUntilTimeout()
    composeTestRule.waitForIdle()
  }

  private fun returnToReaderScreen() {
    activityScenario.onActivity { kiwixMainActivity = it }
    composeTestRule.waitForIdle()
    reader {
      clickOnNavigationIcon(composeTestRule)
    }
    composeTestRule.waitForIdle()
  }

  private fun assertWebViewPrefersColorSchemeDark(expectDark: Boolean) {
    var foundWebView: WebView? = null
    onView(isAssignableFrom(WebView::class.java)).perform(object : ViewAction {
      override fun getConstraints(): Matcher<View> = isAssignableFrom(WebView::class.java)
      override fun getDescription() = "Capture the currently displayed WebView"
      override fun perform(uiController: UiController, view: View) {
        foundWebView = view as WebView
      }
    })
    val webView = requireNotNull(foundWebView) {
      "No WebView found in the current view hierarchy"
    }

    val resultReceived = CountDownLatch(1)
    var matchesDark = false
    InstrumentationRegistry.getInstrumentation().runOnMainSync {
      webView.evaluateJavascript(
        "window.matchMedia('(prefers-color-scheme: dark)').matches"
      ) { value ->
        matchesDark = value == "true"
        resultReceived.countDown()
      }
    }
    assertTrue(
      "Timed out waiting for evaluateJavascript result",
      resultReceived.await(10, TimeUnit.SECONDS)
    )
    assertEquals(
      "WebView's prefers-color-scheme should be ${if (expectDark) "dark" else "light"}",
      expectDark,
      matchesDark
    )
  }
}
