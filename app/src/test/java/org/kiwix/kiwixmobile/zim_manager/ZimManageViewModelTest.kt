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

package org.kiwix.kiwixmobile.zim_manager

import android.app.Application
import com.jraska.livedata.test
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.Single
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.TestScheduler
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.kiwix.kiwixmobile.InstantExecutorExtension
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.book
import org.kiwix.kiwixmobile.bookOnDisk
import org.kiwix.kiwixmobile.data.DataSource
import org.kiwix.kiwixmobile.data.remote.KiwixService
import org.kiwix.kiwixmobile.database.newdb.dao.FetchDownloadDao
import org.kiwix.kiwixmobile.database.newdb.dao.NewBookDao
import org.kiwix.kiwixmobile.database.newdb.dao.NewLanguagesDao
import org.kiwix.kiwixmobile.downloadItem
import org.kiwix.kiwixmobile.downloadModel
import org.kiwix.kiwixmobile.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.language
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book
import org.kiwix.kiwixmobile.libraryNetworkEntity
import org.kiwix.kiwixmobile.resetSchedulers
import org.kiwix.kiwixmobile.setScheduler
import org.kiwix.kiwixmobile.utils.BookUtils
import org.kiwix.kiwixmobile.zim_manager.Fat32Checker.FileSystemState
import org.kiwix.kiwixmobile.zim_manager.Fat32Checker.FileSystemState.CanWrite4GbFile
import org.kiwix.kiwixmobile.zim_manager.Fat32Checker.FileSystemState.CannotWrite4GbFile
import org.kiwix.kiwixmobile.zim_manager.NetworkState.CONNECTED
import org.kiwix.kiwixmobile.zim_manager.NetworkState.NOT_CONNECTED
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.FileSelectListState
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.StorageObserver
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.adapter.BooksOnDiskListItem
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.BookOnDisk
import org.kiwix.kiwixmobile.zim_manager.library_view.adapter.LibraryListItem
import java.util.Locale
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS

@ExtendWith(InstantExecutorExtension::class)
class ZimManageViewModelTest {

  private val downloadDao: FetchDownloadDao = mockk()
  private val newBookDao: NewBookDao = mockk()
  private val newLanguagesDao: NewLanguagesDao = mockk()
  private val storageObserver: StorageObserver = mockk()
  private val kiwixService: KiwixService = mockk()
  private val application: Application = mockk()
  private val connectivityBroadcastReceiver: ConnectivityBroadcastReceiver = mockk()
  private val bookUtils: BookUtils = mockk()
  private val fat32Checker: Fat32Checker = mockk()
  private val defaultLanguageProvider: DefaultLanguageProvider = mockk()
  private val dataSource: DataSource = mockk()
  lateinit var viewModel: ZimManageViewModel

  private val downloads: PublishProcessor<List<DownloadModel>> = PublishProcessor.create()
  private val booksOnFileSystem: PublishProcessor<List<BookOnDisk>> = PublishProcessor.create()
  private val books: PublishProcessor<List<BookOnDisk>> = PublishProcessor.create()
  private val languages: PublishProcessor<List<Language>> = PublishProcessor.create()
  private val fileSystemStates: BehaviorProcessor<FileSystemState> = BehaviorProcessor.create()
  private val networkStates: PublishProcessor<NetworkState> = PublishProcessor.create()
  private val booksOnDiskListItems: PublishProcessor<List<BooksOnDiskListItem>> =
    PublishProcessor.create()

  private val testScheduler = TestScheduler()

  init {
    setScheduler(testScheduler)
  }

  @AfterAll
  fun teardown() {
    resetSchedulers()
  }

  @BeforeEach
  fun init() {
    clearAllMocks()
    every { connectivityBroadcastReceiver.action } returns "test"
    every { downloadDao.downloads() } returns downloads
    every { newBookDao.books() } returns books
    every { storageObserver.booksOnFileSystem } returns booksOnFileSystem
    every { newLanguagesDao.languages() } returns languages
    every { fat32Checker.fileSystemStates } returns fileSystemStates
    every { connectivityBroadcastReceiver.networkStates } returns networkStates
    every { application.registerReceiver(any(), any()) } returns mockk()
    every { dataSource.booksOnDiskAsListItems() } returns booksOnDiskListItems
    viewModel = ZimManageViewModel(
      downloadDao,
      newBookDao,
      newLanguagesDao,
      storageObserver,
      kiwixService,
      application,
      connectivityBroadcastReceiver,
      bookUtils,
      fat32Checker,
      defaultLanguageProvider,
      dataSource
    )
    testScheduler.triggerActions()
  }

