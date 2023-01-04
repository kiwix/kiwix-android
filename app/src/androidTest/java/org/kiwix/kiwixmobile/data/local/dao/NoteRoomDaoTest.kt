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

package org.kiwix.kiwixmobile.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import io.objectbox.Box
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.dao.NewNoteDao
import org.kiwix.kiwixmobile.core.dao.NotesRoomDao
import org.kiwix.kiwixmobile.core.dao.entities.NotesEntity
import org.kiwix.kiwixmobile.core.dao.entities.NotesRoomEntity
import org.kiwix.kiwixmobile.core.data.local.KiwixRoomDatabase
import org.kiwix.kiwixmobile.core.page.adapter.Page
import org.kiwix.kiwixmobile.core.page.notes.adapter.NoteListItem
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class NoteRoomDaoTest {
  private lateinit var noteRoomDao: NotesRoomDao
  private lateinit var db: KiwixRoomDatabase
  private val notesBox: Box<NotesEntity> = mockk(relaxed = true)
  private val newNotesDao = NewNoteDao(notesBox)

  @Test
  @Throws(IOException::class)
  fun deletePages() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val notesItem: NoteListItem = mockk(relaxed = true)
    val notesItemList: List<NoteListItem> = listOf(notesItem)
    val pagesToDelete: List<Page> = notesItemList
    db = Room.inMemoryDatabaseBuilder(
      context, KiwixRoomDatabase::class.java
    ).build()
    noteRoomDao = db.noteRoomDao()
    noteRoomDao.deletePages(pagesToDelete)
    noteRoomDao.deleteNotes(notesItemList)
    noteRoomDao.pages().subscribe {
      Assertions.assertEquals(0, it.size)
    }
  }

  @Test
  @Throws(IOException::class)
  fun deleteNotePage() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val notesItem: NoteListItem = mockk(relaxed = true)
    db = Room.inMemoryDatabaseBuilder(
      context, KiwixRoomDatabase::class.java
    ).build()
    noteRoomDao = db.noteRoomDao()
    val noteTitle = "abNotesTitle"
    notesItem.title = noteTitle
    noteRoomDao.saveNote(NotesRoomEntity(notesItem))
    noteRoomDao.deleteNote(noteTitle)
    noteRoomDao.notesAsEntity().subscribe {
      Assertions.assertEquals(0, it.size)
    }
  }

  @Test
  @Throws(IOException::class)
  fun saveNote() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val notesItem: NoteListItem = mockk(relaxed = true)
    val notesItemList: List<NoteListItem> = listOf(notesItem)
    db = Room.inMemoryDatabaseBuilder(
      context, KiwixRoomDatabase::class.java
    ).build()
    noteRoomDao = db.noteRoomDao()
    val noteTitle = "abNotesTitle"
    notesItem.title = noteTitle
    noteRoomDao.saveNote(NotesRoomEntity(notesItem))
    noteRoomDao.notesAsEntity().subscribe {
      Assertions.assertEquals(1, it.size)
      Assertions.assertEquals(noteTitle, it.first().noteTitle)
    }
  }

  @Test
  @Throws(IOException::class)
  fun saveNotes() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val notesItem: NoteListItem = mockk(relaxed = true)
    val notesItem2: NoteListItem = mockk(relaxed = true)
    val notesItem3: NoteListItem = mockk(relaxed = true)
    val notesItem4: NoteListItem = mockk(relaxed = true)
    val notesItem5: NoteListItem = mockk(relaxed = true)
    db = Room.inMemoryDatabaseBuilder(
      context, KiwixRoomDatabase::class.java
    ).build()
    noteRoomDao = db.noteRoomDao()
    noteRoomDao.saveNote(NotesRoomEntity(notesItem))
    noteRoomDao.saveNote(NotesRoomEntity(notesItem2))
    noteRoomDao.saveNote(NotesRoomEntity(notesItem3))
    noteRoomDao.saveNote(NotesRoomEntity(notesItem4))
    noteRoomDao.saveNote(NotesRoomEntity(notesItem5))
    noteRoomDao.notesAsEntity().subscribe {
      Assertions.assertEquals(5, it.size)
    }
  }

  @Test
  @Throws(IOException::class)
  fun migrationTest() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(
      context, KiwixRoomDatabase::class.java
    ).build()
    noteRoomDao = db.noteRoomDao()
    val newNote: NoteListItem = mockk(relaxed = true)
    newNotesDao.saveNote(newNote)
    notesBox.put(NotesEntity(newNote))
    noteRoomDao.migrationToRoomInsert(notesBox)
    noteRoomDao.pages().subscribe {
      Assertions.assertEquals(1, it.size)
    }
  }

  @Test
  @Throws(IOException::class)
  fun migrationTest2() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(
      context, KiwixRoomDatabase::class.java
    ).build()
    noteRoomDao = db.noteRoomDao()
    val newNote: NoteListItem = mockk(relaxed = true)
    newNotesDao.saveNote(newNote)
    notesBox.put(NotesEntity(newNote))
    notesBox.put(NotesEntity(newNote))
    notesBox.put(NotesEntity(newNote))
    notesBox.put(NotesEntity(newNote))
    notesBox.put(NotesEntity(newNote))
    notesBox.put(NotesEntity(newNote))
    noteRoomDao.migrationToRoomInsert(notesBox)
    noteRoomDao.pages().subscribe {
      Assertions.assertEquals(6, it.size)
    }
  }
}
