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

package org.kiwix.kiwixmobile.zim_manager

import android.app.Application
import android.net.ConnectivityManager
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
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.StorageObserver
import org.kiwix.kiwixmobile.core.dao.FetchDownloadDao
import org.kiwix.kiwixmobile.core.dao.NewBookDao
import org.kiwix.kiwixmobile.core.dao.NewLanguagesDao
import org.kiwix.kiwixmobile.core.data.DataSource
import org.kiwix.kiwixmobile.core.data.remote.KiwixService
import org.kiwix.kiwixmobile.core.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity.Book
import org.kiwix.kiwixmobile.core.utils.BookUtils
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.zim_manager.Language
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.SelectionMode.MULTI
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.SelectionMode.NORMAL
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.BookOnDisk
import org.kiwix.kiwixmobile.zim_manager.Fat32Checker.FileSystemState
import org.kiwix.kiwixmobile.zim_manager.Fat32Checker.FileSystemState.CanWrite4GbFile
import org.kiwix.kiwixmobile.zim_manager.Fat32Checker.FileSystemState.CannotWrite4GbFile
import org.kiwix.kiwixmobile.zim_manager.NetworkState.CONNECTED
import org.kiwix.kiwixmobile.zim_manager.NetworkState.NOT_CONNECTED
import org.kiwix.kiwixmobile.zim_manager.ZimManageViewModel.FileSelectActions.MultiModeFinished
import org.kiwix.kiwixmobile.zim_manager.ZimManageViewModel.FileSelectActions.RequestDeleteMultiSelection
import org.kiwix.kiwixmobile.zim_manager.ZimManageViewModel.FileSelectActions.RequestMultiSelection
import org.kiwix.kiwixmobile.zim_manager.ZimManageViewModel.FileSelectActions.RequestSelect
import org.kiwix.kiwixmobile.zim_manager.ZimManageViewModel.FileSelectActions.RequestShareMultiSelection
import org.kiwix.kiwixmobile.zim_manager.ZimManageViewModel.FileSelectActions.RestartActionMode
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.FileSelectListState
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.effects.DeleteFiles
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.effects.None
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.effects.ShareFiles
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.effects.StartMultiSelection
import org.kiwix.kiwixmobile.zim_manager.library_view.adapter.LibraryListItem
import org.kiwix.sharedFunctions.InstantExecutorExtension
import org.kiwix.sharedFunctions.book
import org.kiwix.sharedFunctions.bookOnDisk
import org.kiwix.sharedFunctions.downloadModel
import org.kiwix.sharedFunctions.language
import org.kiwix.sharedFunctions.libraryNetworkEntity
import org.kiwix.sharedFunctions.resetSchedulers
import org.kiwix.sharedFunctions.setScheduler
import java.util.Locale
import java.util.concurrent.TimeUnit.MILLISECONDS

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
  private val connectivityManager: ConnectivityManager = mockk()
  private val sharedPreferenceUtil: SharedPreferenceUtil = mockk()
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
    every { connectivityManager.activeNetworkInfo?.type } returns ConnectivityManager.TYPE_WIFI
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
      dataSource,
      connectivityManager,
      sharedPreferenceUtil
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
  fun `library update removes from sources and maps to list items`() {
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
          LibraryListItem.DividerItem(Long.MAX_VALUE, R.string.your_languages),
          LibraryListItem.BookItem(bookWithActiveLanguage, CanWrite4GbFile),
          LibraryListItem.DividerItem(Long.MIN_VALUE, R.string.other_languages),
          LibraryListItem.LibraryDownloadItem(downloadModel(book = bookDownloading))
        )
      )
  }

  @Test
  fun `library marks files over 4GB as can't download if file system state says to`() {
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
      .assertValue(
        listOf(
          LibraryListItem.DividerItem(Long.MIN_VALUE, R.string.other_languages),
          LibraryListItem.BookItem(bookOver4Gb, CannotWrite4GbFile)
        )
      )
  }

  @Nested
  inner class SideEffects {

    @Test
    fun `RequestMultiSelection offers StartMultiSelection and selects a book`() {
      val bookToSelect = bookOnDisk(databaseId = 0L)
      val unSelectedBook = bookOnDisk(databaseId = 1L)
      viewModel.fileSelectListStates.value = FileSelectListState(
        listOf(
          bookToSelect,
          unSelectedBook
        ),
        NORMAL
      )
      viewModel.sideEffects.test()
        .also { viewModel.fileSelectActions.offer(RequestMultiSelection(bookToSelect)) }
        .assertValues(StartMultiSelection(viewModel.fileSelectActions))
      viewModel.fileSelectListStates.test()
        .assertValue(
          FileSelectListState(
            listOf(bookToSelect.apply { isSelected = !isSelected }, unSelectedBook),
            MULTI
          )
        )
    }

    @Test
    fun `RequestDeleteMultiSelection offers DeleteFiles with selected books`() {
      val selectedBook = bookOnDisk().apply { isSelected = true }
      viewModel.fileSelectListStates.value =
        FileSelectListState(listOf(selectedBook, bookOnDisk()), NORMAL)
      viewModel.sideEffects.test()
        .also { viewModel.fileSelectActions.offer(RequestDeleteMultiSelection) }
        .assertValues(DeleteFiles(listOf(selectedBook)))
    }

    @Test
    fun `RequestShareMultiSelection offers ShareFiles with selected books`() {
      val selectedBook = bookOnDisk().apply { isSelected = true }
      viewModel.fileSelectListStates.value =
        FileSelectListState(listOf(selectedBook, bookOnDisk()), NORMAL)
      viewModel.sideEffects.test()
        .also { viewModel.fileSelectActions.offer(RequestShareMultiSelection) }
        .assertValues(ShareFiles(listOf(selectedBook)))
    }

    @Test
    fun `MultiModeFinished offers None`() {
      val selectedBook = bookOnDisk().apply { isSelected = true }
      viewModel.fileSelectListStates.value =
        FileSelectListState(listOf(selectedBook, bookOnDisk()), NORMAL)
      viewModel.sideEffects.test()
        .also { viewModel.fileSelectActions.offer(MultiModeFinished) }
        .assertValues(None)
      viewModel.fileSelectListStates.test().assertValue(
        FileSelectListState(
          listOf(
            selectedBook.apply { isSelected = false },
            bookOnDisk()
          )
        )
      )
    }

    @Test
    fun `RequestSelect offers None and inverts selection`() {
      val selectedBook = bookOnDisk(0L).apply { isSelected = true }
      viewModel.fileSelectListStates.value =
        FileSelectListState(listOf(selectedBook, bookOnDisk(1L)), NORMAL)
      viewModel.sideEffects.test()
        .also { viewModel.fileSelectActions.offer(RequestSelect(selectedBook)) }
        .assertValues(None)
      viewModel.fileSelectListStates.test().assertValue(
        FileSelectListState(
          listOf(
            selectedBook.apply { isSelected = false },
            bookOnDisk(1L)
          )
        )
      )
    }

    @Test
    fun `RestartActionMode offers StartMultiSelection`() {
      viewModel.sideEffects.test()
        .also { viewModel.fileSelectActions.offer(RestartActionMode) }
        .assertValues(StartMultiSelection(viewModel.fileSelectActions))
    }
  }
}
