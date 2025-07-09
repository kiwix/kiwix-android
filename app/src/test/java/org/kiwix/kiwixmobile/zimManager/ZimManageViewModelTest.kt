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
import androidx.lifecycle.asFlow
import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import com.jraska.livedata.test
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.kiwix.kiwixmobile.core.StorageObserver
import org.kiwix.kiwixmobile.core.dao.DownloadRoomDao
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookOnDisk
import org.kiwix.kiwixmobile.core.data.DataSource
import org.kiwix.kiwixmobile.core.data.remote.KiwixService
import org.kiwix.kiwixmobile.core.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.core.entity.LibkiwixBook
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
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.SelectionMode.MULTI
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.SelectionMode.NORMAL
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.CanWrite4GbFile
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.CannotWrite4GbFile
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.MultiModeFinished
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.RequestDeleteMultiSelection
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.RequestMultiSelection
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.RequestSelect
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.RequestShareMultiSelection
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.RestartActionMode
import org.kiwix.kiwixmobile.zimManager.fileselectView.FileSelectListState
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.DeleteFiles
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.None
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.ShareFiles
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.StartMultiSelection
import org.kiwix.kiwixmobile.zimManager.libraryView.adapter.LibraryListItem
import org.kiwix.libkiwix.Book
import org.kiwix.sharedFunctions.InstantExecutorExtension
import org.kiwix.sharedFunctions.bookOnDisk
import org.kiwix.sharedFunctions.downloadModel
import org.kiwix.sharedFunctions.language
import org.kiwix.sharedFunctions.libkiwixBook
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(InstantExecutorExtension::class)
class ZimManageViewModelTest {
  private val downloadRoomDao: DownloadRoomDao = mockk()
  private val libkiwixBookOnDisk: LibkiwixBookOnDisk = mockk()
  private val storageObserver: StorageObserver = mockk()
  private val kiwixService: KiwixService = mockk()
  private val application: Application = mockk()
  private val connectivityBroadcastReceiver: ConnectivityBroadcastReceiver = mockk()
  private val fat32Checker: Fat32Checker = mockk()
  private val dataSource: DataSource = mockk()
  private val connectivityManager: ConnectivityManager = mockk()
  private val alertDialogShower: AlertDialogShower = mockk()

  @Suppress("DEPRECATION")
  private val networkCapabilities: NetworkCapabilities = mockk()
  private val sharedPreferenceUtil: SharedPreferenceUtil = mockk()
  lateinit var viewModel: ZimManageViewModel

  private val downloads = MutableStateFlow<List<DownloadModel>>(emptyList())
  private val booksOnFileSystem = MutableStateFlow<List<Book>>(emptyList())
  private val books = MutableStateFlow<List<BookOnDisk>>(emptyList())
  private val languages = MutableStateFlow<List<Language>>(emptyList())
  private val fileSystemStates =
    MutableStateFlow<FileSystemState>(FileSystemState.DetectingFileSystem)
  private val networkStates = MutableStateFlow(NetworkState.NOT_CONNECTED)
  private val booksOnDiskListItems = MutableStateFlow<List<BooksOnDiskListItem>>(emptyList())
  private val testDispatcher = StandardTestDispatcher()
  private val onlineLibraryManager = mockk<OnlineLibraryManager>()

  @AfterAll
  fun teardown() {
    viewModel.onClearedExposed()
    Dispatchers.resetMain()
  }

