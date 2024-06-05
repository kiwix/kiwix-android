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

package org.kiwix.kiwixmobile.roomDao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsEqual.equalTo
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.KiwixRoomDatabaseTest.Companion.getHistoryItem
import org.kiwix.kiwixmobile.core.dao.HistoryRoomDao
import org.kiwix.kiwixmobile.core.data.KiwixRoomDatabase
import org.kiwix.kiwixmobile.core.page.history.adapter.HistoryListItem

@RunWith(AndroidJUnit4::class)
class HistoryRoomDaoTest {
  private lateinit var kiwixRoomDatabase: KiwixRoomDatabase
  private lateinit var historyRoomDao: HistoryRoomDao

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    kiwixRoomDatabase = Room.inMemoryDatabaseBuilder(context, KiwixRoomDatabase::class.java).build()
    historyRoomDao = kiwixRoomDatabase.historyRoomDao()
  }

  @After
  fun tearDown() {
    kiwixRoomDatabase.close()
  }

  @Test
  fun testHistoryRoomDao() = runBlocking {
    // delete all the history from database to properly run the test cases.
    historyRoomDao.deleteAllHistory()
    val historyItem = getHistoryItem(
      title = "Main Page",
      historyUrl = "https://kiwix.app/A/MainPage",
      databaseId = 1
    )

    // Save and retrieve a history item
    historyRoomDao.saveHistory(historyItem)
    var historyList = historyRoomDao.historyRoomEntity().blockingFirst()
    with(historyList.first()) {
      assertThat(historyTitle, equalTo(historyItem.title))
      assertThat(zimId, equalTo(historyItem.zimId))
      assertThat(zimName, equalTo(historyItem.zimName))
      assertThat(historyUrl, equalTo(historyItem.historyUrl))
      assertThat(zimFilePath, equalTo(historyItem.zimFilePath))
      assertThat(favicon, equalTo(historyItem.favicon))
      assertThat(dateString, equalTo(historyItem.dateString))
      assertThat(timeStamp, equalTo(historyItem.timeStamp))
    }

    // Test to update the same day history for url
    historyRoomDao.saveHistory(historyItem)
    historyList = historyRoomDao.historyRoomEntity().blockingFirst()
    assertEquals(historyList.size, 1)

    // Delete the saved history item
    historyRoomDao.deleteHistory(listOf(historyItem))
    historyList = historyRoomDao.historyRoomEntity().blockingFirst()
    assertEquals(historyList.size, 0)

    // Save and delete all history items
    historyRoomDao.saveHistory(historyItem)
    historyRoomDao.saveHistory(getHistoryItem(databaseId = 2, dateString = "31 May 2024"))
    historyRoomDao.deleteAllHistory()
    historyList = historyRoomDao.historyRoomEntity().blockingFirst()
    assertThat(historyList.size, equalTo(0))

    // Save history item with empty fields
    val emptyHistoryUrl = ""
    val emptyTitle = ""
    historyRoomDao.saveHistory(getHistoryItem(emptyTitle, emptyHistoryUrl, databaseId = 1))
    historyList = historyRoomDao.historyRoomEntity().blockingFirst()
    assertThat(historyList.size, equalTo(1))
    historyRoomDao.deleteAllHistory()

    // Attempt to save undefined history item
    lateinit var undefinedHistoryItem: HistoryListItem.HistoryItem
    try {
      historyRoomDao.saveHistory(undefinedHistoryItem)
      assertThat(
        "Undefined value was saved into database",
        false
      )
    } catch (e: Exception) {
      assertThat("Undefined value was not saved, as expected.", true)
    }

    // Save history item with Unicode values
    val unicodeTitle = "title \u03A3" // Unicode character for Greek capital letter Sigma
    val historyItem2 = getHistoryItem(title = unicodeTitle, databaseId = 2)
    historyRoomDao.saveHistory(historyItem2)
    historyList = historyRoomDao.historyRoomEntity().blockingFirst()
    assertThat(historyList.first().historyTitle, equalTo("title Σ"))

    // Test deletePages function
    historyRoomDao.saveHistory(historyItem)
    historyRoomDao.deletePages(listOf(historyItem, historyItem2))
    historyList = historyRoomDao.historyRoomEntity().blockingFirst()
    assertThat(historyList.size, equalTo(0))
  }
}
