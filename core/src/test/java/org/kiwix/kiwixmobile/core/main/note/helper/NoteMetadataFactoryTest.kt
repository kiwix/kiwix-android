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
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.kiwix.kiwixmobile.core.main.AddNoteDialogConfig
import org.kiwix.kiwixmobile.core.page.notes.models.NoteListItem
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.utils.StorageUtils

class NoteMetadataFactoryTest {
  private lateinit var context: Context
  private lateinit var factory: NoteMetadataFactory
  private lateinit var zimReaderContainer: ZimReaderContainer

  @Before
  fun setUp() {
    context = mockk(relaxed = true)
    zimReaderContainer = mockk(relaxed = true)
    mockkObject(StorageUtils)
    every { StorageUtils.getNotesDirectory(context) } returns "/notes/"

    factory = NoteMetadataFactory(context)
  }

  @After
  fun tearDown() {
    unmockkObject(StorageUtils)
    unmockkAll()
  }

  @Test
  fun `create uses noteListItem values when available`() {
    val noteListItem = mockk<NoteListItem>()

    every { noteListItem.title } returns "Wikipedia: Earth"
    every { noteListItem.zimId } returns "zim-id"
    every { noteListItem.zimUrl } returns "A/Earth.html"
    every { noteListItem.noteFilePath } returns ""
    every { noteListItem.favicon } returns "favicon"

    val readerSource = mockk<ZimReaderSource>()
    every { noteListItem.zimReaderSource } returns readerSource
    every { readerSource.toDatabase() } returns "wikipedia_en.zim"

    val config = AddNoteDialogConfig(
      noteListItem = noteListItem
    )

    val metadata = factory.create(config, zimReaderContainer)

    assertEquals("zim-id", metadata.zimId)
    assertEquals("Wikipedia: Earth", metadata.zimFileTitle)
    assertEquals("Earth", metadata.articleTitle)
    assertEquals("A/Earth.html", metadata.zimFileUrl)
    assertEquals("favicon", metadata.favicon)
  }

  @Test
  fun `create uses reader container when noteListItem is null`() {
    every { zimReaderContainer.name } returns "wikipedia_en.zim"
    every { zimReaderContainer.zimFileTitle } returns "Wikipedia"
    every { zimReaderContainer.id } returns "reader-id"

    val readerSource = mockk<ZimReaderSource>()
    every { zimReaderContainer.zimReaderSource } returns readerSource
    every { readerSource.toDatabase() } returns "wikipedia_en.zim"

    val config = AddNoteDialogConfig(
      articleTitle = "Earth",
      currentWebViewUrl = "A/Earth.html"
    )

    val metadata = factory.create(config, zimReaderContainer)

    assertEquals("reader-id", metadata.zimId)
    assertEquals("Wikipedia", metadata.zimFileTitle)
    assertEquals("Earth", metadata.articleTitle)
    assertEquals("A/Earth.html", metadata.zimFileUrl)
  }

  @Test
  fun `create extracts article note file name from url`() {
    every { zimReaderContainer.name } returns "wikipedia_en.zim"

    val metadata = factory.create(
      AddNoteDialogConfig(
        articleTitle = "Earth",
        currentWebViewUrl = "A/Earth.html"
      ),
      zimReaderContainer
    )

    assertEquals("Earth", metadata.articleNoteFileName)
  }

  @Test
  fun `create falls back to article title when url missing`() {
    every { zimReaderContainer.name } returns "wikipedia_en.zim"

    val metadata = factory.create(
      AddNoteDialogConfig(
        articleTitle = "Earth"
      ),
      zimReaderContainer
    )

    assertEquals("Earth", metadata.articleNoteFileName)
  }

  @Test
  fun `create uses note file path when editing existing note`() {
    val noteListItem = mockk<NoteListItem>()

    every { noteListItem.title } returns "Wikipedia: Earth"
    every { noteListItem.zimUrl } returns "A/Earth.html"
    every { noteListItem.zimId } returns "id"
    every { noteListItem.favicon } returns "favicon"

    val readerSource = mockk<ZimReaderSource>()
    every { noteListItem.zimReaderSource } returns readerSource
    every { readerSource.toDatabase() } returns "wikipedia_en.zim"

    every {
      noteListItem.noteFilePath
    } returns "/notes/wiki/Earth.txt"

    val metadata = factory.create(
      AddNoteDialogConfig(noteListItem = noteListItem),
      zimReaderContainer
    )

    assertEquals("Earth", metadata.articleNoteFileName)
    assertEquals("/notes/wiki/", metadata.zimNotesDirectory)
  }

  @Test
  fun `create sets isZimFileExist true when zim file exists`() {
    every { zimReaderContainer.name } returns "wikipedia_en.zim"

    val metadata = factory.create(
      AddNoteDialogConfig(articleTitle = "Earth"),
      zimReaderContainer
    )

    assertTrue(metadata.isZimFileExist)
  }

  @Test
  fun `create sets isZimFileExist false when zim file missing`() {
    every { zimReaderContainer.zimReaderSource } returns null
    every { zimReaderContainer.name } returns null

    val metadata = factory.create(
      AddNoteDialogConfig(articleTitle = "Earth"),
      zimReaderContainer
    )

    assertFalse(metadata.isZimFileExist)
  }

  @Test
  fun `getNoteTitle returns zim title when reader source exists`() {
    val metadata = NoteMetadata(
      zimFileName = null,
      zimFileTitle = "Wikipedia",
      zimId = "",
      zimReaderSource = mockk(),
      favicon = null,
      articleTitle = "Earth",
      zimFileUrl = "",
      zimNoteDirectoryName = "",
      articleNoteFileName = "",
      zimNotesDirectory = "",
      isZimFileExist = true
    )

    assertEquals(
      "Wikipedia",
      metadata.getNoteTitle()
    )
  }

  @Test
  fun `getNoteTitle returns combined title when reader source is null`() {
    val metadata = NoteMetadata(
      zimFileName = null,
      zimFileTitle = "Wikipedia",
      zimId = "",
      zimReaderSource = null,
      favicon = null,
      articleTitle = "Earth",
      zimFileUrl = "",
      zimNoteDirectoryName = "",
      articleNoteFileName = "",
      zimNotesDirectory = "",
      isZimFileExist = true
    )

    assertEquals(
      "Wikipedia: Earth",
      metadata.getNoteTitle()
    )
  }
}
