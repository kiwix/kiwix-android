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

package org.kiwix.kiwixmobile.zimManager.libraryView

import com.tonyodev.fetch2.Status
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.downloader.model.DownloadModel
import com.tonyodev.fetch2.Error
import org.kiwix.kiwixmobile.core.downloader.model.Seconds
import org.kiwix.kiwixmobile.core.entity.LibkiwixBook
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.Companion.FOUR_GIGABYTES_IN_KILOBYTES
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.DetectingFileSystem
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.CanWrite4GbFile
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.CannotWrite4GbFile
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.NotEnoughSpaceFor4GbFile
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState
import org.kiwix.kiwixmobile.zimManager.libraryView.LibraryListItem.BookItem
import org.kiwix.kiwixmobile.zimManager.libraryView.LibraryListItem.DividerItem
import org.kiwix.kiwixmobile.zimManager.libraryView.LibraryListItem.LibraryDownloadItem

internal class LibraryListItemTest {
  private val book = mockk<LibkiwixBook>()

  @BeforeEach
  fun init() {
    clearAllMocks()
    every { book.id } returns "0"
    every { book.size } returns "0"
    every { book.tags } returns null
  }

  @Test
  fun `BookItem should map tags from book`() {
    every { book.tags } returns "wikipedia;science"

    val item = BookItem(book, CanWrite4GbFile)

    assertThat(item.tags).isNotEmpty
  }

  @Test
  fun `BookItem should generate id from book id`() {
    every { book.id } returns "abc"

    val item = BookItem(book, CanWrite4GbFile)

    assertThat(item.id).isEqualTo("abc".hashCode().toLong())
  }

  @Test
  internal fun `Unknown file system state files under 4GB can be downloaded`() {
    assertThat(canBeDownloaded(book, DetectingFileSystem)).isTrue
  }

  @Test
  internal fun `Unknown file system state greater than 4GB can't be downloaded`() {
    every { book.size } returns (FOUR_GIGABYTES_IN_KILOBYTES + 1).toString()
    assertThat(canBeDownloaded(book, DetectingFileSystem)).isFalse
  }

  @Test
  internal fun `Unknown file system state empty size can be downloaded`() {
    every { book.size } returns ""
    assertThat(canBeDownloaded(book, DetectingFileSystem)).isTrue
  }

  @Test
  internal fun `CannotWrite4GbFile file system state can be downloaded`() {
    assertThat(canBeDownloaded(book, CannotWrite4GbFile)).isTrue
  }

  @Test
  internal fun `CannotWrite4GbFile file system state cannot be downloaded if file is too big`() {
    every { book.size } returns (FOUR_GIGABYTES_IN_KILOBYTES + 1).toString()
    assertThat(canBeDownloaded(book, CannotWrite4GbFile)).isFalse
  }

  @Test
  internal fun `CanWrite4GbFile file system state can be downloaded`() {
    assertThat(canBeDownloaded(book, CanWrite4GbFile)).isTrue
  }

  @Test
  internal fun `NotEnoughSpaceFor4GbFile file system state can be downloaded`() {
    assertThat(canBeDownloaded(book, NotEnoughSpaceFor4GbFile)).isTrue
  }

  private fun canBeDownloaded(book: LibkiwixBook, fileSystemState: FileSystemState) =
    BookItem(book, fileSystemState).canBeDownloaded

  @Test
  fun `DividerItem should store values correctly`() {
    val item = DividerItem(
      id = 1L,
      sectionTitle = "Recent"
    )

    assertThat(item.id).isEqualTo(1L)
    assertThat(item.sectionTitle).isEqualTo("Recent")
  }

  @Test
  fun `LibraryDownloadItem constructor should map DownloadModel correctly`() {
    val book = mockk<LibkiwixBook>()

    every { book.id } returns "book-id"
    every { book.favicon } returns "favicon-url"
    every { book.title } returns "Wikipedia"
    every { book.description } returns "Description"
    every { book.url } returns "url"

    val downloadModel = mockk<DownloadModel>()

    every { downloadModel.downloadId } returns 10L
    every { downloadModel.book } returns book
    every { downloadModel.bytesDownloaded } returns 100L
    every { downloadModel.totalSizeOfDownload } returns 1000L
    every { downloadModel.progress } returns 10
    every { downloadModel.etaInMilliSeconds } returns 0L
    every { downloadModel.state } returns Status.DOWNLOADING
    every { downloadModel.error } returns Error.NONE

    val item = LibraryDownloadItem(downloadModel)

    assertThat(item.downloadId).isEqualTo(10L)
    assertThat(item.title).isEqualTo("Wikipedia")
    assertThat(item.description).isEqualTo("Description")
    assertThat(item.bytesDownloaded).isEqualTo(100L)
    assertThat(item.totalSizeBytes).isEqualTo(1000L)
    assertThat(item.progress).isEqualTo(10)
    assertThat(item.eta.seconds).isEqualTo(0L)
    assertThat(item.currentDownloadState).isEqualTo(Status.DOWNLOADING)
    assertThat(item.downloadError).isEqualTo(Error.NONE)
  }

  @Test
  fun `readableEta should be empty when eta is zero`() {
    val item = LibraryListItem.LibraryDownloadItem(
      downloadId = 1L,
      favIconUrl = "",
      title = "",
      description = "",
      bytesDownloaded = 0L,
      totalSizeBytes = 0L,
      progress = 0,
      eta = Seconds(0),
      downloadState = mockk(),
      id = 1L,
      currentDownloadState = Status.NONE,
      downloadError = Error.NONE
    )

    assertThat(item.readableEta).isEmpty()
  }
}
