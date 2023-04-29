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
import io.objectbox.BoxStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.jupiter.api.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.dao.entities.RecentSearchEntity
import org.kiwix.kiwixmobile.core.data.KiwixRoomDatabase
import org.kiwix.kiwixmobile.core.data.remote.RoomDatabaseCallback

@RunWith(AndroidJUnit4::class)
class RoomDatabaseCallbackTest {

  private lateinit var context: Context
  private lateinit var kiwixRoomDatabase: KiwixRoomDatabase
  private var boxStore: BoxStore = mockk()
  private lateinit var callback: RoomDatabaseCallback

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    kiwixRoomDatabase = Room.inMemoryDatabaseBuilder(context, KiwixRoomDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    callback = RoomDatabaseCallback(context)
    callback.kiwixRoomDatabase = kiwixRoomDatabase
    callback.boxStore = boxStore
  }

  @After
  fun cleanup() {
    kiwixRoomDatabase.close()
    boxStore.close()
  }

  @Test
  fun migrateRecentSearch_shouldInsertDataIntoRoomDatabase() = runBlocking {
    // Given
    val box = boxStore.boxFor(RecentSearchEntity::class.java)
    val expectedSearchTerm = "test search"
    val expectedZimId = "8812214350305159407L"
    val recentSearchEntity =
      RecentSearchEntity(searchTerm = expectedSearchTerm, zimId = expectedZimId)
    box.put(recentSearchEntity)

    // When
    callback.migrateRecentSearch(boxStore)

    // Then
    val actual = kiwixRoomDatabase.recentSearchRoomDao().search(expectedZimId).first()
    assertEquals(actual.size, 1)
    assertEquals(actual[0].searchTerm, expectedSearchTerm)
    assertEquals(actual[0].zimId, expectedZimId)
  }
}
