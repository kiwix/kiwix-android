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
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.accessibility.AccessibilityChecks
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.internal.runner.junit4.statement.UiThreadStatement
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesCheck
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesViews
import com.google.android.apps.common.testing.accessibility.framework.checks.TouchTargetSizeCheck
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import leakcanary.LeakAssertions
import okhttp3.Request
import okhttp3.ResponseBody
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.anyOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.R.id
import org.kiwix.kiwixmobile.core.search.SearchFragment
import org.kiwix.kiwixmobile.core.search.viewmodel.Action
import org.kiwix.kiwixmobile.core.utils.LanguageUtils.Companion.handleLocaleChange
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.nav.destination.library.LocalLibraryFragmentDirections.actionNavigationLibraryToNavigationReader
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.testutils.TestUtils.closeSystemDialogs
import org.kiwix.kiwixmobile.testutils.TestUtils.getOkkHttpClientForTesting
import org.kiwix.kiwixmobile.testutils.TestUtils.isSystemUINotRespondingDialogVisible
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.URI

class SearchFragmentTest : BaseActivityTest() {

  private val rayCharlesZimFileUrl =
    "https://dev.kiwix.org/kiwix-android/test/wikipedia_en_ray_charles_maxi_2023-12.zim"

  @Rule
  @JvmField
  var retryRule = RetryRule()

  private lateinit var kiwixMainActivity: KiwixMainActivity
  private lateinit var uiDevice: UiDevice
  private lateinit var downloadingZimFile: File
  private lateinit var testZimFile: File

