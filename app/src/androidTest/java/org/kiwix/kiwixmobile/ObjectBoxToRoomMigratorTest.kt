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

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.objectbox.Box
import io.objectbox.BoxStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsEqual.equalTo
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.KiwixRoomDatabaseTest.Companion.getHistoryItem
import org.kiwix.kiwixmobile.core.dao.entities.HistoryEntity
import org.kiwix.kiwixmobile.core.dao.entities.RecentSearchEntity
import org.kiwix.kiwixmobile.core.data.KiwixRoomDatabase
import org.kiwix.kiwixmobile.core.data.remote.ObjectBoxToRoomMigrator
import org.kiwix.kiwixmobile.core.di.modules.DatabaseModule
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil

@RunWith(AndroidJUnit4::class)
class ObjectBoxToRoomMigratorTest {
  private lateinit var context: Context
  private lateinit var kiwixRoomDatabase: KiwixRoomDatabase
  private lateinit var boxStore: BoxStore
  private lateinit var objectBoxToRoomMigrator: ObjectBoxToRoomMigrator

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    kiwixRoomDatabase = Room.inMemoryDatabaseBuilder(context, KiwixRoomDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    boxStore = DatabaseModule.boxStore!!
    objectBoxToRoomMigrator = ObjectBoxToRoomMigrator()
    objectBoxToRoomMigrator.kiwixRoomDatabase = kiwixRoomDatabase
    objectBoxToRoomMigrator.boxStore = boxStore
    objectBoxToRoomMigrator.sharedPreferenceUtil = SharedPreferenceUtil(context)
  }

  @After
  fun cleanup() {
    kiwixRoomDatabase.close()
    boxStore.close()
  }

