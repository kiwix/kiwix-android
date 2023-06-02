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

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import io.objectbox.Box
import io.objectbox.BoxStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.jupiter.api.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.dao.entities.RecentSearchEntity
import org.kiwix.kiwixmobile.core.data.KiwixRoomDatabase
import org.kiwix.kiwixmobile.core.data.remote.ObjectBoxToRoomMigrator

@RunWith(AndroidJUnit4::class)
class ObjectBoxToRoomMigratorTest {

  private lateinit var context: Context
  private lateinit var kiwixRoomDatabase: KiwixRoomDatabase
  private var boxStore: BoxStore = mockk()
  private lateinit var objectBoxToRoomMigrator: ObjectBoxToRoomMigrator

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    kiwixRoomDatabase = Room.inMemoryDatabaseBuilder(context, KiwixRoomDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    objectBoxToRoomMigrator = ObjectBoxToRoomMigrator()
    objectBoxToRoomMigrator.kiwixRoomDatabase = kiwixRoomDatabase
    objectBoxToRoomMigrator.boxStore = boxStore
  }

  @After
  fun cleanup() {
    kiwixRoomDatabase.close()
    boxStore.close()
  }

  @Test
  fun migrateRecentSearch_shouldInsertDataIntoRoomDatabase() = runBlocking {
    val box = boxStore.boxFor(RecentSearchEntity::class.java)
    val expectedSearchTerm = "test search"
    val expectedZimId = "8812214350305159407L"
    val recentSearchEntity =
      RecentSearchEntity(searchTerm = expectedSearchTerm, zimId = expectedZimId)
    // insert into object box
    box.put(recentSearchEntity)
    // migrate data into room database
    objectBoxToRoomMigrator.migrateRecentSearch(box)
    // check if data successfully migrated to room
    val actual = kiwixRoomDatabase.recentSearchRoomDao().search(expectedZimId).first()
    assertEquals(actual.size, 1)
    assertEquals(actual[0].searchTerm, expectedSearchTerm)
    assertEquals(actual[0].zimId, expectedZimId)

    // clear both databases for recent searches to test more edge cases
    clearRecentSearchDatabases(box)

    // Migrate data from empty ObjectBox database
    objectBoxToRoomMigrator.migrateRecentSearch(box)
    val actualData = kiwixRoomDatabase.recentSearchRoomDao().fullSearch().first()
    assertTrue(actualData.isEmpty())

    // Test if data successfully migrated to Room and existing data is preserved
    val existingSearchTerm = "existing search"
    val existingZimId = "8812214350305159407L"
    kiwixRoomDatabase.recentSearchRoomDao().saveSearch(existingSearchTerm, existingZimId)
    box.put(recentSearchEntity)
    // Migrate data into Room database
    objectBoxToRoomMigrator.migrateRecentSearch(box)
    val actualDataAfterMigration = kiwixRoomDatabase.recentSearchRoomDao().fullSearch().first()
    assertEquals(2, actual.size)
    val existingItem =
      actualDataAfterMigration.find {
        it.searchTerm == existingSearchTerm && it.zimId == existingZimId
      }
    assertNotNull(existingItem)
    val newItem =
      actualDataAfterMigration.find {
        it.searchTerm == expectedSearchTerm && it.zimId == expectedZimId
      }
    assertNotNull(newItem)

    clearRecentSearchDatabases(box)

    // Test migration if ObjectBox has null values
    lateinit var undefinedSearchTerm: String
    lateinit var undefinedZimId: String
    try {
      val invalidSearchEntity =
        RecentSearchEntity(searchTerm = undefinedSearchTerm, zimId = undefinedZimId)
      box.put(invalidSearchEntity)
      // Migrate data into Room database
      objectBoxToRoomMigrator.migrateRecentSearch(box)
    } catch (_: Exception) {
    }
    // Ensure Room database remains empty or unaffected by the invalid data
    val actualDataAfterInvalidMigration =
      kiwixRoomDatabase.recentSearchRoomDao().fullSearch().first()
    assertTrue(actualDataAfterInvalidMigration.isEmpty())

    // Test large data migration for recent searches
    val numEntities = 10000
    // Insert a large number of recent search entities into ObjectBox
    for (i in 1..numEntities) {
      val searchTerm = "search_$i"
      val zimId = "$i"
      val recentSearchEntity = RecentSearchEntity(searchTerm = searchTerm, zimId = zimId)
      box.put(recentSearchEntity)
    }
    val startTime = System.currentTimeMillis()
    // Migrate data into Room database
    objectBoxToRoomMigrator.migrateRecentSearch(box)
    val endTime = System.currentTimeMillis()
    val migrationTime = endTime - startTime
    // Check if data successfully migrated to Room
    val actualDataAfterLargeMigration =
      kiwixRoomDatabase.recentSearchRoomDao().fullSearch().first()
    assertEquals(numEntities, actualDataAfterLargeMigration.size)
    // Assert that the migration completes within a reasonable time frame
    assertTrue("Migration took too long: $migrationTime ms", migrationTime < 5000)
  }

  private fun clearRecentSearchDatabases(box: Box<RecentSearchEntity>) {
    // delete history for testing other edge cases
    kiwixRoomDatabase.recentSearchRoomDao().deleteSearchHistory()
    box.removeAll()
  }
}
