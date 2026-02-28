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
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.internal.runner.junit4.statement.UiThreadStatement
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.data.remote.UserAgentInterceptor
import org.kiwix.kiwixmobile.core.di.modules.CALL_TIMEOUT
import org.kiwix.kiwixmobile.core.di.modules.CONNECTION_TIMEOUT
import org.kiwix.kiwixmobile.core.di.modules.READ_TIMEOUT
import org.kiwix.kiwixmobile.core.di.modules.USER_AGENT
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.search.viewmodel.Action
import org.kiwix.kiwixmobile.core.search.viewmodel.SearchViewModel
import org.kiwix.kiwixmobile.core.ui.components.NAVIGATION_ICON_TESTING_TAG
import org.kiwix.kiwixmobile.core.utils.TestingUtils.COMPOSE_TEST_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.TestingUtils.RETRY_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.custom.main.CustomMainActivity
import org.kiwix.kiwixmobile.custom.main.CustomReaderFragment
import org.kiwix.kiwixmobile.custom.testutils.RetryRule
import org.kiwix.kiwixmobile.custom.testutils.TestUtils
import org.kiwix.kiwixmobile.custom.testutils.TestUtils.closeSystemDialogs
import org.kiwix.kiwixmobile.custom.testutils.TestUtils.isSystemUINotRespondingDialogVisible
import org.kiwix.kiwixmobile.custom.testutils.TestUtils.waitUntilTimeout
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@RunWith(AndroidJUnit4::class)
class SearchScreenTestForCustomApp {
  private val permissions =
    arrayOf(
      Manifest.permission.READ_EXTERNAL_STORAGE,
      Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
  private val lifeCycleScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

  @get:Rule
  var permissionRules: GrantPermissionRule =
    GrantPermissionRule.grant(*permissions)

  private val context: Context by lazy {
    InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
  }

  @Rule(order = RETRY_RULE_ORDER)
  @JvmField
  var retryRule = RetryRule()

  @get:Rule(order = COMPOSE_TEST_RULE_ORDER)
  val composeTestRule = createComposeRule()

  private lateinit var customMainActivity: CustomMainActivity
  private lateinit var uiDevice: UiDevice
  private lateinit var downloadingZimFile: File
  private lateinit var activityScenario: ActivityScenario<CustomMainActivity>

  private val scientificAllianceZIMUrl =
    "https://download.kiwix.org/zim/zimit/scientific-alliance.obscurative.ru_ru_all_2025-06.zim"
  private val rayCharlesZIMFileUrl =
    "https://dev.kiwix.org/kiwix-android/test/wikipedia_en_ray_charles_maxi_2023-12.zim"

  @Before
  fun waitForIdle() {
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
        setPrefIsTest(true)
      }
    }
    activityScenario =
      ActivityScenario.launch(CustomMainActivity::class.java).apply {
        moveToState(Lifecycle.State.RESUMED)
        onActivity {
          AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
        }
      }
  }

  @Test
  fun searchFragment() {
    activityScenario.onActivity {
      customMainActivity = it
    }
    // test with a large ZIM file to properly test the scenario
    downloadingZimFile = getDownloadingZimFileFromDataFolder()
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
    UiThreadStatement.runOnUiThread {
      customMainActivity.navigate(customMainActivity.readerFragmentRoute)
    }
    openZimFileInReader(zimFile = downloadingZimFile)
    openSearchWithQuery()
    val searchTerm = "gard"
    val searchedItem = "Gardanta Spirito - Андивионский Научный Альянс"
    search {
      // test with fast typing/deleting
      searchWithFrequentlyTypedWords(searchTerm, composeTestRule = composeTestRule)
      assertSearchSuccessful(searchedItem, composeTestRule)
      deleteSearchedQueryFrequently(searchTerm, uiDevice, composeTestRule = composeTestRule)

      // test with a short delay typing/deleting to
      // properly test the cancelling of previously searching task
      searchWithFrequentlyTypedWords(searchTerm, 50, composeTestRule)
      assertSearchSuccessful(searchedItem, composeTestRule)
      deleteSearchedQueryFrequently(searchTerm, uiDevice, 50, composeTestRule)

      // test with a long delay typing/deleting to
      // properly execute the search query letter by letter
      searchWithFrequentlyTypedWords(searchTerm, 300, composeTestRule)
      assertSearchSuccessful(searchedItem, composeTestRule)
      deleteSearchedQueryFrequently(searchTerm, uiDevice, 300, composeTestRule)
      // to close the keyboard
      pressBack()
      // go to reader screen
      pressBack()
    }

    // Added test for checking the crash scenario where the application was crashing when we
    // frequently searched for article, and clicked on the searched item.
    search {
      // test by searching 10 article and clicking on them
      searchAndClickOnArticle(searchTerm, composeTestRule)
      searchAndClickOnArticle("eilum", composeTestRule)
      searchAndClickOnArticle("page", composeTestRule)
      searchAndClickOnArticle("list", composeTestRule)
      searchAndClickOnArticle("ladder", composeTestRule)
      searchAndClickOnArticle("welc", composeTestRule)
      searchAndClickOnArticle("js", composeTestRule)
      searchAndClickOnArticle("hizo", composeTestRule)
      searchAndClickOnArticle("fad", composeTestRule)
      searchAndClickOnArticle("forum", composeTestRule)
      assertArticleLoaded()
    }
  }

  @Test
  fun testConcurrencyOfSearch() =
    runBlocking {
      val searchTerms =
        listOf(
          "eilum",
          "page",
          "list",
          "ladder",
          "welc",
          "js",
          "hizo",
          "fad",
          "forum"
        )
      activityScenario.onActivity {
        customMainActivity = it
      }
      // test with a large ZIM file to properly test the scenario
      downloadingZimFile = getDownloadingZimFileFromDataFolder()
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
      UiThreadStatement.runOnUiThread {
        customMainActivity.navigate(customMainActivity.readerFragmentRoute)
      }
      openZimFileInReader(zimFile = downloadingZimFile)
      openSearchWithQuery(searchTerms[0])
      // wait for searchFragment become visible on screen.
      delay(2000)
      val searchViewModel = ViewModelProvider(
        customMainActivity,
        customMainActivity.viewModelFactory
      )[SearchViewModel::class.java]
      for (i in 1..100) {
        // This will execute the render method 100 times frequently.
        val searchTerm = searchTerms[i % searchTerms.size]
        searchViewModel.actions.trySend(Action.Filter(searchTerm)).isSuccess
      }
      for (i in 1..100) {
        // this will execute the render method 100 times with 100MS delay.
        delay(100)
        val searchTerm = searchTerms[i % searchTerms.size]
        searchViewModel.actions.trySend(Action.Filter(searchTerm)).isSuccess
      }
      for (i in 1..100) {
        // this will execute the render method 100 times with 200MS delay.
        delay(200)
        val searchTerm = searchTerms[i % searchTerms.size]
        searchViewModel.actions.trySend(Action.Filter(searchTerm)).isSuccess
      }
      for (i in 1..100) {
        // this will execute the render method 100 times with 200MS delay.
        delay(300)
        val searchTerm = searchTerms[i % searchTerms.size]
        searchViewModel.actions.trySend(Action.Filter(searchTerm)).isSuccess
      }
    }

  @Test
  fun testPreviouslyLoadedArticleLoadsAgainWhenSwitchingToAnotherScreen() {
    activityScenario.onActivity {
      customMainActivity = it
    }
    // test with a large ZIM file to properly test the scenario
    downloadingZimFile = getDownloadingZimFileFromDataFolder()
    getOkkHttpClientForTesting().newCall(downloadRequest(rayCharlesZIMFileUrl)).execute()
      .use { response ->
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
    UiThreadStatement.runOnUiThread {
      customMainActivity.navigate(customMainActivity.readerFragmentRoute)
    }
    openZimFileInReader(zimFile = downloadingZimFile)
    search {
      // click on home button to load the main page of ZIM file.
      clickOnHomeButton(composeTestRule)
      // click on an article to load the other page.
      clickOnAFoolForYouArticle(composeTestRule)
      composeTestRule.mainClock.advanceTimeByFrame()
      assertAFoolForYouArticleLoaded(composeTestRule)
      composeTestRule.waitUntilTimeout()
      // open note screen.
      openNoteFragment(customMainActivity as CoreMainActivity, composeTestRule)
      composeTestRule.waitUntilTimeout()
      composeTestRule.onNodeWithTag(NAVIGATION_ICON_TESTING_TAG).performClick()
      // after came back check the previously loaded article is still showing or not.
      assertAFoolForYouArticleLoaded(composeTestRule)
    }
  }

  private fun openSearchWithQuery(query: String = "") {
    UiThreadStatement.runOnUiThread {
      customMainActivity.openSearch(searchString = query)
    }
  }

  private fun openZimFileInReader(
    assetFileDescriptor: AssetFileDescriptor? = null,
    zimFile: File? = null
  ) {
    UiThreadStatement.runOnUiThread {
      val customReaderFragment =
        customMainActivity.supportFragmentManager.fragments
          .filterIsInstance<CustomReaderFragment>()
          .firstOrNull()
      runBlocking {
        assetFileDescriptor?.let {
          customReaderFragment?.openZimFile(
            ZimReaderSource(assetFileDescriptorList = listOf(assetFileDescriptor)),
            true
          )
        } ?: run {
          customReaderFragment?.openZimFile(
            ZimReaderSource(zimFile),
            true
          )
        }
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

  private fun downloadRequest(zimUrl: String = scientificAllianceZIMUrl) =
    Request.Builder()
      .url(URI.create(zimUrl).toURL())
      .build()

  private fun getDownloadingZimFileFromDataFolder(): File {
    val zimFile = File(context.getExternalFilesDirs(null)[0], "ray_charles.zim")
    if (zimFile.exists()) zimFile.delete()
    zimFile.createNewFile()
    return zimFile
  }

  @Singleton
  private fun getOkkHttpClientForTesting(): OkHttpClient =
    OkHttpClient().newBuilder()
      .followRedirects(true)
      .followSslRedirects(true)
      .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.SECONDS)
      .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
      .callTimeout(CALL_TIMEOUT, TimeUnit.SECONDS)
      .addNetworkInterceptor(UserAgentInterceptor(USER_AGENT))
      .build()

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
