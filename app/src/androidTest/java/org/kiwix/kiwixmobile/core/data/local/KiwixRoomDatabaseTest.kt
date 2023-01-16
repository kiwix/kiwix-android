/*
 * Kiwix Android
 * Copyright (c) 2022 Kiwix <android.kiwix.org>
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
package org.kiwix.kiwixmobile.core.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import io.objectbox.Box
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.dao.RecentSearchRoomDao
import org.kiwix.kiwixmobile.core.dao.entities.RecentSearchEntity
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class KiwixRoomDatabaseTest {
  private val box: Box<RecentSearchEntity> = mockk(relaxed = true)
  private lateinit var recentSearchRoomDao: RecentSearchRoomDao
  private lateinit var db: KiwixRoomDatabase

  // @Before
  // fun createDb() {
  // }

  @Test
  @Throws(IOException::class)
  fun testMigrationTest() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(
      context, KiwixRoomDatabase::class.java
    ).build()
    recentSearchRoomDao = db.recentSearchRoomDao()
    val searchTerm = "title"
    val zimId = "zimId"
    box.put(RecentSearchEntity(searchTerm = searchTerm, zimId = zimId))
    recentSearchRoomDao.migrationToRoomInsert(box)
    recentSearchRoomDao.search("zimId").collect { recentSearchEntites ->
      val entity = recentSearchEntites.find { it.zimId == zimId }
      if (entity != null) {
        Assertions.assertEquals(searchTerm, entity.searchTerm)
      }
    }
  }

  @Test
  @Throws(IOException::class)
  fun testMigrationTest2() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(
      context, KiwixRoomDatabase::class.java
    ).build()
    recentSearchRoomDao = db.recentSearchRoomDao()
    val searchTerm = "title"
    val zimId = "zimId"
    val searchTerm2 = "title2"
    val searchTerm3 = "title3"
    box.put(RecentSearchEntity(searchTerm = searchTerm, zimId = zimId))
    box.put(RecentSearchEntity(searchTerm = searchTerm2, zimId = zimId))
    box.put(RecentSearchEntity(searchTerm = searchTerm3, zimId = zimId))
    recentSearchRoomDao.migrationToRoomInsert(box)
    recentSearchRoomDao.search("zimId").collect { recentSearchEntites ->
      Assertions.assertEquals(3, recentSearchEntites.size)
    }
  }

  @Test
  @Throws(IOException::class)
  fun testMigrationTest3() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(
      context, KiwixRoomDatabase::class.java
    ).build()
    recentSearchRoomDao = db.recentSearchRoomDao()
    val searchTerm = "title"
    val zimId = "zimId"
    val searchTerm2 = "title2"
    val zimId2 = "zimId2"
    val zimId3 = "zimId3"
    val searchTerm3 = "title3"
    box.put(RecentSearchEntity(searchTerm = searchTerm, zimId = zimId))
    box.put(RecentSearchEntity(searchTerm = searchTerm2, zimId = zimId2))
    box.put(RecentSearchEntity(searchTerm = searchTerm3, zimId = zimId3))
    recentSearchRoomDao.migrationToRoomInsert(box)
    val fullSearchList = recentSearchRoomDao.fullSearch().toList()
    Assertions.assertEquals(3, fullSearchList[0].size)
  }

  @After
  @Throws(IOException::class)
  fun closeDb() {
    db.close()
  }
}
