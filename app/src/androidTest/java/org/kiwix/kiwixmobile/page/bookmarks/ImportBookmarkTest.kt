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
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import io.objectbox.BoxStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookOnDisk
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookmarks
import org.kiwix.kiwixmobile.core.page.bookmark.adapter.LibkiwixBookmarkItem
import org.kiwix.kiwixmobile.core.utils.LanguageUtils.Companion.handleLocaleChange
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.TestingUtils.RETRY_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.migration.di.module.DatabaseModule
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.libkiwix.Library
import org.kiwix.libkiwix.Manager
import java.io.File

class ImportBookmarkTest : BaseActivityTest() {
  @Rule(order = RETRY_RULE_ORDER)
  @JvmField
  val retryRule = RetryRule()

  private var boxStore: BoxStore? = null
  private val library = Library()
  private val manager = Manager(library)
  private lateinit var libkiwixBookOnDisk: LibkiwixBookOnDisk
  private lateinit var libkiwixBookmarks: LibkiwixBookmarks

  private val bookmarkXmlData =
    """
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
    val kiwixDataStore = KiwixDataStore(context).apply {
      lifeCycleScope.launch {
        setWifiOnly(false)
        setIntroShown()
        setPrefLanguage("en")
        setLastDonationPopupShownInMilliSeconds(System.currentTimeMillis())
        setIsScanFileSystemDialogShown(true)
      }
    }
    PreferenceManager.getDefaultSharedPreferences(context).edit {
      putBoolean(SharedPreferenceUtil.PREF_IS_TEST, true)
      putBoolean(SharedPreferenceUtil.PREF_IS_FIRST_RUN, false)
    }
    activityScenario =
      ActivityScenario.launch(KiwixMainActivity::class.java).apply {
        moveToState(Lifecycle.State.RESUMED)
        onActivity {
          runBlocking {
            handleLocaleChange(
              it,
              "en",
              kiwixDataStore
            )
          }
        }
      }
    boxStore = DatabaseModule.boxStore
    val sharedPreferenceUtils = SharedPreferenceUtil(context)
    libkiwixBookOnDisk = LibkiwixBookOnDisk(library, manager, sharedPreferenceUtils)
    libkiwixBookmarks =
      LibkiwixBookmarks(
        library,
        manager,
        sharedPreferenceUtils,
        libkiwixBookOnDisk,
        null
      )
  }

  @Test
  fun importBookmark() =
    runBlocking {
      // clear the bookmarks to perform tes case properly.
      clearBookmarks()
      // test with empty data file
      var tempBookmarkFile = getTemporaryBookmarkFile(true)
      importBookmarks(tempBookmarkFile)
      var actualDataAfterImporting = libkiwixBookmarks.bookmarks().first()
      assertEquals(0, actualDataAfterImporting.size)

      // import the bookmark
      tempBookmarkFile = getTemporaryBookmarkFile()
      importBookmarks(tempBookmarkFile)
      actualDataAfterImporting = libkiwixBookmarks.bookmarks().first()
      assertEquals(3, actualDataAfterImporting.size)
      assertEquals(actualDataAfterImporting[0].title, "Main Page")
      assertEquals(actualDataAfterImporting[0].url, "https://kiwix.app/A/Main_Page")
      assertEquals(actualDataAfterImporting[0].zimId, "1f88ab6f-c265-b3ff-8f49-b7f442950380")

      // import duplicate bookmarks
      importBookmarks(tempBookmarkFile)
      actualDataAfterImporting = libkiwixBookmarks.bookmarks().first()
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

  private suspend fun clearBookmarks() {
    // delete bookmarks for testing other edge cases
    libkiwixBookmarks.deleteBookmarks(
      libkiwixBookmarks.bookmarks()
        .first() as List<LibkiwixBookmarkItem>
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
