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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsEqual.equalTo
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.dao.RecentSearchRoomDao
import org.kiwix.kiwixmobile.core.data.KiwixRoomDatabase

@RunWith(AndroidJUnit4::class)
class RecentSearchRoomDaoTest {

  private lateinit var kiwixRoomDatabase: KiwixRoomDatabase
  private lateinit var recentSearchRoomDao: RecentSearchRoomDao

  @After
  fun tearDown() {
    kiwixRoomDatabase.close()
  }

  @Test
  fun testSaveAndSearch() = runBlocking {
    val zimId = "8812214350305159407L"
    val context = ApplicationProvider.getApplicationContext<Context>()
    kiwixRoomDatabase = Room.inMemoryDatabaseBuilder(context, KiwixRoomDatabase::class.java).build()
    recentSearchRoomDao = kiwixRoomDatabase.recentSearchRoomDao()
    // Save a recent search entity
    val query = "query 1"
    recentSearchRoomDao.saveSearch(query, zimId)
    // Search for recent search entities with a matching zimId
    val result = recentSearchRoomDao.search(zimId).first()
    // Verify that the result contains the saved entity
    assertThat(result.size, equalTo(1))
    assertThat(result[0].searchTerm, equalTo(query))
    assertThat(result[0].zimId, equalTo(zimId))
  }

  @Test
  fun testDeleteSearchString() = runBlocking {
    val zimId = "8812214350305159407L"
    val context = ApplicationProvider.getApplicationContext<Context>()
    kiwixRoomDatabase = Room.inMemoryDatabaseBuilder(context, KiwixRoomDatabase::class.java).build()
    recentSearchRoomDao = kiwixRoomDatabase.recentSearchRoomDao()
    // Save a recent search entity
    val query = "query 1"
    recentSearchRoomDao.saveSearch(query, zimId)
    // Delete the saved entity by search term
    recentSearchRoomDao.deleteSearchString(query)
    // Search for recent search entities with a matching zimId
    val result = recentSearchRoomDao.search(zimId).first()
    // Verify that the result does not contain the deleted entity
    assertThat(result.size, equalTo(0))
  }

  @Test
  fun testDeleteSearchHistory() = runBlocking {
    val zimId = "8812214350305159407L"
    val context = ApplicationProvider.getApplicationContext<Context>()
    kiwixRoomDatabase = Room.inMemoryDatabaseBuilder(context, KiwixRoomDatabase::class.java).build()
    recentSearchRoomDao = kiwixRoomDatabase.recentSearchRoomDao()
    // Save two recent search entities
    recentSearchRoomDao.saveSearch("query 1", zimId)
    recentSearchRoomDao.saveSearch("query 2", zimId)
    // Delete all recent search entities
    recentSearchRoomDao.deleteSearchHistory()
    // Search for recent search entities with a matching zimId
    val result = recentSearchRoomDao.search(zimId).first()
    // Verify that the result is empty
    assertThat(result.size, equalTo(0))
  }
}
