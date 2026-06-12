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

package org.kiwix.kiwixmobile.core.main.note.helper

import android.content.Context
import org.kiwix.kiwixmobile.core.main.note.AddNoteDialogConfig
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.StorageUtils.getNotesDirectory
import javax.inject.Inject

class NoteMetadataFactory @Inject constructor(private val context: Context) {
  fun create(config: AddNoteDialogConfig, zimReaderContainer: ZimReaderContainer): NoteMetadata {
    val noteListItem = config.noteListItem

    val zimFileName = noteListItem?.zimReaderSource?.toDatabase()
      ?: zimReaderContainer.zimReaderSource?.toDatabase()
      ?: zimReaderContainer.name
    val zimFileTitle = noteListItem?.title ?: zimReaderContainer.zimFileTitle
    val zimId = noteListItem?.zimId ?: zimReaderContainer.id.orEmpty()
    val zimReaderSource = noteListItem?.zimReaderSource ?: zimReaderContainer.zimReaderSource
    val favicon = noteListItem?.favicon ?: zimReaderContainer.favicon

    val articleTitle = noteListItem?.title?.substringAfter(": ")
      ?: (config.currentWebViewTitle ?: config.articleTitle)

    val zimFileUrl = noteListItem?.zimUrl ?: config.currentWebViewUrl.orEmpty()

    val zimNoteDirectoryName = run {
      val name = getTextAfterLastSlashWithoutExtension(zimFileName.orEmpty())
      name.ifEmpty { zimFileTitle }.orEmpty()
    }

    val articleNoteFileName = if (noteListItem?.noteFilePath != null) {
      getTextAfterLastSlashWithoutExtension(noteListItem.noteFilePath)
    } else {
      val url = config.currentWebViewUrl
      val name = if (url != null) getTextAfterLastSlashWithoutExtension(url) else ""
      name.ifEmpty { articleTitle }.orEmpty()
    }

    val zimNotesDirectory = noteListItem?.noteFilePath?.substringBefore(articleNoteFileName)
      ?: "${getNotesDirectory(context)}$zimNoteDirectoryName/"

    return NoteMetadata(
      zimFileName = zimFileName,
      zimFileTitle = zimFileTitle,
      zimId = zimId,
      zimReaderSource = zimReaderSource,
      favicon = favicon,
      articleTitle = articleTitle,
      zimFileUrl = zimFileUrl,
      zimNoteDirectoryName = zimNoteDirectoryName,
      articleNoteFileName = articleNoteFileName,
      zimNotesDirectory = zimNotesDirectory,
      isZimFileExist = zimFileName != null
    )
  }

  private fun getTextAfterLastSlashWithoutExtension(path: String): String =
    path.substringAfterLast('/', "").substringBeforeLast('.')
}

data class NoteMetadata(
  val zimFileName: String?,
  val zimFileTitle: String?,
  val zimId: String,
  val zimReaderSource: org.kiwix.kiwixmobile.core.reader.ZimReaderSource?,
  val favicon: String?,
  val articleTitle: String?,
  val zimFileUrl: String,
  val zimNoteDirectoryName: String,
  val articleNoteFileName: String,
  val zimNotesDirectory: String,
  val isZimFileExist: Boolean
) {
  fun getNoteTitle(): String =
    if (zimFileTitle != null && zimReaderSource != null) {
      zimFileTitle
    } else {
      "${zimFileTitle.orEmpty()}: $articleTitle"
    }
}
