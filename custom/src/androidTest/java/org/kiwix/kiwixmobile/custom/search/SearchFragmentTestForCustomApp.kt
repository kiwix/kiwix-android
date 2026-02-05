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

import android.content.Context
import android.content.res.AssetFileDescriptor
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.internal.runner.junit4.statement.UiThreadStatement
import androidx.test.platform.app.InstrumentationRegistry
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
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.search.SearchFragment
import org.kiwix.kiwixmobile.core.search.viewmodel.Action
import org.kiwix.kiwixmobile.core.search.viewmodel.SearchViewModel
import org.kiwix.kiwixmobile.core.utils.TestingUtils.COMPOSE_TEST_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.TestingUtils.RETRY_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.custom.main.CustomMainActivity
import org.kiwix.kiwixmobile.custom.main.CustomReaderFragment
import org.kiwix.kiwixmobile.custom.testutils.RetryRule
import org.kiwix.kiwixmobile.custom.testutils.TestUtils
import org.kiwix.kiwixmobile.custom.testutils.TestUtils.closeSystemDialogs
import org.kiwix.kiwixmobile.custom.testutils.TestUtils.isSystemUINotRespondingDialogVisible
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@RunWith(AndroidJUnit4::class)
class SearchFragmentTestForCustomApp {
  private val lifeCycleScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

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

      activityScenario.onActivity { customMainActivity = it }

      downloadingZimFile = getDownloadingZimFileFromDataFolder()
      getOkkHttpClientForTesting().newCall(downloadRequest()).execute().use { response ->
        if (response.isSuccessful) {
          response.body?.let { writeZimFileData(it, downloadingZimFile) }
        }
      }

      UiThreadStatement.runOnUiThread {
        customMainActivity.navigate(customMainActivity.readerFragmentRoute)
      }

      openZimFileInReader(zimFile = downloadingZimFile)
      openSearchWithQuery(searchTerms[0])

      val searchFragment = waitForSearchFragment(customMainActivity)

      val viewModel =
        ViewModelProvider(searchFragment)[SearchViewModel::class.java]

      for (i in 1..100) {
        val term = searchTerms[i % searchTerms.size]
        viewModel.actions.trySend(Action.Filter(term))
      }

      for (i in 1..100) {
        delay(200)
        val term = searchTerms[i % searchTerms.size]
        viewModel.actions.trySend(Action.Filter(term))
      }

      for (i in 1..100) {
        delay(300)
        val term = searchTerms[i % searchTerms.size]
        viewModel.actions.trySend(Action.Filter(term))
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
      val reader =
        customMainActivity.supportFragmentManager.fragments
          .filterIsInstance<CustomReaderFragment>()
          .firstOrNull()

      runBlocking {
        assetFileDescriptor?.let {
          reader?.openZimFile(
            ZimReaderSource(assetFileDescriptorList = listOf(it)),
            true
          )
        } ?: run {
          reader?.openZimFile(
            ZimReaderSource(zimFile),
            true
          )
        }
      }
    }
  }

  private fun writeZimFileData(responseBody: ResponseBody, file: File) {
    FileOutputStream(file).use { output ->
      responseBody.byteStream().use { input ->
        val buffer = ByteArray(4096)
        var read: Int
        while (input.read(buffer).also { read = it } != -1) {
          output.write(buffer, 0, read)
        }
      }
    }
  }

  private fun downloadRequest(zimUrl: String = scientificAllianceZIMUrl) =
    Request.Builder().url(URI.create(zimUrl).toURL()).build()

  private fun getDownloadingZimFileFromDataFolder(): File {
    val file = File(context.getExternalFilesDirs(null)[0], "ray_charles.zim")
    if (file.exists()) file.delete()
    file.createNewFile()
    return file
  }

  @Singleton
  private fun getOkkHttpClientForTesting(): OkHttpClient =
    OkHttpClient.Builder()
      .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.SECONDS)
      .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
      .callTimeout(CALL_TIMEOUT, TimeUnit.SECONDS)
      .addNetworkInterceptor(UserAgentInterceptor(USER_AGENT))
      .build()

  @After
  fun finish() {
    TestUtils.deleteTemporaryFilesOfTestCases(context)
    context.cacheDir?.deleteRecursively()
  }

  private fun waitForSearchFragment(
    activity: FragmentActivity,
    timeoutMs: Long = 5_000
  ): SearchFragment {
    val start = System.currentTimeMillis()
    while (System.currentTimeMillis() - start < timeoutMs) {
      val fragment =
        activity.supportFragmentManager.fragments
          .filterIsInstance<SearchFragment>()
          .firstOrNull { it.isAdded }

      if (fragment != null) return fragment
      Thread.sleep(50)
    }
    error("SearchFragment not found after $timeoutMs ms")
  }
}
