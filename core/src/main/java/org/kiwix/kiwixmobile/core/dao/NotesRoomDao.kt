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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.kiwix.kiwixmobile.core.dao.entities.NotesRoomEntity
import org.kiwix.kiwixmobile.core.page.adapter.Page
import org.kiwix.kiwixmobile.core.page.notes.adapter.NoteListItem

@Dao
abstract class NotesRoomDao : PageRoomDao {
  @Query("SELECT * FROM NotesRoomEntity ORDER BY NotesRoomEntity.noteTitle")
  abstract fun notesAsEntity(): Flow<List<NotesRoomEntity>>

  fun notes(): Flow<List<Page>> = notesAsEntity().map { it.map(::NoteListItem) }
  override fun pages(): Flow<List<Page>> = notes()
  override fun deletePages(pagesToDelete: List<Page>) =
    deleteNotes(pagesToDelete as List<NoteListItem>)

  fun saveNote(noteItem: NoteListItem) {
    saveNote(NotesRoomEntity(noteItem))
  }

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  abstract fun saveNote(notesRoomEntity: NotesRoomEntity)

  @Query("DELETE FROM NotesRoomEntity WHERE noteTitle=:noteTitle")
  abstract fun deleteNote(noteTitle: String)

  fun deleteNotes(notesList: List<NoteListItem>) {
    notesList.forEachIndexed { _, note ->
      val notesRoomEntity = NotesRoomEntity(note)
      deleteNote(noteTitle = notesRoomEntity.noteTitle)
    }
  }
}
