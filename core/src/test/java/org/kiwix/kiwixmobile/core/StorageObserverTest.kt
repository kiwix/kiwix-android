/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.dao.DownloadRoomDao
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookmarks
import org.kiwix.kiwixmobile.core.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimFileReader.Factory
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.files.FileSearch
import org.kiwix.kiwixmobile.core.utils.files.ScanningProgressListener
import org.kiwix.kiwixmobile.core.utils.files.testFlow
import org.kiwix.libkiwix.Book
import org.kiwix.libkiwix.Illustration
import org.kiwix.libzim.Archive
import org.kiwix.sharedFunctions.libkiwixBook
import java.io.File

class StorageObserverTest {
  private val kiwixDataStore: KiwixDataStore = mockk()
  private val downloadRoomDao: DownloadRoomDao = mockk()
  private val fileSearch: FileSearch = mockk()
  private val downloadModel: DownloadModel = mockk()
  private val file: File = mockk()
  private val zimReaderSource: ZimReaderSource = mockk()
  private val readerFactory: Factory = mockk()
  private val zimFileReader: ZimFileReader = mockk()
  private val libkiwixBookmarks: LibkiwixBookmarks = mockk()
  private val scanningProgressListener: ScanningProgressListener = mockk()

  private val files = MutableStateFlow<List<File>>(emptyList())
  private val downloads = MutableStateFlow<List<DownloadModel>>(emptyList())
  private val libkiwixBookFactory: LibkiwixBookFactory = mockk()
  private val libkiwixBook: Book = BookTestWrapper("id")

  private lateinit var storageObserver: StorageObserver

  @BeforeEach fun init() {
    clearAllMocks()
    coEvery { kiwixDataStore.selectedStorage } returns flowOf("a")
    every { fileSearch.scan(scanningProgressListener) } returns files
    every { downloadRoomDao.downloads() } returns downloads
    coEvery { libkiwixBookmarks.addBookToLibrary(any()) } returns Unit
    every { zimFileReader.jniKiwixReader } returns mockk()
    every { runBlocking { readerFactory.create(zimReaderSource, false) } } returns zimFileReader
    every { libkiwixBookFactory.create() } returns libkiwixBook
    storageObserver = StorageObserver(
      downloadRoomDao,
      fileSearch,
      readerFactory,
      libkiwixBookmarks,
      libkiwixBookFactory
    )
  }

  @Test
  fun `books from disk are filtered by current downloads`() = runTest {
    withFiltering()
    testFlow(
      flow = booksOnFileSystem(),
      triggerAction = {},
      assert = { assertThat(awaitItem()).isEqualTo(listOf<Book>()) }
    )
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `zim files are read by the file reader`() = runTest {
    val expectedBook =
      libkiwixBook(
        "id", "title", "1", "favicon", "creator", "publisher", "date",
        "description", "language", nativeBook = libkiwixBook
      )
    withNoFiltering()
    every { zimFileReader.toBook() } returns expectedBook
    every { zimFileReader.zimReaderSource } returns zimReaderSource
    testFlow(
      flow = booksOnFileSystem(),
      triggerAction = {},
      assert = {
        assertThat(awaitItem()).isEqualTo(
          listOfNotNull<Book>(
            expectedBook.nativeBook
          )
        )
      }
    )
    // test the book is added to bookmark's library.
    coVerify { libkiwixBookmarks.addBookToLibrary(archive = any()) }
    verify { zimFileReader.dispose() }
  }

  private fun booksOnFileSystem() =
    storageObserver.getBooksOnFileSystem(scanningProgressListener)
      .also {
        downloads.value = listOf(downloadModel)
        files.value = listOf(file)
      }

  private fun withFiltering() {
    every { downloadModel.fileNameFromUrl } returns "test"
    every { file.absolutePath } returns "This is a test"
  }

  private fun withNoFiltering() {
    every { downloadModel.fileNameFromUrl } returns "test"
    every { file.absolutePath } returns "This won't match"
    every { file.canonicalPath } returns "This won't match"
    every { zimReaderSource.file } returns file
  }
}

class BookTestWrapper(
  private val id: String,
  private val bookTitle: String = "",
  private val bookDescription: String = "",
  private val bookLanguage: String = "",
  private val bookCreator: String = "",
  private val bookPublisher: String = "",
  private val bookDate: String = "",
  private val bookUrl: String = "",
  private val bookArticleCount: String = "",
  private val bookMediaCount: String = "",
  private val bookSize: String = "",
  private val bookPath: String = "",
  private val bookName: String = "",
  private val bookTags: String = ""
) : Book(0L) {
  override fun getId(): String = id
  override fun getTitle(): String = bookTitle
  override fun getDescription(): String = bookDescription
  override fun getLanguage(): String = bookLanguage
  override fun getCreator(): String = bookCreator
  override fun getPublisher(): String = bookPublisher
  override fun getDate(): String = bookDate
  override fun getUrl(): String = bookUrl
  override fun getArticleCount(): Long = bookArticleCount.toLong()
  override fun getMediaCount(): Long = bookMediaCount.toLong()
  override fun getSize(): Long = bookSize.toLong()
  override fun getPath(): String = bookPath
  override fun getName(): String = bookName
  override fun getTags(): String = bookTags

  // Returning null due to not construing the `Illustration` since it is libkiwix internal class.
  override fun getIllustration(size: Int): Illustration? = null
  override fun equals(other: Any?): Boolean = other is BookTestWrapper && getId() == other.getId()
  override fun hashCode(): Int = getId().hashCode()
  override fun update(archive: Archive?) {
    // do nothing
  }
}
