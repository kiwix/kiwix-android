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

import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.accessibility.AccessibilityChecks
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesCheck
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesViews
import com.google.android.apps.common.testing.accessibility.framework.checks.TouchTargetSizeCheck
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.main.CoreReaderFragment
import org.kiwix.kiwixmobile.core.page.bookmark.adapter.LibkiwixBookmarkItem
import org.kiwix.kiwixmobile.core.utils.LanguageUtils.Companion.handleLocaleChange
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.main.topLevel
import org.kiwix.kiwixmobile.nav.destination.library.LocalLibraryFragmentDirections
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.libkiwix.Book
import org.kiwix.libkiwix.Bookmark
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class LibkiwixBookmarkTest : BaseActivityTest() {

  @Rule
  @JvmField
  var retryRule = RetryRule()

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
        allOf(
          matchesCheck(TouchTargetSizeCheck::class.java),
          matchesViews(
            withContentDescription("More options")
          )
        )
      )
    }
  }

  @Test
  fun testBookmarks() {
    activityScenario.onActivity {
      kiwixMainActivity = it
      kiwixMainActivity.navigate(R.id.libraryFragment)
      kiwixMainActivity.navigate(
        LocalLibraryFragmentDirections.actionNavigationLibraryToNavigationReader()
          .apply { zimFileUri = getZimFile().toUri().toString() }
      )
    }
    bookmarks {
      // delete any bookmark if already saved to properly perform this test case.
      longClickOnSaveBookmarkImage()
      clickOnTrashIcon()
      assertDeleteBookmarksDialogDisplayed()
      clickOnDeleteButton()
      assertNoBookMarkTextDisplayed()
      pressBack()
      // Test saving bookmark
      clickOnSaveBookmarkImage()
      clickOnOpenSavedBookmarkButton()
      assertBookmarkSaved()
      pressBack()
      // Test removing bookmark
      clickOnSaveBookmarkImage()
      longClickOnSaveBookmarkImage()
      assertBookmarkRemoved()
      pressBack()
      // Save the bookmark to test whether it remains saved after the application restarts or not.
      clickOnSaveBookmarkImage()
    }
  }

  @Test
  fun testBookmarkRemainsSavedOrNot() {
    topLevel {
      clickBookmarksOnNavDrawer(BookmarksRobot::assertBookmarkSaved)
    }
  }

  @Test
  fun testSavedBookmarksShowingOnBookmarkScreen() {
    val zimFile = getZimFile()
    activityScenario.onActivity {
      kiwixMainActivity = it
      kiwixMainActivity.navigate(R.id.libraryFragment)
      kiwixMainActivity.navigate(
        LocalLibraryFragmentDirections.actionNavigationLibraryToNavigationReader()
          .apply { zimFileUri = zimFile.toUri().toString() }
      )
    }
    bookmarks {
      // delete any bookmark if already saved to properly perform this test case.
      longClickOnSaveBookmarkImage()
      clickOnTrashIcon()
      assertDeleteBookmarksDialogDisplayed()
      clickOnDeleteButton()
      assertNoBookMarkTextDisplayed()
      pressBack()
    }
    val navHostFragment: NavHostFragment =
      kiwixMainActivity.supportFragmentManager
        .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
    val coreReaderFragment = navHostFragment.childFragmentManager.fragments[0] as CoreReaderFragment
    val libKiwixBook = Book().apply {
      update(coreReaderFragment.zimReaderContainer?.zimFileReader?.jniKiwixReader)
    }
    val bookmarkList = arrayListOf<LibkiwixBookmarkItem>()
    for (i in 1..500) {
      val bookmark = Bookmark().apply {
        bookId = coreReaderFragment.zimReaderContainer?.zimFileReader?.id
        title = "bookmark$i"
        url = "http://kiwix.org/demoBookmark$i"
        bookTitle = libKiwixBook.title
      }
      val libkiwixItem =
        LibkiwixBookmarkItem(
          bookmark,
          coreReaderFragment.zimReaderContainer?.zimFileReader?.favicon,
          coreReaderFragment.zimReaderContainer?.zimFileReader?.zimReaderSource
        )
      runBlocking {
        coreReaderFragment.libkiwixBookmarks?.saveBookmark(libkiwixItem).also {
          bookmarkList.add(libkiwixItem)
        }
      }
    }
    bookmarks {
      // test all the saved bookmarks are showing on the bookmarks screen
      openBookmarkScreen()
      testAllBookmarkShowing(bookmarkList)
    }
  }

  private fun getZimFile(): File {
    val loadFileStream =
      LibkiwixBookmarkTest::class.java.classLoader.getResourceAsStream("testzim.zim")
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
}
