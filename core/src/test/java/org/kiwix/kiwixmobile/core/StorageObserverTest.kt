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
import io.mockk.mockkStatic
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.dao.FetchDownloadDao
import org.kiwix.kiwixmobile.core.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimFileReader.Factory
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.files.FileSearch
import org.kiwix.sharedFunctions.book
import org.kiwix.sharedFunctions.bookOnDisk
import org.kiwix.sharedFunctions.resetSchedulers
import org.kiwix.sharedFunctions.setScheduler
import java.io.File

class StorageObserverTest {

  private val sharedPreferenceUtil: SharedPreferenceUtil = mockk()
  private val downloadDao: FetchDownloadDao = mockk()
  private val fileSearch: FileSearch = mockk()
  private val downloadModel: DownloadModel = mockk()
  private val file: File = mockk()
  private val readerFactory: Factory = mockk()
  private val zimFileReader: ZimFileReader

  private val files: PublishProcessor<List<File>> = PublishProcessor.create()
  private val downloads: PublishProcessor<List<DownloadModel>> = PublishProcessor.create()

  private lateinit var storageObserver: StorageObserver

  init {
    setScheduler(Schedulers.trampoline())
    mockkStatic(CoreApp::class)
    every { CoreApp.getInstance().packageName } returns "pkg"
    zimFileReader = mockk()
  }

  @AfterAll
  fun teardown() {
    resetSchedulers()
  }

  @BeforeEach fun init() {
    clearAllMocks()
    every { sharedPreferenceUtil.prefStorage } returns "a"
    every { fileSearch.scan() } returns files
    every { downloadDao.downloads() } returns downloads
    every { readerFactory.create(file) } returns zimFileReader
    storageObserver = StorageObserver(
      downloadDao,
      fileSearch,
      readerFactory
    )
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
    booksOnFileSystem().assertValues(
      listOf(bookOnDisk(book = expectedBook, file = file))
    )
  }

  private fun booksOnFileSystem() = storageObserver.booksOnFileSystem
    .test()
    .also {
      downloads.offer(listOf(downloadModel))
      files.offer(listOf(file))
    }

  private fun withFiltering() {
    every { downloadModel.fileNameFromUrl } returns "test"
    every { file.absolutePath } returns "This is a test"
  }

  private fun withNoFiltering() {
    every { downloadModel.fileNameFromUrl } returns "test"
    every { file.absolutePath } returns "This won't match"
  }
}
