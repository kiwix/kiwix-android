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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.dao.RecentSearchRoomDao
import org.kiwix.kiwixmobile.core.dao.entities.RecentSearchRoomEntity
import org.kiwix.kiwixmobile.core.data.KiwixRoomDatabase

@RunWith(AndroidJUnit4::class)
class KiwixRoomDatabaseTest {
  private lateinit var recentSearchRoomDao: RecentSearchRoomDao
  private lateinit var db: KiwixRoomDatabase

  @After
  fun teardown() {
    db.close()
  }

  @Test
  fun testRecentSearchRoomDao() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, KiwixRoomDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val zimId = "34388L"
    val searchTerm = "title 1"
    val searchTerm2 = "title 2"
    val url = ""
    recentSearchRoomDao = db.recentSearchRoomDao()
    val recentSearch = RecentSearchRoomEntity(zimId = zimId, searchTerm = searchTerm, url = url)
    val recentSearch1 = RecentSearchRoomEntity(zimId = zimId, searchTerm = searchTerm2, url = url)

    // test inserting into recent search database
    recentSearchRoomDao.saveSearch(recentSearch.searchTerm, recentSearch.zimId, url = url)
    var recentSearches = recentSearchRoomDao.search(zimId).first()
    assertEquals(recentSearches.size, 1)
    assertEquals(recentSearch.searchTerm, recentSearches.first().searchTerm)
    assertEquals(recentSearch.zimId, recentSearches.first().zimId)

    // test deleting recent search
    recentSearchRoomDao.deleteSearchString(searchTerm)
    recentSearches = recentSearchRoomDao.search(searchTerm).first()
    assertEquals(recentSearches.size, 0)

    // test deleting all recent search history
    recentSearchRoomDao.saveSearch(recentSearch.searchTerm, recentSearch.zimId, url = url)
    recentSearchRoomDao.saveSearch(recentSearch1.searchTerm, recentSearch1.zimId, url = url)
    recentSearches = recentSearchRoomDao.search(zimId).first()
    assertEquals(recentSearches.size, 2)
    recentSearchRoomDao.deleteSearchHistory()
    recentSearches = recentSearchRoomDao.search(searchTerm).first()
    assertEquals(recentSearches.size, 0)
  }
}
