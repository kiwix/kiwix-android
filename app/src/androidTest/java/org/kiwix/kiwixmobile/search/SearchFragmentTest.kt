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
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavOptions
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.internal.runner.junit4.statement.UiThreadStatement
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.DuplicateClickableBoundsCheck
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import leakcanary.LeakAssertions
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
import org.kiwix.kiwixmobile.core.utils.LanguageUtils.Companion.handleLocaleChange
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
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
    val kiwixDataStore = KiwixDataStore(context).apply {
      lifeCycleScope.launch {
        setWifiOnly(false)
        setIntroShown()
        setPrefLanguage("en")
      }
    }
    PreferenceManager.getDefaultSharedPreferences(context).edit {
      putBoolean(SharedPreferenceUtil.PREF_IS_TEST, true)
      putBoolean(SharedPreferenceUtil.PREF_SCAN_FILE_SYSTEM_DIALOG_SHOWN, true)
      putBoolean(SharedPreferenceUtil.PREF_IS_FIRST_RUN, false)
      putLong(
        SharedPreferenceUtil.PREF_LAST_DONATION_POPUP_SHOWN_IN_MILLISECONDS,
        System.currentTimeMillis()
      )
    }
    activityScenario =
      ActivityScenario.launch(KiwixMainActivity::class.java).apply {
        moveToState(Lifecycle.State.RESUMED)
        onActivity {
          runBlocking {
            handleLocaleChange(
              it,
              "en",
              kiwixDataStore
            )
          }
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
  fun searchFragmentSimple() {
    activityScenario.onActivity {
      kiwixMainActivity = it
      kiwixMainActivity.navigate(KiwixDestination.Library.route)
    }
    testZimFile = getTestZimFile()
    openKiwixReaderFragmentWithFile(testZimFile)
    search { checkZimFileSearchSuccessful(composeTestRule) }
    openSearchWithQuery("Android", testZimFile)
    search {
      clickOnSearchItemInSearchList(composeTestRule)
      checkZimFileSearchSuccessful(composeTestRule)
    }

    openSearchWithQuery(zimFile = testZimFile)
    search {
      // test with fast typing/deleting
      searchWithFrequentlyTypedWords(searchUnitTestingQuery, composeTestRule = composeTestRule)
      assertSearchSuccessful(searchUnitTestResult, composeTestRule)
      deleteSearchedQueryFrequently(
        searchUnitTestingQuery,
        uiDevice,
        composeTestRule = composeTestRule
      )

      // test with a short delay typing/deleting to
      // properly test the cancelling of previously searching task
      searchWithFrequentlyTypedWords(searchUnitTestingQuery, 50, composeTestRule)
      assertSearchSuccessful(searchUnitTestResult, composeTestRule)
      deleteSearchedQueryFrequently(searchUnitTestingQuery, uiDevice, 50, composeTestRule)

      // test with a long delay typing/deleting to
      // properly execute the search query letter by letter
      searchWithFrequentlyTypedWords(searchUnitTestingQuery, 300, composeTestRule)
      assertSearchSuccessful(searchUnitTestResult, composeTestRule)
      deleteSearchedQueryFrequently(searchUnitTestingQuery, uiDevice, 300, composeTestRule)
      // to close the keyboard
      pressBack()
      // go to reader screen
      pressBack()
    }

    UiThreadStatement.runOnUiThread { kiwixMainActivity.navigate(KiwixDestination.Library.route) }
    // test with a large ZIM file to properly test the scenario
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
    search { checkZimFileSearchSuccessful(composeTestRule) }
    openSearchWithQuery(zimFile = downloadingZimFile)
    search {
      // test with fast typing/deleting
      searchWithFrequentlyTypedWords(
        searchQueryForDownloadedZimFile,
        composeTestRule = composeTestRule
      )
      assertSearchSuccessful(searchResultForDownloadedZimFile, composeTestRule)
      deleteSearchedQueryFrequently(
        searchQueryForDownloadedZimFile,
        uiDevice,
        composeTestRule = composeTestRule
      )

      // test with a short delay typing/deleting to
      // properly test the cancelling of previously searching task
      searchWithFrequentlyTypedWords(searchQueryForDownloadedZimFile, 50, composeTestRule)
      assertSearchSuccessful(searchResultForDownloadedZimFile, composeTestRule)
      deleteSearchedQueryFrequently(searchQueryForDownloadedZimFile, uiDevice, 50, composeTestRule)

      // test with a long delay typing/deleting to
      // properly execute the search query letter by letter
      searchWithFrequentlyTypedWords(searchQueryForDownloadedZimFile, 300, composeTestRule)
      assertSearchSuccessful(searchResultForDownloadedZimFile, composeTestRule)
      deleteSearchedQueryFrequently(searchQueryForDownloadedZimFile, uiDevice, 300, composeTestRule)
      // open the reader fragment for next text case.
      clickOnNavigationIcon(composeTestRule)
    }

    // Added test for checking the crash scenario where the application was crashing when we
    // frequently searched for article, and clicked on the searched item.
    search {
      // test by searching 10 article and clicking on them
      searchAndClickOnArticle(searchQueryForDownloadedZimFile, composeTestRule)
      searchAndClickOnArticle("A Song", composeTestRule)
      searchAndClickOnArticle("The Ra", composeTestRule)
      searchAndClickOnArticle("The Ge", composeTestRule)
      searchAndClickOnArticle("Wish", composeTestRule)
      searchAndClickOnArticle("WIFI", composeTestRule)
      searchAndClickOnArticle("Woman", composeTestRule)
      searchAndClickOnArticle("Big Ba", composeTestRule)
      searchAndClickOnArticle("My Wor", composeTestRule)
      searchAndClickOnArticle("100", composeTestRule)
      assertArticleLoaded()
    }
    removeTemporaryZimFilesToFreeUpDeviceStorage()
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
      // temporary disabled on Android 25
      LeakAssertions.assertNoLeaks()
    }
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
      val searchFragment = kiwixMainActivity.supportFragmentManager.fragments
        .filterIsInstance<SearchFragment>()
        .firstOrNull()
      for (i in 1..100) {
        // This will execute the render method 100 times frequently.
        val searchTerm = searchTerms[i % searchTerms.size]
        searchFragment?.searchViewModel?.actions?.trySend(Action.Filter(searchTerm))?.isSuccess
      }
      for (i in 1..100) {
        // this will execute the render method 100 times with 100MS delay.
        delay(100)
        val searchTerm = searchTerms[i % searchTerms.size]
        searchFragment?.searchViewModel?.actions?.trySend(Action.Filter(searchTerm))?.isSuccess
      }
      for (i in 1..100) {
        // this will execute the render method 100 times with 200MS delay.
        delay(200)
        val searchTerm = searchTerms[i % searchTerms.size]
        searchFragment?.searchViewModel?.actions?.trySend(Action.Filter(searchTerm))?.isSuccess
      }
      for (i in 1..100) {
        // this will execute the render method 100 times with 200MS delay.
        delay(300)
        val searchTerm = searchTerms[i % searchTerms.size]
        searchFragment?.searchViewModel?.actions?.trySend(Action.Filter(searchTerm))?.isSuccess
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
}
