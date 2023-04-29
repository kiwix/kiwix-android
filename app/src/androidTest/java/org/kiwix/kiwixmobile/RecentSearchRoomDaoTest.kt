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

  private lateinit var database: KiwixRoomDatabase
  private lateinit var dao: RecentSearchRoomDao

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun testSaveAndSearch() = runBlocking {
    val zimId = "8812214350305159407L"
    val context = ApplicationProvider.getApplicationContext<Context>()
    database = Room.inMemoryDatabaseBuilder(context, KiwixRoomDatabase::class.java).build()
    dao = database.recentSearchRoomDao()
    // Save a recent search entity
    dao.saveSearch("query 1", zimId)
    // Search for recent search entities with a matching zimId
    val result = dao.search(zimId).first()
    // Verify that the result contains the saved entity
    assertThat(result.size, equalTo(1))
    assertThat(result[0].searchTerm, equalTo("query 1"))
    assertThat(result[0].zimId, equalTo("zimId"))
  }

  @Test
  fun testDeleteSearchString() = runBlocking {
    val zimId = "8812214350305159407L"
    val context = ApplicationProvider.getApplicationContext<Context>()
    database = Room.inMemoryDatabaseBuilder(context, KiwixRoomDatabase::class.java).build()
    dao = database.recentSearchRoomDao()
    // Save a recent search entity
    dao.saveSearch("query 1", zimId)
    // Delete the saved entity by search term
    dao.deleteSearchString("query 1")
    // Search for recent search entities with a matching zimId
    val result = dao.search(zimId).first()
    // Verify that the result does not contain the deleted entity
    assertThat(result.size, equalTo(0))
  }

  @Test
  fun testDeleteSearchHistory() = runBlocking {
    val zimId = "8812214350305159407L"
    val context = ApplicationProvider.getApplicationContext<Context>()
    database = Room.inMemoryDatabaseBuilder(context, KiwixRoomDatabase::class.java).build()
    dao = database.recentSearchRoomDao()
    // Save two recent search entities
    dao.saveSearch("query 1", zimId)
    dao.saveSearch("query 2", zimId)
    // Delete all recent search entities
    dao.deleteSearchHistory()
    // Search for recent search entities with a matching zimId
    val result = dao.search(zimId).first()
    // Verify that the result is empty
    assertThat(result.size, equalTo(0))
  }
}
