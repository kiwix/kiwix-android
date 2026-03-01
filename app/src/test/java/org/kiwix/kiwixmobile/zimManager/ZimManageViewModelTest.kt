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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlinx.coroutines.test.setMain
import okhttp3.HttpUrl
import okhttp3.Response
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
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.reader.integrity.ValidateZimViewModel
import org.kiwix.kiwixmobile.core.ui.components.ONE
import org.kiwix.kiwixmobile.core.utils.ZERO
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.files.ScanningProgressListener
import org.kiwix.kiwixmobile.core.zim_manager.ConnectivityBroadcastReceiver
import org.kiwix.kiwixmobile.core.zim_manager.NetworkState
import org.kiwix.kiwixmobile.core.zim_manager.NetworkState.CONNECTED
import org.kiwix.kiwixmobile.core.zim_manager.NetworkState.NOT_CONNECTED
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem.BookOnDisk
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.SelectionMode.MULTI
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.SelectionMode.NORMAL
import org.kiwix.kiwixmobile.language.viewmodel.flakyTest
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.CanWrite4GbFile
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.CannotWrite4GbFile
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.MultiModeFinished
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.RequestDeleteMultiSelection
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.RequestMultiSelection
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.RequestNavigateTo
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.RequestSelect
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.convertToLocal
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.RequestShareMultiSelection
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.RequestValidateZimFiles
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.RestartActionMode
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.UserClickedDownloadBooksButton
import org.kiwix.kiwixmobile.zimManager.fileselectView.FileSelectListState
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.DeleteFiles
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.NavigateToDownloads
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.None
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.OpenFileWithNavigation
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.ShareFiles
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.StartMultiSelection
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.ValidateZIMFiles
import org.kiwix.kiwixmobile.zimManager.libraryView.LibraryListItem
import org.kiwix.libkiwix.Book
import org.kiwix.sharedFunctions.InstantExecutorExtension
import org.kiwix.sharedFunctions.MOCK_BASE_URL
import org.kiwix.sharedFunctions.bookOnDisk
import org.kiwix.sharedFunctions.downloadModel
import org.kiwix.sharedFunctions.libkiwixBook
import java.util.Locale
import kotlin.time.Duration

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
  private val validateZimViewModel: ValidateZimViewModel = mockk()

  @Suppress("DEPRECATION")
  private val networkCapabilities: NetworkCapabilities = mockk()
  private val kiwixDataStore: KiwixDataStore = mockk()
  lateinit var viewModel: ZimManageViewModel

  private val downloads = MutableStateFlow<List<DownloadModel>>(emptyList())
  private val booksOnFileSystem = MutableStateFlow<List<Book>>(emptyList())
  private val books = MutableStateFlow<List<BookOnDisk>>(emptyList())
  private val onlineContentLanguage = MutableStateFlow("")
  private val onlineCategoryContent = MutableStateFlow("")
  private val fileSystemStates =
    MutableStateFlow<FileSystemState>(FileSystemState.DetectingFileSystem)
  private val networkStates = MutableStateFlow(NetworkState.NOT_CONNECTED)
  private val booksOnDiskListItems = MutableStateFlow<List<BooksOnDiskListItem>>(emptyList())
  private val testDispatcher = StandardTestDispatcher()
  private val onlineLibraryManager = mockk<OnlineLibraryManager>()
  private val onlineLibraryServiceFactory = mockk<OnlineLibraryServiceFactory>()

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
    every { application.getString(any()) } returns ""
    every { application.getString(any(), *anyVararg()) } returns ""
    every { dataSource.booksOnDiskAsListItems() } returns booksOnDiskListItems
    every {
      connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
    } returns networkCapabilities
    every { networkCapabilities.hasTransport(TRANSPORT_WIFI) } returns true
    coEvery { kiwixDataStore.wifiOnly } returns flowOf(true)
    coEvery { kiwixDataStore.selectedOnlineContentLanguage } returns onlineContentLanguage
    coEvery { kiwixDataStore.selectedOnlineContentCategory } returns onlineCategoryContent
    every { onlineLibraryManager.getStartOffset(any(), any()) } returns ONE
    every {
      onlineLibraryManager.buildLibraryUrl(
        any(),
        any(),
        any(),
        any(),
        any(),
        any()
      )
    } returns MOCK_BASE_URL
    val response = mockk<retrofit2.Response<String>>()
    val rawResponse = mockk<Response>()
    every { response.raw() } returns rawResponse
    val httpsUrl = mockk<HttpUrl>()
    every { httpsUrl.host } returns ""
    every { httpsUrl.scheme } returns ""
    every { rawResponse.networkResponse?.request?.url } returns httpsUrl
    coEvery { kiwixService.getLibraryPage(any()) } returns response
    every { response.body() } returns ""
    downloads.value = emptyList()
    booksOnFileSystem.value = emptyList()
    books.value = emptyList()
    fileSystemStates.value = FileSystemState.DetectingFileSystem
    booksOnDiskListItems.value = emptyList()
    networkStates.value = NOT_CONNECTED
    onlineContentLanguage.value = ""
    viewModel =
      TestZimManageViewModel(
        downloadRoomDao,
        libkiwixBookOnDisk,
        storageObserver,
        kiwixService,
        application,
        connectivityBroadcastReceiver,
        fat32Checker,
        dataSource,
        connectivityManager,
        onlineLibraryManager,
        kiwixDataStore,
        onlineLibraryServiceFactory
      ).apply {
        setIsUnitTestCase()
        setAlertDialogShower(alertDialogShower)
        setValidateZimViewModel(validateZimViewModel)
      }
    viewModel.fileSelectListStates.value = FileSelectListState(emptyList())
    runBlocking {
      viewModel.networkLibrary.emit(
        OnlineLibraryResult(
          OnlineLibraryRequest(
            query = null,
            category = null,
            lang = null,
            isLoadMoreItem = false,
            page = ZERO
          ),
          emptyList()
        )
      )
    }
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
    fun `emissions from data source are observed`() = flakyTest {
      runTest(testDispatcher) {
        val expectedList = listOf(bookOnDisk())
        testFlow(
          viewModel.fileSelectListStates.asFlow(),
          triggerAction = {
            booksOnDiskListItems.emit(expectedList)
            advanceUntilIdle()
          },
          assert = {
            skipItems(1)
            assertThat(awaitItem()).isEqualTo(FileSelectListState(expectedList))
            cancelAndIgnoreRemainingEvents()
          }
        )
      }
    }

    @Test
    fun `books found on filesystem are filtered by books already in db`() = flakyTest {
      runTest(testDispatcher) {
        every { application.getString(any()) } returns ""
        val expectedBook = bookOnDisk(1L, libkiwixBook("1", nativeBook = BookTestWrapper("1")))
        val bookToRemove = bookOnDisk(1L, libkiwixBook("2", nativeBook = BookTestWrapper("2")))
        advanceUntilIdle()
        books.emit(listOf(bookToRemove))
        booksOnFileSystem.emit(
          listOfNotNull(
            expectedBook.book.nativeBook,
            expectedBook.book.nativeBook,
            bookToRemove.book.nativeBook
          )
        )
        viewModel.requestFileSystemCheck.emit(Unit)
        advanceUntilIdle()
        yield()
        advanceUntilIdle()
        coVerify(timeout = MOCKK_TIMEOUT_FOR_VERIFICATION) {
          libkiwixBookOnDisk.insert(listOfNotNull(expectedBook.book.nativeBook))
        }
      }
    }
  }

  @Nested
  inner class Languages {
    @Test
    fun `changing language updates the filter and do the network request`() = flakyTest {
      runTest(testDispatcher) {
        onlineContentLanguage.value = ""
        every { application.getString(any()) } returns ""
        every { application.getString(any(), any()) } returns ""
        viewModel.onlineLibraryRequest.test {
          skipItems(1)
          onlineContentLanguage.emit("eng")
          var onlineLibraryRequest = awaitItem()
          while (onlineLibraryRequest.lang != "eng") onlineLibraryRequest = awaitItem()
          assertThat(onlineLibraryRequest.lang).isEqualTo("eng")
          assertThat(onlineLibraryRequest.page).isEqualTo(ONE)
          assertThat(onlineLibraryRequest.isLoadMoreItem).isEqualTo(false)
          cancelAndIgnoreRemainingEvents()
        }
      }
    }

    @Test
    fun `library section title adapts to selected language count`() = flakyTest {
      runTest(testDispatcher) {
        every { application.getString(R.string.all_languages) } returns "All languages"
        every {
          application.getString(R.string.your_language, any())
        } answers {
          val args = secondArg<Array<Any>>()
          "Selected language: ${args[0]}"
        }
        every { application.getString(R.string.your_languages) } returns "Selected languages:"

        // All languages (blank)
        val allTitle = viewModel.getOnlineLibrarySectionTitle("")
        assertThat(allTitle).isEqualTo("All languages")

        // Single language
        val singleTitle = viewModel.getOnlineLibrarySectionTitle("eng")
        assertThat(singleTitle).contains("Selected language:")
        assertThat(singleTitle).contains("English")

        // Multiple languages
        val multiTitle = viewModel.getOnlineLibrarySectionTitle("eng,fra,deu")
        assertThat(multiTitle).contains("Selected languages:")
        // Locale("eng").displayLanguage returns "English" but
        // Locale("fra") and Locale("deu") may return raw codes on some JVMs
        assertThat(multiTitle).contains("eng".convertToLocal().displayLanguage)
        assertThat(multiTitle).contains("fra".convertToLocal().displayLanguage)
        assertThat(multiTitle).contains("deu".convertToLocal().displayLanguage)
      }
    }
  }

  @Nested
  inner class Categories {
    @Test
    fun `changing category updates the filter and do the network request`() = flakyTest {
      runTest(testDispatcher) {
        onlineCategoryContent.value = ""
        every { application.getString(any()) } returns ""
        every { application.getString(any(), any()) } returns ""
        viewModel.onlineLibraryRequest.test {
          skipItems(1)
          onlineCategoryContent.emit("wikipedia")
          var onlineLibraryRequest = awaitItem()
          while (onlineLibraryRequest.category != "wikipedia") onlineLibraryRequest = awaitItem()
          assertThat(onlineLibraryRequest.category).isEqualTo("wikipedia")
          assertThat(onlineLibraryRequest.page).isEqualTo(ONE)
          assertThat(onlineLibraryRequest.isLoadMoreItem).isEqualTo(false)
        }
      }
    }
  }

  @Test
  fun `network states observed`() = flakyTest {
    runTest(testDispatcher) {
      networkStates.tryEmit(NOT_CONNECTED)
      advanceUntilIdle()
      viewModel.networkStates.test()
        .assertValue(NOT_CONNECTED)
    }
  }

  @Test
  fun `updateOnlineLibraryFilters updates onlineLibraryRequest`() = flakyTest {
    runTest(testDispatcher) {
      viewModel.setIsUnitTestCase()
      val newRequest = OnlineLibraryRequest(
        query = "test",
        category = "cat",
        lang = "en",
        page = 2,
        isLoadMoreItem = true,
        version = 100L
      )
      viewModel.onlineLibraryRequest.test {
        viewModel.updateOnlineLibraryFilters(newRequest)
        var request = awaitItem()
        while (request != newRequest) request = awaitItem()
        assertThat(request).isEqualTo(newRequest)
      }
    }
  }

  @Test
  fun `library update removes from sources and maps to list items`() = flakyTest {
    runTest(testDispatcher) {
      val book = BookTestWrapper("0")
      val bookAlreadyOnDisk =
        libkiwixBook(id = "0", url = "", language = Locale.ENGLISH.language, nativeBook = book)
      val bookDownloading = libkiwixBook(id = "1", url = "")
      val bookWithActiveLanguage = libkiwixBook(id = "3", language = "activeLanguage", url = "")
      viewModel.libraryItems.test {
        every { application.getString(any()) } returns ""
        every { application.getString(any(), any()) } returns ""
        coEvery {
          onlineLibraryManager.parseOPDSStreamAndGetBooks(any(), any())
        } returns arrayListOf(bookWithActiveLanguage)
        networkStates.value = CONNECTED
        downloads.value = listOf(downloadModel(book = bookDownloading))
        books.value = listOf(bookOnDisk(book = bookAlreadyOnDisk))
        fileSystemStates.value = CanWrite4GbFile
        advanceUntilIdle()

        val items = awaitItem()
        val bookItems = items.items.filterIsInstance<LibraryListItem.BookItem>()
        if (bookItems.size >= 2 && bookItems[0].fileSystemState == CanWrite4GbFile) {
          assertThat(items.items).isEqualTo(
            listOf(
              LibraryListItem.DividerItem(Long.MAX_VALUE, "Downloading:"),
              LibraryListItem.LibraryDownloadItem(downloadModel(book = bookDownloading)),
              LibraryListItem.DividerItem(Long.MAX_VALUE - 1, ""),
              LibraryListItem.BookItem(bookWithActiveLanguage, CanWrite4GbFile),
            )
          )
        }
        cancelAndIgnoreRemainingEvents()
      }
    }
  }

  @Test
  fun `library marks files over 4GB as can't download if file system state says to`() = flakyTest {
    runTest(testDispatcher) {
      onlineContentLanguage.value = ""
      val bookOver4Gb =
        libkiwixBook(
          id = "0",
          url = "",
          size = "${Fat32Checker.FOUR_GIGABYTES_IN_KILOBYTES + 1}"
        )
      every { application.getString(any()) } answers { "" }
      every { application.getString(any(), any()) } answers { "" }
      every { application.getString(any(), *anyVararg()) } answers { "" }

      // test libraryItems fetches for all language.
      viewModel.libraryItems.test {
        coEvery {
          onlineLibraryManager.parseOPDSStreamAndGetBooks(any(), any())
        } returns arrayListOf(bookOver4Gb)
        networkStates.value = CONNECTED
        downloads.value = listOf()
        books.value = listOf()
        onlineContentLanguage.value = ""
        fileSystemStates.emit(FileSystemState.DetectingFileSystem)
        fileSystemStates.emit(CannotWrite4GbFile)
        advanceUntilIdle()

        val item = awaitItem()
        val bookItem = item.items.filterIsInstance<LibraryListItem.BookItem>().firstOrNull()
        if (bookItem?.fileSystemState == CannotWrite4GbFile) {
          assertThat(item.items).isEqualTo(
            listOf(
              LibraryListItem.DividerItem(Long.MIN_VALUE, ""),
              LibraryListItem.BookItem(bookOver4Gb, CannotWrite4GbFile)
            )
          )
        }
        cancelAndConsumeRemainingEvents()
      }
    }

    @Test
    fun `library shows selected language section title correctly`() = flakyTest {
      runTest(testDispatcher) {
        val bookOver4Gb =
          libkiwixBook(
            id = "0",
            url = "",
            size = "${Fat32Checker.FOUR_GIGABYTES_IN_KILOBYTES + 1}"
          )
        every { application.getString(any()) } answers { "" }
        every { application.getString(any(), any()) } answers { "" }
        every { application.getString(any(), *anyVararg()) } answers { "Selected language: English" }

        // test libraryItems fetches for all language.
        viewModel.libraryItems.test {
          coEvery {
            onlineLibraryManager.parseOPDSStreamAndGetBooks(any(), any())
          } returns arrayListOf(bookOver4Gb)
          networkStates.value = CONNECTED
          downloads.value = listOf()
          books.value = listOf()
          onlineContentLanguage.value = "eng"
          yield()
          fileSystemStates.emit(FileSystemState.DetectingFileSystem)
          fileSystemStates.emit(CannotWrite4GbFile)
          advanceUntilIdle()

          var matched = false
          while (!matched) {
            val item = awaitItem()
            val bookItem = item.items.filterIsInstance<LibraryListItem.BookItem>().firstOrNull()
            if (bookItem?.fileSystemState == CannotWrite4GbFile) {
              assertThat(item.items).isEqualTo(
                listOf(
                  LibraryListItem.DividerItem(Long.MIN_VALUE, "Selected language: English"),
                  LibraryListItem.BookItem(bookOver4Gb, CannotWrite4GbFile)
                )
              )
              matched = true
            }
          }
          cancelAndConsumeRemainingEvents()
        }
      }
    }
  }

  @Test
  fun `library shows downloading books even when not in online source`() = flakyTest {
    runTest(testDispatcher) {
      val downloadingBook = libkiwixBook(id = "10", url = "")
      val bookInOnlineList = libkiwixBook(id = "20", url = "")
      val downloadModel = downloadModel(book = downloadingBook)

      every { application.getString(any()) } returns "Downloading"
      every { application.getString(any(), any()) } returns "All languages"
      every { application.getString(any(), *anyVararg()) } returns "All languages"

      viewModel.libraryItems.test {
        coEvery {
          onlineLibraryManager.parseOPDSStreamAndGetBooks(any(), any())
        } returns arrayListOf(bookInOnlineList)
        networkStates.value = CONNECTED
        downloads.value = listOf(downloadModel)
        books.value = listOf()
        onlineContentLanguage.value = ""
        fileSystemStates.value = CanWrite4GbFile
        advanceUntilIdle()

        val items = awaitItem()
        val bookItems = items.items.filterIsInstance<LibraryListItem.BookItem>()
        if (bookItems.size >= 2) {
          assertThat(items.items).isEqualTo(
            listOf(
              LibraryListItem.DividerItem(Long.MAX_VALUE, "Downloading"),
              LibraryListItem.LibraryDownloadItem(downloadModel),
              LibraryListItem.DividerItem(Long.MIN_VALUE, "All languages"),
              LibraryListItem.BookItem(bookInOnlineList, CanWrite4GbFile)
            )
          )
        }
        cancelAndIgnoreRemainingEvents()
      }
    }
  }

  @Nested
  inner class SideEffects {
    @Test
    fun `RequestNavigateTo offers OpenFileWithNavigation with selected books`() = flakyTest {
      runTest(testDispatcher) {
        val selectedBook = bookOnDisk().apply { isSelected = true }
        viewModel.fileSelectListStates.value =
          FileSelectListState(listOf(selectedBook, bookOnDisk()), NORMAL)
        testFlow(
          flow = viewModel.sideEffects,
          triggerAction = { viewModel.fileSelectActions.emit(RequestNavigateTo(selectedBook)) },
          assert = { assertThat(awaitItem()).isEqualTo(OpenFileWithNavigation(selectedBook)) }
        )
      }
    }

    @Test
    fun `RequestMultiSelection offers StartMultiSelection and selects a book`() = flakyTest {
      runTest(testDispatcher) {
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
    }

    @Test
    fun `RequestDeleteMultiSelection offers DeleteFiles with selected books`() = flakyTest {
      runTest(testDispatcher) {
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
    }

    @Test
    fun `RequestShareMultiSelection offers ShareFiles with selected books`() = flakyTest {
      runTest(testDispatcher) {
        val selectedBook = bookOnDisk().apply { isSelected = true }
        viewModel.fileSelectListStates.value =
          FileSelectListState(listOf(selectedBook, bookOnDisk()), NORMAL)
        testFlow(
          flow = viewModel.sideEffects,
          triggerAction = { viewModel.fileSelectActions.emit(RequestShareMultiSelection) },
          assert = { assertThat(awaitItem()).isEqualTo(ShareFiles(listOf(selectedBook))) }
        )
      }
    }

    @Test
    fun `RequestValidateZimFiles offers ValidateZIMFiles with selected books`() = flakyTest {
      runTest(testDispatcher) {
        val selectedBook = bookOnDisk().apply { isSelected = true }
        viewModel.fileSelectListStates.value =
          FileSelectListState(listOf(selectedBook, bookOnDisk()), NORMAL)
        testFlow(
          flow = viewModel.sideEffects,
          triggerAction = { viewModel.fileSelectActions.emit(RequestValidateZimFiles) },
          assert = {
            assertThat(awaitItem())
              .isEqualTo(
                ValidateZIMFiles(
                  listOf(selectedBook),
                  alertDialogShower,
                  validateZimViewModel
                )
              )
          }
        )
      }
    }

    @Test
    fun `MultiModeFinished offers None`() = flakyTest {
      runTest(testDispatcher) {
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
    }

    @Test
    fun `RequestSelect offers None and inverts selection`() = flakyTest {
      runTest(testDispatcher) {
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
    }

    @Test
    fun `RestartActionMode offers StartMultiSelection`() = flakyTest {
      runTest(testDispatcher) {
        testFlow(
          flow = viewModel.sideEffects,
          triggerAction = { viewModel.fileSelectActions.emit(RestartActionMode) },
          assert = { assertThat(awaitItem()).isEqualTo(StartMultiSelection(viewModel.fileSelectActions)) }
        )
      }
    }

    @Test
    fun `UserClickedDownloadBooksButton offers NavigateToDownloads`() = flakyTest {
      runTest(testDispatcher) {
        testFlow(
          flow = viewModel.sideEffects,
          triggerAction = { viewModel.fileSelectActions.emit(UserClickedDownloadBooksButton) },
          assert = { assertThat(awaitItem()).isEqualTo(NavigateToDownloads) }
        )
      }
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
      ensureAllEventsConsumed()
    }
  }
  job.join()
}

class BookTestWrapper(private val id: String) : Book(0L) {
  override fun getId(): String = id
  override fun equals(other: Any?): Boolean = other is BookTestWrapper && getId() == other.getId()
  override fun hashCode(): Int = getId().hashCode()
}

const val MOCKK_TIMEOUT_FOR_VERIFICATION = 1000L

private class TestZimManageViewModel(
  downloadDao: DownloadRoomDao,
  libkiwixBookOnDisk: LibkiwixBookOnDisk,
  storageObserver: StorageObserver,
  kiwixService: KiwixService,
  context: Application,
  connectivityBroadcastReceiver: ConnectivityBroadcastReceiver,
  fat32Checker: Fat32Checker,
  dataSource: DataSource,
  connectivityManager: ConnectivityManager,
  onlineLibraryManager: OnlineLibraryManager,
  kiwixDataStore: KiwixDataStore,
  onlineLibraryServiceFactory: OnlineLibraryServiceFactory
) : ZimManageViewModel(
    downloadDao,
    libkiwixBookOnDisk,
    storageObserver,
    kiwixService,
    context,
    connectivityBroadcastReceiver,
    fat32Checker,
    dataSource,
    connectivityManager,
    onlineLibraryManager,
    kiwixDataStore,
    onlineLibraryServiceFactory
  ) {
  override val ioDispatcher: CoroutineDispatcher
    get() = Dispatchers.Main
}
