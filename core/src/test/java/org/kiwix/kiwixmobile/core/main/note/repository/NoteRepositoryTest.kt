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

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.kiwix.kiwixmobile.core.main.MainRepositoryActions
import org.kiwix.kiwixmobile.core.main.note.helper.NoteMetadata
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class NoteRepositoryTest {
  private lateinit var repositoryActions: MainRepositoryActions
  private lateinit var repository: NoteRepository

  @TempDir
  lateinit var tempDir: File

  @BeforeEach
  fun setUp() {
    repositoryActions = mockk(relaxed = true)
    repository = NoteRepository(repositoryActions)
  }

  private fun createMetadata(
    notesDirectory: String = "${tempDir.absolutePath}/",
    articleFileName: String = "earth",
    zimUrl: String = "zim://earth"
  ) = NoteMetadata(
    zimFileName = "wikipedia.zim",
    zimFileTitle = "Wikipedia",
    zimId = "1",
    zimReaderSource = mockk(relaxed = true),
    favicon = null,
    articleTitle = "Earth",
    zimFileUrl = zimUrl,
    zimNoteDirectoryName = "Wikipedia",
    articleNoteFileName = articleFileName,
    zimNotesDirectory = notesDirectory,
    isZimFileExist = true
  )

  @Test
  fun `loadNote returns content when file exists`() = runTest {
    val metadata = createMetadata()

    File(
      metadata.zimNotesDirectory,
      "${metadata.articleNoteFileName}.txt"
    ).apply {
      parentFile?.mkdirs()
      writeText("My Note")
    }

    val result = repository.loadNote(metadata)

    assertEquals("My Note", result.text)
    assertTrue(result.fileExists)
  }

  @Test
  fun `loadNote returns empty content when file missing`() = runTest {
    val metadata = createMetadata()

    val result = repository.loadNote(metadata)

    assertEquals("", result.text)
    assertFalse(result.fileExists)
  }

  @Test
  fun `saveNote creates file and returns true`() = runTest {
    val metadata = createMetadata()

    val result = repository.saveNote(metadata, "Hello World")

    assertTrue(result)

    val noteFile = File(
      metadata.zimNotesDirectory,
      "${metadata.articleNoteFileName}.txt"
    )

    assertTrue(noteFile.exists())
    assertEquals("Hello World", noteFile.readText())
  }

  @Test
  fun `saveNote saves note to dao`() = runTest {
    val metadata = createMetadata()

    repository.saveNote(metadata, "Hello World")

    coVerify(exactly = 1) {
      repositoryActions.saveNote(any())
    }
  }

  @Test
  fun `saveNote does not save to dao when zim url empty`() = runTest {
    val metadata = createMetadata(zimUrl = "")

    repository.saveNote(metadata, "Hello World")

    coVerify(exactly = 0) {
      repositoryActions.saveNote(any())
    }
  }

  @Test
  fun `deleteNote deletes file and dao entry`() = runTest {
    val metadata = createMetadata()

    val noteFile = File(
      metadata.zimNotesDirectory,
      "${metadata.articleNoteFileName}.txt"
    )

    noteFile.parentFile?.mkdirs()
    noteFile.writeText("Hello")

    val result = repository.deleteNote(metadata)

    assertTrue(result)
    assertFalse(noteFile.exists())

    coVerify(exactly = 1) {
      repositoryActions.deleteNote(metadata.getNoteTitle())
    }
  }

  @Test
  fun `deleteNote returns false when file missing`() = runTest {
    val metadata = createMetadata()

    val result = repository.deleteNote(metadata)

    assertFalse(result)

    coVerify(exactly = 0) {
      repositoryActions.deleteNote(any())
    }
  }
}