  @Test
  fun migrateRecentSearch_shouldInsertDataIntoRoomDatabase() = runBlocking {
    val box = boxStore.boxFor(RecentSearchEntity::class.java)
    // clear both databases for recent searches to test more edge cases
    clearRoomAndBoxStoreDatabases(box)
    val expectedSearchTerm = "test search"
    val expectedZimId = "8812214350305159407L"
    val expectedUrl = "http://kiwix.app/mainPage"
    val recentSearchEntity =
      RecentSearchEntity(searchTerm = expectedSearchTerm, zimId = expectedZimId, url = expectedUrl)
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
    clearRoomAndBoxStoreDatabases(box)

    // Migrate data from empty ObjectBox database
    objectBoxToRoomMigrator.migrateRecentSearch(box)
    val actualData = kiwixRoomDatabase.recentSearchRoomDao().fullSearch().first()
    assertTrue(actualData.isEmpty())

    // Test if data successfully migrated to Room and existing data is preserved
    val existingSearchTerm = "existing search"
    val existingZimId = "8812214350305159407L"
    kiwixRoomDatabase.recentSearchRoomDao()
      .saveSearch(existingSearchTerm, existingZimId, "$expectedUrl/1")
    box.put(recentSearchEntity)
    // Migrate data into Room database
    objectBoxToRoomMigrator.migrateRecentSearch(box)
    val actualDataAfterMigration = kiwixRoomDatabase.recentSearchRoomDao().fullSearch().first()
    assertEquals(2, actualDataAfterMigration.size)
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

    clearRoomAndBoxStoreDatabases(box)

    // Test migration if ObjectBox has null values
    lateinit var undefinedSearchTerm: String
    lateinit var undefinedZimId: String
    lateinit var undefinedUrl: String
    try {
      val invalidSearchEntity =
        RecentSearchEntity(
          searchTerm = undefinedSearchTerm,
          zimId = undefinedZimId,
          url = undefinedUrl
        )
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
    val numEntities = 5000
    // Insert a large number of recent search entities into ObjectBox
    for (i in 1..numEntities) {
      val searchTerm = "search_$i"
      val zimId = "$i"
      box.put(RecentSearchEntity(searchTerm = searchTerm, zimId = zimId, url = "$expectedUrl$i"))
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
    assertTrue("Migration took too long: $migrationTime ms", migrationTime < 10000)
  }

  private fun <T> clearRoomAndBoxStoreDatabases(box: Box<T>) {
    // delete history for testing other edge cases
    kiwixRoomDatabase.recentSearchRoomDao().deleteSearchHistory()
    kiwixRoomDatabase.historyRoomDao().deleteAllHistory()
    box.removeAll()
  }

  @Test
  fun migrateHistory_shouldInsertDataIntoRoomDatabase() = runBlocking {
    val box = boxStore.boxFor(HistoryEntity::class.java)
    // clear both databases for history to test more edge cases
    clearRoomAndBoxStoreDatabases(box)

    val historyItem = getHistoryItem()
    val historyItem2 = getHistoryItem(
      title = "Main Page",
      historyUrl = "https://kiwix.app/A/MainPage"
    )
    val historyItem3 = getHistoryItem(databaseId = 1)
    // insert into object box
    box.put(HistoryEntity(historyItem))
    // migrate data into room database
    objectBoxToRoomMigrator.migrateHistory(box)
    // check if data successfully migrated to room
    val actual = kiwixRoomDatabase.historyRoomDao().historyRoomEntity().first()
    with(actual.first()) {
      assertThat(historyTitle, equalTo(historyItem.title))
      assertThat(zimId, equalTo(historyItem.zimId))
      assertThat(zimName, equalTo(historyItem.zimName))
      assertThat(historyUrl, equalTo(historyItem.historyUrl))
      assertThat(zimFilePath, equalTo(historyItem.zimFilePath))
      assertThat(favicon, equalTo(historyItem.favicon))
      assertThat(dateString, equalTo(historyItem.dateString))
      assertThat(timeStamp, equalTo(historyItem.timeStamp))
    }

    clearRoomAndBoxStoreDatabases(box)

    // Migrate data from empty ObjectBox database
    objectBoxToRoomMigrator.migrateHistory(box)
    var actualData = kiwixRoomDatabase.historyRoomDao().historyRoomEntity().first()
    assertTrue(actualData.isEmpty())

    // Test if data successfully migrated to Room and existing data is preserved
    kiwixRoomDatabase.historyRoomDao().saveHistory(historyItem3)
    box.put(HistoryEntity(historyItem2))
    // Migrate data into Room database
    objectBoxToRoomMigrator.migrateHistory(box)
    actualData = kiwixRoomDatabase.historyRoomDao().historyRoomEntity().first()
    assertEquals(2, actualData.size)
    val existingItem =
      actualData.find {
        it.historyUrl == historyItem.historyUrl && it.historyTitle == historyItem.title
      }
    assertNotNull(existingItem)
    val newItem =
      actualData.find {
        it.historyUrl == historyItem2.historyUrl && it.historyTitle == historyItem2.title
      }
    assertNotNull(newItem)

    clearRoomAndBoxStoreDatabases(box)

    // Test room will not migrate the already exiting data in the database.
    kiwixRoomDatabase.historyRoomDao().saveHistory(historyItem)
    box.put(HistoryEntity(historyItem))
    objectBoxToRoomMigrator.migrateHistory(box)
    actualData = kiwixRoomDatabase.historyRoomDao().historyRoomEntity().first()
    assertEquals(1, actualData.size)

    clearRoomAndBoxStoreDatabases(box)

    // Test migration if ObjectBox has null values
    try {
      lateinit var invalidHistoryEntity: HistoryEntity
      box.put(invalidHistoryEntity)
      // Migrate data into Room database
      objectBoxToRoomMigrator.migrateHistory(box)
    } catch (_: Exception) {
    }
    // Ensure Room database remains empty or unaffected by the invalid data
    actualData = kiwixRoomDatabase.historyRoomDao().historyRoomEntity().first()
    assertTrue(actualData.isEmpty())

    // Test large data migration for recent searches
    val numEntities = 5000
    // Insert a large number of recent search entities into ObjectBox
    for (i in 1..numEntities) {
      box.put(
        HistoryEntity(
          getHistoryItem(
            title = "Installation$i",
            historyUrl = "https://kiwix.app/A/Installation$i"
          )
        )
      )
    }
    val startTime = System.currentTimeMillis()
    // Migrate data into Room database
    objectBoxToRoomMigrator.migrateHistory(box)
    val endTime = System.currentTimeMillis()
    val migrationTime = endTime - startTime
    // Check if data successfully migrated to Room
    actualData = kiwixRoomDatabase.historyRoomDao().historyRoomEntity().first()
    assertEquals(numEntities, actualData.size)
    // Assert that the migration completes within a reasonable time frame
    assertTrue("Migration took too long: $migrationTime ms", migrationTime < 10000)
  }
}