  @Before
  override fun waitForIdle() {
    uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).apply {
      if (isSystemUINotRespondingDialogVisible(this)) {
        closeSystemDialogs(context, this)
      }
      waitForIdle()
    }
    PreferenceManager.getDefaultSharedPreferences(context).edit {
      putBoolean(SharedPreferenceUtil.PREF_SHOW_INTRO, false)
      putBoolean(SharedPreferenceUtil.PREF_WIFI_ONLY, false)
      putBoolean(SharedPreferenceUtil.PREF_IS_TEST, true)
      putString(SharedPreferenceUtil.PREF_LANG, "en")
      putLong(
        SharedPreferenceUtil.PREF_LAST_DONATION_POPUP_SHOWN_IN_MILLISECONDS,
        System.currentTimeMillis()
      )
    }
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
  }

  init {
    AccessibilityChecks.enable().apply {
      setRunChecksFromRootView(true)
      setSuppressingResultMatcher(
        anyOf(
          allOf(
            matchesCheck(TouchTargetSizeCheck::class.java),
            matchesViews(ViewMatchers.withId(id.menu_searchintext))
          ),
          allOf(
            matchesCheck(TouchTargetSizeCheck::class.java),
            matchesViews(
              withContentDescription("More options")
            )
          )
        )
      )
    }
  }

  @Test
  fun searchFragmentSimple() {
    activityScenario.onActivity {
      kiwixMainActivity = it
      kiwixMainActivity.navigate(R.id.libraryFragment)
    }
    testZimFile = getTestZimFile()
    openKiwixReaderFragmentWithFile(testZimFile)
    search { checkZimFileSearchSuccessful(R.id.readerFragment) }
    openSearchWithQuery("Android", testZimFile)
    search {
      clickOnSearchItemInSearchList()
      checkZimFileSearchSuccessful(R.id.readerFragment)
    }

    openSearchWithQuery(zimFile = testZimFile)
    search {
      // test with fast typing/deleting
      searchWithFrequentlyTypedWords(searchUnitTestingQuery)
      assertSearchSuccessful(searchUnitTestResult)
      deleteSearchedQueryFrequently(searchUnitTestingQuery, uiDevice)

      // test with a short delay typing/deleting to
      // properly test the cancelling of previously searching task
      searchWithFrequentlyTypedWords(searchUnitTestingQuery, 50)
      assertSearchSuccessful(searchUnitTestResult)
      deleteSearchedQueryFrequently(searchUnitTestingQuery, uiDevice, 50)

      // test with a long delay typing/deleting to
      // properly execute the search query letter by letter
      searchWithFrequentlyTypedWords(searchUnitTestingQuery, 300)
      assertSearchSuccessful(searchUnitTestResult)
      deleteSearchedQueryFrequently(searchUnitTestingQuery, uiDevice, 300)
      // to close the keyboard
      pressBack()
      // go to reader screen
      pressBack()
    }

    UiThreadStatement.runOnUiThread { kiwixMainActivity.navigate(R.id.libraryFragment) }
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
    openSearchWithQuery(zimFile = downloadingZimFile)
    search {
      // test with fast typing/deleting
      searchWithFrequentlyTypedWords(searchQueryForDownloadedZimFile)
      assertSearchSuccessful(searchResultForDownloadedZimFile)
      deleteSearchedQueryFrequently(searchQueryForDownloadedZimFile, uiDevice)

      // test with a short delay typing/deleting to
      // properly test the cancelling of previously searching task
      searchWithFrequentlyTypedWords(searchQueryForDownloadedZimFile, 50)
      assertSearchSuccessful(searchResultForDownloadedZimFile)
      deleteSearchedQueryFrequently(searchQueryForDownloadedZimFile, uiDevice, 50)

      // test with a long delay typing/deleting to
      // properly execute the search query letter by letter
      searchWithFrequentlyTypedWords(searchQueryForDownloadedZimFile, 300)
      assertSearchSuccessful(searchResultForDownloadedZimFile)
      deleteSearchedQueryFrequently(searchQueryForDownloadedZimFile, uiDevice, 300)
      // open the reader fragment for next text case.
      openKiwixReaderFragmentWithFile(downloadingZimFile)
    }

    // Added test for checking the crash scenario where the application was crashing when we
    // frequently searched for article, and clicked on the searched item.
    search {
      // test by searching 10 article and clicking on them
      searchAndClickOnArticle(searchQueryForDownloadedZimFile)
      searchAndClickOnArticle("A Song")
      searchAndClickOnArticle("The Ra")
      searchAndClickOnArticle("The Ge")
      searchAndClickOnArticle("Wish")
      searchAndClickOnArticle("WIFI")
      searchAndClickOnArticle("Woman")
      searchAndClickOnArticle("Big Ba")
      searchAndClickOnArticle("My Wor")
      searchAndClickOnArticle("100")
      assertArticleLoaded()
    }
    removeTemporaryZimFilesToFreeUpDeviceStorage()
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
      // temporary disabled on Android 25
      LeakAssertions.assertNoLeaks()
    }
  }

  @Test
  fun testConcurrencyOfSearch() = runBlocking {
    val searchTerms = listOf(
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
      kiwixMainActivity.navigate(R.id.libraryFragment)
    }
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
    search { checkZimFileSearchSuccessful(R.id.readerFragment) }
    openSearchWithQuery(searchTerms[0], downloadingZimFile)
    // wait for searchFragment become visible on screen.
    delay(2000)
    val navHostFragment: NavHostFragment =
      kiwixMainActivity.supportFragmentManager
        .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
    val searchFragment = navHostFragment.childFragmentManager.fragments[0] as SearchFragment
    for (i in 1..100) {
      // This will execute the render method 100 times frequently.
      val searchTerm = searchTerms[i % searchTerms.size]
      searchFragment.searchViewModel.actions.trySend(Action.Filter(searchTerm)).isSuccess
    }
    for (i in 1..100) {
      // this will execute the render method 100 times with 100MS delay.
      delay(100)
      val searchTerm = searchTerms[i % searchTerms.size]
      searchFragment.searchViewModel.actions.trySend(Action.Filter(searchTerm)).isSuccess
    }
    for (i in 1..100) {
      // this will execute the render method 100 times with 200MS delay.
      delay(200)
      val searchTerm = searchTerms[i % searchTerms.size]
      searchFragment.searchViewModel.actions.trySend(Action.Filter(searchTerm)).isSuccess
    }
    for (i in 1..100) {
      // this will execute the render method 100 times with 200MS delay.
      delay(300)
      val searchTerm = searchTerms[i % searchTerms.size]
      searchFragment.searchViewModel.actions.trySend(Action.Filter(searchTerm)).isSuccess
    }
  }

  private fun removeTemporaryZimFilesToFreeUpDeviceStorage() {
    testZimFile.delete()
  }

  private fun openKiwixReaderFragmentWithFile(zimFile: File) {
    UiThreadStatement.runOnUiThread {
      kiwixMainActivity.navigate(
        actionNavigationLibraryToNavigationReader()
          .apply { zimFileUri = zimFile.toUri().toString() }
      )
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
    val zimFile = File(
      ContextCompat.getExternalFilesDirs(context, null)[0],
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
