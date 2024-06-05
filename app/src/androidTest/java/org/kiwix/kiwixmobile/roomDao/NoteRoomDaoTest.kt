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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsEqual.equalTo
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.KiwixRoomDatabaseTest.Companion.getNoteListItem
import org.kiwix.kiwixmobile.core.dao.NotesRoomDao
import org.kiwix.kiwixmobile.core.data.KiwixRoomDatabase
import org.kiwix.kiwixmobile.core.page.notes.adapter.NoteListItem

@RunWith(AndroidJUnit4::class)
class NoteRoomDaoTest {
  private lateinit var kiwixRoomDatabase: KiwixRoomDatabase
  private lateinit var notesRoomDao: NotesRoomDao

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    kiwixRoomDatabase = Room.inMemoryDatabaseBuilder(context, KiwixRoomDatabase::class.java).build()
    notesRoomDao = kiwixRoomDatabase.notesRoomDao()
  }

  @After
  fun tearDown() {
    kiwixRoomDatabase.close()
  }

  @Test
  fun testNotesRoomDao() = runBlocking {
    // delete all the notes from database to properly run the test cases.
    clearNotes()
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

    // Test update the existing note
    notesRoomDao.saveNote(noteItem)
    notesList = notesRoomDao.notes().first() as List<NoteListItem>
    assertEquals(notesList.size, 1)

    // Delete the saved note item with all delete methods available in NoteRoomDao.
    // delete via noteTitle
    notesRoomDao.deleteNote(noteItem.title)
    notesList = notesRoomDao.notes().first() as List<NoteListItem>
    assertEquals(notesList.size, 0)

    // delete with deletePages method
    notesRoomDao.saveNote(noteItem)
    notesRoomDao.deletePages(listOf(noteItem))
    notesList = notesRoomDao.notes().first() as List<NoteListItem>
    assertEquals(notesList.size, 0)

    // delete with list of NoteListItem
    notesRoomDao.saveNote(noteItem)
    notesRoomDao.deleteNotes(listOf(noteItem))
    notesList = notesRoomDao.notes().first() as List<NoteListItem>
    assertEquals(notesList.size, 0)

    // Save note with empty title
    notesRoomDao.saveNote(
      getNoteListItem(
        title = "",
        zimUrl = "http://kiwix.app/Installing",
        noteFilePath = "/storage/emulated/0/Download/Notes/Alpine linux/Installing.txt"
      )
    )
    notesList = notesRoomDao.notes().first() as List<NoteListItem>
    assertEquals(notesList.size, 1)
    clearNotes()

    // Attempt to save undefined history item
    lateinit var undefinedNoteListItem: NoteListItem
    try {
      notesRoomDao.saveNote(undefinedNoteListItem)
      assertThat(
        "Undefined value was saved into database",
        false
      )
    } catch (e: Exception) {
      assertThat("Undefined value was not saved, as expected.", true)
    }

    // Save history item with Unicode values
    val unicodeTitle = "title \u03A3" // Unicode character for Greek capital letter Sigma
    val noteListItem2 =
      getNoteListItem(title = unicodeTitle, zimUrl = "http://kiwix.app/Installing")
    notesRoomDao.saveNote(noteListItem2)
    notesList = notesRoomDao.notes().first() as List<NoteListItem>
    assertThat(notesList.first().title, equalTo("title Σ"))
  }

  private suspend fun clearNotes() {
    notesRoomDao.deleteNotes(notesRoomDao.notes().first() as List<NoteListItem>)
  }
}
