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

import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavOptions
import androidx.test.core.app.ActivityScenario
import androidx.test.internal.runner.junit4.statement.UiThreadStatement
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.DuplicateClickableBoundsCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.TouchTargetSizeCheck
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import kotlinx.coroutines.launch
import okhttp3.Request
import okhttp3.ResponseBody
import org.hamcrest.Matchers.anyOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.setNavigationResultOnCurrent
import org.kiwix.kiwixmobile.core.main.ZIM_FILE_URI_KEY
import org.kiwix.kiwixmobile.core.main.reader.READER_SCREEN_TESTING_TAG
import org.kiwix.kiwixmobile.core.utils.TestingUtils.COMPOSE_TEST_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.TestingUtils.RETRY_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.testutils.TestUtils.getOkkHttpClientForTesting
import org.kiwix.kiwixmobile.ui.KiwixDestination
import java.io.File
import java.io.FileOutputStream
import java.net.URI

class ReaderAppBarScrollBehaviourTest : BaseActivityTest() {
  @Rule(order = RETRY_RULE_ORDER)
  @JvmField
  val retryRule = RetryRule()

  @get:Rule(order = COMPOSE_TEST_RULE_ORDER)
  val composeTestRule = createComposeRule()
  private lateinit var kiwixMainActivity: KiwixMainActivity
  private lateinit var uiDevice: UiDevice

  @Before
  override fun waitForIdle() {
    uiDevice =
      UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).apply {
        if (TestUtils.isSystemUINotRespondingDialogVisible(this)) {
          TestUtils.closeSystemDialogs(context, this)
        }
        waitForIdle()
      }
    KiwixDataStore(context).apply {
      lifeCycleScope.launch {
        setWifiOnly(false)
        setIntroShown()
        setPrefLanguage("en")
        setIsScanFileSystemDialogShown(true)
        setIsFirstRun(false)
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
    val accessibilityValidator = AccessibilityValidator().setRunChecksFromRootView(true)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
      accessibilityValidator.setSuppressingResultMatcher(
        anyOf(
          matchesCheck(DuplicateClickableBoundsCheck::class.java),
          matchesCheck(TouchTargetSizeCheck::class.java)
        )
      )
    } else {
      accessibilityValidator.setSuppressingResultMatcher(
        anyOf(
          matchesCheck(DuplicateClickableBoundsCheck::class.java)
        )
      )
    }
    composeTestRule.enableAccessibilityChecks(accessibilityValidator)
  }

  @Test
  fun testReaderScrollBehaviourIsStable() {
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      activityScenario.onActivity {
        kiwixMainActivity = it
        kiwixMainActivity.navigate(KiwixDestination.Library.route)
      }

      composeTestRule.waitForIdle()
      val downloadingZimFile = getDownloadingZimFile()
      getOkkHttpClientForTesting().newCall(downloadRequest()).execute().use { response ->
        if (response.isSuccessful) {
          response.body?.let { responseBody ->
            writeZimFileData(responseBody, downloadingZimFile)
          }
        } else {
          throw RuntimeException(
            "Download Failed. Error: ${response.message}\n" +
              " Status Code: ${response.code}"
          )
        }
      }
      openKiwixReaderFragmentWithFile(downloadingZimFile)
      composeTestRule.waitForIdle()

      reader {
        checkZimFileLoadedSuccessful(composeTestRule)
      }

      val readerNode = composeTestRule.onNodeWithTag(READER_SCREEN_TESTING_TAG)

      readerNode.assertExists()

      val displayWidth = uiDevice.displayWidth
      val displayHeight = uiDevice.displayHeight

      val centerX = displayWidth / 2
      val startY = (displayHeight * 0.85f).toInt()
      val endY = (displayHeight * 0.15f).toInt()

      repeat(5) {
        uiDevice.swipe(centerX, startY, centerX, endY, 20)
        composeTestRule.waitForIdle()
      }

      repeat(5) {
        uiDevice.swipe(centerX, endY, centerX, startY, 20)
        composeTestRule.waitForIdle()
      }

      readerNode.assertExists()
    }
  }

  private fun openKiwixReaderFragmentWithFile(zimFile: File) {
    UiThreadStatement.runOnUiThread {
      val navOptions = NavOptions.Builder()
        .setPopUpTo(KiwixDestination.Reader.route, false)
        .build()
      kiwixMainActivity.apply {
        kiwixMainActivity.navigate(KiwixDestination.Reader.route, navOptions)
        setNavigationResultOnCurrent(zimFile.toUri().toString(), ZIM_FILE_URI_KEY)
      }
    }
  }

  private val rayCharlesZimFileUrl =
    "https://dev.kiwix.org/kiwix-android/test/wikipedia_en_ray_charles_maxi_2023-12.zim"

  private fun downloadRequest(zimUrl: String = rayCharlesZimFileUrl) =
    Request.Builder().url(URI.create(zimUrl).toURL()).build()

  private fun getDownloadingZimFile(): File {
    val zimFile = File(context.cacheDir, "klimawandel.zim")
    if (zimFile.exists()) zimFile.delete()
    zimFile.createNewFile()
    return zimFile
  }

  private fun writeZimFileData(responseBody: ResponseBody, file: File) {
    FileOutputStream(file).use { outputStream ->
      responseBody.byteStream().use { inputStream ->
        val buffer = ByteArray(4096)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
          outputStream.write(buffer, 0, bytesRead)
        }
        outputStream.flush()
      }
    }
  }

  @After
  fun finish() {
    TestUtils.deleteTemporaryFilesOfTestCases(context)
  }
}
