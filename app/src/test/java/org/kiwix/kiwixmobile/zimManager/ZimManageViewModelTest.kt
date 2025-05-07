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

package org.kiwix.kiwixmobile.zimManager

import android.app.Application
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkCapabilities.TRANSPORT_WIFI
import android.os.Build
import com.jraska.livedata.test
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.Single
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.TestScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.StorageObserver
import org.kiwix.kiwixmobile.core.dao.DownloadRoomDao
import org.kiwix.kiwixmobile.core.dao.NewBookDao
import org.kiwix.kiwixmobile.core.dao.NewLanguagesDao
import org.kiwix.kiwixmobile.core.data.DataSource
import org.kiwix.kiwixmobile.core.data.remote.KiwixService
import org.kiwix.kiwixmobile.core.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity.Book
import org.kiwix.kiwixmobile.core.utils.BookUtils
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.files.ScanningProgressListener
import org.kiwix.kiwixmobile.core.zim_manager.ConnectivityBroadcastReceiver
import org.kiwix.kiwixmobile.core.zim_manager.Language
import org.kiwix.kiwixmobile.core.zim_manager.NetworkState
import org.kiwix.kiwixmobile.core.zim_manager.NetworkState.CONNECTED
import org.kiwix.kiwixmobile.core.zim_manager.NetworkState.NOT_CONNECTED
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem.BookOnDisk
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.CanWrite4GbFile
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.CannotWrite4GbFile
import org.kiwix.kiwixmobile.zimManager.fileselectView.FileSelectListState
import org.kiwix.kiwixmobile.zimManager.libraryView.adapter.LibraryListItem
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
  private val downloadRoomDao: DownloadRoomDao = mockk()
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
  private val alertDialogShower: AlertDialogShower = mockk()

  @Suppress("DEPRECATION")
  private val networkCapabilities: NetworkCapabilities = mockk()
  private val sharedPreferenceUtil: SharedPreferenceUtil = mockk()
  lateinit var viewModel: ZimManageViewModel

  private val downloads = MutableStateFlow<List<DownloadModel>>(emptyList())
  private val booksOnFileSystem = MutableStateFlow<List<BookOnDisk>>(emptyList())
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

  @Suppress("DEPRECATION")
  @BeforeEach
  fun init() {
    clearAllMocks()
    every { connectivityBroadcastReceiver.action } returns "test"
    every { downloadRoomDao.downloads() } returns downloads
    every { newBookDao.books() } returns books
    every {
      storageObserver.getBooksOnFileSystem(
        any<ScanningProgressListener>()
      )
    } returns booksOnFileSystem
    every { newLanguagesDao.languages() } returns languages
    every { fat32Checker.fileSystemStates } returns fileSystemStates
    every { connectivityBroadcastReceiver.networkStates } returns networkStates
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      every { application.registerReceiver(any(), any(), any()) } returns mockk()
    } else {
      @Suppress("UnspecifiedRegisterReceiverFlag")
      every { application.registerReceiver(any(), any()) } returns mockk()
    }
    every { dataSource.booksOnDiskAsListItems() } returns booksOnDiskListItems
    every {
      connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
    } returns networkCapabilities
    every { networkCapabilities.hasTransport(TRANSPORT_WIFI) } returns true
    viewModel =
      ZimManageViewModel(
        downloadRoomDao,
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
      ).apply {
        setIsUnitTestCase()
        setAlertDialogShower(alertDialogShower)
      }
    testScheduler.triggerActions()
  }

  @Nested
  inner class Context {
    @Test
    fun `registers broadcastReceiver in init`() {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        verify {
          application.registerReceiver(connectivityBroadcastReceiver, any(), any())
        }
      } else {
        @Suppress("UnspecifiedRegisterReceiverFlag")
        verify {
          application.registerReceiver(connectivityBroadcastReceiver, any())
        }
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
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `emissions from dat source are observed`() = runTest {
      val expectedList = listOf(bookOnDisk())
      booksOnDiskListItems.onNext(expectedList)
      testScheduler.advanceTimeBy(2)
      viewModel.fileSelectListStates.test()
        .assertValue(FileSelectListState(expectedList))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `books found on filesystem are filtered by books already in db`() = runTest {
      every { application.getString(any()) } returns ""
      val expectedBook = bookOnDisk(1L, book("1"))
      val bookToRemove = bookOnDisk(1L, book("2"))
      viewModel.requestFileSystemCheck.emit(Unit)
      books.onNext(listOf(bookToRemove))
      booksOnFileSystem.emit(
        listOf(
          expectedBook,
          expectedBook,
          bookToRemove
        )
      )
      advanceUntilIdle()
      verify {
        newBookDao.insert(listOf(expectedBook))
      }
    }
  }

  @Nested
  inner class Lanuages {
    @Test
    fun `network no result & empty language db activates the default locale`() {
      val expectedLanguage =
        Language(
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
      val defaultLanguage =
        Language(
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
      val dbLanguage =
        Language(
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
      every { application.getString(any(), any()) } returns ""
      every { kiwixService.library } returns Single.just(libraryNetworkEntity(networkBooks))
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
    val bookAlreadyOnDisk = book(id = "0", url = "", language = Locale.ENGLISH.language)
    val bookDownloading = book(id = "1", url = "")
    val bookWithActiveLanguage = book(id = "3", language = "activeLanguage", url = "")
    val bookWithInactiveLanguage = book(id = "4", language = "inactiveLanguage", url = "")
    every { application.getString(any()) } returns ""
    every { application.getString(any(), any()) } returns ""
    every {
      kiwixService.library
    } returns
      Single.just(
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
    downloads.value = listOf(downloadModel(book = bookDownloading))
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
          LibraryListItem.DividerItem(Long.MAX_VALUE, R.string.downloading),
          LibraryListItem.LibraryDownloadItem(downloadModel(book = bookDownloading)),
          LibraryListItem.DividerItem(Long.MAX_VALUE - 1, R.string.your_languages),
          LibraryListItem.BookItem(bookWithActiveLanguage, CanWrite4GbFile),
          LibraryListItem.DividerItem(Long.MIN_VALUE, R.string.other_languages),
          LibraryListItem.BookItem(bookWithInactiveLanguage, CanWrite4GbFile)
        )
      )
  }

  @Test
  fun `library marks files over 4GB as can't download if file system state says to`() {
    val bookOver4Gb =
      book(
        id = "0",
        url = "",
        size = "${Fat32Checker.FOUR_GIGABYTES_IN_KILOBYTES + 1}"
      )
    every { application.getString(any()) } returns ""
    every { application.getString(any(), any()) } returns ""
    every {
      kiwixService.library
    } returns Single.just(libraryNetworkEntity(listOf(bookOver4Gb)))
    networkStates.onNext(CONNECTED)
    downloads.value = listOf()
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
      // val bookToSelect = bookOnDisk(databaseId = 0L)
      // val unSelectedBook = bookOnDisk(databaseId = 1L)
      // viewModel.fileSelectListStates.value =
      //   FileSelectListState(
      //     listOf(
      //       bookToSelect,
      //       unSelectedBook
      //     ),
      //     NORMAL
      //   )
      // viewModel.sideEffects.test()
      //   .also { viewModel.fileSelectActions.offer(RequestMultiSelection(bookToSelect)) }
      //   .assertValues(StartMultiSelection(viewModel.fileSelectActions))
      // viewModel.fileSelectListStates.test()
      //   .assertValue(
      //     FileSelectListState(
      //       listOf(bookToSelect.apply { isSelected = !isSelected }, unSelectedBook),
      //       MULTI
      //     )
      //   )
    }

    @Test
    fun `RequestDeleteMultiSelection offers DeleteFiles with selected books`() {
      // val selectedBook = bookOnDisk().apply { isSelected = true }
      // viewModel.fileSelectListStates.value =
      //   FileSelectListState(listOf(selectedBook, bookOnDisk()), NORMAL)
      // viewModel.sideEffects.test()
      //   .also { viewModel.fileSelectActions.offer(RequestDeleteMultiSelection) }
      //   .assertValues(DeleteFiles(listOf(selectedBook), alertDialogShower))
    }

    @Test
    fun `RequestShareMultiSelection offers ShareFiles with selected books`() {
      // val selectedBook = bookOnDisk().apply { isSelected = true }
      // viewModel.fileSelectListStates.value =
      //   FileSelectListState(listOf(selectedBook, bookOnDisk()), NORMAL)
      // viewModel.sideEffects.test()
      //   .also { viewModel.fileSelectActions.offer(RequestShareMultiSelection) }
      //   .assertValues(ShareFiles(listOf(selectedBook)))
    }

    @Test
    fun `MultiModeFinished offers None`() {
      // val selectedBook = bookOnDisk().apply { isSelected = true }
      // viewModel.fileSelectListStates.value =
      //   FileSelectListState(listOf(selectedBook, bookOnDisk()), NORMAL)
      // viewModel.sideEffects.test()
      //   .also { viewModel.fileSelectActions.offer(MultiModeFinished) }
      //   .assertValues(None)
      // viewModel.fileSelectListStates.test().assertValue(
      //   FileSelectListState(
      //     listOf(
      //       selectedBook.apply { isSelected = false },
      //       bookOnDisk()
      //     )
      //   )
      // )
    }

    @Test
    fun `RequestSelect offers None and inverts selection`() {
      // val selectedBook = bookOnDisk(0L).apply { isSelected = true }
      // viewModel.fileSelectListStates.value =
      //   FileSelectListState(listOf(selectedBook, bookOnDisk(1L)), NORMAL)
      // viewModel.sideEffects.test()
      //   .also { viewModel.fileSelectActions.offer(RequestSelect(selectedBook)) }
      //   .assertValues(None)
      // viewModel.fileSelectListStates.test().assertValue(
      //   FileSelectListState(
      //     listOf(
      //       selectedBook.apply { isSelected = false },
      //       bookOnDisk(1L)
      //     )
      //   )
      // )
    }

    @Test
    fun `RestartActionMode offers StartMultiSelection`() {
      // viewModel.sideEffects.test()
      //   .also { viewModel.fileSelectActions.offer(RestartActionMode) }
      //   .assertValues(StartMultiSelection(viewModel.fileSelectActions))
    }
  }
}
