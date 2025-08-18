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

package org.kiwix.kiwixmobile.page.bookmarks

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.navigation.NavOptions
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.accessibility.AccessibilityChecks
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesCheck
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesViews
import com.google.android.apps.common.testing.accessibility.framework.checks.SpeakableTextPresentCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.TouchTargetSizeCheck
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.anyOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderFragment
import org.kiwix.kiwixmobile.core.page.bookmark.adapter.LibkiwixBookmarkItem
import org.kiwix.kiwixmobile.core.utils.LanguageUtils.Companion.handleLocaleChange
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.TestingUtils.COMPOSE_TEST_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.TestingUtils.RETRY_RULE_ORDER
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.main.topLevel
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.testutils.TestUtils.TEST_PAUSE_MS_FOR_DOWNLOAD_TEST
import org.kiwix.kiwixmobile.testutils.TestUtils.waitUntilTimeout
import org.kiwix.kiwixmobile.ui.KiwixDestination
import org.kiwix.libkiwix.Book
import org.kiwix.libkiwix.Bookmark
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class LibkiwixBookmarkTest : BaseActivityTest() {
  @Rule(order = RETRY_RULE_ORDER)
  @JvmField
  val retryRule = RetryRule()

  @get:Rule(order = COMPOSE_TEST_RULE_ORDER)
  val composeTestRule = createAndroidComposeRule<KiwixMainActivity>()

  private lateinit var kiwixMainActivity: KiwixMainActivity

  @Before
  override fun waitForIdle() {
    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).apply {
      if (TestUtils.isSystemUINotRespondingDialogVisible(this)) {
        TestUtils.closeSystemDialogs(context, this)
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
    kiwixMainActivity = composeTestRule.activity

    composeTestRule.apply {
      runOnUiThread {
        handleLocaleChange(
          kiwixMainActivity,
          "en",
          SharedPreferenceUtil(context)
        )
      }
      waitForIdle()
    }
  }

  init {
    AccessibilityChecks.enable().apply {
      setRunChecksFromRootView(true)
      setSuppressingResultMatcher(
        anyOf(
          allOf(
            matchesCheck(TouchTargetSizeCheck::class.java),
            matchesViews(withContentDescription("More options"))
          ),
          matchesCheck(SpeakableTextPresentCheck::class.java)
        )
      )
    }
  }

  @Test
  fun testBookmarks() {
    openZimFileInReader()
    bookmarks {
      // delete any bookmark if already saved to properly perform this test case.
      longClickOnSaveBookmarkImage(composeTestRule)
      clickOnTrashIcon(composeTestRule)
      assertDeleteBookmarksDialogDisplayed(composeTestRule)
      clickOnDeleteButton(composeTestRule)
      assertNoBookMarkTextDisplayed(composeTestRule)
      pressBack()
      waitComposeToSettleViews()
      // Test saving bookmark
      clickOnSaveBookmarkImage(composeTestRule)
      openBookmarkScreen(kiwixMainActivity as CoreMainActivity, composeTestRule)
      assertBookmarkSaved(composeTestRule)
      pressBack()
      // Test removing bookmark
      waitComposeToSettleViews()
      clickOnSaveBookmarkImage(composeTestRule)
      longClickOnSaveBookmarkImage(composeTestRule, TEST_PAUSE_MS_FOR_DOWNLOAD_TEST.toLong())
      assertBookmarkRemoved(composeTestRule)
      pressBack()
      // Save the bookmark to test whether it remains saved after the application restarts or not.
      waitComposeToSettleViews()
      clickOnSaveBookmarkImage(composeTestRule)
      waitComposeToSettleViews()
      // Close the application.
      InstrumentationRegistry.getInstrumentation().uiAutomation.performGlobalAction(
        AccessibilityService.GLOBAL_ACTION_HOME
      )
      // wait a bit
      waitComposeToSettleViews()
      // reopen the application to test that book remains saved or not.
      val context = ApplicationProvider.getApplicationContext<Context>()
      val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
      intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
      context.startActivity(intent)
      InstrumentationRegistry.getInstrumentation().waitForIdleSync()
      waitComposeToSettleViews()
      topLevel {
        clickBookmarksOnNavDrawer(kiwixMainActivity as CoreMainActivity, composeTestRule) {
          assertBookmarkSaved(composeTestRule)
        }
      }
    }
  }

  private fun waitComposeToSettleViews() {
    composeTestRule.apply {
      waitForIdle()
      waitUntilTimeout()
    }
  }

  @Test
  fun testBookMarkPageOpenInReader() {
    openZimFileInReader()
    bookmarks {
      openBookmarkScreen(kiwixMainActivity as CoreMainActivity, composeTestRule)
      clickOnTrashIcon(composeTestRule)
      assertDeleteBookmarksDialogDisplayed(composeTestRule)
      clickOnDeleteButton(composeTestRule)
      assertNoBookMarkTextDisplayed(composeTestRule)
      pressBack()
      waitComposeToSettleViews() // to properly load the ZIM file in reader.
      assertZimFileLoadedIntoTheReader(composeTestRule)
      clickOnAndroidArticle(composeTestRule)
      waitComposeToSettleViews()
      assertAndroidArticleLoadedInReader(composeTestRule)
      waitComposeToSettleViews()
      // Save bookmark
      clickOnSaveBookmarkImage(composeTestRule)
      // open previous page
      clickOnBackwardButton(composeTestRule)
      // open bookmark screen.
      openBookmarkScreen(kiwixMainActivity as CoreMainActivity, composeTestRule)
      // tries to open the bookmark page in reader.
      openBookmarkInReader(composeTestRule)
      waitComposeToSettleViews()
      assertAndroidArticleLoadedInReader(composeTestRule)
    }
  }

  @Test
  fun testSavedBookmarksShowingOnBookmarkScreen() {
    openZimFileInReader()
    bookmarks {
      // delete any bookmark if already saved to properly perform this test case.
      longClickOnSaveBookmarkImage(composeTestRule)
      clickOnTrashIcon(composeTestRule)
      assertDeleteBookmarksDialogDisplayed(composeTestRule)
      clickOnDeleteButton(composeTestRule)
      assertNoBookMarkTextDisplayed(composeTestRule)
      pressBack()
    }
    waitComposeToSettleViews()
    val coreReaderFragment = kiwixMainActivity.supportFragmentManager.fragments
      .filterIsInstance<CoreReaderFragment>()
      .firstOrNull()
    val libKiwixBook =
      Book().apply {
        update(coreReaderFragment?.zimReaderContainer?.zimFileReader?.jniKiwixReader)
      }
    val bookmarkList = arrayListOf<LibkiwixBookmarkItem>()
    for (i in 1..500) {
      val bookmark =
        Bookmark().apply {
          bookId = coreReaderFragment?.zimReaderContainer?.zimFileReader?.id
          title = "bookmark$i"
          url = "http://kiwix.org/demoBookmark$i"
          bookTitle = libKiwixBook.title
        }
      val libkiwixItem =
        LibkiwixBookmarkItem(
          bookmark,
          coreReaderFragment?.zimReaderContainer?.zimFileReader?.favicon,
          coreReaderFragment?.zimReaderContainer?.zimFileReader?.zimReaderSource
        )
      runBlocking {
        coreReaderFragment?.libkiwixBookmarks?.saveBookmark(libkiwixItem).also {
          bookmarkList.add(libkiwixItem)
        }
      }
    }
    bookmarks {
      // test all the saved bookmarks are showing on the bookmarks screen
      openBookmarkScreen(kiwixMainActivity as CoreMainActivity, composeTestRule)
      testAllBookmarkShowing(bookmarkList, composeTestRule)
    }
  }

  private fun openZimFileInReader() {
    val zimFile = getZimFile()
    composeTestRule.apply {
      runOnUiThread {
        kiwixMainActivity.navigate(KiwixDestination.Library.route)
        val navOptions = NavOptions.Builder()
          .setPopUpTo(KiwixDestination.Reader.route, false)
          .build()
        kiwixMainActivity.navigate(
          KiwixDestination.Reader.createRoute(zimFileUri = zimFile.toUri().toString()),
          navOptions
        )
      }
      waitComposeToSettleViews()
    }
  }

  private fun getZimFile(): File {
    val loadFileStream =
      LibkiwixBookmarkTest::class.java.classLoader.getResourceAsStream("testzim.zim")
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
}