  @Nested
  inner class Context {
    @Test
    fun `registers broadcastReceiver in init`() {
      verify {
        application.registerReceiver(connectivityBroadcastReceiver, any())
      }
    }

    @Test
    fun `unregisters broadcastReceiver in onCleared`() {
      every { application.unregisterReceiver(any()) } returns mockk()
      viewModel.onClearedExposed()
      verify {
        application.unregisterReceiver(connectivityBroadcastReceiver)
      }
    }
  }

  @Nested
  inner class Downloads {
    @Test
    fun `on emission from database render downloads`() {
      expectDownloads()
      viewModel.downloadItems
        .test()
        .assertValue(listOf(downloadItem()))
    }

    private fun expectDownloads(
      expectedDownloads: List<DownloadModel> = listOf(
        downloadModel()
      )
    ) {
      every { application.getString(any()) } returns ""
      downloads.offer(expectedDownloads)
      testScheduler.triggerActions()
      testScheduler.advanceTimeBy(1, SECONDS)
      testScheduler.triggerActions()
    }
  }

  @Nested
  inner class Books {
    @Test
    fun `emissions from dat source are observed`() {
      val expectedList = listOf(bookOnDisk())
      booksOnDiskListItems.onNext(expectedList)
      testScheduler.triggerActions()
      viewModel.fileSelectListStates.test()
        .assertValue(FileSelectListState(expectedList))
    }

    @Test
    fun `books found on filesystem are filtered by books already in db`() {
      every { application.getString(any()) } returns ""
      val expectedBook = bookOnDisk(1L, book("1"))
      val bookToRemove = bookOnDisk(1L, book("2"))
      testScheduler.triggerActions()
      viewModel.requestFileSystemCheck.onNext(Unit)
      testScheduler.triggerActions()
      books.onNext(listOf(bookToRemove))
      testScheduler.triggerActions()
      booksOnFileSystem.onNext(
        listOf(
          expectedBook,
          expectedBook,
          bookToRemove
        )
      )
      testScheduler.triggerActions()
      verify {
        newBookDao.insert(listOf(expectedBook))
      }
    }
  }

  @Nested
  inner class Lanuages {

    @Test
    fun `network no result & empty language db activates the default locale`() {
      val expectedLanguage = Language(
        active = true,
        occurencesOfLanguage = 1,
        language = "eng",
        languageLocalized = "englocal",
        languageCode = "ENG",
        languageCodeISO2 = "en"
      )
      expectNetworkDbAndDefault(
        listOf(),
        listOf(),
        expectedLanguage
      )
      verify { newLanguagesDao.insert(listOf(expectedLanguage)) }
    }

    @Test
    fun `network no result & a language db result triggers nothing`() {
      expectNetworkDbAndDefault(
        listOf(),
        listOf(
          Language(
            active = true,
            occurencesOfLanguage = 1,
            language = "eng",
            languageLocalized = "englocal",
            languageCode = "ENG",
            languageCodeISO2 = "en"
          )
        ),
        language(isActive = true, occurencesOfLanguage = 1)
      )
      verify(exactly = 0) { newLanguagesDao.insert(any()) }
    }

    @Test
    fun `network result & empty language db triggers combined result of default + network`() {
      val defaultLanguage = Language(
        active = true,
        occurencesOfLanguage = 1,
        language = "English",
        languageLocalized = "English",
        languageCode = "eng",
        languageCodeISO2 = "eng"
      )
      expectNetworkDbAndDefault(
        listOf(
          book(language = "eng"),
          book(language = "eng"),
          book(language = "fra")
        ),
        listOf(),
        defaultLanguage
      )
      verify {
        newLanguagesDao.insert(
          listOf(
            defaultLanguage.copy(occurencesOfLanguage = 2),
            Language(
              active = false,
              occurencesOfLanguage = 1,
              language = "fra",
              languageLocalized = "",
              languageCode = "",
              languageCodeISO2 = ""
            )
          )
        )
      }
    }

    @Test
    fun `network result & language db results activates a combined network + db result`() {
      val dbLanguage = Language(
        active = true,
        occurencesOfLanguage = 1,
        language = "English",
        languageLocalized = "English",
        languageCode = "eng",
        languageCodeISO2 = "eng"
      )
      expectNetworkDbAndDefault(
        listOf(
          book(language = "eng"),
          book(language = "eng"),
          book(language = "fra")
        ),
        listOf(dbLanguage),
        language(isActive = true, occurencesOfLanguage = 1)
      )
      verify {
        newLanguagesDao.insert(
          listOf(
            dbLanguage.copy(occurencesOfLanguage = 2),
            Language(
              active = false,
              occurencesOfLanguage = 1,
              language = "fra",
              languageLocalized = "",
              languageCode = "",
              languageCodeISO2 = ""
            )
          )
        )
      }
    }

    private fun expectNetworkDbAndDefault(
      networkBooks: List<Book>,
      dbBooks: List<Language>,
      defaultLanguage: Language
    ) {
      every { application.getString(any()) } returns ""
      every { kiwixService.library } returns Single.just(
        libraryNetworkEntity(networkBooks)
      )
      every { defaultLanguageProvider.provide() } returns defaultLanguage
      languages.onNext(dbBooks)
      testScheduler.triggerActions()
      networkStates.onNext(CONNECTED)
      testScheduler.triggerActions()
    }
  }

