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
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import com.jraska.livedata.test
import io.mockk.clearAllMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.kiwix.kiwixmobile.core.StorageObserver
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookOnDisk
import org.kiwix.kiwixmobile.core.data.DataSource
import org.kiwix.kiwixmobile.core.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.core.reader.integrity.ValidateZimViewModel
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.files.ScanningProgressListener
import org.kiwix.kiwixmobile.core.zim_manager.NetworkState
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.SelectionMode
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.SelectionMode.MULTI
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.SelectionMode.NORMAL
import org.kiwix.kiwixmobile.language.viewmodel.flakyTest
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.MultiModeFinished
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.RequestDeleteMultiSelection
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.RequestMultiSelection
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.RequestNavigateTo
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.RequestSelect
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
import org.kiwix.libkiwix.Book
import org.kiwix.sharedFunctions.InstantExecutorExtension
import org.kiwix.sharedFunctions.MainDispatcherRule
import org.kiwix.sharedFunctions.bookOnDisk
import org.kiwix.sharedFunctions.libkiwixBook
import kotlin.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(InstantExecutorExtension::class)
class ZimManageViewModelTest {
  private val libkiwixBookOnDisk: LibkiwixBookOnDisk = mockk()
  private val storageObserver: StorageObserver = mockk()
  private val application: Application = mockk(relaxed = true)
  private val fat32Checker: Fat32Checker = mockk()
  private val dataSource: DataSource = mockk()
  private val alertDialogShower: AlertDialogShower = mockk()
  private val validateZimViewModel: ValidateZimViewModel = mockk()
  lateinit var viewModel: ZimManageViewModel

  private val downloads = MutableStateFlow<List<DownloadModel>>(emptyList())
  private val booksOnFileSystem = MutableStateFlow<List<Book>>(emptyList())
  private val books = MutableStateFlow<List<BooksOnDiskListItem.BookOnDisk>>(emptyList())
  private val onlineContentLanguage = MutableStateFlow("")
  private val fileSystemStates =
    MutableStateFlow<Fat32Checker.FileSystemState>(Fat32Checker.FileSystemState.DetectingFileSystem)
  private val networkStates = MutableStateFlow(NetworkState.NOT_CONNECTED)
  private val booksOnDiskListItems = MutableStateFlow<List<BooksOnDiskListItem>>(emptyList())

  @RegisterExtension
  private val mainDispatcherRule = MainDispatcherRule()

  @AfterEach
  fun teardown() {
    viewModel.onClearedExposed()
  }

  @BeforeEach
  fun init() {
    clearAllMocks()
    every { libkiwixBookOnDisk.books() } returns books
    every {
      storageObserver.getBooksOnFileSystem(
        any<ScanningProgressListener>()
      )
    } returns booksOnFileSystem
    every { fat32Checker.fileSystemStates } returns fileSystemStates
    every { application.getString(any()) } returns ""
    every { application.getString(any(), any()) } returns ""
    every { dataSource.booksOnDiskAsListItems() } returns booksOnDiskListItems
    downloads.value = emptyList()
    booksOnFileSystem.value = emptyList()
    books.value = emptyList()
    fileSystemStates.value = Fat32Checker.FileSystemState.DetectingFileSystem
    booksOnDiskListItems.value = emptyList()
    networkStates.value = NetworkState.NOT_CONNECTED
    onlineContentLanguage.value = ""
    viewModel =
      ZimManageViewModel(
        libkiwixBookOnDisk,
        storageObserver,
        application,
        dataSource,
        mainDispatcherRule.dispatcher
      ).apply {
        setAlertDialogShower(alertDialogShower)
        setValidateZimViewModel(validateZimViewModel)
      }
    viewModel.fileSelectListStates.value = FileSelectListState(emptyList())
  }

  @Nested
  inner class Books {
    @Test
    fun `emissions from data source are observed`() = flakyTest {
      runTest {
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
    }

    @Test
    fun `books found on filesystem are filtered by books already in db`() = flakyTest {
      runTest {
        every { application.getString(any()) } returns ""
        val expectedBook = bookOnDisk(1L, libkiwixBook("1", nativeBook = BookTestWrapper("1")))
        val bookToRemove = bookOnDisk(1L, libkiwixBook("2", nativeBook = BookTestWrapper("2")))
        booksOnFileSystem.emit(
          listOfNotNull(
            expectedBook.book.nativeBook,
            expectedBook.book.nativeBook,
            bookToRemove.book.nativeBook
          )
        )
        books.emit(listOf(bookToRemove))
        viewModel.requestFileSystemCheck.emit(Unit)
        advanceUntilIdle()
        coVerify {
          libkiwixBookOnDisk.insert(listOfNotNull(expectedBook.book.nativeBook))
        }
      }
    }
  }

  @Nested
  inner class SideEffects {
    @Test
    fun `RequestNavigateTo offers OpenFileWithNavigation with selected books`() = flakyTest {
      runTest {
        val selectedBook = bookOnDisk().apply { isSelected = true }
        viewModel.fileSelectListStates.value =
          FileSelectListState(listOf(selectedBook, bookOnDisk()), NORMAL)
        testFlow(
          flow = viewModel.sideEffects,
          triggerAction = {
            viewModel.fileSelectActions.emit(RequestNavigateTo(selectedBook))
          },
          assert = { assertThat(awaitItem()).isEqualTo(OpenFileWithNavigation(selectedBook)) }
        )
      }
    }

    @Test
    fun `RequestMultiSelection offers StartMultiSelection and selects a book`() = flakyTest {
      runTest {
        val bookToSelect = bookOnDisk(databaseId = 0L)
        val unSelectedBook = bookOnDisk(databaseId = 1L)
        viewModel.fileSelectListStates.value =
          FileSelectListState(
            listOf(
              bookToSelect,
              unSelectedBook
            ),
            SelectionMode.NORMAL
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
      runTest {
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
      runTest {
        val selectedBook = bookOnDisk().apply { isSelected = true }
        viewModel.fileSelectListStates.value =
          FileSelectListState(listOf(selectedBook, bookOnDisk()), NORMAL)
        testFlow(
          flow = viewModel.sideEffects,
          triggerAction = { viewModel.fileSelectActions.emit(RequestShareMultiSelection) },
          assert = {
            assertThat(awaitItem()).isEqualTo(
              ShareFiles(
                listOf(selectedBook),
                viewModel.viewModelScope,
                mainDispatcherRule.dispatcher
              )
            )
          }
        )
      }
    }

    @Test
    fun `RequestValidateZimFiles offers ValidateZIMFiles with selected books`() = flakyTest {
      runTest {
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
      runTest {
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
      runTest {
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
      runTest {
        testFlow(
          flow = viewModel.sideEffects,
          triggerAction = { viewModel.fileSelectActions.emit(RestartActionMode) },
          assert = { assertThat(awaitItem()).isEqualTo(StartMultiSelection(viewModel.fileSelectActions)) }
        )
      }
    }

    @Test
    fun `UserClickedDownloadBooksButton offers NavigateToDownloads`() = flakyTest {
      runTest {
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
  flow.test(timeout = timeout) {
    triggerAction()
    assert()
    cancelAndIgnoreRemainingEvents()
  }
}

suspend inline fun <reified T> ReceiveTurbine<*>.awaitItemOfType(): T {
  while (true) {
    val item = awaitItem()
    if (item is T) return item
  }
}

class BookTestWrapper(private val id: String) : Book(0L) {
  override fun getId(): String = id
  override fun equals(other: Any?): Boolean = other is BookTestWrapper && getId() == other.getId()
  override fun hashCode(): Int = getId().hashCode()
}
