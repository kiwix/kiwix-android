package org.kiwix.kiwixmobile.core.page.notes.adapter

import org.kiwix.kiwixmobile.core.dao.entities.NotesEntity
import org.kiwix.kiwixmobile.core.page.adapter.Page

data class NoteListItem(
  val databaseId: Long = 0L,
  override val zimId: String,
  override val title: String,
  override val zimFilePath: String?,
  val noteFilePath: String,
  val noteBody: String,
  override val favicon: String?,
  override var isSelected: Boolean = false,
  override val url: String = noteBody,
  override val id: Long = databaseId
) : Page {
  constructor(notesEntity: NotesEntity) : this(
    databaseId = notesEntity.id,
    zimId = notesEntity.zimId,
    zimFilePath = notesEntity.zimFilePath,
    noteFilePath = notesEntity.noteFilePath,
    title = notesEntity.noteTitle.orEmpty(),
    noteBody = notesEntity.noteBody,
    favicon = notesEntity.favicon,
    isSelected = false
  )
}