  @Test
  fun `network states observed`() {
    networkStates.offer(NOT_CONNECTED)
    viewModel.networkStates.test()
      .assertValue(NOT_CONNECTED)
  }

  @Test
  fun `library update removes from sources`() {
    every { application.getString(R.string.your_languages) } returns "1"
    every { application.getString(R.string.other_languages) } returns "2"
    val bookAlreadyOnDisk = book(
      id = "0",
      url = "",
      language = Locale.ENGLISH.language
    )
    val bookDownloading = book(
      id = "1",
      url = ""
    )
    val bookWithActiveLanguage = book(
      id = "3",
      language = "activeLanguage",
      url = ""
    )
    val bookWithInactiveLanguage = book(
      id = "3",
      language = "inactiveLanguage",
      url = ""
    )
    every { kiwixService.library } returns Single.just(
      libraryNetworkEntity(
        listOf(
          bookAlreadyOnDisk,
          bookDownloading,
          bookWithActiveLanguage,
          bookWithInactiveLanguage
        )
      )
    )
    networkStates.onNext(CONNECTED)
    downloads.onNext(listOf(downloadModel(book = bookDownloading)))
    books.onNext(listOf(bookOnDisk(book = bookAlreadyOnDisk)))
    languages.onNext(
      listOf(
        language(isActive = true, occurencesOfLanguage = 1, languageCode = "activeLanguage"),
        language(isActive = false, occurencesOfLanguage = 1, languageCode = "inactiveLanguage")
      )
    )
    fileSystemStates.onNext(CanWrite4GbFile)
    testScheduler.advanceTimeBy(500, MILLISECONDS)
    testScheduler.triggerActions()
    viewModel.libraryItems.test()
      .assertValue(
        listOf(
          LibraryListItem.DividerItem(Long.MAX_VALUE, "1"),
          LibraryListItem.BookItem(bookWithActiveLanguage),
          LibraryListItem.DividerItem(Long.MIN_VALUE, "2"),
          LibraryListItem.BookItem(bookWithInactiveLanguage)
        )
      )
  }

  @Test
  fun `library filters out files over 4GB if file system state says to`() {
    val bookOver4Gb = book(
      id = "0",
      url = "",
      size = "${Fat32Checker.FOUR_GIGABYTES_IN_KILOBYTES + 1}"
    )
    every { kiwixService.library } returns Single.just(
      libraryNetworkEntity(
        listOf(bookOver4Gb)
      )
    )
    networkStates.onNext(CONNECTED)
    downloads.onNext(listOf())
    books.onNext(listOf())
    languages.onNext(
      listOf(
        language(isActive = true, occurencesOfLanguage = 1, languageCode = "activeLanguage")
      )
    )
    fileSystemStates.onNext(CannotWrite4GbFile)
    testScheduler.advanceTimeBy(500, MILLISECONDS)
    testScheduler.triggerActions()
    viewModel.libraryItems.test()
      .assertValue(listOf())
  }
}
