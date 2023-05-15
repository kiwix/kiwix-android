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
  fun testRecentSearchRoomDao() = runBlocking {
    val zimId = "8812214350305159407L"
    val context = ApplicationProvider.getApplicationContext<Context>()
    kiwixRoomDatabase = Room.inMemoryDatabaseBuilder(context, KiwixRoomDatabase::class.java).build()
    recentSearchRoomDao = kiwixRoomDatabase.recentSearchRoomDao()
    // Save a recent search entity
    val query =
      "This is a long search term to test whether it will be saved into the room database."
    recentSearchRoomDao.saveSearch(query, zimId)
    // Search for recent search entities with a matching zimId
    val result = getRecentSearchByZimId(zimId)
    // Verify that the result contains the saved entity
    assertThat(result.size, equalTo(1))
    assertThat(result[0].searchTerm, equalTo(query))
    assertThat(result[0].zimId, equalTo(zimId))

    // Delete the saved entity by search term
    recentSearchRoomDao.deleteSearchString(query)
    // Verify that the result does not contain the deleted entity
    assertThat(getRecentSearchByZimId(zimId).size, equalTo(0))

    // Testing deleting all recent searched history
    // Save two recent search entities
    recentSearchRoomDao.saveSearch(query, zimId)
    recentSearchRoomDao.saveSearch("query 2", zimId)
    // Delete all recent search entities
    recentSearchRoomDao.deleteSearchHistory()
    // Verify that the result is empty
    assertThat(getRecentSearchByZimId(zimId).size, equalTo(0))

    // test to save empty values for recent search
    val emptyQuery = ""
    recentSearchRoomDao.saveSearch(emptyQuery, zimId)
    // verify that the result is not empty
    assertThat(getRecentSearchByZimId(zimId).size, equalTo(1))

    // we are not saving undefined or null values into database.
    // test to save undefined value for recent search.
    lateinit var undefinedQuery: String
    try {
      recentSearchRoomDao.saveSearch(undefinedQuery, zimId)
      assertThat(
        "Undefined value was saved into database",
        false
      )
    } catch (e: Exception) {
      assertThat("Undefined value was not saved, as expected.", true)
    }

    // Delete all recent search entities for testing unicodes values
    recentSearchRoomDao.deleteSearchHistory()

    // save unicode values into database
    val unicodeQuery = "title \u03A3" // Unicode character for Greek capital letter Sigma
    recentSearchRoomDao.saveSearch(unicodeQuery, zimId)
    assertThat(getRecentSearchByZimId(zimId)[0].searchTerm, equalTo("title Σ"))
  }

  private suspend fun getRecentSearchByZimId(zimId: String) =
    recentSearchRoomDao.search(zimId).first()
}
