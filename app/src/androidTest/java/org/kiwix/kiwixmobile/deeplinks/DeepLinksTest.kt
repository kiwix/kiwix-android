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

package org.kiwix.kiwixmobile.deeplinks

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.DuplicateClickableBoundsCheck
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.anyOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.fail
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.core.main.ZIM_HOST_NAV_DEEP_LINK
import org.kiwix.kiwixmobile.core.utils.TestingUtils.COMPOSE_TEST_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.TestingUtils.RETRY_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.ALERT_DIALOG_CONFIRM_BUTTON_TESTING_TAG
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.main.OPENING_ZIM_FILE_DELAY
import org.kiwix.kiwixmobile.nav.destination.library.library
import org.kiwix.kiwixmobile.page.history.navigationHistory
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.testutils.TestUtils.TEST_PAUSE_MS_FOR_DOWNLOAD_TEST
import org.kiwix.kiwixmobile.testutils.TestUtils.testFlakyView
import org.kiwix.kiwixmobile.ui.KiwixDestination
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class DeepLinksTest : BaseActivityTest() {
  @Rule(order = RETRY_RULE_ORDER)
  @JvmField
  val retryRule = RetryRule()

  @get:Rule(order = COMPOSE_TEST_RULE_ORDER)
  val composeTestRule = createComposeRule()
  private lateinit var kiwixDataStore: KiwixDataStore

  @Before
  override fun waitForIdle() {
    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).apply {
      if (TestUtils.isSystemUINotRespondingDialogVisible(this)) {
        TestUtils.closeSystemDialogs(context, this)
      }
      waitForIdle()
    }
    kiwixDataStore = KiwixDataStore(
      InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    ).apply {
      lifeCycleScope.launch {
        setWifiOnly(false)
        setIntroShown()
        setPrefLanguage("en")
        setLastDonationPopupShownInMilliSeconds(System.currentTimeMillis())
        setIsScanFileSystemDialogShown(true)
        setShowStorageSelectionDialogOnCopyMove(false)
        setIsFirstRun(false)
        setIsPlayStoreBuild(true)
        setPrefIsTest(true)
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
  fun fileTypeDeepLinkTest() {
    loadZimFileInApplicationAndReturnSchemeTypeUri("file")?.let {
      // Launch the activity to test the deep link
      ActivityScenario.launch<KiwixMainActivity>(
        createDeepLinkIntent(it, "application/octet-stream")
      ).onActivity {}
      clickOnCopy(composeTestRule)
      navigationHistory {
        checkZimFileLoadedSuccessful(composeTestRule)
        assertZimFileLoaded(composeTestRule) // check if the zim file successfully loaded
        clickOnAndroidArticle(composeTestRule)
      }
    } ?: kotlin.run {
      // error in getting the zim file Uri
      fail("Couldn't get file type Uri for zim file")
    }
  }

  private fun clickOnCopy(composeTestRule: ComposeContentTestRule) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      testFlakyView({
        composeTestRule.apply {
          waitUntil(TEST_PAUSE_MS_FOR_DOWNLOAD_TEST.toLong()) {
            onNodeWithTag(ALERT_DIALOG_CONFIRM_BUTTON_TESTING_TAG).isDisplayed()
          }
          onNodeWithTag(ALERT_DIALOG_CONFIRM_BUTTON_TESTING_TAG).performClick()
        }
      })
    }
  }

  @Test
  fun contentTypeDeepLinkTest() {
    loadZimFileInApplicationAndReturnSchemeTypeUri("content")?.let {
      // Launch the activity to test the deep link
      ActivityScenario.launch<KiwixMainActivity>(
        createDeepLinkIntent(it, "application/octet-stream")
      ).onActivity {}
      clickOnCopy(composeTestRule)
      navigationHistory {
        checkZimFileLoadedSuccessful(composeTestRule)
        assertZimFileLoaded(composeTestRule) // check if the zim file successfully loaded
        clickOnAndroidArticle(composeTestRule)
      }
    } ?: kotlin.run {
      // error in getting the zim file Uri
      fail("Couldn't get file type Uri for zim file")
    }
  }

  @Test
  fun zimUrlTypeDeepLinkTest() {
    activityScenario =
      ActivityScenario.launch(KiwixMainActivity::class.java).apply {
        moveToState(Lifecycle.State.RESUMED)
        onActivity {
          AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
        }
      }
    activityScenario.onActivity {
      it.navigate(KiwixDestination.Library.route)
    }
    library {
      refreshList(composeTestRule)
      waitUntilZimFilesRefreshing(composeTestRule)
      deleteZimIfExists(composeTestRule)
    }
    loadZimFileInApplicationAndReturnSchemeTypeUri("file")
    library {
      refreshList(composeTestRule)
      waitUntilZimFilesRefreshing(composeTestRule)
    }
    // it tests the zim deep link e.g. (zim://60094d1e-1c9a-a60b-2011-4fb02f8db6c3/A/Android_(operating_system).html)
    ActivityScenario.launch<KiwixMainActivity>(
      createDeepLinkIntent("zim://60094d1e-1c9a-a60b-2011-4fb02f8db6c3/A/Android_(operating_system).html".toUri())
    ).onActivity {}
    // for a bit to properly handle the deep link.
    composeTestRule.mainClock.advanceTimeBy(OPENING_ZIM_FILE_DELAY + 500)
    composeTestRule.waitForIdle()
    deepLink {
      checkZimFileLoadedSuccessful(composeTestRule)
      assertZimFilePageLoaded(composeTestRule)
    }
  }

  @Test
  fun testZimHostDeepLink() {
    // For testing the deep link triggers when user click on notification of the hotspot.
    // it should open the WIFI-Hotspot screen.
    ActivityScenario.launch<KiwixMainActivity>(
      createDeepLinkIntent(ZIM_HOST_NAV_DEEP_LINK.toUri())
    ).onActivity {}
    // for a bit to properly handle the deep link.
    composeTestRule.mainClock.advanceTimeBy(OPENING_ZIM_FILE_DELAY + 500)
    composeTestRule.waitForIdle()
    deepLink {
      checkZimHostScreenVisible(composeTestRule)
    }
  }

  private fun loadZimFileInApplicationAndReturnSchemeTypeUri(schemeType: String): Uri? {
    val loadFileStream =
      DeepLinksTest::class.java.classLoader.getResourceAsStream("testzim.zim")
    val zimFile = runBlocking { File(kiwixDataStore.defaultStorage(), "testzim.zim") }
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
    return when (schemeType) {
      "file" -> Uri.fromFile(zimFile)
      "content" ->
        FileProvider.getUriForFile(
          context,
          "${context.packageName}.fileprovider",
          zimFile
        )

      else -> null
    }
  }

  private fun createDeepLinkIntent(
    uri: Uri,
    mimeType: String? = null
  ): Intent {
    return Intent(Intent.ACTION_VIEW).apply {
      data = uri
      mimeType?.let { setDataAndType(uri, it) }
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      setPackage(context.packageName)
    }
  }

  @After
  fun finish() {
    TestUtils.deleteTemporaryFilesOfTestCases(context)
  }
}
