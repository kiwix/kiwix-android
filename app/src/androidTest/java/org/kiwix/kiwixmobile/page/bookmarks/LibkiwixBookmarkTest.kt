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
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.core.os.LocaleListCompat
import androidx.navigation.NavOptions
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Before
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.page.bookmark.models.LibkiwixBookmarkItem
import org.kiwix.kiwixmobile.core.ui.components.NAVIGATION_ICON_TESTING_TAG
import org.kiwix.kiwixmobile.core.utils.TestingUtils.COMPOSE_TEST_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.TestingUtils.HILT_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.TestingUtils.RETRY_RULE_ORDER
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.main.topLevel
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils.TEST_PAUSE_MS_FOR_DOWNLOAD_TEST
import org.kiwix.kiwixmobile.testutils.TestUtils.TEST_PAUSE_MS_FOR_SNACKBAR
import org.kiwix.kiwixmobile.testutils.TestUtils.getZimFileFromResourceFolder
import org.kiwix.kiwixmobile.testutils.TestUtils.waitUntilTimeout
import org.kiwix.kiwixmobile.ui.KiwixDestination
import org.kiwix.libkiwix.Book
import org.kiwix.libkiwix.Bookmark

@HiltAndroidTest
class LibkiwixBookmarkTest : BaseActivityTest() {
  @Rule(order = HILT_RULE_ORDER)
  @JvmField
  val hiltRule = HiltAndroidRule(this)

  @Rule(order = RETRY_RULE_ORDER)
  @JvmField
  val retryRule = RetryRule()

  @Rule(order = COMPOSE_TEST_RULE_ORDER)
  @JvmField
  val composeTestRule = createAndroidComposeRule<KiwixMainActivity>()

  @Before
  override fun waitForIdle() {
    hiltRule.injectOnce()
    super.waitForIdle()
    composeTestRule.apply {
      runOnUiThread {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
      }
      waitForIdle()
    }
    composeTestRule.enableAccessibilityChecks(createAccessibilityValidator())
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
    val zimReaderContainer = composeTestRule.activity.zimReaderContainer
    val libKiwixBook =
      Book().apply {
        update(zimReaderContainer.zimFileReader?.jniKiwixReader)
      }
    val bookmarkList = arrayListOf<LibkiwixBookmarkItem>()
    for (i in 1..500) {
      val bookmark =
        Bookmark().apply {
          bookId = zimReaderContainer.zimFileReader?.id
          title = "bookmark$i"
          url = "http://kiwix.org/demoBookmark$i"
          bookTitle = libKiwixBook.title
        }
      val libkiwixItem =
        LibkiwixBookmarkItem(
          bookmark,
          zimReaderContainer.zimFileReader?.favicon,
          zimReaderContainer.zimFileReader?.zimReaderSource
        )
      runBlocking {
        composeTestRule.activity.libkiwixBookmarks.saveBookmark(libkiwixItem).also {
          bookmarkList.add(libkiwixItem)
        }
      }
    }
    bookmarks {
      // test all the saved bookmarks are showing on the bookmarks screen
      openBookmarkScreen(composeTestRule.activity as CoreMainActivity, composeTestRule)
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
      openBookmarkScreen(composeTestRule.activity as CoreMainActivity, composeTestRule)
      assertBookmarkSaved(composeTestRule)
      waitComposeToSettleViews()

      // Open the saved bookmark from the list and verify it loads correctly in the reader
      openBookmarkInReader(composeTestRule)
      waitComposeToSettleViews()
      assertZimFileLoadedIntoTheReader(composeTestRule)

      // Ensure the bookmark toggle reflects the saved state.
      assertBookmarkButtonShowBookmarked(composeTestRule)
      waitComposeToSettleViews()

      // Remove the bookmark and verify it is removed from the reader and the bookmark list.
      clickOnSaveBookmarkImage(composeTestRule)
      composeTestRule.waitUntilTimeout(TEST_PAUSE_MS_FOR_SNACKBAR)
      assertBookmarkButtonShowNotBookmarked(composeTestRule)
      longClickOnSaveBookmarkImage(composeTestRule, TEST_PAUSE_MS_FOR_DOWNLOAD_TEST)
      assertNoBookMarkTextDisplayed(composeTestRule)
      pressBack()
      waitComposeToSettleViews()
      clickOnSaveBookmarkImage(composeTestRule)
      // Verify going to other pages does not affect the saved
      // bookmark(Test scenario of custom apps where ZIM file is already opened
      // in reader and user navigate back to reader).
      topLevel {
        // open settings screen
        clickSettingsOnSideNav(
          composeTestRule.activity as CoreMainActivity,
          composeTestRule,
          true
        ) {
          composeTestRule.onNodeWithTag(NAVIGATION_ICON_TESTING_TAG).performClick()
          waitComposeToSettleViews()
          assertZimFileLoadedIntoTheReader(composeTestRule)
          assertBookmarkButtonShowBookmarked(composeTestRule)
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
      openBookmarkScreen(composeTestRule.activity as CoreMainActivity, composeTestRule)
      openBookmarkInReader(composeTestRule)
      waitComposeToSettleViews()
      assertAndroidArticleLoadedInReader(composeTestRule)
      assertBookmarkButtonShowBookmarked(composeTestRule)
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
      assertBookmarkButtonShowBookmarked(composeTestRule)
      topLevel {
        // Verify the bookmark appears in the bookmark screen.
        clickBookmarksOnNavDrawer(composeTestRule.activity as CoreMainActivity, composeTestRule) {
          assertBookmarkSaved(composeTestRule)
        }
      }
    }
  }

  private fun deletePreviouslySavedBookmarks() {
    bookmarks {
      openBookmarkScreen(composeTestRule.activity as CoreMainActivity, composeTestRule)
      clickOnTrashIcon(composeTestRule)
      assertDeleteBookmarksDialogDisplayed(composeTestRule)
      clickOnDeleteButton(composeTestRule)
      assertNoBookMarkTextDisplayed(composeTestRule)
      pressBack()
      waitComposeToSettleViews()
    }
  }

  private fun openZimFileInReader() {
    val zimFile = getZimFileFromResourceFolder(context, "testzim.zim")

    composeTestRule.runOnUiThread {
      val navOptions = NavOptions.Builder()
        .setPopUpTo(KiwixDestination.Reader.route, false)
        .build()

      composeTestRule.activity.navigate(
        KiwixDestination.Reader.route,
        navOptions
      )
    }

    composeTestRule.runOnUiThread {
      composeTestRule.activity.openZimFromFilePath(zimFile.absolutePath)
    }

    composeTestRule.waitForIdle()
  }
}
