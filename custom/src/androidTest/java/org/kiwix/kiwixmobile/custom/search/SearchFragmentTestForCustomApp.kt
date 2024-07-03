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

package org.kiwix.kiwixmobile.custom.search

import android.Manifest
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.os.ParcelFileDescriptor
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.accessibility.AccessibilityChecks
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.internal.runner.junit4.statement.UiThreadStatement
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils
import com.google.android.apps.common.testing.accessibility.framework.checks.TouchTargetSizeCheck
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.search.SearchFragment
import org.kiwix.kiwixmobile.core.search.viewmodel.Action
import org.kiwix.kiwixmobile.core.utils.LanguageUtils
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.custom.main.CustomMainActivity
import org.kiwix.kiwixmobile.custom.main.CustomReaderFragment
import org.kiwix.kiwixmobile.custom.testutils.RetryRule
import org.kiwix.kiwixmobile.custom.testutils.TestUtils.closeSystemDialogs
import org.kiwix.kiwixmobile.custom.testutils.TestUtils.isSystemUINotRespondingDialogVisible
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URI

@RunWith(AndroidJUnit4::class)
class SearchFragmentTestForCustomApp {
  private val permissions = arrayOf(
    Manifest.permission.READ_EXTERNAL_STORAGE,
    Manifest.permission.WRITE_EXTERNAL_STORAGE
  )

  @get:Rule
  var permissionRules: GrantPermissionRule =
    GrantPermissionRule.grant(*permissions)

  private val context: Context by lazy {
    InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
  }

  @Rule
  @JvmField
  var retryRule = RetryRule()

  private lateinit var customMainActivity: CustomMainActivity
  private lateinit var uiDevice: UiDevice
  private lateinit var downloadingZimFile: File
  private lateinit var activityScenario: ActivityScenario<CustomMainActivity>

  private val rayCharlesZimFileUrl =
    "https://dev.kiwix.org/kiwix-android/test/wikipedia_en_ray_charles_maxi_2023-12.zim"

  @Before
  fun waitForIdle() {
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
      putBoolean(SharedPreferenceUtil.PREF_PLAY_STORE_RESTRICTION, false)
      putString(SharedPreferenceUtil.PREF_LANG, "en")
    }
    activityScenario = ActivityScenario.launch(CustomMainActivity::class.java).apply {
      moveToState(Lifecycle.State.RESUMED)
      onActivity {
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
        Matchers.allOf(
          AccessibilityCheckResultUtils.matchesCheck(TouchTargetSizeCheck::class.java),
          AccessibilityCheckResultUtils.matchesViews(
            ViewMatchers.withId(org.kiwix.kiwixmobile.core.R.id.menu_searchintext)
          )
        )
      )
    }
  }

  @Test
  fun searchFragment() {
    activityScenario.onActivity {
      customMainActivity = it
    }
    // test with a large ZIM file to properly test the scenario
    downloadingZimFile = getDownloadingZimFile()
    if (downloadingZimFile.length() == 0L) {
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
    }
    openZimFileInReaderWithAssetFileDescriptor(downloadingZimFile)
    openSearchWithQuery()
    val searchTerm = "A Fool"
    val searchedItem = "A Fool for You"
    search {
      // test with fast typing/deleting
      searchWithFrequentlyTypedWords(searchTerm)
      assertSearchSuccessful(searchedItem)
      deleteSearchedQueryFrequently(searchTerm, uiDevice)

      // test with a short delay typing/deleting to
      // properly test the cancelling of previously searching task
      searchWithFrequentlyTypedWords(searchTerm, 50)
      assertSearchSuccessful(searchedItem)
      deleteSearchedQueryFrequently(searchTerm, uiDevice, 50)

      // test with a long delay typing/deleting to
      // properly execute the search query letter by letter
      searchWithFrequentlyTypedWords(searchTerm, 300)
      assertSearchSuccessful(searchedItem)
      deleteSearchedQueryFrequently(searchTerm, uiDevice, 300)
      // to close the keyboard
      pressBack()
      // go to reader screen
      pressBack()
    }

    // Added test for checking the crash scenario where the application was crashing when we
    // frequently searched for article, and clicked on the searched item.
    search {
      // test by searching 10 article and clicking on them
      searchAndClickOnArticle(searchTerm)
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
      customMainActivity = it
    }
    // test with a large ZIM file to properly test the scenario
    downloadingZimFile = getDownloadingZimFile()
    if (downloadingZimFile.length() == 0L) {
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
    }
    openZimFileInReaderWithAssetFileDescriptor(downloadingZimFile)
    openSearchWithQuery(searchTerms[0])
    // wait for searchFragment become visible on screen.
    delay(2000)
    val navHostFragment: NavHostFragment =
      customMainActivity.supportFragmentManager
        .findFragmentById(
          customMainActivity.activityCustomMainBinding.customNavController.id
        ) as NavHostFragment
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

  private fun openSearchWithQuery(query: String = "") {
    UiThreadStatement.runOnUiThread {
      customMainActivity.openSearch(searchString = query)
    }
  }

  private fun openZimFileInReaderWithAssetFileDescriptor(downloadingZimFile: File) {
    getAssetFileDescriptorFromFile(downloadingZimFile)?.let(::openZimFileInReader) ?: run {
      throw RuntimeException("Unable to get fileDescriptor from file. Original exception")
    }
  }

  private fun openZimFileInReader(assetFileDescriptor: AssetFileDescriptor) {
    UiThreadStatement.runOnUiThread {
      val navHostFragment: NavHostFragment =
        customMainActivity.supportFragmentManager
          .findFragmentById(
            customMainActivity.activityCustomMainBinding.customNavController.id
          ) as NavHostFragment
      val customReaderFragment =
        navHostFragment.childFragmentManager.fragments[0] as CustomReaderFragment
      customReaderFragment.openZimFile(null, true, listOf(assetFileDescriptor))
    }
  }

  private fun getAssetFileDescriptorFromFile(file: File): AssetFileDescriptor? {
    val parcelFileDescriptor = getFileDescriptor(file)
    if (parcelFileDescriptor != null) {
      return AssetFileDescriptor(parcelFileDescriptor, 0, file.length())
    }
    return null
  }

  private fun getFileDescriptor(file: File?): ParcelFileDescriptor? {
    try {
      return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    } catch (e: IOException) {
      e.printStackTrace()
      return null
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

  private fun getDownloadingZimFile(isDeletePreviousZimFile: Boolean = true): File {
    val zimFile = File(context.cacheDir, "ray_charles.zim")
    if (isDeletePreviousZimFile) {
      if (zimFile.exists()) zimFile.delete()
      zimFile.createNewFile()
    }
    return zimFile
  }
}