  @BeforeEach
  fun init() {
    Dispatchers.setMain(testDispatcher)
    clearAllMocks()
    every { connectivityBroadcastReceiver.action } returns "test"
    every { downloadRoomDao.downloads() } returns downloads
    every { libkiwixBookOnDisk.books() } returns books
    every {
      storageObserver.getBooksOnFileSystem(
        any<ScanningProgressListener>()
      )
    } returns booksOnFileSystem
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
    every { sharedPreferenceUtil.prefWifiOnly } returns true
    coEvery { onlineLibraryManager.getOnlineBooksLanguage() } returns listOf("eng")
    downloads.value = emptyList()
    booksOnFileSystem.value = emptyList()
    books.value = emptyList()
    languages.value = emptyList()
    fileSystemStates.value = FileSystemState.DetectingFileSystem
    booksOnDiskListItems.value = emptyList()
    networkStates.value = NOT_CONNECTED
    viewModel =
      ZimManageViewModel(
        downloadRoomDao,
        libkiwixBookOnDisk,
        storageObserver,
        kiwixService,
        application,
        connectivityBroadcastReceiver,
        fat32Checker,
        dataSource,
        connectivityManager,
        sharedPreferenceUtil,
        onlineLibraryManager
      ).apply {
        setIsUnitTestCase()
        setAlertDialogShower(alertDialogShower)
      }
    viewModel.fileSelectListStates.value = FileSelectListState(emptyList())
    runBlocking { viewModel.networkLibrary.emit(emptyList()) }
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
    @Test
    fun `emissions from data source are observed`() = runTest {
      val expectedList = listOf(bookOnDisk())
      testFlow(
        viewModel.fileSelectListStates.asFlow(),
        triggerAction = { booksOnDiskListItems.emit(expectedList) },
        assert = {
          skipItems(1)
          assertThat(awaitItem()).isEqualTo(FileSelectListState(expectedList))
        }
      )
    }

    @Test
    fun `books found on filesystem are filtered by books already in db`() = runTest {
      every { application.getString(any()) } returns ""
      val expectedBook = bookOnDisk(1L, libkiwixBook("1", nativeBook = BookTestWrapper("1")))
      val bookToRemove = bookOnDisk(1L, libkiwixBook("2", nativeBook = BookTestWrapper("2")))
      advanceUntilIdle()
      viewModel.requestFileSystemCheck.emit(Unit)
      advanceUntilIdle()
      books.emit(listOf(bookToRemove))
      advanceUntilIdle()
      booksOnFileSystem.emit(
        listOfNotNull(
          expectedBook.book.nativeBook,
          expectedBook.book.nativeBook,
          bookToRemove.book.nativeBook
        )
      )
      advanceUntilIdle()
      coVerify(timeout = MOCKK_TIMEOUT_FOR_VERIFICATION) {
        libkiwixBookOnDisk.insert(listOfNotNull(expectedBook.book.nativeBook))
      }
    }
  }

