/*
 * Kiwix Android
 * Copyright (c) 2026 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.main.note.repository

import org.kiwix.kiwixmobile.core.main.MainRepositoryActions
import org.kiwix.kiwixmobile.core.main.note.helper.NoteMetadata
import org.kiwix.kiwixmobile.core.page.notes.models.NoteListItem
import org.kiwix.kiwixmobile.core.utils.files.Log
import java.io.File
import java.io.IOException
import javax.inject.Inject

class NoteRepository @Inject constructor(private val repositoryActions: MainRepositoryActions) {
  data class NoteFileContent(val text: String, val fileExists: Boolean)

  suspend fun loadNote(metadata: NoteMetadata): NoteFileContent {
    val noteFile = File("${metadata.zimNotesDirectory}${metadata.articleNoteFileName}.txt")
    return if (noteFile.exists()) {
      NoteFileContent(noteFile.readText(), true)
    } else {
      NoteFileContent("", false)
    }
  }

  suspend fun saveNote(metadata: NoteMetadata, noteText: String): Boolean {
    val notesFolder = File(metadata.zimNotesDirectory)
    if (!notesFolder.exists() && !notesFolder.mkdirs()) {
      Log.d("AddNoteDialog", "Required folder doesn't exist")
      return false
    }
    val noteFile = File(notesFolder.absolutePath, "${metadata.articleNoteFileName}.txt")
    return try {
      noteFile.writeText(noteText)
      addNoteToDao(noteFile.canonicalPath, metadata)
      true
    } catch (e: IOException) {
      e.printStackTrace()
      false
    }
  }

  private suspend fun addNoteToDao(noteFilePath: String?, metadata: NoteMetadata) {
    noteFilePath?.let { filePath ->
      if (filePath.isNotEmpty() && metadata.zimFileUrl.isNotEmpty()) {
        val noteToSave = NoteListItem(
          zimId = metadata.zimId,
          title = metadata.getNoteTitle(),
          url = metadata.zimFileUrl,
          noteFilePath = noteFilePath,
          zimReaderSource = metadata.zimReaderSource,
          favicon = metadata.favicon,
        )
        repositoryActions.saveNote(noteToSave)
      } else {
        Log.d("AddNoteDialog", "Cannot process with empty zim url or noteFilePath")
      }
    }
  }

  suspend fun deleteNote(metadata: NoteMetadata): Boolean {
    val noteFile = File(metadata.zimNotesDirectory, "${metadata.articleNoteFileName}.txt")
    val noteDeleted = noteFile.delete()
    return if (noteDeleted) {
      repositoryActions.deleteNote(metadata.getNoteTitle())
      true
    } else {
      false
    }
  }
}
