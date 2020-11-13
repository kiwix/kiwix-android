package org.kiwix.kiwixmobile.core.page.notes.adapter

import org.kiwix.kiwixmobile.core.dao.entities.NotesEntity
import org.kiwix.kiwixmobile.core.page.adapter.Page

data class NoteItem(
  val databaseId: Long = 0L,
  override val zimId: String,
  val zimName: String,
  override val zimFilePath: String?,
  val noteBody: String,
  override val title: String,
  override val favicon: String?,
  override var isSelected: Boolean = false,
  override val url: String = noteBody,
  override val id: Long = databaseId
) : Page {
  constructor(notesEntity: NotesEntity) : this(
    notesEntity.id,
    notesEntity.zimId,
    notesEntity.zimName,
    notesEntity.zimFilePath,
    notesEntity.noteBody,
    notesEntity.noteTitle,
    notesEntity.favicon,
    false
  )
}
