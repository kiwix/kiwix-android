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
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.dao.DownloadRoomDao
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookmarks
import org.kiwix.kiwixmobile.core.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimFileReader.Factory
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.files.FileSearch
import org.kiwix.kiwixmobile.core.utils.files.ScanningProgressListener
import org.kiwix.sharedFunctions.book
import org.kiwix.sharedFunctions.bookOnDisk
import org.kiwix.sharedFunctions.resetSchedulers
import org.kiwix.sharedFunctions.setScheduler
import java.io.File
import java.util.concurrent.TimeUnit

class StorageObserverTest {

  private val sharedPreferenceUtil: SharedPreferenceUtil = mockk()
  private val downloadRoomDao: DownloadRoomDao = mockk()
  private val fileSearch: FileSearch = mockk()
  private val downloadModel: DownloadModel = mockk()
  private val file: File = mockk()
  private val zimReaderSource: ZimReaderSource = mockk()
  private val readerFactory: Factory = mockk()
  private val zimFileReader: ZimFileReader = mockk()
  private val libkiwixBookmarks: LibkiwixBookmarks = mockk()
  private val scanningProgressListener: ScanningProgressListener = mockk()

  private val files: PublishProcessor<List<File>> = PublishProcessor.create()
  private val downloads: PublishProcessor<List<DownloadModel>> = PublishProcessor.create()

  private lateinit var storageObserver: StorageObserver

  init {
    setScheduler(Schedulers.trampoline())
  }

  @AfterAll
  fun teardown() {
    resetSchedulers()
  }

  @BeforeEach fun init() {
    clearAllMocks()
    every { sharedPreferenceUtil.prefStorage } returns "a"
    every { fileSearch.scan(scanningProgressListener) } returns files
    every { downloadRoomDao.downloads() } returns downloads
    every { zimFileReader.jniKiwixReader } returns mockk()
    every { runBlocking { readerFactory.create(zimReaderSource) } } returns zimFileReader
    storageObserver = StorageObserver(downloadRoomDao, fileSearch, readerFactory, libkiwixBookmarks)
  }

  @Test
  fun `books from disk are filtered by current downloads`() {
    withFiltering()
    booksOnFileSystem().assertValues(listOf())
  }

  @Test
  fun `zim files are read by the file reader`() {
    val expectedBook = book(
      "id", "title", "1", "favicon", "creator", "publisher", "date",
      "description", "language"
    )
    withNoFiltering()
    every { zimFileReader.toBook() } returns expectedBook
    every { zimFileReader.zimReaderSource } returns zimReaderSource
    booksOnFileSystem().assertValues(
      listOf(bookOnDisk(book = expectedBook, zimReaderSource = zimReaderSource))
    )
    verify { zimFileReader.dispose() }
  }

  private fun booksOnFileSystem() = storageObserver.getBooksOnFileSystem(scanningProgressListener)
    .test()
    .also {
      downloads.offer(listOf(downloadModel))
      files.offer(listOf(file))
      it.awaitDone(2, TimeUnit.SECONDS)
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
