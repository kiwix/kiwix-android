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

package org.kiwix.kiwixmobile

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import io.objectbox.Box
import io.objectbox.BoxStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.jupiter.api.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookmarks
import org.kiwix.kiwixmobile.core.dao.entities.BookmarkEntity
import org.kiwix.kiwixmobile.core.data.remote.ObjectBoxToLibkiwixMigrator
import org.kiwix.kiwixmobile.core.page.bookmark.adapter.LibkiwixBookmarkItem
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.libkiwix.Book

@RunWith(AndroidJUnit4::class)
class ObjectBoxToLibkiwixMigratorTest {
  private var boxStore: BoxStore = mockk()
  private var libkiwixBookmarks: LibkiwixBookmarks = mockk(relaxed = true)
  private lateinit var objectBoxToLibkiwixMigrator: ObjectBoxToLibkiwixMigrator

  @Test
  fun migrateBookmarkTest(): Unit = runBlocking {
    val sharedPreferenceUtil: SharedPreferenceUtil = mockk(relaxed = true)
    objectBoxToLibkiwixMigrator = ObjectBoxToLibkiwixMigrator()
    objectBoxToLibkiwixMigrator.sharedPreferenceUtil = sharedPreferenceUtil
    objectBoxToLibkiwixMigrator.boxStore = boxStore
    objectBoxToLibkiwixMigrator.libkiwixBookmarks = libkiwixBookmarks
    val box = boxStore.boxFor(BookmarkEntity::class.java)
    val expectedZimName = "Alpine_Linux"
    val expectedZimId = "8812214350305159407L"
    val expectedZimFilePath = "data/Android/kiwix/alpine_linux_2022.zim"
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
    // check if data successfully migrated to room
    val actual = libkiwixBookmarks.bookmarks().blockingFirst()
    assertEquals(actual.size, 1)
    assertEquals(actual[0].zimFilePath, expectedZimFilePath)
    assertEquals(actual[0].zimId, expectedZimId)
    assertEquals(actual[0].favicon, expectedFavicon)
    assertEquals(actual[0].title, expectedTitle)
    assertEquals(actual[0].url, expectedBookmarkUrl)

    // clear both databases for recent searches to test more edge cases
    clearBookmarks(box, libkiwixBookmarks)
    // Migrate data from empty ObjectBox database
    objectBoxToLibkiwixMigrator.migrateBookMarks(box)
    val actualData = libkiwixBookmarks.bookmarks().blockingFirst()
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
    val libkiwixBook: Book = mockk(relaxed = true)
    libkiwixBookmarks.saveBookmark(LibkiwixBookmarkItem(secondBookmarkEntity, libkiwixBook))
    box.put(bookmarkEntity)
    // Migrate data into Room database
    objectBoxToLibkiwixMigrator.migrateBookMarks(box)
    val actualDataAfterMigration = libkiwixBookmarks.bookmarks().blockingFirst()
    assertEquals(2, actual.size)
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

    clearBookmarks(box, libkiwixBookmarks)

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
      libkiwixBookmarks.bookmarks().blockingFirst()
    assertEquals(numEntities, actualDataAfterLargeMigration.size)
    // Assert that the migration completes within a reasonable time frame
    assertTrue("Migration took too long: $migrationTime ms", migrationTime < 5000)
  }

  private fun clearBookmarks(box: Box<BookmarkEntity>, libkiwixBookmark: LibkiwixBookmarks) {
    // delete bookmarks for testing other edge cases
    libkiwixBookmark.deleteBookmarks(
      libkiwixBookmark.bookmarks().blockingFirst() as List<LibkiwixBookmarkItem>
    )
    box.removeAll()
  }
}
