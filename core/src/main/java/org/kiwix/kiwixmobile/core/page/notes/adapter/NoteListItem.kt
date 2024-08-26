package org.kiwix.kiwixmobile.core.page.notes.adapter

import org.kiwix.kiwixmobile.core.dao.entities.NotesEntity
import org.kiwix.kiwixmobile.core.dao.entities.NotesRoomEntity
import org.kiwix.kiwixmobile.core.page.adapter.Page
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource

data class NoteListItem(
  val databaseId: Long = 0L,
  override val zimId: String,
  override val title: String,
  override val zimReaderSource: ZimReaderSource?,
  val zimUrl: String,
  val noteFilePath: String,
  override val favicon: String?,
  override var isSelected: Boolean = false,
  override val url: String = zimUrl,
  override val id: Long = databaseId
) : Page {

  constructor(notesEntity: NotesEntity) : this(
    notesEntity.id,
    notesEntity.zimId,
    notesEntity.noteTitle,
    notesEntity.zimReaderSource,
    notesEntity.zimUrl,
    notesEntity.noteFilePath,
    notesEntity.favicon
  )

  constructor(
    title: String,
    url: String,
    noteFilePath: String,
    zimFileReader: ZimFileReader
  ) : this(
    zimId = zimFileReader.id,
    title = title,
    zimReaderSource = zimFileReader.zimReaderSource,
    zimUrl = url,
    favicon = zimFileReader.favicon,
    noteFilePath = noteFilePath
  )

  constructor(notesRoomEntity: NotesRoomEntity) : this(
    notesRoomEntity.id,
    notesRoomEntity.zimId,
    notesRoomEntity.noteTitle,
    notesRoomEntity.zimReaderSource,
    notesRoomEntity.zimUrl,
    notesRoomEntity.noteFilePath,
    notesRoomEntity.favicon
  )
}
