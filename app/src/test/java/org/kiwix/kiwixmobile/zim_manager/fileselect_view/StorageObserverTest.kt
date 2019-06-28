package org.kiwix.kiwixmobile.zim_manager.fileselect_view

/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
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
 */

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.book
import org.kiwix.kiwixmobile.bookOnDisk
import org.kiwix.kiwixmobile.data.ZimContentProvider
import org.kiwix.kiwixmobile.database.newdb.dao.NewDownloadDao
import org.kiwix.kiwixmobile.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.resetSchedulers
import org.kiwix.kiwixmobile.setScheduler
import org.kiwix.kiwixmobile.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.utils.files.FileSearch
import java.io.File

class StorageObserverTest {

  private val sharedPreferenceUtil: SharedPreferenceUtil = mockk()
  private val newDownloadDao: NewDownloadDao = mockk()
  private val fileSearch: FileSearch = mockk()
  private val downloadModel = mockk<DownloadModel>()
  private val file = mockk<File>()

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
    every { fileSearch.scan("a") } returns files
    every { newDownloadDao.downloads() } returns downloads
    storageObserver = StorageObserver(sharedPreferenceUtil, newDownloadDao, fileSearch)
  }

  @Test
  fun `books from disk are filtered by current downloads`() {
    every { downloadModel.fileNameFromUrl } returns "test"
    every { file.absolutePath } returns "This is a test"
    storageObserver.booksOnFileSystem
        .test()
        .also {
          downloads.offer(listOf(downloadModel))
          files.offer(listOf(file))
        }
        .assertValues(listOf())
  }

  @Test
  fun `null books from ZimContentProvider are filtered out`() {
    every { downloadModel.fileNameFromUrl } returns "test"
    every { file.absolutePath } returns "This won't match"

    storageObserver.booksOnFileSystem
        .test()
        .also {
          downloads.offer(listOf(downloadModel))
          files.offer(listOf(file))
        }
        .assertValues(listOf())
  }

  @Test
  fun `iterable ZimContentProvider with zim file produces a book`() {
    val expectedBook = book(
        "id", "title", "1", "favicon", "creator", "publisher", "date",
        "description", "language"
    )
    mockkStatic(ZimContentProvider::class)
    every { downloadModel.fileNameFromUrl } returns "test"
    every { file.absolutePath } returns "This won't match"

    ZimContentProvider.canIterate = true
    every { ZimContentProvider.setZimFile("This won't match") } returns ""

    every { ZimContentProvider.getZimFileTitle() } returns expectedBook.title
    every { ZimContentProvider.getId() } returns expectedBook.id
    every { ZimContentProvider.getFileSize() } returns expectedBook.size.toInt()
    every { ZimContentProvider.getFavicon() } returns expectedBook.favicon
    every { ZimContentProvider.getCreator() } returns expectedBook.creator
    every { ZimContentProvider.getPublisher() } returns expectedBook.publisher
    every { ZimContentProvider.getDate() } returns expectedBook.date
    every { ZimContentProvider.getDescription() } returns expectedBook.description
    every { ZimContentProvider.getLanguage() } returns expectedBook.language

    storageObserver.booksOnFileSystem
        .test()
        .also {
          downloads.offer(listOf(downloadModel))
          files.offer(listOf(file))
        }
        .assertValues(
            listOf(
                bookOnDisk(
                    book = expectedBook,
                    file = file,
                    databaseId = null
                )

            )
        )
  }
}
