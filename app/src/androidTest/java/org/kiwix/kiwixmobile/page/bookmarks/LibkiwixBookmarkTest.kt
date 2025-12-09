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
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.navigation.NavOptions
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.DuplicateClickableBoundsCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.SpeakableTextPresentCheck
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.anyOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.setNavigationResultOnCurrent
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.main.ZIM_FILE_URI_KEY
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderFragment
import org.kiwix.kiwixmobile.core.page.bookmark.adapter.LibkiwixBookmarkItem
import org.kiwix.kiwixmobile.core.ui.components.NAVIGATION_ICON_TESTING_TAG
import org.kiwix.kiwixmobile.core.utils.LanguageUtils.Companion.handleLocaleChange
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.TestingUtils.COMPOSE_TEST_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.TestingUtils.RETRY_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.main.topLevel
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.testutils.TestUtils.TEST_PAUSE_MS_FOR_DOWNLOAD_TEST
import org.kiwix.kiwixmobile.testutils.TestUtils.TEST_PAUSE_MS_FOR_SNACKBAR
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
    kiwixMainActivity = composeTestRule.activity

    composeTestRule.apply {
      runOnUiThread {
        runBlocking {
          handleLocaleChange(
            kiwixMainActivity,
            "en",
            kiwixDataStore
          )
        }
      }
      waitForIdle()
    }
    val accessibilityValidator = AccessibilityValidator().setRunChecksFromRootView(true).apply {
      setSuppressingResultMatcher(
        anyOf(
          matchesCheck(DuplicateClickableBoundsCheck::class.java),
          matchesCheck(SpeakableTextPresentCheck::class.java)
        )
      )
    }
    composeTestRule.enableAccessibilityChecks(accessibilityValidator)
  }

  private fun waitComposeToSettleViews() {
    composeTestRule.apply {
      waitForIdle()
      waitUntilTimeout()
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

  @Test
  fun testBookmarks() {
    // Open a ZIM file and ensure the reader screen is initialized.
    openZimFileInReader()
    bookmarks {
      // Ensure a clean starting state by removing any previously saved bookmarks
      deletePreviouslySavedBookmarks()

      // Save current page as a bookmark
      clickOnSaveBookmarkImage(composeTestRule)
      waitComposeToSettleViews()

      // Verify bookmark appears in the bookmark list
      openBookmarkScreen(kiwixMainActivity as CoreMainActivity, composeTestRule)
      assertBookmarkSaved(composeTestRule)
      waitComposeToSettleViews()

      // Open the saved bookmark from the list and verify it loads correctly in the reader
      openBookmarkInReader(composeTestRule)
      waitComposeToSettleViews()
      assertZimFileLoadedIntoTheReader(composeTestRule)

      // Ensure the bookmark toggle reflects the saved state.
      assertEquals(true, isBookmarked())
      waitComposeToSettleViews()

      // Remove the bookmark and verify it is removed from the reader and the bookmark list.
      clickOnSaveBookmarkImage(composeTestRule)
      composeTestRule.waitUntilTimeout(TEST_PAUSE_MS_FOR_SNACKBAR)
      assertEquals(false, isBookmarked())
      longClickOnSaveBookmarkImage(composeTestRule, TEST_PAUSE_MS_FOR_DOWNLOAD_TEST.toLong())
      assertNoBookMarkTextDisplayed(composeTestRule)
      pressBack()
      waitComposeToSettleViews()
      clickOnSaveBookmarkImage(composeTestRule)
      // Verify going to other pages does not affect the saved
      // bookmark(Test scenario of custom apps where ZIM file is already opened
      // in reader and user navigate back to reader).
      topLevel {
        // open settings screen
        clickSettingsOnSideNav(kiwixMainActivity as CoreMainActivity, composeTestRule, true) {
          composeTestRule.onNodeWithTag(NAVIGATION_ICON_TESTING_TAG).performClick()
          waitComposeToSettleViews()
          assertZimFileLoadedIntoTheReader(composeTestRule)
          assertEquals(true, isBookmarked())
        }
      }

      // Verify saved bookmark properly opened in reader(If some other article is opened in reader).
      clickOnSaveBookmarkImage(composeTestRule)
      composeTestRule.waitUntilTimeout(TEST_PAUSE_MS_FOR_SNACKBAR)
      assertZimFileLoadedIntoTheReader(composeTestRule)
      clickOnAndroidArticle(composeTestRule)
      waitComposeToSettleViews()
      assertAndroidArticleLoadedInReader(composeTestRule)
      waitComposeToSettleViews()
      clickOnSaveBookmarkImage(composeTestRule)
      clickOnBackwardButton(composeTestRule)
      openBookmarkScreen(kiwixMainActivity as CoreMainActivity, composeTestRule)
      openBookmarkInReader(composeTestRule)
      waitComposeToSettleViews()
      assertAndroidArticleLoadedInReader(composeTestRule)
      assertEquals(true, isBookmarked())
      clickOnSaveBookmarkImage(composeTestRule)
      composeTestRule.waitUntilTimeout(TEST_PAUSE_MS_FOR_SNACKBAR)

      // Save again and verify bookmark persistence after app restart.
      clickOnHomeButton(composeTestRule)
      waitComposeToSettleViews()
      clickOnSaveBookmarkImage(composeTestRule)
      waitComposeToSettleViews()
      InstrumentationRegistry.getInstrumentation().uiAutomation.performGlobalAction(
        AccessibilityService.GLOBAL_ACTION_HOME
      )
      waitComposeToSettleViews()

      val context = ApplicationProvider.getApplicationContext<Context>()
      val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
      intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
      context.startActivity(intent)
      InstrumentationRegistry.getInstrumentation().waitForIdleSync()
      waitComposeToSettleViews()
      assertZimFileLoadedIntoTheReader(composeTestRule)
      assertEquals(true, isBookmarked())
      topLevel {
        // Verify the bookmark appears in the bookmark screen.
        clickBookmarksOnNavDrawer(kiwixMainActivity as CoreMainActivity, composeTestRule) {
          assertBookmarkSaved(composeTestRule)
        }
      }
    }
  }

  private fun isBookmarked() = kiwixMainActivity.supportFragmentManager.fragments
    .filterIsInstance<CoreReaderFragment>()
    .firstOrNull()
    ?.getIsBookmarked() ?: false

  private fun deletePreviouslySavedBookmarks() {
    bookmarks {
      openBookmarkScreen(kiwixMainActivity as CoreMainActivity, composeTestRule)
      clickOnTrashIcon(composeTestRule)
      assertDeleteBookmarksDialogDisplayed(composeTestRule)
      clickOnDeleteButton(composeTestRule)
      assertNoBookMarkTextDisplayed(composeTestRule)
      pressBack()
      waitComposeToSettleViews()
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
        kiwixMainActivity.apply {
          kiwixMainActivity.navigate(KiwixDestination.Reader.route, navOptions)
          setNavigationResultOnCurrent(zimFile.toUri().toString(), ZIM_FILE_URI_KEY)
        }
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
