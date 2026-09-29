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

package org.kiwix.kiwixmobile.core.main.reader.helper

import android.content.Intent
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.core.ThemeConfig
import org.kiwix.kiwixmobile.core.main.WebViewCallback
import org.kiwix.kiwixmobile.core.utils.TestingUtils.HILT_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.TestingUtils.RETRY_RULE_ORDER
import org.kiwix.kiwixmobile.testutils.RetryRule
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidTest
class WebViewFactoryDarkModeTest : BaseActivityTest() {
  @Rule(order = HILT_RULE_ORDER)
  @JvmField
  val hiltRule = HiltAndroidRule(this)

  @Rule(order = RETRY_RULE_ORDER)
  @JvmField
  val retryRule = RetryRule()

  @Inject
  lateinit var webViewFactory: WebViewFactory

  private var createdWebView: WebView? = null

  @Before
  override fun waitForIdle() {
    hiltRule.injectOnce()
    super.waitForIdle()
    launchMainActivity()
  }

  @After
  fun resetThemeAndDestroyWebView() {
    runBlocking { kiwixDataStore.updateAppTheme(ThemeConfig.Theme.SYSTEM.value.toString()) }
    InstrumentationRegistry.getInstrumentation().runOnMainSync {
      createdWebView?.destroy()
    }
  }

  @Test
  fun webViewReportsLightSchemeWhenAppThemeIsLight() {
    assertPrefersColorScheme(appTheme = ThemeConfig.Theme.LIGHT, expectDark = false)
  }

  @Test
  fun webViewReportsDarkSchemeWhenAppThemeIsDark() {
    assertPrefersColorScheme(appTheme = ThemeConfig.Theme.DARK, expectDark = true)
  }

  private fun assertPrefersColorScheme(appTheme: ThemeConfig.Theme, expectDark: Boolean) {
    runBlocking { kiwixDataStore.updateAppTheme(appTheme.value.toString()) }

    val webView = runBlocking(Dispatchers.Main) {
      webViewFactory.create(NoOpWebViewCallback, FrameLayout(context))
    }
    createdWebView = webView

    val pageLoaded = CountDownLatch(1)
    InstrumentationRegistry.getInstrumentation().runOnMainSync {
      webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) {
          pageLoaded.countDown()
        }
      }
      webView.loadDataWithBaseURL(null, DARK_MODE_TEST_HTML, "text/html", "utf-8", null)
    }
    assertTrue(
      "WebView did not finish loading test content",
      pageLoaded.await(10, TimeUnit.SECONDS)
    )

    assertEquals(
      "prefers-color-scheme should be ${if (expectDark) "dark" else "light"} " +
        "when app Theme is $appTheme",
      expectDark,
      evaluatePrefersColorSchemeDark(webView)
    )
  }

  private fun evaluatePrefersColorSchemeDark(webView: WebView): Boolean {
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
    return matchesDark
  }

  private object NoOpWebViewCallback : WebViewCallback {
    override fun webViewUrlLoading() = Unit
    override fun webViewUrlFinishedLoading() = Unit
    override fun webViewFailedLoading(failingUrl: String) = Unit
    override fun openExternalUrl(intent: Intent) = Unit
    override fun showSaveOrOpenUnsupportedFilesDialog(url: String, documentType: String?) = Unit
    override fun webViewProgressChanged(progress: Int, webView: WebView) = Unit
    override fun webViewTitleUpdated(title: String) = Unit
    override fun webViewPageChanged(page: Int, maxPages: Int) = Unit
    override fun webViewLongClick(url: String) = Unit
    override fun onFullscreenVideoToggled(isFullScreen: Boolean) = Unit
  }

  companion object {
    private const val DARK_MODE_TEST_HTML = """
      <html>
        <head><meta name="color-scheme" content="light dark"></head>
        <body></body>
      </html>
    """
  }
}
