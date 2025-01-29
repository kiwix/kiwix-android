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

package org.kiwix.kiwixmobile.core.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.reactivex.Flowable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.dao.entities.NotesRoomEntity
import org.kiwix.kiwixmobile.core.extensions.deleteFile
import org.kiwix.kiwixmobile.core.extensions.isFileExist
import org.kiwix.kiwixmobile.core.page.adapter.Page
import org.kiwix.kiwixmobile.core.page.notes.adapter.NoteListItem
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource.Companion.fromDatabaseValue
import java.io.File

@Dao
abstract class NotesRoomDao : PageDao {
  @Query("SELECT * FROM NotesRoomEntity ORDER BY NotesRoomEntity.noteTitle")
  abstract fun notesAsEntity(): Flowable<List<NotesRoomEntity>>

  fun notes(): Flowable<List<Page>> = notesAsEntity().map {
    it.map { notesEntity ->
      notesEntity.zimFilePath?.let { filePath ->
        // set zimReaderSource for previously saved notes
        fromDatabaseValue(filePath)?.let { zimReaderSource ->
          notesEntity.zimReaderSource = zimReaderSource
        }
      }
      NoteListItem(notesEntity)
    }
  }

  override fun pages(): Flowable<List<Page>> = notes()
  override fun deletePages(pagesToDelete: List<Page>) =
    deleteNotes(pagesToDelete as List<NoteListItem>)

  fun saveNote(noteItem: NoteListItem) {
    val notesEntity = NotesRoomEntity(noteItem)
    if (count(notesEntity.id.toInt()) > 0) {
      // set the default id so that room will automatically generates the database id.
      notesEntity.id = 0
    }
    saveNote(notesEntity)
  }

  @Query("SELECT COUNT() FROM NotesRoomEntity WHERE id = :id")
  abstract fun count(id: Int): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  abstract fun saveNote(notesRoomEntity: NotesRoomEntity)

  @Query("DELETE FROM NotesRoomEntity WHERE noteTitle=:noteTitle")
  abstract fun deleteNote(noteTitle: String)

  fun deleteNotes(notesList: List<NoteListItem>) {
    notesList.forEachIndexed { _, note ->
      val notesRoomEntity = NotesRoomEntity(note)
      deleteNote(noteTitle = notesRoomEntity.noteTitle)
      removeNoteFileFromStorage(notesRoomEntity.noteFilePath)
    }
  }

  /**
   * Deletes the saved file from storage.
   * When the user deletes a note from the "Notes" screen,
   * the associated file should also be removed from storage,
   * as it is no longer needed.
   */
  private fun removeNoteFileFromStorage(noteFilePath: String) {
    CoroutineScope(Dispatchers.IO).launch {
      val noteFile = File(noteFilePath)
      if (noteFile.isFileExist()) {
        noteFile.deleteFile()
      }
    }
  }
}
