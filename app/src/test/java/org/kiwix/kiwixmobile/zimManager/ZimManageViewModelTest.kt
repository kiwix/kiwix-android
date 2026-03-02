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
import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import kotlinx.coroutines.flow.Flow
import com.jraska.livedata.test
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.HttpUrl
import okhttp3.Response
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.kiwix.kiwixmobile.core.dao.DownloadRoomDao
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookOnDisk
import org.kiwix.kiwixmobile.core.data.remote.KiwixService
import org.kiwix.kiwixmobile.core.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.core.ui.components.ONE
import org.kiwix.kiwixmobile.core.utils.ZERO
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.zim_manager.ConnectivityBroadcastReceiver
import org.kiwix.kiwixmobile.core.zim_manager.NetworkState
import org.kiwix.kiwixmobile.core.zim_manager.NetworkState.CONNECTED
import org.kiwix.kiwixmobile.core.zim_manager.NetworkState.NOT_CONNECTED
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem.BookOnDisk
import org.kiwix.kiwixmobile.language.viewmodel.flakyTest
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.CanWrite4GbFile
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.CannotWrite4GbFile
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.OnlineLibraryRequest
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.OnlineLibraryResult
import org.kiwix.kiwixmobile.zimManager.libraryView.LibraryListItem
import org.kiwix.libkiwix.Book
import org.kiwix.sharedFunctions.InstantExecutorExtension
import org.kiwix.sharedFunctions.MOCK_BASE_URL
import org.kiwix.sharedFunctions.bookOnDisk
import org.kiwix.sharedFunctions.downloadModel
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
  private val kiwixService: KiwixService = mockk()
  private val application: Application = mockk(relaxed = true)
  private val connectivityBroadcastReceiver: ConnectivityBroadcastReceiver = mockk()
  private val fat32Checker: Fat32Checker = mockk()
  private val connectivityManager: ConnectivityManager = mockk()

  @Suppress("DEPRECATION")
  private val networkCapabilities: NetworkCapabilities = mockk()
  private val kiwixDataStore: KiwixDataStore = mockk()
  lateinit var viewModel: ZimManageViewModel

  private val downloads = MutableStateFlow<List<DownloadModel>>(emptyList())
  private val books = MutableStateFlow<List<BookOnDisk>>(emptyList())
  private val onlineContentLanguage = MutableStateFlow("")
  private val onlineCategoryContent = MutableStateFlow("")
  private val fileSystemStates =
    MutableStateFlow<FileSystemState>(FileSystemState.DetectingFileSystem)
  private val networkStates = MutableStateFlow(NetworkState.NOT_CONNECTED)
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
    every { fat32Checker.fileSystemStates } returns fileSystemStates
    every { connectivityBroadcastReceiver.networkStates } returns networkStates
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      every { application.registerReceiver(any(), any(), any()) } returns mockk()
    } else {
      @Suppress("UnspecifiedRegisterReceiverFlag")
      every { application.registerReceiver(any(), any()) } returns mockk()
    }
    every { application.getString(any()) } returns ""
    every { application.getString(any(), any()) } returns ""
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
    books.value = emptyList()
    fileSystemStates.value = FileSystemState.DetectingFileSystem
    networkStates.value = NOT_CONNECTED
    onlineContentLanguage.value = ""
    viewModel =
      ZimManageViewModel(
        downloadRoomDao,
        libkiwixBookOnDisk,
        kiwixService,
        application,
        connectivityBroadcastReceiver,
        fat32Checker,
        connectivityManager,
        onlineLibraryManager,
        kiwixDataStore
      ).apply {
        setIsUnitTestCase()
      }
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
  inner class Languages {
    @Test
    fun `changing language updates the filter and do the network request`() = flakyTest {
      runTest {
        every { application.getString(any()) } returns ""
        every { application.getString(any(), any()) } returns ""
        viewModel.onlineLibraryRequest.test {
          skipItems(1)
          onlineContentLanguage.emit("eng")
          val onlineLibraryRequest = awaitItem()
          assertThat(onlineLibraryRequest.lang).isEqualTo("eng")
          assertThat(onlineLibraryRequest.page).isEqualTo(ONE)
          assertThat(onlineLibraryRequest.isLoadMoreItem).isEqualTo(false)
        }
      }
    }
  }

  @Nested
  inner class Categories {
    @Test
    fun `changing category updates the filter and do the network request`() = flakyTest {
      runTest {
        every { application.getString(any()) } returns ""
        every { application.getString(any(), any()) } returns ""
        viewModel.onlineLibraryRequest.test {
          skipItems(1)
          onlineCategoryContent.emit("wikipedia")
          val onlineLibraryRequest = awaitItem()
          assertThat(onlineLibraryRequest.category).isEqualTo("wikipedia")
          assertThat(onlineLibraryRequest.page).isEqualTo(ONE)
          assertThat(onlineLibraryRequest.isLoadMoreItem).isEqualTo(false)
        }
      }
    }
  }

  @Test
  fun `network states observed`() = flakyTest {
    runTest {
      networkStates.tryEmit(NOT_CONNECTED)
      advanceUntilIdle()
      viewModel.networkStates.test()
        .assertValue(NOT_CONNECTED)
    }
  }

  @Test
  fun `updateOnlineLibraryFilters updates onlineLibraryRequest`() = flakyTest {
    runTest {
      viewModel.setIsUnitTestCase()
      val newRequest = ZimManageViewModel.OnlineLibraryRequest(
        query = "test",
        category = "cat",
        lang = "en",
        page = 2,
        isLoadMoreItem = true,
        version = 100L
      )
      viewModel.onlineLibraryRequest.test {
        viewModel.updateOnlineLibraryFilters(newRequest)
        assertThat(awaitItem()).isEqualTo(newRequest)
      }
    }
  }

  @Test
  fun `library update removes from sources and maps to list items`() = flakyTest {
    runTest {
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
              LibraryListItem.DividerItem(Long.MAX_VALUE - 1, "All languages"),
              LibraryListItem.BookItem(bookWithActiveLanguage, CanWrite4GbFile),
            )
          )
        }
      }
    }
  }

  @Test
  fun `library marks files over 4GB as can't download if file system state says to`() = flakyTest {
    runTest {
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
              LibraryListItem.DividerItem(Long.MIN_VALUE, "All languages"),
              LibraryListItem.BookItem(bookOver4Gb, CannotWrite4GbFile)
            )
          )
        }
        cancelAndConsumeRemainingEvents()
      }

      // test library items fetches for a particular language
      viewModel.libraryItems.test {
        coEvery {
          onlineLibraryManager.parseOPDSStreamAndGetBooks(any(), any())
        } returns arrayListOf(bookOver4Gb)
        every { application.getString(any(), any()) } answers { "Selected language: English" }
        networkStates.value = CONNECTED
        downloads.value = listOf()
        books.value = listOf()
        onlineContentLanguage.value = "eng"
        fileSystemStates.emit(FileSystemState.DetectingFileSystem)
        fileSystemStates.emit(CannotWrite4GbFile)
        advanceUntilIdle()

        val item = awaitItem()
        val bookItem = item.items.filterIsInstance<LibraryListItem.BookItem>().firstOrNull()
        if (bookItem?.fileSystemState == CannotWrite4GbFile) {
          assertThat(item.items).isEqualTo(
            listOf(
              LibraryListItem.DividerItem(Long.MIN_VALUE, "Selected language: English"),
              LibraryListItem.BookItem(bookOver4Gb, CannotWrite4GbFile)
            )
          )
        }
        cancelAndConsumeRemainingEvents()
      }
    }
  }

  @Test
  fun `library shows downloading books even when not in online source`() = flakyTest {
    runTest {
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

val TURBINE_TIMEOUT = 5000.toDuration(DurationUnit.MILLISECONDS)
const val MOCKK_TIMEOUT_FOR_VERIFICATION = 1000L
