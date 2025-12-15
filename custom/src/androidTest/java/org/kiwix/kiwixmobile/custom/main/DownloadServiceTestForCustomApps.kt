/*
 * Kiwix Android
 * Copyright (c) 2025 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.custom.main

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.os.Build
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.internal.runner.junit4.statement.UiThreadStatement
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.Assertions
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.data.remote.UserAgentInterceptor
import org.kiwix.kiwixmobile.core.di.modules.CALL_TIMEOUT
import org.kiwix.kiwixmobile.core.di.modules.CONNECTION_TIMEOUT
import org.kiwix.kiwixmobile.core.di.modules.READ_TIMEOUT
import org.kiwix.kiwixmobile.core.di.modules.USER_AGENT
import org.kiwix.kiwixmobile.core.downloader.downloadManager.DownloadMonitorService
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.utils.LanguageUtils
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.TestingUtils.COMPOSE_TEST_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.TestingUtils.RETRY_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
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

const val FIVE_SECONDS = 5 * 1000

@RunWith(AndroidJUnit4::class)
class DownloadServiceTestForCustomApps {
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
  private lateinit var activityScenario: ActivityScenario<CustomMainActivity>
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
    val kiwixDataStore = KiwixDataStore(context).apply {
      lifeCycleScope.launch {
        setWifiOnly(false)
        setIntroShown()
        setPrefLanguage("en")
        setLastDonationPopupShownInMilliSeconds(System.currentTimeMillis())
      }
    }
    PreferenceManager.getDefaultSharedPreferences(context).edit {
      putBoolean(SharedPreferenceUtil.PREF_IS_TEST, true)
    }
    activityScenario =
      ActivityScenario.launch(CustomMainActivity::class.java).apply {
        moveToState(Lifecycle.State.RESUMED)
        onActivity {
          runBlocking {
            LanguageUtils.handleLocaleChange(
              it,
              "en",
              kiwixDataStore
            )
          }
        }
      }
  }

  @Test
  fun testDownloadMonitorServiceShouldNotStartForCustomApp() {
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
      activityScenario.onActivity {
        customMainActivity = it
      }
      // test with a large ZIM file to properly test the scenario
      val downloadingZimFile = getDownloadingZimFileFromDataFolder()
      getOkkHttpClientForTesting().newCall(downloadRequest()).execute()
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
      // press the home button so that application goes into background
      InstrumentationRegistry.getInstrumentation().uiAutomation.performGlobalAction(
        AccessibilityService.GLOBAL_ACTION_HOME
      )
      // wait for 5 seconds to check the downloadMonitorService is started or not.
      composeTestRule.waitUntilTimeout(FIVE_SECONDS.toLong())
      // Check the download service is not running.
      Assertions.assertEquals(false, DownloadMonitorService.isDownloadMonitorServiceRunning)
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

  private fun downloadRequest() =
    Request.Builder()
      .url(URI.create(rayCharlesZIMFileUrl).toURL())
      .build()

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

  private fun getDownloadingZimFileFromDataFolder(): File {
    val zimFile = File(context.getExternalFilesDirs(null)[0], "ray_charles.zim")
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
