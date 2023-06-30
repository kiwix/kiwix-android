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

import android.annotation.SuppressLint
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.notNullValue
import org.hamcrest.core.IsEqual.equalTo
import org.junit.jupiter.api.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.dao.NotesRoomDao
import org.kiwix.kiwixmobile.core.dao.entities.NotesRoomEntity
import org.kiwix.kiwixmobile.core.data.KiwixRoomDatabase
import org.kiwix.kiwixmobile.core.page.notes.adapter.NoteListItem
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class NoteRoomDaoTest {
  private lateinit var noteRoomDao: NotesRoomDao
  private lateinit var db: KiwixRoomDatabase

  @Test
  @Throws(IOException::class)
  @SuppressLint("CheckResult")
  fun testNotesRoomDao() = runBlocking {
    val zimId = "8812214350305159407L"
    val noteTitle = "abNotesTitle"
    val zimFilePath = "/storage/emulated/0/Kiwix/alpinelinux_en_all_maxi_2023-01.zim"
    val zimUrl = "https://kiwix.app/A/Main_Page"
    val noteFilePath =
      "/storage/emulated/0/Android/data/org.kiwix.kiwixmobile/files" +
        "/Kiwix/Notes/alpinelinux_en_all_maxi_2023-01/Main_Page.txt"
    val context = ApplicationProvider.getApplicationContext<Context>()
    val notesItem = NoteListItem(
      0,
      zimId,
      noteTitle,
      zimFilePath,
      zimUrl,
      noteFilePath,
      null
    )
    val notesItem2: NoteListItem = mockk(relaxed = true)
    val notesItem3: NoteListItem = mockk(relaxed = true)
    val notesItem4: NoteListItem = mockk(relaxed = true)
    val notesItem5: NoteListItem = mockk(relaxed = true)
    val notesItemList: List<NoteListItem> = listOf(notesItem)

    db = Room.inMemoryDatabaseBuilder(
      context, KiwixRoomDatabase::class.java
    ).build()

    noteRoomDao = db.noteRoomDao()

    // Save notes
    noteRoomDao.saveNote(NotesRoomEntity(notesItem))
    noteRoomDao.saveNote(NotesRoomEntity(notesItem2))
    noteRoomDao.saveNote(NotesRoomEntity(notesItem3))
    noteRoomDao.saveNote(NotesRoomEntity(notesItem4))
    noteRoomDao.saveNote(NotesRoomEntity(notesItem5))
    // Verify that the result contains the saved entity
    noteRoomDao.notesAsEntity().subscribe {
      assertThat(5, equalTo(it.size))
      assertThat(it[0].noteTitle, equalTo(noteTitle))
      assertThat(it[0].zimId, equalTo(zimId))
      assertThat(it[0].zimFilePath, equalTo(zimFilePath))
      assertThat(it[0].zimUrl, equalTo(zimUrl))
      assertThat(it[0].noteFilePath, equalTo(noteFilePath))
    }.dispose()

    // Delete all entities saved in the database
    noteRoomDao.deletePages(notesItemList)
    noteRoomDao.deleteNotes(notesItemList)
    // Verify that the result does not contain the deleted entity
    noteRoomDao.pages().subscribe {
      assertThat(0, equalTo(it.size))
    }.dispose()

    // Save a note entity
    noteRoomDao.saveNote(NotesRoomEntity(notesItem))
    noteRoomDao.notesAsEntity().subscribe {
      assertThat(1, equalTo(it.size))
    }.dispose()
    // Delete the saved entity by title
    noteRoomDao.deleteNote(noteTitle)
    // Verify that the result does not contain the deleted entity
    noteRoomDao.notesAsEntity().subscribe {
      assertThat(0, equalTo(it.size))
    }.dispose()

    // Save a note entity
    noteRoomDao.saveNote(NotesRoomEntity(notesItem))
    // Check if it is successfully saved the entity into database
    noteRoomDao.notesAsEntity().subscribe {
      assertThat(1, equalTo(it.size))
      assertThat(noteTitle, equalTo(it.first().noteTitle))
    }.dispose()

    // Test deleting a note that does not exist
    val nonExistentNoteTitle = "NonExistentNoteTitle"
    noteRoomDao.deleteNote(nonExistentNoteTitle)
    noteRoomDao.notesAsEntity().subscribe {
      assertThat(1, equalTo(it.size))
    }.dispose()

    // Test querying notes with different filter conditions
    noteRoomDao.notesAsEntity().subscribe { notesList ->
      val filteredNotes = notesList.firstOrNull { it.zimId == zimId }
      assertThat(filteredNotes, notNullValue())
      assertThat(notesList.size, equalTo(1))
      assertThat(filteredNotes!!.noteTitle, equalTo(noteTitle))
      assertThat(filteredNotes.zimId, equalTo(zimId))
    }.dispose()

    // Test updating a note
    val updatedNoteTitle = "UpdatedNoteTitle"
    val updatedNoteFilePath = "/storage/emulated/0/UpdatedFilePath.txt"
    noteRoomDao.notesAsEntity().subscribe { noteList ->
      val updatedNote = noteList.firstOrNull { it.noteTitle == noteTitle }
      assertThat(updatedNote, notNullValue())
      updatedNote!!.noteTitle = updatedNoteTitle
      updatedNote.noteFilePath = updatedNoteFilePath
      noteRoomDao.saveNote(updatedNote)

      // Verify the updated note
      noteRoomDao.notesAsEntity().subscribe { updataedNoteList ->
        val updatedNoteFromDb = updataedNoteList.firstOrNull { it.noteTitle == updatedNoteTitle }
        assertThat(updatedNoteFromDb, notNullValue())
        assertThat(updatedNoteFromDb!!.noteTitle, equalTo(updatedNoteTitle))
        assertThat(updatedNoteFromDb.noteFilePath, equalTo(updatedNoteFilePath))
      }.dispose()
    }.dispose()
  }
}
