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
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.dao.NewRecentSearchRoomDao
import org.kiwix.kiwixmobile.core.dao.entities.RecentSearchEntity
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class KiwixRoomDatabaseTest {
  private val box: Box<RecentSearchEntity> = mockk(relaxed = true)
  private lateinit var newRecentSearchRoomDao: NewRecentSearchRoomDao
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
    newRecentSearchRoomDao = db.newRecentSearchRoomDao()
    val searchTerm = "title"
    val zimId = "zimId"
    box.put(RecentSearchEntity(searchTerm = searchTerm, zimId = zimId))
    newRecentSearchRoomDao.migrationToRoomInsert(box)
    newRecentSearchRoomDao.search("zimId").collect { recentSearchEntites ->
      val entity = recentSearchEntites.find { it.zimId == zimId }
      if (entity != null) {
        Assertions.assertEquals(searchTerm, entity.searchTerm)
      }
    }
  }

  @After
  @Throws(IOException::class)
  fun closeDb() {
    db.close()
  }
}