  @Nested
  inner class Languages {
    @Test
    fun `network no result & empty language db activates the default locale`() = runTest {
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
      advanceUntilIdle()
      // verify { languageRoomDao.insert(listOf(expectedLanguage)) }
    }

    @Test
    fun `network no result & a language db result triggers nothing`() = runTest {
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
      // verify { languageRoomDao.insert(any()) }
    }

    @Test
    fun `network result & empty language db triggers combined result of default + network`() =
      runTest {
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
            libkiwixBook(language = "eng"),
            libkiwixBook(language = "eng"),
            libkiwixBook(language = "fra")
          ),
          listOf(),
          defaultLanguage
        )
        verify {
          // languageRoomDao.insert(
          //   listOf(
          //     defaultLanguage.copy(occurencesOfLanguage = 2),
          //     Language(
          //       active = false,
          //       occurencesOfLanguage = 1,
          //       language = "fra",
          //       languageLocalized = "",
          //       languageCode = "",
          //       languageCodeISO2 = ""
          //     )
          //   )
          // )
        }
      }

    @Test
    fun `network result & language db results activates a combined network + db result`() =
      runTest {
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
            libkiwixBook(language = "eng"),
            libkiwixBook(language = "eng"),
            libkiwixBook(language = "fra")
          ),
          listOf(dbLanguage),
          language(isActive = true, occurencesOfLanguage = 1)
        )
        advanceUntilIdle()
        verify(timeout = MOCKK_TIMEOUT_FOR_VERIFICATION) {
          // languageRoomDao.insert(
          //   listOf(
          //     dbLanguage.copy(occurencesOfLanguage = 2),
          //     Language(
          //       active = false,
          //       occurencesOfLanguage = 1,
          //       language = "fra",
          //       languageLocalized = "fra",
          //       languageCode = "fra",
          //       languageCodeISO2 = "fra"
          //     )
          //   )
          // )
        }
      }

    private suspend fun TestScope.expectNetworkDbAndDefault(
      networkBooks: List<LibkiwixBook>,
      dbBooks: List<Language>,
      defaultLanguage: Language
    ) {
      every { application.getString(any()) } returns ""
      every { application.getString(any(), any()) } returns ""
      // every { defaultLanguageProvider.provide() } returns defaultLanguage
      coEvery { onlineLibraryManager.getOnlineBooksLanguage() } returns networkBooks.map { it.language }
      viewModel.networkLibrary.emit(networkBooks)
      advanceUntilIdle()
      languages.value = dbBooks
      advanceUntilIdle()
      networkStates.value = CONNECTED
      advanceUntilIdle()
    }
  }

  @Test
  fun `network states observed`() = runTest {
    networkStates.tryEmit(NOT_CONNECTED)
    advanceUntilIdle()
    viewModel.networkStates.test()
      .assertValue(NOT_CONNECTED)
  }

  @Test
  fun `library update removes from sources and maps to list items`() = runTest {
    val book = BookTestWrapper("0")
    val bookAlreadyOnDisk =
      libkiwixBook(id = "0", url = "", language = Locale.ENGLISH.language, nativeBook = book)
    val bookDownloading = libkiwixBook(id = "1", url = "")
    val bookWithActiveLanguage = libkiwixBook(id = "3", language = "activeLanguage", url = "")
    val bookWithInactiveLanguage = libkiwixBook(id = "4", language = "inactiveLanguage", url = "")
    testFlow(
      flow = viewModel.libraryItems,
      triggerAction = {
        every { application.getString(any()) } returns ""
        every { application.getString(any(), any()) } returns ""
        networkStates.value = CONNECTED
        downloads.value = listOf(downloadModel(book = bookDownloading))
        books.value = listOf(bookOnDisk(book = bookAlreadyOnDisk))
        languages.value =
          listOf(
            language(isActive = true, occurencesOfLanguage = 1, languageCode = "activeLanguage"),
            language(isActive = false, occurencesOfLanguage = 1, languageCode = "inactiveLanguage")
          )
        fileSystemStates.value = CanWrite4GbFile
        runBlocking {
          viewModel.networkLibrary.emit(
            listOf(
              bookAlreadyOnDisk,
              bookDownloading,
              bookWithActiveLanguage,
              bookWithInactiveLanguage
            )
          )
        }
        advanceUntilIdle()
      },
      assert = {
        while (true) {
          val items = awaitItem()
          val bookItems = items.filterIsInstance<LibraryListItem.BookItem>()
          if (bookItems.size >= 2 && bookItems[0].fileSystemState == CanWrite4GbFile) {
            // assertThat(items).isEqualTo(
            //   listOf(
            //     LibraryListItem.DividerItem(Long.MAX_VALUE, R.string.downloading),
            //     LibraryListItem.LibraryDownloadItem(downloadModel(book = bookDownloading)),
            //     LibraryListItem.DividerItem(Long.MAX_VALUE - 1, R.string.your_languages),
            //     LibraryListItem.BookItem(bookWithActiveLanguage, CanWrite4GbFile),
            //     LibraryListItem.DividerItem(Long.MIN_VALUE, R.string.other_languages),
            //     LibraryListItem.BookItem(bookWithInactiveLanguage, CanWrite4GbFile)
            //   )
            // )
            break
          }
        }
      },
      timeout = TURBINE_TIMEOUT
    )
  }

  @Test
  fun `library marks files over 4GB as can't download if file system state says to`() = runTest {
    val bookOver4Gb =
      libkiwixBook(
        id = "0",
        url = "",
        size = "${Fat32Checker.FOUR_GIGABYTES_IN_KILOBYTES + 1}"
      )
    every { application.getString(any()) } returns ""
    every { application.getString(any(), any()) } returns ""
    testFlow(
      viewModel.libraryItems,
      triggerAction = {
        networkStates.tryEmit(CONNECTED)
        downloads.tryEmit(listOf())
        books.tryEmit(listOf())
        languages.tryEmit(
          listOf(
            language(isActive = true, occurencesOfLanguage = 1, languageCode = "activeLanguage")
          )
        )
        fileSystemStates.tryEmit(CannotWrite4GbFile)
        viewModel.networkLibrary.emit(listOf(bookOver4Gb))
      },
      assert = {
        while (true) {
          val item = awaitItem()
          val bookItem = item.filterIsInstance<LibraryListItem.BookItem>().firstOrNull()
          if (bookItem?.fileSystemState == CannotWrite4GbFile) {
            assertThat(item).isEqualTo(
              listOf(
                // LibraryListItem.DividerItem(Long.MIN_VALUE, R.string.other_languages),
                LibraryListItem.BookItem(bookOver4Gb, CannotWrite4GbFile)
              )
            )
            break
          }
        }
      },
      timeout = TURBINE_TIMEOUT
    )
  }

  @Nested
  inner class SideEffects {
    @Test
    fun `RequestMultiSelection offers StartMultiSelection and selects a book`() = runTest {
      val bookToSelect = bookOnDisk(databaseId = 0L)
      val unSelectedBook = bookOnDisk(databaseId = 1L)
      viewModel.fileSelectListStates.value =
        FileSelectListState(
          listOf(
            bookToSelect,
            unSelectedBook
          ),
          NORMAL
        )
      testFlow(
        flow = viewModel.sideEffects,
        triggerAction = { viewModel.fileSelectActions.emit(RequestMultiSelection(bookToSelect)) },
        assert = { assertThat(awaitItem()).isEqualTo(StartMultiSelection(viewModel.fileSelectActions)) }
      )
      viewModel.fileSelectListStates.test()
        .assertValue(
          FileSelectListState(
            listOf(bookToSelect.apply { isSelected = !isSelected }, unSelectedBook),
            MULTI
          )
        )
    }

    @Test
    fun `RequestDeleteMultiSelection offers DeleteFiles with selected books`() = runTest {
      val selectedBook = bookOnDisk().apply { isSelected = true }
      viewModel.fileSelectListStates.value =
        FileSelectListState(listOf(selectedBook, bookOnDisk()), NORMAL)
      testFlow(
        flow = viewModel.sideEffects,
        triggerAction = { viewModel.fileSelectActions.emit(RequestDeleteMultiSelection) },
        assert = {
          assertThat(awaitItem()).isEqualTo(
            DeleteFiles(
              listOf(selectedBook),
              alertDialogShower
            )
          )
        }
      )
    }

    @Test
    fun `RequestShareMultiSelection offers ShareFiles with selected books`() = runTest {
      val selectedBook = bookOnDisk().apply { isSelected = true }
      viewModel.fileSelectListStates.value =
        FileSelectListState(listOf(selectedBook, bookOnDisk()), NORMAL)
      testFlow(
        flow = viewModel.sideEffects,
        triggerAction = { viewModel.fileSelectActions.emit(RequestShareMultiSelection) },
        assert = { assertThat(awaitItem()).isEqualTo(ShareFiles(listOf(selectedBook))) }
      )
    }

    @Test
    fun `MultiModeFinished offers None`() = runTest {
      val selectedBook = bookOnDisk().apply { isSelected = true }
      viewModel.fileSelectListStates.value =
        FileSelectListState(listOf(selectedBook, bookOnDisk()), NORMAL)
      testFlow(
        flow = viewModel.sideEffects,
        triggerAction = { viewModel.fileSelectActions.emit(MultiModeFinished) },
        assert = { assertThat(awaitItem()).isEqualTo(None) }
      )
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
    fun `RequestSelect offers None and inverts selection`() = runTest {
      val selectedBook = bookOnDisk(0L).apply { isSelected = true }
      viewModel.fileSelectListStates.value =
        FileSelectListState(listOf(selectedBook, bookOnDisk(1L)), NORMAL)
      testFlow(
        flow = viewModel.sideEffects,
        triggerAction = { viewModel.fileSelectActions.emit(RequestSelect(selectedBook)) },
        assert = { assertThat(awaitItem()).isEqualTo(None) }
      )
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
    fun `RestartActionMode offers StartMultiSelection`() = runTest {
      testFlow(
        flow = viewModel.sideEffects,
        triggerAction = { viewModel.fileSelectActions.emit(RestartActionMode) },
        assert = { assertThat(awaitItem()).isEqualTo(StartMultiSelection(viewModel.fileSelectActions)) }
      )
    }
  }
}

suspend fun <T> TestScope.testFlow(
  flow: Flow<T>,
  triggerAction: suspend () -> Unit,
  assert: suspend TurbineTestContext<T>.() -> Unit,
  timeout: Duration? = null
) {
  val job = launch {
    flow.test(timeout = timeout) {
      triggerAction()
      assert()
      cancelAndIgnoreRemainingEvents()
    }
  }
  job.join()
}

class BookTestWrapper(private val id: String) : Book(0L) {
  override fun getId(): String = id
  override fun equals(other: Any?): Boolean = other is BookTestWrapper && getId() == other.getId()
  override fun hashCode(): Int = getId().hashCode()
}

val TURBINE_TIMEOUT = 5000.toDuration(DurationUnit.MILLISECONDS)
const val MOCKK_TIMEOUT_FOR_VERIFICATION = 1000L
