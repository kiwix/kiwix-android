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
package org.kiwix.kiwixmobile.search

import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavOptions
import androidx.test.core.app.ActivityScenario
import androidx.test.internal.runner.junit4.statement.UiThreadStatement
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.DuplicateClickableBoundsCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.SpeakableTextPresentCheck
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
import org.kiwix.kiwixmobile.core.search.SearchFragment
import org.kiwix.kiwixmobile.core.search.viewmodel.Action
import org.kiwix.kiwixmobile.core.search.viewmodel.SearchViewModel
import org.kiwix.kiwixmobile.core.utils.TestingUtils.COMPOSE_TEST_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.TestingUtils.RETRY_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.testutils.TestUtils.closeSystemDialogs
import org.kiwix.kiwixmobile.testutils.TestUtils.getOkkHttpClientForTesting
import org.kiwix.kiwixmobile.testutils.TestUtils.isSystemUINotRespondingDialogVisible
import org.kiwix.kiwixmobile.ui.KiwixDestination
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.URI

class SearchFragmentTest : BaseActivityTest() {
  private val rayCharlesZimFileUrl =
    "https://dev.kiwix.org/kiwix-android/test/wikipedia_en_ray_charles_maxi_2023-12.zim"

  @Rule(order = RETRY_RULE_ORDER)
  @JvmField
  val retryRule = RetryRule()

  @get:Rule(order = COMPOSE_TEST_RULE_ORDER)
  val composeTestRule = createComposeRule()

  private lateinit var kiwixMainActivity: KiwixMainActivity
  private lateinit var uiDevice: UiDevice
  private lateinit var downloadingZimFile: File
  private lateinit var testZimFile: File

  @Before
  override fun waitForIdle() {
    uiDevice =
      UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).apply {
        if (isSystemUINotRespondingDialogVisible(this)) {
          closeSystemDialogs(context, this)
        }
        waitForIdle()
      }
    KiwixDataStore(context).apply {
      lifeCycleScope.launch {
        setWifiOnly(false)
        setIntroShown()
        setPrefLanguage("en")
        setLastDonationPopupShownInMilliSeconds(System.currentTimeMillis())
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
          matchesCheck(SpeakableTextPresentCheck::class.java)
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
  fun testConcurrencyOfSearch() =
    runBlocking {
      val searchTerms =
        listOf(
          "A Song",
          "The Ra",
          "The Ge",
          "Wish",
          "WIFI",
          "Woman",
          "Big Ba",
          "My Wor",
          "100"
        )
      activityScenario.onActivity {
        kiwixMainActivity = it
        kiwixMainActivity.navigate(KiwixDestination.Library.route)
      }
      composeTestRule.waitForIdle()
      downloadingZimFile = getDownloadingZimFile()
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
      search { checkZimFileSearchSuccessful(composeTestRule) }
      openSearchWithQuery(searchTerms[0], downloadingZimFile)
      // wait for searchFragment become visible on screen.
      delay(2000)
      val searchFragment =
        waitForSearchFragment(kiwixMainActivity)

      val viewModel =
        ViewModelProvider(searchFragment)[SearchViewModel::class.java]

      for (i in 1..100) {
        val term = searchTerms[i % searchTerms.size]
        viewModel.actions.trySend(Action.Filter(term))
      }
      for (i in 1..100) {
        // this will execute the render method 100 times with 100MS delay.
        delay(100)
        val term = searchTerms[i % searchTerms.size]
        viewModel.actions.trySend(Action.Filter(term))
      }
      for (i in 1..100) {
        // this will execute the render method 100 times with 200MS delay.
        delay(200)
        val term = searchTerms[i % searchTerms.size]
        viewModel.actions.trySend(Action.Filter(term))
      }
      for (i in 1..100) {
        // this will execute the render method 100 times with 200MS delay.
        delay(300)
        val term = searchTerms[i % searchTerms.size]
        viewModel.actions.trySend(Action.Filter(term))
      }
    }

  @Test
  fun testSearchWithExtraSpaces() {
    activityScenario.onActivity {
      kiwixMainActivity = it
      kiwixMainActivity.navigate(KiwixDestination.Library.route)
    }
    testZimFile = getTestZimFile()
    openKiwixReaderFragmentWithFile(testZimFile)
    search { checkZimFileSearchSuccessful(composeTestRule) }
    openSearchWithQuery("Android ", testZimFile)
    search {
      clickOnSearchItemInSearchList(composeTestRule)
      checkZimFileSearchSuccessful(composeTestRule)
    }
    openSearchWithQuery("   Unit test   ", testZimFile)
    search {
      clickOnSearchItemInSearchList(composeTestRule)
      checkZimFileSearchSuccessful(composeTestRule)
    }
  }

  private fun removeTemporaryZimFilesToFreeUpDeviceStorage() {
    testZimFile.delete()
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

  private fun downloadRequest() =
    Request.Builder()
      .url(URI.create(rayCharlesZimFileUrl).toURL())
      .build()

  private fun openSearchWithQuery(query: String = "", zimFile: File) {
    UiThreadStatement.runOnUiThread {
      if (zimFile.canRead()) {
        kiwixMainActivity.openSearch(searchString = query)
      } else {
        throw RuntimeException(
          "File $zimFile is not readable." +
            " Original File $zimFile is readable = ${zimFile.canRead()}" +
            " Size ${zimFile.length()}"
        )
      }
    }
  }

  private fun getTestZimFile(): File {
    val loadFileStream =
      SearchFragmentTest::class.java.classLoader.getResourceAsStream("testzim.zim")
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
    return zimFile
  }

  private fun getDownloadingZimFile(): File {
    val zimFile = File(context.cacheDir, "ray_charles.zim")
    if (zimFile.exists()) zimFile.delete()
    zimFile.createNewFile()
    return zimFile
  }

  @After
  fun finish() {
    TestUtils.deleteTemporaryFilesOfTestCases(context)
    context.cacheDir?.let {
      it.listFiles()?.let { files ->
        for (child in files) {
          child.delete()
        }
      }
      it.delete()
    }
  }

  private fun waitForSearchFragment(
    activity: FragmentActivity,
    timeoutMs: Long = 5_000
  ): SearchFragment {
    val start = System.currentTimeMillis()
    while (System.currentTimeMillis() - start < timeoutMs) {
      val fragment =
        activity.supportFragmentManager.fragments
          .firstOrNull { it is SearchFragment && it.isAdded }
          as? SearchFragment

      if (fragment != null) return fragment
      Thread.sleep(50)
    }
    throw IllegalStateException("SearchFragment not found after $timeoutMs ms")
  }
}
