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
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsEqual.equalTo
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.dao.HistoryRoomDao
import org.kiwix.kiwixmobile.core.dao.NotesRoomDao
import org.kiwix.kiwixmobile.core.dao.RecentSearchRoomDao
import org.kiwix.kiwixmobile.core.dao.entities.RecentSearchRoomEntity
import org.kiwix.kiwixmobile.core.data.KiwixRoomDatabase
import org.kiwix.kiwixmobile.core.page.history.adapter.HistoryListItem
import org.kiwix.kiwixmobile.core.page.notes.adapter.NoteListItem

@RunWith(AndroidJUnit4::class)
class KiwixRoomDatabaseTest {
  private lateinit var recentSearchRoomDao: RecentSearchRoomDao
  private lateinit var db: KiwixRoomDatabase
  private lateinit var historyRoomDao: HistoryRoomDao
  private lateinit var notesRoomDao: NotesRoomDao

  @Before
  fun setUpDatabase() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, KiwixRoomDatabase::class.java)
      .allowMainThreadQueries()
      .build()
  }

  @After
  fun teardown() {
    db.close()
  }

  @Test
  fun testRecentSearchRoomDao() = runBlocking {
    val zimId = "34388L"
    val searchTerm = "title 1"
    val searchTerm2 = "title 2"
    val url = ""
    recentSearchRoomDao = db.recentSearchRoomDao()
    // delete the previous saved data from recentSearches to run the test cases properly.
    recentSearchRoomDao.deleteSearchHistory()
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

  @Test
  fun testHistoryRoomDao() = runBlocking {
    historyRoomDao = db.historyRoomDao()
    // delete the previous saved data from history to run the test cases properly.
    historyRoomDao.deleteAllHistory()
    val historyItem = getHistoryItem(
      title = "Main Page",
      historyUrl = "https://kiwix.app/A/MainPage",
      databaseId = 1
    )

    // test inserting into history database
    historyRoomDao.saveHistory(historyItem)
    var historyList = historyRoomDao.historyRoomEntity().first()
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

    // test deleting the history
    historyRoomDao.deleteHistory(listOf(historyItem))
    historyList = historyRoomDao.historyRoomEntity().first()
    assertEquals(historyList.size, 0)

    // test deleting all history
    historyRoomDao.saveHistory(historyItem)
    historyRoomDao.saveHistory(
      getHistoryItem(databaseId = 2)
    )
    historyList = historyRoomDao.historyRoomEntity().first()
    assertEquals(historyList.size, 2)
    historyRoomDao.deleteAllHistory()
    historyList = historyRoomDao.historyRoomEntity().first()
    assertEquals(historyList.size, 0)
  }

  @Test
  fun testNoteRoomDao() = runBlocking {
    notesRoomDao = db.notesRoomDao()
    // delete all the notes from database to properly run the test cases.
    notesRoomDao.deleteNotes(notesRoomDao.notes().first() as List<NoteListItem>)
    val noteItem = getNoteListItem(
      zimUrl = "http://kiwix.app/MainPage",
      noteFilePath = "/storage/emulated/0/Download/Notes/Alpine linux/MainPage.txt"
    )

    // Save and retrieve a notes item
    notesRoomDao.saveNote(noteItem)
    var notesList = notesRoomDao.notes().first() as List<NoteListItem>
    with(notesList.first()) {
      assertThat(zimId, equalTo(noteItem.zimId))
      assertThat(zimUrl, equalTo(noteItem.zimUrl))
      assertThat(title, equalTo(noteItem.title))
      assertThat(zimFilePath, equalTo(noteItem.zimFilePath))
      assertThat(noteFilePath, equalTo(noteItem.noteFilePath))
      assertThat(favicon, equalTo(noteItem.favicon))
    }
    assertEquals(notesList.size, 1)

    // test deleting the history
    notesRoomDao.deleteNotes(listOf(noteItem))
    notesList = notesRoomDao.notes().first() as List<NoteListItem>
    assertEquals(notesList.size, 0)

    // test deleting all notes
    notesRoomDao.saveNote(noteItem)
    notesRoomDao.saveNote(
      getNoteListItem(
        title = "Installing",
        zimUrl = "http://kiwix.app/Installing"
      )
    )
    notesList = notesRoomDao.notes().first() as List<NoteListItem>
    assertEquals(notesList.size, 2)
    notesRoomDao.deletePages(notesRoomDao.notes().first())
    notesList = notesRoomDao.notes().first() as List<NoteListItem>
    assertEquals(notesList.size, 0)
  }

  companion object {
    fun getHistoryItem(
      title: String = "Installation",
      historyUrl: String = "https://kiwix.app/A/Installation",
      dateString: String = "30 May 2024",
      databaseId: Long = 0L,
      zimId: String = "1f88ab6f-c265-b-3ff-8f49-b7f4429503800",
      zimName: String = "alpinelinux_en_all",
      zimFilePath: String = "/storage/emulated/0/Download/alpinelinux_en_all_maxi_2023-01.zim",
      timeStamp: Long = System.currentTimeMillis()
    ): HistoryListItem.HistoryItem =
      HistoryListItem.HistoryItem(
        databaseId = databaseId,
        zimId = zimId,
        zimName = zimName,
        historyUrl = historyUrl,
        title = title,
        zimFilePath = zimFilePath,
        favicon = null,
        dateString = dateString,
        timeStamp = timeStamp
      )

    fun getNoteListItem(
      databaseId: Long = 0L,
      zimId: String = "1f88ab6f-c265-b-3ff-8f49-b7f4429503800",
      title: String = "Alpine Wiki",
      zimFilePath: String = "/storage/emulated/0/Download/alpinelinux_en_all_maxi_2023-01.zim",
      zimUrl: String,
      noteFilePath: String = "/storage/emulated/0/Download/Notes/Alpine linux/AlpineNote.txt"
    ): NoteListItem = NoteListItem(
      databaseId = databaseId,
      zimId = zimId,
      title = title,
      zimFilePath = zimFilePath,
      zimUrl = zimUrl,
      noteFilePath = noteFilePath,
      null,
      false
    )
  }
}
