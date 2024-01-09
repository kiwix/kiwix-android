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

package org.kiwix.kiwixmobile

import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import io.objectbox.Box
import io.objectbox.BoxStore
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookmarks
import org.kiwix.kiwixmobile.core.dao.entities.BookmarkEntity
import org.kiwix.kiwixmobile.core.data.remote.ObjectBoxToLibkiwixMigrator
import org.kiwix.kiwixmobile.core.di.modules.DatabaseModule
import org.kiwix.kiwixmobile.core.page.bookmark.adapter.LibkiwixBookmarkItem
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.libkiwix.Book
import org.kiwix.libkiwix.Library
import org.kiwix.libkiwix.Manager
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

@RunWith(AndroidJUnit4::class)
class ObjectBoxToLibkiwixMigratorTest : BaseActivityTest() {
  private val objectBoxToLibkiwixMigrator = ObjectBoxToLibkiwixMigrator()

  // take the existing boxStore object
  private val boxStore: BoxStore? = DatabaseModule.boxStore

  // @Rule
  // @JvmField
  // var retryRule = RetryRule()

  @Before
  override fun waitForIdle() {
    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).apply {
      if (TestUtils.isSystemUINotRespondingDialogVisible(this)) {
        TestUtils.closeSystemDialogs(context)
      }
      waitForIdle()
    }
    PreferenceManager.getDefaultSharedPreferences(context).edit {
      putBoolean(SharedPreferenceUtil.PREF_SHOW_INTRO, false)
      putBoolean(SharedPreferenceUtil.PREF_WIFI_ONLY, false)
      putBoolean(SharedPreferenceUtil.PREF_IS_TEST, true)
      putBoolean(SharedPreferenceUtil.PREF_PLAY_STORE_RESTRICTION, false)
    }
    activityScenario = ActivityScenario.launch(KiwixMainActivity::class.java).apply {
      moveToState(Lifecycle.State.RESUMED)
    }
  }

  @Test
  fun migrateBookmarkTest(): Unit = runBlocking {
    if (boxStore == null) {
      throw RuntimeException(
        "BoxStore is not available for testing," +
          " check is your application running"
      )
    }
    val box = boxStore.boxFor(BookmarkEntity::class.java)
    val library = Library()
    val manager = Manager(library)
    val sharedPreferenceUtil = SharedPreferenceUtil(context)
    objectBoxToLibkiwixMigrator.libkiwixBookmarks =
      LibkiwixBookmarks(library, manager, sharedPreferenceUtil)
    objectBoxToLibkiwixMigrator.sharedPreferenceUtil = SharedPreferenceUtil(context)

    // add a file in fileSystem because we need to actual file path for making object of Archive.
    val loadFileStream =
      ObjectBoxToLibkiwixMigratorTest::class.java.classLoader.getResourceAsStream("testzim.zim")
    val zimFile = File(context.cacheDir, "testzim.zim")
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
    // clear the data before running the test case
    clearBookmarks(box, objectBoxToLibkiwixMigrator.libkiwixBookmarks)
    val expectedZimName = "Alpine_Linux"
    val expectedZimId = "60094d1e-1c9a-a60b-2011-4fb02f8db6c3"
    val expectedZimFilePath = zimFile.canonicalPath
    val expectedTitle = "Installing"
    val expectedBookmarkUrl = "https://alpine_linux/InstallingPage"
    val expectedFavicon = ""
    val bookmarkEntity = BookmarkEntity(
      0,
      expectedZimId,
      expectedZimName,
      expectedZimFilePath,
      expectedBookmarkUrl,
      expectedTitle,
      expectedFavicon
    )
    box.put(bookmarkEntity)
    // migrate data into room database
    objectBoxToLibkiwixMigrator.migrateBookMarks(box)
    BaristaSleepInteractions.sleep(2000L)
    // check if data successfully migrated to room
    val actual = objectBoxToLibkiwixMigrator.libkiwixBookmarks.bookmarks().blockingFirst()
    assertEquals(actual.size, 1)
    assertEquals(actual[0].zimFilePath, expectedZimFilePath)
    assertEquals(actual[0].zimId, expectedZimId)
    assertEquals(actual[0].title, expectedTitle)
    assertEquals(actual[0].url, expectedBookmarkUrl)

    // clear both databases for recent searches to test more edge cases
    clearBookmarks(box, objectBoxToLibkiwixMigrator.libkiwixBookmarks)
    // Migrate data from empty ObjectBox database
    objectBoxToLibkiwixMigrator.migrateBookMarks(box)
    val actualData = objectBoxToLibkiwixMigrator.libkiwixBookmarks.bookmarks().blockingFirst()
    assertTrue(actualData.isEmpty())

    // Test if data successfully migrated to Room and existing data is preserved
    val existingTitle = "Home Page"
    val existingBookmarkUrl = "https://alpine_linux/HomePage"
    val secondBookmarkEntity = BookmarkEntity(
      0,
      expectedZimId,
      expectedZimName,
      expectedZimFilePath,
      existingBookmarkUrl,
      existingTitle,
      expectedFavicon
    )
    val libkiwixBook = Book()
    objectBoxToLibkiwixMigrator.libkiwixBookmarks.saveBookmark(
      LibkiwixBookmarkItem(
        secondBookmarkEntity,
        libkiwixBook
      )
    )
    BaristaSleepInteractions.sleep(2000L)
    box.put(bookmarkEntity)
    // Migrate data into Room database
    objectBoxToLibkiwixMigrator.migrateBookMarks(box)
    BaristaSleepInteractions.sleep(2000L)
    val actualDataAfterMigration =
      objectBoxToLibkiwixMigrator.libkiwixBookmarks.bookmarks().blockingFirst()
    assertEquals(2, actualDataAfterMigration.size)
    val existingItem =
      actualDataAfterMigration.find {
        it.url == existingBookmarkUrl && it.title == existingTitle
      }
    assertNotNull(existingItem)
    val newItem =
      actualDataAfterMigration.find {
        it.url == expectedBookmarkUrl && it.title == expectedTitle
      }
    assertNotNull(newItem)

    clearBookmarks(box, objectBoxToLibkiwixMigrator.libkiwixBookmarks)

    // Test large data migration for recent searches
    val numEntities = 10000
    // Insert a large number of recent search entities into ObjectBox
    for (i in 1..numEntities) {
      val bookMarkUrl = "https://alpine_linux/search_$i"
      val title = "title_$i"
      val bookmarkEntity1 = BookmarkEntity(
        0,
        expectedZimId,
        expectedZimName,
        expectedZimFilePath,
        bookMarkUrl,
        title,
        expectedFavicon
      )
      box.put(bookmarkEntity1)
    }
    val startTime = System.currentTimeMillis()
    // Migrate data into Room database
    objectBoxToLibkiwixMigrator.migrateBookMarks(box)
    val endTime = System.currentTimeMillis()
    val migrationTime = endTime - startTime
    // Check if data successfully migrated to Room
    val actualDataAfterLargeMigration =
      objectBoxToLibkiwixMigrator.libkiwixBookmarks.bookmarks().blockingFirst()
    assertEquals(numEntities, actualDataAfterLargeMigration.size)
    // Assert that the migration completes within a reasonable time frame
    assertTrue(migrationTime < 5000)
  }

  private fun clearBookmarks(box: Box<BookmarkEntity>, libkiwixBookmark: LibkiwixBookmarks) {
    // delete bookmarks for testing other edge cases
    libkiwixBookmark.deleteBookmarks(
      libkiwixBookmark.bookmarks().blockingFirst() as List<LibkiwixBookmarkItem>
    )
    box.removeAll()
  }
}
