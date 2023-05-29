/*
 * Kiwix Android
 * Copyright (c) 2022 Kiwix <android.kiwix.org>
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

import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.objectbox.Box
import io.objectbox.query.Query
import io.objectbox.query.QueryBuilder
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.dao.entities.NotesEntity
import org.kiwix.kiwixmobile.core.dao.entities.NotesEntity_
import org.kiwix.kiwixmobile.core.page.adapter.Page
import org.kiwix.kiwixmobile.core.page.notes.adapter.NoteListItem
import java.util.concurrent.Callable

internal class NewNoteDaoTest {

  private val notesBox: Box<NotesEntity> = mockk(relaxed = true)
  private val newNotesDao = NewNoteDao(notesBox)

  @Test
  fun deletePages() {
    val notesItem: NoteListItem = mockk(relaxed = true)
    val notesItemList: List<NoteListItem> = listOf(notesItem)
    val pagesToDelete: List<Page> = notesItemList
    newNotesDao.deletePages(pagesToDelete)
    verify { newNotesDao.deleteNotes(notesItemList) }
  }

  @Test
  fun deleteNotePage() {
    val noteTitle = "abNotesTitle"
    val queryBuilder: QueryBuilder<NotesEntity> = mockk(relaxed = true)
    every { notesBox.query() } returns queryBuilder
    every {
      queryBuilder.equal(
        NotesEntity_.noteTitle,
        noteTitle,
        QueryBuilder.StringOrder.CASE_INSENSITIVE
      )
    } returns queryBuilder
    val query: Query<NotesEntity> = mockk(relaxed = true)
    every { queryBuilder.build() } returns query
    newNotesDao.deleteNote(noteTitle)
    verify { query.remove() }
  }

  @Test
  fun saveNotePage() {
    val newNote: NoteListItem = mockk(relaxed = true)
    val slot: CapturingSlot<Callable<Unit>> = slot()
    every { notesBox.store.callInTx(capture(slot)) } returns Unit
    val queryBuilder: QueryBuilder<NotesEntity> = mockk(relaxed = true)
    every { notesBox.query() } returns queryBuilder
    val query: Query<NotesEntity> = mockk(relaxed = true)
    every { queryBuilder.build() } returns query
    every { newNote.title } returns ""
    every {
      queryBuilder.equal(
        NotesEntity_.noteTitle,
        newNote.title,
        QueryBuilder.StringOrder.CASE_INSENSITIVE
      )
    } returns queryBuilder
    newNotesDao.saveNote(newNote)
    slot.captured.call()
    verify { notesBox.put(NotesEntity(newNote)) }
  }
}
