/*
 * Kiwix Android
 * Copyright (c) 2023 Kiwix <android.kiwix.org>
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

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.internal.runner.junit4.statement.UiThreadStatement
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.DuplicateClickableBoundsCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.SpeakableTextPresentCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.TouchTargetSizeCheck
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import leakcanary.LeakAssertions
import okhttp3.Request
import okhttp3.ResponseBody
import org.hamcrest.Matchers.anyOf
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.Assertions
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.main.KiwixWebView
import org.kiwix.kiwixmobile.core.utils.TestingUtils.COMPOSE_TEST_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.TestingUtils.RETRY_RULE_ORDER
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.main.topLevel
import org.kiwix.kiwixmobile.page.bookmarks.bookmarks
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.testutils.TestUtils.FIVE_SECOND_DELAY
import org.kiwix.kiwixmobile.testutils.TestUtils.getOkkHttpClientForTesting
import org.kiwix.kiwixmobile.testutils.TestUtils.getZimFileFromResourceFolder
import org.kiwix.kiwixmobile.testutils.TestUtils.testFlakyView
import org.kiwix.kiwixmobile.testutils.TestUtils.waitUntilTimeout
import org.kiwix.kiwixmobile.ui.KiwixDestination
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class KiwixReaderScreenTest : BaseActivityTest() {
  @Rule(order = RETRY_RULE_ORDER)
  @JvmField
  val retryRule = RetryRule()

  @Rule(order = COMPOSE_TEST_RULE_ORDER)
  @JvmField
  val composeTestRule = createComposeRule()
  private lateinit var kiwixMainActivity: KiwixMainActivity
  private val rayCharlesZimFileUrl =
    "https://dev.kiwix.org/kiwix-android/test/wikipedia_en_ray_charles_maxi_2023-12.zim"

  @Before
  override fun waitForIdle() {
    super.waitForIdle()
    launchMainActivity()
    composeTestRule.enableAccessibilityChecks(createAccessibilityValidator())
  }

  override fun createAccessibilityValidator(): AccessibilityValidator =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      AccessibilityValidator()
        .setRunChecksFromRootView(true)
        .apply {
          setSuppressingResultMatcher(
            anyOf(
              matchesCheck(DuplicateClickableBoundsCheck::class.java),
              matchesCheck(SpeakableTextPresentCheck::class.java),
              matchesCheck(TouchTargetSizeCheck::class.java)
            )
          )
        }
    } else {
      super.createAccessibilityValidator()
    }

  @Test
  fun testTabsRestoredAfterNavigatingLeftDrawerScreens() {
    activityScenario.onActivity {
      kiwixMainActivity = it
      kiwixMainActivity.navigate(KiwixDestination.Library.route)
    }
    composeTestRule.waitForIdle()
    val zimFile = getZimFileFromResourceFolder(context, "testzim.zim")
    openKiwixReaderScreenWithFile(zimFile)
    reader {
      checkZimFileLoadedSuccessful(composeTestRule, "Android_(operating_system)")
      // open a new tab
      openSearchWithQuery("Android", zimFile)
      openAndroidArticleInNewTab(composeTestRule)
      checkZimFileLoadedSuccessful(composeTestRule, "Android_(operating_system)")
      // Wait a bit to properly saving the history.
      composeTestRule.waitUntilTimeout(FIVE_SECOND_DELAY)
      // open bookmark screen.
      bookmarks {
        openBookmarkScreen(kiwixMainActivity as CoreMainActivity, composeTestRule)
        assertBookMarksDisplayed(composeTestRule)
      }
      composeTestRule.waitForIdle()
      // Click on back button showing in navigation bar to come back to previous screen.
      clickOnNavigationIcon(composeTestRule)
      checkZimFileLoadedSuccessful(composeTestRule, "Android_(operating_system)")
      assertTabsRestored(composeTestRule)
    }
  }

  @Test
  fun testTabsRestoredWhenNavigatingToOtherScreenViaBottomAppBar() {
    activityScenario.onActivity {
      kiwixMainActivity = it
      kiwixMainActivity.navigate(KiwixDestination.Library.route)
    }
    composeTestRule.waitForIdle()
    val zimFile = getZimFileFromResourceFolder(context, "testzim.zim")
    openKiwixReaderScreenWithFile(zimFile)
    reader {
      checkZimFileLoadedSuccessful(composeTestRule, "Android_(operating_system)")
      // open a new tab
      openSearchWithQuery("Android", zimFile)
      openAndroidArticleInNewTab(composeTestRule)
      checkZimFileLoadedSuccessful(composeTestRule, "Android_(operating_system)")
      // Wait a bit to properly saving the history.
      composeTestRule.waitUntilTimeout(FIVE_SECOND_DELAY)
      // open local library screen.
      openLocalLibraryScreenViaBottomAppBar(composeTestRule)
      composeTestRule.waitForIdle()
      // click reader bottomAppBar icon to come back to reader screen.
      clickOnReaderScreenInBottomAppBar(composeTestRule)
      checkZimFileLoadedSuccessful(composeTestRule, "Android_(operating_system)")
      assertTabsRestored(composeTestRule)
    }
  }

  @Test
  fun testTabClosedDialog() {
    activityScenario.onActivity {
      kiwixMainActivity = it
      kiwixMainActivity.navigate(KiwixDestination.Library.route)
    }
    composeTestRule.waitForIdle()
    openKiwixReaderScreenWithFile(getZimFileFromResourceFolder(context, "testzim.zim"))
    composeTestRule.waitForIdle()
    reader {
      checkZimFileLoadedSuccessful(composeTestRule)
      clickOnTabIcon(composeTestRule)
      clickOnClosedAllTabsButton(composeTestRule)
      clickOnUndoButton(composeTestRule)
      assertTabRestored(composeTestRule)
      pressBack()
      checkZimFileLoadedSuccessful(composeTestRule)
    }
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1 && Build.VERSION.SDK_INT != Build.VERSION_CODES.TIRAMISU) {
      // temporary disabled on Android 25
      LeakAssertions.assertNoLeaks()
    }
  }

  @Test
  fun testZimFileRendering() {
    activityScenario.onActivity {
      kiwixMainActivity = it
      kiwixMainActivity.navigate(KiwixDestination.Library.route)
    }
    composeTestRule.waitForIdle()
    var downloadingZimFile: File? = null
    testFlakyView({
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
    })
    openKiwixReaderScreenWithFile(downloadingZimFile!!)
    composeTestRule.waitForIdle()
    reader {
      checkZimFileLoadedSuccessful(composeTestRule)
      clickOnTabIcon(composeTestRule)
      clickOnTabIcon(composeTestRule)
      // test the whole welcome page is loaded or not
      assertArticleLoaded("Hydrogène")
      assertArticleLoaded("Automobile")
      assertArticleLoaded("Agriculture")
      assertArticleLoaded("Dioxyde de carbone")
      assertArticleLoaded("Développement durable")
      assertArticleLoaded("Précipitations")
      assertArticleLoaded("Énergie renouvelable")
      assertArticleLoaded("Cyclone tropical")
      assertArticleLoaded("Charbon")
      assertArticleLoaded("Riz")
      assertArticleLoaded("Fromage")
      assertArticleLoaded("Gaz naturel")
      assertArticleLoaded("Transport en commun")
      assertArticleLoaded("Inondation")
      assertArticleLoaded("Ammoniac")
      assertArticleLoaded("Énergie hydroélectrique")
      assertArticleLoaded("Nuage")
      assertArticleLoaded("Essence (hydrocarbure)")
      assertArticleLoaded("Glacier")
      assertArticleLoaded("Ciment")
      assertArticleLoaded("Canicule")
      assertArticleLoaded("Énergie éolienne")
      assertArticleLoaded("Ours blanc")
      assertArticleLoaded("Camion")
      assertArticleLoaded("Glaciation")
      assertArticleLoaded("Engrais")
      assertArticleLoaded("Greenpeace")
      assertArticleLoaded("Déforestation")
      assertArticleLoaded("Bos taurus")
      assertArticleLoaded("Agriculteur")
      assertArticleLoaded("Baleine")
      assertArticleLoaded("Catastrophe naturelle")
      assertArticleLoaded("Tropique")
      assertArticleLoaded("Irrigation")
      assertArticleLoaded("Classification de Köppen")
      assertArticleLoaded("Effet de serre")
      assertArticleLoaded("Géothermie")
      assertArticleLoaded("Combustible fossile")
      assertArticleLoaded("Tourbe")
      assertArticleLoaded("Chanvre")
      assertArticleLoaded("Greta Thunberg")
      assertArticleLoaded("Zone humide")
      assertArticleLoaded("Al Gore")
      assertArticleLoaded("Albédo")
      // click on a article and see it is loaded or not
      clickOnArticle("Transport en commun")
      assertArticleLoaded("transport en commun")
    }
  }

  @Test
  fun testReadAloudFeature() {
    Assume.assumeTrue("Text-to-speech is not available on this device", isTextToSpeechAvailable())
    activityScenario.onActivity {
      kiwixMainActivity = it
      kiwixMainActivity.navigate(KiwixDestination.Library.route)
    }
    composeTestRule.waitForIdle()
    var downloadingZimFile: File? = null
    testFlakyView({
      downloadingZimFile = getDownloadingZimFile()
      getOkkHttpClientForTesting().newCall(downloadRequest(rayCharlesZimFileUrl)).execute()
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
    })
    openKiwixReaderScreenWithFile(downloadingZimFile!!)
    composeTestRule.waitForIdle()
    reader {
      startReadAloudFeature(composeTestRule)
      // Open history screen.
      topLevel {
        clickHistoryOnSideNav(kiwixMainActivity, composeTestRule) {
          clickOnHistoryItem(composeTestRule)
          startReadAloudFeature(composeTestRule)
        }
      }
    }
  }

  @Test
  fun testBase64ImageSaving() {
    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1) return
    activityScenario.onActivity {
      kiwixMainActivity = it
      kiwixMainActivity.navigate(KiwixDestination.Library.route)
    }
    composeTestRule.waitForIdle()
    val zimFile = getZimFileFromResourceFolder(context, "testzim.zim")
    openKiwixReaderScreenWithFile(zimFile)
    composeTestRule.waitForIdle()
    reader {
      checkZimFileLoadedSuccessful(composeTestRule)
    }
    val base64Src =
      "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR4nGNgYGBgAAAABAABJzQnCgAAAABJRU5ErkJggg=="

    val msg = Message.obtain().apply {
      data = Bundle().apply {
        putString("src", base64Src)
        putString("url", null)
      }
    }
    val testComponent = testComponent()
    val saveHandler = KiwixWebView.SaveHandler(
      testComponent.zimReaderContainer(),
      testComponent.provideMainDispatcher(),
      testComponent.provideIoDispatcher()
    )

    // Must run on main thread because Handler uses MainLooper
    InstrumentationRegistry.getInstrumentation().runOnMainSync {
      saveHandler.handleMessage(msg)
    }
    val savedFile = waitForDownloadedImageFile()

    Assertions.assertNotNull(savedFile)
    Assertions.assertTrue(savedFile.exists())
    Assertions.assertTrue(savedFile.length() > 0)
    Assertions.assertTrue(savedFile.extension == "png")
  }

  private fun waitForDownloadedImageFile(): File {
    repeat(20) {
      val projection = arrayOf(
        MediaStore.Images.Media.DATA
      )

      val selection = "${MediaStore.Images.Media.RELATIVE_PATH}=?"
      val selectionArgs = arrayOf("Pictures/Kiwix/")

      val cursor = context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        null
      )

      cursor?.use {
        if (it.moveToFirst()) {
          val index = it.getColumnIndex(MediaStore.Images.Media.DATA)
          if (index != -1) {
            val path = it.getString(index)
            return@waitForDownloadedImageFile File(path)
          }
        }
      }
      Thread.sleep(500)
    }
    throw AssertionError("Base64 image was not saved")
  }

  private fun ReaderRobot.startReadAloudFeature(composeTestRule: ComposeContentTestRule) {
    checkZimFileLoadedSuccessful(composeTestRule)
    clickOnReadAloudMenuItem(composeTestRule)
    try {
      assertTTSLanguageIsNotSupportedDialogDisplayed(composeTestRule)
      clickOnCancelButton(composeTestRule)
    } catch (_: ComposeTimeoutException) {
      assertTTSControlsVisible(composeTestRule)
      clickOnTTSStopButton(composeTestRule)
    }
  }

  /**
   * Check whether the device has a TextToSpeech service available. This is important because
   * default APIs emulators does not have TTS installed, so running on those devices will fail the test.
   */
  private fun hasTextToSpeechService(): Boolean {
    val intent = Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE)

    return context.packageManager
      .queryIntentServices(intent, 0)
      .isNotEmpty()
  }

  /**
   * Check whether the device has a TextToSpeech service available and can be initialized successfully.
   * This is important because default APIs emulators does not have TTS installed, so running on those devices will fail the test.
   * @return true if TTS service is available and can be initialized successfully, false otherwise.
   */
  private fun isTextToSpeechAvailable(): Boolean {
    if (!hasTextToSpeechService()) {
      return false
    }
    val latch = CountDownLatch(1)
    var textToSpeech: TextToSpeech? = null
    var initializationSuccessful = false

    return try {
      textToSpeech = TextToSpeech(context) { status ->
        initializationSuccessful = status == TextToSpeech.SUCCESS
        latch.countDown()
      }

      latch.await(5, TimeUnit.SECONDS) && initializationSuccessful
    } catch (_: Exception) {
      false
    } finally {
      textToSpeech?.shutdown()
    }
  }

  private fun downloadRequest(zimUrl: String = "https://download.kiwix.org/zim/wikipedia_fr_climate-change_mini.zim") =
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

  private fun openKiwixReaderScreenWithFile(zimFile: File) {
    composeTestRule.runOnUiThread {
      kiwixMainActivity.openZimFromFilePath(zimFile.absolutePath)
    }
    composeTestRule.waitForIdle()
  }

  @After
  fun finish() {
    TestUtils.deleteTemporaryFilesOfTestCases(context)
  }
}

fun SemanticsNodeInteraction.getText(): String {
  return fetchSemanticsNode()
    .config[SemanticsProperties.Text]
    .joinToString("") { it.text }
}
