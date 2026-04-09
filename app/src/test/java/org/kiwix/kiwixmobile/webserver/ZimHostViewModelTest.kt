/*
 * Kiwix Android
 * Copyright (c) 2026 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.webserver

import android.app.Application
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.ui.theme.StartServerGreen
import org.kiwix.kiwixmobile.core.ui.theme.StopServerRed
import org.kiwix.kiwixmobile.core.data.DataSource
import org.kiwix.kiwixmobile.core.entity.LibkiwixBook
import org.kiwix.kiwixmobile.core.qr.GenerateQR
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.ConnectivityReporter
import org.kiwix.kiwixmobile.core.utils.KiwixPermissionChecker
import org.kiwix.kiwixmobile.core.utils.ServerUtils
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem.BookOnDisk
import org.kiwix.kiwixmobile.webserver.ZimHostViewModel.Event
import org.kiwix.sharedFunctions.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class ZimHostViewModelTest {
  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()

  private val context: Application = mockk(relaxed = true)
  private val dataSource: DataSource = mockk()
  private val kiwixDataStore: KiwixDataStore = mockk(relaxed = true)
  private val generateQr: GenerateQR = mockk(relaxed = true)
  private val connectivityReporter: ConnectivityReporter = mockk()
  private val zimReaderContainer: ZimReaderContainer = mockk()
  private val kiwixPermissionChecker: KiwixPermissionChecker = mockk()
  private val fakeReader: ZimFileReader = mockk(relaxed = true)
  private val book1 = BookOnDisk(zimFileReader = fakeReader, isSelected = false)
  private val book2 = BookOnDisk(zimFileReader = fakeReader, isSelected = false)

  companion object {
    private const val IP_ADDRESS = "192.168.31.9:8080"
  }

  private lateinit var viewModel: ZimHostViewModel

  @Before
  fun setUp() {

    coEvery { dataSource.getLanguageCategorizedBooks() } returns flowOf(
      listOf(book1, book2)
    )
    coEvery { kiwixDataStore.hostedBookIds } returns flowOf(emptySet())
    coEvery { kiwixDataStore.setHostedBookIds(any()) } returns Unit

    every { zimReaderContainer.zimFileReader } returns null

    // Permission
    coEvery { kiwixPermissionChecker.hasNotificationPermission() } returns true
    coEvery { kiwixPermissionChecker.hasReadExternalStoragePermission() } returns true
    coEvery { kiwixPermissionChecker.isManageExternalStoragePermissionGranted() } returns true
    every { kiwixPermissionChecker.isAndroid13orAbove() } returns false

    every { context.getString(any()) } returns ""
    every { context.getString(any(), any()) } returns ""

    ServerUtils.isServerStarted = false

    viewModel = ZimHostViewModel(
      context = context,
      dataSource = dataSource,
      kiwixDataStore = kiwixDataStore,
      generateQr = generateQr,
      connectivityReporter = connectivityReporter,
      zimReaderContainer = zimReaderContainer,
      ioDispatcher = mainDispatcherRule.dispatcher,
      kiwixPermissionChecker = kiwixPermissionChecker
    )
  }

  @After
  fun tearDown() {
    ServerUtils.isServerStarted = false
  }

  // ======== loadBooks() ========

  @Test
  fun loadBooks_whenHostedIdsEmpty_SelectsAllBooks() = runTest {

    viewModel.loadBooks(isCustomApp = false)
    advanceUntilIdle()

    val books = viewModel.uiState.value.books.filterIsInstance<BookOnDisk>()
    assertTrue("Expected books list to be non-empty", books.isNotEmpty())
    assertTrue(
      "Initially all books should be selected when no host IDs",
      books.all { it.isSelected })
  }

  @Test
  fun loadBooks_whenHostedIdsContainBookId_onlySelectsThatBook() = runTest {
    val reader1: ZimFileReader =
      mockk(relaxed = true) { every { toBook() } returns LibkiwixBook(_id = "id1") }
    val reader2: ZimFileReader =
      mockk(relaxed = true) { every { toBook() } returns LibkiwixBook(_id = "id2") }

    coEvery { dataSource.getLanguageCategorizedBooks() } returns flowOf(
      listOf(
        BookOnDisk(zimFileReader = reader1, isSelected = false),
        BookOnDisk(zimFileReader = reader2, isSelected = false)
      )
    )
    coEvery { kiwixDataStore.hostedBookIds } returns flowOf(setOf("id1"))

    viewModel.loadBooks(isCustomApp = false)
    advanceUntilIdle()

    val books = viewModel.uiState.value.books.filterIsInstance<BookOnDisk>()
    assertTrue(books.find { it.book.id == "id1" }!!.isSelected)
    assertFalse(books.find { it.book.id == "id2" }!!.isSelected)
  }

  @Test
  fun loadBooks_whenHostedIdsContainTitle_selectsMatchingBookByTitle() = runTest {
    val reader: ZimFileReader =
      mockk(relaxed = true) { every { toBook() } returns LibkiwixBook(_title = "Kotlin") }

    coEvery { dataSource.getLanguageCategorizedBooks() } returns flowOf(
      listOf(BookOnDisk(zimFileReader = reader, isSelected = false))
    )
    coEvery { kiwixDataStore.hostedBookIds } returns flowOf(setOf("Kotlin"))

    viewModel.loadBooks(isCustomApp = false)
    advanceUntilIdle()

    val books = viewModel.uiState.value.books.filterIsInstance<BookOnDisk>()
    assertTrue(books.find { it.book.title == "Kotlin" }!!.isSelected)
  }

  @Test
  fun loadBooks_whenIsCustomAppTrue_returnsSingleSelectedBook() = runTest {
    val fakeReader: ZimFileReader = mockk(relaxed = true)
    every { zimReaderContainer.zimFileReader } returns fakeReader

    coEvery { dataSource.getLanguageCategorizedBooks() } returns flowOf(listOf(book1))

    viewModel.loadBooks(isCustomApp = true)
    advanceUntilIdle()

    val books = viewModel.uiState.value.books
    assertEquals("Custom app should have only one book", 1, books.size)
    assertTrue((books[0] as BookOnDisk).isSelected)
  }

  @Test
  fun loadBooks_whenServerStarted_returnsServerIsRunning() = runTest {
    ServerUtils.isServerStarted = true
    ServerUtils.serverAddress = IP_ADDRESS

    viewModel.loadBooks(isCustomApp = false)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(IP_ADDRESS, state.serverIpAddress)
    assertTrue(state.showShareIcon)
    assertTrue(state.qrVisible)

    ServerUtils.isServerStarted = false
  }

  @Test
  fun loadBooks_whenServerStopped_returnsServerIsStopped() = runTest {
    viewModel.loadBooks(isCustomApp = false)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals("", state.serverIpAddress)
    assertFalse(state.showShareIcon)
    assertFalse(state.qrVisible)
  }

  // ======== startServerButtonClick() ========

  @Test
  fun startServerButtonClick_whenServerIsRunning_returnsStopServer() = runTest {
    ServerUtils.isServerStarted = true

    viewModel.events.test {
      viewModel.startServerButtonClick()
      advanceUntilIdle()

      val stopServerEvent = awaitItem()
      assertEquals(Event.StopServer, stopServerEvent)

      cancelAndIgnoreRemainingEvents()
    }

    ServerUtils.isServerStarted = false
  }

  @Test
  fun startServerButtonClick_whenNoBooksSelected_emitsShowNoBooksToast() = runTest {
    val reader: ZimFileReader = mockk(relaxed = true) {
      every { toBook() } returns LibkiwixBook(_id = "id1")
    }
    coEvery { dataSource.getLanguageCategorizedBooks() } returns flowOf(
      listOf(BookOnDisk(zimFileReader = reader, isSelected = false))
    )
    coEvery { kiwixDataStore.hostedBookIds } returns flowOf(setOf("id2"))

    viewModel.loadBooks(isCustomApp = false)
    advanceUntilIdle()

    viewModel.events.test {
      viewModel.startServerButtonClick()
      advanceUntilIdle()
      assertEquals(Event.ShowNoBooksToast, awaitItem())
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun startServerButtonClick_whenWifiAvailable_emitsShowWifiDialog() = runTest {
    setupOneSelectedBook()
    every { connectivityReporter.checkWifi() } returns true

    viewModel.events.test {
      viewModel.startServerButtonClick()
      advanceUntilIdle()

      assertEquals(Event.ShowWifiDialog, awaitItem())
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun startServerButtonClick_whenTethering_emitsStartIpCheck() = runTest {
    setupOneSelectedBook()
    every { connectivityReporter.checkWifi() } returns false
    every { connectivityReporter.checkTethering() } returns true

    viewModel.events.test {
      viewModel.startServerButtonClick()
      advanceUntilIdle()

      assertEquals(Event.StartIpCheck, awaitItem())
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun startServerButtonClick_whenNoNetwork_emitsShowManualHotspotDialog() = runTest {
    setupOneSelectedBook()
    every { connectivityReporter.checkWifi() } returns false
    every { connectivityReporter.checkTethering() } returns false

    viewModel.events.test {
      viewModel.startServerButtonClick()
      advanceUntilIdle()

      assertEquals(Event.ShowManualHotspotDialog, awaitItem())
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun startServerButtonClick_whenNoNotificationPermission_emitsAskNotificationPermission() =
    runTest {
      coEvery { kiwixPermissionChecker.hasNotificationPermission() } returns false

      viewModel.events.test {
        viewModel.startServerButtonClick()
        advanceUntilIdle()

        assertEquals(Event.AskNotificationPermission, awaitItem())
        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun startServerButtonClick_whenNoReadPermission_emitsAskReadWritePermission() = runTest {
    coEvery { kiwixPermissionChecker.hasReadExternalStoragePermission() } returns false

    viewModel.events.test {
      viewModel.startServerButtonClick()
      advanceUntilIdle()

      assertEquals(Event.AskReadWritePermission, awaitItem())
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun startServerButtonClick_whenNoManageStoragePerm_emitsAllFilesPermissionDialog() = runTest {
    coEvery {
      kiwixPermissionChecker.isManageExternalStoragePermissionGranted()
    } returns false

    viewModel.events.test {
      viewModel.startServerButtonClick()
      advanceUntilIdle()

      assertEquals(Event.AllFilesPermissionDialog, awaitItem())
      cancelAndIgnoreRemainingEvents()
    }
  }

  // ======== onBookSelected() ========

  @Test
  fun onBookSelected_whenTogglesIsSelected_updatesUIState() = runTest {
    val reader: ZimFileReader =
      mockk(relaxed = true) { every { toBook() } returns LibkiwixBook(_id = "id1") }
    val book = BookOnDisk(zimFileReader = reader, isSelected = false)

    coEvery { dataSource.getLanguageCategorizedBooks() } returns flowOf(listOf(book))
    coEvery { kiwixDataStore.hostedBookIds } returns flowOf(setOf("id2"))

    viewModel.loadBooks(isCustomApp = false)
    advanceUntilIdle()

    val initialBook = viewModel.uiState.value.books.filterIsInstance<BookOnDisk>().first()
    assertFalse("Initially book should be unselected", initialBook.isSelected)

    viewModel.onBookSelected(book)
    advanceUntilIdle()

    val updatedBook = viewModel.uiState.value.books.filterIsInstance<BookOnDisk>().first()
    assertTrue("Book should be selected after toggle", updatedBook.isSelected)
  }

  @Test
  fun onBookSelected_savesSelectionToDataStore() = runTest {
    viewModel.loadBooks(isCustomApp = false)
    advanceUntilIdle()

    val book = viewModel.uiState.value.books
      .filterIsInstance<BookOnDisk>()
      .first()

    viewModel.onBookSelected(book)
    advanceUntilIdle()

    coVerify { kiwixDataStore.setHostedBookIds(any()) }
  }

  @Test
  fun onBookSelected_whenServerRunning_emitsRestartServer() = runTest {
    ServerUtils.isServerStarted = true

    viewModel.loadBooks(isCustomApp = false)
    advanceUntilIdle()

    val book = viewModel.uiState.value.books
      .filterIsInstance<BookOnDisk>()
      .first()

    viewModel.events.test {
      viewModel.onBookSelected(book)
      advanceUntilIdle()

      val event = awaitItem() as Event.StartServer
      assertTrue(event.restart)
      cancelAndIgnoreRemainingEvents()
    }

    ServerUtils.isServerStarted = false
  }

  // ======== onServerStarted() ========
  @Test
  fun onServerStarted_whenServerStarts_updatesUiStateWithIpAndCorrectButtonColor() = runTest {
    viewModel.onServerStarted(IP_ADDRESS)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(IP_ADDRESS, state.serverIpAddress)
    assertTrue(state.showShareIcon)
    assertTrue(state.qrVisible)
    assertEquals(StopServerRed, state.startServerButtonColor)
  }

  @Test
  fun onServerStarted_whenServerStarts_emitsDismissDialog() = runTest {
    viewModel.events.test {
      viewModel.onServerStarted(IP_ADDRESS)
      advanceUntilIdle()

      assertEquals(Event.DismissDialog, awaitItem())
      cancelAndIgnoreRemainingEvents()
    }
  }

  // ======== onServerStopped() ========

  @Test
  fun onServerStopped_whenServerStops_resetsUiStateToDefaults() = runTest {
    viewModel.onServerStopped()
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals("", state.serverIpAddress)
    assertFalse(state.showShareIcon)
    assertFalse(state.qrVisible)
    assertEquals(StartServerGreen, state.startServerButtonColor)
  }
  // ======== onServerFailedToStart ========

  @Test
  fun onServerFailedToStart_whenServerFailsToStart_returnsWithErrorMessageAndEmitsDismissDialogThenErrorToast() =
    runTest {
      val errorRes = R.string.no_books_selected_toast_message

      viewModel.events.test {
        viewModel.onServerFailedToStart(errorRes)
        advanceUntilIdle()

        assertEquals(Event.DismissDialog, awaitItem())
        val toast = awaitItem()
        assertTrue(toast is Event.ShowErrorToast)
        assertEquals(errorRes, (toast as Event.ShowErrorToast).messageRes)

        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun onServerFailedToStart_whenErrorIsNull_emitsDismissDialogOnly() = runTest {
    viewModel.events.test {
      viewModel.onServerFailedToStart(null)
      advanceUntilIdle()

      assertEquals(Event.DismissDialog, awaitItem())

      expectNoEvents()

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun onIpAddressValid_whenValidIpAddress_emitsStartServerWithRestartFalse() = runTest {
    viewModel.events.test {
      viewModel.onIpAddressValid()
      advanceUntilIdle()

      val event = awaitItem() as Event.StartServer
      assertFalse("Restart should be false", event.restart)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun onIpAddressInvalid_emitsDismissDialog() = runTest {
    viewModel.events.test {
      viewModel.onIpAddressInvalid()
      advanceUntilIdle()

      val dismissDialogEvent = awaitItem()
      assertEquals(Event.DismissDialog, dismissDialogEvent)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun onWifiConfirmed_emitsStartIpCheck() = runTest {
    viewModel.events.test {
      viewModel.onWifiConfirmed()
      advanceUntilIdle()

      val ipCheckEvent = awaitItem()
      assertEquals(Event.StartIpCheck, ipCheckEvent)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun showNotificationPermissionRationaleDialog_emitsCorrectEvent() = runTest {
    viewModel.events.test {
      viewModel.showNotificationPermissionRationaleDialog()
      advanceUntilIdle()

      val permissionEvent = awaitItem()
      assertEquals(Event.NotificationPermissionRationaleDialog, permissionEvent)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun showReadPermissionRationalDialog_emitsCorrectEvent() = runTest {
    viewModel.events.test {
      viewModel.showReadPermissionRationalDialog()
      advanceUntilIdle()

      val readPermissionDialogEvent = awaitItem()
      assertEquals(Event.ReadPermissionRationaleDialog, readPermissionDialogEvent)
      cancelAndIgnoreRemainingEvents()
    }
  }

  private fun TestScope.setupOneSelectedBook() {
    val reader: ZimFileReader = mockk(relaxed = true) {
      every { toBook() } returns LibkiwixBook(_id = "id1")
    }
    coEvery { dataSource.getLanguageCategorizedBooks() } returns flowOf(
      listOf(BookOnDisk(zimFileReader = reader, isSelected = false))
    )
    coEvery { kiwixDataStore.hostedBookIds } returns flowOf(setOf("id1"))

    viewModel.loadBooks(isCustomApp = false)
    advanceUntilIdle()
  }
}
