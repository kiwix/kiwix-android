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

import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.internal.runner.junit4.statement.UiThreadStatement
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import leakcanary.LeakAssertions
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.LONG_WAIT
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.nav.destination.library.LocalLibraryFragmentDirections.actionNavigationLibraryToNavigationReader
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils.closeSystemDialogs
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
      closeSystemDialogs(context)
      waitForIdle(LONG_WAIT)
    }
    PreferenceManager.getDefaultSharedPreferences(context).edit {
      putBoolean(SharedPreferenceUtil.PREF_SHOW_INTRO, false)
      putBoolean(SharedPreferenceUtil.PREF_WIFI_ONLY, false)
      putBoolean(SharedPreferenceUtil.PREF_IS_TEST, true)
      putBoolean(SharedPreferenceUtil.PREF_PLAY_STORE_RESTRICTION, false)
    }
    activityScenario = ActivityScenario.launch(KiwixMainActivity::class.java).apply {
      moveToState(Lifecycle.State.RESUMED)
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
      assertSearchSuccessful(searchUnitTestResult)

      // test with a short delay typing/deleting to
      // properly test the cancelling of previously searching task
      searchWithFrequentlyTypedWords(searchUnitTestingQuery, 50)
      assertSearchSuccessful(searchUnitTestResult)
      deleteSearchedQueryFrequently(searchUnitTestingQuery, uiDevice, 50)
      assertSearchSuccessful(searchUnitTestResult)

      // test with a long delay typing/deleting to
      // properly execute the search query letter by letter
      searchWithFrequentlyTypedWords(searchUnitTestingQuery, 300)
      assertSearchSuccessful(searchUnitTestResult)
      deleteSearchedQueryFrequently(searchUnitTestingQuery, uiDevice, 300)
      assertSearchSuccessful(searchUnitTestResult)
      // to close the keyboard
      pressBack()
      // go to reader screen
      pressBack()
    }

    UiThreadStatement.runOnUiThread { kiwixMainActivity.navigate(R.id.libraryFragment) }
    // test with a large ZIM file to properly test the scenario
    downloadingZimFile = getDownloadingZimFile()
    OkHttpClient().newCall(downloadRequest()).execute().use { response ->
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
      assertSearchSuccessful(searchResultForDownloadedZimFile)

      // test with a short delay typing/deleting to
      // properly test the cancelling of previously searching task
      searchWithFrequentlyTypedWords(searchQueryForDownloadedZimFile, 50)
      assertSearchSuccessful(searchResultForDownloadedZimFile)
      deleteSearchedQueryFrequently(searchQueryForDownloadedZimFile, uiDevice, 50)
      assertSearchSuccessful(searchResultForDownloadedZimFile)

      // test with a long delay typing/deleting to
      // properly execute the search query letter by letter
      searchWithFrequentlyTypedWords(searchQueryForDownloadedZimFile, 300)
      assertSearchSuccessful(searchResultForDownloadedZimFile)
      deleteSearchedQueryFrequently(searchQueryForDownloadedZimFile, uiDevice, 300)
      assertSearchSuccessful(searchResultForDownloadedZimFile)
      // to close the keyboard
      pressBack()
      // go to reader screen
      pressBack()
    }
    removeTemporaryZimFilesToFreeUpDeviceStorage()
    LeakAssertions.assertNoLeaks()
  }

  private fun removeTemporaryZimFilesToFreeUpDeviceStorage() {
    testZimFile.delete()
    downloadingZimFile.delete()
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
    val zimFile = File(context.cacheDir, "testzim.zim")
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
    val zimFile = File(context.cacheDir, "stack_exchange.zim")
    if (zimFile.exists()) zimFile.delete()
    zimFile.createNewFile()
    return zimFile
  }

  @After
  fun setIsTestPreference() {
    PreferenceManager.getDefaultSharedPreferences(context).edit {
      putBoolean(SharedPreferenceUtil.PREF_IS_TEST, false)
    }
  }
}
