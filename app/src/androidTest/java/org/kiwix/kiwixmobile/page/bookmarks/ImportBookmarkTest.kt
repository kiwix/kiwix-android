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

package org.kiwix.kiwixmobile.page.bookmarks

import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.accessibility.AccessibilityChecks
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import io.objectbox.BoxStore
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookmarks
import org.kiwix.kiwixmobile.core.dao.NewBookDao
import org.kiwix.kiwixmobile.core.dao.entities.BookOnDiskEntity
import org.kiwix.kiwixmobile.core.di.modules.DatabaseModule
import org.kiwix.kiwixmobile.core.page.bookmark.adapter.LibkiwixBookmarkItem
import org.kiwix.kiwixmobile.core.utils.LanguageUtils
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.libkiwix.Library
import org.kiwix.libkiwix.Manager
import java.io.File

class ImportBookmarkTest : BaseActivityTest() {

  @Rule
  @JvmField
  var retryRule = RetryRule()

  private val boxStore: BoxStore? = DatabaseModule.boxStore
  private val library = Library()
  private val manager = Manager(library)
  private val newBookDao = NewBookDao(boxStore!!.boxFor(BookOnDiskEntity::class.java))
  private lateinit var libkiwixBookmarks: LibkiwixBookmarks

  private val bookmarkXmlData = """
        <bookmarks>
          <bookmark>
            <book>
              <id>1f88ab6f-c265-b3ff-8f49-b7f442950380</id>
              <title>Alpine Linux Wiki</title>
              <name>alpinelinux_en_all</name>
              <flavour>maxi</flavour>
              <language>eng</language>
              <date>2023-01-18</date>
            </book>
            <title>Main Page</title>
            <url>https://kiwix.app/A/Main_Page</url>
          </bookmark>
          <bookmark>
            <book>
              <id>1f88ab6f-c265-b3ff-8f49-b7f442950380</id>
              <title>Alpine Linux Wiki</title>
              <name>alpinelinux_en_all</name>
              <flavour>maxi</flavour>
              <language>eng</language>
              <date>2023-01-18</date>
            </book>
            <title>Installation</title>
            <url>https://kiwix.app/A/Installation</url>
          </bookmark>
          <bookmark>
            <book>
              <id>04bf4329-9bfb-3681-03e2-cfae7b047f24</id>
              <title>Ray Charles</title>
              <name>wikipedia_en_ray_charles</name>
              <flavour>maxi</flavour>
              <language>eng</language>
              <date>2024-03-17</date>
            </book>
            <title>Wikipedia</title>
            <url>https://kiwix.app/A/index</url>
          </bookmark>
        </bookmarks>
  """.trimIndent()

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
        LanguageUtils.handleLocaleChange(
          it,
          "en",
          SharedPreferenceUtil(context)
        )
      }
    }
    libkiwixBookmarks =
      LibkiwixBookmarks(
        library,
        manager,
        SharedPreferenceUtil(context),
        newBookDao,
        null
      )
  }

  init {
    AccessibilityChecks.enable().setRunChecksFromRootView(true)
  }

  @Test
  fun importBookmark() = runBlocking {
    // clear the bookmarks to perform tes case properly.
    clearBookmarks()
    // test with empty data file
    var tempBookmarkFile = getTemporaryBookmarkFile(true)
    importBookmarks(tempBookmarkFile)
    var actualDataAfterImporting = libkiwixBookmarks.bookmarks().blockingFirst()
    assertEquals(0, actualDataAfterImporting.size)

    // import the bookmark
    tempBookmarkFile = getTemporaryBookmarkFile()
    importBookmarks(tempBookmarkFile)
    actualDataAfterImporting = libkiwixBookmarks.bookmarks().blockingFirst()
    assertEquals(3, actualDataAfterImporting.size)
    assertEquals(actualDataAfterImporting[0].title, "Main Page")
    assertEquals(actualDataAfterImporting[0].url, "https://kiwix.app/A/Main_Page")
    assertEquals(actualDataAfterImporting[0].zimId, "1f88ab6f-c265-b3ff-8f49-b7f442950380")

    // import duplicate bookmarks
    importBookmarks(tempBookmarkFile)
    actualDataAfterImporting = libkiwixBookmarks.bookmarks().blockingFirst()
    assertEquals(3, actualDataAfterImporting.size)

    // delete the temp file
    if (tempBookmarkFile.exists()) tempBookmarkFile.delete()
  }

  private fun importBookmarks(tempBookmarkFile: File) {
    activityScenario.onActivity {
      runBlocking {
        libkiwixBookmarks.importBookmarks(tempBookmarkFile)
      }
    }
  }

  private fun clearBookmarks() {
    // delete bookmarks for testing other edge cases
    libkiwixBookmarks.deleteBookmarks(
      libkiwixBookmarks.bookmarks()
        .blockingFirst() as List<LibkiwixBookmarkItem>
    )
  }

  private fun getTemporaryBookmarkFile(isWithEmptyData: Boolean = false): File =
    File(context.externalCacheDir, "bookmark.xml").apply {
      if (exists()) delete()
      createNewFile()

      if (!isWithEmptyData) {
        // Write the XML data to the temp file
        writeText(bookmarkXmlData)
      }
    }
}
